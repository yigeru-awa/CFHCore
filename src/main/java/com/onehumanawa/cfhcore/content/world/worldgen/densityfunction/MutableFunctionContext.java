package com.onehumanawa.cfhcore.content.world.worldgen.densityfunction;

import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;

public class MutableFunctionContext implements FunctionContext {
   private int blockX;
   private int blockY;
   private int blockZ;

   public MutableFunctionContext at(int x, int y, int z) {
      this.blockX = x;
      this.blockY = y;
      this.blockZ = z;
      return this;
   }

   @Override
   public int blockX() {
      return this.blockX;
   }

   @Override
   public int blockY() {
      return this.blockY;
   }

   @Override
   public int blockZ() {
      return this.blockZ;
   }
}
