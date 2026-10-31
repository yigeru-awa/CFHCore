package com.onehumanawa.cfhcore.content.gui.config.curated;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.onehumanawa.cfhcore.content.gui.config.ConfigSettings;
import com.onehumanawa.cfhcore.content.gui.config.CFHGuiConfig;
import com.onehumanawa.cfhcore.content.gui.config.file.TextFile;
import com.onehumanawa.cfhcore.content.gui.config.option.Binding;
import com.onehumanawa.cfhcore.content.gui.config.option.Choice;
import com.onehumanawa.cfhcore.content.gui.config.option.NumberRange;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionBinding;
import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class CuratedPages {
   public static Path dir() {
      return JsonConfig.dir().resolve("config").resolve("pages");
   }

   public static void bootstrap() {
      try {
         Files.createDirectories(dir());
      } catch (IOException e) {
         CFHGuiConfig.LOGGER.warn("[CFHGui Config] cannot prepare the curated pages folder: {}", e.toString());
      }

      if (!ConfigSettings.get().dfPageRetired) {
         Path df = dir().resolve("df.json");

         try {
            if (Files.exists(df, new LinkOption[0]) && "df".equals(idOf(df))) {
               Files.move(df, dir().resolve("df.json.retired"), StandardCopyOption.REPLACE_EXISTING);
               CFHGuiConfig.LOGGER.info("[CFHGui Config] retired the old DF pack settings page (pages/df.json -> df.json.retired)");
            }
         } catch (IOException e) {
            CFHGuiConfig.LOGGER.warn("[CFHGui Config] cannot retire pages/df.json: {}", e.toString());
         }

         ConfigSettings.file().update((c) -> c.dfPageRetired = true);
      }
   }

   private static @Nullable String idOf(Path file) {
      try {
         JsonReader r = new JsonReader(new StringReader(TextFile.read(file)));
         r.setLenient(true);
         JsonElement root = JsonParser.parseReader(r);
         return root != null && root.isJsonObject() ? str(root.getAsJsonObject(), "id", (String)null) : null;
      } catch (Exception var3) {
         return null;
      }
   }

   public static List<PageDef> load() {
      List<PageDef> out = new ArrayList();
      if (!Files.isDirectory(dir(), new LinkOption[0])) {
         return out;
      } else {
         try {
            Stream<Path> s = Files.list(dir());

            try {
               for(Path p : s.sorted().toList()) {
                  if (p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json")) {
                     try {
                        JsonReader r = new JsonReader(new StringReader(TextFile.read(p)));
                        r.setLenient(true);
                        JsonElement root = JsonParser.parseReader(r);
                        if (root != null && root.isJsonObject()) {
                           PageDef def = parse(root.getAsJsonObject(), p);
                           if (def != null) {
                              out.add(def);
                           }
                        }
                     } catch (Exception e) {
                        CFHGuiConfig.LOGGER.warn("[CFHGui Config] bad curated page {}: {}", p.getFileName(), e.toString());
                     }
                  }
               }
            } catch (Throwable var9) {
               if (s != null) {
                  try {
                     s.close();
                  } catch (Throwable var7) {
                     var9.addSuppressed(var7);
                  }
               }

               throw var9;
            }

            if (s != null) {
               s.close();
            }
         } catch (IOException var10) {
         }

         return out;
      }
   }

   private static @Nullable PageDef parse(JsonObject o, Path file) {
      String fallbackId = file.getFileName().toString().replaceAll("\\.json$", "");
      String id = str(o, "id", fallbackId);
      String title = str(o, "title", id);
      Icon icon = Icon.SPARKLE;

      try {
         icon = Icon.valueOf(str(o, "icon", "SPARKLE").toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException var21) {
      }

      List<SectionDef> sections = new ArrayList();
      JsonElement secs = o.get("sections");
      if (secs != null && secs.isJsonArray()) {
         for(JsonElement se : secs.getAsJsonArray()) {
            if (se.isJsonObject()) {
               JsonObject so = se.getAsJsonObject();
               List<OptionDef> opts = new ArrayList();
               JsonElement options = so.get("options");
               if (options != null && options.isJsonArray()) {
                  for(JsonElement oe : options.getAsJsonArray()) {
                     if (oe.isJsonPrimitive()) {
                        opts.add(new OptionDef(oe.getAsString(), (String)null, (String)null, (NumberRange)null, (List)null, (Boolean)null));
                     } else if (oe.isJsonObject()) {
                        JsonObject oo = oe.getAsJsonObject();
                        String path = str(oo, "path", (String)null);
                        if (path != null) {
                           NumberRange range = null;
                           if (oo.has("min") && oo.has("max")) {
                              range = NumberRange.of(oo.get("min").getAsDouble(), oo.get("max").getAsDouble(), oo.has("step") ? oo.get("step").getAsDouble() : (double)0.0F);
                           }

                           List<String> choices = null;
                           if (oo.has("choices") && oo.get("choices").isJsonArray()) {
                              choices = new ArrayList();

                              for(JsonElement c : oo.getAsJsonArray("choices")) {
                                 choices.add(c.getAsString());
                              }
                           }

                           opts.add(new OptionDef(path, str(oo, "label", (String)null), str(oo, "tooltip", (String)null), range, choices, oo.has("restart") ? oo.get("restart").getAsBoolean() : null));
                        }
                     }
                  }
               }

               sections.add(new SectionDef(str(so, "title", ""), str(so, "description", (String)null), opts));
            }
         }
      }

      return new PageDef(id, title, icon, sections, file);
   }

   private static @Nullable String str(JsonObject o, String key, @Nullable String def) {
      JsonElement e = o.get(key);
      return e != null && e.isJsonPrimitive() ? e.getAsString() : def;
   }

   public static void write(PageDef def) throws IOException {
      JsonObject o = new JsonObject();
      o.addProperty("id", def.id());
      o.addProperty("title", def.title());
      o.addProperty("icon", def.icon().name());
      JsonArray secs = new JsonArray();

      for(SectionDef s : def.sections()) {
         JsonObject so = new JsonObject();
         so.addProperty("title", s.title());
         if (s.description() != null) {
            so.addProperty("description", s.description());
         }

         JsonArray opts = new JsonArray();

         for(OptionDef od : s.options()) {
            JsonObject oo = new JsonObject();
            oo.addProperty("path", od.path());
            if (od.label() != null) {
               oo.addProperty("label", od.label());
            }

            if (od.tooltip() != null) {
               oo.addProperty("tooltip", od.tooltip());
            }

            opts.add(oo);
         }

         so.add("options", opts);
         secs.add(so);
      }

      o.add("sections", secs);
      TextFile.writeAtomic(def.file(), JsonConfig.GSON.toJson(o));
   }

   private CuratedPages() {
   }

   public static record OptionDef(String path, @Nullable String label, @Nullable String tooltip, @Nullable NumberRange range, @Nullable List<String> choices, @Nullable Boolean restart) {
      public OptionBinding apply(OptionBinding base) {
         return this.label == null && this.tooltip == null && this.range == null && this.choices == null && this.restart == null ? base : Binding.override(base, this.label == null ? null : Component.literal(this.label), this.tooltip == null ? null : Component.literal(this.tooltip), this.range, this.choices == null ? null : Choice.ofStrings(this.choices), this.restart);
      }
   }

   public static record SectionDef(String title, @Nullable String description, List<OptionDef> options) {
   }

   public static record PageDef(String id, String title, Icon icon, List<SectionDef> sections, Path file) {
   }
}
