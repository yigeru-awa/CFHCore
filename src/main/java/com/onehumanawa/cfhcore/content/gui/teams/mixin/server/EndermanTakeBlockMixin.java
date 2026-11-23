package com.onehumanawa.cfhcore.content.gui.teams.mixin.server;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.onehumanawa.cfhcore.content.gui.teams.server.protect.LandGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
	targets = {"net.minecraft.world.entity.monster.EnderMan$EndermanTakeBlockGoal"}
)
public abstract class EndermanTakeBlockMixin {
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

	@ModifyExpressionValue(
		method = {"tick"},
		require = 0,
		at = {@At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z"
		)}
	)
	private boolean slateTeams$notOutOfClaimedLand(boolean holdable, @Local BlockPos pos) {
		return holdable && !LandGuard.mobGriefRefused(this.enderman.level(), pos);
	}
}
