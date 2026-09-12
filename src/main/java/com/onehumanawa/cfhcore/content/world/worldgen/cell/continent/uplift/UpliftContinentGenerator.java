package com.onehumanawa.cfhcore.content.world.worldgen.cell.continent.uplift;

import com.onehumanawa.cfhcore.content.world.concurrent.Resource;
import com.onehumanawa.cfhcore.content.world.preset.settings.WorldSettings;
import com.onehumanawa.cfhcore.content.world.worldgen.GeneratorContext;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.Cell;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.continent.SimpleContinent;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.continent.advanced.AbstractContinent;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.heightmap.Levels;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.rivermap.Rivermap;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.NoiseUtil;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.domain.Domain;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.domain.Domains;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Line;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noise;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noises;
import com.onehumanawa.cfhcore.content.world.worldgen.util.PosUtil;
import com.onehumanawa.cfhcore.content.world.worldgen.util.Seed;

public class UpliftContinentGenerator extends AbstractContinent implements SimpleContinent {
   protected static float CENTER_CORRECTION = 0.35F;
   protected float frequency;
   protected float variance;
   protected int varianceSeed;
   protected Domain warp;
   protected Domain cleanWarp;
   protected Noise cliffNoise;
   protected Noise bayNoise;
   protected Levels levels;

   public UpliftContinentGenerator(Seed seed, GeneratorContext context) {
      super(seed, context);
      WorldSettings settings = context.preset.world();
      int tectonicScale = settings.continent.continentScale * 4;
      this.frequency = 1.0F / (float)tectonicScale;
      this.varianceSeed = seed.next();
      this.variance = settings.continent.continentSizeVariance;
      int warpSeedX = seed.next();
      int warpSeedZ = seed.next();
      this.warp = this.createWarp(warpSeedX, warpSeedZ, tectonicScale, settings.continent, true);
      this.cleanWarp = this.createWarp(warpSeedX, warpSeedZ, tectonicScale, settings.continent, false);
      float frequency = 1.0F / this.frequency;
      Noise cliffNoise = Noises.simplex2(seed.next(), this.continentScale / 2, 2);
      cliffNoise = Noises.clamp(cliffNoise, 0.1F, 0.25F);
      cliffNoise = Noises.map(cliffNoise, 0.0F, 1.0F);
      cliffNoise = Noises.frequency(cliffNoise, frequency);
      this.cliffNoise = cliffNoise;
      Noise bayNoise = Noises.simplex(seed.next(), 100, 1);
      bayNoise = Noises.mul(bayNoise, 0.1F);
      bayNoise = Noises.add(bayNoise, 0.9F);
      bayNoise = Noises.frequency(bayNoise, frequency);
      this.bayNoise = bayNoise;
      this.levels = context.levels;
   }

   @Override
   public void apply(Cell cell, float rawX, float rawY) {
      long warped = this.warp.getPos(rawX, rawY, 0);
      float wx = PosUtil.unpackLeftf(warped);
      float wy = PosUtil.unpackRightf(warped);
      float x = wx * this.frequency;
      float y = wy * this.frequency;
      int xi = NoiseUtil.floor(x);
      int yi = NoiseUtil.floor(y);
      int cellX = xi;
      int cellY = yi;
      float cellPointX = x;
      float cellPointY = y;
      float nearest = Float.MAX_VALUE;

      for (int cy = yi - 1; cy <= yi + 1; cy++) {
         for (int cx = xi - 1; cx <= xi + 1; cx++) {
            NoiseUtil.Vec2f vec = NoiseUtil.cell(this.seed, cx, cy);
            float px = (float)cx + vec.x() * this.jitter;
            float py = (float)cy + vec.y() * this.jitter;
            float dist2 = Line.distSq(x, y, px, py);
            if (dist2 < nearest) {
               cellPointX = px;
               cellPointY = py;
               cellX = cx;
               cellY = cy;
               nearest = dist2;
            }
         }
      }

      nearest = Float.MAX_VALUE;
      float sumX = 0.0F;
      float sumY = 0.0F;

      for (int cy2 = cellY - 1; cy2 <= cellY + 1; cy2++) {
         for (int cx2 = cellX - 1; cx2 <= cellX + 1; cx2++) {
            if (cx2 != cellX || cy2 != cellY) {
               NoiseUtil.Vec2f vec2 = NoiseUtil.cell(this.seed, cx2, cy2);
               float px2 = (float)cx2 + vec2.x() * this.jitter;
               float py2 = (float)cy2 + vec2.y() * this.jitter;
               float dist3 = getDistance(x, y, cellPointX, cellPointY, px2, py2);
               sumX += px2;
               sumY += py2;
               if (dist3 < nearest) {
                  nearest = dist3;
               }
            }
         }
      }

      if (!this.shouldSkip(cellX, cellY)) {
         cell.continentDistance = NoiseUtil.sqrt(nearest);
         cell.continentEdge = this.getDistanceValue(x, y, cellX, cellY, nearest);
         cell.continentId = AbstractContinent.getCellValue(this.seed, cellX, cellY);
         float upliftGradient = this.getSmoothVoronoiGradient(cell, rawX, rawY);
         upliftGradient = this.shiftAndRemap(upliftGradient, this.levels.water);
         upliftGradient = this.shiftAndRemap(upliftGradient, 0.15F);
         cell.continentSizeModifier = this.getContinentSizeModifier(cellX, cellY);
         if (cell.continentSizeModifier != 1.0F) {
            upliftGradient = this.shiftAndRemap(upliftGradient, 1.0F - cell.continentSizeModifier);
         }

         float customPeak = this.shiftAndRemap(cell.continentEdge, this.levels.water);
         if (customPeak < 0.05F && customPeak < upliftGradient) {
            upliftGradient = customPeak;
         }

         cell.waterTable = upliftGradient;
      }
   }

   public float shiftAndRemap(float value, float threshold) {
      if (value <= threshold) {
         return 0.0F;
      } else {
         float remapped = (value - threshold) / (1.0F - threshold);
         return NoiseUtil.clamp(remapped, 0.0F, 1.0F);
      }
   }

   public float getSmoothVoronoiGradient(Cell cell, float rawX, float rawZ) {
      if (!cell.terrain.isShallowOcean() && !cell.terrain.isDeepOcean()) {
         float x = rawX * this.frequency;
         float y = rawZ * this.frequency;
         int xi = NoiseUtil.floor(x);
         int yi = NoiseUtil.floor(y);
         int cellX = xi;
         int cellY = yi;
         float cellPointX = x;
         float cellPointY = y;
         float nearestSq = Float.MAX_VALUE;

         for (int cy = yi - 1; cy <= yi + 1; cy++) {
            for (int cx = xi - 1; cx <= xi + 1; cx++) {
               NoiseUtil.Vec2f vec = NoiseUtil.cell(this.seed, cx, cy);
               float px = (float)cx + vec.x() * this.jitter;
               float py = (float)cy + vec.y() * this.jitter;
               float dist2 = Line.distSq(x, y, px, py);
               if (dist2 < nearestSq) {
                  nearestSq = dist2;
                  cellPointX = px;
                  cellPointY = py;
                  cellX = cx;
                  cellY = cy;
               }
            }
         }

         float[] neighborX = new float[8];
         float[] neighborY = new float[8];
         int nIndex = 0;

         for (int cy2 = cellY - 1; cy2 <= cellY + 1; cy2++) {
            for (int cx2 = cellX - 1; cx2 <= cellX + 1; cx2++) {
               if (cx2 != cellX || cy2 != cellY) {
                  NoiseUtil.Vec2f vec2 = NoiseUtil.cell(this.seed, cx2, cy2);
                  neighborX[nIndex] = (float)cx2 + vec2.x() * this.jitter;
                  neighborY[nIndex] = (float)cy2 + vec2.y() * this.jitter;
                  nIndex++;
               }
            }
         }

         float vertexSumX = 0.0F;
         float vertexSumY = 0.0F;
         int vertexCount = 0;
         float s0Sq = cellPointX * cellPointX + cellPointY * cellPointY;

         for (int i = 0; i < 8; i++) {
            float x1 = neighborX[i];
            float y1 = neighborY[i];
            float dx1 = x1 - cellPointX;
            float dy1 = y1 - cellPointY;
            float b1 = 0.5F * (x1 * x1 + y1 * y1 - s0Sq);

            for (int j = i + 1; j < 8; j++) {
               float x2 = neighborX[j];
               float y2 = neighborY[j];
               float dx2 = x2 - cellPointX;
               float dy2 = y2 - cellPointY;
               float b2 = 0.5F * (x2 * x2 + y2 * y2 - s0Sq);
               float det = dx1 * dy2 - dy1 * dx2;
               if (!(Math.abs(det) < 1.0E-5F)) {
                  float vx = (b1 * dy2 - b2 * dy1) / det;
                  float vy = (dx1 * b2 - dx2 * b1) / det;
                  float d0Sq = (vx - cellPointX) * (vx - cellPointX) + (vy - cellPointY) * (vy - cellPointY);
                  boolean isValidVertex = true;

                  for (int k = 0; k < 8; k++) {
                     if (k != i && k != j) {
                        float xk = neighborX[k];
                        float yk = neighborY[k];
                        float dkSq = (vx - xk) * (vx - xk) + (vy - yk) * (vy - yk);
                        if (dkSq < d0Sq - 1.0E-4F) {
                           isValidVertex = false;
                           break;
                        }
                     }
                  }

                  if (isValidVertex) {
                     vertexSumX += vx;
                     vertexSumY += vy;
                     vertexCount++;
                  }
               }
            }
         }

         float centerX = vertexCount > 0 ? vertexSumX / (float)vertexCount : cellPointX;
         float centerY = vertexCount > 0 ? vertexSumY / (float)vertexCount : cellPointY;
         cell.continentX = Math.round(centerX / this.frequency);
         cell.continentZ = Math.round(centerY / this.frequency);
         float minGradient = 1.0F;

         for (int i = 0; i < 8; i++) {
            float px2 = neighborX[i];
            float py2 = neighborY[i];
            float dx = px2 - cellPointX;
            float dy = py2 - cellPointY;
            float lenSq = dx * dx + dy * dy;
            if (lenSq > 1.0E-5F) {
               float siSq = px2 * px2 + py2 * py2;
               float baseHalfDiff = 0.5F * (siSq - s0Sq);
               float h_x = baseHalfDiff - (x * dx + y * dy);
               float h_c = baseHalfDiff - (centerX * dx + centerY * dy);
               if (h_c > 1.0E-5F) {
                  float planeValue = h_x / h_c;
                  if (planeValue < minGradient) {
                     minGradient = planeValue;
                  }
               }
            }
         }

         return NoiseUtil.clamp(minGradient, 0.0F, 1.0F);
      } else {
         return 0.0F;
      }
   }

   @Override
   public float getEdgeValue(float x, float z) {
      float var5;
      try (Resource<Cell> resource = Cell.getResource()) {
         Cell cell = resource.get();
         this.apply(cell, x, z);
         var5 = cell.continentEdge;
      }

      return var5;
   }

   @Override
   public long getNearestCenter(float x, float z) {
      long var5;
      try (Resource<Cell> resource = Cell.getResource()) {
         Cell cell = resource.get();
         this.apply(cell, x, z);
         var5 = PosUtil.pack(cell.continentX, cell.continentZ);
      }

      return var5;
   }

   @Override
   public Rivermap getRivermap(int x, int z) {
      return this.riverCache.getRivers(x, z);
   }

   protected Domain createWarp(int seedX, int seedZ, int tectonicScale, WorldSettings.Continent continent, boolean applyLacunarity) {
      int warpScale = NoiseUtil.round((float)tectonicScale * 0.225F);
      float strength = (float)NoiseUtil.round((float)tectonicScale * 0.33F);
      float lacunarity = applyLacunarity ? continent.continentNoiseLacunarity : 0.0F;
      return Domains.domain(
         Noises.perlin2(seedX, warpScale, continent.continentNoiseOctaves, lacunarity, continent.continentNoiseGain),
         Noises.perlin2(seedZ, warpScale, continent.continentNoiseOctaves, lacunarity, continent.continentNoiseGain),
         Noises.constant(strength)
      );
   }

   protected float getContinentSizeModifier(int cellX, int cellY) {
      if (this.variance > 0.0F && !this.isDefaultContinent(cellX, cellY)) {
         float sizeValue = AbstractContinent.getCellValue(this.varianceSeed, cellX, cellY);
         return NoiseUtil.map(sizeValue, 0.0F, this.variance, this.variance);
      } else {
         return 1.0F;
      }
   }

   protected float getDistanceValue(float x, float y, int cellX, int cellY, float distance) {
      distance = this.getVariedDistanceValue(cellX, cellY, distance);
      distance = NoiseUtil.sqrt(distance);
      distance = NoiseUtil.map(distance, 0.05F, 0.25F, 0.2F);
      distance = this.getCoastalDistanceValue(x, y, distance);
      if (distance < this.controlPoints.inland && distance >= this.controlPoints.shallowOcean) {
         distance = this.getCoastalDistanceValue(x, y, distance);
      }

      return distance;
   }

   protected float getVariedDistanceValue(int cellX, int cellY, float distance) {
      if (this.variance > 0.0F && !this.isDefaultContinent(cellX, cellY)) {
         float sizeValue = AbstractContinent.getCellValue(this.varianceSeed, cellX, cellY);
         float sizeModifier = NoiseUtil.map(sizeValue, 0.0F, this.variance, this.variance);
         distance *= sizeModifier;
      }

      return distance;
   }

   protected float getCoastalDistanceValue(float x, float y, float distance) {
      if (distance > this.controlPoints.shallowOcean && distance < this.controlPoints.inland) {
         float alpha = distance / this.controlPoints.inland;
         float cliff = this.cliffNoise.compute(x, y, 0);
         distance = NoiseUtil.lerp(distance * cliff, distance, alpha);
         if (distance < this.controlPoints.shallowOcean) {
            distance = this.controlPoints.shallowOcean * this.bayNoise.compute(x, y, 0);
         }
      }

      return distance;
   }

   protected static float midPoint(float a, float b) {
      return (a + b) * 0.5F;
   }

   protected static float getDistance(float x, float y, float ax, float ay, float bx, float by) {
      float mx = midPoint(ax, bx);
      float my = midPoint(ay, by);
      float dx = bx - ax;
      float dy = by - ay;
      float nx = -dy;
      return getDistance2Line(x, y, mx, my, mx + nx, my + dx);
   }

   protected static float getDistance2Line(float x, float y, float ax, float ay, float bx, float by) {
      float dx = bx - ax;
      float dy = by - ay;
      float v = (x - ax) * dx + (y - ay) * dy;
      v /= dx * dx + dy * dy;
      float ox = ax + dx * v;
      float oy = ay + dy * v;
      return Line.distSq(x, y, ox, oy);
   }
}
