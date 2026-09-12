package com.onehumanawa.cfhcore.content.world.worldgen.cell.climate;

import net.minecraft.core.HolderGetter;
import com.onehumanawa.cfhcore.content.world.preset.PresetClimateNoise;
import com.onehumanawa.cfhcore.content.world.preset.PresetNoiseData;
import com.onehumanawa.cfhcore.content.world.preset.settings.ClimateSettings;
import com.onehumanawa.cfhcore.content.world.preset.settings.Preset;
import com.onehumanawa.cfhcore.content.world.preset.settings.WorldSettings;
import com.onehumanawa.cfhcore.content.world.worldgen.GeneratorContext;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.Cell;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.continent.Continent;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.heightmap.Levels;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.terrain.TerrainType;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.NoiseUtil;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noise;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noises;

public record Climate(int randomSeed, Noise offsetX, Noise offsetZ, int offsetDistance, Levels levels, ClimateModule biomeNoise) {
   public void apply(Cell cell, float x, float z, boolean applyClimate) {
      this.biomeNoise.apply(cell, x, z, x, z, true);
      float edgeBlend = 0.4F;
      if (cell.height <= this.levels.water) {
         if (cell.terrain == TerrainType.COAST) {
            cell.terrain = TerrainType.SHALLOW_OCEAN;
         }
      } else if (cell.biomeRegionEdge < edgeBlend || cell.terrain == TerrainType.MOUNTAIN_CHAIN) {
         float modifier = 1.0F - NoiseUtil.map(cell.biomeRegionEdge, 0.0F, edgeBlend, edgeBlend);
         float distance = (float)this.offsetDistance * modifier;
         float dx = this.offsetX.compute(x, z, 0) * distance;
         float dz = this.offsetZ.compute(x, z, 0) * distance;
         float var12 = x + dx;
         float var13 = z + dz;
         this.biomeNoise.apply(cell, var12, var13, x, z, false);
      }
   }

   public static Climate make(Continent continent, GeneratorContext context) {
      HolderGetter<Noise> noiseLookup = context.noiseLookup;
      Preset preset = context.preset;
      WorldSettings worldSettings = preset.world();
      ClimateSettings climateSettings = preset.climate();
      ClimateModule biomeNoise = new ClimateModule(context.seed, continent, worldSettings.controlPoints, climateSettings, context.levels);
      Levels levels = context.levels;
      int randSeed = context.seed.next();
      Noise biomeEdgeShape = PresetNoiseData.getNoise(noiseLookup, PresetClimateNoise.BIOME_EDGE_SHAPE);
      Noise offsetX = Noises.shiftSeed(biomeEdgeShape, context.seed.next());
      Noise offsetZ = Noises.shiftSeed(biomeEdgeShape, context.seed.next());
      int offsetDistance = climateSettings.biomeEdgeShape.strength;
      return new Climate(randSeed, offsetX, offsetZ, offsetDistance, levels, biomeNoise);
   }
}
