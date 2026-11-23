package com.onehumanawa.cfhcore.content.gui.teams.mixin.client;

import com.onehumanawa.cfhcore.content.gui.teams.client.hud.PingWheel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MouseHandler.class})
public abstract class TeamsMouseHandlerMixin {
	@Shadow
	@Final
	private Minecraft minecraft;
	@Shadow
	private double accumulatedDX;
	@Shadow
	private double accumulatedDY;

	@Shadow
	public abstract boolean isMouseGrabbed();

	@Inject(
		method = {"onPress"},
		at = {@At("HEAD")},
		cancellable = true
	)
	private void slateTeams$onPress(long window, int button, int action, int mods, CallbackInfo ci) {
		if (action == 1 && window == this.minecraft.getWindow().getWindow() && this.minecraft.screen == null && this.isMouseGrabbed()) {
			if (PingWheel.onMousePress(button)) {
				ci.cancel();
			}
		}
	}

	@Inject(
		method = {"onScroll"},
		at = {@At("HEAD")},
		cancellable = true
	)
	private void slateTeams$onScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
		if (window == this.minecraft.getWindow().getWindow() && this.minecraft.screen == null) {
			if (PingWheel.isOpen()) {
				ci.cancel();
			}
		}
	}

	@Inject(
		method = {"turnPlayer"},
		at = {@At("HEAD")},
		cancellable = true
	)
	private void slateTeams$turnPlayer(double movementTime, CallbackInfo ci) {
		if (PingWheel.isOpen() && (this.accumulatedDX != 0.0 || this.accumulatedDY != 0.0)) {
			if (PingWheel.onMouseLook(this.accumulatedDX, this.accumulatedDY)) {
				this.accumulatedDX = 0.0;
				this.accumulatedDY = 0.0;
				ci.cancel();
			}
		}
	}
}
