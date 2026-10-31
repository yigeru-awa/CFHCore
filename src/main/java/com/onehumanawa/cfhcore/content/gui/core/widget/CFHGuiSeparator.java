package com.onehumanawa.cfhcore.content.gui.core.widget;

import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;

public class CFHGuiSeparator extends CFHGuiWidget {
   private final boolean vertical;

   public CFHGuiSeparator(int x, int y, int length, boolean vertical) {
      super(x, y, vertical ? 1 : length, vertical ? length : 1, Component.empty());
      this.vertical = vertical;
      this.active = false;
   }

   public CFHGuiSeparator(int x, int y, int width, Component caption) {
      super(x, y, width, 10, caption);
      this.vertical = false;
      this.active = false;
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return false;
   }

   public ComponentPath nextFocusPath(FocusNavigationEvent event) {
      return null;
   }

   protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.draw(g, Theme.current().palette().border(), Theme.current().palette().textMuted(), false);
   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.draw(g, -9474193, -6250336, true);
   }

   private void draw(GuiGraphics g, int line, int textColor, boolean shadow) {
      int lc = Colors.scaleAlpha(line, this.effectiveAlpha());
      if (this.vertical) {
         CFHGuiDraw.vline(g, this.getX(), this.getY(), this.getHeight(), lc);
      } else {
         String cap = this.getMessage().getString();
         if (cap.isEmpty()) {
            CFHGuiDraw.hline(g, this.getX(), this.getY(), this.getWidth(), lc);
         } else {
            int y = this.getY() + 4;
            g.drawString(CFHGuiDraw.font(), this.getMessage(), this.getX(), this.getY(), Colors.scaleAlpha(textColor, this.effectiveAlpha()), shadow);
            int tw = CFHGuiDraw.width(this.getMessage()) + 6;
            CFHGuiDraw.hline(g, this.getX() + tw, y, this.getWidth() - tw, lc);
         }
      }
   }
}
