package com.onehumanawa.cfhcore.content.gui.core.widget;

import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;

public class CFHGuiBadge extends CFHGuiWidget {
   private int color;
   private static final int PILL_H = 11;

   public CFHGuiBadge(int x, int y, Component text, int color) {
      super(x, y, CFHGuiDraw.width(text) + 8, 10, text);
      this.color = color;
      this.active = false;
   }

   public CFHGuiBadge color(int argb) {
      this.color = argb;
      return this;
   }

   public CFHGuiBadge text(Component t) {
      this.setMessage(t);
      this.setWidth(CFHGuiDraw.width(t) + 8);
      return this;
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return false;
   }

   public ComponentPath nextFocusPath(FocusNavigationEvent event) {
      return null;
   }

   protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      draw(g, this.getMessage(), this.getX(), this.getY(), Colors.scaleAlpha(this.color, this.effectiveAlpha()));
   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      draw(g, this.getMessage(), this.getX(), this.getY(), Colors.scaleAlpha(this.color, this.effectiveAlpha()));
   }

   public static int draw(GuiGraphics g, Component text, int x, int y, int color) {
      int w = CFHGuiDraw.width(text) + 8;
      int fill = Theme.current().isVanilla() ? Colors.withAlpha(color, 192) : Colors.withAlpha(color, 64);
      CFHGuiDraw.pixelRound(g, x, y - 1, w, 11, fill, 2);
      if (!Theme.current().isVanilla()) {
         CFHGuiDraw.outline(g, x, y - 1, w, 11, Colors.withAlpha(color, 144), 2);
      }

      int fg = Theme.current().isVanilla() ? Colors.readableOn(color) : Colors.brighten(color, 0.35F);
      g.drawString(CFHGuiDraw.font(), text, x + 4, y + 1, Colors.withAlpha(fg, Colors.alpha(color)), false);
      return w;
   }

   public static int drawCount(GuiGraphics g, int count, int x, int y) {
      String s = count > 99 ? "99+" : Integer.toString(count);
      int w = Math.max(11, CFHGuiDraw.width(s) + 6);
      int c = Theme.current().accent();
      CFHGuiDraw.pixelRound(g, x, y - 1, w, 11, c, 3);
      g.drawString(CFHGuiDraw.font(), s, x + (w - CFHGuiDraw.width(s) + 1) / 2, y + 1, Theme.current().palette().accentText(), false);
      return w;
   }
}
