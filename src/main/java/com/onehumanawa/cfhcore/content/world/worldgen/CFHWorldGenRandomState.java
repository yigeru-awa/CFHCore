package com.onehumanawa.cfhcore.content.world.worldgen;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.jetbrains.annotations.Nullable;
import com.onehumanawa.cfhcore.content.world.preset.settings.Preset;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noise;

public interface CFHWorldGenRandomState {
   void initialize(RegistryAccess var1);

   @Nullable
   Preset preset();

   @Nullable
   GeneratorContext generatorContext();

   DensityFunction wrap(DensityFunction var1);

   Noise seed(Noise var1);
}
