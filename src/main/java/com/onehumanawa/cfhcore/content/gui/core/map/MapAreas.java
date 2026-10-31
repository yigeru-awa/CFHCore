package com.onehumanawa.cfhcore.content.gui.core.map;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class MapAreas {
   private static final List<Provider> PROVIDERS = new CopyOnWriteArrayList();

   public static void register(Provider provider) {
      PROVIDERS.add(provider);
   }

   public static boolean any() {
      return !PROVIDERS.isEmpty();
   }

   public static @Nullable Area at(String dimension, int chunkX, int chunkZ) {
      for(Provider p : PROVIDERS) {
         Area a = p.at(dimension, chunkX, chunkZ);
         if (a != null) {
            return a;
         }
      }

      return null;
   }

   public static long revision(String dimension) {
      long r = 0L;

      for(Provider p : PROVIDERS) {
         r = r * 31L + p.revision(dimension);
      }

      return r;
   }

   private MapAreas() {
   }

   public static record Area(String owner, Component name, int rgb, int fillAlpha, int borderAlpha) {
   }

   public interface Provider {
      String id();

      @Nullable Area at(String var1, int var2, int var3);

      long revision(String var1);
   }
}
