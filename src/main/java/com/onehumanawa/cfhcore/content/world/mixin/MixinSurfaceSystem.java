package com.onehumanawa.cfhcore.content.world.mixin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.SurfaceRules.RuleSource;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.onehumanawa.cfhcore.content.world.CFHWorldGenCommon;
import com.onehumanawa.cfhcore.content.world.worldgen.GeneratorContext;
import com.onehumanawa.cfhcore.content.world.worldgen.CFHWorldGenRandomState;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.Cell;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.heightmap.Levels;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.rivermap.ContinentalHydrology;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.rivermap.river.RiverCarverSettings;
import com.onehumanawa.cfhcore.content.world.worldgen.densityfunction.tile.Tile;
import com.onehumanawa.cfhcore.content.world.worldgen.surface.CFHWorldGenSurfaceSystem;
import com.onehumanawa.cfhcore.content.world.worldgen.surface.rule.StrataRule;

@Mixin({SurfaceSystem.class})
@Implements({@Interface(
      iface = CFHWorldGenSurfaceSystem.class,
      prefix = "cfhcore$CFHWorldGenSurfaceSystem$"
   )})
class MixinSurfaceSystem {
   private static final ResourceLocation GEOLOGY_RANDOM = CFHWorldGenCommon.location("geology");
   private RandomState randomState;
   private Map<ResourceLocation, List<List<StrataRule.Layer>>> strata;

   @Inject(
      at = {@At("TAIL")},
      method = {"<init>(Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/block/state/BlockState;ILnet/minecraft/world/level/levelgen/PositionalRandomFactory;)V"}
   )
   public void SurfaceSystem(RandomState randomState, BlockState blockState, int i, PositionalRandomFactory positionalRandomFactory, CallbackInfo callback) {
      this.randomState = randomState;
      this.strata = new ConcurrentHashMap<>();
   }

   @Inject(
      method = {"buildSurface(Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/biome/BiomeManager;Lnet/minecraft/core/Registry;ZLnet/minecraft/world/level/levelgen/WorldGenerationContext;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/levelgen/NoiseChunk;Lnet/minecraft/world/level/levelgen/SurfaceRules$RuleSource;)V"},
      at = {@At("HEAD")}
   )
   private void onBuildSurface(
      RandomState randomState,
      BiomeManager biomeManager,
      Registry<Biome> biomes,
      boolean useLegacyRandom,
      WorldGenerationContext context,
      ChunkAccess chunk,
      NoiseChunk noiseChunk,
      RuleSource ruleSource,
      CallbackInfo ci
   ) {
      if (randomState instanceof CFHWorldGenRandomState cfhWorldGenRandomState) {
         GeneratorContext genCtx = cfhWorldGenRandomState.generatorContext();
         if (genCtx != null) {
            this.cfhcore$placeRiverWater(chunk, biomeManager, genCtx);
         }
      }
   }

   public List<List<StrataRule.Layer>> cfhcore$CFHWorldGenSurfaceSystem$getOrCreateStrata(
      ResourceLocation name, Function<RandomSource, List<List<StrataRule.Layer>>> strata
   ) {
      return this.strata.computeIfAbsent(name, k -> {
         PositionalRandomFactory factory = this.randomState.getOrCreateRandomFactory(GEOLOGY_RANDOM);
         return strata.apply(factory.fromHashOf(k));
      });
   }

   @Unique
   private void cfhcore$placeRiverWater(ChunkAccess chunk, BiomeManager biomeManager, GeneratorContext genCtx) {
      ChunkPos chunkPos = chunk.getPos();
      Tile tile = genCtx.cache.provideAtChunk(chunkPos.x, chunkPos.z);
      Tile.Chunk reader = tile.getChunkReader(chunkPos.x, chunkPos.z);
      Levels levels = genCtx.generator.getHeightmap().levels();
      float oceanLevel = levels.water;
      MutableBlockPos pos = new MutableBlockPos();
      BlockState water = Blocks.WATER.defaultBlockState();
      BlockState flowingWater = water.setValue(BlockStateProperties.LEVEL, Integer.valueOf(1));
      BlockState stone = Blocks.STONE.defaultBlockState();

      for (int localX = 0; localX < 16; localX++) {
         for (int localZ = 0; localZ < 16; localZ++) {
            int globalX = chunkPos.getMinBlockX() + localX;
            int globalZ = chunkPos.getMinBlockZ() + localZ;
            Cell cell = reader.getCell(localX, localZ);
            boolean isWaterCell = (cell.terrain.isRiver() || cell.terrain.isLake() || cell.terrain.isWetland()) && cell.riverWaterLevel > 0.0F;
            int scaledY = levels.scale(cell.height);
            if (isWaterCell) {
               int waterY = levels.scale(ContinentalHydrology.getWeightedWaterHeight(cell.waterTable) + oceanLevel);
               if (waterY < levels.scale(levels.water)) {
                  waterY = levels.scale(levels.water);
               }

               if (waterY > scaledY) {
                  boolean isTransitionColumn = false;
                  boolean multiBlockDrop = false;
                  int lowestNeighborWaterY = waterY;
                  int[] dx = new int[]{0, 0, 1, -1};
                  int[] dz = new int[]{-1, 1, 0, 0};

                  for (int i = 0; i < 4; i++) {
                     int nx = globalX + dx[i];
                     int nz = globalZ + dz[i];
                     Tile neighborTile = genCtx.cache.provideAtChunk(nx >> 4, nz >> 4);
                     Tile.Chunk neighborReader = neighborTile.getChunkReader(nx >> 4, nz >> 4);
                     Cell neighborCell = neighborReader.getCell(nx & 15, nz & 15);
                     boolean nIsWaterCell = (neighborCell.terrain.isRiver() || neighborCell.terrain.isLake() || neighborCell.terrain.isWetland())
                        && neighborCell.riverWaterLevel > 0.0F;
                     if (nIsWaterCell) {
                        int nWaterY = levels.scale(ContinentalHydrology.getWeightedWaterHeight(neighborCell.waterTable) + levels.water);
                        if (nWaterY < levels.scale(levels.water)) {
                           nWaterY = levels.scale(levels.water);
                        }

                        if (nWaterY < waterY) {
                           isTransitionColumn = true;
                           lowestNeighborWaterY = Math.min(lowestNeighborWaterY, nWaterY);
                           if (waterY - nWaterY > 1) {
                              multiBlockDrop = true;
                           }
                        }
                     }
                  }

                  if (isTransitionColumn && multiBlockDrop) {
                     scaledY = (scaledY + lowestNeighborWaterY) / 2;
                  }

                  for (int wy = scaledY + 1; wy <= waterY; wy++) {
                     boolean isTopBlock = wy == waterY;
                     pos.set(globalX, wy, globalZ);
                     if (isTopBlock && isTransitionColumn && multiBlockDrop) {
                        chunk.setBlockState(pos, water, false);
                        chunk.markPosForPostprocessing(pos);
                     } else if (isTopBlock && isTransitionColumn && !multiBlockDrop) {
                        if (biomeManager.getBiome(pos).value().coldEnoughToSnow(pos)) {
                           chunk.setBlockState(pos, Blocks.ICE.defaultBlockState(), false);
                        } else {
                           chunk.setBlockState(pos, flowingWater, false);
                        }
                     } else if (isTopBlock || !multiBlockDrop) {
                        chunk.setBlockState(pos, water, false);
                        if (isTopBlock) {
                           chunk.markPosForPostprocessing(pos);
                        }
                     } else if (wy > lowestNeighborWaterY) {
                        chunk.setBlockState(pos, flowingWater, false);
                        chunk.markPosForPostprocessing(pos);
                     } else {
                        chunk.setBlockState(pos, stone, false);
                     }
                  }
               }
            } else {
               int oceanY = levels.scale(levels.water);
               if (scaledY >= oceanY) {
                  int maxNeighborWaterY = scaledY;
                  float maxRiverMask = 0.0F;
                  int[] dx = new int[]{0, 0, 1, -1};
                  int[] dz = new int[]{-1, 1, 0, 0};

                  for (int ix = 0; ix < 4; ix++) {
                     int nx = globalX + dx[ix];
                     int nz = globalZ + dz[ix];
                     Tile neighborTile = genCtx.cache.provideAtChunk(nx >> 4, nz >> 4);
                     Tile.Chunk neighborReader = neighborTile.getChunkReader(nx >> 4, nz >> 4);
                     Cell neighborCell = neighborReader.getCell(nx & 15, nz & 15);
                     if (neighborCell.terrain.isRiver() || neighborCell.terrain.isLake()) {
                        int nWaterYx = levels.scale(ContinentalHydrology.getWeightedWaterHeight(neighborCell.waterTable) + oceanLevel);
                        if (nWaterYx > maxNeighborWaterY) {
                           maxNeighborWaterY = nWaterYx;
                           maxRiverMask = neighborCell.riverMask;
                        }
                     }
                  }

                  int waterTableCeil = levels.scale(ContinentalHydrology.getWeightedWaterHeight(cell.waterTable) + oceanLevel);
                  boolean isInRiverZone = cell.riverZone == RiverCarverSettings.RiverZone.Banks
                     || cell.riverZone == RiverCarverSettings.RiverZone.ValleyFloor
                     || cell.riverZone == RiverCarverSettings.RiverZone.ValleyFadeout;
                  boolean isLowerThanWaterTable = scaledY < waterTableCeil;
                  boolean isAboveOcean = scaledY > oceanY;
                  if (isInRiverZone && isLowerThanWaterTable && isAboveOcean) {
                     for (int wyx = scaledY + 1; wyx <= waterTableCeil; wyx++) {
                        pos.set(globalX, wyx, globalZ);
                        chunk.setBlockState(pos, stone, false);
                     }
                  }

                  if (maxNeighborWaterY > scaledY) {
                     for (int wyx = scaledY + 1; wyx <= maxNeighborWaterY; wyx++) {
                        pos.set(globalX, wyx, globalZ);
                        chunk.setBlockState(pos, stone, false);
                     }
                  }
               }
            }
         }
      }
   }
}
