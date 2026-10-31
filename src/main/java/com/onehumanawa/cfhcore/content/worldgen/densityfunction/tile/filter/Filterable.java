package com.onehumanawa.cfhcore.content.worldgen.densityfunction.tile.filter;

import com.onehumanawa.cfhcore.content.worldgen.cell.Cell;
import com.onehumanawa.cfhcore.content.worldgen.densityfunction.tile.Size;

public interface Filterable {
    int getBlockX();
    
    int getBlockZ();
    
    Size getBlockSize();
    
    Cell[] getBacking();
    
    Cell getCellRaw(int x, int z);
}
