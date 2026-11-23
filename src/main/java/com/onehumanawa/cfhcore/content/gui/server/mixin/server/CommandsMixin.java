package com.onehumanawa.cfhcore.content.gui.server.mixin.server;

import com.mojang.brigadier.ParseResults;
import com.onehumanawa.cfhcore.content.gui.server.service.Hooks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Commands.class})
public abstract class CommandsMixin {
	@Inject(
		method = {"performCommand"},
		at = {@At("HEAD")},
		cancellable = true
	)
	private void cfhServer$heldOrMuted(ParseResults<CommandSourceStack> parseResults, String command, CallbackInfo ci) {
		if (Hooks.commandRefused((CommandSourceStack)parseResults.getContext().getSource(), command)) {
			ci.cancel();
		}
	}
}
