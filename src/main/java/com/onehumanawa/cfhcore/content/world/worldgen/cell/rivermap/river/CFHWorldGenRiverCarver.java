package com.onehumanawa.cfhcore.content.world.worldgen.cell.rivermap.river;

import com.onehumanawa.cfhcore.content.world.worldgen.cell.Cell;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.heightmap.Levels;

public interface CFHWorldGenRiverCarver {
   boolean isMain();

   River getRiver();

   RiverWarp getWarp();

   RiverConfig getConfig();

   void carve(Cell var1, float var2, float var3, float var4, float var5, float var6, float var7);

   RiverConfig createForkConfig(float var1, Levels var2);
}
