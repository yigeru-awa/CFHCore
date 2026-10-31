package com.onehumanawa.cfhcore.content.gui.core.media;

import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class GifStore {
   private static final int MAX_RECENT = 60;
   private static final int MAX_FAVORITES = 300;
   private static JsonConfig<Data> file;

   private static synchronized JsonConfig<Data> file() {
      if (file == null) {
         file = JsonConfig.<Data>at(JsonConfig.dir().resolve("media").resolve("gifs.json"), Data.class, Data::new);
      }

      Data d = file.get();
      if (d.favorites == null) {
         d.favorites = new ArrayList();
      }

      if (d.recent == null) {
         d.recent = new ArrayList();
      }

      return file;
   }

   public static synchronized List<String> favorites() {
      return new ArrayList(((Data)file().get()).favorites);
   }

   public static synchronized List<String> recent() {
      return new ArrayList(((Data)file().get()).recent);
   }

   public static synchronized boolean isFavorite(String url) {
      return ((Data)file().get()).favorites.contains(url);
   }

   public static synchronized boolean toggleFavorite(String url) {
      Data d = (Data)file().get();
      boolean now;
      if (d.favorites.remove(url)) {
         now = false;
      } else {
         d.favorites.add(0, url);

         while(d.favorites.size() > 300) {
            d.favorites.remove(d.favorites.size() - 1);
         }

         now = true;
      }

      file().save();
      return now;
   }

   public static synchronized void noteRecent(String url) {
      if (url != null && !url.isBlank()) {
         Data d = (Data)file().get();
         LinkedHashSet<String> set = new LinkedHashSet();
         set.add(url);
         set.addAll(d.recent);
         d.recent = new ArrayList(set);

         while(d.recent.size() > 60) {
            d.recent.remove(d.recent.size() - 1);
         }

         file().save();
      }
   }

   public static synchronized void clearRecent() {
      ((Data)file().get()).recent.clear();
      file().save();
   }

   private GifStore() {
   }

   public static final class Data {
      public List<String> favorites = new ArrayList();
      public List<String> recent = new ArrayList();
   }
}
