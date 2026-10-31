package com.onehumanawa.cfhcore.content.gui.core.mixin;

import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.screen.reskin.ReskinDraw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({TooltipRenderUtil.class})
public abstract class TooltipRenderUtilMixin {
   @Inject(
      method = {"renderTooltipBackground(Lnet/minecraft/client/gui/GuiGraphics;IIIII)V"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private static void cfhgui$background(GuiGraphics g, int x, int y, int w, int h, int z, CallbackInfo ci) {
      if (Reskin.tooltip()) {
         ci.cancel();
         ReskinDraw.tooltipBackground(g, x, y, w, h, z);
      }
   }
}
