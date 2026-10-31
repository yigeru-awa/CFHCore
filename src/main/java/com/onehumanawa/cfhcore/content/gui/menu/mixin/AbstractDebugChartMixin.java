package com.onehumanawa.cfhcore.content.gui.menu.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.onehumanawa.cfhcore.content.gui.menu.client.debug.DebugCharts;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.debugchart.AbstractDebugChart;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({AbstractDebugChart.class})
public abstract class AbstractDebugChartMixin {
   @WrapMethod(
      method = {"drawChart"},
      require = 0
   )
   private void cfhgui$chart(GuiGraphics graphics, int x, int width, Operation<Void> original) {
      if (!DebugCharts.begin((AbstractDebugChart)(Object)this, graphics, x, width)) {
         original.call(new Object[]{graphics, x, width});
      } else {
         try {
            original.call(new Object[]{graphics, x, width});
         } finally {
            DebugCharts.end(graphics);
         }

      }
   }

   @WrapOperation(
      method = {"drawChart"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiGraphics;fill(Lnet/minecraft/client/renderer/RenderType;IIIII)V"
)},
      require = 0
   )
   private void cfhgui$plate(GuiGraphics graphics, RenderType type, int x0, int y0, int x1, int y1, int color, Operation<Void> original) {
      if (!DebugCharts.plate(graphics, x0, y0, x1, y1)) {
         original.call(new Object[]{graphics, type, x0, y0, x1, y1, color});
      }

   }

   @WrapOperation(
      method = {"drawChart"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiGraphics;hLine(Lnet/minecraft/client/renderer/RenderType;IIII)V"
)},
      require = 0
   )
   private void cfhgui$hLine(GuiGraphics graphics, RenderType type, int minX, int maxX, int y, int color, Operation<Void> original) {
      if (!DebugCharts.framing()) {
         original.call(new Object[]{graphics, type, minX, maxX, y, color});
      }

   }

   @WrapOperation(
      method = {"drawChart"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiGraphics;vLine(Lnet/minecraft/client/renderer/RenderType;IIII)V"
)},
      require = 0
   )
   private void cfhgui$vLine(GuiGraphics graphics, RenderType type, int x, int minY, int maxY, int color, Operation<Void> original) {
      if (!DebugCharts.framing()) {
         original.call(new Object[]{graphics, type, x, minY, maxY, color});
      }

   }

   @WrapOperation(
      method = {"drawChart"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)I"
)},
      require = 0
   )
   private int cfhgui$number(GuiGraphics graphics, Font font, String text, int x, int y, int color, Operation<Integer> original) {
      return DebugCharts.framing() ? DebugCharts.number(graphics, font, text, x, y) : (Integer)original.call(new Object[]{graphics, font, text, x, y, color});
   }

   @WrapOperation(
      method = {"drawChart"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiGraphics;drawCenteredString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"
)},
      require = 0
   )
   private void cfhgui$average(GuiGraphics graphics, Font font, String text, int x, int y, int color, Operation<Void> original) {
      if (DebugCharts.framing()) {
         DebugCharts.number(graphics, font, text, x - font.width(text) / 2, y);
      } else {
         original.call(new Object[]{graphics, font, text, x, y, color});
      }

   }

   @WrapMethod(
      method = {"getSampleColor(DDIDIDI)I"},
      require = 0
   )
   private int cfhgui$sampleColor(double value, double low, int lowColor, double mid, int midColor, double high, int highColor, Operation<Integer> original) {
      return DebugCharts.recolours(lowColor, midColor, highColor) ? DebugCharts.sampleColor(value, mid, high) : (Integer)original.call(new Object[]{value, low, lowColor, mid, midColor, high, highColor});
   }

   @WrapOperation(
      method = {"drawStringWithShade"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiGraphics;fill(Lnet/minecraft/client/renderer/RenderType;IIIII)V"
)},
      require = 0
   )
   private void cfhgui$shade(GuiGraphics graphics, RenderType type, int x0, int y0, int x1, int y1, int color, Operation<Void> original) {
      original.call(new Object[]{graphics, type, x0, y0, x1, y1, DebugCharts.framing() ? DebugCharts.shade() : color});
   }
}
