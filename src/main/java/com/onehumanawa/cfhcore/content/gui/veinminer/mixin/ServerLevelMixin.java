package com.onehumanawa.cfhcore.content.gui.veinminer.mixin;

import com.onehumanawa.cfhcore.content.gui.veinminer.server.DropCollector;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ServerLevel.class})
public abstract class ServerLevelMixin {
	@Inject(
		method = {"addFreshEntity"},
		at = {@At("HEAD")},
		cancellable = true
	)
	private void cfhVeinminer$drops(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (DropCollector.intercept((ServerLevel)(Object)this, entity)) {
			cir.setReturnValue(true);
		}
	}
}
