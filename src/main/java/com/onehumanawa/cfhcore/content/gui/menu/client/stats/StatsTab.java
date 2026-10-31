package com.onehumanawa.cfhcore.content.gui.menu.client.stats;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public enum StatsTab {
   OVERVIEW(Icon.HOME, false),
   COMBAT(Icon.SWORD, false),
   TRAVEL(Icon.MAP, false),
   BLOCKS(Icon.BLOCK, false),
   ITEMS(Icon.PACK, false),
   GENERAL(Icon.LIST, false),
   PLAYERS(Icon.FRIENDS, true),
   RANKINGS(Icon.TROPHY, true);

   public final Icon icon;
   public final boolean server;

   private StatsTab(final Icon icon, final boolean server) {
      this.icon = icon;
      this.server = server;
   }

   public String id() {
      return this.name().toLowerCase(Locale.ROOT);
   }

   public Component title() {
      return Component.translatable("cfhgui.stats.tab." + this.id());
   }

   public static @Nullable StatsTab byId(@Nullable String id) {
      if (id == null) {
         return null;
      } else {
         for(StatsTab t : values()) {
            if (t.id().equals(id)) {
               return t;
            }
         }

         return null;
      }
   }
}
