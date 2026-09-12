package com.onehumanawa.cfhcore.content.world.worldgen.cell.biome.spawn;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Climate.ParameterPoint;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.biome.Climate.TargetPoint;
import com.onehumanawa.cfhcore.content.world.worldgen.biome.CFHWorldGenClimateSampler;

public class SpawnFinderFix {
   public SpawnFinderFix.Result result;

   public SpawnFinderFix(List<ParameterPoint> list, Sampler sampler) {
      // Sampler is a record, so it is final; the (Object) widening keeps the duck check legal.
      // MixinClimateSampler grafts CFHWorldGenClimateSampler onto it at runtime.
      if ((Object)sampler instanceof CFHWorldGenClimateSampler cfhWorldGenClimateSampler) {
         BlockPos center = cfhWorldGenClimateSampler.getSpawnSearchCenter();
         this.result = getSpawnPositionAndFitness(list, sampler, center.getX(), center.getZ());
         this.radialSearch(list, sampler, 2048.0F, 512.0F);
         this.radialSearch(list, sampler, 512.0F, 32.0F);
      }
   }

   private void radialSearch(List<ParameterPoint> list, Sampler sampler, float f, float g) {
      float h = 0.0F;
      float i = g;
      BlockPos blockPos = this.result.location();

      while (i <= f) {
         int j = blockPos.getX() + (int)(Math.sin((double)h) * (double)i);
         SpawnFinderFix.Result result = getSpawnPositionAndFitness(list, sampler, j, blockPos.getZ() + (int)(Math.cos((double)h) * (double)i));
         if (result.fitness() < this.result.fitness()) {
            this.result = result;
         }

         if ((double)(h += g / i) > Math.PI * 2) {
            h = 0.0F;
            i += g;
         }
      }
   }

   private static SpawnFinderFix.Result getSpawnPositionAndFitness(List<ParameterPoint> list, Sampler sampler, int i, int j) {
      double d = Mth.square(2500.0);
      long l = (long)((double)Mth.square(10000.0F) * Math.pow((double)(Mth.square((long)i) + Mth.square((long)j)) / d, 2.0));
      TargetPoint targetPoint = sampler.sample(QuartPos.fromBlock(i), 0, QuartPos.fromBlock(j));
      TargetPoint targetPoint2 = new TargetPoint(
         targetPoint.temperature(), targetPoint.humidity(), targetPoint.continentalness(), targetPoint.erosion(), 0L, targetPoint.weirdness()
      );
      long m = Long.MAX_VALUE;

      for (ParameterPoint parameterPoint : list) {
         m = Math.min(m, parameterPoint.fitness(targetPoint2));
      }

      return new SpawnFinderFix.Result(new BlockPos(i, 0, j), l + m);
   }

   public static record Result(BlockPos location, long fitness) {
   }
}
