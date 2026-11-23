package com.onehumanawa.cfhcore.content.gui.teams.mixin.client;

import com.onehumanawa.cfhcore.content.gui.teams.client.hud.PingWheel;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Gui.class})
public abstract class TeamsGuiMixin {
	@Inject(
		method = {"renderCrosshair"},
		at = {@At("HEAD")},
		cancellable = true,
		require = 0
	)
	private void slateTeams$hideCrosshair(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
		if (PingWheel.hidesCrosshair()) {
			ci.cancel();
		}
	}
}
