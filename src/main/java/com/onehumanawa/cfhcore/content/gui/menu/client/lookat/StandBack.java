package com.onehumanawa.cfhcore.content.gui.menu.client.lookat;

import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import org.jetbrains.annotations.Nullable;

final class StandBack {
   private static final String[] OWNERS = new String[]{"jade", "wthit", "theoneprobe", "insight"};
   private static boolean looked;
   private static @Nullable String found;

   private StandBack() {
   }

   static @Nullable String owner() {
      if (!looked) {
         looked = true;

         for(String id : OWNERS) {
            try {
               if (CFHGuiPlatform.get().isModLoaded(id)) {
                  found = id;
                  break;
               }
            } catch (RuntimeException e) {
               CFHGuiMenu.LOGGER.warn("[CFHGui Menu] could not ask whether {} is installed: {}", id, e.toString());
            }
         }

         if (found != null) {
            CFHGuiMenu.LOGGER.info("[CFHGui Menu] the look-at plate is left to {}", found);
         }
      }

      return found;
   }

   static String ownerName() {
      String id = owner();
      if (id == null) {
         return "";
      } else {
         try {
            return (String)CFHGuiPlatform.get().modInfo(id).map((info) -> info.name()).orElse(id);
         } catch (RuntimeException var2) {
            return id;
         }
      }
   }
}
