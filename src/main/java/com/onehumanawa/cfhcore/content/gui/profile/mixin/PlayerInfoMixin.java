package com.onehumanawa.cfhcore.content.gui.profile.mixin;

import com.mojang.authlib.GameProfile;
import com.onehumanawa.cfhcore.content.gui.profile.client.AppliedLooks;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.PlayerSkin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PlayerInfo.class})
public abstract class PlayerInfoMixin {
	@Shadow
	@Final
	private GameProfile profile;

	@Inject(
		method = {"getSkin"},
		at = {@At("RETURN")},
		cancellable = true
	)
	private void slate$look(CallbackInfoReturnable<PlayerSkin> cir) {
		AppliedLooks.Applied look = AppliedLooks.of(this.profile.getId());
		if (look != null) {
			cir.setReturnValue(look.over((PlayerSkin)cir.getReturnValue()));
		}
	}
}
