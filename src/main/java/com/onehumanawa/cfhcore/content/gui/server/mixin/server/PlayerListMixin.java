package com.onehumanawa.cfhcore.content.gui.server.mixin.server;

import com.mojang.authlib.GameProfile;
import com.onehumanawa.cfhcore.content.gui.server.service.Hooks;
import java.net.SocketAddress;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PlayerList.class})
public abstract class PlayerListMixin {
	@Inject(
		method = {"canPlayerLogin"},
		at = {@At("RETURN")},
		cancellable = true
	)
	private void cfhServer$maintenance(SocketAddress address, GameProfile profile, CallbackInfoReturnable<Component> cir) {
		if (cir.getReturnValue() == null) {
			Component refusal = Hooks.loginRefusal(profile);
			if (refusal != null) {
				cir.setReturnValue(refusal);
			}
		}
	}

	@Inject(
		method = {"placeNewPlayer"},
		at = {@At("HEAD")}
	)
	private void cfhServer$comingIn(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
		Hooks.joining(player);
	}

	@Inject(
		method = {"placeNewPlayer"},
		at = {@At("RETURN")}
	)
	private void cfhServer$cameIn(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
		Hooks.joining(null);
	}

	@Inject(
		method = {"broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"},
		at = {@At("HEAD")},
		cancellable = true
	)
	private void cfhServer$quietComingAndGoing(Component message, boolean bypassHiddenChat, CallbackInfo ci) {
		if (Hooks.hideBroadcast((PlayerList)(Object)this, message)) {
			ci.cancel();
		}
	}

	@Inject(
		method = {"broadcastAll(Lnet/minecraft/network/protocol/Packet;)V"},
		at = {@At("HEAD")},
		cancellable = true
	)
	private void cfhServer$quietTabEntry(Packet<?> packet, CallbackInfo ci) {
		if (Hooks.hideTabEntry((PlayerList)(Object)this, packet)) {
			ci.cancel();
		}
	}
}
