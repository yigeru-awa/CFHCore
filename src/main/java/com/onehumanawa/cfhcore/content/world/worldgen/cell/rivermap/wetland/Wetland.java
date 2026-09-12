package com.onehumanawa.cfhcore.content.world.worldgen.cell.rivermap.wetland;

import com.onehumanawa.cfhcore.content.world.worldgen.cell.Cell;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.heightmap.Levels;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.rivermap.ContinentalHydrology;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.terrain.TerrainType;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.NoiseUtil;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Line;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noise;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noises;
import com.onehumanawa.cfhcore.content.world.worldgen.util.Boundsf;

public class Wetland {
   private final NoiseUtil.Vec2f a;
   private final NoiseUtil.Vec2f b;
   private final float radius;
   private final float radius2;
   private final Noise moundShape;
   private final Noise moundHeight;
   private final Noise warpNoise;
   private final Levels levels;

   public Wetland(int seed, NoiseUtil.Vec2f a, NoiseUtil.Vec2f b, float radius, Levels levels) {
      this.a = a;
      this.b = b;
      this.radius = radius;
      this.radius2 = radius * radius;
      this.levels = levels;
      this.moundShape = Noises.map(Noises.clamp(Noises.perlin(++seed, 10, 1), 0.3F, 0.6F), 0.0F, 1.0F);
      this.moundHeight = Noises.map(Noises.clamp(Noises.simplex(++seed, 20, 1), 0.0F, 0.3F), 0.0F, 1.0F);
      this.warpNoise = Noises.perlin(++seed, 25, 2);
   }

   public void apply(Cell cell, float rx, float rz, float x, float z) {
      float upliftOffset = ContinentalHydrology.getWeightedWaterHeight(cell.waterTable);
      float oceanHeightOffset = this.levels.scale(this.levels.waterLevel);
      float localWaterSurface = oceanHeightOffset + upliftOffset;
      float singleBlock = this.levels.ground(1) - this.levels.ground(0);
      float bed = localWaterSurface - 3.5F * singleBlock;
      if (!(cell.height < bed)) {
         float warpStrength = 8.0F;
         float wx = rx + this.warpNoise.compute(x, z, 0) * warpStrength;
         float wz = rz + this.warpNoise.compute(x, z, 1) * warpStrength;
         float t = Line.distanceOnLine(wx, wz, this.a.x(), this.a.y(), this.b.x(), this.b.y());
         float d2 = getDistance2(wx, wz, this.a.x(), this.a.y(), this.b.x(), this.b.y(), t);
         if (!(d2 > this.radius2)) {
            float dist = 1.0F - d2 / this.radius2;
            float banks = cell.height;
            float tStart = 0.4F;
            float tEnd = 0.7F;
            float totalAlpha = NoiseUtil.map(dist, 0.0F, tEnd, tEnd);
            float rivuletNoise = Math.abs(this.warpNoise.compute(x * 0.4F, z * 0.4F, 2));
            float slopeMask = (float)Math.sin((double)totalAlpha * Math.PI);
            if (slopeMask > 0.0F) {
               totalAlpha += rivuletNoise * 0.25F * slopeMask;
            }

            totalAlpha = Math.max(0.0F, Math.min(1.0F, totalAlpha));
            totalAlpha = NoiseUtil.interpQuintic(totalAlpha);
            float internalAlpha = NoiseUtil.map(dist, tStart, tEnd, tEnd - tStart);
            internalAlpha = Math.max(0.0F, Math.min(1.0F, internalAlpha));
            internalAlpha = NoiseUtil.interpQuintic(internalAlpha);
            float targetHeight = NoiseUtil.lerp(banks, bed, internalAlpha);
            if (cell.height > targetHeight) {
               cell.height = NoiseUtil.lerp(banks, targetHeight, internalAlpha);
            }

            if (internalAlpha > 0.1F) {
               cell.riverWaterLevel = upliftOffset;
            }

            if (dist >= tEnd) {
               cell.terrain = TerrainType.WETLAND;
               cell.erosionMask = true;
            }

            if (internalAlpha > 0.0F) {
               cell.riverWaterLevel = upliftOffset;
            }

            if (dist >= tEnd) {
               cell.terrain = TerrainType.WETLAND;
               cell.erosionMask = true;
            }

            float localMoundMin = localWaterSurface + 1.0F * singleBlock;
            float localMoundMax = localWaterSurface + 2.0F * singleBlock;
            float localMoundVariance = localMoundMax - localMoundMin;
            if (cell.height >= bed && cell.height < localMoundMax) {
               float shapeAlpha = this.moundShape.compute(x, z, 0) * totalAlpha;
               float moundHeightNoise = this.moundHeight.compute(x, z, 0);
               float mounds = localMoundMin + moundHeightNoise * localMoundVariance;
               cell.height = NoiseUtil.lerp(cell.height, mounds, shapeAlpha * 0.8F);
            }

            cell.riverMask = Math.min(cell.riverMask, 1.0F - totalAlpha);
         }
      }
   }

   public void recordBounds(Boundsf.Builder builder) {
      builder.record(Math.min(this.a.x(), this.b.x()) - this.radius, Math.min(this.a.y(), this.b.y()) - this.radius);
      builder.record(Math.max(this.a.x(), this.b.x()) + this.radius, Math.max(this.a.y(), this.b.y()) + this.radius);
   }

   private static float getDistance2(float x, float y, float ax, float ay, float bx, float by, float t) {
      if (t <= 0.0F) {
         return Line.distSq(x, y, ax, ay);
      } else if (t >= 1.0F) {
         return Line.distSq(x, y, bx, by);
      } else {
         float px = ax + t * (bx - ax);
         float py = ay + t * (by - ay);
         return Line.distSq(x, y, px, py);
      }
   }
}
