package com.onehumanawa.cfhcore.content.gui.core.widget;

import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class CFHGuiSlider extends CFHGuiWidget {
   private static final int KNOB = 8;
   private static final int TRACK_H = 4;
   private static final int VALUE_GAP = 6;
   private final double min;
   private final double max;
   private final double step;
   private double value;
   private final DoubleFunction<String> format;
   private final DoubleConsumer onChange;
   private DoubleConsumer onCommit;
   private boolean dragging;
   private boolean compact;
   private boolean dirty;
   private int valueColumnMin;

   public CFHGuiSlider(int x, int y, int width, Component label, double min, double max, double step, double value, DoubleFunction<String> format, DoubleConsumer onChange) {
      super(x, y, width, 30, label);
      this.min = min;
      this.max = max;
      this.step = step;
      this.value = this.clamp(value);
      this.format = format != null ? format : (v) -> step >= (double)1.0F ? Integer.toString((int)Math.round(v)) : "%.2f".formatted(v);
      this.onChange = onChange;
   }

   public CFHGuiSlider compact(boolean compact) {
      this.compact = compact;
      this.setHeight(compact ? 20 : 30);
      return this;
   }

   public CFHGuiSlider onCommit(DoubleConsumer c) {
      this.onCommit = c;
      return this;
   }

   public double value() {
      return this.value;
   }

   public CFHGuiSlider setValue(double v) {
      this.value = this.clamp(v);
      return this;
   }

   public double min() {
      return this.min;
   }

   public double max() {
      return this.max;
   }

   private double clamp(double v) {
      if (this.step > (double)0.0F) {
         v = (double)Math.round((v - this.min) / this.step) * this.step + this.min;
      }

      return Mth.clamp(v, this.min, this.max);
   }

   private float fraction() {
      return this.max <= this.min ? 0.0F : (float)((this.value - this.min) / (this.max - this.min));
   }

   private void change(double nv) {
      if (nv != this.value) {
         this.value = nv;
         this.dirty = true;
         if (this.onChange != null) {
            this.onChange.accept(this.value);
         }

      }
   }

   private void commit() {
      if (this.dirty) {
         this.dirty = false;
         if (this.onCommit != null) {
            this.onCommit.accept(this.value);
         }

      }
   }

   private boolean inline() {
      return this.compact && this.getMessage().getString().isEmpty();
   }

   private int valueColumn() {
      return Math.max(this.valueColumnMin, this.naturalValueColumn());
   }

   public int naturalValueColumn() {
      int widest = Math.max(CFHGuiDraw.width((String)this.format.apply(this.value)), Math.max(CFHGuiDraw.width((String)this.format.apply(this.min)), CFHGuiDraw.width((String)this.format.apply(this.max))));
      return Math.max(CFHGuiDraw.width("000%"), widest);
   }

   public boolean valueBeside() {
      return this.inline() && !Theme.current().isVanilla();
   }

   public CFHGuiSlider valueColumnAtLeast(int width) {
      this.valueColumnMin = Math.max(0, width);
      return this;
   }

   private int trackWidth() {
      return this.inline() && !Theme.current().isVanilla() ? Math.max(24, this.getWidth() - this.valueColumn() - 6) : this.getWidth();
   }

   private void setFromMouse(double mouseX) {
      int tx = this.getX();
      int tw = this.trackWidth();
      double f = Mth.clamp((mouseX - (double)tx - (double)4.0F) / (double)Math.max(1, tw - 8), (double)0.0F, (double)1.0F);
      this.change(this.clamp(this.min + f * (this.max - this.min)));
   }

   public void onClick(double mouseX, double mouseY) {
      super.onClick(mouseX, mouseY);
      this.dragging = true;
      this.setFromMouse(mouseX);
   }

   protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
      if (this.dragging) {
         this.setFromMouse(mouseX);
      }

   }

   public void onRelease(double mouseX, double mouseY) {
      super.onRelease(mouseX, mouseY);
      this.dragging = false;
      this.commit();
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.active && this.visible) {
         double s = this.step > (double)0.0F ? this.step : (this.max - this.min) / (double)100.0F;
         switch (keyCode) {
            case 262:
               this.change(this.clamp(this.value + s));
               break;
            case 263:
               this.change(this.clamp(this.value - s));
               break;
            case 264:
            case 265:
            case 266:
            case 267:
            default:
               return false;
            case 268:
               this.change(this.min);
               break;
            case 269:
               this.change(this.max);
         }

         this.commit();
         return true;
      } else {
         return false;
      }
   }

   public void playDownSound(SoundManager handler) {
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
         int fg = Colors.scaleAlpha(this.active ? p.text() : p.textDim(), a);
         String val = (String)this.format.apply(this.value);
         int valW = CFHGuiDraw.width(val);
         boolean inline = this.inline();
         int tw = this.trackWidth();
         int trackY = inline ? y + (h - 4) / 2 : (this.compact ? y + h - 4 - 3 : y + 10 + (h - 10 - 4) / 2);
         int textY = inline ? CFHGuiDraw.textY(y, h) : (this.compact ? y : y + 1);
         if (!inline) {
            g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(this.getMessage(), w - valW - 8), x, textY, fg, false);
         }

         g.drawString(CFHGuiDraw.font(), val, x + w - valW, textY, Colors.scaleAlpha(this.active ? p.textMuted() : p.textDim(), a), false);
         int fillW = 4 + Math.round((float)(tw - 8) * this.fraction());
         CFHGuiDraw.pixelRound(g, x, trackY, tw, 4, Colors.scaleAlpha(this.active ? p.surfaceActive() : Colors.withAlpha(p.surfaceActive(), 128), a), 1);
         int fillColor = this.active ? Colors.lerp(p.accent(), p.accentHover(), this.hover()) : p.textDim();
         CFHGuiDraw.pixelRound(g, x, trackY, Math.max(4, fillW), 4, Colors.scaleAlpha(fillColor, a), 1);
         int ks = 8 + (this.active ? Math.round(2.0F * Math.max(this.hover(), this.press())) : 0);
         int kcx = x + fillW;
         CFHGuiDraw.pixelRound(g, kcx - ks / 2, trackY + 2 - ks / 2, ks, ks, Colors.scaleAlpha(this.active ? p.text() : p.textMuted(), a), 2);
         if (this.dragging || this.press() > 0.01F) {
            CFHGuiDraw.outline(g, kcx - ks / 2, trackY + 2 - ks / 2, ks, ks, Colors.scaleAlpha(p.accent(), a), 2);
         }

         CFHGuiDraw.focusRing(g, x, trackY - 2, tw, 8, this.focus() * a);
      }
   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      float a = this.effectiveAlpha();
      if (!(a <= 0.004F)) {
         int x = this.getX();
         int y = this.getY() + this.enterOffset();
         int w = this.getWidth();
         int sh = 20;
         int sy = this.compact ? y : y + (this.getHeight() - 20) / 2;
         float lift = this.active ? Math.max(this.hover(), this.focus()) : 0.0F;
         int kx = x + Math.round((float)(w - 8) * this.fraction());
         CFHGuiDraw.vanillaSlider(g, x, sy, w, 20, kx, lift, this.active, a);
         String val = (String)this.format.apply(this.value);
         int fg = Colors.scaleAlpha(this.active ? -1 : -6250336, a);
         Component text = Component.empty().append(this.getMessage()).append(": " + val);
         CFHGuiDraw.drawScrollingTextCentered(g, text, x + 2, CFHGuiDraw.textY(sy, 20), w - 4, fg, true);
      }
   }

   protected void updateWidgetNarration(NarrationElementOutput out) {
      out.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.slider", new Object[]{Component.empty().append(this.getMessage()).append(": " + (String)this.format.apply(this.value))}));
      if (this.active) {
         out.add(NarratedElementType.USAGE, Component.translatable(this.isFocused() ? "narration.slider.usage.focused" : "narration.slider.usage.hovered"));
      }

   }
}
