package com.onehumanawa.cfhcore.content.gui.core.layout.editor;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

final class EditorStyle {
   static final int Z = 150;
   static final int TOOLBAR_H = 24;
   static final int PROPS_W = 186;
   static final int LAYERS_W = 150;
   static final int HANDLE_HIT = 3;

   static Theme theme() {
      return Theme.current();
   }

   static Palette p() {
      return Theme.current().palette();
   }

   static boolean vanilla() {
      return Theme.current().isVanilla();
   }

   static int accent() {
      return p().accent();
   }

   static int text() {
      return p().text();
   }

   static int muted() {
      return p().textMuted();
   }

   static int dim() {
      return p().textDim();
   }

   static int panelFill() {
      return vanilla() ? -267251182 : Colors.withAlpha(p().surface(), 244);
   }

   static int panelBorder() {
      return vanilla() ? -7631989 : p().borderStrong();
   }

   static int rowHover() {
      return vanilla() ? 822083583 : p().surfaceHover();
   }

   static int rowActive() {
      return vanilla() ? 1358954495 : p().surfaceActive();
   }

   static int gridDot() {
      return Colors.withAlpha(vanilla() ? 16777215 : p().text(), 44);
   }

   static int hoverOutline() {
      return Colors.withAlpha(accent(), 144);
   }

   static int guide() {
      return Colors.withAlpha(accent(), 200);
   }

   static int bandFill() {
      return Colors.withAlpha(accent(), 40);
   }

   static int ghost() {
      return Colors.withAlpha(p().textMuted(), 144);
   }

   static int radius() {
      return theme().radius();
   }

   static void panel(GuiGraphics g, int x, int y, int w, int h) {
      if (vanilla()) {
         g.fill(x, y, x + w, y + h, panelFill());
         CFHGuiDraw.outline(g, x, y, w, h, panelBorder(), 0);
      } else {
         CFHGuiDraw.pixelRound(g, x, y, w, h, panelFill(), radius());
         CFHGuiDraw.outline(g, x, y, w, h, panelBorder(), radius());
      }

      CFHGuiDraw.hline(g, x + 1, y, w - 2, Colors.withAlpha(accent(), 160));
   }

   static int dashPhase() {
      return theme().motion() <= 0.0F ? 0 : (int)(Clock.nowMs() / 90L % 5L);
   }

   static void dashedRect(GuiGraphics g, int x, int y, int w, int h, int color, int phase) {
      if (w > 0 && h > 0) {
         if (w >= 8 && h >= 8) {
            int dash = 3;
            int period = 5;
            int per = 2 * (w + h) - 4;

            for(int start = -(phase % 5); start < per; start += 5) {
               int end = Math.min(per, start + 3);

               for(int k = Math.max(0, start); k < end; ++k) {
                  int px;
                  int py;
                  if (k < w) {
                     px = x + k;
                     py = y;
                  } else if (k < w + h - 1) {
                     px = x + w - 1;
                     py = y + (k - w) + 1;
                  } else if (k < 2 * w + h - 2) {
                     px = x + w - 2 - (k - (w + h - 1));
                     py = y + h - 1;
                  } else {
                     px = x;
                     py = y + h - 2 - (k - (2 * w + h - 2));
                  }

                  g.fill(px, py, px + 1, py + 1, color);
               }
            }

         } else {
            CFHGuiDraw.outline(g, x, y, w, h, color, 0);
         }
      }
   }

   static int[][] handlePoints(Rect r) {
      int l = r.x();
      int t = r.y();
      int rr = r.right() - 1;
      int b = r.bottom() - 1;
      int cx = r.x() + r.w() / 2;
      int cy = r.y() + r.h() / 2;
      return new int[][]{{l, t}, {cx, t}, {rr, t}, {rr, cy}, {rr, b}, {cx, b}, {l, b}, {l, cy}};
   }

   static void handles(GuiGraphics g, Rect r) {
      int outer = vanilla() ? -16777216 : p().bg();

      for(int[] pt : handlePoints(r)) {
         g.fill(pt[0] - 2, pt[1] - 2, pt[0] + 3, pt[1] + 3, outer);
         g.fill(pt[0] - 1, pt[1] - 1, pt[0] + 2, pt[1] + 2, accent());
      }

   }

   static int labelPill(GuiGraphics g, Component text, int x, int y) {
      FormattedCharSequence seq = CFHGuiDraw.truncate(text, 180);
      int w = CFHGuiDraw.font().width(seq) + 8;
      int h = 12;
      int fill = vanilla() ? -267382768 : Colors.withAlpha(p().surfaceActive(), 244);
      int r = radius() > 0 ? 2 : 0;
      CFHGuiDraw.pixelRound(g, x, y, w, 12, fill, r);
      CFHGuiDraw.outline(g, x, y, w, 12, Colors.withAlpha(accent(), 192), r);
      g.drawString(CFHGuiDraw.font(), seq, x + 4, y + 2, text(), vanilla());
      return w;
   }

   static void grid(GuiGraphics g, int w, int h, int spacing) {
      int step;
      for(step = Math.max(2, spacing); (long)(w / step) * (long)(h / step) > 16000L; step *= 2) {
      }

      int c = gridDot();

      for(int y = 0; y <= h; y += step) {
         for(int x = 0; x <= w; x += step) {
            g.fill(x, y, x + 1, y + 1, c);
         }
      }

   }

   private EditorStyle() {
   }
}
