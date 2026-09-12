package com.onehumanawa.cfhcore.content.world.worldgen.noise.domain;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noise;
import com.onehumanawa.cfhcore.content.world.worldgen.util.PosUtil;

public record CompoundWarp(Domain input1, Domain input2) implements Domain {
   public static final MapCodec<CompoundWarp> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(Domain.CODEC.fieldOf("input1").forGetter(CompoundWarp::input1), Domain.CODEC.fieldOf("input2").forGetter(CompoundWarp::input2))
            .apply(instance, CompoundWarp::new)
   );

   @Override
   public float getOffsetX(float x, float z, int seed) {
      float ax = this.input1.getX(x, z, seed);
      float ay = this.input1.getZ(x, z, seed);
      return this.input2.getOffsetX(ax, ay, seed);
   }

   @Override
   public float getOffsetZ(float x, float z, int seed) {
      float ax = this.input1.getX(x, z, seed);
      float ay = this.input1.getZ(x, z, seed);
      return this.input2.getOffsetZ(ax, ay, seed);
   }

   @Override
   public long getOffset(float x, float z, int seed) {
      long warped = this.input1.getPos(x, z, seed);
      return this.input2.getOffset(PosUtil.unpackLeftf(warped), PosUtil.unpackRightf(warped), seed);
   }

   @Override
   public Domain mapAll(Noise.Visitor visitor) {
      return new CompoundWarp(this.input1.mapAll(visitor), this.input2.mapAll(visitor));
   }

   @Override
   public MapCodec<CompoundWarp> codec() {
      return CODEC;
   }
}
