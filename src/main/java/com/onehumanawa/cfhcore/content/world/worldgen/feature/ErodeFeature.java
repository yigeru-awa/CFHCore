package com.onehumanawa.cfhcore.content.world.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import com.onehumanawa.cfhcore.content.world.tags.CFHWorldGenBlockTags;
import com.onehumanawa.cfhcore.content.world.worldgen.GeneratorContext;
import com.onehumanawa.cfhcore.content.world.worldgen.CFHWorldGenRandomState;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.Cell;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.heightmap.Heightmap;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.heightmap.Levels;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.terrain.TerrainType;
import com.onehumanawa.cfhcore.content.world.worldgen.densityfunction.tile.Tile;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noise;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noises;

public class ErodeFeature extends Feature<ErodeFeature.Config> {
   public ErodeFeature(Codec<ErodeFeature.Config> codec) {
      super(codec);
   }

   @Override
   public boolean place(FeaturePlaceContext<ErodeFeature.Config> placeContext) {
      WorldGenLevel level = placeContext.level();
      GeneratorContext generatorContext;
      if (level.getLevel().getChunkSource().randomState() instanceof CFHWorldGenRandomState cfhWorldGenRandomState
         && (generatorContext = cfhWorldGenRandomState.generatorContext()) != null) {
         ChunkPos chunkPos = new ChunkPos(placeContext.origin());
         int chunkX = chunkPos.x;
         int chunkZ = chunkPos.z;
         ChunkGenerator generator = placeContext.chunkGenerator();
         ChunkAccess chunk = level.getChunk(chunkX, chunkZ);
         Tile.Chunk tileChunk = generatorContext.cache.provideAtChunk(chunkX, chunkZ).getChunkReader(chunkX, chunkZ);
         Heightmap heightmap = generatorContext.generator.getHeightmap();
         Levels levels = heightmap.levels();
         Noise rand = Noises.white(heightmap.climate().randomSeed(), 1);
         Noise desertErosionVariance = makeDesertErosionVariance(levels);
         MutableBlockPos pos = new MutableBlockPos();
         ErodeFeature.Config config = placeContext.config();

         for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
               int worldX = chunkPos.getBlockX(x);
               int worldZ = chunkPos.getBlockZ(z);
               Cell cell = tileChunk.getCell(x, z);
               int scaledY = levels.scale(cell.height);
               int surfaceY = chunk.getHeight(Types.WORLD_SURFACE_WG, x, z);
               Holder<Biome> biome = level.getBiome(pos.set(worldX, surfaceY, worldZ));
               pos.set(worldX, surfaceY, worldZ);
               if (biome.is(Biomes.DESERT)) {
                  erodeDesert(desertErosionVariance, levels, chunk, cell, pos, surfaceY);
               } else if (surfaceY <= scaledY && surfaceY >= generator.getSeaLevel() - 1 && !biome.is(Biomes.WOODED_BADLANDS) && !biome.is(Biomes.BADLANDS)) {
                  erodeColumn(config, rand, generator, chunk, cell, pos, surfaceY);
                  pos.setY(surfaceY);

                  while (!level.getBlockState(pos.setY(pos.getY() + 1)).canSurvive(level, pos)) {
                     level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                  }
               }
            }
         }

         return true;
      }

      throw new IllegalStateException();
   }

   @Deprecated(
      forRemoval = true
   )
   private static Noise makeDesertErosionVariance(Levels levels) {
      Noise noise = Noises.perlin(435, 8, 1);
      return Noises.mul(noise, levels.scale(16));
   }

   private static void erodeDesert(Noise variance, Levels levels, ChunkAccess chunk, Cell cell, MutableBlockPos pos, int surfaceY) {
      float min = levels.ground(10);
      float threshold = levels.ground(40);
      if (!(cell.gradient < 0.15F)) {
         if (!(cell.height < min)) {
            float value = cell.height + variance.compute((float)pos.getX(), (float)pos.getZ(), 0);
            if (cell.gradient > 0.3F || value > threshold) {
               BlockState state = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
               if (value > threshold) {
                  if ((double)cell.gradient > 0.975) {
                     state = Blocks.TERRACOTTA.defaultBlockState();
                  } else if ((double)cell.gradient > 0.85) {
                     state = Blocks.BROWN_TERRACOTTA.defaultBlockState();
                  } else if ((double)cell.gradient > 0.75) {
                     state = Blocks.ORANGE_TERRACOTTA.defaultBlockState();
                  } else if ((double)cell.gradient > 0.65) {
                     state = Blocks.TERRACOTTA.defaultBlockState();
                  }
               }

               for (int dy = 0; dy < 4; dy++) {
                  chunk.setBlockState(pos.setY(surfaceY - dy), state, false);
               }
            }
         }
      }
   }

   private static void erodeColumn(
      ErodeFeature.Config config, Noise rand, ChunkGenerator generator, ChunkAccess chunk, Cell cell, MutableBlockPos pos, int surfaceY
   ) {
      if (!cell.terrain.isRiver() && !cell.terrain.isWetland()) {
         if (cell.terrain != TerrainType.VOLCANO_PIPE) {
            BlockState top = chunk.getBlockState(pos);
            if (top.is(CFHWorldGenBlockTags.ERODIBLE)) {
               BlockState material = getMaterial(
                  config,
                  rand,
                  cell,
                  pos,
                  top,
                  generator instanceof NoiseBasedChunkGenerator noiseChunkGenerator
                     ? noiseChunkGenerator.generatorSettings().value().defaultBlock()
                     : Blocks.STONE.defaultBlockState()
               );
               if (material != top) {
                  if (material.is(CFHWorldGenBlockTags.ROCK)) {
                     erodeRock(chunk, cell, pos, surfaceY);
                     return;
                  }

                  ColumnDecorator.fillDownSolid(chunk, pos, surfaceY, surfaceY - 4, material);
               }

               placeScree(config, rand, chunk, cell, pos, surfaceY);
            }
         }
      }
   }

   private static void erodeRock(ChunkAccess chunk, Cell cell, MutableBlockPos pos, int y) {
      int depth = 32;
      BlockState material = Blocks.GRAVEL.defaultBlockState();

      for (int dy = 3; dy < 32; dy++) {
         pos.setY(y - dy);
         BlockState state = chunk.getBlockState(pos);
         if (state.is(CFHWorldGenBlockTags.ROCK)) {
            material = state;
            depth = dy + 1;
            break;
         }
      }

      for (int dyx = 0; dyx < depth; dyx++) {
         ColumnDecorator.replaceSolid(chunk, pos.setY(y - dyx), material);
      }
   }

   private static void placeScree(ErodeFeature.Config config, Noise rand, ChunkAccess chunk, Cell cell, MutableBlockPos pos, int surfaceY) {
      int x = pos.getX();
      int z = pos.getZ();
      float steepness = cell.gradient + rand.compute((float)x, (float)z, 1) * config.slopeModifier();
      if (!(steepness < config.screeSteepness())) {
         float sediment = cell.sediment * config.sedimentNoise();
         float noise = rand.compute((float)x, (float)z, 2) * config.sedimentNoise();
         if (sediment + noise > config.screeValue()) {
            ColumnDecorator.fillDownSolid(chunk, pos, surfaceY, surfaceY - 2, Blocks.GRAVEL.defaultBlockState());
         }
      }
   }

   private static BlockState getMaterial(ErodeFeature.Config config, Noise rand, Cell cell, MutableBlockPos pos, BlockState top, BlockState middle) {
      int x = pos.getX();
      int z = pos.getZ();
      float height = cell.height + rand.compute((float)x, (float)z, 0) * config.heightModifier();
      float steepness = cell.gradient + rand.compute((float)x, (float)z, 1) * config.slopeModifier();
      if (steepness > config.rockSteepness() || height > ColumnDecorator.sampleNoise((float)x, (float)z, config.rockVar(), config.rockMin())) {
         return rock(middle);
      } else {
         return steepness > config.dirtSteepness() && height > ColumnDecorator.sampleNoise((float)x, (float)z, config.dirtVar(), config.dirtMin())
            ? ground(top)
            : top;
      }
   }

   private static BlockState rock(BlockState state) {
      return state.is(CFHWorldGenBlockTags.ROCK) ? state : Blocks.STONE.defaultBlockState();
   }

   private static BlockState ground(BlockState state) {
      if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM)) {
         return Blocks.COARSE_DIRT.defaultBlockState();
      } else if (state.is(BlockTags.BASE_STONE_OVERWORLD)) {
         return Blocks.GRAVEL.defaultBlockState();
      } else if (state.is(BlockTags.DIRT)) {
         return state;
      } else if (state.is(Blocks.SAND)) {
         return Blocks.SMOOTH_SANDSTONE.defaultBlockState();
      } else {
         return state.is(Blocks.RED_SAND) ? Blocks.SMOOTH_RED_SANDSTONE.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState();
      }
   }

   public static record Config(
      int rockVar,
      int rockMin,
      int dirtVar,
      int dirtMin,
      float rockSteepness,
      float dirtSteepness,
      float screeSteepness,
      float heightModifier,
      float slopeModifier,
      float sedimentModifier,
      float sedimentNoise,
      float screeValue
   ) implements FeatureConfiguration {
      public static final Codec<ErodeFeature.Config> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
                  Codec.INT.fieldOf("rock_var").forGetter(ErodeFeature.Config::rockVar),
                  Codec.INT.fieldOf("rock_min").forGetter(ErodeFeature.Config::rockMin),
                  Codec.INT.fieldOf("dirt_var").forGetter(ErodeFeature.Config::dirtVar),
                  Codec.INT.fieldOf("dirt_min").forGetter(ErodeFeature.Config::dirtMin),
                  Codec.FLOAT.fieldOf("rock_steepness").forGetter(ErodeFeature.Config::rockSteepness),
                  Codec.FLOAT.fieldOf("dirt_steepness").forGetter(ErodeFeature.Config::dirtSteepness),
                  Codec.FLOAT.fieldOf("scree_steepness").forGetter(ErodeFeature.Config::screeSteepness),
                  Codec.FLOAT.fieldOf("height_modifier").forGetter(ErodeFeature.Config::heightModifier),
                  Codec.FLOAT.fieldOf("slope_modifier").forGetter(ErodeFeature.Config::slopeModifier),
                  Codec.FLOAT.fieldOf("sediment_modifier").forGetter(ErodeFeature.Config::sedimentModifier),
                  Codec.FLOAT.fieldOf("sediment_noise").forGetter(ErodeFeature.Config::sedimentNoise),
                  Codec.FLOAT.fieldOf("screeValue").forGetter(ErodeFeature.Config::screeValue)
               )
               .apply(instance, ErodeFeature.Config::new)
      );
   }
}
