package com.onehumanawa.cfhcore.content.gui.emi.mixin;

import com.onehumanawa.cfhcore.content.gui.emi.screen.EmiScreenManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Screen.class})
public class ScreenMixin {
	@Inject(
		at = {@At("RETURN")},
		method = {"init(Lnet/minecraft/client/Minecraft;II)V"}
	)
	private void init(Minecraft client, int width, int height, CallbackInfo info) {
		if (client.screen instanceof AbstractContainerScreen hs) {
			EmiScreenManager.addWidgets(hs);
		}
	}

	@Inject(
		at = {@At("RETURN")},
		method = {"resize(Lnet/minecraft/client/Minecraft;II)V"}
	)
	private void resize(Minecraft client, int width, int height, CallbackInfo info) {
		if (client.screen instanceof AbstractContainerScreen hs) {
			EmiScreenManager.addWidgets(hs);
		}
	}
}
