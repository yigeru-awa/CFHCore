package com.onehumanawa.cfhcore.content.gui.teams.mixin.compat;

import com.onehumanawa.cfhcore.content.gui.teams.compat.TeamsCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
	targets = {"xaero.map.highlight.HighlighterRegistry"},
	remap = false
)
public abstract class XaeroWorldMapHighlightersMixin {
	@Inject(
		method = {"end"},
		at = {@At("HEAD")},
		remap = false,
		require = 0
	)
	private void slate$teamsLand(CallbackInfo ci) {
		TeamsCompat.xaeroHighlighters("xaeroworldmap", this);
	}
}
