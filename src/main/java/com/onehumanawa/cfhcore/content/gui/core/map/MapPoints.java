package com.onehumanawa.cfhcore.content.gui.core.map;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.network.chat.Component;

public final class MapPoints {
   private static final List<Provider> PROVIDERS = new CopyOnWriteArrayList();
   private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList();
   private static volatile int revision;

   public static void register(Provider provider) {
      PROVIDERS.add(provider);
      changed();
   }

   public static List<Provider> providers() {
      return List.copyOf(PROVIDERS);
   }

   public static void changed() {
      ++revision;

      for(Runnable r : LISTENERS) {
         try {
            r.run();
         } catch (RuntimeException e) {
            CFHGui.LOGGER.warn("[CFHGui] map point listener threw: {}", e.toString());
         }
      }

   }

   public static int revision() {
      return revision;
   }

   public static void onChange(Runnable listener) {
      LISTENERS.add(listener);
   }

   public static List<MapPoint> all() {
      List<MapPoint> out = new ArrayList();

      for(Provider p : PROVIDERS) {
         try {
            out.addAll(p.points());
         } catch (RuntimeException e) {
            CFHGui.LOGGER.warn("[CFHGui] map points of {} threw: {}", p.id(), e.toString());
         }
      }

      return out;
   }

   public static List<MapPoint> in(String dimension) {
      List<MapPoint> out = new ArrayList();

      for(MapPoint p : all()) {
         if (p.dimension().equals(dimension)) {
            out.add(p);
         }
      }

      return out;
   }

   public static List<MapAction> actions(MapPoint point) {
      for(Provider p : PROVIDERS) {
         try {
            for(MapPoint q : p.points()) {
               if (q.id().equals(point.id())) {
                  return p.actions(point);
               }
            }
         } catch (RuntimeException e) {
            CFHGui.LOGGER.warn("[CFHGui] map actions of {} threw: {}", p.id(), e.toString());
         }
      }

      return List.of();
   }

   public static Component groupName(String group) {
      for(Provider p : PROVIDERS) {
         try {
            for(MapPoint q : p.points()) {
               if (q.group().equals(group)) {
                  return p.groupName(group);
               }
            }
         } catch (RuntimeException var5) {
         }
      }

      return Component.literal(group);
   }

   private MapPoints() {
   }

   public interface Provider {
      String id();

      List<MapPoint> points();

      default List<MapAction> actions(MapPoint point) {
         return List.of();
      }

      default Component groupName(String group) {
         return Component.literal(group);
      }
   }
}
