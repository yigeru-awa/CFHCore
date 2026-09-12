package com.onehumanawa.cfhcore.content.world.worldgen.densityfunction.tile.filter;

import java.util.function.IntFunction;
import com.onehumanawa.cfhcore.content.world.preset.settings.FilterSettings;
import com.onehumanawa.cfhcore.content.world.worldgen.GeneratorContext;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.Cell;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.heightmap.Levels;
import com.onehumanawa.cfhcore.content.world.worldgen.densityfunction.tile.Size;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.NoiseUtil;
import com.onehumanawa.cfhcore.content.world.worldgen.util.FastRandom;

public class Erosion implements Filter {
   private final float erodeSpeed;
   private final float depositSpeed;
   private final float initialSpeed;
   private final float initialWaterVolume;
   private final int maxDropletLifetime;
   private final int[][] erosionBrushIndices;
   private final float[][] erosionBrushWeights;
   private final int seed;
   private final int mapSize;
   private final Modifier modifier;

   public Erosion(int seed, int mapSize, FilterSettings.Erosion settings, Modifier modifier) {
      this.seed = seed;
      this.mapSize = mapSize;
      this.modifier = modifier;
      this.erodeSpeed = settings.erosionRate;
      this.depositSpeed = settings.depositeRate;
      this.initialSpeed = settings.dropletVelocity;
      this.initialWaterVolume = settings.dropletVolume;
      this.maxDropletLifetime = settings.dropletLifetime;
      this.erosionBrushIndices = new int[mapSize * mapSize][];
      this.erosionBrushWeights = new float[mapSize * mapSize][];
      this.initBrushes(mapSize, 4);
   }

   public int getSize() {
      return this.mapSize;
   }

   @Override
   public void apply(Filterable map, int regionX, int regionZ, int iterationsPerChunk) {
      int chunkX = map.getBlockX() >> 4;
      int chunkZ = map.getBlockZ() >> 4;
      int lengthChunks = map.getBlockSize().total() >> 4;
      int borderChunks = map.getBlockSize().border() >> 4;
      Size size = map.getBlockSize();
      int mapSize = size.total();
      float maxPos = (float)(mapSize - 2);
      Cell[] cells = map.getBacking();
      Erosion.TerrainPos gradient1 = new Erosion.TerrainPos();
      Erosion.TerrainPos gradient2 = new Erosion.TerrainPos();
      FastRandom random = new FastRandom();

      for (int i = 0; i < iterationsPerChunk; i++) {
         long iterationSeed = NoiseUtil.seed(this.seed, i);

         for (int cz = 0; cz < lengthChunks; cz++) {
            int relZ = cz << 4;
            int seedZ = chunkZ + cz - borderChunks;

            for (int cx = 0; cx < lengthChunks; cx++) {
               int relX = cx << 4;
               int seedX = chunkX + cx - borderChunks;
               long chunkSeed = NoiseUtil.seed(seedX, seedZ);
               random.seed(chunkSeed, iterationSeed);
               float posX = (float)(relX + random.nextInt(16));
               float posZ = (float)(relZ + random.nextInt(16));
               posX = NoiseUtil.clamp(posX, 1.0F, maxPos);
               posZ = NoiseUtil.clamp(posZ, 1.0F, maxPos);
               this.applyDrop(posX, posZ, cells, mapSize, gradient1, gradient2);
            }
         }
      }
   }

   private void applyDrop(float posX, float posY, Cell[] cells, int mapSize, Erosion.TerrainPos gradient1, Erosion.TerrainPos gradient2) {
      float dirX = 0.0F;
      float dirY = 0.0F;
      float sediment = 0.0F;
      float speed = this.initialSpeed;
      float water = this.initialWaterVolume;
      gradient1.reset();
      gradient2.reset();

      for (int lifetime = 0; lifetime < this.maxDropletLifetime; lifetime++) {
         int nodeX = (int)posX;
         int nodeY = (int)posY;
         int dropletIndex = nodeY * mapSize + nodeX;
         float cellOffsetX = posX - (float)nodeX;
         float cellOffsetY = posY - (float)nodeY;
         gradient1.at(cells, mapSize, posX, posY);
         dirX = dirX * 0.05F - gradient1.gradientX * 0.95F;
         dirY = dirY * 0.05F - gradient1.gradientY * 0.95F;
         float len = (float)Math.sqrt((double)(dirX * dirX + dirY * dirY));
         if (Float.isNaN(len)) {
            len = 0.0F;
         }

         if (len != 0.0F) {
            dirX /= len;
            dirY /= len;
         }

         posX += dirX;
         posY += dirY;
         if (dirX == 0.0F && dirY == 0.0F || posX < 0.0F || posX >= (float)(mapSize - 1) || posY < 0.0F || posY >= (float)(mapSize - 1)) {
            return;
         }

         float newHeight = gradient2.at(cells, mapSize, posX, posY).height;
         float deltaHeight = newHeight - gradient1.height;
         float sedimentCapacity = Math.max(-deltaHeight * speed * water * 4.0F, 0.01F);
         if (!(sediment > sedimentCapacity) && !(deltaHeight > 0.0F)) {
            float amountToErode = Math.min((sedimentCapacity - sediment) * this.erodeSpeed, -deltaHeight);
            int[] brushIndices = this.erosionBrushIndices[dropletIndex];
            float[] brushWeights = this.erosionBrushWeights[dropletIndex];

            for (int brushPointIndex = 0; brushPointIndex < brushIndices.length; brushPointIndex++) {
               int nodeIndex = brushIndices[brushPointIndex];
               Cell cell = cells[nodeIndex];
               float brushWeight = brushWeights[brushPointIndex];
               float weighedErodeAmount = amountToErode * brushWeight;
               float deltaSediment = Math.min(cell.height, weighedErodeAmount);
               this.erode(cell, deltaSediment);
               sediment += deltaSediment;
            }
         } else {
            float amountToDeposit = deltaHeight > 0.0F ? Math.min(deltaHeight, sediment) : (sediment - sedimentCapacity) * this.depositSpeed;
            sediment -= amountToDeposit;
            this.deposit(cells[dropletIndex], amountToDeposit * (1.0F - cellOffsetX) * (1.0F - cellOffsetY));
            this.deposit(cells[dropletIndex + 1], amountToDeposit * cellOffsetX * (1.0F - cellOffsetY));
            this.deposit(cells[dropletIndex + mapSize], amountToDeposit * (1.0F - cellOffsetX) * cellOffsetY);
            this.deposit(cells[dropletIndex + mapSize + 1], amountToDeposit * cellOffsetX * cellOffsetY);
         }

         speed = (float)Math.sqrt((double)(speed * speed + deltaHeight * 3.0F));
         water *= 0.99F;
         if (Float.isNaN(speed)) {
            speed = 0.0F;
         }
      }
   }

   private void initBrushes(int size, int radius) {
      int[] xOffsets = new int[radius * radius * 4];
      int[] yOffsets = new int[radius * radius * 4];
      float[] weights = new float[radius * radius * 4];
      float weightSum = 0.0F;
      int addIndex = 0;

      for (int i = 0; i < this.erosionBrushIndices.length; i++) {
         int centreX = i % size;
         int centreY = i / size;
         if (centreY <= radius || centreY >= size - radius || centreX <= radius + 1 || centreX >= size - radius) {
            weightSum = 0.0F;
            addIndex = 0;

            for (int y = -radius; y <= radius; y++) {
               for (int x = -radius; x <= radius; x++) {
                  float sqrDst = (float)(x * x + y * y);
                  if (sqrDst < (float)(radius * radius)) {
                     int coordX = centreX + x;
                     int coordY = centreY + y;
                     if (coordX >= 0 && coordX < size && coordY >= 0 && coordY < size) {
                        float weight = 1.0F - (float)Math.sqrt((double)sqrDst) / (float)radius;
                        weightSum += weight;
                        weights[addIndex] = weight;
                        xOffsets[addIndex] = x;
                        yOffsets[addIndex] = y;
                        addIndex++;
                     }
                  }
               }
            }
         }

         int numEntries = addIndex;
         this.erosionBrushIndices[i] = new int[addIndex];
         this.erosionBrushWeights[i] = new float[addIndex];

         for (int j = 0; j < numEntries; j++) {
            this.erosionBrushIndices[i][j] = (yOffsets[j] + centreY) * size + xOffsets[j] + centreX;
            this.erosionBrushWeights[i][j] = weights[j] / weightSum;
         }
      }
   }

   private void deposit(Cell cell, float amount) {
      if (!cell.erosionMask) {
         float change = this.modifier.modify(cell, amount);
         cell.height += change;
         cell.sediment += change;
      }
   }

   private void erode(Cell cell, float amount) {
      if (!cell.erosionMask) {
         float change = this.modifier.modify(cell, amount);
         cell.height -= change;
         cell.heightErosion -= change;
      }
   }

   public static IntFunction<Erosion> factory(GeneratorContext context) {
      return new Erosion.Factory(context.seed.root(), context.preset.filters(), context.levels);
   }

   private static class Factory implements IntFunction<Erosion> {
      private static final int SEED_OFFSET = 12768;
      private final int seed;
      private final Modifier modifier;
      private final FilterSettings.Erosion settings;

      private Factory(int seed, FilterSettings filters, Levels levels) {
         this.seed = seed + 12768;
         this.settings = filters.erosion.copy();
         this.modifier = Modifier.range(levels.ground, levels.ground(15));
      }

      public Erosion apply(int size) {
         return new Erosion(this.seed, size, this.settings, this.modifier);
      }
   }

   private static class TerrainPos {
      private float height;
      private float gradientX;
      private float gradientY;

      private Erosion.TerrainPos at(Cell[] nodes, int mapSize, float posX, float posY) {
         int coordX = (int)posX;
         int coordY = (int)posY;
         float x = posX - (float)coordX;
         float y = posY - (float)coordY;
         int nodeIndexNW = coordY * mapSize + coordX;
         float heightNW = nodes[nodeIndexNW].height;
         float heightNE = nodes[nodeIndexNW + 1].height;
         float heightSW = nodes[nodeIndexNW + mapSize].height;
         float heightSE = nodes[nodeIndexNW + mapSize + 1].height;
         this.gradientX = (heightNE - heightNW) * (1.0F - y) + (heightSE - heightSW) * y;
         this.gradientY = (heightSW - heightNW) * (1.0F - x) + (heightSE - heightNE) * x;
         this.height = heightNW * (1.0F - x) * (1.0F - y) + heightNE * x * (1.0F - y) + heightSW * (1.0F - x) * y + heightSE * x * y;
         return this;
      }

      private void reset() {
         this.height = 0.0F;
         this.gradientX = 0.0F;
         this.gradientY = 0.0F;
      }
   }
}
