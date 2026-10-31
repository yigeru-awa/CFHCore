package com.onehumanawa.cfhcore.content.gui.menu.client.infohud;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

final class MemoryProbe {
   private static final long RATE_EVERY_MS = 500L;
   private static final float PRESSURE_STEP = 0.06F;
   private static List<GarbageCollectorMXBean> collectors;
   private static boolean collectorsAsked;
   private static long used;
   private static long allocated;
   private static long max;
   private static long rate = -1L;
   private static float pressure = -1.0F;
   private static long rateAtMs;
   private static long rateUsed;
   private static long rateCollections = -1L;

   private MemoryProbe() {
   }

   static void refresh(long nowMs) {
      Runtime runtime = Runtime.getRuntime();
      allocated = runtime.totalMemory();
      used = allocated - runtime.freeMemory();
      long limit = runtime.maxMemory();
      max = limit > 0L && limit != Long.MAX_VALUE ? limit : allocated;
      float fraction = max > 0L ? (float)((double)used / (double)max) : 0.0F;
      pressure = pressure < 0.0F ? fraction : pressure + (fraction - pressure) * 0.06F;
      if (nowMs - rateAtMs >= 500L) {
         long collections = collections();
         if (rateAtMs != 0L && collections >= 0L && collections == rateCollections) {
            long grown = used - rateUsed;
            if (grown >= 0L) {
               rate = Math.round((double)grown * (double)1000.0F / (double)(nowMs - rateAtMs));
            }
         }

         rateAtMs = nowMs;
         rateUsed = used;
         rateCollections = collections;
      }

   }

   private static long collections() {
      if (!collectorsAsked) {
         collectorsAsked = true;

         try {
            collectors = ManagementFactory.getGarbageCollectorMXBeans();
         } catch (Throwable var6) {
            collectors = null;
         }
      }

      List<GarbageCollectorMXBean> beans = collectors;
      if (beans == null) {
         return -1L;
      } else {
         long total = 0L;

         for(int i = 0; i < beans.size(); ++i) {
            long n = ((GarbageCollectorMXBean)beans.get(i)).getCollectionCount();
            if (n > 0L) {
               total += n;
            }
         }

         return total;
      }
   }

   static long used() {
      return used;
   }

   static long allocated() {
      return allocated;
   }

   static long max() {
      return max;
   }

   static long rate() {
      return rate;
   }

   static float pressure() {
      return Math.max(0.0F, pressure);
   }
}
