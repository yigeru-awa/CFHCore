package com.onehumanawa.cfhcore.content.world.worldgen.cell.rivermap.wetland;

import com.onehumanawa.cfhcore.content.world.preset.settings.RiverSettings;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.NoiseUtil;
import com.onehumanawa.cfhcore.content.world.worldgen.util.Variance;

public class WetlandConfig {
   public int skipSize;
   public Variance length;
   public Variance width;

   public WetlandConfig(RiverSettings.Wetland settings) {
      this.skipSize = Math.max(1, NoiseUtil.round((1.0F - settings.chance) * 10.0F));
      this.length = Variance.of((float)settings.sizeMin, (float)settings.sizeMax);
      this.width = Variance.of(50.0F, 150.0F);
   }
}
