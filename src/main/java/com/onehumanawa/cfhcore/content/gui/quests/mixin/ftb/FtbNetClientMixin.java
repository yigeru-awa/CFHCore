package com.onehumanawa.cfhcore.content.gui.quests.mixin.ftb;

import com.onehumanawa.cfhcore.content.gui.quests.client.FtbTakeover;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
	targets = {"dev.ftb.mods.ftbquests.client.FTBQuestsNetClient"},
	remap = false
)
public abstract class FtbNetClientMixin {
	@Inject(
		method = {"notifyPlayerOfCompletion"},
		at = {@At("HEAD")},
		cancellable = true,
		remap = false,
		require = 0
	)
	private static void slateQuests$quietToast(CallbackInfo ci) {
		if (FtbTakeover.active()) {
			ci.cancel();
		}
	}
}
