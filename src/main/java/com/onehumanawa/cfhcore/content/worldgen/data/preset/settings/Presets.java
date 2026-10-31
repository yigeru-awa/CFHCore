package com.onehumanawa.cfhcore.content.worldgen.data.preset.settings;

import com.onehumanawa.cfhcore.content.worldgen.data.preset.settings.ClimateSettings.BiomeNoise;
import com.onehumanawa.cfhcore.content.worldgen.data.preset.settings.ClimateSettings.BiomeShape;
import com.onehumanawa.cfhcore.content.worldgen.data.preset.settings.ClimateSettings.RangeValue;
import com.onehumanawa.cfhcore.content.worldgen.data.preset.settings.FilterSettings.Erosion;
import com.onehumanawa.cfhcore.content.worldgen.data.preset.settings.FilterSettings.Smoothing;
import com.onehumanawa.cfhcore.content.worldgen.data.preset.settings.RiverSettings.Lake;
import com.onehumanawa.cfhcore.content.worldgen.data.preset.settings.RiverSettings.River;
import com.onehumanawa.cfhcore.content.worldgen.data.preset.settings.RiverSettings.Wetland;
import com.onehumanawa.cfhcore.content.worldgen.data.preset.settings.TerrainSettings.General;
import com.onehumanawa.cfhcore.content.worldgen.data.preset.settings.TerrainSettings.Terrain;
import com.onehumanawa.cfhcore.content.worldgen.data.preset.settings.WorldSettings.Continent;
import com.onehumanawa.cfhcore.content.worldgen.data.preset.settings.WorldSettings.ControlPoints;
import com.onehumanawa.cfhcore.content.worldgen.data.preset.settings.WorldSettings.Properties;
import com.onehumanawa.cfhcore.content.worldgen.noise.function.DistanceFunction;

public class Presets {

	/**
	 * The single, hardcoded worldgen preset. Formerly user-selectable via the
	 * preset config screen; removed in favour of a fixed, non-adjustable default.
	 */
	public static Preset makeDefault() {
		return new Preset(
			new WorldSettings(
				new Continent(ContinentType.UPLIFT, DistanceFunction.EUCLIDEAN, 3000, 0.7F, 0.25F, 0.25F, 5, 0.26F, 4.33F),
				new ControlPoints(0.0F, 0.074F, 0.1F, 0.25F, 0.327F, 0.448F, 0.502F),
				new Properties(SpawnType.CONTINENT_CENTER, 384, 64, 63, -54, 0, 0)
			),
			new SurfaceSettings(new SurfaceSettings.Erosion(30, 256, 40, 95, 0.65F, 0.475F, 0.4F)),
			new CaveSettings(0.0F, 1.5625F, 1.0F, 1.0F, 1.0F, 0.05F, 0.07F, 0.0075F, true, false),
			new ClimateSettings(
				new RangeValue(0, 6, 2, 0.0F, 0.98F, 0.05F),
				new RangeValue(0, 6, 1, 0.0F, 1.0F, 0.0F),
				new BiomeShape(225, 8, 150, 80),
				new BiomeNoise(ClimateSettings.BiomeNoise.EdgeType.SIMPLEX, 24, 2, 0.5F, 2.65F, 14)
			),
			new TerrainSettings(
				new General(0, 1200, 1.0F, 1.0F, true, false),
				new Terrain(1.0F, 2.0F, 1.0F, 1.0F),
				new Terrain(2.0F, 2.0F, 1.0F, 1.0F),
				new Terrain(2.0F, 1.0F, 1.0F, 1.0F),
				new Terrain(1.5F, 1.0F, 1.0F, 1.0F),
				new Terrain(1.5F, 1.0F, 1.0F, 1.0F),
				new Terrain(1.0F, 1.0F, 1.0F, 1.0F),
				new Terrain(2.0F, 1.0F, 1.0F, 1.0F),
				new Terrain(2.5F, 1.0F, 1.0F, 1.0F),
				new Terrain(5.0F, 1.0F, 1.0F, 1.0F)
			),
			new RiverSettings(
				0, 8,
				new River(5, 2, 6, 20, 8, 0.75F),
				new River(4, 1, 4, 14, 5, 0.975F),
				new Lake(0.3F, 0.0F, 0.03F, 10, 75, 150, 2, 10),
				new Wetland(0.6F, 175, 225)
			),
			IslandSettings.makeDefault(),
			new FilterSettings(
				new Erosion(135, 12, 0.7F, 0.7F, 0.5F, 0.5F),
				new Smoothing(1, 1.8F, 0.9F)
			),
			new StructureSettings(),
			new MiscellaneousSettings(true, 600, true, true, true, false, true, true, false, true, false, 0.4F, 0.4F)
		);
	}
}
