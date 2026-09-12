package com.onehumanawa.cfhcore.content.world.neoforge.mixin;

import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatusTasks;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.onehumanawa.cfhcore.content.world.worldgen.GeneratorContext;
import com.onehumanawa.cfhcore.content.world.worldgen.CFHWorldGenRandomState;

@Mixin({ChunkStatusTasks.class})
public class MixinChunkStatusTasks {
   @Inject(
      at = {@At("HEAD")},
      method = {"generateStructureStarts(Lnet/minecraft/world/level/chunk/status/WorldGenContext;Lnet/minecraft/world/level/chunk/status/ChunkStep;Lnet/minecraft/util/StaticCache2D;Lnet/minecraft/world/level/chunk/ChunkAccess;)Ljava/util/concurrent/CompletableFuture;"},
      require = 1
   )
   private static void generateStructureStarts(
      WorldGenContext worldGenContext,
      ChunkStep chunkStep,
      StaticCache2D<GenerationChunkHolder> staticCache2D,
      ChunkAccess chunkAccess,
      CallbackInfoReturnable<CompletableFuture<ChunkAccess>> callback
   ) {
      if (worldGenContext.level().getChunkSource().randomState() instanceof CFHWorldGenRandomState cfhWorldGenRandomState) {
         ChunkPos chunkPos = chunkAccess.getPos();
         GeneratorContext context = cfhWorldGenRandomState.generatorContext();
         if (context != null) {
            context.cache.queueAtChunk(chunkPos.x, chunkPos.z);
         }
      }
   }

   @Inject(
      at = {@At("TAIL")},
      method = {"generateFeatures(Lnet/minecraft/world/level/chunk/status/WorldGenContext;Lnet/minecraft/world/level/chunk/status/ChunkStep;Lnet/minecraft/util/StaticCache2D;Lnet/minecraft/world/level/chunk/ChunkAccess;)Ljava/util/concurrent/CompletableFuture;"},
      require = 1
   )
   private static void generateFeatures(
      WorldGenContext worldGenContext,
      ChunkStep chunkStep,
      StaticCache2D<GenerationChunkHolder> staticCache2D,
      ChunkAccess chunkAccess,
      CallbackInfoReturnable<CompletableFuture<ChunkAccess>> callback
   ) {
      if (worldGenContext.level().getChunkSource().randomState() instanceof CFHWorldGenRandomState cfhWorldGenRandomState) {
         ChunkPos chunkPos = chunkAccess.getPos();
         GeneratorContext context = cfhWorldGenRandomState.generatorContext();
         if (context != null) {
            context.cache.dropAtChunk(chunkPos.x, chunkPos.z);
         }
      }
   }
}
