package com.onehumanawa.cfhcore.content.gui.teams.mixin.server;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.onehumanawa.cfhcore.content.gui.teams.server.protect.LandGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({FarmBlock.class})
public abstract class FarmBlockMixin {
	@WrapWithCondition(
		method = {"fallOn"},
		require = 0,
		at = {@At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/FarmBlock;turnToDirt(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"
		)}
	)
	private boolean slateTeams$claimedLand(Entity entity, BlockState state, Level level, BlockPos pos) {
		return !LandGuard.trampleRefused(level, pos, entity);
	}
}
