package com.onehumanawa.cfhcore.content.world.worldgen;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.Beardifier.Rigid;
import net.minecraft.world.level.levelgen.DensityFunctions.BeardifierOrMarker;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool.Projection;
import com.onehumanawa.cfhcore.content.world.mixin.BeardifierAccessor;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.Cell;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.heightmap.WorldLookup;

public class MaxHeightUtil {
   private static final int GLOBAL_TERRAIN_MARGIN = 32;
   private static final ThreadLocal<Cell> LOOKUP_CELL = ThreadLocal.withInitial(Cell::new);

   public static int getMaxHeight(int chunkX, int chunkZ, int maxHeight, NoiseGeneratorSettings generatorSettings, NoiseSettings noiseSettings) {
      int minY = noiseSettings.minY();
      int height = noiseSettings.height();
      int cellHeight = noiseSettings.getCellHeight();
      int seaLevel = generatorSettings.seaLevel();
      int scaledDynamicHeight = -minY + Math.max(seaLevel, maxHeight + 1);
      return Math.min(height, (scaledDynamicHeight / cellHeight + 1) * cellHeight);
   }

   public static NoiseSettings capNoiseSettingsForColumn(
      NoiseSettings noiseSettings, NoiseGeneratorSettings generatorSettings, WorldLookup lookup, int blockX, int blockZ
   ) {
      int cellWidth = noiseSettings.getCellWidth();
      int firstBlockX = Math.floorDiv(blockX, cellWidth) * cellWidth;
      int firstBlockZ = Math.floorDiv(blockZ, cellWidth) * cellWidth;
      Cell cell = LOOKUP_CELL.get();
      float maxCellHeight = Float.MIN_VALUE;

      for (int dx = 0; dx <= cellWidth; dx += cellWidth) {
         for (int dz = 0; dz <= cellWidth; dz += cellWidth) {
            lookup.applyCell(cell.reset(), firstBlockX + dx, firstBlockZ + dz, false);
            maxCellHeight = Math.max(maxCellHeight, cell.height);
         }
      }

      lookup.applyCell(cell.reset(), QuartPos.toBlock(QuartPos.fromBlock(blockX)), QuartPos.toBlock(QuartPos.fromBlock(blockZ)), false);
      maxCellHeight = Math.max(maxCellHeight, cell.height);
      int maxTerrainHeight = Mth.ceil(maxCellHeight * 256.0F);
      int cappedHeight = getMaxHeight(blockX >> 4, blockZ >> 4, maxTerrainHeight, generatorSettings, noiseSettings);
      return cappedHeight < noiseSettings.height()
         ? new NoiseSettings(noiseSettings.minY(), cappedHeight, noiseSettings.noiseSizeHorizontal(), noiseSettings.noiseSizeVertical())
         : noiseSettings;
   }

   public static int getGlobalScanHeight(NoiseSettings noiseSettings, NoiseGeneratorSettings generatorSettings, int scalerHeight) {
      int fullHeight = generatorSettings.noiseSettings().height();
      int cellHeight = noiseSettings.getCellHeight();
      int seaLevel = generatorSettings.seaLevel();
      int scaledBound = -noiseSettings.minY() + Math.max(seaLevel, scalerHeight + 32 + 1);
      return Math.min(fullHeight, (scaledBound / cellHeight + 1) * cellHeight);
   }

   public static int getMaxHeight(
      ChunkPos chunkPos, int maxHeight, NoiseGeneratorSettings generatorSettings, NoiseSettings noiseSettings, StructureManager structureManager
   ) {
      int dynamicHeight = getMaxHeight(chunkPos.x, chunkPos.z, maxHeight, generatorSettings, noiseSettings);
      return Math.clamp((long)getHighestStructureY(chunkPos, noiseSettings, structureManager), dynamicHeight, noiseSettings.height());
   }

   public static int getMaxHeight(
      int chunkX, int chunkZ, int maxHeight, NoiseGeneratorSettings generatorSettings, NoiseSettings noiseSettings, BeardifierOrMarker beardifierOrMarker
   ) {
      int dynamicHeight = getMaxHeight(chunkX, chunkZ, maxHeight, generatorSettings, noiseSettings);
      if (beardifierOrMarker instanceof BeardifierAccessor accessor) {
         ObjectListIterator<Rigid> pieceIterator = accessor.getPieceIterator();
         ObjectListIterator<JigsawJunction> junctionIterator = accessor.getJunctionIterator();
         dynamicHeight = Math.clamp((long)getHighestStructureY(noiseSettings, pieceIterator, junctionIterator), dynamicHeight, noiseSettings.height());
      }

      return dynamicHeight;
   }

   private static int getHighestStructureY(NoiseSettings noiseSettings, ObjectListIterator<Rigid> pieces, ObjectListIterator<JigsawJunction> junctions) {
      int highestY = Integer.MIN_VALUE;

      while (pieces.hasNext()) {
         BoundingBox boundingBox = pieces.next().box();
         highestY = Math.max(highestY, boundingBox.maxY());
      }

      pieces.back(Integer.MAX_VALUE);
      return -noiseSettings.minY() + highestY;
   }

   private static int getHighestStructureY(ChunkPos chunkPos, NoiseSettings noiseSettings, StructureManager structureManager) {
      int minBlockX = chunkPos.getMinBlockX();
      int minBlockZ = chunkPos.getMinBlockZ();
      ObjectList<Rigid> pieces = new ObjectArrayList<>(10);
      ObjectList<JigsawJunction> junctions = new ObjectArrayList<>(32);
      structureManager.startsForStructure(chunkPos, structure -> structure.terrainAdaptation() != TerrainAdjustment.NONE).forEach(structureStart -> {
         TerrainAdjustment terrainAdjustment = structureStart.getStructure().terrainAdaptation();

         for (StructurePiece structurePiece : structureStart.getPieces()) {
            if (structurePiece.isCloseToChunk(chunkPos, 12)) {
               if (structurePiece instanceof PoolElementStructurePiece) {
                  PoolElementStructurePiece poolElementStructurePiece = (PoolElementStructurePiece)structurePiece;
                  Projection projection = poolElementStructurePiece.getElement().getProjection();
                  if (projection == Projection.RIGID) {
                     pieces.add(new Rigid(poolElementStructurePiece.getBoundingBox(), terrainAdjustment, poolElementStructurePiece.getGroundLevelDelta()));
                  }

                  for (JigsawJunction jigsawJunction : poolElementStructurePiece.getJunctions()) {
                     int sourceX = jigsawJunction.getSourceX();
                     int sourceZ = jigsawJunction.getSourceZ();
                     if (sourceX > minBlockX - 12 && sourceZ > minBlockZ - 12 && sourceX < minBlockX + 15 + 12 && sourceZ < minBlockZ + 15 + 12) {
                        junctions.add(jigsawJunction);
                     }
                  }
               } else {
                  pieces.add(new Rigid(structurePiece.getBoundingBox(), terrainAdjustment, 0));
               }
            }
         }
      });
      return getHighestStructureY(noiseSettings, pieces.iterator(), junctions.iterator());
   }
}
