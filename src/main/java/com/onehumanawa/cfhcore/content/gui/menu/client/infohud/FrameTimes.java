package com.onehumanawa.cfhcore.content.gui.menu.client.infohud;

import java.util.Arrays;

final class FrameTimes {
   private static final int KEPT = 4096;
   private static final int MASK = 4095;
   private static final long SECOND = 1000000000L;
   private static final long GAP = 1000000000L;
   private static final long LOW_WINDOW = 10000000000L;
   static final int SLICES = 128;
   private static final long SLICE = 100000000L;
   private static final long[] ended = new long[4096];
   private static final long[] took = new long[4096];
   private static final long[] scratch = new long[4096];
   private static int head;
   private static int kept;
   private static long last;
   private static boolean interrupted = true;
   private static final float[] slices = new float[128];
   private static int sliceHead;
   private static long sliceEnds;
   private static float sliceWorst;
   private static int fps = -1;
   private static int low = -1;
   private static float frameMs = -1.0F;

   private FrameTimes() {
   }

   static void frame(long now) {
      long d = now - last;
      last = now;
      if (!interrupted && d > 0L && d <= 1000000000L) {
         head = head + 1 & 4095;
         ended[head] = now;
         took[head] = d;
         if (kept < 4096) {
            ++kept;
         }

         float ms = (float)d / 1000000.0F;
         if (ms > sliceWorst) {
            sliceWorst = ms;
         }

         while(now >= sliceEnds) {
            sliceHead = (sliceHead + 1) % 128;
            slices[sliceHead] = sliceWorst;
            sliceEnds += 100000000L;
            if (now < sliceEnds) {
               sliceWorst = 0.0F;
            }
         }

      } else {
         interrupted = false;
         sliceEnds = now + 100000000L;
         sliceWorst = 0.0F;
      }
   }

   static void interrupt() {
      interrupted = true;
   }

   static void reset() {
      kept = 0;
      low = -1;
      fps = -1;
      frameMs = -1.0F;
      Arrays.fill(slices, 0.0F);
      interrupted = true;
   }

   static void refresh(long now) {
      long sum = 0L;
      int n = 0;

      for(int i = 0; i < kept; ++i) {
         int at = head - i & 4095;
         if (now - ended[at] > 1000000000L) {
            break;
         }

         sum += took[at];
         ++n;
      }

      if (n >= 3 && sum > 0L) {
         double mean = (double)sum / (double)n;
         fps = (int)Math.round((double)1.0E9F / mean);
         frameMs = (float)(mean / (double)1000000.0F);
      } else {
         if (kept == 0) {
            fps = -1;
            frameMs = -1.0F;
         }

      }
   }

   static void refreshLow(long now) {
      int n = 0;

      for(int i = 0; i < kept; ++i) {
         int at = head - i & 4095;
         if (now - ended[at] > 10000000000L) {
            break;
         }

         scratch[n++] = took[at];
      }

      if (n < 100) {
         low = -1;
      } else {
         Arrays.sort(scratch, 0, n);
         int worst = Math.max(1, n / 100);
         long sum = 0L;

         for(int i = n - worst; i < n; ++i) {
            sum += scratch[i];
         }

         low = (int)Math.round((double)1.0E9F / ((double)sum / (double)worst));
      }
   }

   static int fps() {
      return fps;
   }

   static int low() {
      return low;
   }

   static float frameMs() {
      return frameMs;
   }

   static float slice(int back) {
      return back <= 0 ? sliceWorst : slices[Math.floorMod(sliceHead - (back - 1), 128)];
   }
}
