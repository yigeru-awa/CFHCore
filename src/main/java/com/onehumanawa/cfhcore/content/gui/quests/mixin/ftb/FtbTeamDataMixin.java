package com.onehumanawa.cfhcore.content.gui.quests.mixin.ftb;

import com.onehumanawa.cfhcore.content.gui.quests.server.QuestsServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
	targets = {"dev.ftb.mods.ftbquests.quest.TeamData"},
	remap = false
)
public abstract class FtbTeamDataMixin {
	@Inject(
		method = {"setProgress"},
		at = {@At("HEAD")},
		cancellable = true,
		remap = false,
		require = 0
	)
	private void slateQuests$noProgress(CallbackInfo ci) {
		if (QuestsServer.quietsFtb()) {
			ci.cancel();
		}
	}

	@Inject(
		method = {"isLocked"},
		at = {@At("HEAD")},
		cancellable = true,
		remap = false,
		require = 0
	)
	private void slateQuests$locked(CallbackInfoReturnable<Boolean> cir) {
		if (QuestsServer.quietsFtb()) {
			cir.setReturnValue(Boolean.TRUE);
		}
	}

	@Inject(
		method = {"claimReward"},
		at = {@At("HEAD")},
		cancellable = true,
		remap = false,
		require = 0
	)
	private void slateQuests$noClaim(CallbackInfo ci) {
		if (QuestsServer.quietsFtb()) {
			ci.cancel();
		}
	}
}
