package com.onehumanawa.cfhcore.content.gui.core.widget;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public class CFHGuiSegmented<T> extends CFHGuiWidget {
   private final List<T> options;
   private final Function<T, Component> labeler;
   private int index;
   private final Consumer<T> onChange;
   private final Anim slide;
   private int hoverIdx;
   private Predicate<T> disabled;
   private Function<T, Component> optionTip;

   public CFHGuiSegmented(int x, int y, int width, List<T> options, T value, Function<T, Component> labeler, Consumer<T> onChange) {
      super(x, y, width, 20, Component.empty());
      this.slide = new Anim(0.0F, 180, Ease.OUT_CUBIC);
      this.hoverIdx = -1;
      this.disabled = (t) -> false;
      this.optionTip = (t) -> null;
      this.options = new ArrayList(options);
      this.labeler = labeler;
      this.index = Math.max(0, options.indexOf(value));
      this.slide.snap((float)this.index);
      this.onChange = onChange;
   }

   public T value() {
      return (T)this.options.get(this.index);
   }

   public CFHGuiSegmented<T> disable(Predicate<T> p) {
      this.disabled = p == null ? (t) -> false : p;
      return this;
   }

   public CFHGuiSegmented<T> optionTip(Function<T, Component> f) {
      this.optionTip = f == null ? (t) -> null : f;
      return this;
   }

   public boolean isDisabled(T option) {
      return this.disabled.test(option);
   }

   public int index() {
      return this.index;
   }

   public CFHGuiSegmented<T> setValue(T v) {
      int i = this.options.indexOf(v);
      if (i >= 0) {
         this.index = i;
         this.slide.set((float)i);
      }

      return this;
   }

   private int count() {
      return Math.max(1, this.options.size());
   }

   private int segX(int i) {
      return this.getX() + i * this.getWidth() / this.count();
   }

   private int segW(int i) {
      return this.segX(i + 1) - this.segX(i);
   }

   private int segAt(double mx) {
      int i = (int)((mx - (double)this.getX()) * (double)this.count() / (double)Math.max(1, this.getWidth()));
      return i >= 0 && i < this.options.size() ? i : -1;
   }

   private void choose(int i) {
      if (i >= 0 && i < this.options.size() && i != this.index && !this.disabled.test(this.options.get(i))) {
         this.index = i;
         this.slide.set((float)i);
         CFHGuiSounds.tick();
         if (this.onChange != null) {
            this.onChange.accept(this.options.get(i));
         }

      }
   }

   public void onClick(double mouseX, double mouseY) {
      super.onClick(mouseX, mouseY);
      this.choose(this.segAt(mouseX));
   }

   public void playDownSound(SoundManager handler) {
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.active && this.visible) {
         if (keyCode == 263) {
            this.choose(this.nextEnabled(this.index, -1));
            return true;
         } else if (keyCode == 262) {
            this.choose(this.nextEnabled(this.index, 1));
            return true;
         } else if (keyCode == 268) {
            this.choose(this.nextEnabled(-1, 1));
            return true;
         } else if (keyCode == 269) {
            this.choose(this.nextEnabled(this.options.size(), -1));
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private int nextEnabled(int from, int dir) {
      for(int i = from + dir; i >= 0 && i < this.options.size(); i += dir) {
         if (!this.disabled.test(this.options.get(i))) {
            return i;
         }
      }

      return -1;
   }

   private void hoverTip() {
      if (this.hoverIdx >= 0) {
         Component tip = (Component)this.optionTip.apply(this.options.get(this.hoverIdx));
         if (tip != null) {
            CFHGuiTooltips.request(tip, this);
         }

      }
   }

   private int[] highlightRect() {
      float s = Math.max(0.0F, Math.min((float)(this.options.size() - 1), this.slide.get()));
      int i0 = (int)Math.floor((double)s);
      int i1 = Math.min(this.options.size() - 1, i0 + 1);
      float f = s - (float)i0;
      int hx = Math.round((float)this.segX(i0) + (float)(this.segX(i1) - this.segX(i0)) * f);
      int hw = Math.round((float)this.segW(i0) + (float)(this.segW(i1) - this.segW(i0)) * f);
      return new int[]{hx, hw};
   }

   protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      float a = this.effectiveAlpha();
      if (!(a <= 0.004F)) {
         int x = this.getX();
         int y = this.getY() + this.enterOffset();
         int w = this.getWidth();
         int h = this.getHeight();
         this.hoverIdx = this.isHovered() && this.active ? this.segAt((double)mouseX) : -1;
         this.hoverTip();
         CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(t.field(p.bg2()), a), t.radius());
         CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(t.fieldEdge(this.active ? p.border() : Colors.withAlpha(p.border(), 128)), a), t.radius());
         if (this.hoverIdx >= 0 && this.hoverIdx != this.index && !this.disabled.test(this.options.get(this.hoverIdx))) {
            CFHGuiDraw.pixelRound(g, this.segX(this.hoverIdx) + 2, y + 2, this.segW(this.hoverIdx) - 4, h - 4, Colors.scaleAlpha(Theme.setIn() ? t.field(p.surfaceHover()) : p.surfaceHover(), a * (Theme.setIn() ? 1.0F : 0.6F)), Math.max(0, t.radius() - 1));
         }

         int[] hl = this.highlightRect();
         int inner = Math.max(0, t.radius() - 1);
         CFHGuiDraw.pixelRound(g, hl[0] + 2, y + 2, hl[1] - 4, h - 4, Colors.scaleAlpha(t.control(this.active ? p.surfaceActive() : p.surface()), a), inner);
         CFHGuiDraw.outline(g, hl[0] + 2, y + 2, hl[1] - 4, h - 4, Colors.scaleAlpha(t.controlEdge(this.active ? p.borderStrong() : p.border()), a), inner);
         CFHGuiDraw.rect(g, hl[0] + 2 + inner, y + h - 3, hl[1] - 4 - inner * 2, 1, Colors.scaleAlpha(p.accent(), a * (this.active ? 1.0F : 0.4F)));

         for(int i = 0; i < this.options.size(); ++i) {
            int sx = this.segX(i);
            int sw = this.segW(i);
            boolean off = this.disabled.test(this.options.get(i));
            int fg = this.active && !off ? (i == this.index ? p.text() : (i == this.hoverIdx ? p.textMuted() : p.textDim())) : Colors.withAlpha(p.textDim(), off ? 160 : 255);
            FormattedCharSequence seq = CFHGuiDraw.truncate((Component)this.labeler.apply(this.options.get(i)), sw - 8);
            g.drawString(CFHGuiDraw.font(), seq, sx + (sw - CFHGuiDraw.width(seq)) / 2, CFHGuiDraw.textY(y, h), Colors.scaleAlpha(fg, a), false);
         }

         CFHGuiDraw.focusRing(g, x, y, w, h, this.focus() * a);
      }
   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      float a = this.effectiveAlpha();
      if (!(a <= 0.004F)) {
         int x = this.getX();
         int y = this.getY() + this.enterOffset();
         int h = this.getHeight();
         this.hoverIdx = this.isHovered() && this.active ? this.segAt((double)mouseX) : -1;
         this.hoverTip();

         for(int i = 0; i < this.options.size(); ++i) {
            int sx = this.segX(i);
            int sw = this.segW(i);
            boolean off = this.disabled.test(this.options.get(i));
            float lift = this.active && !off ? (i == this.index ? 1.0F : (i == this.hoverIdx ? 0.6F : (i == this.index && this.focus() > 0.0F ? this.focus() : 0.0F))) : 0.0F;
            CFHGuiDraw.vanillaButton(g, sx, y, sw, h, lift, this.active && !off, a);
            if (i == this.index) {
               g.fill(sx + 1, y + 1, sx + sw - 1, y + h - 1, Colors.scaleAlpha(1073741824, a));
            }

            FormattedCharSequence seq = CFHGuiDraw.truncate((Component)this.labeler.apply(this.options.get(i)), sw - 8);
            int fg = this.active && !off ? (i == this.index ? -96 : -2039584) : -6250336;
            g.drawString(CFHGuiDraw.font(), seq, sx + (sw - CFHGuiDraw.width(seq)) / 2, CFHGuiDraw.textY(y, h), Colors.scaleAlpha(fg, a), true);
         }

         if (this.focus() > 0.5F) {
            CFHGuiDraw.outline(g, this.segX(this.index), y, this.segW(this.index), h, Colors.scaleAlpha(-1, a), 0);
         }

      }
   }

   protected void updateWidgetNarration(NarrationElementOutput out) {
      out.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.button", new Object[]{this.labeler.apply(this.options.get(this.index))}));
      if (this.active) {
         out.add(NarratedElementType.USAGE, Component.translatable(this.isFocused() ? "narration.slider.usage.focused" : "narration.slider.usage.hovered"));
      }

   }
}
