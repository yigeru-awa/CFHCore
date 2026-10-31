package com.onehumanawa.cfhcore.content.gui.menu.client.title;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public class NavButton extends CFHGuiButton {
   public static final int HEIGHT = 24;
   private boolean danger;
   private boolean primary;
   private boolean centred;

   public NavButton(int x, int y, int width, Icon icon, Component label, Runnable onPress) {
      super(x, y, width, 24, label, onPress);
      this.icon = icon;
      this.iconSize = 14;
      this.leftAligned();
   }

   public NavButton danger() {
      this.danger = true;
      return this;
   }

   public NavButton primary() {
      this.primary = true;
      return this;
   }

   public NavButton centred() {
      this.centred = true;
      return this;
   }

   public boolean iconOnly(int room) {
      return this.centred && CFHGuiDraw.width(this.getMessage()) > room;
   }

   private int contentX(int x, int w, int pad, int gap, FormattedCharSequence text) {
      return !this.centred ? x + pad : x + Math.max(pad, (w - this.iconSize - gap - CFHGuiDraw.font().width(text)) / 2);
   }

   protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      float a = this.effectiveAlpha();
      if (!(a <= 0.004F)) {
         float hov = this.hover();
         float prs = this.press();
         float foc = this.focus();
         boolean usable = this.active && !this.isLocked();
         float lift = Math.max(hov, foc) * (usable ? 1.0F : 0.3F);
         int x = this.getX() + (this.centred ? 0 : Math.round(lift * 4.0F));
         int y = this.getY() + this.enterOffset() + Math.round(prs);
         int w = this.getWidth();
         int h = this.getHeight();
         int fill = Colors.lerp(p.surface(), p.surfaceHover(), lift);
         int border = Colors.lerp(p.border(), p.borderStrong(), lift);
         if (this.primary) {
            fill = Colors.lerp(Colors.lerp(p.surface(), p.accent(), 0.2F), Colors.lerp(p.surfaceHover(), p.accent(), 0.34F), lift);
            border = Colors.lerp(Colors.withAlpha(p.accent(), 160), p.accent(), lift);
         }

         if (!usable) {
            fill = Colors.withAlpha(p.surface(), 112);
            border = Colors.withAlpha(p.border(), 112);
         }

         fill = t.control(Colors.brighten(fill, -0.1F * prs));
         border = t.controlEdge(border);
         CFHGuiDraw.shadow(g, x, y, w, h, 0.3F * a * (1.0F - prs));
         CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(fill, a), t.radius());
         CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(border, a), t.radius());
         int barH = usable ? Math.round((float)(h - 10) * (this.primary ? 1.0F : lift)) : 0;
         if (barH > 0) {
            CFHGuiDraw.rect(g, x + 2, y + (h - barH) / 2, 2, barH, Colors.scaleAlpha(this.danger ? p.danger() : p.accent(), a));
         }

         CFHGuiDraw.focusRing(g, x, y, w, h, foc * a);
         int ic = usable ? Colors.lerp(this.primary ? p.accent() : p.textMuted(), this.danger ? p.danger() : p.accent(), lift) : p.textDim();
         int fg = !usable ? p.textDim() : (this.danger ? Colors.lerp(p.text(), p.danger(), lift) : Colors.lerp(p.text(), -1, lift * 0.3F));
         int lock = this.isLocked() ? lockSize(h) + 4 : 0;
         if (this.iconOnly(w - this.iconSize - 24 - lock)) {
            Icons.draw(g, this.icon, x + (w - this.iconSize) / 2, y + (h - this.iconSize) / 2, this.iconSize, Colors.scaleAlpha(ic, a));
         } else {
            FormattedCharSequence text = CFHGuiDraw.truncate(this.getMessage(), w - this.iconSize - 24 - lock);
            int ix = this.contentX(x, w, 9, 7, text);
            Icons.draw(g, this.icon, ix, y + (h - this.iconSize) / 2, this.iconSize, Colors.scaleAlpha(ic, a));
            g.drawString(CFHGuiDraw.font(), text, ix + this.iconSize + 7, y + (h - 9) / 2 + 1, Colors.scaleAlpha(fg, a), false);
         }

         if (this.isLocked()) {
            this.drawLock(g, x, y, w, h, Colors.scaleAlpha(fg, a));
         }

      }
   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      float a = this.effectiveAlpha();
      if (!(a <= 0.004F)) {
         boolean usable = this.active && !this.isLocked();
         int x = this.getX();
         int y = this.getY() + this.enterOffset() + Math.round(this.press());
         int w = this.getWidth();
         int h = this.getHeight();
         CFHGuiDraw.vanillaButton(g, x, y, w, h, usable ? Math.max(this.hover(), this.focus()) : 0.0F, usable, a);
         if (this.primary && usable) {
            CFHGuiDraw.rect(g, x + 1, y + 1, w - 2, h - 2, Colors.scaleAlpha(Colors.withAlpha(Theme.current().palette().accent(), 90), a));
         }

         int fg = !usable ? -6250336 : (this.danger ? Theme.current().palette().danger() : -1);
         int lock = this.isLocked() ? lockSize(h) + 4 : 0;
         if (this.iconOnly(w - this.iconSize - 22 - lock)) {
            Icons.draw(g, this.icon, x + (w - this.iconSize) / 2, y + (h - this.iconSize) / 2, this.iconSize, Colors.scaleAlpha(fg, a));
         } else {
            FormattedCharSequence text = CFHGuiDraw.truncate(this.getMessage(), w - this.iconSize - 22 - lock);
            int ix = this.contentX(x, w, 8, 6, text);
            Icons.draw(g, this.icon, ix, y + (h - this.iconSize) / 2, this.iconSize, Colors.scaleAlpha(fg, a));
            g.drawString(CFHGuiDraw.font(), text, ix + this.iconSize + 6, y + (h - 9) / 2 + 1, Colors.scaleAlpha(fg, a), true);
         }

         if (this.isLocked()) {
            this.drawLock(g, x, y, w, h, Colors.scaleAlpha(fg, a));
         }

      }
   }
}
