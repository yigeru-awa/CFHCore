package com.onehumanawa.cfhcore.content.gui.teams.mixin.server;

import com.onehumanawa.cfhcore.content.gui.teams.server.protect.LandGuard;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BlockItem.class})
public abstract class BlockItemMixin {
	@Inject(
		method = {"place"},
		at = {@At("HEAD")},
		cancellable = true
	)
	private void slateTeams$claimedLand(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (LandGuard.placeRefused(context.getPlayer(), context.getLevel(), context.getClickedPos())) {
			cir.setReturnValue(InteractionResult.FAIL);
		}
	}
}
