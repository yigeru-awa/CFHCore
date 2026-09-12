package com.onehumanawa.cfhcore.content.world.worldgen.noise.domain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import com.onehumanawa.cfhcore.content.world.registries.CFHWorldGenBuiltInRegistries;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noise;
import com.onehumanawa.cfhcore.content.world.worldgen.util.PosUtil;

public interface Domain {
   Codec<Domain> CODEC = CFHWorldGenBuiltInRegistries.DOMAIN_TYPE.byNameCodec().dispatch(Domain::codec, Function.identity());

   float getOffsetX(float var1, float var2, int var3);

   float getOffsetZ(float var1, float var2, int var3);

   Domain mapAll(Noise.Visitor var1);

   MapCodec<? extends Domain> codec();

   default float getX(float x, float z, int seed) {
      return x + this.getOffsetX(x, z, seed);
   }

   default float getZ(float x, float z, int seed) {
      return z + this.getOffsetZ(x, z, seed);
   }

   default long getOffset(float x, float z, int seed) {
      return PosUtil.packf(this.getOffsetX(x, z, seed), this.getOffsetZ(x, z, seed));
   }

   default long getPos(float x, float z, int seed) {
      long offset = this.getOffset(x, z, seed);
      return PosUtil.packf(x + PosUtil.unpackLeftf(offset), z + PosUtil.unpackRightf(offset));
   }
}
