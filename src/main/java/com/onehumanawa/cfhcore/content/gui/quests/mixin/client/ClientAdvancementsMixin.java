package com.onehumanawa.cfhcore.content.gui.quests.mixin.client;

import com.onehumanawa.cfhcore.content.gui.quests.client.vanilla.AdvancementChapters;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientAdvancements.class})
public abstract class ClientAdvancementsMixin {
	@Inject(
		method = {"update"},
		at = {@At("TAIL")}
	)
	private void slateQuests$updated(ClientboundUpdateAdvancementsPacket packet, CallbackInfo ci) {
		AdvancementChapters.updated((ClientAdvancements)(Object)this, packet);
	}
}
