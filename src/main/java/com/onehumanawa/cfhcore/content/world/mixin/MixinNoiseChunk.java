package com.onehumanawa.cfhcore.content.world.mixin;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.Aquifer.FluidPicker;
import net.minecraft.world.level.levelgen.Aquifer.FluidStatus;
import net.minecraft.world.level.levelgen.DensityFunction.Visitor;
import net.minecraft.world.level.levelgen.DensityFunctions.BeardifierOrMarker;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.onehumanawa.cfhcore.content.world.preset.settings.Preset;
import com.onehumanawa.cfhcore.content.world.worldgen.ActiveChunk;
import com.onehumanawa.cfhcore.content.world.worldgen.GeneratorContext;
import com.onehumanawa.cfhcore.content.world.worldgen.MaxHeightUtil;
import com.onehumanawa.cfhcore.content.world.worldgen.CFHWorldGenChunk;
import com.onehumanawa.cfhcore.content.world.worldgen.CFHWorldGenRandomState;
import com.onehumanawa.cfhcore.content.world.worldgen.densityfunction.CellSampler;
import com.onehumanawa.cfhcore.content.world.worldgen.densityfunction.tile.Tile;

@Mixin({NoiseChunk.class})
class MixinNoiseChunk {
   private RandomState randomState;
   private NoiseGeneratorSettings generatorSettings;
   private int chunkX;
   private int chunkZ;
   @Nullable
   private Tile.Chunk chunk;
   private CellSampler.Cache2d cache2d;
   @Shadow
   @Mutable
   private int cellCountY;
   @Shadow
   @Final
   private int cellHeight;
   @Shadow
   @Final
   int firstNoiseX;
   @Shadow
   @Final
   int firstNoiseZ;
   @Shadow
   @Final
   private int cellCountXZ;

   @Redirect(
      method = {"<init>(ILnet/minecraft/world/level/levelgen/RandomState;IILnet/minecraft/world/level/levelgen/NoiseSettings;Lnet/minecraft/world/level/levelgen/DensityFunctions$BeardifierOrMarker;Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;Lnet/minecraft/world/level/levelgen/Aquifer$FluidPicker;Lnet/minecraft/world/level/levelgen/blending/Blender;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/levelgen/NoiseRouter;mapAll(Lnet/minecraft/world/level/levelgen/DensityFunction$Visitor;)Lnet/minecraft/world/level/levelgen/NoiseRouter;"
      )
   )
   private NoiseRouter init(
      NoiseRouter noiseRouter,
      Visitor visitor,
      int cellCountXZ,
      RandomState randomState,
      int minBlockX,
      int minBlockZ,
      NoiseSettings noiseSettings,
      BeardifierOrMarker beardifierOrMarker,
      NoiseGeneratorSettings generatorSettings
   ) {
      this.randomState = randomState;
      this.generatorSettings = generatorSettings;
      this.chunkX = SectionPos.blockToSectionCoord(minBlockX);
      this.chunkZ = SectionPos.blockToSectionCoord(minBlockZ);
      GeneratorContext generatorContext;
      if (randomState instanceof CFHWorldGenRandomState cfhWorldGenRandomState && cellCountXZ > 1 && (generatorContext = cfhWorldGenRandomState.generatorContext()) != null) {
         this.chunk = generatorContext.cache.provideAtChunk(this.chunkX, this.chunkZ).getChunkReader(this.chunkX, this.chunkZ);
         CFHWorldGenChunk cfhWorldGenChunk = (CFHWorldGenChunk)ActiveChunk.get();
         int maxHeight = Math.min(
            noiseSettings.height(),
            MaxHeightUtil.getMaxHeight(
               this.chunkX, this.chunkZ, cfhWorldGenChunk.getMaxHeight().orElseGet(noiseSettings::height), generatorSettings, noiseSettings, beardifierOrMarker
            )
         );
         this.cellCountY = Math.min(this.cellCountY, maxHeight / this.cellHeight);
      }

      this.cache2d = new CellSampler.Cache2d();
      return randomState.router();
   }

   @ModifyVariable(
      method = {"<init>(ILnet/minecraft/world/level/levelgen/RandomState;IILnet/minecraft/world/level/levelgen/NoiseSettings;Lnet/minecraft/world/level/levelgen/DensityFunctions$BeardifierOrMarker;Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;Lnet/minecraft/world/level/levelgen/Aquifer$FluidPicker;Lnet/minecraft/world/level/levelgen/blending/Blender;)V"},
      at = @At("HEAD"),
      name = {"fluidPicker"},
      index = 7,
      ordinal = 0,
      argsOnly = true
   )
   private static FluidPicker modifyFluidPicker(
      FluidPicker fluidPicker,
      int i,
      RandomState randomState,
      int j,
      int k,
      NoiseSettings noiseSettings,
      BeardifierOrMarker beardifierOrMarker,
      NoiseGeneratorSettings noiseGeneratorSettings
   ) {
      if (randomState instanceof CFHWorldGenRandomState cfhWorldGenRandomState) {
         Preset preset = cfhWorldGenRandomState.preset();
         if (preset != null && cfhWorldGenRandomState.generatorContext() != null) {
            int lavaLevel = preset.world().properties.lavaLevel;
            FluidStatus lava = new FluidStatus(lavaLevel, Blocks.LAVA.defaultBlockState());
            int seaLevel = noiseGeneratorSettings.seaLevel();
            FluidStatus defaultFluid = new FluidStatus(seaLevel, noiseGeneratorSettings.defaultFluid());
            return (x, y, z) -> y < Math.min(lavaLevel, seaLevel) ? lava : defaultFluid;
         }
      }

      return fluidPicker;
   }

   @Inject(
      at = {@At("HEAD")},
      method = {"wrapNew(Lnet/minecraft/world/level/levelgen/DensityFunction;)Lnet/minecraft/world/level/levelgen/DensityFunction;"},
      cancellable = true
   )
   private void wrapNew(DensityFunction function, CallbackInfoReturnable<DensityFunction> callback) {
      if (this.randomState instanceof CFHWorldGenRandomState randomState && function instanceof CellSampler mapped) {
         callback.setReturnValue(mapped.new CacheChunk(this.chunk, this.cache2d, this.chunkX, this.chunkZ));
      }
   }

   @ModifyArg(
      method = {"<init>(ILnet/minecraft/world/level/levelgen/RandomState;IILnet/minecraft/world/level/levelgen/NoiseSettings;Lnet/minecraft/world/level/levelgen/DensityFunctions$BeardifierOrMarker;Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;Lnet/minecraft/world/level/levelgen/Aquifer$FluidPicker;Lnet/minecraft/world/level/levelgen/blending/Blender;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/levelgen/Aquifer;create(Lnet/minecraft/world/level/levelgen/NoiseChunk;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/levelgen/NoiseRouter;Lnet/minecraft/world/level/levelgen/PositionalRandomFactory;IILnet/minecraft/world/level/levelgen/Aquifer$FluidPicker;)Lnet/minecraft/world/level/levelgen/Aquifer;"
      ),
      index = 5
   )
   private int capAquiferHeight(int height) {
      if (this.randomState instanceof CFHWorldGenRandomState cfhWorldGenRandomState && cfhWorldGenRandomState.generatorContext() != null) {
         return Math.min(height, this.cellCountY * this.cellHeight);
      }

      return height;
   }

   @Redirect(
      method = {"computePreliminarySurfaceLevel(J)I"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/levelgen/NoiseSettings;height()I"
      )
   )
   private int capPreliminarySurfaceScan(NoiseSettings noiseSettings) {
      GeneratorContext generatorContext;
      if (this.randomState instanceof CFHWorldGenRandomState cfhWorldGenRandomState
         && (generatorContext = cfhWorldGenRandomState.generatorContext()) != null
         && this.generatorSettings != null) {
         return MaxHeightUtil.getGlobalScanHeight(noiseSettings, this.generatorSettings, generatorContext.levels.worldHeight);
      }

      return noiseSettings.height();
   }
}
