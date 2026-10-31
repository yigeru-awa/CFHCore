package com.onehumanawa.cfhcore.content.gui.core.widget;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
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

public class CFHGuiCheckbox extends CFHGuiWidget {
   private static final int BOX = 12;
   private static final int VANILLA_BOX = 17;
   private boolean value;
   private final Anim check;
   private Consumer<Boolean> onChange;

   public CFHGuiCheckbox(int x, int y, int width, Component label, boolean value, Consumer<Boolean> onChange) {
      super(x, y, width, 16, label);
      this.check = new Anim(0.0F, 140, Ease.OUT_BACK);
      this.value = value;
      this.check.snap(value ? 1.0F : 0.0F);
      this.onChange = onChange;
   }

   public boolean value() {
      return this.value;
   }

   public CFHGuiCheckbox setValue(boolean v) {
      this.value = v;
      this.check.set(v ? 1.0F : 0.0F);
      return this;
   }

   public CFHGuiCheckbox onChange(Consumer<Boolean> c) {
      this.onChange = c;
      return this;
   }

   public void onClick(double mouseX, double mouseY) {
      super.onClick(mouseX, mouseY);
      this.toggle();
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (!this.active || !this.visible || keyCode != 257 && keyCode != 32 && keyCode != 335) {
         return false;
      } else {
         this.toggle();
         return true;
      }
   }

   private void toggle() {
      this.value = !this.value;
      this.check.set(this.value ? 1.0F : 0.0F);
      CFHGuiSounds.tick();
      if (this.onChange != null) {
         this.onChange.accept(this.value);
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
         int h = this.getHeight();
         float k = Math.max(0.0F, Math.min(1.0F, this.check.get()));
         int by = y + (h - 12) / 2;
         int fill = Colors.lerp(Colors.lerp(p.surface(), p.surfaceHover(), this.hover()), p.accent(), k);
         int border = Colors.lerp(p.borderStrong(), p.accentHover(), k);
         if (!this.active) {
            fill = Colors.withAlpha(fill, 112);
            border = Colors.withAlpha(border, 112);
         }

         fill = Colors.brighten(fill, -0.12F * this.press());
         CFHGuiDraw.pixelRound(g, x, by, 12, 12, Colors.scaleAlpha(fill, a), 2);
         CFHGuiDraw.outline(g, x, by, 12, 12, Colors.scaleAlpha(border, a), 2);
         if (k > 0.05F) {
            int s = k < 0.5F ? 6 : 8;
            Icons.draw(g, Icon.CHECK, x + (12 - s) / 2, by + (12 - s) / 2, s, Colors.scaleAlpha(p.accentText(), a * k));
         }

         CFHGuiDraw.focusRing(g, x, by, 12, 12, this.focus() * a);
         int fg = Colors.scaleAlpha(this.active ? Colors.lerp(p.text(), -1, this.hover() * 0.3F) : p.textDim(), a);
         g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(this.getMessage(), this.getWidth() - 12 - 6), x + 12 + 6, CFHGuiDraw.textY(y, h), fg, false);
      }
   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      float a = this.effectiveAlpha();
      if (!(a <= 0.004F)) {
         int x = this.getX();
         int y = this.getY() + this.enterOffset();
         int h = this.getHeight();
         int by = y + (h - 17) / 2;
         float lift = this.active ? Math.max(this.hover(), this.focus()) : 0.0F;
         CFHGuiDraw.vanillaCheckbox(g, x, by, 17, this.value, lift, a);
         int fg = Colors.scaleAlpha(this.active ? -1 : -6250336, a);
         g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(this.getMessage(), this.getWidth() - 17 - 4), x + 17 + 4, CFHGuiDraw.textY(y, h), fg, true);
      }
   }

   protected void updateWidgetNarration(NarrationElementOutput out) {
      out.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.button", new Object[]{Component.empty().append(this.getMessage()).append(": ").append(this.value ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF)}));
      if (this.active) {
         out.add(NarratedElementType.USAGE, Component.translatable(this.isFocused() ? "narration.checkbox.usage.focused" : "narration.checkbox.usage.hovered"));
      }

   }
}
