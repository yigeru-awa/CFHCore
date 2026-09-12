package com.onehumanawa.cfhcore.content.world.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SpreadingSnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import com.onehumanawa.cfhcore.content.world.worldgen.GeneratorContext;
import com.onehumanawa.cfhcore.content.world.worldgen.CFHWorldGenRandomState;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.Cell;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.heightmap.Heightmap;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.heightmap.Levels;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.terrain.TerrainType;
import com.onehumanawa.cfhcore.content.world.worldgen.densityfunction.tile.Tile;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.NoiseUtil;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noise;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noises;

public class DecorateSnowFeature extends Feature<DecorateSnowFeature.Config> {
   private static final float SNOW_ROCK_STEEPNESS = 0.45F;
   private static final float SNOW_ROCK_HEIGHT = 0.37254903F;
   private static final float MIN = (float)min(SnowLayerBlock.LAYERS);
   private static final float MAX = (float)max(SnowLayerBlock.LAYERS);

   public DecorateSnowFeature(Codec<DecorateSnowFeature.Config> codec) {
      super(codec);
   }

   @Override
   public boolean place(FeaturePlaceContext<DecorateSnowFeature.Config> placeContext) {
      WorldGenLevel level = placeContext.level();
      GeneratorContext generatorContext;
      if (level.getLevel().getChunkSource().randomState() instanceof CFHWorldGenRandomState cfhWorldGenRandomState
         && (generatorContext = cfhWorldGenRandomState.generatorContext()) != null) {
         ChunkGenerator generator = placeContext.chunkGenerator();
         ChunkPos chunkPos = new ChunkPos(placeContext.origin());
         int chunkX = chunkPos.x;
         int chunkZ = chunkPos.z;
         ChunkAccess chunk = level.getChunk(chunkX, chunkZ);
         Tile.Chunk tileChunk = generatorContext.cache.provideAtChunk(chunkX, chunkZ).getChunkReader(chunkX, chunkZ);
         Heightmap heightmap = generatorContext.generator.getHeightmap();
         Levels levels = heightmap.levels();
         Noise rand = Noises.white(heightmap.climate().randomSeed(), 1);
         MutableBlockPos pos = new MutableBlockPos();
         DecorateSnowFeature.Config config = placeContext.config();
         ErodeFeature.Config erodeConfig = config.erodeConfig();

         for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
               Cell cell = tileChunk.getCell(x, z);
               int scaledY = levels.scale(cell.height);
               int surfaceY = chunk.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z);
               if (scaledY == surfaceY && scaledY >= generator.getSeaLevel()) {
                  int worldX = chunkPos.getBlockX(x);
                  int worldZ = chunkPos.getBlockZ(z);
                  pos.set(worldX, surfaceY, worldZ);
                  if (config.erode && (double)level.getBiome(pos).value().getTemperature(pos) <= 0.25) {
                     float var = -ColumnDecorator.sampleNoise((float)worldX, (float)worldZ, 16, 0);
                     float hNoise = rand.compute((float)worldX, (float)worldZ, 4) * erodeConfig.heightModifier();
                     float sNoise = rand.compute((float)worldX, (float)worldZ, 5) * erodeConfig.slopeModifier();
                     float vModifier = cell.terrain == TerrainType.VOLCANO ? 0.15F : 0.0F;
                     float height = cell.height + var + hNoise + vModifier;
                     float steepness = cell.gradient + var + sNoise + vModifier;
                     if (snowErosion(erodeConfig, (float)worldX, (float)worldZ, steepness, height)) {
                        Predicate<BlockState> predicate = Types.MOTION_BLOCKING.isOpaque();

                        for (int dy = 2; dy > 0; dy--) {
                           pos.setY(surfaceY + dy);
                           BlockState state = chunk.getBlockState(pos);
                           if (!predicate.test(state) || state.is(Blocks.SNOW)) {
                              erodeSnow(chunk, pos);
                           }
                        }
                     }
                  }

                  if (config.smooth) {
                     pos.setY(surfaceY + 1);
                     BlockState state = chunk.getBlockState(pos);
                     if (state.isAir()) {
                        pos.setY(surfaceY);
                        state = chunk.getBlockState(pos);
                        if (state.isAir()) {
                           continue;
                        }
                     }

                     if (state.is(Blocks.SNOW)) {
                        smoothSnow(chunk, pos, cell, levels, 0.0F);
                     }
                  }
               }
            }
         }

         return true;
      }

      throw new IllegalStateException();
   }

   private static boolean snowErosion(ErodeFeature.Config erodeConfig, float x, float z, float steepness, float height) {
      return steepness > erodeConfig.rockSteepness()
         || steepness > 0.45F && height > 0.37254903F
         || steepness > erodeConfig.dirtSteepness() && height > ColumnDecorator.sampleNoise(x, z, erodeConfig.dirtVar(), erodeConfig.dirtMin());
   }

   private static void erodeSnow(ChunkAccess chunk, MutableBlockPos pos) {
      chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
      if (pos.getY() > 0) {
         pos.setY(pos.getY() - 1);
         BlockState below = chunk.getBlockState(pos);
         if (below.hasProperty(GrassBlock.SNOWY)) {
            chunk.setBlockState(pos, below.setValue(GrassBlock.SNOWY, Boolean.valueOf(false)), false);
         }
      }
   }

   private static void smoothSnow(ChunkAccess chunk, MutableBlockPos pos, Cell cell, Levels levels, float min) {
      float height = cell.height * (float)levels.worldHeight;
      float depth = getDepth(height);
      if (depth > min) {
         int level = getLevel(depth);
         BlockState layer = getState(level);
         if (layer.is(Blocks.AIR)) {
            return;
         }

         chunk.setBlockState(pos, layer, false);
         fixBaseBlock(chunk, pos, layer, level);
      }
   }

   private static void fixBaseBlock(ChunkAccess chunk, BlockPos pos, BlockState layerMaterial, int level) {
      if (level > 1 && layerMaterial.is(Blocks.SNOW)) {
         BlockPos pos1 = pos.below();
         BlockState below = chunk.getBlockState(pos1);
         if (below.getBlock() instanceof SpreadingSnowyDirtBlock) {
            chunk.setBlockState(pos1, Blocks.DIRT.defaultBlockState(), false);
         }
      }
   }

   private static BlockState getState(int level) {
      if ((float)level < MIN) {
         return Blocks.AIR.defaultBlockState();
      } else {
         return (float)level >= MAX
            ? Blocks.SNOW_BLOCK.defaultBlockState()
            : Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, Integer.valueOf(level));
      }
   }

   private static int getLevel(float depth) {
      if (depth > 1.0F) {
         depth = getDepth(depth);
      } else if (depth < 0.0F) {
         depth = 0.0F;
      }

      return NoiseUtil.round(depth * MAX);
   }

   private static float getDepth(float height) {
      return height - (float)((int)height);
   }

   private static int min(Property<Integer> property) {
      return property.getPossibleValues().stream().min(Integer::compareTo).orElse(0);
   }

   private static int max(Property<Integer> property) {
      return property.getPossibleValues().stream().max(Integer::compareTo).orElse(0);
   }

   public static record Config(boolean erode, boolean smooth, ErodeFeature.Config erodeConfig) implements FeatureConfiguration {
      public static final Codec<DecorateSnowFeature.Config> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
                  Codec.BOOL.fieldOf("erode").forGetter(DecorateSnowFeature.Config::erode),
                  Codec.BOOL.fieldOf("smooth").forGetter(DecorateSnowFeature.Config::smooth),
                  ErodeFeature.Config.CODEC.fieldOf("erode_config").forGetter(DecorateSnowFeature.Config::erodeConfig)
               )
               .apply(instance, DecorateSnowFeature.Config::new)
      );
   }
}
