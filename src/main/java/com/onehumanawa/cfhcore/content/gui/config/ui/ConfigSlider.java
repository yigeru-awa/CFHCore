package com.onehumanawa.cfhcore.content.gui.config.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSlider;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class ConfigSlider extends CFHGuiSlider {
   private final double min;
   private final double max;
   private final DoubleFunction<String> format;

   public ConfigSlider(int x, int y, int width, double min, double max, double step, double value, DoubleFunction<String> format, DoubleConsumer onChange) {
      super(x, y, width, Component.empty(), min, max, step, value, format, onChange);
      this.min = min;
      this.max = max;
      this.format = format;
      this.compact(true);
   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      float a = this.effectiveAlpha();
      int x = this.getX();
      int y = this.getY() + this.enterOffset();
      int w = this.getWidth();
      int h = 20;
      RenderSystem.enableBlend();
      g.setColor(1.0F, 1.0F, 1.0F, a);
      boolean hl = this.hover() > 0.5F || this.focus() > 0.5F;
      g.blitSprite(hl ? CFHGuiDraw.SLIDER_HIGHLIGHTED : CFHGuiDraw.SLIDER, x, y, w, 20);
      float fraction = this.max <= this.min ? 0.0F : (float)((this.value() - this.min) / (this.max - this.min));
      int kx = x + Math.round((float)(w - 8) * Math.max(0.0F, Math.min(1.0F, fraction)));
      g.blitSprite(hl ? CFHGuiDraw.SLIDER_HANDLE_HIGHLIGHTED : CFHGuiDraw.SLIDER_HANDLE, kx, y, 8, 20);
      g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.disableBlend();
      int fg = Colors.scaleAlpha(this.active ? -1 : -6250336, a);
      CFHGuiDraw.textCentered(g, Component.literal((String)this.format.apply(this.value())), x + w / 2, y + 6, fg);
   }
}
