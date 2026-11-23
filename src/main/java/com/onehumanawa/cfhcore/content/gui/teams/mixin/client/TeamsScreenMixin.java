package com.onehumanawa.cfhcore.content.gui.teams.mixin.client;

import com.onehumanawa.cfhcore.content.gui.teams.client.hud.HudKeys;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Screen.class})
public abstract class TeamsScreenMixin {
	@Inject(
		method = {"keyPressed"},
		at = {@At("RETURN")},
		cancellable = true,
		require = 0
	)
	private void slateTeams$menuKeyCloses(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ()) {
			if (HudKeys.closesTeamsMenu((Screen)(Object)this, keyCode, scanCode)) {
				cir.setReturnValue(true);
			}
		}
	}
}
