package com.onehumanawa.cfhcore.content.world.mixin;

import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.onehumanawa.cfhcore.content.world.worldgen.CFHWorldGenChunk;

@Mixin({ImposterProtoChunk.class})
public abstract class MixinImposterProtoChunk implements CFHWorldGenChunk {
   @Inject(
      method = {"<init>(Lnet/minecraft/world/level/chunk/LevelChunk;Z)V"},
      at = {@At("TAIL")}
   )
   public void init(LevelChunk levelChunk, boolean bl, CallbackInfo callback) {
      CFHWorldGenChunk cfhWorldGenChunk = (CFHWorldGenChunk)levelChunk;
      cfhWorldGenChunk.getMaxHeight().ifPresent(this::setMaxHeight);
   }
}
