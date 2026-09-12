package com.onehumanawa.cfhcore.content.world.worldgen.cell.rivermap.river;

import java.util.Random;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.Cell;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.heightmap.Levels;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.rivermap.ContinentalHydrology;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.rivermap.lake.LakeConfig;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.terrain.TerrainType;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.NoiseUtil;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.function.CurveFunction;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Line;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noise;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noises;

public class UpliftRiverCarver implements CFHWorldGenRiverCarver {
   public boolean main;
   private boolean connecting;
   private float fade;
   private float fadeInv;
   private Range bedWidth;
   private Range banksWidth;
   private Range valleyWidth;
   private Range bedDepth;
   private Range banksDepth;
   private float waterLine;
   public River river;
   public RiverWarp warp;
   public RiverConfig config;
   public CurveFunction valleyCurve;
   private Levels levels;
   private Noise widthNoise;
   private Noise depthNoise;
   private Noise terraceNoise;
   private Noise asymmetryNoise;
   private Noise gullyNoise;
   private Noise rivuletNoise;
   private Noise lakeWarpNoise;
   public LakeConfig lakeConfig;
   private final float riverValleyWidthModifier;
   private boolean isUpliftContinent;

   public UpliftRiverCarver(
      River river, RiverWarp warp, RiverConfig config, RiverCarverSettings settings, Levels levels, LakeConfig lakeConfig, boolean isUpliftContinent
   ) {
      this.fade = settings.fadeIn;
      this.fadeInv = 1.0F / settings.fadeIn;
      this.bedWidth = new Range(0.25F, (float)(config.bedWidth * config.bedWidth));
      float erosionScale = 3.5F;
      float sqErosionScale = erosionScale * erosionScale;
      float outerErosionScale = 1.15F;
      float sqOuterErosionScale = outerErosionScale * outerErosionScale;
      this.banksWidth = new Range(1.5625F * sqErosionScale, (float)(config.bankWidth * config.bankWidth) * sqOuterErosionScale);
      float expandedValley = settings.valleySize * erosionScale;
      this.valleyWidth = new Range(expandedValley * expandedValley, expandedValley * expandedValley);
      this.river = river;
      this.warp = warp;
      this.config = config;
      this.main = config.main;
      this.connecting = settings.connecting;
      this.waterLine = levels.water;
      this.bedDepth = new Range(levels.water, config.bedHeight);
      this.banksDepth = new Range(config.minBankHeight, config.maxBankHeight);
      this.valleyCurve = settings.valleyCurve;
      this.levels = levels;
      this.widthNoise = Noises.simplex(8241, 150, 2);
      this.depthNoise = Noises.simplex(3912, 100, 2);
      this.terraceNoise = Noises.simplex(5510, 200, 1);
      this.asymmetryNoise = Noises.simplex(1193, 250, 1);
      this.gullyNoise = Noises.simplex(9876, 65, 2);
      this.rivuletNoise = Noises.simplex(5432, 20, 2);
      this.lakeWarpNoise = Noises.simplex(7439, 55, 3);
      this.lakeConfig = lakeConfig;
      this.isUpliftContinent = isUpliftContinent;
      int rh1 = Float.floatToIntBits(river.x1);
      int rh2 = Float.floatToIntBits(river.z1);
      long uniqueRiverSeed = (long)rh1 << 32 | (long)rh2 & 4294967295L;
      uniqueRiverSeed ^= 20195750309L;
      Random riverVarRand = new Random(uniqueRiverSeed);
      this.riverValleyWidthModifier = 0.7F + riverVarRand.nextFloat() * 0.6F;
   }

   @Override
   public void carve(Cell cell, float prevX, float prevZ, float prevT, float currX, float currZ, float currT) {
      float distSqToCurr = this.getDistance2(currX, currZ, currT);
      float currentLinearDist = (float)Math.sqrt((double)distSqToCurr);
      float flatnessInput = this.isUpliftContinent ? cell.waterTable : currT;
      float flatnessFactor = NoiseUtil.clamp(ContinentalHydrology.getFlatnessFactor((double)flatnessInput), 0.0F, 1.0F);
      float scaleFactor = 1.0F + 0.75F * flatnessFactor;
      float sqScaleFactor = scaleFactor * scaleFactor;
      float widthVar = this.widthNoise.compute(currX, currZ, 8241);
      float depthVar = this.depthNoise.compute(currX, currZ, 3912);
      float terraceMask = this.terraceNoise.compute(currX, currZ, 5510);
      float asymmetry = this.asymmetryNoise.compute(currX, currZ, 1193);
      float gullyRaw = this.gullyNoise.compute(currX, currZ, 9876);
      float gullyShape = 1.0F - Math.abs(gullyRaw);
      gullyShape *= gullyShape;
      float rivuletRaw = this.rivuletNoise.compute(currX, currZ, 5432);
      float rivuletShape = 1.0F - Math.abs(rivuletRaw);
      rivuletShape = rivuletShape * rivuletShape * rivuletShape;
      float drainageMask = gullyShape * 0.7F + rivuletShape * 0.3F;
      float dynamicWidthMult = 1.0F + widthVar * 0.35F;
      float dynamicDepthMult = 1.0F + depthVar * 0.25F;
      float sideBias = 1.0F + asymmetry * 0.4F;
      float oceanHeightOffset = this.levels.water;
      float targetWaterLevel = ContinentalHydrology.getWeightedWaterHeight(cell.waterTable) + oceanHeightOffset;
      float baseBedDepthOffset = oceanHeightOffset - this.config.bedHeight;
      float bedDepthOffset = baseBedDepthOffset * dynamicDepthMult;
      float targetBedFloor = targetWaterLevel - bedDepthOffset;
      float bankHeightOffset = this.config.maxBankHeight - this.config.minBankHeight;
      float targetValleyFloor = targetWaterLevel + bankHeightOffset;
      float discrepancyScale = 1.0F + (float)this.levels.scale(cell.height - targetWaterLevel) / 100.0F;
      float biasedScale = sqScaleFactor * dynamicWidthMult * sideBias;
      float zone1Radius = (float)Math.sqrt((double)(this.getScaledSize(currT, this.bedWidth) * biasedScale));
      float plateauInput = this.isUpliftContinent ? cell.waterTable : currT;
      int plateauIndex = ContinentalHydrology.getStepId((double)plateauInput);
      float widenMultiplier = 1.0F;
      if (this.shouldWidenOnPlateau(plateauIndex, this.lakeConfig, currT)) {
         float lakeScaleMin = this.lakeConfig.sizeMin / 100.0F;
         float lakeScaleMax = this.lakeConfig.sizeMax / 100.0F;
         float baseStepScale = this.getLakeScaleForPlateau(plateauIndex, lakeScaleMin, lakeScaleMax);
         float shorelineWarp = this.lakeWarpNoise.compute(currX, currZ, 7439);
         float organicWarpFactor = baseStepScale * (1.0F + shorelineWarp * 0.45F);
         float distanceMask = 1.0F;
         float fadeWindow = 0.04F;
         if (currT < this.lakeConfig.distanceMin) {
            distanceMask = NoiseUtil.clamp((currT - (this.lakeConfig.distanceMin - fadeWindow)) / fadeWindow, 0.0F, 1.0F);
         } else if (currT > this.lakeConfig.distanceMax) {
            distanceMask = NoiseUtil.clamp((this.lakeConfig.distanceMax + fadeWindow - currT) / fadeWindow, 0.0F, 1.0F);
         }

         distanceMask = distanceMask * distanceMask * (3.0F - 2.0F * distanceMask);
         widenMultiplier = 1.0F + flatnessFactor * organicWarpFactor * distanceMask;
         zone1Radius *= widenMultiplier;
      }

      float zone2Width = (this.config.maxBankHeight - this.config.minBankHeight) / this.levels.unit * biasedScale;
      float zone2Radius = zone1Radius + zone2Width;
      float zone3Width = (float)this.config.bankWidth * dynamicWidthMult * this.riverValleyWidthModifier;
      float zone3Radius = zone2Radius + zone3Width;
      float zone4Radius = zone3Radius + zone3Width * (4.0F + discrepancyScale);
      if (!(currentLinearDist >= zone4Radius)) {
         float finalHeight = cell.height;
         if (currentLinearDist < zone1Radius) {
            finalHeight = this.carveZone1Riverbed(
               cell, currT, distSqToCurr, bedDepthOffset, oceanHeightOffset, sqScaleFactor, targetWaterLevel, widenMultiplier
            );
            cell.riverZone = RiverCarverSettings.RiverZone.Riverbed;
         } else if (currentLinearDist < zone2Radius) {
            finalHeight = this.carveZone2BankStep(currentLinearDist, zone1Radius, zone2Radius, targetWaterLevel, targetValleyFloor, terraceMask, drainageMask);
            if (cell.riverZone != RiverCarverSettings.RiverZone.Riverbed) {
               cell.riverZone = RiverCarverSettings.RiverZone.Banks;
            }
         } else if (currentLinearDist < zone3Radius) {
            finalHeight = this.carveZone3ValleyFloor(targetValleyFloor, terraceMask, drainageMask);
            if (cell.riverZone != RiverCarverSettings.RiverZone.Riverbed && cell.riverZone != RiverCarverSettings.RiverZone.Banks) {
               cell.riverZone = RiverCarverSettings.RiverZone.ValleyFloor;
            }
         } else {
            finalHeight = this.carveZone4Fadeout(cell.height, currentLinearDist, zone3Radius, zone4Radius, targetValleyFloor, terraceMask, drainageMask);
            if (cell.riverZone != RiverCarverSettings.RiverZone.Riverbed
               && cell.riverZone != RiverCarverSettings.RiverZone.Banks
               && cell.riverZone != RiverCarverSettings.RiverZone.ValleyFloor) {
               cell.riverZone = RiverCarverSettings.RiverZone.ValleyFadeout;
            }
         }

         if (finalHeight < cell.height) {
            cell.height = finalHeight;
         }

         this.updateValleyMask(prevX, prevZ, prevT, currX, currZ, currT, distSqToCurr, sqScaleFactor, targetBedFloor, cell);
      }
   }

   private float carveZone1Riverbed(
      Cell cell,
      float currT,
      float distSqToCurr,
      float bedDepthOffset,
      float oceanHeightOffset,
      float sqScaleFactor,
      float targetWaterLevel,
      float widenMultiplier
   ) {
      float effectiveScaleFactor = sqScaleFactor * widenMultiplier * widenMultiplier;
      float bedInfluence = this.getDistanceAlpha(currT, distSqToCurr, this.bedWidth, effectiveScaleFactor);
      bedInfluence = bedInfluence * bedInfluence * (3.0F - 2.0F * bedInfluence);
      float lakeDepthMulti = 0.35F + this.lakeConfig.depth / 50.0F;
      float dynamicDepthOffset = bedDepthOffset * (1.0F + (widenMultiplier - 1.0F) * lakeDepthMulti);
      float bedHeight = ContinentalHydrology.getWeightedWaterHeight(cell.waterTable) - dynamicDepthOffset * bedInfluence + oceanHeightOffset;
      cell.moisture = 1.0F;
      this.tag(cell, targetWaterLevel);
      return bedHeight;
   }

   private float carveZone2BankStep(
      float distance, float zone1Radius, float zone2Radius, float targetWaterLevel, float targetValleyFloor, float terraceMask, float drainageMask
   ) {
      float progress = (distance - zone1Radius) / (zone2Radius - zone1Radius);
      progress = NoiseUtil.clamp(progress, 0.0F, 1.0F);
      progress = this.applyTerracing(progress, terraceMask, drainageMask, 3.0F);
      float arc = progress * (1.0F - progress) * 4.0F;
      progress = Math.max(0.0F, progress - drainageMask * 0.3F * arc);
      float smoothProgress = progress * progress * (3.0F - 2.0F * progress);
      return NoiseUtil.lerp(targetWaterLevel, targetValleyFloor, smoothProgress);
   }

   private float carveZone3ValleyFloor(float targetValleyFloor, float terraceMask, float drainageMask) {
      float bumpiness = terraceMask * 0.4F - drainageMask * 0.6F;
      return targetValleyFloor + bumpiness * this.levels.unit;
   }

   private float carveZone4Fadeout(
      float originalTerrainHeight, float distance, float zone3Radius, float zone4Radius, float targetValleyFloor, float terraceMask, float drainageMask
   ) {
      float progress = (distance - zone3Radius) / (zone4Radius - zone3Radius);
      progress = NoiseUtil.clamp(progress, 0.0F, 1.0F);
      float modifiedProgress = this.applyTerracing(progress, terraceMask, drainageMask, 5.0F);
      float slopeMask = progress * (1.0F - progress) * 4.0F;
      modifiedProgress = Math.max(0.0F, modifiedProgress - drainageMask * 0.25F * slopeMask);
      float smoothProgress = modifiedProgress * modifiedProgress * (3.0F - 2.0F * modifiedProgress);
      return NoiseUtil.lerp(targetValleyFloor, originalTerrainHeight, smoothProgress);
   }

   private float applyTerracing(float progress, float terraceMask, float drainageMask, float steps) {
      float intactTerrace = Math.max(0.0F, terraceMask - drainageMask * 1.5F);
      float terraceStrength = NoiseUtil.clamp(intactTerrace * 1.5F, 0.0F, 1.0F);
      if (terraceStrength > 0.0F) {
         float steppedProgress = (float)Math.round(progress * steps) / steps;
         return NoiseUtil.lerp(progress, steppedProgress, terraceStrength * 0.65F);
      } else {
         return progress;
      }
   }

   private void updateValleyMask(
      float prevX, float prevZ, float prevT, float currX, float currZ, float currT, float distSqToCurr, float sqScaleFactor, float targetBedFloor, Cell cell
   ) {
      float distSqToPrev = this.getDistance2(prevX, prevZ, prevT);
      float valleyInfluence = this.getDistanceAlpha(currT, Math.min(distSqToCurr, distSqToPrev), this.valleyWidth, sqScaleFactor);
      if (valleyInfluence > 0.0F) {
         valleyInfluence = this.valleyCurve.apply(valleyInfluence);
         cell.riverMask = Math.min(cell.riverMask, 1.0F - valleyInfluence);
      }
   }

   private boolean shouldWidenOnPlateau(int plateauIndex, LakeConfig config, float currT) {
      if (plateauIndex < -1) {
         return false;
      } else {
         float fadeWindow = 0.04F;
         if (currT < config.distanceMin - fadeWindow) {
            return false;
         } else if (currT > config.distanceMax + fadeWindow) {
            return false;
         } else {
            int h1 = Float.floatToIntBits(this.river.x1);
            int h2 = Float.floatToIntBits(this.river.z1);
            long riverSeed = (long)h1 << 32 | (long)h2 & 4294967295L;
            riverSeed ^= (long)plateauIndex * 25214903917L;
            Random selectionRand = new Random(riverSeed);
            return selectionRand.nextFloat() < config.chance;
         }
      }
   }

   private float getLakeScaleForPlateau(int plateauIndex, float minScale, float maxScale) {
      int h1 = Float.floatToIntBits(this.river.x1);
      int h2 = Float.floatToIntBits(this.river.z1);
      long riverSeed = (long)h1 << 32 | (long)h2 & 4294967295L;
      riverSeed ^= (long)plateauIndex * 10005465364L;
      Random scaleRand = new Random(riverSeed);
      return minScale + scaleRand.nextFloat() * (maxScale - minScale);
   }

   @Override
   public RiverConfig createForkConfig(float t, Levels levels) {
      int bedHeight = levels.scale(this.getScaledSize(t, this.bedDepth));
      int bedWidth = (int)Math.round(Math.sqrt((double)this.getScaledSize(t, this.bedWidth)) * 0.75);
      int bankWidth = (int)Math.round(Math.sqrt((double)this.getScaledSize(t, this.banksWidth)) * 0.75);
      bedWidth = Math.max(1, bedWidth);
      bankWidth = Math.max(bedWidth + 1, bankWidth);
      return this.config.createFork(bedHeight, bedWidth, bankWidth, levels);
   }

   private float getDistance2(float x, float y, float t) {
      if (t <= 0.0F) {
         return Line.distSq(x, y, this.river.x1, this.river.z1);
      } else if (t >= 1.0F) {
         return Line.distSq(x, y, this.river.x2, this.river.z2);
      } else {
         float px = this.river.x1 + t * this.river.dx;
         float py = this.river.z1 + t * this.river.dz;
         return Line.distSq(x, y, px, py);
      }
   }

   private float getDistanceAlpha(float t, float dist2, Range range, float sqScaleFactor) {
      float size2 = this.getScaledSize(t, range) * sqScaleFactor;
      return dist2 >= size2 ? 0.0F : 1.0F - dist2 / size2;
   }

   private float getScaledSize(float t, Range range) {
      if (t < 0.0F) {
         return range.min();
      } else if (t > 1.0F) {
         return range.max();
      } else if (range.min() == range.max()) {
         return range.min();
      } else {
         return t >= this.fade ? range.max() : NoiseUtil.lerp(range.min(), range.max(), t * this.fadeInv);
      }
   }

   private void tag(Cell cell, float bedHeight) {
      if (!cell.terrain.isLake()) {
         cell.erosionMask = true;
         cell.terrain = TerrainType.RIVER;
         float newMax = Math.max(this.waterLine, bedHeight);
         if (newMax > cell.riverWaterLevel) {
            cell.riverWaterLevel = Math.max(this.waterLine, bedHeight);
         }
      }
   }

   @Override
   public boolean isMain() {
      return this.main;
   }

   @Override
   public River getRiver() {
      return this.river;
   }

   @Override
   public RiverWarp getWarp() {
      return this.warp;
   }

   @Override
   public RiverConfig getConfig() {
      return this.config;
   }
}
