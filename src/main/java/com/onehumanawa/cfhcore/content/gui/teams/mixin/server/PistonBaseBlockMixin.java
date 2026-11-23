package com.onehumanawa.cfhcore.content.gui.teams.mixin.server;

import com.onehumanawa.cfhcore.content.gui.teams.server.protect.LandGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PistonBaseBlock.class})
public abstract class PistonBaseBlockMixin {
	@Inject(
		method = {"moveBlocks"},
		at = {@At("HEAD")},
		cancellable = true
	)
	private void slateTeams$stayOffClaimedLand(Level level, BlockPos pos, Direction facing, boolean extending, CallbackInfoReturnable<Boolean> cir) {
		if (LandGuard.pistonRefused(level, pos, facing, extending)) {
			cir.setReturnValue(false);
		}
	}
}
