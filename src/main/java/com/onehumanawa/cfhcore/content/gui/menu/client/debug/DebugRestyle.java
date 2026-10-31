package com.onehumanawa.cfhcore.content.gui.menu.client.debug;

import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Style;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;

public final class DebugRestyle {
   private static final String[] REBUILDERS = new String[]{"betterf3", "better_f3", "cleanf3", "clean_f3", "clean-f3", "f3scale", "debugtweaks"};
   private static boolean initialised;
   private static boolean on;
   private static boolean yielded;
   private static boolean broken;

   private DebugRestyle() {
   }

   public static synchronized void init() {
      if (!initialised) {
         initialised = true;

         try {
            CFHGuiPlatform platform = CFHGuiPlatform.get();

            for(String id : REBUILDERS) {
               if (platform.isModLoaded(id)) {
                  yielded = true;
                  CFHGuiMenu.LOGGER.info("[CFHGui Menu] the debug screen (F3) is left to {}, which draws it itself", id);
                  return;
               }
            }
         } catch (Exception e) {
            CFHGuiMenu.LOGGER.warn("[CFHGui Menu] could not ask which mods draw the debug screen: {}", e.toString());
         }

         Theme.onChange(DebugRestyle::refresh);
         MenuSlots.onChange(DebugRestyle::refresh);
         refresh();
      }
   }

   private static void refresh() {
      on = !yielded && !broken && MenuSlots.style("minecraft:debug") == Style.CFHGUI;
   }

   public static boolean active() {
      return on;
   }

   static Palette palette() {
      return theme().palette();
   }

   static Theme theme() {
      return Theme.variant(Style.CFHGUI);
   }

   public static boolean lines(GuiGraphics g, List<String> lines, boolean leftSide) {
      if (on && lines != null) {
         try {
            DebugText.draw(g, lines, leftSide);
            return true;
         } catch (RuntimeException e) {
            failed("drawing its text", e);
            return false;
         }
      } else {
         return false;
      }
   }

   static void failed(String what, Throwable t) {
      if (!broken) {
         broken = true;
         on = false;
         CFHGuiMenu.LOGGER.warn("[CFHGui Menu] the debug screen (F3) keeps the game's own look: {} failed", what, t);
      }
   }
}
