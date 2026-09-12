package com.onehumanawa.cfhcore.content.world.neoforge.mixin;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.BiomeGenerationSettings.PlainBuilder;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({PlainBuilder.class})
public interface MixinBiomeGenerationSettingsPlainsBuilder {
   @Accessor("features")
   List<List<Holder<PlacedFeature>>> getFeatures();
}
