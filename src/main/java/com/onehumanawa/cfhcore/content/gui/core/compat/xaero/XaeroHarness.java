package com.onehumanawa.cfhcore.content.gui.core.compat.xaero;

import com.google.common.collect.Lists;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.config.CoreConfig;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Style;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import xaero.common.HudMod;
import xaero.common.gui.GuiAddWaypoint;
import xaero.common.gui.GuiMinimapMain;
import xaero.common.gui.GuiWaypoints;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.lib.client.gui.config.context.BuiltInEditConfigScreenContexts;
import xaero.lib.client.gui.widget.dropdown.DropDownWidget;
import xaero.lib.common.config.profile.ConfigProfile;
import xaero.map.WorldMapSession;
import xaero.map.gui.GuiMap;
import xaero.map.gui.GuiWorldMapSettings;

final class XaeroHarness {
   private static Runnable later;
   private static int laterIn;

   private XaeroHarness() {
   }

   static void register(boolean minimap, boolean worldMap) {
      if (worldMap) {
         put("xaero:world_map", (parent) -> XaeroHarness.WorldMapScreens.map(parent));
         put("xaero:world_map_settings", (parent) -> XaeroHarness.WorldMapScreens.settings(parent));
         put("xaero:map_message", (parent) -> XaeroHarness.WorldMapScreens.message(parent));
         put("xaero:style_world_map_vanilla", (parent) -> style(parent, "xaero:world_map", Style.VANILLA));
         put("xaero:style_world_map_cfhgui", (parent) -> style(parent, "xaero:world_map", Style.CFHGUI));
      }

      if (minimap) {
         put("xaero:minimap_settings", (parent) -> XaeroHarness.MinimapScreens.settings(parent));
         put("xaero:waypoints", (parent) -> XaeroHarness.MinimapScreens.waypoints(parent));
         put("xaero:add_waypoint", (parent) -> XaeroHarness.MinimapScreens.addWaypoint(parent));
         put("xaero:sample_waypoints", (parent) -> XaeroHarness.MinimapScreens.sampleWaypoints(parent));
         put("xaero:shape_round", (parent) -> XaeroHarness.MinimapScreens.option(parent, 0, 1));
         put("xaero:shape_square", (parent) -> XaeroHarness.MinimapScreens.option(parent, 0, 0));
         put("xaero:north_locked", (parent) -> XaeroHarness.MinimapScreens.option(parent, 1, 1));
         put("xaero:north_free", (parent) -> XaeroHarness.MinimapScreens.option(parent, 1, 0));
         put("xaero:compass_on_frame", (parent) -> XaeroHarness.MinimapScreens.option(parent, 2, 2));
         put("xaero:compass_inside", (parent) -> XaeroHarness.MinimapScreens.option(parent, 2, 1));
         put("xaero:style_minimap_vanilla", (parent) -> style(parent, "xaero:minimap", Style.VANILLA));
         put("xaero:style_minimap_cfhgui", (parent) -> style(parent, "xaero:minimap", Style.CFHGUI));
      }

      put("xaero:style_follow", (parent) -> style(parent, (String)null, (Style)null));
      put("xaero:global_vanilla", (parent) -> global(parent, Style.VANILLA));
      put("xaero:global_cfhgui", (parent) -> global(parent, Style.CFHGUI));
      put("xaero:translucent_on", (parent) -> translucent(parent, true));
      put("xaero:translucent_off", (parent) -> translucent(parent, false));
      put("xaero:accent_blue", (parent) -> accent(parent, -10576935));
      put("xaero:accent_default", (parent) -> accent(parent, -7726052));
      put("xaero:open_dropdown", (parent) -> later(parent, () -> XaeroHarness.LibraryWidgets.openDropdown(false)));
      put("xaero:open_dropdown_lowest", (parent) -> later(parent, () -> XaeroHarness.LibraryWidgets.openDropdown(true)));
      put("xaero:scroll_end", (parent) -> later(parent, XaeroHarness::scrollToEnd));
      CFHGuiEvents.CLIENT_TICK_END.register(XaeroHarness::tick);
      CFHGui.LOGGER.info("[CFHGui] Xaero's screens are registered for the harness");

      try {
         CFHGui.LOGGER.info("[CFHGui] harness: of what the restyle holds on to in Xaero's classes, missing here: {}", XaeroPreflight.missing(minimap, worldMap));
      } catch (Throwable t) {
         CFHGui.LOGGER.warn("[CFHGui] harness: Xaero's classes could not be looked through: {}", t.toString());
      }

   }

   private static Screen accent(Screen parent, int argb) {
      CFHGui.config().accent = Colors.toHex(argb);
      Theme.reload();
      return parent;
   }

   private static Screen style(Screen parent, String slot, Style style) {
      CoreConfig config = CFHGui.config();
      if (slot == null) {
         if (config.screens != null) {
            config.screens.remove("xaero:minimap");
            config.screens.remove("xaero:world_map");
         }
      } else {
         config.overrideOrCreate(slot).style = style.name();
      }

      MenuSlots.refresh();
      return parent;
   }

   private static Screen global(Screen parent, Style style) {
      CFHGui.config().setStyle(style);
      MenuSlots.refresh();
      Reskin.invalidate();
      return parent;
   }

   private static Screen translucent(Screen parent, boolean on) {
      CFHGui.config().translucentPanels = on;
      MenuSlots.refresh();
      return parent;
   }

   private static Screen later(Screen parent, Runnable action) {
      later = action;
      laterIn = 3;
      return parent;
   }

   private static void tick() {
      if (later != null && --laterIn <= 0) {
         Runnable action = later;
         later = null;

         try {
            action.run();
         } catch (Throwable t) {
            CFHGui.LOGGER.warn("[CFHGui] harness: {}", t.toString());
         }

      }
   }

   private static void scrollToEnd() {
      Screen screen = Minecraft.getInstance().screen;
      if (screen != null) {
         scrollToEnd(screen.children());
      }

   }

   private static void scrollToEnd(List<? extends GuiEventListener> children) {
      for(GuiEventListener child : children) {
         if (child instanceof CFHGuiScrollPanel panel) {
            panel.scrollTo(Double.MAX_VALUE);
         }

         if (child instanceof ContainerEventHandler container) {
            scrollToEnd(container.children());
         }
      }

   }

   private static void put(String id, Function<Screen, Screen> factory) {
      CoreActions.SCREEN_FACTORIES.put(id, (Function<Screen, Screen>)(parent) -> {
         try {
            return factory.apply(parent);
         } catch (Throwable t) {
            CFHGui.LOGGER.warn("[CFHGui] harness: {} could not be opened: {}", id, t.toString());
            return parent;
         }
      });
   }

   private static final class LibraryWidgets {
      static void openDropdown(boolean lowest) {
         Screen screen = Minecraft.getInstance().screen;
         if (screen != null) {
            DropDownWidget found = null;

            for(GuiEventListener child : screen.children()) {
               if (child instanceof DropDownWidget) {
                  DropDownWidget dropdown = (DropDownWidget)child;
                  if (found == null || lowest && dropdown.getY() > found.getY()) {
                     found = dropdown;
                  }

                  if (!lowest) {
                     break;
                  }
               }
            }

            if (found != null) {
               found.setClosed(false);
            }

         }
      }
   }

   private static final class WorldMapScreens {
      static Screen map(Screen parent) {
         Minecraft mc = Minecraft.getInstance();
         WorldMapSession session = WorldMapSession.getCurrentSession();
         Entity camera = mc.getCameraEntity();
         return (Screen)(session != null && camera != null ? new GuiMap((Screen)null, (Screen)null, session.getMapProcessor(), camera) : parent);
      }

      static Screen settings(Screen parent) {
         return new GuiWorldMapSettings(parent, BuiltInEditConfigScreenContexts.CLIENT);
      }

      static Screen message(Screen parent) {
         WorldMapSession session = WorldMapSession.getCurrentSession();
         if (session == null) {
            return parent;
         } else {
            session.getMapProcessor().getMessageBox().addMessage(Component.literal("The map was exported as a picture"));
            session.getMapProcessor().getMessageBox().addMessage(Component.literal("Cave mode is off"));
            return parent;
         }
      }
   }

   private static final class MinimapScreens {
      private static boolean sampled;

      static Screen settings(Screen parent) {
         return new GuiMinimapMain(HudMod.INSTANCE, parent, (Screen)null, true, BuiltInEditConfigScreenContexts.CLIENT);
      }

      static Screen waypoints(Screen parent) {
         MinimapSession session = (MinimapSession)BuiltInHudModules.MINIMAP.getCurrentSession();
         return (Screen)(session == null ? parent : new GuiWaypoints(HudMod.INSTANCE, session, parent, (Screen)null));
      }

      static Screen addWaypoint(Screen parent) {
         MinimapSession session = (MinimapSession)BuiltInHudModules.MINIMAP.getCurrentSession();
         return (Screen)(session == null ? parent : new GuiAddWaypoint(HudMod.INSTANCE, session, parent, Lists.newArrayList(), session.getWorldState().getCurrentWorldPath().getRoot(), session.getWorldManager().getCurrentWorld(), true));
      }

      static Screen option(Screen parent, int which, int value) {
         ConfigProfile profile = HudMod.INSTANCE.getHudConfigs().getClientConfigManager().getCurrentProfile();
         switch (which) {
            case 0 -> profile.set(MinimapProfiledConfigOptions.SHAPE, value);
            case 1 -> profile.set(MinimapProfiledConfigOptions.NORTH_LOCKED, value != 0);
            default -> profile.set(MinimapProfiledConfigOptions.COMPASS_LOCATION, value);
         }

         return parent;
      }

      static Screen sampleWaypoints(Screen parent) {
         Minecraft mc = Minecraft.getInstance();
         MinimapSession session = (MinimapSession)BuiltInHudModules.MINIMAP.getCurrentSession();
         if (session != null && mc.player != null && !sampled) {
            WaypointSet set = session.getWorldManager().getCurrentWorld().getCurrentWaypointSet();
            if (set == null) {
               return parent;
            } else {
               sampled = true;
               if (!set.isEmpty()) {
                  return parent;
               } else {
                  int x = mc.player.getBlockX();
                  int y = mc.player.getBlockY();
                  int z = mc.player.getBlockZ();
                  set.add(new Waypoint(x + 6, y + 1, z + 18, "Home", "H", WaypointColor.GREEN));
                  set.add(new Waypoint(x - 14, y + 4, z + 26, "Mine", "M", WaypointColor.GOLD));
                  set.add(new Waypoint(x + 30, y + 8, z + 44, "Village", "V", WaypointColor.AQUA));
                  return parent;
               }
            }
         } else {
            return parent;
         }
      }
   }
}
