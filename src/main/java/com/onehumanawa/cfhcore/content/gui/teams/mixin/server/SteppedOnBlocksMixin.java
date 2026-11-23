package com.onehumanawa.cfhcore.content.gui.teams.mixin.server;

import com.onehumanawa.cfhcore.content.gui.teams.server.protect.LandGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({BasePressurePlateBlock.class, TripWireBlock.class, ButtonBlock.class})
public abstract class SteppedOnBlocksMixin {
	@Inject(
		method = {"entityInside"},
		at = {@At("HEAD")},
		cancellable = true
	)
	private void slateTeams$claimedLand(BlockState state, Level level, BlockPos pos, Entity entity, CallbackInfo ci) {
		if (!level.isClientSide() && LandGuard.stepRefused(level, pos, entity)) {
			ci.cancel();
		}
	}
}
