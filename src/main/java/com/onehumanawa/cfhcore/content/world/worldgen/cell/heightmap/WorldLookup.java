package com.onehumanawa.cfhcore.content.world.worldgen.cell.heightmap;

import com.onehumanawa.cfhcore.content.world.worldgen.GeneratorContext;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.Cell;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.terrain.TerrainType;
import com.onehumanawa.cfhcore.content.world.worldgen.densityfunction.tile.Tile;
import com.onehumanawa.cfhcore.content.world.worldgen.densityfunction.tile.TileCache;

public class WorldLookup {
   private float waterLevel;
   private float beachLevel;
   private TileCache cache;
   private Heightmap heightmap;
   private Levels levels;

   public WorldLookup(GeneratorContext context) {
      this.cache = context.cache;
      this.heightmap = context.generator.getHeightmap();
      this.waterLevel = context.levels.water;
      this.beachLevel = context.levels.water(5);
      this.levels = context.levels;
   }

   public Heightmap getHeightmap() {
      return this.heightmap;
   }

   public boolean applyCell(Cell cell, int x, int z, boolean applyClimate) {
      return this.applyCell(cell, x, z, false, applyClimate);
   }

   public boolean applyCell(Cell cell, int x, int z, boolean load, boolean applyClimate) {
      if (load && this.computeAccurate(cell, x, z)) {
         return true;
      } else {
         return this.computeCached(cell, x, z) ? true : this.compute(cell, x, z, applyClimate);
      }
   }

   private boolean computeAccurate(Cell cell, int x, int z) {
      int rx = this.cache.chunkToTile(x >> 4);
      int rz = this.cache.chunkToTile(z >> 4);
      Tile tile = this.cache.provide(rx, rz);
      Cell c = tile.lookup(x, z);
      if (c != null) {
         cell.copyFrom(c);
      }

      return cell.terrain != null;
   }

   private boolean computeCached(Cell cell, int x, int z) {
      int rx = this.cache.chunkToTile(x >> 4);
      int rz = this.cache.chunkToTile(z >> 4);
      Tile tile = this.cache.provideIfPresent(rx, rz);
      if (tile != null) {
         Cell c = tile.lookup(x, z);
         if (c != null) {
            cell.copyFrom(c);
         }

         return cell.terrain != null;
      } else {
         return false;
      }
   }

   private boolean compute(Cell cell, int x, int z, boolean applyClimate) {
      this.heightmap.apply(cell, (float)x, (float)z, applyClimate);
      if (cell.terrain == TerrainType.COAST && cell.height > this.waterLevel && cell.height <= this.beachLevel) {
         cell.terrain = TerrainType.BEACH;
      }

      return false;
   }
}
