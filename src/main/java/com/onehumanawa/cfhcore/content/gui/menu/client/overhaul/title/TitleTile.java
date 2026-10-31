package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

final class TitleTile extends CFHGuiButton {
   private static final int INSET = 5;
   private final Component heading;
   private final int textScale;
   private boolean primary;

   TitleTile(int x, int y, int w, int h, Component label, Runnable onPress) {
      super(x, y, w, h, label, onPress);
      this.heading = Fonts.heading(label);
      this.textScale = w >= 104 && h >= 104 ? 2 : 1;
   }

   TitleTile primary() {
      this.primary = true;
      return this;
   }

   private int band() {
      return 9 * this.textScale + 9;
   }

   float lift() {
      return this.usable() ? Math.max(this.hover(), this.focus()) : 0.0F;
   }

   float shown() {
      return this.effectiveAlpha();
   }

   boolean usable() {
      return this.active && !this.isLocked();
   }

   private int drawnY() {
      return this.getY() + this.enterOffset() - Math.round(this.lift()) + Math.round(this.press());
   }

   Rect window() {
      int y = this.drawnY();
      return new Rect(this.getX() + 5, y + 5, this.getWidth() - 10, this.getHeight() - 5 - this.band());
   }

   protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      float a = this.effectiveAlpha();
      if (!(a <= 0.004F)) {
         boolean usable = this.usable();
         float lift = this.lift();
         float prs = this.press();
         int x = this.getX();
         int y = this.drawnY();
         int w = this.getWidth();
         int h = this.getHeight();
         int r = t.radius();
         int fill = Colors.lerp(p.surface(), p.surfaceHover(), lift);
         int border = Colors.lerp(p.border(), p.borderStrong(), lift);
         if (!usable) {
            fill = Colors.withAlpha(p.surface(), 168);
            border = Colors.withAlpha(p.border(), 144);
         }

         fill = Colors.brighten(fill, -0.12F * prs);
         CFHGuiDraw.shadow(g, x, y, w, h, (0.4F + 0.2F * lift) * a * (1.0F - prs));
         CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(fill, a), r);
         CFHGuiDraw.hline(g, x + r + 1, y + 1, w - 2 * r - 2, Colors.scaleAlpha(352321535, a * (1.0F - prs)));
         CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(border, a), r);
         Rect win = this.window();
         int wr = Math.max(0, r - 1);
         CFHGuiDraw.pixelRound(g, win.x(), win.y(), win.w(), win.h(), Colors.scaleAlpha(p.bg2(), a), wr);
         CFHGuiDraw.hline(g, win.x() + wr + 1, win.y() + 1, win.w() - 2 * wr - 2, Colors.scaleAlpha(939524096, a));
         if (lift > 0.01F) {
            CFHGuiDraw.vgradient(g, win.x() + 1, win.y() + win.h() / 3, win.w() - 2, win.h() - win.h() / 3 - 1, Colors.withAlpha(p.accent(), 0), Colors.scaleAlpha(Colors.withAlpha(p.accent(), 60), lift * a));
         }

         CFHGuiDraw.outline(g, win.x(), win.y(), win.w(), win.h(), Colors.scaleAlpha(Colors.lerp(Colors.withAlpha(p.border(), 160), p.border(), lift), a), wr);
         int fg = !usable ? p.textDim() : Colors.lerp(p.text(), -1, lift * 0.4F);
         this.drawName(g, x, y + h - this.band(), w, this.band(), Colors.scaleAlpha(fg, a), false);
         if (this.isLocked()) {
            this.drawLock(g, x, y + h - this.band(), w, this.band(), Colors.scaleAlpha(fg, a));
         }

         float out = !usable ? 0.0F : (this.primary ? 0.3F + 0.7F * lift : lift);
         int barW = Math.round((float)(w - 2 * r - 12) * out);
         if (barW > 1) {
            CFHGuiDraw.rect(g, x + (w - barW) / 2, y + h - 3, barW, 2, Colors.scaleAlpha(p.accent(), a));
         }

         CFHGuiDraw.focusRing(g, x, y, w, h, this.focus() * a);
      }
   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      float a = this.effectiveAlpha();
      if (!(a <= 0.004F)) {
         boolean usable = this.usable();
         int x = this.getX();
         int y = this.drawnY();
         int w = this.getWidth();
         int h = this.getHeight();
         CFHGuiDraw.vanillaButton(g, x, y, w, h, this.lift(), usable, a);
         Rect win = this.window();
         CFHGuiDraw.rect(g, win.x(), win.y(), win.w(), win.h(), Colors.scaleAlpha(-1610612736, a));
         CFHGuiDraw.outline(g, win.x(), win.y(), win.w(), win.h(), Colors.scaleAlpha(-16777216, a), 0);
         CFHGuiDraw.outline(g, win.x() + 1, win.y() + 1, win.w() - 2, win.h() - 2, Colors.scaleAlpha(822083583, a), 0);
         int fg = usable ? -1 : -6250336;
         this.drawName(g, x, y + h - this.band(), w, this.band(), Colors.scaleAlpha(fg, a), true);
         if (this.isLocked()) {
            this.drawLock(g, x, y + h - this.band(), w, this.band(), Colors.scaleAlpha(fg, a));
         }

      }
   }

   private void drawName(GuiGraphics g, int x, int y, int w, int h, int color, boolean shadow) {
      int room = (w - 12) / this.textScale;
      FormattedCharSequence text = CFHGuiDraw.truncate(Theme.current().isVanilla() ? this.getMessage() : this.heading, room);
      int tw = CFHGuiDraw.font().width(text) * this.textScale;
      g.pose().pushPose();
      g.pose().translate((float)x + (float)(w - tw) / 2.0F, (float)y + (float)(h - 9 * this.textScale) / 2.0F + (this.textScale == 1 ? 0.0F : -1.0F), 0.0F);
      g.pose().scale((float)this.textScale, (float)this.textScale, 1.0F);
      g.drawString(CFHGuiDraw.font(), text, 0, 0, color, shadow);
      g.pose().popPose();
   }
}
