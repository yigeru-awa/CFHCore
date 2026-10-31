package com.onehumanawa.cfhcore.content.gui.core.widget;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class CFHGuiToggle extends CFHGuiWidget {
   public static final int SWITCH_W = 22;
   public static final int SWITCH_H = 12;
   private boolean value;
   private final Anim knob;
   private Consumer<Boolean> onChange;
   private boolean labelLeft;

   public CFHGuiToggle(int x, int y, int width, Component label, boolean value, Consumer<Boolean> onChange) {
      super(x, y, width, 20, label);
      this.knob = new Anim(0.0F, 160, Ease.OUT_BACK);
      this.labelLeft = true;
      this.value = value;
      this.knob.snap(value ? 1.0F : 0.0F);
      this.onChange = onChange;
   }

   public boolean value() {
      return this.value;
   }

   public CFHGuiToggle setValue(boolean v) {
      this.value = v;
      this.knob.set(v ? 1.0F : 0.0F);
      return this;
   }

   public CFHGuiToggle onChange(Consumer<Boolean> c) {
      this.onChange = c;
      return this;
   }

   public CFHGuiToggle switchFirst() {
      this.labelLeft = false;
      return this;
   }

   public void onClick(double mouseX, double mouseY) {
      super.onClick(mouseX, mouseY);
      this.toggle();
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.active && this.visible) {
         if (keyCode != 257 && keyCode != 32 && keyCode != 335) {
            if (keyCode == 263 && this.value) {
               this.toggle();
               return true;
            } else if (keyCode == 262 && !this.value) {
               this.toggle();
               return true;
            } else {
               return false;
            }
         } else {
            this.toggle();
            return true;
         }
      } else {
         return false;
      }
   }

   private void toggle() {
      this.value = !this.value;
      this.knob.set(this.value ? 1.0F : 0.0F);
      CFHGuiSounds.tick();
      if (this.onChange != null) {
         this.onChange.accept(this.value);
      }

   }

   public void playDownSound(SoundManager handler) {
   }

   private int switchX() {
      return this.labelLeft ? this.getX() + this.getWidth() - 22 : this.getX();
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
         float k = this.knob.get();
         int sx = this.switchX();
         int sy = y + (h - 12) / 2;
         int track = Colors.lerp(Colors.lerp(p.surfaceActive(), p.surfaceHover(), this.hover()), p.accent(), k);
         int trackBorder = Colors.lerp(p.borderStrong(), p.accentHover(), k);
         int knobColor = Colors.lerp(p.textMuted(), p.accentText(), k);
         if (!this.active) {
            track = Colors.withAlpha(track, 112);
            trackBorder = Colors.withAlpha(trackBorder, 112);
            knobColor = p.textDim();
         }

         CFHGuiDraw.pixelRound(g, sx, sy, 22, 12, Colors.scaleAlpha(track, a), 3);
         CFHGuiDraw.outline(g, sx, sy, 22, 12, Colors.scaleAlpha(trackBorder, a), 3);
         int kx = sx + 2 + Math.round(10.0F * clamp01(k));
         CFHGuiDraw.pixelRound(g, kx, sy + 2, 8, 8, Colors.scaleAlpha(knobColor, a), 2);
         CFHGuiDraw.focusRing(g, sx, sy, 22, 12, this.focus() * a);
         int fg = Colors.scaleAlpha(this.active ? Colors.lerp(p.text(), -1, this.hover() * 0.3F) : p.textDim(), a);
         int tx = this.labelLeft ? x : x + 22 + 8;
         int tw = w - 22 - 8;
         g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(this.getMessage(), tw), tx, CFHGuiDraw.textY(y, h), fg, false);
      }
   }

   private static float clamp01(float k) {
      return Math.max(0.0F, Math.min(1.0F, k));
   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      float a = this.effectiveAlpha();
      if (!(a <= 0.004F)) {
         int x = this.getX();
         int y = this.getY() + this.enterOffset();
         int w = this.getWidth();
         int h = this.getHeight();
         float k = clamp01(this.knob.get());
         int sx = this.switchX() - (this.labelLeft ? 2 : 0);
         int sy = y + (h - 12) / 2;
         int tw = 24;
         float lift = this.active ? Math.max(this.hover(), this.focus()) : 0.0F;
         int kx = sx + 1 + Math.round(14.0F * k);
         CFHGuiDraw.vanillaSlider(g, sx, sy, 24, 12, kx, lift, this.active, a);
         int on = Colors.scaleAlpha(Colors.withAlpha(t.palette().success(), 144), a * k);
         g.fill(sx + 1, sy + 1, kx, sy + 12 - 1, on);
         int fg = Colors.scaleAlpha(this.active ? -1 : -6250336, a);
         int tx = this.labelLeft ? x : x + 24 + 8;
         g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(this.getMessage(), w - 24 - 8), tx, CFHGuiDraw.textY(y, h), fg, true);
      }
   }

   protected void updateWidgetNarration(NarrationElementOutput out) {
      out.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.button", new Object[]{Component.empty().append(this.getMessage()).append(": ").append(this.value ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF)}));
      if (this.active) {
         out.add(NarratedElementType.USAGE, Component.translatable(this.isFocused() ? "narration.button.usage.focused" : "narration.button.usage.hovered"));
      }

   }
}
