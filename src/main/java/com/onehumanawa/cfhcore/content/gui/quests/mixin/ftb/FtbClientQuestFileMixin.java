package com.onehumanawa.cfhcore.content.gui.quests.mixin.ftb;

import com.onehumanawa.cfhcore.content.gui.quests.client.FtbTakeover;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
	targets = {"dev.ftb.mods.ftbquests.client.ClientQuestFile"},
	remap = false
)
public abstract class FtbClientQuestFileMixin {
	@Inject(
		method = {"openQuestGui"},
		at = {@At("HEAD")},
		cancellable = true,
		remap = false,
		require = 0
	)
	private void slateQuests$openSlateBook(CallbackInfoReturnable<Object> cir) {
		if (FtbTakeover.open(this)) {
			cir.setReturnValue(null);
		}
	}
}
