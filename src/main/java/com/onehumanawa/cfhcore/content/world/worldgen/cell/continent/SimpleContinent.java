package com.onehumanawa.cfhcore.content.world.worldgen.cell.continent;

public interface SimpleContinent extends Continent {
   @Override
   float getEdgeValue(float var1, float var2);

   default float getDistanceToEdge(int cx, int cz, float dx, float dy) {
      return 1.0F;
   }

   default float getDistanceToOcean(int cx, int cz, float dx, float dy) {
      return 1.0F;
   }
}
