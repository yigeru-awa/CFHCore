package com.onehumanawa.cfhcore.content.gui.core.compat.xaero.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.onehumanawa.cfhcore.content.gui.core.compat.xaero.XaeroMinimapDraw;
import com.onehumanawa.cfhcore.content.gui.core.compat.xaero.XaeroStyle;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(
   targets = {"xaero.hud.minimap.waypoint.render.world.WaypointWorldRenderer"},
   remap = false
)
public abstract class XaeroWaypointWorldRendererMixin {
   @WrapOperation(
      method = {"renderWaypointLabel"},
      at = {@At(
   value = "INVOKE",
   target = "Lxaero/hud/render/util/RenderBufferUtil;addColoredRect"
)},
      require = 0
   )
   private void cfhgui$plate(Matrix4f matrix, VertexConsumer consumer, float x, float y, int w, int h, float r, float g, float b, float a, Operation<Void> original) {
      if (!XaeroStyle.minimap()) {
         original.call(new Object[]{matrix, consumer, x, y, w, h, r, g, b, a});
      } else {
         int plate = XaeroMinimapDraw.labelPlate();
         original.call(new Object[]{matrix, consumer, x, y, w, h, (float)(plate >> 16 & 255) / 255.0F, (float)(plate >> 8 & 255) / 255.0F, (float)(plate & 255) / 255.0F, (float)(plate >>> 24) / 255.0F});
      }
   }

   @WrapOperation(
      method = {"renderWaypointLabel"},
      at = {@At(
   value = "INVOKE",
   target = "Lxaero/common/misc/Misc;drawNormalText"
)},
      require = 0
   )
   private void cfhgui$text(PoseStack pose, String text, float x, float y, int color, boolean shadow, MultiBufferSource.BufferSource buffers, Operation<Void> original) {
      original.call(new Object[]{pose, text, x, y, XaeroStyle.minimap() ? XaeroMinimapDraw.labelText() : color, shadow, buffers});
   }
}
