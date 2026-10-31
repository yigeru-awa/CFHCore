package com.onehumanawa.cfhcore.content.gui.core.compat.xaero.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.onehumanawa.cfhcore.content.gui.core.compat.xaero.XaeroMinimapDraw;
import com.onehumanawa.cfhcore.content.gui.core.compat.xaero.XaeroStyle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.hud.minimap.common.config.MinimapConfigConstants;
import xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions;
import xaero.lib.client.config.ClientConfigManager;
import xaero.lib.common.config.option.ConfigOption;

@Pseudo
@Mixin(
   targets = {"xaero.common.minimap.render.MinimapRenderer"},
   remap = false
)
public abstract class XaeroMinimapRendererMixin {
   @WrapOperation(
      method = {"renderMinimap"},
      at = {@At(
   value = "INVOKE",
   target = "Lxaero/common/minimap/render/MinimapRendererHelper;drawMyTexturedModalRectPre"
)},
      require = 0
   )
   private void cfhgui$squareMap(MinimapRendererHelper helper, PoseStack pose, float x, float y, int u, int v, float w, float h, float texH, float texSize, float discardAlpha, Operation<Void> original) {
      if (XaeroStyle.minimap() && XaeroMinimapDraw.frames() && !(w <= 0.0F) && !(h <= 0.0F)) {
         try {
            original.call(new Object[]{helper, pose, x, y, u, v, w, 0.0F, 0.0F, texSize, discardAlpha});
            XaeroMinimapDraw.squareMap(pose, x, y, u, v, w, h, texH, texSize);
            XaeroMinimapDraw.squareFrame(pose, x, y, w, h);
         } catch (RuntimeException e) {
            XaeroStyle.failed(1, "the minimap's frame", e);
            original.call(new Object[]{helper, pose, x, y, u, v, w, h, texH, texSize, discardAlpha});
         }

      } else {
         original.call(new Object[]{helper, pose, x, y, u, v, w, h, texH, texSize, discardAlpha});
      }
   }

   @WrapOperation(
      method = {"renderMinimap"},
      at = {@At(
   value = "INVOKE",
   target = "Lxaero/common/minimap/render/MinimapRendererHelper;drawTexturedElipseInsideRectangle"
)},
      require = 0
   )
   private void cfhgui$roundMap(MinimapRendererHelper helper, PoseStack pose, double startAngle, int sides, float x, float y, int u, int v, float width, float texSize, Operation<Void> original) {
      original.call(new Object[]{helper, pose, startAngle, sides, x, y, u, v, width, texSize});
      if (XaeroStyle.minimap() && XaeroMinimapDraw.frames()) {
         try {
            XaeroMinimapDraw.roundFrame(pose, startAngle, sides, x, y, width);
         } catch (RuntimeException e) {
            XaeroStyle.failed(1, "the minimap's ring", e);
         }

      }
   }

   @WrapOperation(
      method = {"renderMinimap"},
      at = {@At(
   value = "INVOKE",
   target = "Lxaero/lib/client/config/ClientConfigManager;getEffective(Lxaero/lib/common/config/option/ConfigOption;)Ljava/lang/Object;"
)},
      require = 0
   )
   private Object cfhgui$noFrame(ClientConfigManager manager, ConfigOption<?> option, Operation<Object> original) {
      if (XaeroStyle.minimap()) {
         try {
            if (option == MinimapProfiledConfigOptions.FRAME && XaeroMinimapDraw.takeFramed()) {
               return MinimapConfigConstants.FRAME_NAMES.length - 1;
            }
         } catch (LinkageError e) {
            XaeroStyle.failed(1, "the minimap's frame setting", e);
         }
      }

      return original.call(new Object[]{manager, option});
   }
}
