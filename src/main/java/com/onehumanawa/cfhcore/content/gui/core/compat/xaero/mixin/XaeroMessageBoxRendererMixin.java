package com.onehumanawa.cfhcore.content.gui.core.compat.xaero.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.onehumanawa.cfhcore.content.gui.core.compat.xaero.XaeroScreenDraw;
import com.onehumanawa.cfhcore.content.gui.core.compat.xaero.XaeroStyle;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(
   targets = {"xaero.map.gui.message.render.MessageBoxRenderer"},
   remap = false
)
public abstract class XaeroMessageBoxRendererMixin {
   @Unique
   private static boolean cfhgui$on() {
      return XaeroStyle.worldMap() && Reskin.dark();
   }

   @WrapOperation(
      method = {"render"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V",
   remap = true
)},
      remap = false,
      require = 0
   )
   private void cfhgui$plate(GuiGraphics g, int x0, int y0, int x1, int y1, int color, Operation<Void> original) {
      original.call(new Object[]{g, x0, y0, x1, y1, cfhgui$on() ? XaeroScreenDraw.messagePlate(color) : color});
   }

   @WrapOperation(
      method = {"render"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)I",
   remap = true
)},
      remap = false,
      require = 0
   )
   private int cfhgui$text(GuiGraphics g, Font font, Component text, int x, int y, int color, Operation<Integer> original) {
      return !cfhgui$on() ? (Integer)original.call(new Object[]{g, font, text, x, y, color}) : g.drawString(font, text, x, y, XaeroScreenDraw.messageText(color), false);
   }
}
