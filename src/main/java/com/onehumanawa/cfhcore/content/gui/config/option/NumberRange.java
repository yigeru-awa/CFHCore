package com.onehumanawa.cfhcore.content.gui.config.option;

public record NumberRange(double min, double max, double step) {
   public static NumberRange of(double min, double max, double step) {
      return new NumberRange(min, max, step);
   }

   public static NumberRange ints(int min, int max) {
      return new NumberRange((double)min, (double)max, (double)1.0F);
   }

   public static NumberRange unit() {
      return new NumberRange((double)0.0F, (double)1.0F, 0.01);
   }

   public double effectiveStep() {
      if (this.step > (double)0.0F) {
         return this.step;
      } else {
         double span = this.max - this.min;
         return span <= (double)0.0F ? (double)1.0F : span / (double)100.0F;
      }
   }

   public boolean isBounded() {
      return Double.isFinite(this.min) && Double.isFinite(this.max) && this.max > this.min && this.max - this.min < (double)1.0E9F;
   }

   public double clamp(double v) {
      return Math.max(this.min, Math.min(this.max, v));
   }
}
