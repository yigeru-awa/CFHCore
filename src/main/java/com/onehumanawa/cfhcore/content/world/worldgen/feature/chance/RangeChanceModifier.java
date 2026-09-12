package com.onehumanawa.cfhcore.content.world.worldgen.feature.chance;

import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public abstract class RangeChanceModifier implements ChanceModifier {
   protected float from;
   protected float to;
   protected boolean exclusive;

   public RangeChanceModifier(float from, float to, boolean exclusive) {
      this.from = from;
      this.to = to;
      this.exclusive = exclusive;
   }

   protected abstract float getValue(ChanceContext var1, FeaturePlaceContext<?> var2);

   @Override
   public float getChance(ChanceContext chanceCtx, FeaturePlaceContext<?> placeCtx) {
      return this.apply(this.getValue(chanceCtx, placeCtx));
   }

   private float apply(float value) {
      float max = this.exclusive ? 0.0F : 1.0F;
      float range = Math.abs(max - this.from);
      if (this.from < this.to) {
         if (value <= this.from) {
            return 0.0F;
         } else {
            return value >= this.to ? max : (value - this.from) / range;
         }
      } else if (this.from > this.to) {
         if (value <= this.to) {
            return max;
         } else {
            return value >= this.from ? 0.0F : 1.0F - (value - this.to) / range;
         }
      } else {
         return 0.0F;
      }
   }
}
