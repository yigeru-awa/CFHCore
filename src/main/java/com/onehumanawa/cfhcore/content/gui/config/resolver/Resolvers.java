package com.onehumanawa.cfhcore.content.gui.config.resolver;

import com.onehumanawa.cfhcore.content.gui.config.ConfigPlatform;
import com.onehumanawa.cfhcore.content.gui.config.CFHGuiConfigApi;
import com.onehumanawa.cfhcore.content.gui.config.doc.DocSection;
import com.onehumanawa.cfhcore.content.gui.config.doc.Documents;
import com.onehumanawa.cfhcore.content.gui.config.doc.FileDocument;
import com.onehumanawa.cfhcore.content.gui.config.doc.JsonDocument;
import com.onehumanawa.cfhcore.content.gui.config.doc.PropertiesDocument;
import com.onehumanawa.cfhcore.content.gui.config.doc.TomlDocument;
import com.onehumanawa.cfhcore.content.gui.config.option.Binding;
import com.onehumanawa.cfhcore.content.gui.config.option.KeyBinding;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionBinding;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionResolver;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionResolvers;
import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class Resolvers {
   public static void registerAll() {
      OptionResolvers.register(new OptionResolver() {
         public String prefix() {
            return "optionsTxt";
         }

         public Optional<OptionBinding> resolve(String rest) {
            return VanillaOptions.get(rest);
         }
      });
      OptionResolvers.register(new OptionResolver() {
         public String prefix() {
            return "key";
         }

         public Optional<OptionBinding> resolve(String rest) {
            for(KeyMapping m : Minecraft.getInstance().options.keyMappings) {
               if (m.getName().equals(rest)) {
                  return Optional.of(new KeyBinding(m));
               }
            }

            return Optional.empty();
         }
      });
      OptionResolvers.register(new OptionResolver() {
         public String prefix() {
            return "json";
         }

         public Optional<OptionBinding> resolve(String rest) {
            int i = rest.lastIndexOf(58);
            if (i <= 0) {
               return Optional.empty();
            } else {
               Path file = Documents.locate(rest.substring(0, i));
               if (!Files.isRegularFile(file, new LinkOption[0])) {
                  return Optional.empty();
               } else {
                  JsonDocument doc = Documents.json(file, "");
                  return doc.binding(rest.substring(i + 1));
               }
            }
         }
      });
      OptionResolvers.register(new OptionResolver() {
         public String prefix() {
            return "toml";
         }

         public Optional<OptionBinding> resolve(String rest) {
            String[] parts = rest.split(":", 3);
            String modId;
            String file;
            String path;
            if (parts.length == 3) {
               modId = parts[0];
               file = parts[1];
               path = parts[2];
            } else {
               if (parts.length != 2) {
                  return Optional.empty();
               }

               modId = "";
               file = parts[0];
               path = parts[1];
            }

            String fileName = Path.of(file).getFileName().toString();
            Optional<FileDocument> nativeDoc = ConfigPlatform.get().nativeConfig(fileName);
            if (nativeDoc.isPresent() && ((FileDocument)nativeDoc.get()).editable()) {
               for(DocSection s : ((FileDocument)nativeDoc.get()).sections()) {
                  for(OptionBinding b : s.options()) {
                     if (b.id().endsWith(":" + path)) {
                        return Optional.of(b);
                     }
                  }
               }
            }

            Path p = CFHGuiPlatform.get().configDir().resolve(file);
            if (!Files.isRegularFile(p, new LinkOption[0])) {
               p = Documents.locate(file);
            }

            if (!Files.isRegularFile(p, new LinkOption[0])) {
               return Optional.empty();
            } else {
               TomlDocument doc = Documents.toml(p, modId);
               return doc.binding(path);
            }
         }
      });
      OptionResolvers.register(new OptionResolver() {
         public String prefix() {
            return "props";
         }

         public Optional<OptionBinding> resolve(String rest) {
            int i = rest.lastIndexOf(58);
            if (i <= 0) {
               return Optional.empty();
            } else {
               Path file = Documents.locate(rest.substring(0, i));
               if (!Files.isRegularFile(file, new LinkOption[0])) {
                  return Optional.empty();
               } else {
                  PropertiesDocument doc = Documents.properties(file, "");
                  return doc.binding(rest.substring(i + 1));
               }
            }
         }
      });
      OptionResolvers.register(new OptionResolver() {
         public String prefix() {
            return "cfhgui";
         }

         public Optional<OptionBinding> resolve(String rest) {
            int i = rest.indexOf(58);
            if (i <= 0) {
               return Optional.empty();
            } else {
               String module = rest.substring(0, i);
               String key = rest.substring(i + 1);
               return module.equals("core") ? CoreBindings.get(key) : Resolvers.cfhguiModuleBinding(module, key);
            }
         }
      });
      OptionResolvers.register(new OptionResolver() {
         public String prefix() {
            return "iris";
         }

         public Optional<OptionBinding> resolve(String rest) {
            return Optional.empty();
         }
      });
      OptionResolvers.register(new OptionResolver() {
         public String prefix() {
            return "voicechat";
         }

         public Optional<OptionBinding> resolve(String rest) {
            return Optional.empty();
         }
      });
   }

   public static Optional<OptionBinding> cfhguiModuleBinding(String module, String key) {
      Path file = JsonConfig.dir().resolve(module + ".json");
      if (!Files.isRegularFile(file, new LinkOption[0])) {
         return Optional.empty();
      } else {
         JsonDocument doc = Documents.json(file, "cfhgui_" + module);
         String pointer = key.startsWith("/") ? key : "/" + key.replace('.', '/');
         return doc.binding(pointer).map((b) -> wrapCFHGuiModule(module, b));
      }
   }

   public static List<OptionBinding> cfhguiModuleBindings(String module) {
      List<OptionBinding> out = new ArrayList();
      Path file = JsonConfig.dir().resolve(module + ".json");
      if (!Files.isRegularFile(file, new LinkOption[0])) {
         return out;
      } else {
         JsonDocument doc = Documents.json(file, "cfhgui_" + module);

         for(DocSection s : doc.sections()) {
            if (s.path().isEmpty()) {
               for(OptionBinding b : s.options()) {
                  out.add(wrapCFHGuiModule(module, b));
               }
            }
         }

         return out;
      }
   }

   private static OptionBinding wrapCFHGuiModule(String module, OptionBinding b) {
      boolean hasHook = CFHGuiConfigApi.hasReloadHook(module);
      String id = "cfhgui:" + module + ":" + b.id().substring(b.id().lastIndexOf(58) + 2).replace('/', '.');
      Binding var10000 = (new Binding(id, b.type(), b.label())).tooltip(b.tooltip());
      Objects.requireNonNull(b);
      return var10000.getter(b::get).setter((v) -> {
         b.set(v);
         CFHGuiConfigApi.runReloadHook(module);
      }).def(b.defaultValue()).range(b.range()).choices(b.choices()).restart(!hasHook).searchWords("cfhgui " + module);
   }

   private Resolvers() {
   }
}
