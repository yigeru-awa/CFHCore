package com.onehumanawa.cfhcore.content.gui.core.compat.xaero;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.screen.reskin.IconCell;
import com.onehumanawa.cfhcore.content.gui.core.screen.reskin.ReskinDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

public final class XaeroScreenDraw {
   public static final int DROPDOWN_RADIUS = 2;
   private static final int ROW = -939524096;
   private static final int ROW_HOVERED = -13487566;
   private static final int ROW_SELECTED = -922757376;
   private static final int ROW_SELECTED_HOVERED = -10496;
   private static final int EDGE_CLOSED = -6250336;
   private static final int KEY_GREEN = 5635925;
   private static final int KEY_DARK_GREEN = 43520;
   private static Style keyStyleIn;
   private static Style keyStyleOut;
   private static int keyAccent;
   public static final int LABEL_PAD = 3;

   private XaeroScreenDraw() {
   }

   public static void tooltipBox(GuiGraphics g, int x0, int y0, int x1, int y1) {
      Palette p = Theme.current().palette();
      CFHGuiDraw.popupPlate(g, x0, y0, x1 - x0, y1 - y0, p.surfaceActive(), p.borderStrong(), 0.6F, 1.0F);
   }

   public static void tooltipLine(GuiGraphics g, Font font, Component text, int x, int y, int color) {
      g.drawString(font, keysInAccent(text.getVisualOrderText()), x, y, textColor(color, Theme.current().palette().text()), false);
   }

   private static int textColor(int color, int cfhgui) {
      return (color & 16777215) == 16777215 ? cfhgui : color | -16777216;
   }

   private static FormattedCharSequence keysInAccent(FormattedCharSequence line) {
      return (sink) -> line.accept((index, style, codePoint) -> sink.accept(index, keyStyle(style), codePoint));
   }

   private static Style keyStyle(Style style) {
      int accent = Theme.current().palette().accent() & 16777215;
      if (style == keyStyleIn && accent == keyAccent) {
         return keyStyleOut;
      } else {
         TextColor color = style.getColor();
         boolean key = color != null && (color.getValue() == 5635925 || color.getValue() == 43520);
         keyStyleIn = style;
         keyAccent = accent;
         keyStyleOut = key ? style.withColor(TextColor.fromRgb(accent)) : style;
         return keyStyleOut;
      }
   }

   public static boolean dropdownLit(int color) {
      return color != -939524096;
   }

   public static int dropdownRow(int color) {
      Palette p = Theme.current().palette();
      int var10000;
      switch (color) {
         case -939524096:
            var10000 = p.surface();
            break;
         case -922757376:
            var10000 = Colors.mix(p.surface(), p.accent(), 0.2F);
            break;
         case -13487566:
            var10000 = p.surfaceHover();
            break;
         case -10496:
            var10000 = Colors.mix(p.surfaceHover(), p.accent(), 0.3F);
            break;
         default:
            int r = color >> 16 & 255;
            int gr = color >> 8 & 255;
            int b = color & 255;
            var10000 = Math.abs(r - gr) < 12 && Math.abs(gr - b) < 12 ? p.bg2() : color;
      }

      return var10000;
   }

   public static int dropdownEdge(int color, boolean hovered, boolean menu) {
      Palette p = Theme.current().palette();
      if (color == -6250336) {
         return hovered ? p.borderStrong() : p.border();
      } else {
         return menu ? p.borderStrong() : p.accent();
      }
   }

   public static void dropdownLabel(GuiGraphics g, Font font, Component text, int centerX, int y, int color, boolean bright, boolean menu) {
      Palette p = Theme.current().palette();
      FormattedCharSequence line = text.getVisualOrderText();
      g.drawString(font, menu ? keysInAccent(line) : line, centerX - font.width(line) / 2, y, textColor(color, bright ? p.text() : p.textMuted()), false);
   }

   public static void dropdownShadow(GuiGraphics g, int x0, int top, int x1, int bottom) {
      int shade = Colors.scaleAlpha(Theme.current().palette().shadow(), 0.6F);
      CFHGuiDraw.rect(g, x1 + 1, top + 2, 2, bottom - top + 1, shade);
      CFHGuiDraw.rect(g, x0 + 2, bottom + 1, x1 - x0 - 1, 2, shade);
   }

   public static void iconButton(GuiGraphics g, AbstractWidget widget, ResourceLocation texture, int u, int v, int iconWidth, int iconHeight) {
      Palette p = Theme.current().palette();
      float hover = ReskinDraw.hover(widget);
      float press = ReskinDraw.press(widget);
      int nudge = Math.round(press);
      int x = widget.getX() + 1;
      int y = widget.getY() + 1 + nudge;
      int w = widget.getWidth() - 2;
      int h = widget.getHeight() - 2;
      ReskinDraw.button(g, widget, x, y, w, h, hover, press);
      int tint = widget.active ? Colors.lerp(p.textMuted(), p.text(), Math.max(hover, widget.isFocused() ? 1.0F : 0.0F)) : p.textDim();
      RenderSystem.enableBlend();
      g.setColor((float)Colors.red(tint) / 255.0F, (float)Colors.green(tint) / 255.0F, (float)Colors.blue(tint) / 255.0F, widget.active ? 1.0F : 0.6F);
      IconCell.blitCentered(g, texture, (float)widget.getX() + (float)widget.getWidth() / 2.0F, (float)widget.getY() + (float)widget.getHeight() / 2.0F + (float)nudge, u, v, iconWidth, iconHeight);
      g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.disableBlend();
   }

   public static int labelPlate(float r, float g, float b, float a) {
      return !(r > 0.02F) && !(g > 0.02F) && !(b > 0.02F) ? Colors.withAlpha(Theme.current().palette().bg(), 216) : Colors.argb(Math.round(a * 255.0F), Math.round(r * 255.0F), Math.round(g * 255.0F), Math.round(b * 255.0F));
   }

   public static int labelText(int color) {
      return textColor(color, Theme.current().palette().text());
   }

   public static int messagePlate(int color) {
      return Colors.withAlpha(Theme.current().palette().bg(), Math.min(255, Colors.alpha(color) * 216 / 127));
   }

   public static int messageText(int color) {
      return Colors.withAlpha(textColor(color, Theme.current().palette().text()), Colors.alpha(color));
   }

   public static int listPlate() {
      return Colors.withAlpha(Theme.current().palette().bg(), 216);
   }

   public static int listText(int color) {
      Palette p = Theme.current().palette();
      return (color & 16777215) == 16777215 ? p.text() : ((color & 16777215) == 5592405 ? p.textDim() : color | -16777216);
   }
}
