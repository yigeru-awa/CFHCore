package com.onehumanawa.cfhcore.content.gui.core.compat.maps.xaero;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.compat.maps.MapMenus;
import com.onehumanawa.cfhcore.content.gui.core.compat.maps.MapPins;
import com.onehumanawa.cfhcore.content.gui.core.compat.maps.MapsCompat;
import com.onehumanawa.cfhcore.content.gui.core.compat.maps.PlaceBridge;
import com.onehumanawa.cfhcore.content.gui.core.map.MapAction;
import com.onehumanawa.cfhcore.content.gui.core.map.MapSpots;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import xaero.map.WorldMap;
import xaero.map.WorldMapSession;
import xaero.map.element.MapElementRenderHandler;
import xaero.map.gui.GuiMap;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

public final class XaeroWorldMapPlaces implements PlaceBridge, MapsCompat.XaeroMenu {
   private static final int NO_HEIGHT = 32767;
   private static final String BEFORE = "gui.xaero_right_click_box_map_export";
   private static volatile boolean off;

   static boolean off() {
      return off;
   }

   public String id() {
      return "xaeroworldmap";
   }

   public boolean attach() {
      try {
         MapElementRenderHandler handler = WorldMap.mapElementRenderHandler;
         if (handler == null) {
            return false;
         }

         if (!WorldMapPins.fits()) {
            throw new IllegalStateException("its element renderers ask for more than CFHGui knows");
         }

         handler.add(new WorldMapPins());
      } catch (Throwable t) {
         CFHGui.LOGGER.warn("[CFHGui] Xaero's World Map has changed since this version was made; CFHGui's places are not shown on it ({})", t.toString());
      }

      return true;
   }

   public void menu(Object screen, ArrayList<?> options, int x, int y, int z, @Nullable ResourceKey<Level> dimension, @Nullable int[] selection) {
      if (!off && screen instanceof IRightClickableElement target) {
         boolean places = MapPins.any();
         if (places || MapSpots.any()) {
            String where = dimension != null ? dimension.location().toString() : current();
            if (where != null) {
               MapSpots.Spot spot = MapMenus.spot(where, x, y == 32767 ? Integer.MIN_VALUE : y, z);
               MapSpots.Area area = selection == null ? null : new MapSpots.Area(where, Math.min(selection[0], selection[2]), Math.min(selection[1], selection[3]), Math.max(selection[0], selection[2]), Math.max(selection[1], selection[3]));
               List<MapAction> actions = MapMenus.at(spot, area);
               if (!actions.isEmpty() || places) {
                  ArrayList<RightClickOption> list = (ArrayList<RightClickOption>)(ArrayList<?>)options;
                  int at = options.size();

                  for(int i = 0; i < list.size(); ++i) {
                     ComponentContents var18 = ((RightClickOption)list.get(i)).getDisplayName().getContents();
                     if (var18 instanceof TranslatableContents) {
                        TranslatableContents words = (TranslatableContents)var18;
                        if ("gui.xaero_right_click_box_map_export".equals(words.getKey())) {
                           at = i;
                           break;
                        }
                     }
                  }

                  List<RightClickOption> ours = new ArrayList(actions.size() + 1);

                  for(MapAction action : actions) {
                     ours.add(XaeroOptions.action(action, at + ours.size(), target));
                  }

                  if (places) {
                     ours.add(XaeroOptions.places(at + ours.size(), target));
                  }

                  list.addAll(at, ours);
               }
            }
         }
      }
   }

   private static @Nullable String current() {
      Minecraft mc = Minecraft.getInstance();
      return mc.level == null ? null : mc.level.dimension().location().toString();
   }

   public void abandon() {
      off = true;
   }

   public @Nullable Screen mapScreen() {
      WorldMapSession session = WorldMapSession.getCurrentSession();
      return session != null && session.getMapProcessor() != null ? new GuiMap((Screen)null, (Screen)null, session.getMapProcessor(), Minecraft.getInstance().getCameraEntity()) : null;
   }
}
