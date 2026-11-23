package com.onehumanawa.cfhcore.content.gui.teams.mixin.client;

import com.onehumanawa.cfhcore.content.gui.teams.client.hud.HudKeys;
import com.onehumanawa.cfhcore.content.gui.teams.client.hud.PingWheel;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({KeyboardHandler.class})
public abstract class TeamsKeyboardHandlerMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(
		method = {"keyPress"},
		at = {@At("HEAD")},
		cancellable = true
	)
	private void slateTeams$keyPress(long window, int key, int scancode, int action, int mods, CallbackInfo ci) {
		if (window == this.minecraft.getWindow().getWindow()) {
			HudKeys.keyEvent(key, action);
			if (action == 1 && this.minecraft.screen == null) {
				if (PingWheel.onKeyPress(key, scancode)) {
					ci.cancel();
				}
			}
		}
	}
}
