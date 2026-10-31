package com.onehumanawa.cfhcore.content.gui.core.layout;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import com.onehumanawa.cfhcore.content.gui.core.layout.editor.LayoutEditor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class LayoutStore {
   private static final Map<String, ScreenLayout> CACHE = new ConcurrentHashMap();
   private static final Map<String, Long> STAMPS = new ConcurrentHashMap();

   public static Path dir() {
      return JsonConfig.dir().resolve("layouts");
   }

   public static Path fileFor(String screenId) {
      Path var10000 = dir();
      String var10001 = screenId.replace(':', '.');
      return var10000.resolve(var10001.replaceAll("[^A-Za-z0-9._-]", "_") + ".json");
   }

   public static ScreenLayout get(String screenId) {
      return (ScreenLayout)CACHE.computeIfAbsent(screenId, (id) -> {
         Path f = fileFor(id);
         STAMPS.put(id, stamp(f));
         if (!Files.isRegularFile(f, new LinkOption[0])) {
            return new ScreenLayout();
         } else {
            try {
               ScreenLayout l = (ScreenLayout)JsonConfig.GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), ScreenLayout.class);
               return l == null ? new ScreenLayout() : l.fillNulls();
            } catch (Exception e) {
               CFHGui.LOGGER.warn("[CFHGui] bad layout file {}: {}", f, e.toString());
               return new ScreenLayout();
            }
         }
      });
   }

   public static boolean refresh(String screenId) {
      if (CACHE.containsKey(screenId) && !LayoutEditor.holds(screenId)) {
         Long was = (Long)STAMPS.get(screenId);
         if (was != null && was == stamp(fileFor(screenId))) {
            return false;
         } else {
            CACHE.remove(screenId);
            get(screenId);
            CFHGui.LOGGER.info("[CFHGui] layout of {} changed on disk: read again", screenId);
            return true;
         }
      } else {
         return false;
      }
   }

   private static long stamp(Path f) {
      try {
         return !Files.isRegularFile(f, new LinkOption[0]) ? 0L : Files.getLastModifiedTime(f).toMillis() * 31L + Files.size(f);
      } catch (IOException var2) {
         return -1L;
      }
   }

   public static boolean has(String screenId) {
      return !get(screenId).isEmpty();
   }

   public static void save(String screenId, ScreenLayout layout) {
      layout.screen = screenId;
      CACHE.put(screenId, layout);
      Path f = fileFor(screenId);

      try {
         Files.createDirectories(f.getParent());
         if (layout.isEmpty()) {
            Files.deleteIfExists(f);
         } else {
            Path tmp = f.resolveSibling(String.valueOf(f.getFileName()) + ".tmp");
            Files.writeString(tmp, JsonConfig.GSON.toJson(layout), StandardCharsets.UTF_8);
            Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         }
      } catch (IOException e) {
         CFHGui.LOGGER.error("[CFHGui] could not save layout {}", f, e);
      }

      STAMPS.put(screenId, stamp(f));
   }

   public static void reset(String screenId) {
      save(screenId, new ScreenLayout());
   }

   public static void invalidate() {
      CACHE.clear();
      STAMPS.clear();
   }

   public static List<String> saved() {
      List<String> out = new ArrayList();
      if (!Files.isDirectory(dir(), new LinkOption[0])) {
         return out;
      } else {
         try {
            Stream<Path> s = Files.list(dir());

            try {
               s.filter((p) -> p.toString().endsWith(".json")).forEach((p) -> {
                  String n = p.getFileName().toString();
                  String base = n.substring(0, n.length() - 5);
                  int dot = base.indexOf(46);
                  out.add(dot < 0 ? base : base.substring(0, dot) + ":" + base.substring(dot + 1));
               });
            } catch (Throwable var5) {
               if (s != null) {
                  try {
                     s.close();
                  } catch (Throwable var4) {
                     var5.addSuppressed(var4);
                  }
               }

               throw var5;
            }

            if (s != null) {
               s.close();
            }
         } catch (IOException var6) {
         }

         return out;
      }
   }

   public static String export(String screenId) {
      return JsonConfig.GSON.toJson(get(screenId));
   }

   public static boolean importJson(String screenId, String json) {
      try {
         ScreenLayout l = (ScreenLayout)JsonConfig.GSON.fromJson(json, ScreenLayout.class);
         if (l == null) {
            return false;
         } else {
            save(screenId, l.fillNulls());
            return true;
         }
      } catch (Exception var3) {
         return false;
      }
   }

   private LayoutStore() {
   }
}
