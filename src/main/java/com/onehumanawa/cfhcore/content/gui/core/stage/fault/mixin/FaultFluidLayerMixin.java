package com.onehumanawa.cfhcore.content.gui.core.stage.fault.mixin;

import com.onehumanawa.cfhcore.content.gui.core.stage.StageFaults;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ItemBlockRenderTypes.class})
public abstract class FaultFluidLayerMixin {
   @Inject(
      method = {"getRenderLayer(Lnet/minecraft/world/level/material/FluidState;)Lnet/minecraft/client/renderer/RenderType;"},
      at = {@At("HEAD")},
      require = 0
   )
   private static void cfhgui$faultWithoutPlayer(FluidState fluid, CallbackInfoReturnable<RenderType> cir) {
      if (StageFaults.on()) {
         StageFaults.noPlayer("ItemBlockRenderTypes.getRenderLayer(FluidState)");
      }

   }
}
