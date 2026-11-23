package com.onehumanawa.cfhcore.content.gui.teams.mixin.server;

import com.onehumanawa.cfhcore.content.gui.teams.server.protect.LandGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({DispenserBlock.class})
public abstract class DispenserBlockMixin {
	@Inject(
		method = {"dispenseFrom"},
		at = {@At("HEAD")},
		cancellable = true
	)
	private void slateTeams$stayOffClaimedLand(ServerLevel level, BlockState state, BlockPos pos, CallbackInfo ci) {
		if (state.hasProperty(DispenserBlock.FACING) && LandGuard.reachRefused(level, pos, pos.relative((Direction)state.getValue(DispenserBlock.FACING)))) {
			ci.cancel();
		}
	}
}
