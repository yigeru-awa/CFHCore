package com.onehumanawa.cfhcore.content.world.preset;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import com.onehumanawa.cfhcore.content.world.preset.settings.Preset;
import com.onehumanawa.cfhcore.content.world.preset.settings.TerrainSettings;
import com.onehumanawa.cfhcore.content.world.preset.settings.WorldSettings;

public class PresetTerrainProvider {
   public static DensityFunction createOffsetSpline(Preset preset, DensityFunction continent, DensityFunction erosion, DensityFunction ridge) {
      WorldSettings worldSettings = preset.world();
      WorldSettings.Properties properties = worldSettings.properties;
      WorldSettings.ControlPoints controlPoints = worldSettings.controlPoints;
      TerrainSettings terrainSettings = preset.terrain();
      return DensityFunctions.constant(0.0);
   }
}
