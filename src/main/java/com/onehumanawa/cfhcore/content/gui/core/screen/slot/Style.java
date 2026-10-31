package com.onehumanawa.cfhcore.content.gui.core.screen.slot;

import com.onehumanawa.cfhcore.content.gui.core.theme.Skin;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public enum Style {
   CFHGUI,
   VANILLA;

   public static Style parse(@Nullable String s, Style fallback) {
      if (s == null) {
         return fallback;
      } else {
         String v = s.trim().toUpperCase(Locale.ROOT);
         if (v.equals("VANILLA")) {
            return VANILLA;
         } else {
            return !v.equals("CFHGUI") && !v.equals("DARK") ? fallback : CFHGUI;
         }
      }
   }

   public static Style of(Skin skin) {
      return skin == Skin.VANILLA ? VANILLA : CFHGUI;
   }

   public Skin skin() {
      return this == VANILLA ? Skin.VANILLA : Skin.DARK;
   }

   public String key() {
      return this.name().toLowerCase(Locale.ROOT);
   }
}
