package com.onehumanawa.cfhcore.content.world.worldgen.cell.continent;

import com.onehumanawa.cfhcore.content.world.worldgen.cell.Cell;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.CellPopulator;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.rivermap.Rivermap;

public interface Continent extends CellPopulator {
   float getEdgeValue(float var1, float var2);

   default float getLandValue(float x, float z) {
      return this.getEdgeValue(x, z);
   }

   long getNearestCenter(float var1, float var2);

   Rivermap getRivermap(int var1, int var2);

   default Rivermap getRivermap(Cell cell) {
      return this.getRivermap(cell.continentX, cell.continentZ);
   }
}
