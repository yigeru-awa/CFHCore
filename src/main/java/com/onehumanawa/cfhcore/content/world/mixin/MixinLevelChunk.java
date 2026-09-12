package com.onehumanawa.cfhcore.content.world.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk.PostLoadProcessor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.onehumanawa.cfhcore.content.world.worldgen.CFHWorldGenChunk;

@Mixin({LevelChunk.class})
public abstract class MixinLevelChunk implements CFHWorldGenChunk {
   @Inject(
      method = {"<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V"},
      at = {@At("TAIL")}
   )
   public void init(ServerLevel serverLevel, ProtoChunk protoChunk, @Nullable PostLoadProcessor postLoadProcessor, CallbackInfo callback) {
      CFHWorldGenChunk cfhWorldGenChunk = (CFHWorldGenChunk)protoChunk;
      cfhWorldGenChunk.getMaxHeight().ifPresent(this::setMaxHeight);
   }
}
