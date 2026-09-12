package com.onehumanawa.cfhcore.content.world.worldgen.cell.terrain.populator;

import com.onehumanawa.cfhcore.content.world.preset.settings.IslandSettings;
import com.onehumanawa.cfhcore.content.world.preset.settings.WorldSettings;
import com.onehumanawa.cfhcore.content.world.worldgen.biome.Erosion;
import com.onehumanawa.cfhcore.content.world.worldgen.biome.Weirdness;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.Cell;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.CellPopulator;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.heightmap.Levels;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.terrain.TerrainType;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.NoiseUtil;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noise;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noises;
import com.onehumanawa.cfhcore.content.world.worldgen.util.Seed;

public class ArchipelagoPopulator implements CellPopulator {
   private IslandSettings settings;
   private Levels levels;
   private WorldSettings.ControlPoints controlPoints;
   private Noise sizeNoise;
   private Noise densityNoise;
   private Noise ridgeHeight;
   private Noise hillHeight;
   private Noise volcanoHeight;
   private Noise mountainSelector;
   private Noise volcanoSelector;
   private Noise beachVariance;
   private Noise baseTerrainDetail;
   private Noise mountainGullies;
   private Noise islandErosion;
   private Noise islandWeirdness;
   private Noise beachErosion;
   private Noise beachWeirdness;

   public ArchipelagoPopulator(IslandSettings settings, Levels levels, WorldSettings.ControlPoints controlPoints, Seed seed) {
      this.settings = settings;
      this.levels = levels;
      this.controlPoints = controlPoints;
      int salt = seed.get();
      int size = Math.round(settings.islandSize);
      float hScale = Math.max(0.1F, settings.islandHorizontalScale);
      Noise sizeN = Noises.simplex(1273 + salt, Math.max(1, Math.round((float)size * 3.5F / hScale)), 3);
      sizeN = Noises.warpPerlin(sizeN, 1273 + salt, Math.max(1, Math.round((float)size * 2.0F / hScale)), 2, (float)size * 0.5F / hScale);
      sizeN = Noises.warpPerlin(sizeN, 4830 + salt, Math.max(1, Math.round((float)size * 0.5F / hScale)), 1, (float)size * 0.3F / hScale);
      sizeN = Noises.warpPerlin(sizeN, 8932 + salt, Math.max(1, Math.round((float)size * 0.08F / hScale)), 2, (float)size * 0.15F / hScale);
      sizeN = Noises.clamp(sizeN, 0.0F, 1.0F);
      this.sizeNoise = sizeN;
      Noise densityN = Noises.simplex(9735 + salt, 4000, 3);
      densityN = Noises.warpPerlin(densityN, 9735 + salt, 2000, 2, 1000.0F);
      densityN = Noises.clamp(densityN, 0.0F, 1.0F);
      this.densityNoise = densityN;
      Noise ridge = Noises.perlinRidge(4829 + salt, Math.max(1, Math.round((float)size * 1.6F / hScale)), 4, 2.1F, 0.82F);
      ridge = Noises.warpPerlin(ridge, 4830 + salt, Math.max(1, Math.round((float)size * 0.9F / hScale)), 2, (float)size * 0.35F / hScale);
      ridge = Noises.clamp(ridge, 0.0F, 1.0F);
      this.ridgeHeight = ridge;
      Noise hills = Noises.billow(3811 + salt, Math.max(1, Math.round((float)size * 0.35F / hScale)), 3, 2.25F, 0.55F);
      hills = Noises.warpPerlin(hills, 3812 + salt, Math.max(1, Math.round((float)size * 0.7F / hScale)), 1, (float)size * 0.2F / hScale);
      hills = Noises.clamp(hills, 0.0F, 1.0F);
      this.hillHeight = hills;
      Noise volcano = Noises.perlinRidge(6721 + salt, Math.max(1, Math.round((float)size * 0.45F / hScale)), 3, 2.4F, 0.9F);
      volcano = Noises.powCurve(volcano, 1.8F);
      volcano = Noises.clamp(volcano, 0.0F, 1.0F);
      this.volcanoHeight = volcano;
      this.beachVariance = Noises.simplex(5541 + salt, Math.max(1, Math.round((float)size * 0.15F / hScale)), 2);
      this.baseTerrainDetail = Noises.simplex(7712 + salt, Math.max(1, Math.round((float)size * 0.1F / hScale)), 3);
      this.mountainGullies = Noises.perlinRidge(9912 + salt, Math.max(1, Math.round((float)size * 0.06F / hScale)), 3, 2.2F, 0.8F);
      this.mountainSelector = Noises.clamp(Noises.simplex(11867 + salt, Math.max(1, Math.round((float)size * 1.25F / hScale)), 2), 0.0F, 1.0F);
      this.volcanoSelector = Noises.clamp(Noises.simplex(22193 + salt, Math.max(1, Math.round((float)size * 1.75F / hScale)), 2), 0.0F, 1.0F);
      this.islandErosion = Erosion.LEVEL_4.source();
      this.islandWeirdness = Weirdness.MID_SLICE_NORMAL_DESCENDING.source();
      this.beachErosion = Erosion.LEVEL_4.source();
      this.beachWeirdness = Weirdness.MID_SLICE_NORMAL_DESCENDING.source();
   }

   @Override
   public void apply(Cell cell, float x, float z) {
      float originalContinentEdge = cell.continentEdge;
      float sizeValue = this.sizeNoise.compute(x, z, 0);
      float densityValue = this.densityNoise.compute(x, z, 0);
      float densityThreshold = NoiseUtil.clamp(1.0F - this.settings.islandDensity * 0.8F, 0.05F, 0.98F);
      float shapeAlpha = smoothStep(0.5F, 1.0F, sizeValue);
      float densityFade = NoiseUtil.clamp((1.0F - densityThreshold) * 0.5F, 0.04F, 0.12F);
      float densityAlpha = smoothStep(densityThreshold, densityThreshold + densityFade, densityValue);
      float fadeStart = this.controlPoints.islandCoast;
      float fadeEnd = this.controlPoints.deepOcean;
      float continentFade = 1.0F - smoothStep(fadeStart, fadeEnd, originalContinentEdge);
      float islandAlpha = shapeAlpha * densityAlpha * continentFade;
      if (!(islandAlpha <= 0.001F)) {
         float beachWidth = NoiseUtil.clamp(Math.max(0.05F, this.settings.beachWidth), 0.05F, 0.45F);
         float beachCoverage = NoiseUtil.clamp(this.settings.beachCoverage, 0.0F, 1.0F);
         float shelfEnd = NoiseUtil.clamp(beachWidth * 0.65F, 0.04F, 0.35F);
         float rawVariance = this.beachVariance.compute(x, z, 0);
         float cliffFactor = smoothStep(-0.2F, 0.6F, rawVariance);
         float activeBeachWidth = NoiseUtil.lerp(beachWidth, 0.01F, cliffFactor);
         float coastEnd = NoiseUtil.clamp(shelfEnd + activeBeachWidth * 0.5F, shelfEnd + 0.005F, 0.85F);
         float baseBeachEnd = coastEnd + activeBeachWidth * beachCoverage * 1.5F;
         float bVariance = rawVariance * 0.15F * (1.0F - cliffFactor) - 0.05F * (1.0F - cliffFactor);
         float dynamicBeachEnd = NoiseUtil.clamp(baseBeachEnd + bVariance, coastEnd + 0.005F, 0.85F);
         float oceanHeight = cell.height;
         int offshoreDepth = Math.max(2, Math.round(4.0F + this.settings.offshoreDepth * 10.0F));
         float shelfTarget = Math.max(oceanHeight, this.levels.water(-offshoreDepth));
         float shelfAlpha = smoothStep(0.0F, shelfEnd, islandAlpha);
         float shelfHeight = NoiseUtil.lerp(oceanHeight, shelfTarget, shelfAlpha);
         float coastAlpha = smoothStep(shelfEnd, coastEnd, islandAlpha);
         float beachHeight = NoiseUtil.lerp(shelfHeight, this.levels.ground, coastAlpha);
         float mountainGap = NoiseUtil.lerp(0.08F, 0.005F, cliffFactor);
         float mountainStart = NoiseUtil.clamp(dynamicBeachEnd + mountainGap, shelfEnd + 0.01F, 0.9F);
         float mountainTransition = NoiseUtil.lerp(0.08F, 0.04F, cliffFactor);
         float mountainEnd = Math.max(mountainStart + mountainTransition, 0.72F * (1.0F - cliffFactor * 0.25F));
         float mountainAlpha = smoothStep(mountainStart, mountainEnd, islandAlpha);
         float mountainFreqMod = NoiseUtil.lerp(2.0F, 0.3F, this.settings.mountainScale);
         float volcanoFreqMod = NoiseUtil.lerp(2.0F, 0.3F, this.settings.volcanismScale);
         float mountainGate = chanceMask(this.mountainSelector, this.settings.mountainChance, x * mountainFreqMod, z * mountainFreqMod);
         float volcanoGate = chanceMask(this.volcanoSelector, this.settings.volcanoChance, x * volcanoFreqMod, z * volcanoFreqMod);
         float hillValue = this.hillHeight.compute(x * mountainFreqMod, z * mountainFreqMod, 0) * (0.15F + mountainGate * 0.55F);
         float ridgeValue = this.ridgeHeight.compute(x * mountainFreqMod, z * mountainFreqMod, 0) * mountainGate;
         float volcanoValue = this.volcanoHeight.compute(x * volcanoFreqMod, z * volcanoFreqMod, 0) * volcanoGate;
         float mountainValue = NoiseUtil.clamp(hillValue * 0.35F + ridgeValue * 0.5F + volcanoValue * 0.75F, 0.0F, 1.0F);
         float gullyErosion = this.mountainGullies.compute(x, z, 0) * mountainAlpha * 0.45F;
         mountainValue = Math.max(0.0F, mountainValue - gullyErosion);
         float terraceSteps = 7.0F;
         float terracedMountain = (float)Math.floor((double)(mountainValue * terraceSteps)) / terraceSteps;
         mountainValue = NoiseUtil.lerp(mountainValue, terracedMountain, 0.55F);
         float baseHeight = this.settings.islandHeight * (0.015F + this.settings.islandBaseScale * 0.08F);
         float reliefHeight = mountainValue * mountainAlpha * this.settings.islandHeight * this.settings.islandVerticalScale * 0.3F;
         float landTransitionEnd = NoiseUtil.lerp(1.0F, dynamicBeachEnd + 0.12F, cliffFactor);
         float landAlpha = smoothStep(dynamicBeachEnd, landTransitionEnd, islandAlpha);
         float landDetail = this.baseTerrainDetail.compute(x, z, 0) * this.settings.islandHeight * 0.08F * landAlpha;
         float targetHeight = this.levels.ground + baseHeight + reliefHeight + landDetail;
         cell.height = NoiseUtil.lerp(beachHeight, targetHeight, landAlpha);
         cell.continentEdge = Math.max(originalContinentEdge, this.continentEdge(islandAlpha, shelfEnd, dynamicBeachEnd));
         if (islandAlpha < shelfEnd) {
            cell.terrain = TerrainType.SHALLOW_OCEAN;
         } else if (islandAlpha < dynamicBeachEnd) {
            cell.terrain = TerrainType.ISLAND_BEACH;
         } else if (!(mountainAlpha > 0.35F) || !(mountainValue > 0.35F) || !(mountainGate > 0.1F) && !(volcanoGate > 0.2F)) {
            cell.terrain = TerrainType.ISLAND;
         } else {
            cell.terrain = TerrainType.ISLAND_MOUNTAINS;
         }

         if (islandAlpha >= shelfEnd) {
            if (cell.terrain == TerrainType.ISLAND_BEACH) {
               cell.erosion = this.beachErosion.compute(x, z, 0);
               cell.weirdness = this.beachWeirdness.compute(x, z, 0);
            } else {
               cell.erosion = this.islandErosion.compute(x, z, 0);
               cell.weirdness = this.islandWeirdness.compute(x, z, 0);
            }
         }
      }
   }

   private float continentEdge(float islandAlpha, float shelfEnd, float beachEnd) {
      if (islandAlpha < shelfEnd) {
         float alpha = smoothStep(0.0F, shelfEnd, islandAlpha);
         return NoiseUtil.lerp(this.controlPoints.deepOcean, this.controlPoints.shallowOcean, alpha);
      } else if (islandAlpha < beachEnd) {
         float alpha = smoothStep(shelfEnd, beachEnd, islandAlpha);
         return NoiseUtil.lerp(this.controlPoints.shallowOcean, this.controlPoints.coast, alpha);
      } else {
         float alpha = smoothStep(beachEnd, 1.0F, islandAlpha);
         return NoiseUtil.lerp(this.controlPoints.coast, this.controlPoints.inland, alpha);
      }
   }

   private static float chanceMask(Noise selector, float chance, float x, float z) {
      chance = NoiseUtil.clamp(chance, 0.0F, 1.0F);
      if (chance <= 0.0F) {
         return 0.0F;
      } else if (chance >= 1.0F) {
         return 1.0F;
      } else {
         float threshold = 1.0F - chance;
         return smoothStep(threshold, Math.min(1.0F, threshold + 0.2F), selector.compute(x, z, 0));
      }
   }

   private static float smoothStep(float min, float max, float value) {
      if (max <= min) {
         return value >= max ? 1.0F : 0.0F;
      } else {
         float alpha = NoiseUtil.clamp((value - min) / (max - min), 0.0F, 1.0F);
         return alpha * alpha * (3.0F - 2.0F * alpha);
      }
   }
}
