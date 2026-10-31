package com.onehumanawa.cfhcore.content.gui.config.doc;

import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class Documents {
   private static final Map<String, FileDocument> CACHE = new ConcurrentHashMap();

   public static Path locate(String file) {
      Path p = Path.of(file);
      if (p.isAbsolute()) {
         return p;
      } else {
         Path game = CFHGuiPlatform.get().gameDir().resolve(file);
         if (Files.exists(game, new LinkOption[0])) {
            return game;
         } else {
            Path cfg = CFHGuiPlatform.get().configDir().resolve(file);
            if (Files.exists(cfg, new LinkOption[0])) {
               return cfg;
            } else {
               return !file.startsWith("config/") && !file.startsWith("config\\") ? cfg : game;
            }
         }
      }
   }

   public static String relativeToGame(Path path) {
      Path game = CFHGuiPlatform.get().gameDir().toAbsolutePath().normalize();
      Path abs = path.toAbsolutePath().normalize();
      return abs.startsWith(game) ? game.relativize(abs).toString().replace('\\', '/') : abs.toString().replace('\\', '/');
   }

   public static String relativeToConfig(Path path) {
      Path cfg = CFHGuiPlatform.get().configDir().toAbsolutePath().normalize();
      Path abs = path.toAbsolutePath().normalize();
      return abs.startsWith(cfg) ? cfg.relativize(abs).toString().replace('\\', '/') : abs.getFileName().toString();
   }

   public static JsonDocument json(Path path, String modId) {
      return (JsonDocument)CACHE.computeIfAbsent(key(path), (k) -> new JsonDocument(path, modId, relativeToGame(path)));
   }

   public static TomlDocument toml(Path path, String modId) {
      return (TomlDocument)CACHE.computeIfAbsent(key(path), (k) -> new TomlDocument(path, modId, relativeToConfig(path)));
   }

   public static PropertiesDocument properties(Path path, String modId) {
      return (PropertiesDocument)CACHE.computeIfAbsent(key(path), (k) -> new PropertiesDocument(path, modId, relativeToGame(path)));
   }

   public static Optional<FileDocument> open(Path path, String modId) {
      String n = path.getFileName().toString().toLowerCase(Locale.ROOT);
      if (!n.endsWith(".json") && !n.endsWith(".json5") && !n.endsWith(".jsonc")) {
         if (n.endsWith(".toml")) {
            return Optional.of(toml(path, modId));
         } else {
            return !n.endsWith(".properties") && !n.endsWith(".cfg") && (!n.endsWith(".txt") || !n.contains("options")) ? Optional.empty() : Optional.of(properties(path, modId));
         }
      } else {
         return Optional.of(json(path, modId));
      }
   }

   public static boolean isSupported(Path path) {
      String n = path.getFileName().toString().toLowerCase(Locale.ROOT);
      return n.endsWith(".json") || n.endsWith(".json5") || n.endsWith(".jsonc") || n.endsWith(".toml") || n.endsWith(".properties") || n.endsWith(".cfg");
   }

   public static void forget(Path path) {
      CACHE.remove(key(path));
   }

   private static String key(Path p) {
      return p.toAbsolutePath().normalize().toString();
   }

   private Documents() {
   }
}
