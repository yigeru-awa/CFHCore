package com.onehumanawa.cfhcore.content.gui.veinminer.mixin;

import com.onehumanawa.cfhcore.content.gui.veinminer.client.VeinClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MouseHandler.class})
public abstract class VeinMouseHandlerMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(
		method = {"onScroll"},
		at = {@At("HEAD")},
		cancellable = true
	)
	private void cfhVeinminer$onScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
		if (window == this.minecraft.getWindow().getWindow() && this.minecraft.screen == null) {
			if (VeinClient.onScroll(yOffset)) {
				ci.cancel();
			}
		}
	}
}
