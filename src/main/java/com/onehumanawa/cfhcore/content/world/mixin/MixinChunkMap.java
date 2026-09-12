package com.onehumanawa.cfhcore.content.world.mixin;

import com.mojang.datafixers.DataFixer;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.onehumanawa.cfhcore.content.world.worldgen.CFHWorldGenRandomState;

@Mixin({ChunkMap.class})
public class MixinChunkMap {
   @Shadow
   private RandomState randomState;

   @Inject(
      at = {@At("TAIL")},
      method = {"<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;Lcom/mojang/datafixers/DataFixer;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplateManager;Ljava/util/concurrent/Executor;Lnet/minecraft/util/thread/BlockableEventLoop;Lnet/minecraft/world/level/chunk/LightChunkGetter;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/server/level/progress/ChunkProgressListener;Lnet/minecraft/world/level/entity/ChunkStatusUpdateListener;Ljava/util/function/Supplier;IZ)V"}
   )
   public void ChunkMap(
      ServerLevel serverLevel,
      LevelStorageAccess storageAccess,
      DataFixer dataFixer,
      StructureTemplateManager templateLoader,
      Executor executor,
      BlockableEventLoop<Runnable> eventLoop,
      LightChunkGetter lightChunkGetter,
      ChunkGenerator chunkGenerator,
      ChunkProgressListener chunkProgressListener,
      ChunkStatusUpdateListener chunkStatusListener,
      Supplier<DimensionDataStorage> dimensionStorage,
      int viewDistance,
      boolean syncChunkWrites,
      CallbackInfo callback
   ) {
      if (this.randomState instanceof CFHWorldGenRandomState cfhWorldGenRandomState) {
         cfhWorldGenRandomState.initialize(serverLevel.registryAccess());
      }
   }
}
