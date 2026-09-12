package com.onehumanawa.cfhcore.content.world.mixin;

import java.util.OptionalInt;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.onehumanawa.cfhcore.content.world.worldgen.ActiveChunk;
import com.onehumanawa.cfhcore.content.world.worldgen.GeneratorContext;
import com.onehumanawa.cfhcore.content.world.worldgen.MaxHeightUtil;
import com.onehumanawa.cfhcore.content.world.worldgen.CFHWorldGenChunk;
import com.onehumanawa.cfhcore.content.world.worldgen.CFHWorldGenRandomState;
import com.onehumanawa.cfhcore.content.world.worldgen.biome.ColumnCachedBiomeResolver;
import com.onehumanawa.cfhcore.content.world.worldgen.densityfunction.tile.Tile;

@Mixin({NoiseBasedChunkGenerator.class})
abstract class MixinNoiseBasedChunkGenerator extends ChunkGenerator {
   @Shadow
   @Final
   private Holder<NoiseGeneratorSettings> settings;

   public MixinNoiseBasedChunkGenerator(BiomeSource biomeSource, Function<Holder<Biome>, BiomeGenerationSettings> settingsGetter) {
      super(biomeSource, settingsGetter);
   }

   @Inject(
      method = {"doCreateBiomes(Lnet/minecraft/world/level/levelgen/blending/Blender;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/chunk/ChunkAccess;)V"},
      at = {@At("HEAD")}
   )
   public void doCreateBiomes(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk, CallbackInfo callback) {
      CFHWorldGenRandomState cfhWorldGenRandomState = (CFHWorldGenRandomState)randomState;
      GeneratorContext generatorContext = cfhWorldGenRandomState.generatorContext();
      if (generatorContext != null) {
         CFHWorldGenChunk cfhWorldGenChunk = (CFHWorldGenChunk)chunk;
         ChunkPos chunkPos = chunk.getPos();
         Tile tile = generatorContext.cache.provideAtChunk(chunkPos.x, chunkPos.z);
         Tile.Chunk tileChunk = tile.getChunkReader(chunkPos.x, chunkPos.z);
         float maxHeight = Float.MIN_VALUE;

         for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
               float cellHeight = tileChunk.getCell(x, z).height * 256.0F;
               if (cellHeight > maxHeight) {
                  maxHeight = cellHeight;
               }
            }
         }

         cfhWorldGenChunk.setMaxHeight(Mth.ceil(maxHeight));
      }
   }

   @Redirect(
      method = {"fillFromNoise(Lnet/minecraft/world/level/levelgen/blending/Blender;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/chunk/ChunkAccess;)Ljava/util/concurrent/CompletableFuture;", "populateNoise"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/levelgen/NoiseSettings;height()I"
      )
   )
   public int fillFromNoise(NoiseSettings settings, Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
      CFHWorldGenChunk cfhWorldGenChunk = (CFHWorldGenChunk)chunk;
      int maxHeight = cfhWorldGenChunk.getMaxHeight().orElseGet(settings::height);
      return MaxHeightUtil.getMaxHeight(chunk.getPos(), maxHeight, this.settings.value(), settings, structureManager);
   }

   @Inject(
      method = {"createNoiseChunk(Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/levelgen/blending/Blender;Lnet/minecraft/world/level/levelgen/RandomState;)Lnet/minecraft/world/level/levelgen/NoiseChunk;"},
      at = {@At("HEAD")}
   )
   private void createNoiseChunk(
      ChunkAccess chunkAccess, StructureManager structureManager, Blender blender, RandomState randomState, CallbackInfoReturnable<NoiseChunk> callback
   ) {
      ActiveChunk.set(chunkAccess);
   }

   @Redirect(
      method = {"iterateNoiseColumn(Lnet/minecraft/world/level/LevelHeightAccessor;Lnet/minecraft/world/level/levelgen/RandomState;IILorg/apache/commons/lang3/mutable/MutableObject;Ljava/util/function/Predicate;)Ljava/util/OptionalInt;"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/levelgen/NoiseSettings;clampToHeightAccessor(Lnet/minecraft/world/level/LevelHeightAccessor;)Lnet/minecraft/world/level/levelgen/NoiseSettings;"
      )
   )
   private NoiseSettings iterateNoiseColumn(
      NoiseSettings noiseSettings,
      LevelHeightAccessor heightAccessor,
      LevelHeightAccessor levelHeightAccessor,
      RandomState randomState,
      int blockX,
      int blockZ,
      @Nullable MutableObject<NoiseColumn> mutableObject,
      @Nullable Predicate<BlockState> predicate
   ) {
      NoiseSettings clamped = noiseSettings.clampToHeightAccessor(heightAccessor);
      GeneratorContext generatorContext;
      if (randomState instanceof CFHWorldGenRandomState cfhWorldGenRandomState && (generatorContext = cfhWorldGenRandomState.generatorContext()) != null) {
         clamped = MaxHeightUtil.capNoiseSettingsForColumn(clamped, this.settings.value(), generatorContext.lookup, blockX, blockZ);
      }

      return clamped;
   }

   @Redirect(
      method = {"doCreateBiomes(Lnet/minecraft/world/level/levelgen/blending/Blender;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/chunk/ChunkAccess;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/chunk/ChunkAccess;fillBiomesFromNoise(Lnet/minecraft/world/level/biome/BiomeResolver;Lnet/minecraft/world/level/biome/Climate$Sampler;)V"
      )
   )
   private void fillBiomesFromNoise(
      ChunkAccess chunkAccess,
      BiomeResolver resolver,
      Sampler sampler,
      Blender blender,
      RandomState randomState,
      StructureManager structureManager,
      ChunkAccess chunk
   ) {
      CFHWorldGenRandomState cfhWorldGenRandomState = (CFHWorldGenRandomState)randomState;
      OptionalInt maxHeight;
      if (cfhWorldGenRandomState.generatorContext() != null
         && blender == Blender.empty()
         && !chunkAccess.isUpgrading()
         && (maxHeight = ((CFHWorldGenChunk)chunkAccess).getMaxHeight()).isPresent()) {
         int seaLevel = this.settings.value().seaLevel();
         int skyBlockY = Math.max(seaLevel, maxHeight.getAsInt() + 1) + 8;
         int skyQuartY = QuartPos.fromBlock(skyBlockY) + 1;
         resolver = new ColumnCachedBiomeResolver(resolver, skyQuartY);
      }

      chunkAccess.fillBiomesFromNoise(resolver, sampler);
   }
}
