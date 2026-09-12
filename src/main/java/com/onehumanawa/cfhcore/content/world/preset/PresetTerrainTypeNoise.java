package com.onehumanawa.cfhcore.content.world.preset;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import com.onehumanawa.cfhcore.content.world.preset.settings.Preset;
import com.onehumanawa.cfhcore.content.world.preset.settings.WorldSettings;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noise;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noises;

public class PresetTerrainTypeNoise {
   public static final ResourceKey<Noise> GROUND = PresetTerrainNoise.createKey("ground");

   public static void bootstrap(Preset preset, BootstrapContext<Noise> ctx) {
      WorldSettings worldSettings = preset.world();
      WorldSettings.Properties properties = worldSettings.properties;
      float seaLevel = (float)properties.seaLevel;
      int terrainScaler = properties.terrainScaler();
      ctx.register(GROUND, Noises.constant(seaLevel / (float)terrainScaler));
   }
}
