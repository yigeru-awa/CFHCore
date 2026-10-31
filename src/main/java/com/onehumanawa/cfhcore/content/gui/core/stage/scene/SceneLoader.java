package com.onehumanawa.cfhcore.content.gui.core.stage.scene;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.stage.anim.CameraPath;
import com.onehumanawa.cfhcore.content.gui.core.stage.anim.CameraPose;
import com.onehumanawa.cfhcore.content.gui.core.stage.anim.HoldPath;
import com.onehumanawa.cfhcore.content.gui.core.stage.anim.IdleMotion;
import com.onehumanawa.cfhcore.content.gui.core.stage.anim.LinePath;
import com.onehumanawa.cfhcore.content.gui.core.stage.anim.OrbitPath;
import com.onehumanawa.cfhcore.content.gui.core.stage.anim.SplinePath;
import com.onehumanawa.cfhcore.content.gui.core.stage.builds.BuildSpec;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.StructureNode;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public final class SceneLoader {
   private static final Map<ResourceLocation, Scene> CACHE = new HashMap();

   public static synchronized Scene load(ResourceLocation id) {
      Scene s = (Scene)CACHE.get(id);
      if (s == null) {
         s = read(id);
         CACHE.put(id, s);
      }

      return s;
   }

   public static synchronized void clear() {
      CACHE.clear();
   }

   public static Path configDir() {
      return CFHGuiPlatform.get().configDir().resolve("cfhgui").resolve("scenes");
   }

   public static String configName(ResourceLocation id, String ext) {
      String var10000 = id.getNamespace();
      return var10000 + "." + id.getPath().replace('/', '.') + "." + ext;
   }

   private static Scene read(ResourceLocation id) {
      JsonObject json = null;
      Path override = configDir().resolve(configName(id, "json"));
      if (Files.isRegularFile(override, new LinkOption[0])) {
         try {
            BufferedReader r = Files.newBufferedReader(override);

            try {
               json = JsonParser.parseReader(r).getAsJsonObject();
               CFHGui.LOGGER.info("[CFHGui] scene {} from {}", id, override);
            } catch (Throwable var12) {
               if (r != null) {
                  try {
                     r.close();
                  } catch (Throwable var9) {
                     var12.addSuppressed(var9);
                  }
               }

               throw var12;
            }

            if (r != null) {
               r.close();
            }
         } catch (Exception e) {
            CFHGui.LOGGER.warn("[CFHGui] scene override {} unreadable: {}", override, e.toString());
         }
      }

      if (json == null) {
         ResourceLocation file = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "cfhgui/scenes/" + id.getPath() + ".json");

         try {
            Resource res = (Resource)Minecraft.getInstance().getResourceManager().getResource(file).orElse(null);
            if (res != null) {
               BufferedReader r = res.openAsReader();

               try {
                  json = JsonParser.parseReader(r).getAsJsonObject();
               } catch (Throwable var10) {
                  if (r != null) {
                     try {
                        r.close();
                     } catch (Throwable var8) {
                        var10.addSuppressed(var8);
                     }
                  }

                  throw var10;
               }

               if (r != null) {
                  r.close();
               }
            }
         } catch (Exception e) {
            CFHGui.LOGGER.warn("[CFHGui] scene {} unreadable: {}", file, e.toString());
         }
      }

      if (json == null) {
         CFHGui.LOGGER.warn("[CFHGui] scene {} not found; using an empty scene", id);
         json = new JsonObject();
      }

      return parse(id, json);
   }

   static Scene parse(ResourceLocation id, JsonObject json) {
      Scene.Background background = parseBackground(json.get("background"));
      Scene.StructureRef structure = parseStructure(id, json.has("structure") && json.get("structure").isJsonObject() ? json.getAsJsonObject("structure") : null);
      Scene.CameraConfig camera = parseCamera(json.has("camera") && json.get("camera").isJsonObject() ? json.getAsJsonObject("camera") : new JsonObject());
      Scene.Light light = parseLight(json.has("light") && json.get("light").isJsonObject() ? json.getAsJsonObject("light") : new JsonObject());
      Map<String, Anchor> anchors = new HashMap();
      if (json.has("anchors") && json.get("anchors").isJsonObject()) {
         for(Map.Entry<String, JsonElement> e : json.getAsJsonObject("anchors").entrySet()) {
            if (((JsonElement)e.getValue()).isJsonObject()) {
               JsonObject a = ((JsonElement)e.getValue()).getAsJsonObject();
               Vector3f pos = vec3(a.get("pos"), new Vector3f());
               float yaw = GsonHelper.getAsFloat(a, "yaw", 0.0F);
               float pitch = 0.0F;
               float roll = 0.0F;
               if (a.has("rot") && a.get("rot").isJsonArray()) {
                  JsonArray r = a.getAsJsonArray("rot");
                  if (r.size() > 0) {
                     yaw = r.get(0).getAsFloat();
                  }

                  if (r.size() > 1) {
                     pitch = r.get(1).getAsFloat();
                  }

                  if (r.size() > 2) {
                     roll = r.get(2).getAsFloat();
                  }
               }

               anchors.put((String)e.getKey(), new Anchor((String)e.getKey(), pos, yaw, pitch, roll));
            }
         }
      }

      float resolution = GsonHelper.getAsFloat(json, "resolution", 1.0F);
      BuildSpec build = BuildSpec.parse(id, json.has("build") && json.get("build").isJsonObject() ? json.getAsJsonObject("build") : null);
      return new Scene(id, background, structure, camera, light, anchors, resolution, build, SceneLook.parse(json), json);
   }

   private static Scene.Background parseBackground(@Nullable JsonElement el) {
      if (el != null && !el.isJsonNull()) {
         if (el.isJsonPrimitive()) {
            if (el.getAsJsonPrimitive().isString() && "transparent".equalsIgnoreCase(el.getAsString())) {
               return new Scene.Background(0, 0, true);
            } else {
               int c = color(el, -15329771);
               return new Scene.Background(c, c, false);
            }
         } else {
            JsonObject o = el.getAsJsonObject();
            int top = color(o.get("top"), -15723492);
            int bottom = color(o.get("bottom"), -14473165);
            return new Scene.Background(top, bottom, false);
         }
      } else {
         return new Scene.Background(-15329771, -15329771, false);
      }
   }

   private static Scene.@Nullable StructureRef parseStructure(ResourceLocation id, @Nullable JsonObject o) {
      if (o == null) {
         return null;
      } else {
         String file = GsonHelper.getAsString(o, "file", id.getPath());
         Path var10000 = configDir();
         String var10001 = id.getNamespace();
         CompoundTag nbt = StructureNode.readFile(var10000.resolve(var10001 + "." + file.replace('/', '.') + ".nbt"));
         if (nbt == null) {
            nbt = StructureNode.readResource(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "cfhgui/scenes/" + file + ".nbt"));
         }

         if (nbt == null) {
            CFHGui.LOGGER.warn("[CFHGui] scene {}: structure '{}' not found", id, file);
         }

         Set<BlockPos> hidden = new HashSet();
         if (o.has("hide") && o.get("hide").isJsonArray()) {
            for(JsonElement e : o.getAsJsonArray("hide")) {
               if (e.isJsonArray() && e.getAsJsonArray().size() >= 3) {
                  JsonArray a = e.getAsJsonArray();
                  hidden.add(new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()));
               }
            }
         }

         return new Scene.StructureRef(nbt, vec3(o.get("offset"), new Vector3f()), GsonHelper.getAsBoolean(o, "centered", false), GsonHelper.getAsBoolean(o, "hideAnchorBlocks", true), hidden);
      }
   }

   private static Scene.CameraConfig parseCamera(JsonObject o) {
      float fov = GsonHelper.getAsFloat(o, "fov", 45.0F);
      CameraPose rest = new CameraPose(0.0F, 2.0F, 6.0F, 0.0F, 1.0F, 0.0F, fov, 0.0F);
      if (o.has("pose") && o.get("pose").isJsonObject()) {
         pose(o.getAsJsonObject("pose"), rest, rest);
      }

      List<CameraPath> intro = new ArrayList();
      if (o.has("intro") && o.get("intro").isJsonArray()) {
         for(JsonElement e : o.getAsJsonArray("intro")) {
            if (e.isJsonObject()) {
               CameraPath p = parsePath(e.getAsJsonObject(), rest);
               if (p != null) {
                  intro.add(p);
               }
            }
         }
      }

      IdleMotion idle = IdleMotion.NONE;
      if (o.has("idle") && o.get("idle").isJsonObject()) {
         JsonObject i = o.getAsJsonObject("idle");
         String type = GsonHelper.getAsString(i, "type", "sway").toLowerCase(Locale.ROOT);
         float period = GsonHelper.getAsFloat(i, "period", 12000.0F);
         float bob = GsonHelper.getAsFloat(i, "bob", 0.0F);
         IdleMotion var10000;
         switch (type) {
            case "orbit" -> var10000 = IdleMotion.orbit(period, bob);
            case "none" -> var10000 = IdleMotion.NONE;
            default -> var10000 = IdleMotion.sway(period, GsonHelper.getAsFloat(i, "yaw", 3.0F), GsonHelper.getAsFloat(i, "pitch", 1.0F), bob);
         }

         idle = var10000;
      }

      return new Scene.CameraConfig(rest, intro, idle, GsonHelper.getAsFloat(o, "near", 0.05F), GsonHelper.getAsFloat(o, "far", 256.0F));
   }

   public static @Nullable CameraPath parsePath(JsonObject o, CameraPose defaults) {
      String type = GsonHelper.getAsString(o, "type", "line").toLowerCase(Locale.ROOT);
      float duration = GsonHelper.getAsFloat(o, "duration", 1000.0F);
      Ease ease = CameraPath.parseEase(GsonHelper.getAsString(o, "ease", "outCubic"));
      switch (type) {
         case "hold":
            return new HoldPath(o.has("pose") && o.get("pose").isJsonObject() ? pose(o.getAsJsonObject("pose"), defaults, new CameraPose()) : null, duration);
         case "line":
            if (o.has("to") && o.get("to").isJsonObject()) {
               CameraPose to = pose(o.getAsJsonObject("to"), defaults, new CameraPose());
               CameraPose from = o.has("from") && o.get("from").isJsonObject() ? pose(o.getAsJsonObject("from"), defaults, new CameraPose()) : null;
               return new LinePath(from, to, duration, ease);
            }

            return null;
         case "spline":
            List<CameraPose> points = new ArrayList();
            if (o.has("points") && o.get("points").isJsonArray()) {
               for(JsonElement e : o.getAsJsonArray("points")) {
                  if (e.isJsonObject()) {
                     points.add(pose(e.getAsJsonObject(), defaults, new CameraPose()));
                  }
               }
            }

            if (points.isEmpty()) {
               return null;
            }

            return new SplinePath(points, duration, ease);
         case "orbit":
            return new OrbitPath(vec3(o.get("center"), new Vector3f()), GsonHelper.getAsFloat(o, "radius", 6.0F), GsonHelper.getAsFloat(o, "height", 2.0F), GsonHelper.getAsFloat(o, "from", 0.0F), GsonHelper.getAsFloat(o, "to", 90.0F), GsonHelper.getAsFloat(o, "fov", defaults.fov), GsonHelper.getAsFloat(o, "targetHeight", 1.0F), duration, ease);
         default:
            CFHGui.LOGGER.warn("[CFHGui] scene: unknown camera path type '{}'", type);
            return null;
      }
   }

   private static Scene.Light parseLight(JsonObject o) {
      Vector3f sun = vec3(o.get("sun"), new Vector3f(0.35F, 1.0F, 0.55F));
      boolean fog = false;
      float fogStart = 12.0F;
      float fogEnd = 40.0F;
      int fogColor = -15329771;
      if (o.has("fog") && o.get("fog").isJsonObject()) {
         JsonObject f = o.getAsJsonObject("fog");
         fog = GsonHelper.getAsBoolean(f, "enabled", true);
         fogStart = GsonHelper.getAsFloat(f, "start", fogStart);
         fogEnd = GsonHelper.getAsFloat(f, "end", fogEnd);
         fogColor = color(f.get("color"), fogColor);
      }

      return new Scene.Light(sun, GsonHelper.getAsFloat(o, "ambient", 0.45F), GsonHelper.getAsInt(o, "blockLight", 15), GsonHelper.getAsInt(o, "skyLight", 15), GsonHelper.getAsFloat(o, "brightness", 1.0F), fog, fogStart, fogEnd, fogColor);
   }

   static CameraPose pose(JsonObject o, CameraPose defaults, CameraPose out) {
      Vector3f p = vec3(o.get("position"), new Vector3f(defaults.position));
      Vector3f t = vec3(o.get("target"), new Vector3f(defaults.target));
      out.set(p.x, p.y, p.z, t.x, t.y, t.z, GsonHelper.getAsFloat(o, "fov", defaults.fov), GsonHelper.getAsFloat(o, "roll", defaults.roll));
      return out;
   }

   static Vector3f vec3(@Nullable JsonElement el, Vector3f fallback) {
      if (el != null && el.isJsonArray() && el.getAsJsonArray().size() >= 3) {
         JsonArray a = el.getAsJsonArray();
         return fallback.set(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat());
      } else {
         return fallback;
      }
   }

   static int color(@Nullable JsonElement el, int fallback) {
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
         } catch (Exception var3) {
         }

         return fallback;
      } else {
         return fallback;
      }
   }

   private SceneLoader() {
   }
}
