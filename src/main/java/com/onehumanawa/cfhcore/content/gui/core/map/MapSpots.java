package com.onehumanawa.cfhcore.content.gui.core.map;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jetbrains.annotations.Nullable;

public final class MapSpots {
   public static final int NO_Y = Integer.MIN_VALUE;
   private static final List<Provider> PROVIDERS = new CopyOnWriteArrayList();

   public static void register(Provider provider) {
      PROVIDERS.add(provider);
   }

   public static boolean any() {
      return !PROVIDERS.isEmpty();
   }

   public static List<MapAction> in(Area area) {
      List<MapAction> out = new ArrayList();

      for(Provider p : PROVIDERS) {
         try {
            out.addAll(p.in(area));
         } catch (RuntimeException e) {
            CFHGui.LOGGER.warn("[CFHGui] map selection actions of {} threw: {}", p.id(), e.toString());
         }
      }

      return out;
   }

   public static List<MapAction> at(Spot spot) {
      List<MapAction> out = new ArrayList();

      for(Provider p : PROVIDERS) {
         try {
            out.addAll(p.at(spot));
         } catch (RuntimeException e) {
            CFHGui.LOGGER.warn("[CFHGui] map spot actions of {} threw: {}", p.id(), e.toString());
         }
      }

      return out;
   }

   public static List<MapAction> at(Spot spot, @Nullable Area area) {
      List<MapAction> out = new ArrayList();
      Map<Provider, Set<String>> marked = new IdentityHashMap();
      if (area != null) {
         for(Provider p : PROVIDERS) {
            try {
               List<MapAction> offered = p.in(area);
               if (!offered.isEmpty()) {
                  Set<String> ids = new HashSet();

                  for(MapAction a : offered) {
                     ids.add(a.id());
                  }

                  out.addAll(offered);
                  marked.put(p, ids);
               }
            } catch (RuntimeException e) {
               CFHGui.LOGGER.warn("[CFHGui] map selection actions of {} threw: {}", p.id(), e.toString());
            }
         }
      }

      for(Provider p : PROVIDERS) {
         try {
            Set<String> ids = (Set)marked.get(p);

            for(MapAction a : p.at(spot)) {
               if (ids == null || !ids.contains(a.id())) {
                  out.add(a);
               }
            }
         } catch (RuntimeException e) {
            CFHGui.LOGGER.warn("[CFHGui] map spot actions of {} threw: {}", p.id(), e.toString());
         }
      }

      return out;
   }

   private MapSpots() {
   }

   public static record Spot(String dimension, int x, int y, int z) {
      public boolean hasY() {
         return this.y != Integer.MIN_VALUE;
      }

      public int chunkX() {
         return this.x >> 4;
      }

      public int chunkZ() {
         return this.z >> 4;
      }
   }

   public static record Area(String dimension, int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
      public int chunks() {
         return (this.maxChunkX - this.minChunkX + 1) * (this.maxChunkZ - this.minChunkZ + 1);
      }
   }

   public interface Provider {
      String id();

      List<MapAction> at(Spot var1);

      default List<MapAction> in(Area area) {
         return List.of();
      }
   }
}
