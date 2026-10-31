package com.onehumanawa.cfhcore.content.gui.menu.client.play.model;

import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ServerMeta {
   private static JsonConfig<Data> file;

   private static synchronized JsonConfig<Data> file() {
      if (file == null) {
         file = JsonConfig.at(JsonConfig.dir().resolve("menu").resolve("servers.json"), Data.class, Data::new);
      }

      return file;
   }

   private static Entry entry(String address, boolean create) {
      Data d = (Data)file().get();
      String key = ServerActions.normalize(address);
      Entry e = (Entry)d.servers.get(key);
      if (e == null && create) {
         e = new Entry();
         d.servers.put(key, e);
      }

      return e;
   }

   public static boolean isFavorite(String address) {
      Entry e = entry(address, false);
      return e != null && e.favorite;
   }

   public static void setFavorite(String address, boolean favorite) {
      file().update((d) -> entry(address, true).favorite = favorite);
   }

   public static String group(String address) {
      Entry e = entry(address, false);
      return e != null && e.group != null ? e.group : "";
   }

   public static void setGroup(String address, String group) {
      file().update((d) -> {
         entry(address, true).group = group == null ? "" : group;
         if (group != null && !group.isBlank() && !d.groups.contains(group)) {
            d.groups.add(group);
         }

      });
   }

   public static List<String> groups() {
      return List.copyOf(((Data)file().get()).groups);
   }

   public static void addGroup(String group) {
      if (group != null && !group.isBlank()) {
         file().update((d) -> {
            if (!d.groups.contains(group)) {
               d.groups.add(group);
            }

         });
      }
   }

   public static void removeGroup(String group) {
      file().update((d) -> {
         d.groups.remove(group);

         for(Entry e : d.servers.values()) {
            if (group.equals(e.group)) {
               e.group = "";
            }
         }

      });
   }

   public static void forget(String address) {
      file().update((d) -> d.servers.remove(ServerActions.normalize(address)));
   }

   private ServerMeta() {
   }

   public static final class Entry {
      public boolean favorite;
      public String group = "";
   }

   public static final class Data {
      public Map<String, Entry> servers = new LinkedHashMap();
      public List<String> groups = new ArrayList();
   }
}
