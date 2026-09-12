package com.onehumanawa.cfhcore.content.world.worldgen.cell;

import com.onehumanawa.cfhcore.content.world.concurrent.Resource;
import com.onehumanawa.cfhcore.content.world.concurrent.SimpleResource;
import com.onehumanawa.cfhcore.content.world.concurrent.pool.ThreadLocalPool;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.biome.type.BiomeType;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.rivermap.river.RiverCarverSettings;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.terrain.Terrain;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.terrain.TerrainType;

public class Cell {
   private static final Cell DEFAULTS = new Cell();
   private static final Cell EMPTY = new Cell() {
      @Override
      public boolean isAbsent() {
         return true;
      }
   };
   private static final ThreadLocalPool<Cell> POOL = new ThreadLocalPool<>(32, Cell::new, Cell::reset);
   public static final ThreadLocal<Resource<Cell>> LOCAL = ThreadLocal.withInitial(() -> new SimpleResource<>(new Cell(), Cell::reset));
   public float height;
   public float heightErosion;
   public float sediment;
   public float gradient;
   public float regionMoisture;
   public float regionTemperature;
   public float continentId;
   public float continentSizeModifier;
   public float continentEdge;
   public float waterTable;
   public float continentDistance;
   public float terrainRegionId;
   public float terrainRegionEdge;
   public float terrainRegionCenterX;
   public float terrainRegionCenterZ;
   public float biomeRegionId;
   public float biomeRegionEdge;
   public float macroBiomeId;
   public float riverMask;
   public float riverWaterLevel = 0.0F;
   public int continentX;
   public int continentZ;
   public boolean erosionMask;
   public Terrain terrain;
   public BiomeType biome;
   public float erosion;
   public float weirdness;
   public float temperature;
   public float moisture;
   public float beachNoise;
   public RiverCarverSettings.RiverZone riverZone = RiverCarverSettings.RiverZone.None;

   public Cell() {
      this.regionMoisture = 0.5F;
      this.regionTemperature = 0.5F;
      this.biomeRegionEdge = 1.0F;
      this.riverMask = 1.0F;
      this.erosionMask = false;
      this.terrain = TerrainType.NONE;
      this.biome = BiomeType.GRASSLAND;
      this.waterTable = 0.0F;
      this.continentSizeModifier = 1.0F;
   }

   public void copyFrom(Cell other) {
      this.height = other.height;
      this.heightErosion = other.heightErosion;
      this.sediment = other.sediment;
      this.gradient = other.gradient;
      this.regionMoisture = other.regionMoisture;
      this.regionTemperature = other.regionTemperature;
      this.continentId = other.continentId;
      this.continentEdge = other.continentEdge;
      this.continentDistance = other.continentDistance;
      this.terrainRegionId = other.terrainRegionId;
      this.terrainRegionEdge = other.terrainRegionEdge;
      this.terrainRegionCenterX = other.terrainRegionCenterX;
      this.terrainRegionCenterZ = other.terrainRegionCenterZ;
      this.biomeRegionId = other.biomeRegionId;
      this.biomeRegionEdge = other.biomeRegionEdge;
      this.macroBiomeId = other.macroBiomeId;
      this.riverMask = other.riverMask;
      this.continentX = other.continentX;
      this.continentZ = other.continentZ;
      this.erosionMask = other.erosionMask;
      this.terrain = other.terrain;
      this.biome = other.biome;
      this.erosion = other.erosion;
      this.weirdness = other.weirdness;
      this.temperature = other.temperature;
      this.moisture = other.moisture;
      this.beachNoise = other.beachNoise;
      this.continentSizeModifier = other.continentSizeModifier;
   }

   public Cell reset() {
      this.copyFrom(DEFAULTS);
      return this;
   }

   public boolean isAbsent() {
      return false;
   }

   public static Cell empty() {
      return EMPTY;
   }

   public static Resource<Cell> getResource() {
      Resource<Cell> resource = LOCAL.get();
      return resource.isOpen() ? POOL.get() : resource;
   }

   public interface Visitor {
      void visit(Cell var1, int var2, int var3);
   }
}
