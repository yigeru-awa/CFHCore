package com.onehumanawa.cfhcore.content.world.worldgen.feature.placement.poisson;

import com.mojang.serialization.MapCodec;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.Cell;
import com.onehumanawa.cfhcore.content.world.worldgen.densityfunction.tile.Tile;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noise;

public class BiomeVariance implements Noise {
   public static final BiomeVariance NONE = new BiomeVariance(null, 0.0F) {
      @Override
      public float compute(float x, float y, int seed) {
         return 1.0F;
      }
   };
   public static final float MIN_FADE = 0.025F;
   public static final float NO_SPREAD = 1.0F;
   private static final float SPREAD_VARIANCE = 1.0F;
   private static final float MAX_SPREAD = 2.0F;
   private final float fade;
   private final float range;
   private final Tile.Chunk chunk;

   public BiomeVariance(Tile.Chunk chunk, float fade) {
      this.chunk = chunk;
      this.fade = fade;
      this.range = fade - 0.025F;
   }

   @Override
   public float compute(float x, float z, int seed) {
      Cell cell = this.chunk.getCell((int)x, (int)z);
      float edge = 0.02F + cell.biomeRegionEdge;
      if (edge >= this.fade) {
         return 1.0F;
      } else if (edge <= 0.025F) {
         return 2.0F;
      } else {
         float alpha = (edge - 0.025F) / this.range;
         alpha = 1.0F - alpha;
         return 1.0F + alpha * 1.0F;
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
   public MapCodec<BiomeVariance> codec() {
      throw new UnsupportedOperationException();
   }
}
