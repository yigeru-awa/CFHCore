package com.onehumanawa.cfhcore.content.gui.core.compat.xaero.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.onehumanawa.cfhcore.content.gui.core.compat.xaero.XaeroScreenDraw;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(
   targets = {"xaero.lib.client.gui.widget.Tooltip"},
   remap = false
)
public abstract class XaeroTooltipMixin {
   @WrapOperation(
      method = {"drawBox"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V",
   remap = true
)},
      remap = false,
      require = 0
   )
   private void cfhgui$box(GuiGraphics g, int x0, int y0, int x1, int y1, int color, Operation<Void> original) {
      if (Reskin.dark()) {
         XaeroScreenDraw.tooltipBox(g, x0, y0, x1, y1);
      } else {
         original.call(new Object[]{g, x0, y0, x1, y1, color});
      }

   }

   @WrapOperation(
      method = {"drawBox"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)I",
   remap = true
)},
      remap = false,
      require = 0
   )
   private int cfhgui$line(GuiGraphics g, Font font, Component text, int x, int y, int color, Operation<Integer> original) {
      if (!Reskin.dark()) {
         return (Integer)original.call(new Object[]{g, font, text, x, y, color});
      } else {
         XaeroScreenDraw.tooltipLine(g, font, text, x, y, color);
         return 0;
      }
   }
}
