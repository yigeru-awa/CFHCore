package com.onehumanawa.cfhcore.content.gui.core.client;

import java.util.LinkedHashMap;
import java.util.Map;

public final class HudKeepOut {
   private static final long HOLD_NS = 250000000L;
   private static final Map<String, float[]> BOXES = new LinkedHashMap();
   private static final Map<String, Long> DRAWN = new LinkedHashMap();

   private HudKeepOut() {
   }

   public static void begin(String owner, float x0, float y0, float x1, float y1) {
      BOXES.put(owner, new float[]{Math.min(x0, x1), Math.min(y0, y1), Math.max(x0, x1), Math.max(y0, y1)});
      DRAWN.put(owner, System.nanoTime());
   }

   public static void add(String owner, float x0, float y0, float x1, float y1) {
      float[] b = (float[])BOXES.get(owner);
      if (b == null) {
         begin(owner, x0, y0, x1, y1);
      } else {
         b[0] = Math.min(b[0], Math.min(x0, x1));
         b[1] = Math.min(b[1], Math.min(y0, y1));
         b[2] = Math.max(b[2], Math.max(x0, x1));
         b[3] = Math.max(b[3], Math.max(y0, y1));
         DRAWN.put(owner, System.nanoTime());
      }
   }

   public static float clearY(float x, float y, float w, float h, float screenH, float gap) {
      if (BOXES.isEmpty()) {
         return y;
      } else {
         long now = System.nanoTime();
         boolean down = y + h / 2.0F < screenH / 2.0F;
         float top = y;

         for(int pass = 0; pass < 2; ++pass) {
            for(Map.Entry<String, float[]> e : BOXES.entrySet()) {
               Long at = (Long)DRAWN.get(e.getKey());
               if (at != null && now - at <= 250000000L) {
                  float[] b = (float[])e.getValue();
                  if (!(x + w <= b[0]) && !(x >= b[2]) && !(top + h <= b[1] - gap) && !(top >= b[3] + gap)) {
                     top = down ? b[3] + gap : b[1] - gap - h;
                  }
               }
            }
         }

         return Math.max(0.0F, Math.min(top, screenH - h));
      }
   }
}
