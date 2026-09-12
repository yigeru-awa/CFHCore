package com.onehumanawa.cfhcore.content.world.worldgen.densityfunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Supplier;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;
import com.onehumanawa.cfhcore.content.world.preset.settings.WorldSettings;
import com.onehumanawa.cfhcore.content.world.worldgen.biome.Continentalness;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.Cell;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.heightmap.Heightmap;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.heightmap.Levels;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.heightmap.WorldLookup;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.terrain.TerrainCategory;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.terrain.TerrainType;
import com.onehumanawa.cfhcore.content.world.worldgen.densityfunction.tile.Tile;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.NoiseUtil;
import com.onehumanawa.cfhcore.content.world.worldgen.util.PosUtil;

public record CellSampler(Supplier<WorldLookup> deferredLookup, CellSampler.Field field) implements MarkerFunction.Mapped {
   private static final ThreadLocal<CellSampler.Cache2d> CELL = ThreadLocal.withInitial(CellSampler.Cache2d::new);

   @Override
   public double compute(FunctionContext ctx) {
      WorldLookup worldLookup = this.deferredLookup.get();
      Cell cell = CELL.get().getAndUpdate(worldLookup, ctx.blockX(), ctx.blockZ(), true);
      return (double)this.field.read(cell, worldLookup.getHeightmap());
   }

   @Override
   public double minValue() {
      return 0.0;
   }

   @Override
   public double maxValue() {
      return 1.0;
   }

   public static class Cache2d {
      private long lastPos = Long.MAX_VALUE;
      private Cell cell = new Cell();

      public Cell getAndUpdate(WorldLookup lookup, int blockX, int blockZ, boolean sampleClimate) {
         blockX = QuartPos.toBlock(QuartPos.fromBlock(blockX));
         blockZ = QuartPos.toBlock(QuartPos.fromBlock(blockZ));
         long packedPos = PosUtil.pack(blockX, blockZ);
         if (this.lastPos != packedPos) {
            lookup.applyCell(this.cell.reset(), blockX, blockZ, false, sampleClimate);
            this.lastPos = packedPos;
         }

         return this.cell;
      }
   }

   public class CacheChunk implements MarkerFunction.Mapped {
      @Nullable
      private Tile.Chunk chunk;
      private CellSampler.Cache2d cache2d;
      private int chunkX;
      private int chunkZ;
      @Nullable
      private WorldLookup lookup;
      private Heightmap heightmap;

      public CacheChunk(@Nullable Tile.Chunk chunk, @Nullable CellSampler.Cache2d cache2d, int chunkX, int chunkZ) {
         this.chunk = chunk;
         this.cache2d = cache2d != null ? cache2d : new CellSampler.Cache2d();
         this.chunkX = chunkX;
         this.chunkZ = chunkZ;
      }

      @Override
      public double compute(FunctionContext ctx) {
         int blockX = ctx.blockX();
         int blockZ = ctx.blockZ();
         WorldLookup worldLookup = this.lookup;
         if (worldLookup == null) {
            this.lookup = worldLookup = CellSampler.this.deferredLookup.get();
            this.heightmap = worldLookup.getHeightmap();
         }

         Cell cell = this.chunk != null && this.chunkX == SectionPos.blockToSectionCoord(blockX) && this.chunkZ == SectionPos.blockToSectionCoord(blockZ)
            ? this.chunk.getCell(blockX, blockZ)
            : this.cache2d.getAndUpdate(worldLookup, blockX, blockZ, false);
         return (double)CellSampler.this.field.read(cell, this.heightmap);
      }

      @Override
      public double minValue() {
         return CellSampler.this.minValue();
      }

      @Override
      public double maxValue() {
         return CellSampler.this.maxValue();
      }
   }

   public static enum Field implements StringRepresentable {
      HEIGHT("height") {
         @Override
         public float read(Cell cell, Heightmap heightmap) {
            return cell.height;
         }
      },
      CONTINENT("continent") {
         @Override
         public float read(Cell cell, Heightmap heightmap) {
            Levels levels = heightmap.levels();
            WorldSettings.ControlPoints controlPoints = heightmap.controlPoints();
            float deepOcean = controlPoints.deepOcean;
            float shallowOcean = controlPoints.shallowOcean;
            float beach = controlPoints.beach;
            float inland = controlPoints.inland;
            if (cell.terrain == TerrainType.MUSHROOM_FIELDS) {
               return Continentalness.MUSHROOM_FIELDS.mid();
            } else if (cell.terrain.isDeepOcean()) {
               float alpha = NoiseUtil.clamp(cell.continentEdge, 0.0F, deepOcean);
               alpha = NoiseUtil.lerp(alpha, 0.0F, deepOcean, 0.0F, 1.0F);
               return NoiseUtil.lerp(Continentalness.DEEP_OCEAN.min() + 0.05F, Continentalness.DEEP_OCEAN.max(), alpha);
            } else if (cell.terrain.isShallowOcean()) {
               float alpha = NoiseUtil.clamp(cell.continentEdge, deepOcean, shallowOcean);
               alpha = NoiseUtil.lerp(alpha, deepOcean, shallowOcean, 0.0F, 0.98F);
               return NoiseUtil.lerp(Continentalness.OCEAN.min(), Continentalness.OCEAN.max(), alpha);
            } else if (cell.terrain.getDelegate() == TerrainCategory.BEACH && cell.height + cell.beachNoise < levels.water(5)) {
               float alpha = NoiseUtil.clamp(cell.continentEdge, shallowOcean, beach);
               alpha = NoiseUtil.lerp(alpha, shallowOcean, beach, 0.0F, 1.0F);
               return NoiseUtil.lerp(Continentalness.COAST.min(), Continentalness.COAST.max(), alpha);
            } else {
               float alpha = NoiseUtil.clamp(cell.continentEdge, beach, inland);
               alpha = NoiseUtil.lerp(alpha, beach, inland, 0.0F, 1.0F);
               return NoiseUtil.lerp(Continentalness.NEAR_INLAND.mid(), Continentalness.FAR_INLAND.max(), alpha);
            }
         }
      },
      EROSION("erosion") {
         @Override
         public float read(Cell cell, Heightmap heightmap) {
            return cell.erosion;
         }
      },
      WEIRDNESS("weirdness") {
         @Override
         public float read(Cell cell, Heightmap heightmap) {
            return cell.weirdness;
         }
      },
      BIOME_REGION("biome_region") {
         @Override
         public float read(Cell cell, Heightmap heightmap) {
            return cell.biomeRegionId;
         }
      },
      TEMPERATURE("temperature") {
         @Override
         public float read(Cell cell, Heightmap heightmap) {
            return cell.temperature;
         }
      },
      MOISTURE("moisture") {
         @Override
         public float read(Cell cell, Heightmap heightmap) {
            return cell.moisture;
         }
      },
      GRADIENT("gradient") {
         @Override
         public float read(Cell cell, Heightmap heightmap) {
            return cell.gradient;
         }
      },
      HEIGHT_EROSION("height_erosion") {
         @Override
         public float read(Cell cell, Heightmap heightmap) {
            return cell.heightErosion;
         }
      },
      SEDIMENT("sediment") {
         @Override
         public float read(Cell cell, Heightmap heightmap) {
            return cell.sediment;
         }
      };

      public static final Codec<CellSampler.Field> CODEC = StringRepresentable.fromEnum(CellSampler.Field::values);
      private String name;

      private Field(String name) {
         this.name = name;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      public abstract float read(Cell var1, Heightmap var2);
   }

   public static record Marker(CellSampler.Field field) implements MarkerFunction {
      public static final MapCodec<CellSampler.Marker> CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(CellSampler.Field.CODEC.fieldOf("field").forGetter(CellSampler.Marker::field)).apply(instance, CellSampler.Marker::new)
      );

      @Override
      public KeyDispatchDataCodec<CellSampler.Marker> codec() {
         return new KeyDispatchDataCodec<>(CODEC);
      }
   }
}
