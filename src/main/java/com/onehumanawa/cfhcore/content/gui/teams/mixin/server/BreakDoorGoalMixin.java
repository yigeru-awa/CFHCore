package com.onehumanawa.cfhcore.content.gui.teams.mixin.server;

import com.onehumanawa.cfhcore.content.gui.teams.server.protect.LandGuard;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.ai.goal.DoorInteractGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BreakDoorGoal.class})
public abstract class BreakDoorGoalMixin extends DoorInteractGoal {
	private BreakDoorGoalMixin(Mob mob) {
		super(mob);
	}

	@Inject(
		method = {"canUse"},
		at = {@At("RETURN")},
		cancellable = true
	)
	private void slateTeams$claimedLand(CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && LandGuard.mobGriefRefused(this.mob.level(), this.doorPos)) {
			cir.setReturnValue(false);
		}
	}
}
