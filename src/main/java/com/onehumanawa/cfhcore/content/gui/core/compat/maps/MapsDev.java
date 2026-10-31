package com.onehumanawa.cfhcore.content.gui.core.compat.maps;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.event.Event;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.map.MapAction;
import com.onehumanawa.cfhcore.content.gui.core.map.MapAreas;
import com.onehumanawa.cfhcore.content.gui.core.map.MapPoint;
import com.onehumanawa.cfhcore.content.gui.core.map.MapPoints;
import com.onehumanawa.cfhcore.content.gui.core.map.MapSpots;
import com.onehumanawa.cfhcore.content.gui.core.mixin.MouseHandlerAccessor;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiModal;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class MapsDev {
   private static final String SWITCH = "cfhgui.maps.devPlaces";
   private static final int STEP = 4;
   private static final ArrayDeque<Runnable> STEPS = new ArrayDeque();
   private static int nextStepIn;
   private static final List<String> SPOT_ACTIONS = List.of("claim", "unclaim", "force_load", "team_home", "team_waypoint", "team_ping", "dev_home", "dev_warp", "dev_teleport");
   private static final List<String> PIN_ACTIONS = List.of("home:base/rename", "home:base/delete", "home:mine/teleport", "warp:arena/teleport", "team:home/teleport", "team:home/delete");

   static void init() {
      String mode = System.getProperty("cfhgui.maps.devPlaces", "");
      boolean places = "true".equalsIgnoreCase(mode);
      if (places || "harness".equalsIgnoreCase(mode)) {
         Places made = new Places();
         if (places) {
            MapPoints.register(made);
            MapSpots.register(made);
            CFHGuiEvents.CLIENT_LEFT_SERVER.register(made::clear);
         }

         CFHGuiEvents.CLIENT_TICK_END.register((Runnable)() -> {
            if (places) {
               made.tick();
            }

            if (!STEPS.isEmpty() && --nextStepIn <= 0) {
               nextStepIn = 4;

               try {
                  ((Runnable)STEPS.poll()).run();
               } catch (Throwable t) {
                  STEPS.clear();
                  CFHGui.LOGGER.warn("[CFHGui] maps harness: {}", t.toString());
               }

            }
         });
         harness(made);
         CFHGui.LOGGER.info("[CFHGui] the maps harness is registered{} ({}={})", new Object[]{places ? ", and made-up places stand in for a module's" : "", "cfhgui.maps.devPlaces", mode});
      }
   }

   private static void harness(Places places) {
      put("cfhgui_maps:xaero_map", (p) -> map("xaeroworldmap", p));
      put("cfhgui_maps:journeymap_map", (p) -> map("journeymap", p));
      put("cfhgui_maps:zoom_in", (p) -> {
         if (p != null) {
            p.getClass().getMethod("zoomIn").invoke(p);
         }

         return p;
      });
      put("cfhgui_maps:places_menu", (p) -> later(p, MapMenus::places));
      put("cfhgui_maps:hide_warps", (p) -> {
         MapPins.setShown("warps", false);
         return p;
      });
      put("cfhgui_maps:show_all", (p) -> {
         for(String group : MapPins.groups()) {
            MapPins.setShown(group, true);
         }

         return p;
      });
      put("cfhgui_maps:rename_base", (p) -> {
         places.rename("home:base", word("place.camp"));
         return p;
      });
      put("cfhgui_maps:add_camp", (p) -> {
         places.addNear("home:lookout", "homes", word("place.lookout"), -26, 4, -18, 14254174);
         return p;
      });
      put("cfhgui_maps:state", (p) -> {
         CFHGui.LOGGER.info("[CFHGui] maps: {}", MapsCompat.describe());
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.level != null) {
            String dimension = mc.level.dimension().location().toString();
            int x = mc.player.chunkPosition().x;
            int z = mc.player.chunkPosition().z;
            CFHGui.LOGGER.info("[CFHGui] maps: land at chunk {}, {} is {} (revision {})", new Object[]{x, z, MapAreas.at(dimension, x, z), MapAreas.revision(dimension)});
         }

         return p;
      });

      for(String action : SPOT_ACTIONS) {
         put("cfhgui_maps:spot/" + action, (p) -> later(p, () -> spot(action)));
      }

      for(String pin : PIN_ACTIONS) {
         put("cfhgui_maps:pin/" + pin, (p) -> later(p, () -> pin(pin)));
      }

      put("cfhgui_maps:popup_type", (p) -> {
         for(char c : word("place.camp").toCharArray()) {
            Popups.charTyped(c, 0);
         }

         return p;
      });
      put("cfhgui_maps:popup_enter", (p) -> {
         Popups.keyPressed(257, 0, 0);
         return p;
      });
      put("cfhgui_maps:select", (p) -> later(p, () -> pointer(0.38, 0.36), () -> press(1, true), () -> pointer(0.585, 0.64), () -> press(1, false)));
      put("cfhgui_maps:popup_here", (p) -> later(p, () -> pointer(0.3, 0.3), () -> {
            press(1, true);
            press(1, false);
         }, () -> popupRow(0.3, 0.3, 5), MapsDev::moved));
      put("cfhgui_maps:popup_places", (p) -> later(p, () -> pointer(0.3, 0.3), () -> {
            press(1, true);
            press(1, false);
         }, () -> popupRow(0.3, 0.3, 6), MapsDev::moved));
      put("cfhgui_maps:popup_pin_second", (p) -> later(p, () -> pointer((double)0.5F, 0.47), () -> {
            press(1, true);
            press(1, false);
         }, () -> popupPinRow((double)0.5F, 0.47, 2), MapsDev::moved, () -> {
            press(0, true);
            press(0, false);
         }));
   }

   private static void popupPinRow(double x, double y, int row) {
      Minecraft mc = Minecraft.getInstance();
      int unit = mc.getWindow().getScreenHeight() >= 1000 ? 3 : 2;
      MouseHandlerAccessor mouse = (MouseHandlerAccessor)mc.mouseHandler;
      mouse.cfhgui$setXpos(x * (double)mc.getWindow().getScreenWidth() + (double)(unit * 27));
      mouse.cfhgui$setYpos(y * (double)mc.getWindow().getScreenHeight() + (double)unit * (8.3 + 12.3 * (double)(row - 1)));
   }

   private static void popupRow(double x, double y, int row) {
      Minecraft mc = Minecraft.getInstance();
      int unit = mc.getWindow().getScreenHeight() >= 1000 ? 3 : 2;
      MouseHandlerAccessor mouse = (MouseHandlerAccessor)mc.mouseHandler;
      mouse.cfhgui$setXpos(x * (double)mc.getWindow().getScreenWidth() + (double)(unit * 40));
      mouse.cfhgui$setYpos(y * (double)mc.getWindow().getScreenHeight() + (double)(unit * (9 + 12 * (row - 1))));
   }

   private static void moved() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.screen != null) {
         mc.screen.mouseMoved(mc.mouseHandler.xpos() * (double)mc.getWindow().getGuiScaledWidth() / (double)Math.max(1, mc.getWindow().getScreenWidth()), mc.mouseHandler.ypos() * (double)mc.getWindow().getGuiScaledHeight() / (double)Math.max(1, mc.getWindow().getScreenHeight()));
      }
   }

   private static void pointer(double x, double y) {
      Minecraft mc = Minecraft.getInstance();
      MouseHandlerAccessor mouse = (MouseHandlerAccessor)mc.mouseHandler;
      mouse.cfhgui$setXpos(x * (double)mc.getWindow().getScreenWidth());
      mouse.cfhgui$setYpos(y * (double)mc.getWindow().getScreenHeight());
   }

   private static void press(int button, boolean down) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.screen != null) {
         double x = mc.mouseHandler.xpos() * (double)mc.getWindow().getGuiScaledWidth() / (double)Math.max(1, mc.getWindow().getScreenWidth());
         double y = mc.mouseHandler.ypos() * (double)mc.getWindow().getGuiScaledHeight() / (double)Math.max(1, mc.getWindow().getScreenHeight());
         if (down) {
            mc.screen.mouseClicked(x, y, button);
         } else {
            mc.screen.mouseReleased(x, y, button);
         }

      }
   }

   private static String word(String key) {
      return Component.translatable("cfhgui.maps.dev." + key).getString();
   }

   private static void put(String id, Entry factory) {
      CoreActions.SCREEN_FACTORIES.put(id, (Function<Screen, Screen>)(parent) -> {
         try {
            return factory.open(parent);
         } catch (Throwable t) {
            CFHGui.LOGGER.warn("[CFHGui] maps harness: {} failed: {}", id, t.toString());
            return parent;
         }
      });
   }

   private static Screen later(Screen parent, Runnable... steps) {
      STEPS.clear();
      STEPS.addAll(List.of(steps));
      nextStepIn = 3;
      return parent;
   }

   private static Screen map(String id, Screen parent) {
      PlaceBridge bridge = MapsCompat.bridge(id);
      Screen map = bridge == null ? null : bridge.mapScreen();
      return map != null ? map : parent;
   }

   private static void spot(String action) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         MapSpots.Spot spot = MapMenus.spot(mc.level.dimension().location().toString(), mc.player.getBlockX(), Integer.MIN_VALUE, mc.player.getBlockZ());

         for(MapAction a : MapMenus.at(spot, (MapSpots.Area)null)) {
            if (a.id().equals(action)) {
               CFHGui.LOGGER.info("[CFHGui] maps harness: {} at {} ({})", new Object[]{action, spot, a.enabled() ? "runs" : "greyed out"});
               MapMenus.run(a);
               return;
            }
         }

         CFHGui.LOGGER.info("[CFHGui] maps harness: nothing offers {} at {}", action, spot);
      }
   }

   private static void pin(String named) {
      int slash = named.lastIndexOf(47);
      MapPins.Pin pin = MapPins.byId(named.substring(0, slash));
      if (pin == null) {
         CFHGui.LOGGER.info("[CFHGui] maps harness: no pin {}", named.substring(0, slash));
      } else {
         for(MapAction a : MapPoints.actions(pin.point())) {
            if (a.id().equals(named.substring(slash + 1))) {
               CFHGui.LOGGER.info("[CFHGui] maps harness: {} ({})", named, a.enabled() ? "runs" : "greyed out");
               MapMenus.run(a);
               return;
            }
         }

         CFHGui.LOGGER.info("[CFHGui] maps harness: {} has no such action", named);
      }
   }

   private MapsDev() {
   }

   private static final class Places implements MapPoints.Provider, MapSpots.Provider {
      private List<MapPoint> points = List.of();
      private boolean made;
      private int nextName = 1;

      public String id() {
         return "cfhgui_dev";
      }

      public List<MapPoint> points() {
         return this.points;
      }

      void tick() {
         Minecraft mc = Minecraft.getInstance();
         LocalPlayer player = mc.player;
         if (!this.made && player != null && mc.level != null) {
            this.made = true;
            String here = mc.level.dimension().location().toString();
            int x = player.getBlockX();
            int y = player.getBlockY();
            int z = player.getBlockZ();
            List<MapPoint> out = new ArrayList();
            out.add(point("home:base", "homes", Component.translatable("cfhgui.maps.dev.place.base"), here, x, y, z, 6080680, (Icon)null));
            out.add(point("home:mine", "homes", Component.translatable("cfhgui.maps.dev.place.mine"), here, x - 34, y - 18, z + 22, 14722136, Icon.BLOCK));
            out.add(point("warp:market", "warps", Component.translatable("cfhgui.maps.dev.place.market"), here, x + 48, y, z - 30, 6007008, (Icon)null));
            out.add(point("warp:arena", "warps", Component.translatable("cfhgui.maps.dev.place.arena"), here, x - 60, y + 6, z - 44, 11894492, Icon.SWORD));
            out.add(point("spawn:spawn", "spawn", Component.translatable("cfhgui.maps.dev.place.spawn"), here, x + 72, y, z + 16, 15262936, (Icon)null));
            out.add(point("death:last", "deaths", Component.translatable("cfhgui.maps.dev.place.death"), here, x + 20, y, z + 38, 15026253, (Icon)null));
            out.add(point("home:nether", "homes", Component.translatable("cfhgui.maps.dev.place.nether"), "minecraft:the_nether", x / 8 + 6, 70, z / 8 - 4, 14254174, (Icon)null));
            this.set(out);
         }
      }

      void clear() {
         this.made = false;
         if (!this.points.isEmpty()) {
            this.set(List.of());
         }

      }

      private void set(List<MapPoint> next) {
         this.points = List.copyOf(next);
         MapPoints.changed();
      }

      private static MapPoint point(String id, String group, Component label, String dimension, int x, int y, int z, int rgb, @Nullable Icon icon) {
         return new MapPoint(id, group, label, dimension, (double)x + (double)0.5F, (double)y, (double)z + (double)0.5F, rgb, icon, (UUID)null);
      }

      public List<MapAction> actions(MapPoint point) {
         List<MapAction> out = new ArrayList();
         MapAction teleport = MapAction.of("teleport", Component.translatable("cfhgui.maps.dev.teleport"), Icon.SEND, () -> teleport(point.dimension(), point.x(), point.y(), point.z()));
         out.add("warp:arena".equals(point.id()) ? teleport.disabled(Component.translatable("cfhgui.maps.dev.closed")) : teleport);
         if ("homes".equals(point.group()) || "warps".equals(point.group())) {
            out.add(MapAction.of("rename", Component.translatable("cfhgui.maps.dev.rename"), Icon.EDIT, () -> CFHGuiModal.prompt(Component.translatable("cfhgui.maps.dev.rename.title"), (Component)null, point.label().getString(), (name) -> this.rename(point.id(), name))));
         }

         if (!"spawn".equals(point.group())) {
            out.add(MapAction.danger("delete", Component.translatable("cfhgui.maps.dev.delete"), Icon.TRASH, () -> CFHGuiModal.confirmDanger(Component.translatable("cfhgui.maps.dev.delete.title"), Component.translatable("cfhgui.maps.dev.delete.body", new Object[]{point.label()}), Component.translatable("cfhgui.maps.dev.delete"), () -> this.remove(point.id()))));
         }

         return out;
      }

      void rename(String id, String name) {
         String to = name == null ? "" : name.trim();
         if (!to.isEmpty()) {
            List<MapPoint> out = new ArrayList(this.points);

            for(int i = 0; i < out.size(); ++i) {
               MapPoint p = (MapPoint)out.get(i);
               if (p.id().equals(id)) {
                  out.set(i, new MapPoint(p.id(), p.group(), Component.literal(to), p.dimension(), p.x(), p.y(), p.z(), p.rgb(), p.icon(), p.owner()));
               }
            }

            this.set(out);
         }
      }

      private void remove(String id) {
         List<MapPoint> out = new ArrayList(this.points);
         out.removeIf((p) -> p.id().equals(id));
         this.set(out);
      }

      void addNear(String id, String group, String name, int dx, int dy, int dz, int rgb) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.level != null) {
            this.add(id, group, name, mc.level.dimension().location().toString(), mc.player.getBlockX() + dx, mc.player.getBlockY() + dy, mc.player.getBlockZ() + dz, rgb);
         }
      }

      private void add(String id, String group, String name, String dimension, int x, int y, int z, int rgb) {
         List<MapPoint> out = new ArrayList(this.points);
         out.removeIf((p) -> p.id().equals(id));
         out.add(point(id, group, Component.literal(name), dimension, x, y, z, rgb, (Icon)null));
         this.set(out);
      }

      private static void teleport(String dimension, double x, double y, double z) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.getConnection() != null) {
            mc.getConnection().sendCommand(String.format(Locale.ROOT, "execute in %s run tp @s %.1f %.1f %.1f", dimension, x, y, z));
            mc.setScreen((Screen)null);
         }
      }

      public List<MapAction> at(MapSpots.Spot spot) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player == null) {
            return List.of();
         } else {
            int y = spot.hasY() ? spot.y() : mc.player.getBlockY();
            List<MapAction> out = new ArrayList();
            out.add(MapAction.of("dev_home", Component.translatable("cfhgui.maps.dev.home_here"), Icon.HOME, () -> CFHGuiModal.prompt(Component.translatable("cfhgui.maps.dev.home_here"), Component.translatable("cfhgui.maps.dev.name"), "", (name) -> {
                  int var10003 = this.nextName++;
                  this.add("home:made" + var10003, "homes", name.isBlank() ? MapsDev.word("place.home") : name.trim(), spot.dimension(), spot.x(), y, spot.z(), 6080680);
               })));
            out.add(MapAction.of("dev_warp", Component.translatable("cfhgui.maps.dev.warp_here"), Icon.COMPASS, () -> CFHGuiModal.prompt(Component.translatable("cfhgui.maps.dev.warp_here"), Component.translatable("cfhgui.maps.dev.name"), "", (name) -> {
                  int var10003 = this.nextName++;
                  this.add("warp:made" + var10003, "warps", name.isBlank() ? MapsDev.word("place.warp") : name.trim(), spot.dimension(), spot.x(), y, spot.z(), 6007008);
               })));
            MapAction teleport = MapAction.of("dev_teleport", Component.translatable("cfhgui.maps.dev.teleport_here"), Icon.SEND, () -> teleport(spot.dimension(), (double)spot.x() + (double)0.5F, (double)y, (double)spot.z() + (double)0.5F));
            out.add(spot.hasY() ? teleport : teleport.disabled(Component.translatable("cfhgui.maps.dev.no_ground")));
            return out;
         }
      }
   }

   @FunctionalInterface
   private interface Entry {
      Screen open(Screen var1) throws Exception;
   }
}
