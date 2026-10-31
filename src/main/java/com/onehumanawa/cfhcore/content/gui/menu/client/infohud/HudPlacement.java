package com.onehumanawa.cfhcore.content.gui.menu.client.infohud;

final class HudPlacement {
   static final int MARGIN = 6;
   static final int GAP = 4;
   private static final float VERTICAL_WEIGHT = 1.25F;
   private static final float STAY_BONUS = 0.75F;

   private HudPlacement() {
   }

   static boolean solve(InfoHudConfig.Corner corner, int w, int h, int sw, int sh, Rects taken, int lastX, int lastY, int[] out) {
      int homeX = corner.right ? sw - 6 - w : 6;
      int homeY = corner.bottom ? sh - 6 - h : 6;
      int bestX = homeX;
      int bestY = homeY;
      float best = Float.MAX_VALUE;

      for(int i = -1; i < taken.n; ++i) {
         int x = i < 0 ? homeX : (corner.right ? taken.x0(i) - 4 - w : taken.x1(i) + 4);
         if (x >= 6 && x + w <= sw - 6) {
            if (corner.right) {
               if (x + w / 2 < sw / 2) {
                  continue;
               }
            } else if (x + w / 2 > sw / 2) {
               continue;
            }

            for(int j = -1; j < taken.n; ++j) {
               int y = j < 0 ? homeY : (corner.bottom ? taken.y0(j) - 4 - h : taken.y1(j) + 4);
               if (y >= 6 && y + h <= sh - 6 && !taken.hits(x - 1, y - 1, w + 2, h + 2)) {
                  float cost = (float)Math.abs(x - homeX) + (float)Math.abs(y - homeY) * 1.25F;
                  if (lastX >= 0 && Math.abs(x - lastX) <= 2 && Math.abs(y - lastY) <= 2) {
                     cost *= 0.75F;
                  }

                  if (cost < best) {
                     best = cost;
                     bestX = x;
                     bestY = y;
                  }
               }
            }
         }
      }

      out[0] = Math.max(0, Math.min(Math.max(0, sw - w), bestX));
      out[1] = Math.max(0, Math.min(Math.max(0, sh - h), bestY));
      return best < Float.MAX_VALUE;
   }
}
