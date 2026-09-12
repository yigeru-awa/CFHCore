package com.onehumanawa.cfhcore.content.world.preset.settings;

import com.onehumanawa.cfhcore.content.world.worldgen.noise.function.DistanceFunction;

/**
 * CFHCore ships exactly one hardcoded terrain preset.
 *
 * <p>CFHWorldGen originally exposed a data-driven preset registry plus an in-game preset
 * editor. Both have been removed; the values below are the only terrain configuration and
 * cannot be changed by the player.
 */
public class Presets {
   public static final Preset DEFAULT = makeDefault();

   private Presets() {
   }

   private static Preset makeDefault() {
      return new Preset(
         new WorldSettings(
            new WorldSettings.Continent(ContinentType.UPLIFT, DistanceFunction.EUCLIDEAN, 3000, 0.7F, 0.25F, 0.25F, 5, 0.26F, 4.33F),
            new WorldSettings.ControlPoints(0.0F, 0.0F, 0.1F, 0.25F, 0.327F, 0.448F, 0.502F),
            new WorldSettings.Properties(SpawnType.CONTINENT_CENTER, 384, 64, 63, -54, 0, 0)
         ),
         new SurfaceSettings(new SurfaceSettings.Erosion(30, 256, 40, 95, 0.65F, 0.475F, 0.4F)),
         new CaveSettings(0.0F, 1.5625F, 1.0F, 1.0F, 1.0F, 0.05F, 0.07F, 0.0075F, true, false),
         new ClimateSettings(
            new ClimateSettings.RangeValue(0, 6, 2, 0.0F, 0.98F, 0.05F),
            new ClimateSettings.RangeValue(0, 6, 1, 0.0F, 1.0F, 0.0F),
            new ClimateSettings.BiomeShape(225, 8, 150, 80),
            new ClimateSettings.BiomeNoise(ClimateSettings.BiomeNoise.EdgeType.SIMPLEX, 24, 2, 0.5F, 2.65F, 14)
         ),
         new TerrainSettings(
            new TerrainSettings.General(0, 1200, 1.0F, 1.0F, true, false),
            new TerrainSettings.Terrain(1.0F, 2.0F, 1.0F, 1.0F),
            new TerrainSettings.Terrain(2.0F, 2.0F, 1.0F, 1.0F),
            new TerrainSettings.Terrain(2.0F, 1.0F, 1.0F, 1.0F),
            new TerrainSettings.Terrain(1.5F, 1.0F, 1.0F, 1.0F),
            new TerrainSettings.Terrain(1.5F, 1.0F, 1.0F, 1.0F),
            new TerrainSettings.Terrain(1.0F, 1.0F, 1.0F, 1.0F),
            new TerrainSettings.Terrain(2.0F, 1.0F, 1.0F, 1.0F),
            new TerrainSettings.Terrain(2.5F, 1.0F, 1.0F, 1.0F),
            new TerrainSettings.Terrain(5.0F, 1.0F, 1.0F, 1.0F)
         ),
         new RiverSettings(
            0,
            8,
            new RiverSettings.River(5, 2, 6, 20, 8, 0.75F),
            new RiverSettings.River(4, 1, 4, 14, 5, 0.975F),
            new RiverSettings.Lake(0.3F, 0.0F, 0.03F, 10, 75, 150, 2, 10),
            new RiverSettings.Wetland(0.6F, 175, 225)
         ),
         IslandSettings.makeDefault(),
         new FilterSettings(new FilterSettings.Erosion(135, 12, 0.7F, 0.7F, 0.5F, 0.5F), new FilterSettings.Smoothing(1, 1.8F, 0.9F)),
         new StructureSettings(),
         new MiscellaneousSettings(true, 600, true, true, true, false, true, true, false, true, false, 0.4F, 0.4F)
      );
   }
}
