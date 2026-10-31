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
   targets = {"xaero.hud.minimap.info.render.InfoDisplayRenderer"},
   remap = false
)
public abstract class XaeroInfoDisplayRendererMixin {
   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      require = 0
   )
   private void cfhgui$begin(CallbackInfo ci) {
      if (XaeroStyle.minimap()) {
         XaeroMinimapDraw.infoBegin();
      }

   }

   @Inject(
      method = {"render"},
      at = {@At("RETURN")},
      require = 0
   )
   private void cfhgui$end(CallbackInfo ci) {
      if (XaeroMinimapDraw.infoOpen()) {
         try {
            XaeroMinimapDraw.infoEnd();
         } catch (RuntimeException e) {
            XaeroStyle.failed(1, "the minimap's text", e);
         }

      }
   }

   @WrapOperation(
      method = {"render"},
      at = {@At(
   value = "INVOKE",
   target = "Lxaero/common/misc/Misc;drawNormalText"
)},
      require = 0
   )
   private void cfhgui$line(PoseStack pose, Component text, float x, float y, int color, boolean shadow, MultiBufferSource.BufferSource buffers, Operation<Void> original) {
      if (!XaeroMinimapDraw.infoLine(pose, buffers, text, x, y, color)) {
         original.call(new Object[]{pose, text, x, y, color, shadow, buffers});
      }

   }

   @WrapOperation(
      method = {"render"},
      at = {@At(
   value = "INVOKE",
   target = "Lxaero/hud/render/util/RenderBufferUtil;addColoredRect"
)},
      require = 0
   )
   private void cfhgui$noBackground(Matrix4f matrix, VertexConsumer consumer, float x, float y, int w, int h, int color, Operation<Void> original) {
      if (!XaeroMinimapDraw.infoOpen()) {
         original.call(new Object[]{matrix, consumer, x, y, w, h, color});
      }

   }
}
