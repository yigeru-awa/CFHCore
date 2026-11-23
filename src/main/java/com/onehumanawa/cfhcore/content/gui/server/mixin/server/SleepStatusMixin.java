package com.onehumanawa.cfhcore.content.gui.server.mixin.server;

import com.onehumanawa.cfhcore.content.gui.server.service.Hooks;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.SleepStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin({SleepStatus.class})
public abstract class SleepStatusMixin {
	@ModifyVariable(
		method = {"update"},
		at = @At("HEAD"),
		argsOnly = true
	)
	private List<ServerPlayer> cfhServer$withoutTheIdle(List<ServerPlayer> players) {
		return Hooks.sleepers(players);
	}
}
