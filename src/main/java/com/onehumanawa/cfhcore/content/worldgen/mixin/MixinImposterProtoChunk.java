package com.onehumanawa.cfhcore.content.worldgen.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import com.onehumanawa.cfhcore.content.worldgen.CFHChunk;

@Mixin(ImposterProtoChunk.class)
public abstract class MixinImposterProtoChunk implements CFHChunk {

	@Inject(
		method = "<init>",
		at = @At("TAIL")
	)
	public void init(LevelChunk levelChunk, boolean bl, CallbackInfo callback) {
		CFHChunk rtfChunk = (CFHChunk) levelChunk;
		rtfChunk.getMaxHeight().ifPresent(this::setMaxHeight);
	}
}
