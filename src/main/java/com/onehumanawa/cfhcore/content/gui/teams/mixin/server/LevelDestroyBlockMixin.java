package com.onehumanawa.cfhcore.content.gui.teams.mixin.server;

import com.onehumanawa.cfhcore.content.gui.teams.server.protect.LandGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Level.class})
public abstract class LevelDestroyBlockMixin {
	@Inject(
		method = {"destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z"},
		at = {@At("HEAD")},
		cancellable = true
	)
	private void slateTeams$claimedLand(BlockPos pos, boolean drop, @Nullable Entity entity, int recursionLeft, CallbackInfoReturnable<Boolean> cir) {
		if (entity instanceof Enemy && !(entity instanceof Player) && LandGuard.mobGriefRefused((Level)(Object)this, pos)) {
			cir.setReturnValue(false);
		}
	}
}
