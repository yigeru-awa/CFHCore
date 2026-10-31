package com.onehumanawa.cfhcore.content.gui.menu.client.play.model;

import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WorldFavorites {
   private static JsonConfig<Data> file;

   private static synchronized JsonConfig<Data> file() {
      if (file == null) {
         file = JsonConfig.at(JsonConfig.dir().resolve("menu").resolve("favorites.json"), Data.class, Data::new);
      }

      return file;
   }

   public static boolean isFavorite(String levelId) {
      return ((Data)file().get()).worlds.contains(levelId);
   }

   public static void setFavorite(String levelId, boolean favorite) {
      file().update((d) -> {
         d.worlds.remove(levelId);
         if (favorite) {
            d.worlds.add(levelId);
         }

      });
   }

   public static boolean toggleFavorite(String levelId) {
      boolean now = !isFavorite(levelId);
      setFavorite(levelId, now);
      return now;
   }

   public static List<String> tags(String levelId) {
      List<String> t = (List)((Data)file().get()).tags.get(levelId);
      return t == null ? List.of() : List.copyOf(t);
   }

   public static void setTags(String levelId, List<String> tags) {
      file().update((d) -> {
         List<String> clean = new ArrayList();

         for(String t : tags) {
            String s = t == null ? "" : t.trim();
            if (!s.isEmpty() && !clean.contains(s)) {
               clean.add(s);
            }
         }

         if (clean.isEmpty()) {
            d.tags.remove(levelId);
         } else {
            d.tags.put(levelId, clean);
         }

      });
   }

   public static List<String> allTags() {
      List<String> out = new ArrayList();

      for(List<String> l : ((Data)file().get()).tags.values()) {
         for(String t : l) {
            if (!out.contains(t)) {
               out.add(t);
            }
         }
      }

      return out;
   }

   public static void forget(String levelId) {
      file().update((d) -> {
         d.worlds.remove(levelId);
         d.tags.remove(levelId);
      });
   }

   private WorldFavorites() {
   }

   public static final class Data {
      public List<String> worlds = new ArrayList();
      public Map<String, List<String>> tags = new LinkedHashMap();
   }
}
