package com.onehumanawa.cfhcore.content.gui.menu.client.lookat;

import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;

public final class LookAtConfig {
   public boolean enabled = true;
   public boolean yieldToMods = true;
   public boolean portrait = true;
   public boolean name = true;
   public boolean kind = true;
   public boolean health = true;
   public boolean armor = true;
   public boolean baby = true;
   public boolean owner = true;
   public boolean villager = true;
   public boolean mount = true;
   public boolean effects = true;
   private static JsonConfig<LookAtConfig> file;
   private static volatile int revision;

   public static synchronized JsonConfig<LookAtConfig> file() {
      if (file == null) {
         file = JsonConfig.of("lookat", LookAtConfig.class, LookAtConfig::new);
      }

      return file;
   }

   public static LookAtConfig get() {
      return (LookAtConfig)file().get();
   }

   public static void save() {
      file().save();
      ++revision;
   }

   public static void touch() {
      ++revision;
   }

   public static void reload() {
      file().load();
      ++revision;
   }

   public static int revision() {
      return revision;
   }
}
