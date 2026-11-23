package com.onehumanawa.cfhcore.content.gui.emi.mixin;

import com.onehumanawa.cfhcore.content.gui.emi.platform.EmiClient;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiLog;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiReloadManager;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.ProxyRecipeManager;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Minecraft.class})
public class MinecraftClientMixin {
	@Shadow
	public ClientLevel level;

	@Inject(
		at = {@At("RETURN")},
		method = {"reloadResourcePacks(ZLnet/minecraft/client/Minecraft$GameLoadCookie;)Ljava/util/concurrent/CompletableFuture;"}
	)
	private void reloadResources(boolean force, @Coerce Object loadingContext, CallbackInfoReturnable<CompletableFuture<Void>> info) {
		CompletableFuture<Void> future = (CompletableFuture<Void>)info.getReturnValue();
		if (future != null) {
			future.thenRunAsync(() -> {
				if (ProxyRecipeManager.isAvailable()) {
					EmiReloadManager.reload();
				}
			}, Executors.newFixedThreadPool(1));
		}
	}

	@Inject(
		at = {@At("HEAD")},
		method = {"disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V"}
	)
	private void disconnect(CallbackInfo info) {
		EmiLog.info("Disconnecting from server, EMI data cleared");
		EmiReloadManager.clear();
		EmiClient.onServer = false;
	}
}
