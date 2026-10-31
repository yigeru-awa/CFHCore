package com.onehumanawa.cfhcore.content.gui.core.media;

import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;

public final class MediaConfig {
   public String tenorApiKey = "";
   public String tenorProxy = "https://bigbro-itzamekyros.duckdns.org/cfhgui/tenor/";
   public boolean diskCache = true;
   public int maxDiskCacheMb = 64;
   public int maxEntries = 64;
   public String userAgent = "CFHGui/1.0 (Minecraft)";
   private static JsonConfig<MediaConfig> file;

   public static synchronized JsonConfig<MediaConfig> file() {
      if (file == null) {
         file = JsonConfig.<MediaConfig>of("media", MediaConfig.class, MediaConfig::new);
      }

      return file;
   }

   public static MediaConfig get() {
      return (MediaConfig)file().get();
   }
}
