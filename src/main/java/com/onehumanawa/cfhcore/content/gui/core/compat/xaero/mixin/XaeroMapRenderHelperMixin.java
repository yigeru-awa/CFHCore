package com.onehumanawa.cfhcore.content.gui.core.compat.xaero.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.onehumanawa.cfhcore.content.gui.core.compat.xaero.XaeroScreenDraw;
import com.onehumanawa.cfhcore.content.gui.core.compat.xaero.XaeroStyle;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(
   targets = {"xaero.map.graphics.MapRenderHelper"},
   remap = false
)
public abstract class XaeroMapRenderHelperMixin {
   @Unique
   private static boolean cfhgui$on() {
      return XaeroStyle.worldMap() && Reskin.dark();
   }

   @WrapOperation(
      method = {"drawStringWithBackground*"},
      at = {@At(
   value = "INVOKE",
   target = "Lxaero/map/graphics/MapRenderHelper;fillIntoExistingBuffer"
)},
      require = 0
   )
   private static void cfhgui$plate(Matrix4f matrix, VertexConsumer consumer, int x0, int y0, int x1, int y1, float r, float g, float b, float a, Operation<Void> original) {
      if (!cfhgui$on()) {
         original.call(new Object[]{matrix, consumer, x0, y0, x1, y1, r, g, b, a});
      } else {
         int plate = XaeroScreenDraw.labelPlate(r, g, b, a);
         float pr = (float)Colors.red(plate) / 255.0F;
         float pg = (float)Colors.green(plate) / 255.0F;
         float pb = (float)Colors.blue(plate) / 255.0F;
         float pa = (float)Colors.alpha(plate) / 255.0F;
         int left = x0 - 3;
         int right = x1 + 3;
         int steps = Math.max(0, Math.min(2, (y1 - y0) / 2 - 1));

         for(int i = 0; i < steps; ++i) {
            int inset = steps - i;
            original.call(new Object[]{matrix, consumer, left + inset, y0 + i, right - inset, y0 + i + 1, pr, pg, pb, pa});
            original.call(new Object[]{matrix, consumer, left + inset, y1 - i - 1, right - inset, y1 - i, pr, pg, pb, pa});
         }

         original.call(new Object[]{matrix, consumer, left, y0 + steps, right, y1 - steps, pr, pg, pb, pa});
      }
   }

   @WrapOperation(
      method = {"drawStringWithBackground*"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)I",
   remap = true
)},
      remap = false,
      require = 0
   )
   private static int cfhgui$text(GuiGraphics g, Font font, Component text, int x, int y, int color, Operation<Integer> original) {
      return !cfhgui$on() ? (Integer)original.call(new Object[]{g, font, text, x, y, color}) : g.drawString(font, text, x, y, XaeroScreenDraw.labelText(color), false);
   }

   @WrapOperation(
      method = {"drawStringWithBackground*"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)I",
   remap = true
)},
      remap = false,
      require = 0
   )
   private static int cfhgui$plainText(GuiGraphics g, Font font, String text, int x, int y, int color, Operation<Integer> original) {
      return !cfhgui$on() ? (Integer)original.call(new Object[]{g, font, text, x, y, color}) : g.drawString(font, text, x, y, XaeroScreenDraw.labelText(color), false);
   }
}
