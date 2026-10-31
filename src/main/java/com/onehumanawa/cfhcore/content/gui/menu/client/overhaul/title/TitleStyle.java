package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title;

import java.util.Locale;
import net.minecraft.network.chat.Component;

public enum TitleStyle {
   MIXED("mixed"),
   DEFAULT("default"),
   STYLIZED("stylized");

   private final String id;

   private TitleStyle(final String id) {
      this.id = id;
   }

   public String id() {
      return this.id;
   }

   public Component title() {
      return Component.translatable("cfhgui.overhaul.style." + this.id);
   }

   public Component description() {
      return Component.translatable("cfhgui.overhaul.style." + this.id + ".desc");
   }

   public static TitleStyle of(String id) {
      if (id != null) {
         String key = id.trim().toLowerCase(Locale.ROOT);

         for(TitleStyle s : values()) {
            if (s.id.equals(key)) {
               return s;
            }
         }
      }

      return MIXED;
   }
}
