package com.onehumanawa.cfhcore.content.gui.quests.mixin.server;

import com.onehumanawa.cfhcore.content.gui.quests.server.QuestEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ServerPlayer.class})
public abstract class KillCreditMixin {
	@Inject(
		method = {"awardKillScore"},
		at = {@At("HEAD")}
	)
	private void slateQuests$kill(Entity killed, int score, DamageSource source, CallbackInfo ci) {
		QuestEvents.killed((ServerPlayer)(Object)this, killed);
	}
}
