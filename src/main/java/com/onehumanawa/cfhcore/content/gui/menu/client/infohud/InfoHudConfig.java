package com.onehumanawa.cfhcore.content.gui.menu.client.infohud;

import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public final class InfoHudConfig {
   public boolean enabled = false;
   public String mode = "FULL";
   public String corner = "TOP_LEFT";
   public double scale = (double)0.75F;
   public int opacity = 85;
   public boolean position = true;
   public boolean chunk = false;
   public boolean facing = true;
   public boolean biome = true;
   public boolean dimension = true;
   public boolean light = true;
   public boolean time = true;
   public boolean speed = false;
   public boolean fps = true;
   public boolean graph = true;
   public boolean latency = true;
   public boolean memory = true;
   public boolean videoMemory = true;
   private static JsonConfig<InfoHudConfig> file;
   private static volatile int revision;

   public Mode mode() {
      return InfoHudConfig.Mode.parse(this.mode);
   }

   public Corner corner() {
      return InfoHudConfig.Corner.parse(this.corner);
   }

   public static synchronized JsonConfig<InfoHudConfig> file() {
      if (file == null) {
         file = JsonConfig.of("infohud", InfoHudConfig.class, InfoHudConfig::new);
      }

      return file;
   }

   public static InfoHudConfig get() {
      return (InfoHudConfig)file().get();
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

   public static enum Mode {
      FULL,
      COMPACT;

      public static Mode parse(@Nullable String s) {
         return s != null && s.trim().equalsIgnoreCase("COMPACT") ? COMPACT : FULL;
      }

      public String key() {
         return this.name().toLowerCase(Locale.ROOT);
      }
   }

   public static enum Corner {
      TOP_LEFT(false, false),
      TOP_RIGHT(true, false),
      BOTTOM_LEFT(false, true),
      BOTTOM_RIGHT(true, true);

      public final boolean right;
      public final boolean bottom;
      private static final Corner[] ALL = values();

      private Corner(final boolean right, final boolean bottom) {
         this.right = right;
         this.bottom = bottom;
      }

      public static Corner parse(@Nullable String s) {
         if (s != null) {
            String v = s.trim().toUpperCase(Locale.ROOT);

            for(Corner c : ALL) {
               if (c.name().equals(v)) {
                  return c;
               }
            }
         }

         return TOP_LEFT;
      }

      public String key() {
         return this.name().toLowerCase(Locale.ROOT);
      }
   }
}
