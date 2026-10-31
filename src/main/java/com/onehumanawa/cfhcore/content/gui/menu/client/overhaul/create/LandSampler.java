package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.create;

import com.onehumanawa.cfhcore.content.gui.core.stage.node.TerrainNode;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.jetbrains.annotations.Nullable;

public final class LandSampler {
   public static final int RISE = 2;
   public static final int DEEP = 39;
   public static final int DEEPEST = 4;
   private static final int WHITE = -1;
   private static final String[] BADLANDS = new String[]{"terracotta", "orange_terracotta", "yellow_terracotta", "terracotta", "red_terracotta", "white_terracotta", "orange_terracotta"};
   private static final double SOLID = (double)0.390625F;
   private final ChunkGenerator generator;
   private final RandomState random;
   private final LevelHeightAccessor heights;
   private final int sea;
   private final int table;
   private final int rise;
   private final long seed;
   private final @Nullable DensityFunction density;
   private final int bottom;
   private final int top;
   private final int cell;

   public static LandSampler of(WorldCreationContext context, ChunkGenerator generator, long seed, int rise) {
      HolderLookup.RegistryLookup<NormalNoise.NoiseParameters> noises = context.worldgenLoadContext().lookupOrThrow(Registries.NOISE);
      RandomState var10000;
      if (generator instanceof NoiseBasedChunkGenerator noise) {
         var10000 = RandomState.create((NoiseGeneratorSettings)noise.generatorSettings().value(), noises, seed);
      } else {
         var10000 = RandomState.create(NoiseGeneratorSettings.dummy(), noises, seed);
      }

      RandomState random = var10000;
      Optional<LevelStem> stem = context.selectedDimensions().get(LevelStem.OVERWORLD);
      int minY = (Integer)stem.map((s) -> ((DimensionType)s.type().value()).minY()).orElse(-64);
      int height = (Integer)stem.map((s) -> ((DimensionType)s.type().value()).height()).orElse(384);
      return new LandSampler(generator, random, LevelHeightAccessor.create(minY, height), seed, rise);
   }

   public static LandSampler of(ChunkGenerator generator, RandomState random, LevelHeightAccessor heights, long seed) {
      return new LandSampler(generator, random, LevelHeightAccessor.create(heights.getMinBuildHeight(), heights.getHeight()), seed, 2);
   }

   private LandSampler(ChunkGenerator generator, RandomState random, LevelHeightAccessor heights, long seed, int rise) {
      this.generator = generator;
      this.seed = seed;
      this.random = random;
      this.heights = heights;
      this.rise = Math.max(1, rise);
      int minY = heights.getMinBuildHeight();
      int height = heights.getHeight();
      this.sea = generator.getSeaLevel();
      this.table = Math.max(minY, this.sea - 39);
      if (generator instanceof NoiseBasedChunkGenerator noise) {
         NoiseSettings shape = ((NoiseGeneratorSettings)noise.generatorSettings().value()).noiseSettings();
         this.density = random.router().initialDensityWithoutJaggedness();
         this.cell = Math.max(1, shape.getCellHeight());
         this.bottom = shape.minY();
         this.top = shape.minY() + shape.height();
      } else {
         this.density = null;
         this.cell = 1;
         this.bottom = minY;
         this.top = minY + height;
      }

   }

   public boolean quick() {
      return this.density != null;
   }

   private double densityAt(int x, int y, int z) {
      return this.density.compute(new DensityFunction.SinglePointContext(x, y, z));
   }

   public int sea() {
      return this.sea;
   }

   public int cells(int y) {
      return Math.max(1, Math.round((float)(y - this.table) / (float)this.rise));
   }

   private static int lively(int rgb) {
      int r = rgb >> 16 & 255;
      int g = rgb >> 8 & 255;
      int b = rgb & 255;
      return -16777216 | r + (255 - r) / 3 << 16 | g + (255 - g) / 3 << 8 | b + (255 - b) / 3;
   }

   private float chance(int x, int z, int salt) {
      long h = this.seed * -7046029254386353131L + (long)x * -4658895280553007687L + (long)z * -7723592293110705685L + (long)salt * 2685821657736338717L;
      h = (h ^ h >>> 30) * -4658895280553007687L;
      h = (h ^ h >>> 27) * -7723592293110705685L;
      h ^= h >>> 31;
      return (float)(h >>> 40) / 1.6777216E7F;
   }

   public int floor(int x, int z) {
      if (this.density == null) {
         return this.generator.getBaseHeight(x, z, Types.OCEAN_FLOOR_WG, this.heights, this.random);
      } else {
         int solid = Math.min(this.top - this.cell, this.bottom + 5 * this.cell);
         int air = this.top;
         double below = (double)1.0F;
         double above = (double)-1.0F;
         if (this.densityAt(x, solid, z) <= (double)0.390625F) {
            for(int y = this.top - this.cell; y >= this.bottom; y -= this.cell) {
               if (this.densityAt(x, y, z) > (double)0.390625F) {
                  return y;
               }
            }

            return this.bottom;
         } else {
            while(air - solid > this.cell) {
               int mid = solid + Math.max(this.cell, (air - solid) / 2 / this.cell * this.cell);
               if (mid >= air) {
                  break;
               }

               double d = this.densityAt(x, mid, z);
               if (d > (double)0.390625F) {
                  solid = mid;
                  below = d;
               } else {
                  air = mid;
                  above = d;
               }
            }

            if (below > 0.999) {
               below = this.densityAt(x, solid, z);
            }

            if (above < -0.999) {
               above = this.densityAt(x, air, z);
            }

            double span = below - above;
            double part = span <= 1.0E-9 ? (double)0.5F : Mth.clamp((below - (double)0.390625F) / span, (double)0.0F, (double)1.0F);
            return solid + (int)Math.round(part * (double)(air - solid));
         }
      }
   }

   public TerrainNode.Column column(int x, int z, int floor) {
      boolean wet = floor < this.sea;
      Holder<Biome> holder = this.generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(Math.max(floor, this.sea)), QuartPos.fromBlock(z), this.random.sampler());
      Biome biome = (Biome)holder.value();
      String name = ((String)holder.unwrapKey().map((k) -> k.location().getPath()).orElse("")).toLowerCase(Locale.ROOT);
      float temperature = biome.getBaseTemperature();
      boolean cold = temperature < 0.15F;
      int grass = biome.getGrassColor((double)x, (double)z) | -16777216;
      int foliage = biome.getFoliageColor() | -16777216;
      int ground = this.cells(floor);
      if (wet) {
         int water = Math.min(4, Math.max(1, this.cells(this.sea) - ground));
         boolean warm = name.contains("warm") || holder.is(BiomeTags.IS_RIVER) || holder.is(BiomeTags.IS_BEACH);
         String bed = name.contains("swamp") ? "mud" : (warm ? "sand" : (this.sea - floor > 14 ? "gravel" : (this.sea - floor > 5 ? "clay" : "sand")));
         boolean frozen = cold && (name.contains("frozen") || name.contains("ice") || this.sea - floor < 4);
         return new TerrainNode.Column(Math.max(ground, this.cells(this.sea) - water), bed, -1, bed, water, lively(biome.getWaterColor()), frozen, (String)null, (String)null, -1);
      } else {
         String top = "grass_block_top";
         String under = "dirt";
         int tint = grass;
         int above = floor - this.sea;
         if (holder.is(BiomeTags.IS_NETHER)) {
            top = "netherrack";
            under = "netherrack";
            tint = -1;
         } else if (holder.is(BiomeTags.IS_END)) {
            top = "end_stone";
            under = "end_stone";
            tint = -1;
         } else if (holder.is(BiomeTags.IS_BADLANDS)) {
            boolean plateau = above > 12;
            top = plateau ? BADLANDS[Math.floorMod(floor / 3, BADLANDS.length)] : "red_sand";
            under = BADLANDS[Math.floorMod(floor / 3 + 1, BADLANDS.length)];
            tint = -1;
         } else if (name.contains("desert")) {
            top = "sand";
            under = "sandstone";
            tint = -1;
         } else if (name.contains("mushroom")) {
            top = "mycelium_top";
            tint = -1;
         } else if (!name.contains("stony") && !name.contains("stone_shore")) {
            if (name.contains("gravel")) {
               top = "gravel";
               under = "stone";
               tint = -1;
            } else if (!name.contains("frozen_peaks") && !name.contains("ice_spikes")) {
               if (!name.contains("jagged") && !name.contains("snowy_slopes") && !name.contains("grove")) {
                  if (!holder.is(BiomeTags.IS_BEACH) && (above > 1 || name.contains("swamp") || name.contains("mangrove"))) {
                     if (cold) {
                        top = "snow";
                        tint = -1;
                     } else if (name.contains("mangrove")) {
                        top = "mud";
                        under = "mud";
                        tint = -1;
                     } else if (name.contains("old_growth") && name.contains("taiga")) {
                        top = this.chance(x, z, 7) < 0.55F ? "podzol_top" : "grass_block_top";
                        tint = top.startsWith("podzol") ? -1 : grass;
                     } else if (above > 130) {
                        top = above > 170 ? "snow" : "stone";
                        under = "stone";
                        tint = -1;
                     } else if (holder.is(BiomeTags.IS_MOUNTAIN) && above > 85 && this.chance(x, z, 8) < 0.5F) {
                        top = "stone";
                        under = "stone";
                        tint = -1;
                     }
                  } else {
                     top = cold ? "snow" : "sand";
                     under = "sand";
                     tint = -1;
                  }
               } else {
                  top = "snow";
                  under = above > 110 ? "stone" : "dirt";
                  tint = -1;
               }
            } else {
               top = this.chance(x, z, 6) < 0.35F ? "packed_ice" : "snow";
               under = "packed_ice";
               tint = -1;
            }
         } else {
            top = above > 90 && this.chance(x, z, 5) < 0.3F ? "calcite" : "stone";
            under = "stone";
            tint = -1;
         }

         String canopy = null;
         int canopyTint = foliage;
         float trees = 0.0F;
         boolean soil = top.equals("grass_block_top") || top.equals("podzol_top") || top.equals("snow") || top.equals("mud");
         String trunk = "oak_log";
         if (soil && above < 140) {
            if (name.contains("cherry")) {
               canopy = "cherry_leaves";
               trunk = "cherry_log";
               canopyTint = -1;
               trees = 0.22F;
            } else if (name.contains("mangrove")) {
               canopy = "mangrove_leaves";
               trunk = "mangrove_log";
               trees = 0.4F;
            } else if (name.contains("dark_forest")) {
               canopy = "dark_oak_leaves";
               trunk = "dark_oak_log";
               trees = 0.7F;
            } else if (name.contains("birch")) {
               canopy = "birch_leaves";
               trunk = "birch_log";
               canopyTint = -8345771;
               trees = 0.36F;
            } else if (holder.is(BiomeTags.IS_JUNGLE)) {
               canopy = "jungle_leaves";
               trunk = "jungle_log";
               trees = !name.contains("sparse") && !name.contains("bamboo") ? 0.6F : 0.2F;
            } else if (!holder.is(BiomeTags.IS_TAIGA) && !name.contains("grove")) {
               if (holder.is(BiomeTags.IS_SAVANNA)) {
                  canopy = "acacia_leaves";
                  trunk = "acacia_log";
                  trees = 0.05F;
               } else if (name.contains("swamp")) {
                  canopy = "oak_leaves";
                  trees = 0.12F;
               } else if (name.contains("flower_forest")) {
                  canopy = "oak_leaves";
                  trees = 0.26F;
               } else if (!holder.is(BiomeTags.IS_FOREST) && !name.contains("forest")) {
                  if (name.contains("windswept")) {
                     canopy = "spruce_leaves";
                     trunk = "spruce_log";
                     canopyTint = -10380959;
                     trees = 0.04F;
                  } else if (top.equals("grass_block_top")) {
                     canopy = "oak_leaves";
                     trees = 0.008F;
                  }
               } else {
                  canopy = "oak_leaves";
                  trees = 0.38F;
               }
            } else {
               canopy = "spruce_leaves";
               trunk = "spruce_log";
               canopyTint = -10380959;
               trees = name.contains("old_growth") ? 0.45F : 0.32F;
            }
         }

         if (canopy != null && this.chance(x, z, 1) >= trees) {
            canopy = null;
         }

         return new TerrainNode.Column(ground, top, tint, under, 0, -1, false, canopy == null ? null : trunk, canopy, canopyTint);
      }
   }
}
