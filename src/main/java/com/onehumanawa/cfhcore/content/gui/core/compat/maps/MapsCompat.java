package com.onehumanawa.cfhcore.content.gui.core.compat.maps;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.compat.maps.xaero.XaeroMinimapPlaces;
import com.onehumanawa.cfhcore.content.gui.core.compat.maps.xaero.XaeroWorldMapPlaces;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class MapsCompat {
   private static final String[] MINIMAP_MODS = new String[]{"xaerominimap", "xaerominimapfair", "xaerobetterpvp", "xaerobetterpvpfair"};
   private static final List<PlaceBridge> WAITING = new CopyOnWriteArrayList();
   private static final List<PlaceBridge> ATTACHED = new CopyOnWriteArrayList();
   private static boolean listening;
   private static volatile boolean inWorld;
   private static boolean menuBroken;

   public static void init() {
      try {
         CFHGuiPlatform platform = CFHGuiPlatform.get();
         boolean worldMap = platform.isModLoaded("xaeroworldmap");
         boolean minimap = false;

         for(String id : MINIMAP_MODS) {
            minimap |= platform.isModLoaded(id);
         }

         if (worldMap) {
            add("xaeroworldmap", Xaero::worldMap);
         }

         if (minimap) {
            add("xaerominimap", Xaero::minimap);
         }

         if (worldMap || minimap) {
            MapsPreflight.run(worldMap, minimap);
         }
      } catch (RuntimeException e) {
         CFHGui.LOGGER.warn("[CFHGui] could not ask which map mods are installed: {}", e.toString());
      }

      if (!WAITING.isEmpty()) {
         listen();
      }

      MapsDev.init();
   }

   public static void register(PlaceBridge bridge) {
      WAITING.add(bridge);
      listen();
   }

   private static synchronized void listen() {
      if (!listening) {
         listening = true;
         CFHGuiEvents.CLIENT_TICK_END.register(MapsCompat::tick);
      }
   }

   private static void add(String id, Supplier<PlaceBridge> make) {
      try {
         WAITING.add((PlaceBridge)make.get());
      } catch (Throwable t) {
         CFHGui.LOGGER.warn("[CFHGui] {} is installed but not as this version knows it; CFHGui's places are not shown on its map ({})", id, t.toString());
      }

   }

   private static void tick() {
      for(PlaceBridge bridge : WAITING) {
         try {
            if (bridge.attach()) {
               WAITING.remove(bridge);
               ATTACHED.add(bridge);
               if (bridge instanceof WorldPins) {
                  inWorld = true;
               }

               CFHGui.LOGGER.info("[CFHGui] places and map actions are on {}", bridge.id());
            }
         } catch (Throwable t) {
            fail(bridge, t);
         }
      }

      if (!ATTACHED.isEmpty()) {
         if (MapPins.update()) {
            each(PlaceBridge::pinsChanged);
            if (MapPins.pins().isEmpty()) {
               PinImages.clear();
            }
         }

         each(PlaceBridge::tick);
      }
   }

   private static void each(Consumer<PlaceBridge> call) {
      for(PlaceBridge bridge : ATTACHED) {
         try {
            call.accept(bridge);
         } catch (Throwable t) {
            fail(bridge, t);
         }
      }

   }

   private static void fail(PlaceBridge bridge, Throwable t) {
      WAITING.remove(bridge);
      ATTACHED.remove(bridge);
      CFHGui.LOGGER.warn("[CFHGui] {} has changed since this version was made; CFHGui's places are no longer shown on its map ({})", bridge.id(), t.toString());

      try {
         bridge.abandon();
      } catch (Throwable var5) {
      }

      boolean world = false;

      for(PlaceBridge b : ATTACHED) {
         world |= b instanceof WorldPins;
      }

      inWorld = world;
   }

   public static boolean drawsInWorld() {
      return inWorld;
   }

   static boolean attached() {
      return !ATTACHED.isEmpty();
   }

   static @Nullable PlaceBridge bridge(String id) {
      for(PlaceBridge b : ATTACHED) {
         if (b.id().equals(id)) {
            return b;
         }
      }

      for(PlaceBridge b : WAITING) {
         if (b.id().equals(id)) {
            return b;
         }
      }

      return null;
   }

   static String describe() {
      StringBuilder out = new StringBuilder();
      out.append(MapPins.pins().size()).append(" pins in groups ").append(MapPins.groups());

      for(PlaceBridge b : ATTACHED) {
         String holds = b.describe();
         out.append("; ").append(b.id()).append(holds.isEmpty() ? " (draws what there is)" : ": " + holds);
      }

      for(PlaceBridge b : WAITING) {
         out.append("; ").append(b.id()).append(": waiting for its mod");
      }

      return out.toString();
   }

   public static void xaeroMapMenu(Object screen, ArrayList<?> options, int x, int y, int z, @Nullable ResourceKey<Level> dimension, @Nullable int[] selection) {
      if (!menuBroken) {
         PlaceBridge bridge = bridge("xaeroworldmap");
         if (bridge instanceof XaeroMenu) {
            XaeroMenu menu = (XaeroMenu)bridge;

            try {
               menu.menu(screen, options, x, y, z, dimension, selection);
            } catch (Throwable t) {
               menuBroken = true;
               CFHGui.LOGGER.warn("[CFHGui] Xaero's World Map has changed since this version was made; CFHGui adds nothing to its right-click menu ({})", t.toString());
            }

         }
      }
   }

   private MapsCompat() {
   }

   private static final class Xaero {
      static PlaceBridge worldMap() {
         return new XaeroWorldMapPlaces();
      }

      static PlaceBridge minimap() {
         return new XaeroMinimapPlaces();
      }
   }

   public interface WorldPins {
   }

   public interface XaeroMenu {
      void menu(Object var1, ArrayList<?> var2, int var3, int var4, int var5, @Nullable ResourceKey<Level> var6, @Nullable int[] var7);
   }
}
