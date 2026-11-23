package com.onehumanawa.cfhcore.content.gui.teams.mixin.server;

import com.onehumanawa.cfhcore.content.gui.teams.server.protect.LandGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ServerPlayer.class})
public abstract class ServerPlayerMixin {
	@Inject(
		method = {"mayInteract"},
		at = {@At("RETURN")},
		cancellable = true
	)
	private void slateTeams$claimedLand(Level level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && LandGuard.buildRefusedQuietly((ServerPlayer)(Object)this, level, pos)) {
			cir.setReturnValue(false);
		}
	}
}
