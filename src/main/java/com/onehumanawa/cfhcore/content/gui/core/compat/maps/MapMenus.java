package com.onehumanawa.cfhcore.content.gui.core.compat.maps;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.map.MapAction;
import com.onehumanawa.cfhcore.content.gui.core.map.MapSpots;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToggle;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import org.jetbrains.annotations.Nullable;

public final class MapMenus {
   public static MapSpots.Spot spot(String dimension, int x, int y, int z) {
      if (y != Integer.MIN_VALUE) {
         return new MapSpots.Spot(dimension, x, y, z);
      } else {
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null && level.dimension().location().toString().equals(dimension) && level.hasChunk(x >> 4, z >> 4) && !level.dimensionType().hasCeiling()) {
            int top = level.getHeight(Types.MOTION_BLOCKING, x, z);
            if (top > level.getMinBuildHeight()) {
               return new MapSpots.Spot(dimension, x, top, z);
            }
         }

         return new MapSpots.Spot(dimension, x, Integer.MIN_VALUE, z);
      }
   }

   public static List<MapAction> at(MapSpots.Spot spot, MapSpots.@Nullable Area area) {
      return !MapSpots.any() ? List.of() : MapSpots.at(spot, area != null && area.chunks() > 1 ? area : null);
   }

   public static void run(MapAction action) {
      if (!action.enabled()) {
         if (action.tip() != null) {
            CFHGuiToasts.show(action.label(), action.tip(), Icon.INFO);
         }

      } else {
         try {
            action.run().run();
         } catch (RuntimeException e) {
            CFHGui.LOGGER.warn("[CFHGui] the map action {} threw: {}", action.id(), e.toString());
         }

      }
   }

   public static String plain(MapAction action) {
      String label = action.label().getString();
      return action.enabled() ? label : Component.translatable("cfhgui.maps.menu.unavailable", new Object[]{label}).getString();
   }

   public static List<MapAction> hideActions(MapPins.Pin pin) {
      String id = pin.id;
      String group = pin.group();
      return List.of(MapAction.of("hide", Component.translatable("cfhgui.maps.hide.this"), Icon.EYE_OFF, () -> MapPins.setPlaceShown(id, false)), MapAction.of("hide_group", Component.translatable("cfhgui.maps.hide.group", new Object[]{MapPins.groupName(group)}), Icon.EYE_OFF, () -> MapPins.setShown(group, false)));
   }

   public static Component hereLabel() {
      return Component.translatable("cfhgui.maps.here");
   }

   public static Component placesLabel() {
      return Component.translatable("cfhgui.maps.places");
   }

   public static void places() {
      Minecraft mc = Minecraft.getInstance();
      int x = (int)(mc.mouseHandler.xpos() * (double)mc.getWindow().getGuiScaledWidth() / (double)Math.max(1, mc.getWindow().getScreenWidth()));
      int y = (int)(mc.mouseHandler.ypos() * (double)mc.getWindow().getGuiScaledHeight() / (double)Math.max(1, mc.getWindow().getScreenHeight()));
      places(x, y, -1);
   }

   private static void places(int x, int y, int highlight) {
      List<String> groups = MapPins.groups();
      if (!groups.isEmpty()) {
         List<MenuPopup.Item> items = new ArrayList();

         for(int i = 0; i < groups.size(); ++i) {
            String group = (String)groups.get(i);
            boolean shown = MapPins.shown(group);
            int at = i;
            items.add(new MenuPopup.Item(MapPins.groupName(group), MapPins.groupIcon(group), true, shown, false, false, () -> {
               MapPins.setShown(group, !shown);
               places(x, y, at);
            }));
         }

         List<MapPins.Hidden> singles = MapPins.hiddenPlaces();
         if (!singles.isEmpty()) {
            items.add(MenuPopup.Item.sep());

            for(MapPins.Hidden one : singles) {
               items.add(new MenuPopup.Item(Component.translatable("cfhgui.maps.places.show_one", new Object[]{one.name()}), Icon.EYE, true, false, false, false, () -> {
                  MapPins.setPlaceShown(one.id(), true);
                  places(x, y, -1);
               }));
            }
         }

         if (MapsCompat.drawsInWorld()) {
            boolean on = MapPins.inWorld();
            int row = items.size() + 1;
            items.add(MenuPopup.Item.sep());
            items.add(new MenuPopup.Item(Component.translatable("cfhgui.maps.places.in_world"), Icon.EYE, true, on, false, false, () -> {
               MapPins.setInWorld(!on);
               places(x, y, row);
            }));
         }

         Popups.open((new MenuPopup(x, y, items, 110)).highlight(highlight));
      }
   }

   public static Component settingsTitle() {
      return Component.translatable("cfhgui.maps.settings");
   }

   public static List<AbstractWidget> settingRows(int width) {
      if (MapsCompat.attached() && MapPins.any()) {
         List<AbstractWidget> rows = new ArrayList();
         rows.add((new CFHGuiLabel(0, 0, width, Component.translatable("cfhgui.maps.settings.hint"))).style(CFHGuiLabel.Style.MUTED).wrap(true));

         for(String group : MapPins.groups()) {
            rows.add(new CFHGuiToggle(0, 0, width, MapPins.groupName(group), MapPins.shown(group), (shown) -> MapPins.setShown(group, shown)));
         }

         List<MapPins.Hidden> singles = MapPins.hiddenPlaces();
         if (!singles.isEmpty()) {
            rows.add((new CFHGuiLabel(0, 0, width, Component.translatable("cfhgui.maps.settings.hidden"))).style(CFHGuiLabel.Style.MUTED).wrap(true));

            for(MapPins.Hidden one : singles) {
               rows.add(new CFHGuiToggle(0, 0, width, Component.literal(one.name()), false, (shown) -> MapPins.setPlaceShown(one.id(), shown)));
            }
         }

         if (MapsCompat.drawsInWorld()) {
            rows.add((new CFHGuiToggle(0, 0, width, Component.translatable("cfhgui.maps.places.in_world"), MapPins.inWorld(), MapPins::setInWorld)).tip(Component.translatable("cfhgui.maps.places.in_world.tip")));
         }

         return rows;
      } else {
         return List.of();
      }
   }

   private MapMenus() {
   }
}
