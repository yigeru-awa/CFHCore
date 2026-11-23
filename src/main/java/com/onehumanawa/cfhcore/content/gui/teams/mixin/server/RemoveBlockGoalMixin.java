package com.onehumanawa.cfhcore.content.gui.teams.mixin.server;

import com.onehumanawa.cfhcore.content.gui.teams.server.protect.LandGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.RemoveBlockGoal;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({RemoveBlockGoal.class})
public abstract class RemoveBlockGoalMixin {
	@Shadow
	@Final
	private Mob removerMob;

	@Inject(
		method = {"isValidTarget"},
		at = {@At("RETURN")},
		cancellable = true
	)
	private void slateTeams$claimedLand(LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && LandGuard.mobGriefRefused(this.removerMob.level(), pos)) {
			cir.setReturnValue(false);
		}
	}
}
