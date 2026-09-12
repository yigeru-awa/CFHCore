package com.onehumanawa.cfhcore.content.world.preset;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import com.onehumanawa.cfhcore.content.world.preset.settings.Preset;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.function.DistanceFunction;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.function.EdgeFunction;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.function.Interpolation;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noise;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noises;

public class PresetTerrainNoise {
   public static final ResourceKey<Noise> MOUNTAIN_CHAIN_ALPHA = createKey("mountain_chain_alpha");
   public static final ResourceKey<Noise> RIVER_ALPHA = createKey("river_alpha");
   public static final ResourceKey<Noise> LAKE_ALPHA = createKey("lake_alpha");
   public static final ResourceKey<Noise> SWAMP_ALPHA = createKey("swamp_alpha");
   public static final ResourceKey<Noise> EROSION = createKey("erosion");
   public static final ResourceKey<Noise> RIDGES = createKey("ridges");
   public static final ResourceKey<Noise> RIDGES_FOLDED = createKey("ridges_folded");
   public static final ResourceKey<Noise> OFFSET = createKey("offset");

   public static void bootstrap(Preset preset, BootstrapContext<Noise> ctx) {
      PresetTerrainTypeNoise.bootstrap(preset, ctx);
      Noise mountainChainAlpha = PresetNoiseData.registerAndWrap(ctx, MOUNTAIN_CHAIN_ALPHA, makeMountainChainAlpha(0));
      Noise erosion = PresetNoiseData.registerAndWrap(ctx, EROSION, makeErosion(mountainChainAlpha));
      Noise ridges = PresetNoiseData.registerAndWrap(ctx, RIDGES, makeRidges(mountainChainAlpha));
   }

   private static Noise makeMountainChainAlpha(int mountainSeed) {
      Noise mountainShape = Noises.worleyEdge(mountainSeed++, 1000, EdgeFunction.DISTANCE_2_ADD, DistanceFunction.EUCLIDEAN);
      mountainShape = Noises.warpPerlin(mountainShape, mountainSeed++, 333, 2, 250.0F);
      mountainShape = Noises.curve(mountainShape, Interpolation.CURVE3);
      mountainShape = Noises.clamp(mountainShape, 0.0F, 0.9F);
      return Noises.map(mountainShape, 0.0F, 1.0F);
   }

   private static Noise peaksAndValleys(Noise noise) {
      return Noises.mul(
         Noises.add(Noises.abs(Noises.add(Noises.abs(noise), Noises.constant(-0.6666667F))), Noises.constant(-0.33333334F)), Noises.constant(-3.0F)
      );
   }

   private static Noise makeErosion(Noise mountainChainAlpha) {
      return Noises.mul(Noises.perlin(0, 1, 200), Noises.worley(0, 200));
   }

   private static Noise makeRidges(Noise mountainChainAlpha) {
      return Noises.mul(Noises.perlin(0, 1, 200), Noises.worley(0, 200));
   }

   protected static ResourceKey<Noise> createKey(String name) {
      return PresetNoiseData.createKey("terrain/" + name);
   }
}
