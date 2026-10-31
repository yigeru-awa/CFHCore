package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title;

import java.util.Locale;
import net.minecraft.network.chat.Component;

public enum TitlePlayIcon {
   BLOCK("block"),
   PLANET("planet");

   private final String id;

   private TitlePlayIcon(final String id) {
      this.id = id;
   }

   public String id() {
      return this.id;
   }

   public Component title() {
      return Component.translatable("cfhgui.overhaul.play_icon." + this.id);
   }

   public Component description() {
      return Component.translatable("cfhgui.overhaul.play_icon." + this.id + ".desc");
   }

   public static TitlePlayIcon of(String id) {
      if (id != null) {
         String key = id.trim().toLowerCase(Locale.ROOT);

         for(TitlePlayIcon i : values()) {
            if (i.id.equals(key)) {
               return i;
            }
         }
      }

      return BLOCK;
   }
}
