package com.onehumanawa.cfhcore.content.world.worldgen.util;

import com.onehumanawa.cfhcore.content.world.worldgen.noise.NoiseUtil;

public record Scaling(int worldHeight, float unit, int waterY, int groundY, int groundLevel, int waterLevel, float ground, float water, float elevationRange) {
   public int scale(float value) {
      return (int)(value * (float)this.worldHeight);
   }

   public float elevation(float value) {
      return value <= this.water ? 0.0F : (value - this.water) / this.elevationRange;
   }

   public float elevation(int y) {
      return y <= this.waterY ? 0.0F : (float)this.scale((float)(y - this.waterY)) / this.elevationRange;
   }

   public float blocks(int level) {
      return NoiseUtil.div(level, this.worldHeight);
   }

   public float water(int amount) {
      return NoiseUtil.div(this.waterY + amount, this.worldHeight);
   }

   public float ground(int amount) {
      return NoiseUtil.div(this.groundY + amount, this.worldHeight);
   }

   public static Scaling make(int height, int seaLevel) {
      int worldHeight = Math.max(1, height);
      float unit = NoiseUtil.div(1, worldHeight);
      int groundLevel = seaLevel + 1;
      int waterY = Math.min(seaLevel - 1, worldHeight);
      int groundY = Math.min(groundLevel - 1, worldHeight);
      float ground = NoiseUtil.div(groundY, worldHeight);
      float water = NoiseUtil.div(waterY, worldHeight);
      float elevationRange = 1.0F - water;
      return new Scaling(worldHeight, unit, waterY, groundY, groundLevel, seaLevel, ground, water, elevationRange);
   }
}
