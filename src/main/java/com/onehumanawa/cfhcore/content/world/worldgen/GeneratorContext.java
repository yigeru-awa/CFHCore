package com.onehumanawa.cfhcore.content.world.worldgen;

import net.minecraft.core.HolderGetter;
import org.jetbrains.annotations.Nullable;
import com.onehumanawa.cfhcore.content.world.preset.settings.Preset;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.heightmap.Heightmap;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.heightmap.Levels;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.heightmap.WorldLookup;
import com.onehumanawa.cfhcore.content.world.worldgen.densityfunction.tile.TileCache;
import com.onehumanawa.cfhcore.content.world.worldgen.densityfunction.tile.generation.TileGenerator;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noise;
import com.onehumanawa.cfhcore.content.world.worldgen.util.Seed;

public class GeneratorContext {
   public Seed seed;
   public Levels levels;
   public Preset preset;
   public HolderGetter<Noise> noiseLookup;
   public TileGenerator generator;
   @Nullable
   public TileCache cache;
   public WorldLookup lookup;

   public GeneratorContext(Preset preset, HolderGetter<Noise> noiseLookup, int seed, int tileSize, int tileBorder, int batchCount, @Nullable TileCache cache) {
      this.preset = preset;
      this.noiseLookup = noiseLookup;
      this.seed = new Seed(seed);
      this.levels = new Levels(preset.world().properties.terrainScaler(), preset.world().properties.seaLevel);
      this.generator = new TileGenerator(Heightmap.make(this), new WorldFilters(this), tileSize, tileBorder, batchCount);
      this.cache = cache;
      this.lookup = new WorldLookup(this);
   }

   public static GeneratorContext makeCached(Preset preset, HolderGetter<Noise> noiseLookup, int seed, int tileSize, int batchCount, boolean queue) {
      GeneratorContext ctx = makeUncached(
         preset, noiseLookup, seed, tileSize, Math.min(2, Math.max(1, preset.filters().erosion.dropletLifetime / 16)), batchCount
      );
      ctx.cache = new TileCache(tileSize, queue, ctx.generator);
      ctx.lookup = new WorldLookup(ctx);
      return ctx;
   }

   public static GeneratorContext makeUncached(Preset preset, HolderGetter<Noise> noiseLookup, int seed, int tileSize, int tileBorder, int batchCount) {
      return new GeneratorContext(preset, noiseLookup, seed, tileSize, tileBorder, batchCount, null);
   }
}
