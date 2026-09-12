package com.onehumanawa.cfhcore.content.world.worldgen.noise.domain;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.NoiseUtil;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noise;
import com.onehumanawa.cfhcore.content.world.worldgen.util.PosUtil;

record DirectionWarp(Noise direction, Noise strength) implements Domain {
   public static final MapCodec<DirectionWarp> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
               Noise.HOLDER_HELPER_CODEC.fieldOf("direction").forGetter(DirectionWarp::direction),
               Noise.HOLDER_HELPER_CODEC.fieldOf("strength").forGetter(DirectionWarp::strength)
            )
            .apply(instance, DirectionWarp::new)
   );

   @Override
   public float getOffsetX(float x, float z, int seed) {
      float angle = this.direction.compute(x, z, seed) * (float) (Math.PI * 2);
      return NoiseUtil.sin(angle) * this.strength.compute(x, z, seed);
   }

   @Override
   public float getOffsetZ(float x, float z, int seed) {
      float angle = this.direction.compute(x, z, seed) * (float) (Math.PI * 2);
      return NoiseUtil.cos(angle) * this.strength.compute(x, z, seed);
   }

   @Override
   public long getOffset(float x, float z, int seed) {
      float angle = this.direction.compute(x, z, seed) * (float) (Math.PI * 2);
      float strength = this.strength.compute(x, z, seed);
      return PosUtil.packf(NoiseUtil.sin(angle) * strength, NoiseUtil.cos(angle) * strength);
   }

   @Override
   public Domain mapAll(Noise.Visitor visitor) {
      return new DirectionWarp(this.direction.mapAll(visitor), this.strength.mapAll(visitor));
   }

   @Override
   public MapCodec<DirectionWarp> codec() {
      return CODEC;
   }
}
