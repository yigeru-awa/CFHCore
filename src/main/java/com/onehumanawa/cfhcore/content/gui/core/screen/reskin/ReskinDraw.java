package com.onehumanawa.cfhcore.content.gui.core.screen.reskin;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;

public final class ReskinDraw {
   public static float hover(AbstractWidget w) {
      if (w instanceof ReskinState st) {
         st.cfhgui$hoverAnim().set(w.active && w.isHoveredOrFocused());
         return st.cfhgui$hoverAnim().get();
      } else {
         return w.active && w.isHoveredOrFocused() ? 1.0F : 0.0F;
      }
   }

   public static float press(AbstractWidget w) {
      boolean down = w.active && w.isHovered() && Minecraft.getInstance().mouseHandler.isLeftPressed();
      if (w instanceof ReskinState st) {
         st.cfhgui$pressAnim().set(down);
         return st.cfhgui$pressAnim().get();
      } else {
         return down ? 1.0F : 0.0F;
      }
   }

   public static void button(GuiGraphics g, AbstractWidget w, int x, int y, int width, int height, float hover, float press) {
      CFHGuiDraw.darkFace(g, x, y, width, height, hover, press, w.active, 1.0F);
      if (w.active && w.isFocused() && !w.isHovered()) {
         CFHGuiDraw.focusRing(g, x, y, width, height, 1.0F);
      }

   }

   public static void sliderTrack(GuiGraphics g, int x, int y, int w, int h, float value, float hover, boolean active) {
      Theme t = Theme.current();
      Palette p = t.palette();
      int fill = active ? Colors.lerp(p.bg2(), p.surface(), hover) : Colors.withAlpha(p.bg2(), 128);
      int border = active ? Colors.lerp(p.border(), p.borderStrong(), hover) : Colors.withAlpha(p.border(), 128);
      CFHGuiDraw.pixelRound(g, x, y, w, h, fill, t.radius());
      int filled = Math.round(4.0F + (float)(w - 8) * Math.max(0.0F, Math.min(1.0F, value)));
      CFHGuiDraw.pixelRound(g, x, y, Math.max(4, filled), h, Colors.withAlpha(active ? p.accent() : p.textDim(), 56), t.radius());
      CFHGuiDraw.outline(g, x, y, w, h, border, t.radius());
   }

   public static void sliderHandle(GuiGraphics g, int x, int y, int w, int h, float hover, boolean active) {
      Theme t = Theme.current();
      Palette p = t.palette();
      int r = Math.min(2, t.radius());
      CFHGuiDraw.pixelRound(g, x, y, w, h, active ? Colors.lerp(p.surfaceActive(), p.textDim(), hover * 0.4F) : p.surface(), r);
      CFHGuiDraw.outline(g, x, y, w, h, active ? Colors.lerp(p.borderStrong(), p.accent(), hover) : p.border(), r);
      CFHGuiDraw.rect(g, x + w / 2 - 1, y + h / 2 - 3, 2, 6, Colors.withAlpha(active ? Colors.lerp(p.textMuted(), p.accent(), hover) : p.textDim(), 224));
   }

   public static void textField(GuiGraphics g, int x, int y, int w, int h, boolean focused, float hover, boolean active) {
      Theme t = Theme.current();
      Palette p = t.palette();
      int fill = focused ? p.surface() : Colors.lerp(p.bg2(), p.surface(), hover);
      int border = focused ? p.accent() : Colors.lerp(p.border(), p.borderStrong(), hover);
      if (!active) {
         fill = Colors.withAlpha(p.bg2(), 128);
         border = Colors.withAlpha(p.border(), 128);
      }

      CFHGuiDraw.pixelRound(g, x, y, w, h, fill, t.radius());
      CFHGuiDraw.outline(g, x, y, w, h, border, t.radius());
   }

   public static void checkbox(GuiGraphics g, int x, int y, int size, boolean selected, float hover, boolean focused, boolean active) {
      Theme t = Theme.current();
      Palette p = t.palette();
      int box = Math.max(8, size - 4);
      int bx = x + (size - box) / 2;
      int by = y + (size - box) / 2;
      int fill = selected ? p.accent() : Colors.lerp(p.surface(), p.surfaceHover(), hover);
      int border = selected ? p.accentHover() : Colors.lerp(p.borderStrong(), p.textDim(), hover);
      if (!active) {
         fill = Colors.withAlpha(fill, 112);
         border = Colors.withAlpha(border, 112);
      }

      CFHGuiDraw.pixelRound(g, bx, by, box, box, fill, 2);
      CFHGuiDraw.outline(g, bx, by, box, box, border, 2);
      if (selected) {
         Icons.draw(g, Icon.CHECK, bx + (box - 8) / 2, by + (box - 8) / 2, 8, p.accentText());
      }

      if (focused) {
         CFHGuiDraw.focusRing(g, bx, by, box, box, 1.0F);
      }

   }

   public static void listBackground(GuiGraphics g, int x, int y, int w, int h, boolean inWorld) {
      Theme t = Theme.current();
      CFHGuiDraw.rect(g, x, y, w, h, t.panel(t.palette().bg2()));
   }

   public static void listSeparators(GuiGraphics g, int x, int top, int w, int bottom, boolean inWorld) {
      Palette p = Theme.current().palette();
      CFHGuiDraw.hline(g, x, top - 1, w, p.border());
      CFHGuiDraw.hline(g, x, bottom, w, p.border());
   }

   public static void selection(GuiGraphics g, int x, int y, int w, int h, boolean focused) {
      Theme t = Theme.current();
      Palette p = t.palette();
      CFHGuiDraw.pixelRound(g, x, y, w, h, p.surfaceActive(), t.radius());
      CFHGuiDraw.outline(g, x, y, w, h, focused ? Colors.withAlpha(p.accent(), 160) : p.borderStrong(), t.radius());
      CFHGuiDraw.rect(g, x, y + 2, 2, h - 4, p.accent());
   }

   public static void scrollTrack(GuiGraphics g, int x, int y, int w, int h) {
      Palette p = Theme.current().palette();
      CFHGuiDraw.pixelRound(g, x + 1, y, 4, h, Colors.withAlpha(p.surface(), 128), 1);
   }

   public static void scrollThumb(GuiGraphics g, int x, int y, int w, int h) {
      Palette p = Theme.current().palette();
      CFHGuiDraw.pixelRound(g, x + 1, y, 4, h, p.borderStrong(), 1);
   }

   public static void menuBackground(GuiGraphics g, int x, int y, int w, int h, boolean inWorld) {
      Theme t = Theme.current();
      if (inWorld) {
         CFHGuiDraw.rect(g, x, y, w, h, t.palette().overlay());
      } else {
         CFHGuiDraw.rect(g, x, y, w, h, t.bg());
         CFHGuiDraw.vignette(g, x, y, w, h, 0.3F);
      }

   }

   public static void transparentBackground(GuiGraphics g, int w, int h) {
      transparentBackground(g, w, h, 1.0F);
   }

   public static void transparentBackground(GuiGraphics g, int w, int h, float fade) {
      CFHGuiDraw.rect(g, 0, 0, w, h, Colors.withAlpha(Theme.current().bg(), Math.round(200.0F * fade)));
   }

   public static void panoramaVeil(GuiGraphics g, int w, int h, float fade) {
      if (!(fade <= 0.01F)) {
         CFHGuiDraw.rect(g, 0, 0, w, h, Colors.withAlpha(Theme.current().bg(), Math.round(64.0F * fade)));
         CFHGuiDraw.vignette(g, 0, 0, w, h, 0.5F * fade);
      }
   }

   public static void containerPanel(GuiGraphics g, int x, int y, int w, int h) {
      Theme t = Theme.current();
      Palette p = t.containerPalette();
      int r = t.containerRadius();
      if (t.panelAlpha() < 255) {
         CFHGuiDraw.underlay(g, x, y, w, h, r, 1.0F);
      }

      containerShadow(g, x, y, w, h, r, p);
      CFHGuiDraw.pixelRound(g, x, y, w, h, t.panel(p.bg()), r);
      CFHGuiDraw.vgradient(g, x + r + 1, y + r + 1, w - 2 * r - 2, Math.min(24, h / 4), Colors.withAlpha(p.surface(), 112), 0);
      CFHGuiDraw.hline(g, x + r + 1, y + 1, w - 2 * r - 2, Colors.withAlpha(16777215, 12));
      CFHGuiDraw.outline(g, x, y, w, h, p.border(), r);
   }

   private static void containerShadow(GuiGraphics g, int x, int y, int w, int h, int r, Palette p) {
      CFHGuiDraw.shadow(g, x, y, w, h, Colors.scaleAlpha(p.shadow(), 0.7F), r);
   }

   public static void containerSlot(GuiGraphics g, int x, int y) {
      Palette p = Theme.current().containerPalette();
      CFHGuiDraw.pixelRound(g, x, y, 18, 18, Colors.brighten(p.bg(), -0.32F), 1);
      CFHGuiDraw.outline(g, x, y, 18, 18, Colors.withAlpha(p.border(), 208), 1);
      CFHGuiDraw.hline(g, x + 1, y + 1, 16, Colors.withAlpha(0, 72));
      CFHGuiDraw.vline(g, x + 1, y + 2, 15, Colors.withAlpha(0, 72));
      CFHGuiDraw.hline(g, x + 2, y + 16, 15, Colors.withAlpha(16777215, 10));
      CFHGuiDraw.vline(g, x + 16, y + 2, 14, Colors.withAlpha(16777215, 10));
   }

   public static void containerSlotHighlight(GuiGraphics g, int x, int y, int z) {
      Palette p = Theme.current().containerPalette();
      g.pose().pushPose();
      g.pose().translate(0.0F, 0.0F, (float)z);
      CFHGuiDraw.rect(g, x, y, 16, 16, Colors.withAlpha(16777215, 44));
      CFHGuiDraw.outline(g, x - 1, y - 1, 18, 18, Colors.withAlpha(p.accent(), 200), 1);
      CFHGuiDraw.outline(g, x - 2, y - 2, 20, 20, Colors.withAlpha(p.accent(), 64), 2);
      g.pose().popPose();
   }

   public static void containerInset(GuiGraphics g, int x, int y, int w, int h) {
      Palette p = Theme.current().containerPalette();
      CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.brighten(p.bg(), -0.32F), 2);
      CFHGuiDraw.outline(g, x, y, w, h, Colors.withAlpha(p.border(), 208), 2);
      CFHGuiDraw.hline(g, x + 2, y + 1, w - 4, Colors.withAlpha(0, 72));
      CFHGuiDraw.vline(g, x + 1, y + 2, h - 4, Colors.withAlpha(0, 72));
   }

   public static void containerArrow(GuiGraphics g, int x, int y) {
      Icons.draw(g, Icon.ARROW_RIGHT, x + 4, y, 14, Theme.current().containerPalette().textDim());
   }

   public static void containerTint(GuiGraphics g, int x, int y, int w, int h) {
      Theme t = Theme.current();
      Palette p = t.containerPalette();
      int r = t.containerRadius();
      CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.withAlpha(p.bg(), 212), r);
      CFHGuiDraw.outline(g, x, y, w, h, p.border(), r);
   }

   public static void containerTab(GuiGraphics g, int x, int y, int w, int h, boolean selected) {
      Theme t = Theme.current();
      Palette p = t.containerPalette();
      CFHGuiDraw.pixelRound(g, x, y, w, h, t.chrome(selected ? p.bg() : p.bg2()), 2);
      CFHGuiDraw.outline(g, x, y, w, h, selected ? p.borderStrong() : Colors.withAlpha(p.border(), 192), 2);
      if (selected) {
         CFHGuiDraw.rect(g, x + 3, y + 1, w - 6, 2, p.accent());
      }

   }

   public static void containerThumb(GuiGraphics g, int x, int y, int w, int h, boolean enabled) {
      Palette p = Theme.current().containerPalette();
      CFHGuiDraw.pixelRound(g, x, y, w, h, enabled ? p.borderStrong() : Colors.withAlpha(p.borderStrong(), 96), 1);
      if (enabled) {
         CFHGuiDraw.rect(g, x + 1, y + 1, Math.max(1, w - 2), 1, Colors.withAlpha(16777215, 24));
      }

   }

   public static void tooltipBackground(GuiGraphics g, int x, int y, int w, int h, int z) {
      Theme t = Theme.current();
      Palette p = t.palette();
      int bx = x - 4;
      int by = y - 4;
      int bw = w + 8;
      int bh = h + 8;
      g.pose().pushPose();
      g.pose().translate(0.0F, 0.0F, (float)z);
      CFHGuiDraw.popupPlate(g, bx, by, bw, bh, p.surfaceActive(), p.borderStrong(), 0.6F, 1.0F);
      g.pose().popPose();
   }

   public static void tabBar(GuiGraphics g, int x, int y, int w, int h) {
      Theme t = Theme.current();
      Palette p = t.palette();
      CFHGuiDraw.rect(g, x, y, w, h, t.chrome(p.bg2()));
      CFHGuiDraw.hline(g, x, y + h - 1, w, p.border());
   }

   public static void tab(GuiGraphics g, int x, int y, int w, int h, boolean selected, float hover, boolean focused) {
      Theme t = Theme.current();
      Palette p = t.palette();
      int r = t.radius();
      if (selected) {
         CFHGuiDraw.pixelRound(g, x, y + 2, w, h - 2, p.surface(), r);
         CFHGuiDraw.outline(g, x, y + 2, w, h - 2, p.borderStrong(), r);
         CFHGuiDraw.rect(g, x + 1, y + h - 1, w - 2, 1, p.surface());
         CFHGuiDraw.rect(g, x + r + 1, y + 2, w - r * 2 - 2, 2, p.accent());
      } else if (hover > 0.01F) {
         CFHGuiDraw.pixelRound(g, x + 1, y + 4, w - 2, h - 6, Colors.scaleAlpha(p.surfaceHover(), hover), r);
      }

      if (focused) {
         CFHGuiDraw.rect(g, x + 4, y + h - 3, w - 8, 1, p.accent());
      }

   }

   private ReskinDraw() {
   }
}
