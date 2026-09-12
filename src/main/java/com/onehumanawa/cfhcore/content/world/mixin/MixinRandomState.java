package com.onehumanawa.cfhcore.content.world.mixin;

import com.google.common.base.Suppliers;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import net.minecraft.world.level.levelgen.DensityFunction.NoiseHolder;
import net.minecraft.world.level.levelgen.DensityFunction.Visitor;
import net.minecraft.world.level.levelgen.synth.NormalNoise.NoiseParameters;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.onehumanawa.cfhcore.content.world.CFHWorldGenCommon;
import com.onehumanawa.cfhcore.content.world.concurrent.ThreadPools;
import com.onehumanawa.cfhcore.content.world.preset.settings.Preset;
import com.onehumanawa.cfhcore.content.world.preset.settings.Presets;
import com.onehumanawa.cfhcore.content.world.registries.CFHWorldGenRegistries;
import com.onehumanawa.cfhcore.content.world.worldgen.GeneratorContext;
import com.onehumanawa.cfhcore.content.world.worldgen.CFHWorldGenRandomState;
import com.onehumanawa.cfhcore.content.world.worldgen.densityfunction.CellSampler;
import com.onehumanawa.cfhcore.content.world.worldgen.densityfunction.NoiseFunction;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noise;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noises;

@Mixin({RandomState.class})
@Implements({@Interface(
      iface = CFHWorldGenRandomState.class,
      prefix = "cfhcore$CFHWorldGenRandomState$"
   )})
class MixinRandomState {
   private Visitor densityFunctionWrapper;
   @Shadow
   @Final
   private Sampler sampler;
   @Shadow
   @Final
   private SurfaceSystem surfaceSystem;
   private boolean hasContext;
   @Nullable
   private GeneratorContext generatorContext;
   @Nullable
   private Preset preset;
   private long seed;

   @Redirect(
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/levelgen/NoiseRouter;mapAll(Lnet/minecraft/world/level/levelgen/DensityFunction$Visitor;)Lnet/minecraft/world/level/levelgen/NoiseRouter;"
      ),
      method = {"<init>(Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;Lnet/minecraft/core/HolderGetter;J)V"},
      require = 1
   )
   private NoiseRouter RandomState(
      NoiseRouter router, Visitor visitor, NoiseGeneratorSettings noiseGeneratorSettings, HolderGetter<NoiseParameters> params, long seed
   ) {
      this.seed = seed;
      this.densityFunctionWrapper = new Visitor() {
         @Override
         public DensityFunction apply(DensityFunction function) {
            if (function instanceof NoiseFunction.Marker marker) {
               return new NoiseFunction(marker.noise(), (int)seed);
            } else if (function instanceof CellSampler.Marker marker) {
               MixinRandomState.this.hasContext |= true;
               return new CellSampler(Suppliers.memoize(() -> MixinRandomState.this.generatorContext.lookup), marker.field());
            } else {
               return visitor.apply(function);
            }
         }

         @Override
         public NoiseHolder visitNoise(NoiseHolder noiseHolder) {
            return visitor.visitNoise(noiseHolder);
         }
      };
      return router.mapAll(this.densityFunctionWrapper);
   }

   public void cfhcore$CFHWorldGenRandomState$initialize(RegistryAccess registries) {
      // hasContext is only set while mapping this dimension's own noise router, so CFHWorldGen
      // terrain kicks in exclusively for dimensions whose noise settings reference the cell
      // markers (the overworld, which the built-in datapack overrides). Nether, end and modded
      // dimensions keep generating exactly like vanilla.
      if (!this.hasContext) {
         return;
      }

      RegistryLookup<Noise> noises = registries.lookupOrThrow(CFHWorldGenRegistries.NOISE);
      // CFHCore has no preset editor or preset registry: the terrain is hardcoded.
      this.preset = Presets.DEFAULT;
      this.generatorContext = GeneratorContext.makeCached(
         this.preset, noises, (int)this.seed, CFHWorldGenCommon.TILE_SIZE, CFHWorldGenCommon.BATCH_COUNT, ThreadPools.availableProcessors() > 4
      );
   }

   @Nullable
   public Preset cfhcore$CFHWorldGenRandomState$preset() {
      return this.preset;
   }

   @Nullable
   public GeneratorContext cfhcore$CFHWorldGenRandomState$generatorContext() {
      return this.generatorContext;
   }

   @Nullable
   public DensityFunction cfhcore$CFHWorldGenRandomState$wrap(DensityFunction function) {
      return function.mapAll(this.densityFunctionWrapper);
   }

   public Noise cfhcore$CFHWorldGenRandomState$seed(Noise noise) {
      return Noises.shiftSeed(noise, (int)this.seed);
   }
}
