package com.onehumanawa.cfhcore.content.worldgen.cell.heightmap;

import net.minecraft.core.HolderGetter;
import com.onehumanawa.cfhcore.content.worldgen.data.preset.PresetNoiseData;
import com.onehumanawa.cfhcore.content.worldgen.data.preset.PresetTerrainTypeNoise;
import com.onehumanawa.cfhcore.content.worldgen.data.preset.settings.Preset;
import com.onehumanawa.cfhcore.content.worldgen.data.preset.settings.TerrainSettings;
import com.onehumanawa.cfhcore.content.worldgen.data.preset.settings.WorldSettings;
import com.onehumanawa.cfhcore.content.worldgen.data.preset.settings.WorldSettings.ControlPoints;
import com.onehumanawa.cfhcore.content.worldgen.GeneratorContext;
import com.onehumanawa.cfhcore.content.worldgen.biome.Erosion;
import com.onehumanawa.cfhcore.content.worldgen.biome.Weirdness;
import com.onehumanawa.cfhcore.content.worldgen.cell.Cell;
import com.onehumanawa.cfhcore.content.worldgen.cell.CellPopulator;
import com.onehumanawa.cfhcore.content.worldgen.cell.climate.Climate;
import com.onehumanawa.cfhcore.content.worldgen.cell.continent.Continent;
import com.onehumanawa.cfhcore.content.worldgen.cell.continent.ContinentLerper2;
import com.onehumanawa.cfhcore.content.worldgen.cell.continent.ContinentLerper3;
import com.onehumanawa.cfhcore.content.worldgen.cell.rivermap.ContinentalHydrology;
import com.onehumanawa.cfhcore.content.worldgen.cell.rivermap.Rivermap;
import com.onehumanawa.cfhcore.content.worldgen.cell.terrain.Blender;
import com.onehumanawa.cfhcore.content.worldgen.cell.terrain.IslandBlender;
import com.onehumanawa.cfhcore.content.worldgen.cell.terrain.Populators;
import com.onehumanawa.cfhcore.content.worldgen.cell.terrain.TerrainType;
import com.onehumanawa.cfhcore.content.worldgen.cell.terrain.populator.ArchipelagoPopulator;
import com.onehumanawa.cfhcore.content.worldgen.cell.terrain.populator.VolcanoPopulator;
import com.onehumanawa.cfhcore.content.worldgen.cell.terrain.provider.TerrainProvider;
import com.onehumanawa.cfhcore.content.worldgen.cell.terrain.region.RegionLerper;
import com.onehumanawa.cfhcore.content.worldgen.cell.terrain.region.RegionModule;
import com.onehumanawa.cfhcore.content.worldgen.cell.terrain.region.RegionSelector;
import com.onehumanawa.cfhcore.content.worldgen.noise.function.DistanceFunction;
import com.onehumanawa.cfhcore.content.worldgen.noise.function.EdgeFunction;
import com.onehumanawa.cfhcore.content.worldgen.noise.function.Interpolation;
import com.onehumanawa.cfhcore.content.worldgen.noise.module.Noise;
import com.onehumanawa.cfhcore.content.worldgen.noise.module.Noises;
import com.onehumanawa.cfhcore.content.worldgen.util.Seed;

public record Heightmap(CellPopulator terrain, CellPopulator region, Continent continent, Climate climate, Levels levels, ControlPoints controlPoints, float terrainFrequency, Noise beachNoise) {
	
	public void apply(Cell cell, float x, float z, boolean applyClimate) {
		this.applyTerrain(cell, x, z);
		if (cell.terrain == TerrainType.ISLAND_BEACH) {
			cell.terrain = TerrainType.BEACH;
		}
		this.applyRivers(cell, x, z, this.continent.getRivermap(cell));
		this.applyClimate(cell, x, z, applyClimate);
	}
	
	public void applyTerrain(Cell cell, float x, float z) {
        cell.terrain = TerrainType.FLATS;
        cell.beachNoise = this.beachNoise.compute(x, z, 0);
        this.continent.apply(cell, x, z);
        this.region.apply(cell, x, z);
        this.terrain.apply(cell, x * this.terrainFrequency, z * this.terrainFrequency);
	}
	
	public void applyRivers(Cell cell, float x, float z, Rivermap rivermap) {
        rivermap.apply(cell, x, z);
        VolcanoPopulator.modifyVolcanoType(cell, this.levels);
	}
	
	public void applyClimate(Cell cell, float x, float z, boolean applyClimate) {
		float riverValleyThreshold = 0.675F;
        if(cell.riverMask < riverValleyThreshold && !isIslandTerrain(cell)) {
        	cell.erosion = 0.445F;
        	cell.weirdness = 0.34F;
        }
        
        if(cell.terrain.isRiver()) {
            cell.erosion = -0.05F;
            cell.weirdness = -0.03F;
        }
        
        if(cell.terrain.isLake() && cell.height < this.levels.water) {
            cell.erosion = Erosion.LEVEL_4.mid();
            cell.weirdness = -0.03F;
        }
        if(cell.terrain.isWetland()) {
        	cell.erosion = Erosion.LEVEL_6.mid();
        	cell.weirdness = Weirdness.VALLEY.mid();
        }
        
        this.climate.apply(cell, x, z, applyClimate);

        if(cell.riverMask >= riverValleyThreshold && cell.macroBiomeId > 0.5F) { 
        	cell.weirdness = -cell.weirdness;
        }
	}
	
	public static Heightmap make(GeneratorContext ctx) {
    	HolderGetter<Noise> noiseLookup = ctx.noiseLookup;
    	
        Preset preset = ctx.preset;
        WorldSettings world = ctx.preset.world();
        ControlPoints controlPoints = world.controlPoints;

        TerrainSettings terrainSettings = preset.terrain();
        TerrainSettings.General general = terrainSettings.general;
        float globalVerticalScale = general.globalVerticalScale;
        
        Seed regionWarp = ctx.seed.offset(8934);
        int regionWarpScale = 400;
        int regionWarpStrength = 200;
        
        RegionConfig regionConfig = new RegionConfig(
        	ctx.seed.root() + 789124, 
        	general.terrainRegionSize, 
        	Noises.simplex(regionWarp.next(), regionWarpScale, 1),
        	Noises.simplex(regionWarp.next(), regionWarpScale, 1), 
        	regionWarpStrength
        );
        Levels levels = ctx.levels;
        float terrainFrequency = 1.0F / terrainSettings.general.globalHorizontalScale;
        CellPopulator region = new RegionModule(regionConfig);

        Seed mountainSeed = ctx.seed.offset(general.terrainSeedOffset);
        Noise mountainShape = Noises.worleyEdge(mountainSeed.next(), general.legacyMountainScaling ? 1000 : Math.round(1000 * terrainSettings.mountains.horizontalScale * 2.25F), EdgeFunction.DISTANCE_2_ADD, DistanceFunction.EUCLIDEAN);
        mountainShape = Noises.warpPerlin(mountainShape, mountainSeed.next(), 333, 2, 250.0F);
        mountainShape = Noises.curve(mountainShape, Interpolation.CURVE3);
        mountainShape = Noises.clamp(mountainShape, 0.0F, 0.9F);
        mountainShape = Noises.map(mountainShape, 0.0F, 1.0F);

        Noise ground = PresetNoiseData.getNoise(noiseLookup, PresetTerrainTypeNoise.GROUND);
        
        CellPopulator terrainRegions = new RegionSelector(TerrainProvider.generateTerrain(ctx.seed, terrainSettings, regionConfig, levels, noiseLookup));
        CellPopulator terrainRegionBorders = Populators.makeBorder(ctx.seed, ground, terrainSettings.plains, terrainSettings.steppe, globalVerticalScale);
        CellPopulator terrainBlend = new RegionLerper(terrainRegionBorders, terrainRegions);
        CellPopulator mountains = Populators.makeMountainChain(mountainSeed, ground, terrainSettings.mountains, terrainSettings.general.legacyMountainScaling ? 1.0F : terrainSettings.mountains.horizontalScale * 2.25F, terrainSettings.general.legacyMountainScaling ? globalVerticalScale : globalVerticalScale * terrainSettings.mountains.verticalScale, general.fancyMountains, general.legacyMountainScaling);
        Continent continent = world.continent.continentType.create(ctx.seed, ctx);
        Climate climate = Climate.make(continent, ctx);
        CellPopulator land = new Blender(mountainShape, terrainBlend, mountains, 0.3F, 0.8F, 0.575F);
        
        CellPopulator deepOcean = Populators.makeDeepOcean(ctx.seed.next(), levels.water);
        CellPopulator shallowOcean = Populators.makeShallowOcean(ctx.levels);
        CellPopulator coast = Populators.makeCoast(ctx.levels);

        CellPopulator oceans = new ContinentLerper3(deepOcean, shallowOcean, coast, controlPoints.deepOcean, controlPoints.shallowOcean, controlPoints.coast);

        CellPopulator terrain = new ContinentLerper2(oceans, (cell, x, z) -> {
            land.apply(cell, x, z);
            cell.globalContinentScale = world.continent.continentScale;
            cell.height += (ContinentalHydrology.getComplexWaterHeight(
                    cell.waterTable,
                    cell.globalContinentScale,
                    cell.continentSizeModifier)
            );
        }, controlPoints.shallowOcean, controlPoints.inland);
        
        // Wrap with archipelago layer if enabled
        if (ctx.preset.island().enableArchipelago) {
            terrain = new IslandBlender(terrain, new ArchipelagoPopulator(ctx.preset.island(), ctx.levels, controlPoints, ctx.seed), ctx.levels);
        }

        Noise beachNoise = Noises.perlin2(ctx.seed.next(), 20, 1);
        beachNoise = Noises.mul(beachNoise, ctx.levels.scale(5));
        return new Heightmap(terrain, region, continent, climate, levels, controlPoints, terrainFrequency, beachNoise);
	}
	
	private static boolean isIslandTerrain(Cell cell) {
	    return cell.terrain == TerrainType.ISLAND || cell.terrain == TerrainType.ISLAND_BEACH || cell.terrain == TerrainType.ISLAND_MOUNTAINS;
	}
}
