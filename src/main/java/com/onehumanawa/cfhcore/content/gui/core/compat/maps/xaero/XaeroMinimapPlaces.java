package com.onehumanawa.cfhcore.content.gui.core.compat.maps.xaero;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.compat.maps.MapsCompat;
import com.onehumanawa.cfhcore.content.gui.core.compat.maps.PlaceBridge;
import xaero.common.HudMod;
import xaero.hud.minimap.Minimap;

public final class XaeroMinimapPlaces implements PlaceBridge, MapsCompat.WorldPins {
   private static volatile boolean off;

   static boolean off() {
      return off;
   }

   public String id() {
      return "xaerominimap";
   }

   public boolean attach() {
      Minimap minimap;
      try {
         HudMod mod = HudMod.INSTANCE;
         if (mod == null || !mod.isLoadedClient()) {
            return false;
         }

         minimap = mod.getMinimap();
         if (minimap == null) {
            return false;
         }

         if (!MinimapPins.fits()) {
            throw new IllegalStateException("its element renderers ask for more than CFHGui knows");
         }
      } catch (Throwable t) {
         CFHGui.LOGGER.warn("[CFHGui] Xaero's Minimap has changed since this version was made; CFHGui's places are not shown with it ({})", t.toString());
         return true;
      }

      try {
         minimap.getOverMapRendererHandler().add(new MinimapPins(false));
      } catch (Throwable t) {
         CFHGui.LOGGER.warn("[CFHGui] Xaero's Minimap has changed since this version was made; CFHGui's places are not shown on it ({})", t.toString());
      }

      try {
         minimap.getWorldRendererHandler().add(new MinimapPins(true));
      } catch (Throwable t) {
         CFHGui.LOGGER.warn("[CFHGui] Xaero's Minimap has changed since this version was made; CFHGui's places are not shown in the world ({})", t.toString());
      }

      return true;
   }

   public void abandon() {
      off = true;
   }
}
