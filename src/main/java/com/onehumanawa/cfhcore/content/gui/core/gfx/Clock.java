package com.onehumanawa.cfhcore.content.gui.core.gfx;

public final class Clock {
   private static long lastFrameNanos = System.nanoTime();
   private static float frameDeltaMs = 16.0F;

   public static long nowMs() {
      return System.nanoTime() / 1000000L;
   }

   public static long nowNanos() {
      return System.nanoTime();
   }

   public static void onFrame() {
      long now = System.nanoTime();
      frameDeltaMs = Math.min(100.0F, (float)(now - lastFrameNanos) / 1000000.0F);
      lastFrameNanos = now;
   }

   public static float frameDelta() {
      return frameDeltaMs;
   }

   private Clock() {
   }
}
