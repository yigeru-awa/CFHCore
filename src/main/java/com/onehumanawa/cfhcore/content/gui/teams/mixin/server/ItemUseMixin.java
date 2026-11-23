package com.onehumanawa.cfhcore.content.gui.teams.mixin.server;

import com.onehumanawa.cfhcore.content.gui.teams.server.protect.LandGuard;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ItemStack.class})
public abstract class ItemUseMixin {
	@Inject(
		method = {"useOn"},
		at = {@At("HEAD")},
		cancellable = true
	)
	private void slateTeams$claimedLand(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (LandGuard.useOnRefused(context)) {
			cir.setReturnValue(InteractionResult.FAIL);
		}
	}
}
