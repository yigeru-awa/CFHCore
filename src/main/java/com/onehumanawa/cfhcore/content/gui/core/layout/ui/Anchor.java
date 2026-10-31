package com.onehumanawa.cfhcore.content.gui.core.layout.ui;

import java.util.Locale;

public enum Anchor {
   TOP_LEFT(0.0F, 0.0F),
   TOP(0.5F, 0.0F),
   TOP_RIGHT(1.0F, 0.0F),
   LEFT(0.0F, 0.5F),
   CENTER(0.5F, 0.5F),
   RIGHT(1.0F, 0.5F),
   BOTTOM_LEFT(0.0F, 1.0F),
   BOTTOM(0.5F, 1.0F),
   BOTTOM_RIGHT(1.0F, 1.0F);

   public final float fx;
   public final float fy;

   private Anchor(final float fx, final float fy) {
      this.fx = fx;
      this.fy = fy;
   }

   public int x(int screenW, int ox, int w) {
      return Math.round((float)screenW * this.fx - (float)w * this.fx) + ox;
   }

   public int y(int screenH, int oy, int h) {
      return Math.round((float)screenH * this.fy - (float)h * this.fy) + oy;
   }

   public int offsetX(int screenW, int x, int w) {
      return x - Math.round((float)screenW * this.fx - (float)w * this.fx);
   }

   public int offsetY(int screenH, int y, int h) {
      return y - Math.round((float)screenH * this.fy - (float)h * this.fy);
   }

   public static Anchor parse(String s, Anchor fallback) {
      if (s == null) {
         return fallback;
      } else {
         try {
            return valueOf(s.trim().toUpperCase(Locale.ROOT));
         } catch (IllegalArgumentException var3) {
            return fallback;
         }
      }
   }

   public static Anchor nearest(int x, int y, int screenW, int screenH) {
      float fx = (float)x / (float)Math.max(1, screenW);
      float fy = (float)y / (float)Math.max(1, screenH);
      Anchor best = CENTER;
      float bestD = Float.MAX_VALUE;

      for(Anchor a : values()) {
         float d = (a.fx - fx) * (a.fx - fx) + (a.fy - fy) * (a.fy - fy);
         if (d < bestD) {
            bestD = d;
            best = a;
         }
      }

      return best;
   }
}
