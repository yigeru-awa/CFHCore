package com.onehumanawa.cfhcore.content.gui.server.mixin.server;

import com.onehumanawa.cfhcore.content.gui.server.service.Hooks;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ServerGamePacketListenerImpl.class})
public abstract class ServerGamePacketListenerImplMixin {
	@Shadow
	public ServerPlayer player;

	@Inject(
		method = {"handleContainerClick"},
		at = {@At(
			value = "INVOKE",
			target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
			shift = Shift.AFTER
		)},
		cancellable = true
	)
	private void cfhServer$heldClick(ServerboundContainerClickPacket packet, CallbackInfo ci) {
		if (Hooks.held(this.player)) {
			this.player.containerMenu.sendAllDataToRemote();
			ci.cancel();
		}
	}

	@Inject(
		method = {"handleInteract"},
		at = {@At(
			value = "INVOKE",
			target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
			shift = Shift.AFTER
		)},
		cancellable = true
	)
	private void cfhServer$heldInteract(ServerboundInteractPacket packet, CallbackInfo ci) {
		if (Hooks.held(this.player)) {
			ci.cancel();
		}
	}
}
