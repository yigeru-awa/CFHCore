package com.onehumanawa.cfhcore.content.gui.teams.mixin.server;

import com.onehumanawa.cfhcore.content.gui.teams.server.protect.LandGuard;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public abstract class EntityDamageMixin {
	@Inject(
		method = {"isInvulnerableTo"},
		at = {@At("RETURN")},
		cancellable = true
	)
	private void slateTeams$claimedLand(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() && LandGuard.damageRefused((Entity)(Object)this, source)) {
			cir.setReturnValue(true);
		}
	}
}
