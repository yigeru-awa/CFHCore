package com.onehumanawa.cfhcore.content.gui.server.mixin.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.onehumanawa.cfhcore.content.gui.server.service.Hooks;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({MinecraftServer.class})
public abstract class MinecraftServerMixin {
	@WrapOperation(
		method = {"buildPlayerStatus"},
		at = {@At(
			value = "INVOKE",
			target = "Lnet/minecraft/server/players/PlayerList;getPlayers()Ljava/util/List;"
		)}
	)
	private List<ServerPlayer> cfhServer$withoutTheVanished(PlayerList list, Operation<List<ServerPlayer>> original) {
		return Hooks.shown((List<ServerPlayer>)original.call(new Object[]{list}), null);
	}

	@Inject(
		method = {"saveEverything"},
		at = {@At("RETURN")}
	)
	private void cfhServer$savedWithTheWorld(boolean suppressLog, boolean flush, boolean forced, CallbackInfoReturnable<Boolean> cir) {
		Hooks.worldSaved();
	}
}
