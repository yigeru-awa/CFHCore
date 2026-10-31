package com.onehumanawa.cfhcore.content.gui.core.compat.xaero.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.onehumanawa.cfhcore.content.gui.core.compat.xaero.XaeroMinimapDraw;
import com.onehumanawa.cfhcore.content.gui.core.compat.xaero.XaeroStyle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
   targets = {"xaero.hud.minimap.compass.render.CompassRenderer"},
   remap = false
)
public abstract class XaeroCompassRendererMixin {
   @Inject(
      method = {"drawCompass"},
      at = {@At("HEAD")},
      require = 0
   )
   private void cfhgui$begin(CallbackInfo ci) {
      if (XaeroStyle.minimap()) {
         XaeroMinimapDraw.compassBegin();
      }

   }

   @Inject(
      method = {"drawCompass"},
      at = {@At("RETURN")},
      require = 0
   )
   private void cfhgui$end(CallbackInfo ci) {
      XaeroMinimapDraw.compassEnd();
   }

   @WrapOperation(
      method = {"drawCompass"},
      at = {@At(
   value = "INVOKE",
   target = "Lxaero/common/misc/Misc;drawNormalText"
)},
      require = 0
   )
   private void cfhgui$letter(PoseStack pose, Component text, float x, float y, int color, boolean shadow, MultiBufferSource.BufferSource buffers, Operation<Void> original) {
      if (!XaeroMinimapDraw.compassOpen()) {
         original.call(new Object[]{pose, text, x, y, color, shadow, buffers});
      } else {
         int cfhgui;
         try {
            cfhgui = XaeroMinimapDraw.compassPass(pose, buffers, x);
         } catch (RuntimeException e) {
            XaeroMinimapDraw.compassEnd();
            XaeroStyle.failed(1, "the minimap's compass", e);
            cfhgui = color;
         }

         if (cfhgui != 0) {
            original.call(new Object[]{pose, text, x, y, cfhgui, shadow, buffers});
         }

      }
   }

   @WrapOperation(
      method = {"drawCompass"},
      at = {@At(
   value = "INVOKE",
   target = "Lxaero/hud/render/util/RenderBufferUtil;addColoredRect"
)},
      require = 0
   )
   private void cfhgui$noSquare(Matrix4f matrix, VertexConsumer consumer, float x, float y, int w, int h, int color, Operation<Void> original) {
      if (!XaeroMinimapDraw.compassOpen()) {
         original.call(new Object[]{matrix, consumer, x, y, w, h, color});
      }

   }
}
