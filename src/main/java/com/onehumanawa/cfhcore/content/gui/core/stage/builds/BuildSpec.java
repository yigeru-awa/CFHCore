package com.onehumanawa.cfhcore.content.gui.core.stage.builds;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public record BuildSpec(ResourceLocation scene, String file, @Nullable int[] from, @Nullable int[] to, int rotate, Vector3f at, int grass, int foliage, int water, boolean light, int lightColor, float lightStrength, float glow, float fadeFrom, float fadeRound, List<int[]> clear, List<Edit> edits, int maxEntities, float entityReach, int entityTint, List<String> requires, @Nullable Credit credit, @Nullable Download download, @Nullable Fit fit, float yaw, @Nullable Land land, Map<String, List<String>> standins) {
   public static final List<String> EXTENSIONS = List.of("schem", "litematic", "nbt", "schematic");

   public @Nullable List<String> standinsFor(String id) {
      return (List)this.standins.get(id);
   }

   private static @Nullable Fit parseFit(@Nullable JsonElement el) {
      if (el != null && el.isJsonObject()) {
         JsonObject f = el.getAsJsonObject();
         return new Fit(GsonHelper.getAsInt(f, "x", 0), GsonHelper.getAsInt(f, "ground", -1), GsonHelper.getAsInt(f, "front", -12));
      } else {
         return null;
      }
   }

   public static @Nullable BuildSpec parse(ResourceLocation scene, @Nullable JsonObject o) {
      if (o == null) {
         return null;
      } else {
         String file = GsonHelper.getAsString(o, "file", scene.getPath());
         int[] from = ints(o.get("from"), 3);
         int[] to = ints(o.get("to"), 3);
         int rotate = Math.floorMod(Math.round(GsonHelper.getAsFloat(o, "rotate", 0.0F) / 90.0F) * 90, 360);
         int[] at = ints(o.get("at"), 3);
         int grass = 0;
         int foliage = 0;
         int water = 0;
         if (o.has("tint") && o.get("tint").isJsonObject()) {
            JsonObject t = o.getAsJsonObject("tint");
            grass = color(t.get("grass"));
            foliage = color(t.get("foliage"));
            water = color(t.get("water"));
         }

         boolean light = false;
         int lightColor = -15741;
         float lightStrength = 1.0F;
         if (o.has("light")) {
            JsonElement l = o.get("light");
            if (l.isJsonPrimitive()) {
               light = l.getAsBoolean();
            } else if (l.isJsonObject()) {
               light = GsonHelper.getAsBoolean(l.getAsJsonObject(), "enabled", true);
               if (l.getAsJsonObject().has("color")) {
                  lightColor = color(l.getAsJsonObject().get("color"));
               }

               lightStrength = GsonHelper.getAsFloat(l.getAsJsonObject(), "strength", 1.0F);
            }
         }

         float fadeFrom = 1.0F;
         float fadeRound = 0.0F;
         if (o.has("fade") && o.get("fade").isJsonObject()) {
            fadeFrom = GsonHelper.getAsFloat(o.getAsJsonObject("fade"), "from", 0.7F);
            fadeRound = GsonHelper.getAsFloat(o.getAsJsonObject("fade"), "round", 3.0F);
         }

         List<int[]> clear = new ArrayList();
         if (o.has("clear") && o.get("clear").isJsonArray()) {
            for(JsonElement e : o.getAsJsonArray("clear")) {
               int[] box = ints(e, 6);
               if (box != null) {
                  clear.add(box);
               }
            }
         }

         List<Edit> edits = new ArrayList();
         if (o.has("set") && o.get("set").isJsonArray()) {
            for(JsonElement e : o.getAsJsonArray("set")) {
               if (e.isJsonObject()) {
                  JsonObject s = e.getAsJsonObject();
                  String block = GsonHelper.getAsString(s, "block", "minecraft:air");
                  int[] a = ints(s.has("at") ? s.get("at") : s.get("from"), 3);
                  int[] b = ints(s.has("at") ? s.get("at") : s.get("to"), 3);
                  if (a != null && b != null) {
                     edits.add(new Edit(Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2]), Math.max(a[0], b[0]), Math.max(a[1], b[1]), Math.max(a[2], b[2]), block));
                  }
               }
            }
         }

         int maxEntities = 256;
         int entityTint = -1;
         float reach = 72.0F;
         if (o.has("entities") && o.get("entities").isJsonObject()) {
            JsonObject e = o.getAsJsonObject("entities");
            maxEntities = GsonHelper.getAsInt(e, "max", maxEntities);
            reach = GsonHelper.getAsFloat(e, "reach", reach);
            if (e.has("tint")) {
               entityTint = color(e.get("tint")) | -16777216;
            }
         }

         List<String> requires = new ArrayList();
         if (o.has("requires") && o.get("requires").isJsonArray()) {
            for(JsonElement e : o.getAsJsonArray("requires")) {
               if (e.isJsonPrimitive()) {
                  requires.add(e.getAsString().trim().toLowerCase(Locale.ROOT));
               }
            }
         }

         Credit credit = null;
         if (o.has("credit") && o.get("credit").isJsonObject()) {
            JsonObject c = o.getAsJsonObject("credit");
            credit = new Credit(GsonHelper.getAsString(c, "title", ""), GsonHelper.getAsString(c, "author", ""), GsonHelper.getAsString(c, "url", ""), GsonHelper.getAsString(c, "license", ""));
         }

         Download download = null;
         if (o.has("download") && o.get("download").isJsonObject()) {
            JsonObject d = o.getAsJsonObject("download");
            Match match = null;
            if (d.has("match") && d.get("match").isJsonObject()) {
               JsonObject m = d.getAsJsonObject("match");
               int[] size = ints(m.get("size"), 3);
               if (size != null) {
                  match = new Match(size[0], size[1], size[2], GsonHelper.getAsInt(m, "blocks", 0), strings(m.get("has")));
               }
            }

            download = new Download(d.has("url") ? GsonHelper.getAsString(d, "url") : null, d.has("page") ? GsonHelper.getAsString(d, "page") : null, strings(d.get("names")), match);
         }

         Map<String, List<String>> standins = new LinkedHashMap();
         if (o.has("standins") && o.get("standins").isJsonObject()) {
            for(Map.Entry<String, JsonElement> e : o.getAsJsonObject("standins").entrySet()) {
               List<String> to2 = ((JsonElement)e.getValue()).isJsonArray() ? strings((JsonElement)e.getValue()) : (((JsonElement)e.getValue()).isJsonPrimitive() ? List.of(((JsonElement)e.getValue()).getAsString()) : List.of());
               if (!to2.isEmpty()) {
                  standins.put(BuildPeek.id((String)e.getKey()), to2);
               }
            }
         }

         return new BuildSpec(scene, file, from, to, rotate, at == null ? new Vector3f() : new Vector3f((float)at[0], (float)at[1], (float)at[2]), grass, foliage, water, light, lightColor, lightStrength, GsonHelper.getAsFloat(o, "glow", 1.0F), fadeFrom, fadeRound, List.copyOf(clear), List.copyOf(edits), Math.max(0, maxEntities), reach, entityTint, List.copyOf(requires), credit, download, parseFit(o.get("fit")), GsonHelper.getAsFloat(o, "yaw", 0.0F), BuildSpec.Land.parse(o.get("land")), Collections.unmodifiableMap(standins));
      }
   }

   private static List<String> strings(@Nullable JsonElement el) {
      if (el != null && el.isJsonArray()) {
         List<String> out = new ArrayList();

         for(JsonElement e : el.getAsJsonArray()) {
            if (e.isJsonPrimitive()) {
               out.add(e.getAsString().trim());
            }
         }

         return List.copyOf(out);
      } else {
         return List.of();
      }
   }

   public boolean available() {
      for(String id : this.requires) {
         if (!CFHGuiPlatform.get().isModLoaded(id)) {
            return false;
         }
      }

      return true;
   }

   public String key() {
      StringBuilder b = (new StringBuilder(this.scene.getNamespace())).append(':').append(this.file);
      if (this.from != null && this.to != null) {
         b.append('|').append(Arrays.toString(this.from)).append(Arrays.toString(this.to));
      }

      b.append("|r").append(this.rotate).append("|l").append(this.light ? 1 : 0);

      for(int[] c : this.clear) {
         b.append("|c").append(Arrays.toString(c));
      }

      for(Edit e : this.edits) {
         b.append("|s").append(e);
      }

      b.append("|t").append(Integer.toHexString(this.grass)).append(',').append(Integer.toHexString(this.foliage)).append(',').append(Integer.toHexString(this.water));
      b.append("|y").append(this.yaw);
      if (this.fit != null) {
         b.append("|f").append(this.fit);
      } else {
         b.append("|a").append(this.at.x).append(',').append(this.at.y).append(',').append(this.at.z);
      }

      if (!this.standins.isEmpty()) {
         b.append("|st").append(this.standins.hashCode());
      }

      return b.toString();
   }

   public static Path configDir() {
      return CFHGuiPlatform.get().configDir().resolve("cfhgui").resolve("scenes");
   }

   public String fileName(String extension) {
      int slash = this.file.lastIndexOf(47);
      String var10000 = slash >= 0 ? this.file.substring(slash + 1) : this.file;
      return var10000 + "." + extension;
   }

   public @Nullable Found find() {
      Path dir = configDir();
      Path named = null;
      String namedExt = null;
      long newest = Long.MIN_VALUE;

      for(String ext : EXTENSIONS) {
         Path[] var10000 = new Path[2];
         String var10004 = this.scene.getNamespace();
         var10000[0] = dir.resolve(var10004 + "." + this.file.replace('/', '.') + "." + ext);
         var10000[1] = dir.resolve(this.fileName(ext));

         for(Path p : var10000) {
            if (Files.isRegularFile(p, new LinkOption[0])) {
               long t;
               try {
                  t = Files.getLastModifiedTime(p).toMillis();
               } catch (IOException var15) {
                  t = 0L;
               }

               if (named == null || t > newest) {
                  named = p;
                  namedExt = ext;
                  newest = t;
               }
            }
         }
      }

      if (named != null) {
         return new Found(named, (ResourceLocation)null, namedExt);
      } else {
         Path own = BuildFinder.inFolder(this, dir);
         if (own != null) {
            return new Found(own, (ResourceLocation)null, BuildFinder.extension(own));
         } else {
            try {
               ResourceManager rm = Minecraft.getInstance().getResourceManager();

               for(String ext : EXTENSIONS) {
                  ResourceLocation r = ResourceLocation.fromNamespaceAndPath(this.scene.getNamespace(), "cfhgui/scenes/" + this.file + "." + ext);
                  if (rm.getResource(r).isPresent()) {
                     return new Found((Path)null, r, ext);
                  }
               }
            } catch (RuntimeException var16) {
            }

            return null;
         }
      }
   }

   public String baseName() {
      int slash = this.file.lastIndexOf(47);
      return slash >= 0 ? this.file.substring(slash + 1) : this.file;
   }

   static @Nullable int[] ints(@Nullable JsonElement el, int count) {
      if (el != null && el.isJsonArray()) {
         JsonArray a = el.getAsJsonArray();
         if (a.size() < count) {
            return null;
         } else {
            int[] out = new int[count];

            for(int i = 0; i < count; ++i) {
               out[i] = Math.round(a.get(i).getAsFloat());
            }

            return out;
         }
      } else {
         return null;
      }
   }

   static int color(@Nullable JsonElement el) {
      if (el != null && !el.isJsonNull()) {
         try {
            if (el.getAsJsonPrimitive().isNumber()) {
               return el.getAsInt();
            }

            String s = el.getAsString().trim();
            if (s.startsWith("#")) {
               s = s.substring(1);
            }

            if (s.length() == 6) {
               return -16777216 | (int)Long.parseLong(s, 16);
            }

            if (s.length() == 8) {
               return (int)Long.parseLong(s, 16);
            }
         } catch (RuntimeException var2) {
         }

         return 0;
      } else {
         return 0;
      }
   }

   public static record Edit(int x0, int y0, int z0, int x1, int y1, int z1, String block) {
   }

   public static record Credit(String title, String author, String url, String license) {
   }

   public static record Download(@Nullable String url, @Nullable String page, List<String> names, @Nullable Match match) {
   }

   public static record Match(int sizeX, int sizeY, int sizeZ, int blocks, List<String> has) {
      static final float TOLERANCE = 0.02F;

      public boolean matches(BuildPeek.Signature s) {
         boolean size = s.sizeY() == this.sizeY && (s.sizeX() == this.sizeX && s.sizeZ() == this.sizeZ || s.sizeX() == this.sizeZ && s.sizeZ() == this.sizeX);
         if (!size) {
            return false;
         } else if (this.blocks > 0 && (float)Math.abs(s.blocks() - this.blocks) > Math.max(16.0F, (float)this.blocks * 0.02F)) {
            return false;
         } else if (this.has.isEmpty()) {
            return true;
         } else {
            int found = 0;

            for(String id : this.has) {
               if (s.kinds().contains(id)) {
                  ++found;
               }
            }

            return (double)found >= Math.ceil((double)this.has.size() * (double)0.75F);
         }
      }
   }

   public static record Fit(int x, int ground, int front) {
   }

   public static record Land(float level, float sea, boolean hasSea, float hills, float hillSize, float mountains, float mountainSize, float mountainFrom, float snow, float trees, int ground, float blend, int[][] rings, boolean under, long seed, float keep) {
      static final int[][] RINGS = new int[][]{{1, 32}, {4, 700}, {12, 3200}};

      static @Nullable Land parse(@Nullable JsonElement el) {
         if (el != null && el.isJsonObject()) {
            JsonObject o = el.getAsJsonObject();
            float[] hills = floats(o.get("hills"), new float[]{18.0F, 260.0F});
            float[] mountains = floats(o.get("mountains"), new float[]{0.0F, 700.0F, 900.0F});
            int[][] rings = RINGS;
            if (o.has("rings") && o.get("rings").isJsonArray()) {
               List<int[]> list = new ArrayList();

               for(JsonElement r : o.getAsJsonArray("rings")) {
                  int[] pair = BuildSpec.ints(r, 2);
                  if (pair != null && pair[0] >= 1 && pair[1] >= 1) {
                     list.add(pair);
                  }
               }

               if (!list.isEmpty()) {
                  rings = (int[][])list.toArray(new int[0][]);
               }
            }

            byte var10000;
            switch (GsonHelper.getAsString(o, "ground", "grass").toLowerCase(Locale.ROOT)) {
               case "sand" -> var10000 = 1;
               case "stone" -> var10000 = 2;
               case "snow" -> var10000 = 3;
               case "gravel" -> var10000 = 4;
               case "dirt" -> var10000 = 5;
               default -> var10000 = 0;
            }

            int ground = var10000;
            boolean sea = o.has("sea") && o.get("sea").isJsonPrimitive();
            return new Land(GsonHelper.getAsFloat(o, "level", 0.0F), sea ? o.get("sea").getAsFloat() : 0.0F, sea, hills[0], Math.max(8.0F, hills[1]), mountains[0], Math.max(16.0F, mountains[1]), mountains.length > 2 ? mountains[2] : 900.0F, GsonHelper.getAsFloat(o, "snow", 0.6F), Math.max(0.0F, Math.min(1.0F, GsonHelper.getAsFloat(o, "trees", 0.3F))), ground, Math.max(1.0F, GsonHelper.getAsFloat(o, "blend", 48.0F)), rings, GsonHelper.getAsBoolean(o, "under", false), GsonHelper.getAsLong(o, "seed", 7L), Math.max(0.0F, GsonHelper.getAsFloat(o, "keep", 0.0F)));
         } else {
            return null;
         }
      }

      private static float[] floats(@Nullable JsonElement el, float[] fallback) {
         if (el == null) {
            return fallback;
         } else if (el.isJsonPrimitive()) {
            float[] out = (float[])((float[]) fallback).clone();
            out[0] = el.getAsFloat();
            return out;
         } else if (!el.isJsonArray()) {
            return fallback;
         } else {
            float[] out = (float[])((float[]) fallback).clone();
            JsonArray a = el.getAsJsonArray();

            for(int i = 0; i < Math.min(a.size(), out.length); ++i) {
               out[i] = a.get(i).getAsFloat();
            }

            return out;
         }
      }

      String key() {
         StringBuilder b = (new StringBuilder()).append(this.level).append(',').append(this.hasSea ? String.valueOf(this.sea) : "-").append(',').append(this.hills).append(',').append(this.hillSize).append(',').append(this.mountains).append(',').append(this.mountainSize).append(',').append(this.mountainFrom).append(',').append(this.snow).append(',').append(this.trees).append(',').append(this.ground).append(',').append(this.blend).append(',').append(this.under).append(',').append(this.seed).append(',').append(this.keep);

         for(int[] r : this.rings) {
            b.append(';').append(r[0]).append('x').append(r[1]);
         }

         return b.toString();
      }
   }

   public static record Found(@Nullable Path path, @Nullable ResourceLocation resource, String extension) {
      public InputStream open() throws IOException {
         if (this.path != null) {
            return Files.newInputStream(this.path);
         } else {
            Resource res = (Resource)Minecraft.getInstance().getResourceManager().getResource(this.resource).orElseThrow(() -> new IOException("gone: " + String.valueOf(this.resource)));
            return res.open();
         }
      }

      public String toString() {
         return this.path != null ? this.path.toString() : String.valueOf(this.resource);
      }
   }
}
