package com.onehumanawa.cfhcore.content.gui.server.mixin.server;

import com.onehumanawa.cfhcore.content.gui.server.service.Hooks;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({CommandSourceStack.class})
public abstract class CommandSourceStackMixin {
	@Inject(
		method = {"getOnlinePlayerNames"},
		at = {@At("RETURN")},
		cancellable = true
	)
	private void cfhServer$withoutTheVanished(CallbackInfoReturnable<Collection<String>> cir) {
		if (cir.getReturnValue() instanceof List<String> names) {
			List<String> shown = Hooks.shownNames((CommandSourceStack)(Object)this, names);
			if (shown != names) {
				cir.setReturnValue(shown);
			}
		}
	}
}
