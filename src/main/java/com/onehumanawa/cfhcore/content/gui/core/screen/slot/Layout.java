package com.onehumanawa.cfhcore.content.gui.core.screen.slot;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public enum Layout {
   VANILLA,
   CUSTOM,
   OVERHAUL;

   public static Layout parse(@Nullable String s, Layout fallback) {
      if (s == null) {
         return fallback;
      } else {
         try {
            return valueOf(s.trim().toUpperCase(Locale.ROOT));
         } catch (IllegalArgumentException var3) {
            return fallback;
         }
      }
   }

   public String key() {
      return this.name().toLowerCase(Locale.ROOT);
   }

   public @Nullable Layout lower() {
      return this.ordinal() == 0 ? null : values()[this.ordinal() - 1];
   }
}
