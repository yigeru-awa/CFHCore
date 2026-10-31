package com.onehumanawa.cfhcore.content.gui.core.stage.fault.mixin;

import com.onehumanawa.cfhcore.content.gui.core.stage.StageFaults;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LevelRenderer.class})
public abstract class FaultLightColorMixin {
   @Inject(
      method = {"getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I"},
      at = {@At("HEAD")},
      require = 0
   )
   private static void cfhgui$faultOnSceneWorld(BlockAndTintGetter level, BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
      if (level instanceof StageLevel && StageFaults.on()) {
         StageFaults.noWorldOfItsOwn("LevelRenderer.getLightColor on a scene's world");
      }

   }
}
