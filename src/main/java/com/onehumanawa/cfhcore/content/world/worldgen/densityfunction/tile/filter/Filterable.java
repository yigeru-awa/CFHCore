package com.onehumanawa.cfhcore.content.world.worldgen.densityfunction.tile.filter;

import com.onehumanawa.cfhcore.content.world.worldgen.cell.Cell;
import com.onehumanawa.cfhcore.content.world.worldgen.densityfunction.tile.Size;

public interface Filterable {
   int getBlockX();

   int getBlockZ();

   Size getBlockSize();

   Cell[] getBacking();

   Cell getCellRaw(int var1, int var2);
}
