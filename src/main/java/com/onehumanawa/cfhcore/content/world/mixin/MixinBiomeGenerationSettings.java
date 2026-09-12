package com.onehumanawa.cfhcore.content.world.mixin;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Deprecated
@Mixin({BiomeGenerationSettings.class})
public interface MixinBiomeGenerationSettings {
   @Accessor("features")
   List<HolderSet<PlacedFeature>> getFeatures();

   @Accessor("flowerFeatures")
   void setFlowerFeatures(Supplier<List<ConfiguredFeature<?, ?>>> var1);
}
