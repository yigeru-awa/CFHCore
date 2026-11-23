package com.onehumanawa.cfhcore.content.gui.server.mixin.server;

import com.onehumanawa.cfhcore.content.gui.server.service.Hooks;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.RelativeMovement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ServerPlayer.class})
public abstract class ServerPlayerMixin {
	@Inject(
		method = {"doTick"},
		at = {@At("HEAD")}
	)
	private void cfhServer$steady(CallbackInfo ci) {
		Hooks.beforeTick((ServerPlayer)(Object)this);
	}

	@Inject(
		method = {"getTabListDisplayName"},
		at = {@At("RETURN")},
		cancellable = true
	)
	private void cfhServer$tabName(CallbackInfoReturnable<Component> cir) {
		if (cir.getReturnValue() == null) {
			Component name = Hooks.tabName((ServerPlayer)(Object)this);
			if (name != null) {
				cir.setReturnValue(name);
			}
		}
	}

	@Inject(
		method = {"drop(Z)Z"},
		at = {@At("HEAD")},
		cancellable = true
	)
	private void cfhServer$heldDrop(boolean wholeStack, CallbackInfoReturnable<Boolean> cir) {
		ServerPlayer self = (ServerPlayer)(Object)this;
		if (Hooks.held(self)) {
			self.inventoryMenu.sendAllDataToRemote();
			cir.setReturnValue(false);
		}
	}

	@Inject(
		method = {"broadcastToPlayer"},
		at = {@At("RETURN")},
		cancellable = true
	)
	private void cfhServer$vanished(ServerPlayer viewer, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && Hooks.hiddenFrom((ServerPlayer)(Object)this, viewer)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(
		method = {"teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z"},
		at = {@At("RETURN")}
	)
	private void cfhServer$movedByCommand(
		ServerLevel level, double x, double y, double z, Set<RelativeMovement> relative, float yaw, float pitch, CallbackInfoReturnable<Boolean> cir
	) {
		if (cir.getReturnValueZ()) {
			Hooks.moved((ServerPlayer)(Object)this);
		}
	}
}
