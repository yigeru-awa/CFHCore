package com.onehumanawa.cfhcore.content.gui.server.mixin.server;

import com.onehumanawa.cfhcore.content.gui.server.service.Hooks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Player.class})
public abstract class PlayerDisplayNameMixin {
	@ModifyArg(
		method = {"getDisplayName"},
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/scores/PlayerTeam;formatNameForTeam(Lnet/minecraft/world/scores/Team;Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/MutableComponent;"
		),
		index = 1
	)
	private Component cfhServer$nickname(Component name) {
		return Hooks.nameForTeam((Player)(Object)this, name);
	}

	@Inject(
		method = {"getDisplayName"},
		at = {@At("RETURN")},
		cancellable = true
	)
	private void cfhServer$rolePrefix(CallbackInfoReturnable<Component> cir) {
		Component named = Hooks.displayName((Player)(Object)this, (Component)cir.getReturnValue());
		if (named != null) {
			cir.setReturnValue(named);
		}
	}
}
