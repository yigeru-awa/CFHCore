package com.onehumanawa.cfhcore.content.gui.shops.mixin;

import com.onehumanawa.cfhcore.content.gui.shops.service.ShopsRuntime;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({MinecraftServer.class})
public abstract class MinecraftServerMixin {
	@Inject(
		method = {"saveEverything"},
		at = {@At("RETURN")}
	)
	private void slateShops$savedWithTheWorld(boolean suppressLog, boolean flush, boolean forced, CallbackInfoReturnable<Boolean> cir) {
		ShopsRuntime.worldSaved();
	}
}
