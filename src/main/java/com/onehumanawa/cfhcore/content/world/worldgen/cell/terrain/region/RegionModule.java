package com.onehumanawa.cfhcore.content.world.worldgen.cell.terrain.region;

import com.onehumanawa.cfhcore.content.world.worldgen.cell.Cell;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.CellPopulator;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.heightmap.RegionConfig;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.NoiseUtil;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.domain.Domain;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.domain.Domains;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.function.DistanceFunction;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.function.EdgeFunction;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noises;
import com.onehumanawa.cfhcore.content.world.worldgen.util.PosUtil;

public class RegionModule implements CellPopulator {
   private int seed;
   private float frequency;
   private float edgeMin;
   private float edgeMax;
   private float edgeRange;
   private Domain warp;

   public RegionModule(RegionConfig regionConfig) {
      this.seed = regionConfig.seed() + 7;
      this.edgeMin = 0.0F;
      this.edgeMax = 0.5F;
      this.edgeRange = this.edgeMax - this.edgeMin;
      this.frequency = 1.0F / (float)regionConfig.scale();
      this.warp = Domains.domain(regionConfig.warpX(), regionConfig.warpZ(), Noises.constant(regionConfig.warpStrength()));
   }

   @Override
   public void apply(Cell cell, float x, float z) {
      long offset = this.warp.getOffset(x, z, 0);
      float px = x + PosUtil.unpackLeftf(offset);
      float py = z + PosUtil.unpackRightf(offset);
      px *= this.frequency;
      py *= this.frequency;
      int cellX = 0;
      int cellY = 0;
      int xi = NoiseUtil.floor(px);
      int yi = NoiseUtil.floor(py);
      float edgeDistance = Float.MAX_VALUE;
      float edgeDistance2 = Float.MAX_VALUE;
      DistanceFunction dist = DistanceFunction.NATURAL;

      for (int dy = -1; dy <= 1; dy++) {
         for (int dx = -1; dx <= 1; dx++) {
            int cx = xi + dx;
            int cy = yi + dy;
            NoiseUtil.Vec2f vec = NoiseUtil.cell(this.seed, cx, cy);
            float vecX = (float)cx + vec.x() * 0.7F;
            float vecY = (float)cy + vec.y() * 0.7F;
            float distance = dist.apply(vecX - px, vecY - py);
            if (distance < edgeDistance) {
               edgeDistance2 = edgeDistance;
               edgeDistance = distance;
               cellX = cx;
               cellY = cy;
            } else if (distance < edgeDistance2) {
               edgeDistance2 = distance;
            }
         }
      }

      cell.terrainRegionId = this.cellValue(this.seed, cellX, cellY);
      cell.terrainRegionEdge = this.edgeValue(edgeDistance, edgeDistance2);
      NoiseUtil.Vec2f winningVec = NoiseUtil.cell(this.seed, cellX, cellY);
      cell.terrainRegionCenterX = ((float)cellX + winningVec.x() * 0.7F) / this.frequency;
      cell.terrainRegionCenterZ = ((float)cellY + winningVec.y() * 0.7F) / this.frequency;
   }

   private float cellValue(int seed, int cellX, int cellY) {
      float value = NoiseUtil.valCoord2D(seed, cellX, cellY);
      return NoiseUtil.map(value, -1.0F, 1.0F, 2.0F);
   }

   private float edgeValue(float distance, float distance2) {
      EdgeFunction edge = EdgeFunction.DISTANCE_2_DIV;
      float value = edge.apply(distance, distance2);
      float edgeValue = 1.0F - NoiseUtil.map(value, edge.min(), edge.max(), edge.range());
      edgeValue = NoiseUtil.pow(edgeValue, 1.5F);
      if (edgeValue < this.edgeMin) {
         return 0.0F;
      } else {
         return edgeValue > this.edgeMax ? 1.0F : (edgeValue - this.edgeMin) / this.edgeRange;
      }
   }
}
