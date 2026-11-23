package com.onehumanawa.cfhcore.content.gui.teams.mixin.server;

import com.onehumanawa.cfhcore.content.gui.teams.server.protect.LandGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
	targets = {"net.minecraft.world.entity.monster.EnderMan$EndermanLeaveBlockGoal"}
)
public abstract class EndermanLeaveBlockMixin {
	@Shadow
	@Final
	private EnderMan enderman;

	@Inject(
		method = {"canUse"},
		at = {@At("RETURN")},
		cancellable = true
	)
	private void slateTeams$claimedLand(CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && LandGuard.mobGriefRefused(this.enderman.level(), this.enderman.blockPosition())) {
			cir.setReturnValue(false);
		}
	}

	@Inject(
		method = {"canPlaceBlock"},
		at = {@At("RETURN")},
		cancellable = true,
		require = 0
	)
	private void slateTeams$notOntoClaimedLand(
		Level level, BlockPos pos, BlockState carried, BlockState there, BlockState below, BlockPos belowPos, CallbackInfoReturnable<Boolean> cir
	) {
		if (cir.getReturnValueZ() && LandGuard.mobGriefRefused(level, pos)) {
			cir.setReturnValue(false);
		}
	}
}
