package com.onehumanawa.cfhcore.content.world.worldgen.cell.rivermap.lake;

import com.onehumanawa.cfhcore.content.world.preset.settings.RiverSettings;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.heightmap.Levels;

public class LakeConfig {
   public float depth;
   public float chance;
   public float sizeMin;
   public float sizeMax;
   public float sizeRange;
   public float bankMin;
   public float bankMax;
   public float distanceMin;
   public float distanceMax;
   public float oceanLevel;

   private LakeConfig(LakeConfig.Builder builder) {
      this.depth = builder.depth;
      this.chance = builder.chance;
      this.sizeMin = builder.sizeMin;
      this.sizeMax = builder.sizeMax;
      this.sizeRange = this.sizeMax - this.sizeMin;
      this.bankMin = builder.bankMin;
      this.bankMax = builder.bankMax;
      this.distanceMin = builder.distanceMin;
      this.distanceMax = builder.distanceMax;
      this.oceanLevel = builder.oceanLevel;
   }

   public static LakeConfig of(RiverSettings.Lake settings, Levels levels) {
      LakeConfig.Builder builder = new LakeConfig.Builder();
      builder.chance = settings.chance;
      builder.sizeMin = (float)settings.sizeMin;
      builder.sizeMax = (float)settings.sizeMax;
      builder.depth = levels.water(-settings.depth);
      builder.distanceMin = settings.minStartDistance;
      builder.distanceMax = settings.maxStartDistance;
      builder.bankMin = levels.water(settings.minBankHeight);
      builder.bankMax = levels.water(settings.maxBankHeight);
      builder.oceanLevel = levels.water;
      return new LakeConfig(builder);
   }

   public static class Builder {
      public float chance;
      public float depth = 10.0F;
      public float sizeMin = 30.0F;
      public float sizeMax = 100.0F;
      public float bankMin = 1.0F;
      public float bankMax = 8.0F;
      public float distanceMin = 0.025F;
      public float distanceMax = 0.05F;
      public float oceanLevel = 0.25F;
   }
}
