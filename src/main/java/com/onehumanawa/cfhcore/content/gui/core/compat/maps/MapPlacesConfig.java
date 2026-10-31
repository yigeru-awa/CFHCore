package com.onehumanawa.cfhcore.content.gui.core.compat.maps;

import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import java.util.ArrayList;
import java.util.List;

public final class MapPlacesConfig {
   public List<String> hiddenGroups = new ArrayList();
   public List<String> hiddenPlaces = new ArrayList();
   public boolean inWorld = true;
   private static JsonConfig<MapPlacesConfig> file;

   static synchronized JsonConfig<MapPlacesConfig> file() {
      if (file == null) {
         file = JsonConfig.<MapPlacesConfig>of("map-places", MapPlacesConfig.class, MapPlacesConfig::new);
      }

      return file;
   }

   static MapPlacesConfig get() {
      MapPlacesConfig c = (MapPlacesConfig)file().get();
      if (c.hiddenGroups == null) {
         c.hiddenGroups = new ArrayList();
      }

      if (c.hiddenPlaces == null) {
         c.hiddenPlaces = new ArrayList();
      }

      return c;
   }

   static void save() {
      file().save();
   }
}
