package com.onehumanawa.cfhcore.content.worldgen.data.tags;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.level.levelgen.DensityFunction;
import com.onehumanawa.cfhcore.content.worldgen.data.preset.PresetNoiseRouterData;
import com.onehumanawa.cfhcore.content.worldgen.tags.CFHDensityFunctionTags;

public class CFHDensityFunctionTagsProvider extends TagsProvider<DensityFunction> {

	public CFHDensityFunctionTagsProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> completableFuture) {
		super(packOutput, Registries.DENSITY_FUNCTION, completableFuture);
	}

	@Override
	protected void addTags(HolderLookup.Provider provider) {
		this.tag(CFHDensityFunctionTags.ADDITIONAL_NOISE_ROUTER_FUNCTIONS).add(PresetNoiseRouterData.GRADIENT, PresetNoiseRouterData.HEIGHT_EROSION, PresetNoiseRouterData.SEDIMENT);
	}
}
