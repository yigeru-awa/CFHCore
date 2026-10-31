package com.onehumanawa.cfhcore.content.gui.menu.client.play.model;

import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CommunityServers {
   private static JsonConfig<Data> file;

   private static synchronized JsonConfig<Data> file() {
      if (file == null) {
         file = JsonConfig.at(JsonConfig.dir().resolve("menu").resolve("community_servers.json"), Data.class, Data::new);
      }

      return file;
   }

   public static List<Entry> all() {
      List<Entry> out = new ArrayList();
      Data d = (Data)file().get();
      if (d.servers == null) {
         return out;
      } else {
         for(Entry e : d.servers) {
            if (e != null && e.address != null && !e.address.isBlank() && !isExample(e.address)) {
               out.add(e);
            }
         }

         return out;
      }
   }

   static boolean isExample(String address) {
      String host = address.trim().toLowerCase(Locale.ROOT);
      int port = host.lastIndexOf(58);
      if (port > 0 && host.indexOf(58) == port) {
         host = host.substring(0, port);
      }

      if (host.endsWith(".")) {
         host = host.substring(0, host.length() - 1);
      }

      for(String domain : List.of("example.com", "example.net", "example.org", "example")) {
         if (host.equals(domain) || host.endsWith("." + domain)) {
            return true;
         }
      }

      return false;
   }

   public static void reload() {
      file().load();
   }

   private CommunityServers() {
   }

   public static final class Entry {
      public String name = "";
      public String address = "";
      public String description = "";

      public Entry() {
      }

      public Entry(String name, String address, String description) {
         this.name = name;
         this.address = address;
         this.description = description;
      }
   }

   public static final class Data {
      public String _readme = "Servers listed here appear under 'Community' on the CFHGui multiplayer screen. Each entry needs a name and an address (host or host:port); description is optional and shown as a tooltip. Players can join directly or add an entry to their own list. The example entry is not shown (example.com addresses never are); replace it with real ones.";
      public List<Entry> servers = new ArrayList(List.of(new Entry("Example community server", "play.example.com", "Replace me: this entry ships as an example.")));
   }
}
