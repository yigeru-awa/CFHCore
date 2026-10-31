package com.onehumanawa.cfhcore.content.gui.menu.client.lookat;

import net.minecraft.util.Mth;

final class LookPlacement {
   static final int NONE = -1;
   static final int RIGHT = 0;
   static final int LEFT = 1;
   static final int BELOW = 2;
   static final int ABOVE = 3;
   private static final int CROSS = 12;
   private static final int UNDER = 22;
   private static final int GAP = 4;
   private static final int MARGIN = 4;
   private static final int FOOT = 64;
   private static final float SECOND = 1.05F;
   private static final float STAY = 0.7F;
   static int x;
   static int y;
   private static boolean head;
   private static float headX0;
   private static float headY0;
   private static float headX1;
   private static float headY1;
   private static int px;
   private static int py;

   private LookPlacement() {
   }

   static void noHead() {
      head = false;
   }

   static void head(float x0, float y0, float x1, float y1) {
      head = true;
      headX0 = x0;
      headY0 = y0;
      headX1 = x1;
      headY1 = y1;
   }

   static int solve(int sw, int sh, int w, int h, int last) {
      int side = pair(0, 1, sw, sh, w, h, last, head);
      if (side == -1) {
         side = pair(2, 3, sw, sh, w, h, last, head);
      }

      if (side == -1 && head) {
         side = pair(0, 1, sw, sh, w, h, last, false);
         return side != -1 ? side : pair(2, 3, sw, sh, w, h, last, false);
      } else {
         return side;
      }
   }

   private static int pair(int first, int second, int sw, int sh, int w, int h, int last, boolean avoid) {
      int best = -1;
      float least = Float.MAX_VALUE;

      for(int side = first; side <= second; ++side) {
         if (place(side, sw, sh, w, h, avoid)) {
            int var10000;
            switch (side) {
               case 0 -> var10000 = px - sw / 2;
               case 1 -> var10000 = sw / 2 - px - w;
               case 2 -> var10000 = py - sh / 2;
               default -> var10000 = sh / 2 - py - h;
            }

            int away = var10000;
            float cost = (float)away * (side == second ? 1.05F : 1.0F) * (side == last ? 0.7F : 1.0F);
            if (cost < least) {
               least = cost;
               best = side;
               x = px;
               y = py;
            }
         }
      }

      return best;
   }

   private static boolean place(int side, int sw, int sh, int w, int h, boolean avoid) {
      int cx = sw / 2;
      int cy = sh / 2;
      switch (side) {
         case 0:
            px = cx + 12;
            py = cy - h / 2;
            if (avoid && onHead(w, h)) {
               px = Mth.ceil(headX1) + 4;
            }
            break;
         case 1:
            px = cx - 12 - w;
            py = cy - h / 2;
            if (avoid && onHead(w, h)) {
               px = Mth.floor(headX0) - 4 - w;
            }
            break;
         case 2:
            px = cx - w / 2;
            py = cy + 22;
            if (avoid && onHead(w, h)) {
               py = Mth.ceil(headY1) + 4;
            }
            break;
         default:
            px = cx - w / 2;
            py = cy - 12 - h;
            if (avoid && onHead(w, h)) {
               py = Mth.floor(headY0) - 4 - h;
            }
      }

      return px >= 4 && px + w <= sw - 4 && py >= 4 && py + h <= sh - (side == 2 ? 64 : 4);
   }

   private static boolean onHead(int w, int h) {
      return (float)px < headX1 + 4.0F && (float)(px + w) > headX0 - 4.0F && (float)py < headY1 + 4.0F && (float)(py + h) > headY0 - 4.0F;
   }
}
