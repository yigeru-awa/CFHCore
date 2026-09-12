package com.onehumanawa.cfhcore.content.world.worldgen.cell.heightmap;

import com.onehumanawa.cfhcore.content.world.worldgen.noise.NoiseUtil;

public class Levels {
   public int worldHeight;
   public float unit;
   public int waterY;
   private int groundY;
   public int groundLevel;
   public int waterLevel;
   public float ground;
   public float water;
   private float elevationRange;

   public Levels(int height, int seaLevel) {
      this.worldHeight = Math.max(1, height);
      this.unit = NoiseUtil.div(1, this.worldHeight);
      this.waterLevel = seaLevel;
      this.groundLevel = this.waterLevel + 1;
      this.waterY = Math.min(this.waterLevel - 1, this.worldHeight);
      this.groundY = Math.min(this.groundLevel - 1, this.worldHeight);
      this.ground = NoiseUtil.div(this.groundY, this.worldHeight);
      this.water = NoiseUtil.div(this.waterY, this.worldHeight);
      this.elevationRange = 1.0F - this.water;
   }

   public int scale(float value) {
      return (int)(value * (float)this.worldHeight);
   }

   public float elevation(float value) {
      return value <= this.water ? 0.0F : (value - this.water) / this.elevationRange;
   }

   public float elevation(int y) {
      return y <= this.waterY ? 0.0F : this.scale(y - this.waterY) / this.elevationRange;
   }

   public float scale(int level) {
      return NoiseUtil.div(level, this.worldHeight);
   }

   public float water(int amount) {
      return NoiseUtil.div(this.waterY + amount, this.worldHeight);
   }

   public float ground(int amount) {
      return NoiseUtil.div(this.groundY + amount, this.worldHeight);
   }
}
