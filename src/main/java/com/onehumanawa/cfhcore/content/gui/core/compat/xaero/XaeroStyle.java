package com.onehumanawa.cfhcore.content.gui.core.compat.xaero;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Style;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public final class XaeroStyle {
   private static final String[] MINIMAP_MODS = new String[]{"xaerominimap", "xaerominimapfair", "xaerobetterpvp", "xaerobetterpvpfair"};
   private static final String WORLD_MAP_MOD = "xaeroworldmap";
   private static final String CLASSES = "xaero.";
   private static final String WORLD_MAP_CLASSES = "xaero.map.";
   private static final String LIBRARY_CLASSES = "xaero.lib.";
   private static boolean looked;
   private static boolean minimapInstalled;
   private static boolean worldMapInstalled;
   private static boolean minimap;
   private static boolean worldMap;
   public static final int MINIMAP = 1;
   public static final int WORLD_MAP = 2;
   public static final int BOTH = 3;
   private static boolean minimapBroken;
   private static boolean worldMapBroken;
   private static boolean frameWarned;
   private static String lastSlot = "xaero:minimap";

   private XaeroStyle() {
   }

   public static boolean minimapInstalled() {
      look();
      return minimapInstalled;
   }

   public static boolean worldMapInstalled() {
      look();
      return worldMapInstalled;
   }

   private static synchronized void look() {
      if (!looked) {
         try {
            CFHGuiPlatform platform = CFHGuiPlatform.get();

            for(String id : MINIMAP_MODS) {
               minimapInstalled |= platform.isModLoaded(id);
            }

            worldMapInstalled = platform.isModLoaded("xaeroworldmap");
         } catch (Exception e) {
            CFHGui.LOGGER.warn("[CFHGui] could not ask whether Xaero's maps are installed: {}", e.toString());
         }

         looked = true;
      }
   }

   public static void init() {
      look();
      if (minimapInstalled || worldMapInstalled) {
         MenuSlots.resolveWith(XaeroStyle::slotOf);
         Theme.onChange(XaeroStyle::refresh);
         MenuSlots.onChange(XaeroStyle::refresh);
         refresh();
         XaeroPreflight.run(minimapInstalled, worldMapInstalled);
         if (Boolean.getBoolean("cfhgui.xaero.harness")) {
            XaeroHarness.register(minimapInstalled, worldMapInstalled);
         }

      }
   }

   private static void refresh() {
      boolean mini = minimapInstalled && !minimapBroken && MenuSlots.style("xaero:minimap") == Style.CFHGUI;
      boolean world = worldMapInstalled && !worldMapBroken && MenuSlots.style("xaero:world_map") == Style.CFHGUI;
      if (mini != minimap || world != worldMap) {
         minimap = mini;
         worldMap = world;
         Reskin.invalidate();
      }
   }

   public static boolean minimap() {
      return minimap;
   }

   public static boolean worldMap() {
      return worldMap;
   }

   public static Palette palette() {
      return Theme.variant(Style.CFHGUI).palette();
   }

   public static int radius() {
      return Theme.variant(Style.CFHGUI).radius();
   }

   public static int panel(int color) {
      return Theme.variant(Style.CFHGUI).panel(color);
   }

   public static void failed(int which, String what, Throwable t) {
      boolean mini = (which & 1) != 0 && !minimapBroken;
      boolean world = (which & 2) != 0 && !worldMapBroken;
      if (mini || world) {
         minimapBroken |= mini;
         worldMapBroken |= world;
         CFHGui.LOGGER.warn("[CFHGui] Xaero's {} its own look: {} failed: {}", new Object[]{mini && world ? "maps keep" : (mini ? "Minimap keeps" : "World Map keeps"), what, t.toString()});
         refresh();
      }
   }

   public static void frameUnknown() {
      if (!frameWarned) {
         frameWarned = true;
         CFHGui.LOGGER.warn("[CFHGui] Xaero's Minimap keeps its own frame: this version of it draws its map in a way CFHGui does not know");
      }
   }

   public static boolean owns(String className) {
      return (minimapInstalled || worldMapInstalled) && className.startsWith("xaero.");
   }

   public static boolean restyles(Screen screen) {
      String slot = slotOf(screen);
      if (slot == null) {
         return false;
      } else {
         return "xaero:world_map".equals(slot) ? worldMap : minimap;
      }
   }

   private static @Nullable String slotOf(Screen screen) {
      String name = screen.getClass().getName();
      if (!name.startsWith("xaero.")) {
         return null;
      } else if (name.startsWith("xaero.lib.")) {
         return installed(lastSlot);
      } else {
         lastSlot = name.startsWith("xaero.map.") ? "xaero:world_map" : "xaero:minimap";
         return installed(lastSlot);
      }
   }

   private static @Nullable String installed(String slot) {
      if ("xaero:world_map".equals(slot)) {
         return worldMapInstalled ? slot : (minimapInstalled ? "xaero:minimap" : null);
      } else {
         return minimapInstalled ? slot : (worldMapInstalled ? "xaero:world_map" : null);
      }
   }
}
