package com.onehumanawa.cfhcore.content.world.worldgen.util;

public record Boundsf(float minX, float minY, float maxX, float maxY) {
   public static final Boundsf NONE = new Boundsf(1.0F, 1.0F, -1.0F, -1.0F);

   public boolean contains(float x, float y) {
      return x >= this.minX && x <= this.maxX && y >= this.minY && y <= this.maxY;
   }

   public static Boundsf.Builder builder() {
      return new Boundsf.Builder();
   }

   public static class Builder {
      private float minX = Float.MAX_VALUE;
      private float minY = Float.MAX_VALUE;
      private float maxX = Float.MIN_VALUE;
      private float maxY = Float.MIN_VALUE;

      public void record(float x, float y) {
         this.minX = Math.min(this.minX, x);
         this.minY = Math.min(this.minY, y);
         this.maxX = Math.max(this.maxX, x);
         this.maxY = Math.max(this.maxY, y);
      }

      public Boundsf build() {
         return new Boundsf(this.minX, this.minY, this.maxX, this.maxY);
      }
   }
}
