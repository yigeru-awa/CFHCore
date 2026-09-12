package com.onehumanawa.cfhcore.content.world.worldgen.feature.placement.poisson;

import com.mojang.serialization.MapCodec;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.NoiseUtil;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noise;

public class DensityNoise implements Noise {
   private BiomeVariance biome;
   private Noise variance;

   public DensityNoise(BiomeVariance biome, Noise variance) {
      this.biome = biome;
      this.variance = variance;
   }

   @Override
   public float compute(float x, float y, int seed) {
      float value1 = this.biome.compute(x, y, 0);
      if (value1 > 2.0F) {
         return value1;
      } else {
         float value2 = this.variance.compute(x, y, 0);
         return value1 > 1.0F ? NoiseUtil.lerp(value2, value1, (value1 - 0.25F) / 0.25F) : value2;
      }
   }

   @Override
   public float minValue() {
      return 0.0F;
   }

   @Override
   public float maxValue() {
      return 1.0F;
   }

   @Override
   public Noise mapAll(Noise.Visitor visitor) {
      return visitor.apply(this);
   }

   @Override
   public MapCodec<DensityNoise> codec() {
      throw new UnsupportedOperationException();
   }
}
