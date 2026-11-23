package com.onehumanawa.cfhcore.content.gui.teams.mixin.server;

import com.llamalad7.mixinextras.sugar.Local;
import com.onehumanawa.cfhcore.content.gui.teams.server.protect.LandGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({FireBlock.class})
public abstract class FireBlockMixin {
	@Inject(
		method = {"getIgniteOdds(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)I"},
		at = {@At("HEAD")},
		cancellable = true
	)
	private void slateTeams$doNotCatch(LevelReader level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
		if (level instanceof Level l && LandGuard.fireRefused(l, pos)) {
			cir.setReturnValue(0);
		}
	}

	@Inject(
		method = {"checkBurnOut"},
		at = {@At("HEAD")},
		cancellable = true
	)
	private void slateTeams$doNotBurn(CallbackInfo ci, @Local(argsOnly = true) Level level, @Local(argsOnly = true) BlockPos pos) {
		if (LandGuard.fireRefused(level, pos)) {
			ci.cancel();
		}
	}
}
