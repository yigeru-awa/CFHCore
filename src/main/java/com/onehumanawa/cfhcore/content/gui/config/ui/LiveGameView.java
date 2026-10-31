package com.onehumanawa.cfhcore.content.gui.config.ui;

import com.onehumanawa.cfhcore.content.gui.core.client.render.GameView;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class LiveGameView extends CFHGuiWidget {
   public static final int BAR_H = 20;
   private static final int INSET = 3;
   private final boolean minimised;
   private final Runnable onToggle;
   private final Anim fade;

   public LiveGameView(int x, int y, int width, int height, boolean minimised, Runnable onToggle) {
      super(x, y, width, height, Component.translatable("cfhgui_config.live.title"));
      this.fade = new Anim(0.0F, 260, Ease.OUT_CUBIC);
      this.minimised = minimised;
      this.onToggle = onToggle;
      this.tip(Component.translatable(minimised ? "cfhgui_config.live.expand" : "cfhgui_config.live.minimise"));
      this.silent();
   }

   public static int heightFor(Rect area, boolean minimised) {
      return minimised ? 20 : Math.max(60, Math.min(area.h() * 2 / 5, area.w() * 9 / 32));
   }

   public void onClick(double mouseX, double mouseY) {
      super.onClick(mouseX, mouseY);
      this.onToggle.run();
   }

   protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.draw(g, false);
   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.draw(g, true);
   }

   private void draw(GuiGraphics g, boolean vanilla) {
      Theme t = Theme.current();
      Palette p = t.palette();
      float a = this.effectiveAlpha();
      int x = this.getX();
      int y = this.getY() + this.enterOffset();
      int w = this.getWidth();
      int h = this.getHeight();
      CFHGuiDraw.floatingPanel(g, x, y, w, h, a);
      int text = vanilla ? -1 : p.text();
      int muted = vanilla ? -6250336 : p.textMuted();
      if (this.minimised) {
         Icons.draw(g, Icon.VIDEO, x + 6, y + (h - 12) / 2, 12, Colors.scaleAlpha(vanilla ? -2039584 : p.accentInk(), a));
         g.drawString(CFHGuiDraw.font(), this.getMessage(), x + 22, CFHGuiDraw.textY(y, h), Colors.scaleAlpha(text, a), vanilla);
         Component hint = Component.translatable("cfhgui_config.live.expand");
         int hw = CFHGuiDraw.width(hint);
         if (x + 22 + CFHGuiDraw.width(this.getMessage()) + 12 + hw < x + w - 24) {
            g.drawString(CFHGuiDraw.font(), hint, x + w - 22 - hw, CFHGuiDraw.textY(y, h), Colors.scaleAlpha(muted, a * (this.hover() * 0.6F + 0.4F)), vanilla);
         }

         Icons.draw(g, Icon.CHEVRON_DOWN, x + w - 16, y + (h - 10) / 2, 10, Colors.scaleAlpha(muted, a));
         CFHGuiDraw.focusRing(g, x, y, w, h, this.focus() * a);
      } else {
         GameView.request();
         int ix = x + 3;
         int iy = y + 3;
         int iw = w - 6;
         int ih = h - 6;
         this.fade.set(GameView.hasFrame());
         float fa = this.fade.get() * a;
         if (fa > 0.004F) {
            g.enableScissor(ix, iy, ix + iw, iy + ih);
            GameView.drawCover(g, ix, iy, iw, ih, fa);
            g.disableScissor();
         } else {
            CFHGuiDraw.textCentered(g, Component.translatable("cfhgui_config.live.waiting"), x + w / 2, y + h / 2 - 4, Colors.scaleAlpha(muted, a), vanilla);
         }

         int cw = CFHGuiDraw.width(this.getMessage()) + 24;
         int ch = 16;
         int cx = ix + 6;
         int cy = iy + 6;
         CFHGuiDraw.pixelRound(g, cx, cy, cw, 16, Colors.scaleAlpha(vanilla ? -1073741824 : Colors.withAlpha(p.bg(), 216), a), vanilla ? 0 : t.radius());
         Icons.draw(g, Icon.VIDEO, cx + 5, cy + 3, 10, Colors.scaleAlpha(vanilla ? -2039584 : p.accentInk(), a));
         g.drawString(CFHGuiDraw.font(), this.getMessage(), cx + 19, CFHGuiDraw.textY(cy, 16), Colors.scaleAlpha(text, a), vanilla);
         int bx = ix + iw - 22;
         int by = iy + 6;
         CFHGuiDraw.pixelRound(g, bx, by, 16, 16, Colors.scaleAlpha(vanilla ? -1073741824 : Colors.withAlpha(p.bg(), 216), a * (0.6F + 0.4F * this.hover())), vanilla ? 0 : t.radius());
         Icons.draw(g, Icon.CHEVRON_UP, bx + 3, by + 3, 10, Colors.scaleAlpha(this.hover() > 0.5F ? text : muted, a));
         CFHGuiDraw.focusRing(g, x, y, w, h, this.focus() * a);
      }
   }
}
