package com.onehumanawa.cfhcore.content.world.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noise;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noises;

@Deprecated
public class ColumnDecorator {
   private static final Noise VARIANCE = Noises.perlin(0, 100, 1);

   public static void fillDownSolid(ChunkAccess chunk, MutableBlockPos pos, int from, int to, BlockState state) {
      for (int dy = from; dy > to; dy--) {
         replaceSolid(chunk, pos.setY(dy), state);
      }
   }

   public static void replaceSolid(ChunkAccess chunk, BlockPos pos, BlockState state) {
      if (!chunk.getBlockState(pos).isAir()) {
         chunk.setBlockState(pos, state, false);
      }
   }

   public static float sampleNoise(float x, float z, float scale, float bias) {
      return VARIANCE.compute(x, z, 0) * scale + bias;
   }

   public static float sampleNoise(float x, float z, int scale, int bias) {
      return sampleNoise(x, z, (float)scale / 255.0F, (float)bias / 255.0F);
   }
}
