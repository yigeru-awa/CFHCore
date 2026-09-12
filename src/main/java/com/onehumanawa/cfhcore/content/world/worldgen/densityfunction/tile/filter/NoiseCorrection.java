package com.onehumanawa.cfhcore.content.world.worldgen.densityfunction.tile.filter;

import net.minecraft.core.QuartPos;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.Cell;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.heightmap.Levels;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.terrain.TerrainCategory;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.terrain.TerrainType;
import com.onehumanawa.cfhcore.content.world.worldgen.densityfunction.tile.Size;

public record NoiseCorrection(Levels levels) implements Filter {
   @Override
   public void apply(Filterable map, int seedX, int seedZ, int iterations) {
      Size size = map.getBlockSize();
      int total = size.total();
      int quartTotal = QuartPos.fromBlock(total);

      for (int quartX = 0; quartX < quartTotal; quartX++) {
         for (int quartZ = 0; quartZ < quartTotal; quartZ++) {
            int startX = QuartPos.toBlock(quartX);
            int startZ = QuartPos.toBlock(quartZ);
            int endX = QuartPos.toBlock(quartX + 1);
            int endZ = QuartPos.toBlock(quartZ + 1);
            boolean isBeach = false;

            label51:
            for (int x = startX; x < endX; x++) {
               for (int z = startZ; z < endZ; z++) {
                  Cell cell = map.getCellRaw(x, z);
                  if (cell.terrain.getDelegate() == TerrainCategory.BEACH
                     || (cell.terrain.isShallowOcean() || cell.terrain.isDeepOcean()) && cell.height > this.levels.water) {
                     isBeach = true;
                     break label51;
                  }
               }
            }

            if (isBeach) {
               for (int x = startX; x < endX; x++) {
                  for (int zx = startZ; zx < endZ; zx++) {
                     map.getCellRaw(x, zx).terrain = TerrainType.BEACH;
                  }
               }
            }
         }
      }
   }
}
