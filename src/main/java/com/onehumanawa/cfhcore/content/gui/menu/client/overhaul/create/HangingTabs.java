package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.create;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

final class HangingTabs extends CFHGuiWidget {
   static final int HEIGHT = 19;
   private final List<Component> labels;
   private final IntConsumer onChange;
   private final List<Anim> hovers = new ArrayList();
   private final Anim slide;
   private final int tabW;
   private final int tabsX;
   private int index;

   HangingTabs(int x, int y, int width, List<Component> labels, int selected, IntConsumer onChange) {
      super(x, y, width, 19, Component.empty());
      this.labels = List.copyOf(labels);
      this.onChange = onChange;
      this.index = Mth.clamp(selected, 0, Math.max(0, labels.size() - 1));
      this.slide = new Anim((float)this.index, 220, Ease.OUT_CUBIC);
      int widest = 40;

      for(Component c : labels) {
         widest = Math.max(widest, CFHGuiDraw.font().width(Fonts.heading(c)) + 22);
      }

      int n = Math.max(1, labels.size());
      this.tabW = Math.min(widest, Math.max(30, (width - 8) / n));
      this.tabsX = x + (width - this.tabW * n) / 2;

      for(int i = 0; i < n; ++i) {
         this.hovers.add(new Anim(0.0F, 150, Ease.OUT_CUBIC));
      }

      this.silent();
   }

   int index() {
      return this.index;
   }

   private int tabAt(double mx, double my) {
      if (!(my < (double)this.getY()) && !(my >= (double)(this.getY() + 19)) && !(mx < (double)this.tabsX)) {
         int i = (int)((mx - (double)this.tabsX) / (double)this.tabW);
         return i >= 0 && i < this.labels.size() ? i : -1;
      } else {
         return -1;
      }
   }

   private void choose(int i) {
      if (i >= 0 && i < this.labels.size() && i != this.index) {
         this.index = i;
         this.slide.set((float)i);
         CFHGuiSounds.tick();
         this.onChange.accept(i);
      }
   }

   public boolean isMouseOver(double mouseX, double mouseY) {
      return this.active && this.visible && this.tabAt(mouseX, mouseY) >= 0;
   }

   public void onClick(double mouseX, double mouseY) {
      super.onClick(mouseX, mouseY);
      this.choose(this.tabAt(mouseX, mouseY));
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (!this.isFocused()) {
         return false;
      } else if (keyCode == 263) {
         this.choose(Math.max(0, this.index - 1));
         return true;
      } else if (keyCode == 262) {
         this.choose(Math.min(this.labels.size() - 1, this.index + 1));
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.draw(g, mouseX, mouseY, false);
   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.draw(g, mouseX, mouseY, true);
   }

   private void draw(GuiGraphics g, int mouseX, int mouseY, boolean van) {
      Theme t = Theme.current();
      Palette p = t.palette();
      float a = this.effectiveAlpha();
      if (!(a <= 0.004F)) {
         Font font = CFHGuiDraw.font();
         int y = this.getY() + this.enterOffset();
         int h = 18;
         int line = van ? -9474193 : p.border();
         int lx = this.getX();
         int lw = this.getWidth();
         CFHGuiDraw.hgradient(g, lx, y, lw / 5, 1, Colors.scaleAlpha(Colors.withAlpha(line, 0), a), Colors.scaleAlpha(line, a));
         CFHGuiDraw.rect(g, lx + lw / 5, y, lw - 2 * (lw / 5), 1, Colors.scaleAlpha(line, a));
         CFHGuiDraw.hgradient(g, lx + lw - lw / 5, y, lw / 5, 1, Colors.scaleAlpha(line, a), Colors.scaleAlpha(Colors.withAlpha(line, 0), a));
         int hovered = this.isHovered() ? this.tabAt((double)mouseX, (double)mouseY) : -1;

         for(int i = 0; i < this.labels.size(); ++i) {
            int x = this.tabsX + i * this.tabW;
            Anim hover = (Anim)this.hovers.get(i);
            hover.set(i == hovered && i != this.index);
            float hv = hover.get();
            if (van) {
               g.fill(x + 1, y + 1, x + this.tabW - 1, y + 18, Colors.scaleAlpha(Colors.lerp(1610612736, -2144325584, hv), a));
               CFHGuiDraw.outline(g, x, y, this.tabW, 19, Colors.scaleAlpha(-16777216, a), 0);
            } else {
               CFHGuiDraw.rect(g, x + 1, y + 1, this.tabW - 2, 17, Colors.scaleAlpha(t.chrome(Colors.lerp(p.bg2(), p.surfaceHover(), hv)), a));
               CFHGuiDraw.vline(g, x, y + 1, 16, Colors.scaleAlpha(line, a));
               CFHGuiDraw.vline(g, x + this.tabW - 1, y + 1, 16, Colors.scaleAlpha(line, a));
               CFHGuiDraw.hline(g, x + 1, y + 18 - 1, this.tabW - 2, Colors.scaleAlpha(line, a));
            }
         }

         int sx = this.tabsX + Math.round(this.slide.get() * (float)this.tabW);
         int accent = van ? -1 : p.accent();
         if (van) {
            g.fill(sx + 1, y + 1, sx + this.tabW - 1, y + 18, Colors.scaleAlpha(1895825407, a));
         } else {
            CFHGuiDraw.vgradient(g, sx + 1, y + 1, this.tabW - 2, 17, Colors.scaleAlpha(Colors.withAlpha(accent, 24), a), Colors.scaleAlpha(Colors.withAlpha(accent, 88), a));
         }

         CFHGuiDraw.rect(g, sx + 1, y + 18 - 2, this.tabW - 2, 2, Colors.scaleAlpha(accent, a));

         for(int i = 0; i < this.labels.size(); ++i) {
            int x = this.tabsX + i * this.tabW;
            boolean sel = i == this.index;
            Component label = Fonts.heading((Component)this.labels.get(i));
            int fg = sel ? (van ? -1 : p.text()) : Colors.lerp(van ? -4144960 : p.textMuted(), van ? -1 : p.text(), ((Anim)this.hovers.get(i)).get());
            FormattedCharSequence cut = CFHGuiDraw.truncate(label, this.tabW - 8);
            g.drawString(font, cut, x + (this.tabW - font.width(cut)) / 2, y + 5, Colors.scaleAlpha(fg, a), van);
         }

         if (this.isFocused()) {
            CFHGuiDraw.focusRing(g, this.tabsX + this.index * this.tabW, y, this.tabW, 18, this.focus() * a);
         }

      }
   }
}
