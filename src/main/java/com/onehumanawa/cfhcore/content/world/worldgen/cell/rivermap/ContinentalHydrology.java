package com.onehumanawa.cfhcore.content.world.worldgen.cell.rivermap;

import java.util.Arrays;
import java.util.Random;

public class ContinentalHydrology {
   private static final ContinentalHydrology.Step[] BOUNDARIES = new ContinentalHydrology.Step[15];
   private static final double TRANSITION_WIDTH = 0.02;
   private static final int NUM_STEPS = 15;
   private static final double[] SPLITS = new double[30];
   private static final ThreadLocal<ContinentalHydrology.StepCache> THREAD_CACHE = ThreadLocal.withInitial(ContinentalHydrology.StepCache::new);

   public static int getStepId(double x) {
      double val = Math.max(0.0, Math.min(1.0, x));
      ContinentalHydrology.StepCache cache = THREAD_CACHE.get();
      if (val == cache.lastX) {
         return cache.lastId;
      } else {
         int idx = Arrays.binarySearch(SPLITS, val);
         int insertionPoint = idx >= 0 ? idx + 1 : -(idx + 1);
         int result;
         if (insertionPoint == 0) {
            result = -1;
         } else if ((insertionPoint & 1) == 1) {
            result = -2;
         } else {
            result = (insertionPoint >> 1) - 1;
         }

         cache.lastX = val;
         cache.lastId = result;
         return result;
      }
   }

   public static float fixedCurvedStep(double x) {
      double val = Math.max(0.0, Math.min(1.0, x));
      if (val == 0.0) {
         return 0.0F;
      } else if (val < SPLITS[0]) {
         return 0.0F;
      } else {
         for (int i = 0; i < 15; i++) {
            double targetY = BOUNDARIES[i].y;
            double prevY = i > 0 ? BOUNDARIES[i - 1].y : 0.0;
            double tStart = SPLITS[i * 2];
            double tEnd = SPLITS[i * 2 + 1];
            if (val >= tStart && val <= tEnd) {
               double t = (val - tStart) / 0.02;
               double smoothT = t * t * (3.0 - 2.0 * t);
               return (float)(prevY + smoothT * (targetY - prevY));
            }

            double nextRampStart = i + 1 < 15 ? SPLITS[(i + 1) * 2] : 1.1;
            if (val > tEnd && val < nextRampStart) {
               return (float)targetY;
            }
         }

         return (float)BOUNDARIES[14].y;
      }
   }

   public static float getTargetWaterHeight(float continentEdge) {
      return fixedCurvedStep((double)continentEdge);
   }

   public static float getWeightedWaterHeight(float inlandPercentage) {
      return getTargetWaterHeight(inlandPercentage) * 0.5F;
   }

   public static float getFlatnessFactor(double x) {
      double val = Math.max(0.0, Math.min(1.0, x));
      if (val < SPLITS[0]) {
         double mid = SPLITS[0] / 2.0;
         double radius = SPLITS[0] / 2.0;
         double linearDist = 1.0 - Math.abs(val - mid) / radius;
         return (float)(linearDist * linearDist * (3.0 - 2.0 * linearDist));
      } else {
         for (int i = 0; i < 15; i++) {
            double pStart = SPLITS[i * 2 + 1];
            double pEnd = i + 1 < 15 ? SPLITS[(i + 1) * 2] : 1.0;
            if (val >= pStart && val <= pEnd) {
               double mid = (pStart + pEnd) / 2.0;
               double radius = (pEnd - pStart) / 2.0;
               if (radius < 1.0E-6) {
                  return 0.0F;
               }

               double linearDist = 1.0 - Math.abs(val - mid) / radius;
               return (float)(linearDist * linearDist * (3.0 - 2.0 * linearDist));
            }
         }

         return 0.0F;
      }
   }

   static {
      Random rand = new Random(42L);
      double[] xDeltas = new double[15];
      double[] yDeltas = new double[15];
      double totalX = 0.0;
      double totalY = 0.0;

      for (int i = 0; i < 15; i++) {
         xDeltas[i] = 0.5 + rand.nextDouble();
         yDeltas[i] = 0.5 + rand.nextDouble();
         totalX += xDeltas[i];
         totalY += yDeltas[i];
      }

      double currX = 0.0;
      double currY = 0.0;

      for (int i = 0; i < 15; i++) {
         currX += xDeltas[i];
         currY += yDeltas[i];
         BOUNDARIES[i] = new ContinentalHydrology.Step(currX / totalX, currY / totalY);
      }

      for (int i = 0; i < 15; i++) {
         SPLITS[i * 2] = BOUNDARIES[i].x - 0.02;
         SPLITS[i * 2 + 1] = BOUNDARIES[i].x;
      }
   }

   private static record Step(double x, double y) {
   }

   private static class StepCache {
      double lastX = -1.0;
      int lastId = -3;
   }
}
