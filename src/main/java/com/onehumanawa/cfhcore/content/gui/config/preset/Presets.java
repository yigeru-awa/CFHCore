package com.onehumanawa.cfhcore.content.gui.config.preset;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onehumanawa.cfhcore.content.gui.config.ConfigSettings;
import com.onehumanawa.cfhcore.content.gui.config.CFHGuiConfig;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionBinding;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionResolvers;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionValues;
import com.onehumanawa.cfhcore.content.gui.config.ui.ApplyQueue;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Presets {
   private static List<ConfigSettings.Preset> builtins;

   public static synchronized List<ConfigSettings.Preset> builtins() {
      if (builtins != null) {
         return builtins;
      } else {
         builtins = new ArrayList();

         for(String name : List.of("performance", "quality")) {
            try {
               InputStream in = Presets.class.getResourceAsStream("/cfhgui_config/presets/" + name + ".json");

               label55: {
                  try {
                     if (in != null) {
                        JsonObject o = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                        ConfigSettings.Preset p = new ConfigSettings.Preset(o.get("name").getAsString(), o.getAsJsonObject("values"), true);
                        p.description = o.has("description") ? o.get("description").getAsString() : "";
                        builtins.add(p);
                        break label55;
                     }
                  } catch (Throwable var6) {
                     if (in != null) {
                        try {
                           in.close();
                        } catch (Throwable var5) {
                           var6.addSuppressed(var5);
                        }
                     }

                     throw var6;
                  }

                  if (in != null) {
                     in.close();
                  }
                  continue;
               }

               if (in != null) {
                  in.close();
               }
            } catch (Exception e) {
               CFHGuiConfig.LOGGER.warn("[CFHGui Config] bad built-in preset {}: {}", name, e.toString());
            }
         }

         return builtins;
      }
   }

   public static List<ConfigSettings.Preset> all() {
      List<ConfigSettings.Preset> out = new ArrayList(builtins());
      out.addAll(ConfigSettings.get().presets);
      return out;
   }

   public static ApplyResult apply(ConfigSettings.Preset preset) {
      int applied = 0;
      int skipped = 0;

      for(Map.Entry<String, JsonElement> e : preset.values.entrySet()) {
         OptionBinding b = (OptionBinding)OptionResolvers.resolve((String)e.getKey()).orElse(null);
         if (b != null && b.type().snapshotable()) {
            try {
               b.set(OptionValues.fromJson(b, (JsonElement)e.getValue()));
               ++applied;
            } catch (Exception ex) {
               CFHGuiConfig.LOGGER.warn("[CFHGui Config] preset value {} rejected: {}", e.getKey(), ex.toString());
               ++skipped;
            }
         } else {
            ++skipped;
         }
      }

      ApplyQueue.flush();
      return new ApplyResult(applied, skipped);
   }

   public static JsonObject snapshot(List<? extends OptionBinding> bindings) {
      JsonObject o = new JsonObject();

      for(OptionBinding b : bindings) {
         if (b.type().snapshotable()) {
            try {
               o.add(b.id(), OptionValues.toJson(b, b.get()));
            } catch (Exception var5) {
            }
         }
      }

      return o;
   }

   public static void saveUser(String name, String description, JsonObject values) {
      ConfigSettings.file().update((c) -> {
         c.presets.removeIf((px) -> px.name.equalsIgnoreCase(name));
         ConfigSettings.Preset p = new ConfigSettings.Preset(name, values, false);
         p.description = description == null ? "" : description;
         c.presets.add(p);
      });
   }

   public static void deleteUser(ConfigSettings.Preset preset) {
      ConfigSettings.file().update((c) -> c.presets.removeIf((p) -> p == preset || p.name.equals(preset.name) && !preset.builtin));
   }

   private Presets() {
   }

   public static record ApplyResult(int applied, int skipped) {
   }
}
