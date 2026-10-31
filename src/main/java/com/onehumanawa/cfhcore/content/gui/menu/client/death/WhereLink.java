package com.onehumanawa.cfhcore.content.gui.menu.client.death;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

final class WhereLink extends CFHGuiButton {
   static final int HEIGHT = 11;
   private static final int GLYPH = 8;
   private static final int GAP = 3;
   private static final int DONE_MS = 1400;
   private long copiedMs;

   WhereLink(int x, int y, int w, Component words, Runnable copy) {
      super(x, y, w, 11, words, (Runnable)null);
      this.onPress(() -> {
         copy.run();
         this.copiedMs = Clock.nowMs();
      });
      this.tip(Component.translatable("chat.copy.click"));
   }

   static int width(Font font, Component words) {
      return font.width(words) + 3 + 8;
   }

   protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Palette p = Theme.current().palette();
      this.draw(g, p.textMuted(), p.text(), p.textDim(), p.success(), false);
   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.draw(g, -3618616, -1, -7303024, -11141291, true);
   }

   private void draw(GuiGraphics g, int rest, int over, int glyph, int done, boolean shadow) {
      float a = this.effectiveAlpha();
      if (!(a <= 0.02F)) {
         float lift = Math.max(this.hover(), this.focus());
         int x = this.getX();
         int y = this.getY() + this.enterOffset();
         int w = this.getWidth();
         int h = this.getHeight();
         int fg = Colors.lerp(rest, over, lift);
         FormattedCharSequence words = CFHGuiDraw.truncate(this.getMessage(), w - 3 - 8);
         int wordsW = CFHGuiDraw.width(words);
         g.drawString(CFHGuiDraw.font(), words, x, y + 1, Colors.scaleAlpha(fg, a), shadow);
         if (lift > 0.05F) {
            CFHGuiDraw.hline(g, x, y + h - 1, wordsW, Colors.scaleAlpha(fg, a * lift * 0.7F));
         }

         boolean copied = this.copiedMs != 0L && Clock.nowMs() - this.copiedMs < 1400L;
         Icons.draw(g, copied ? Icon.CHECK : Icon.COPY, x + wordsW + 3, y + 1, 8, Colors.scaleAlpha(copied ? done : Colors.lerp(glyph, over, lift), a));
         CFHGuiDraw.focusRing(g, x - 2, y - 1, w + 4, h + 2, this.focus() * a);
      }
   }
}
