package com.onehumanawa.cfhcore.content.worldgen.data.tags;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import com.onehumanawa.cfhcore.content.worldgen.data.preset.settings.MiscellaneousSettings;
import com.onehumanawa.cfhcore.content.worldgen.data.preset.settings.Preset;
import com.onehumanawa.cfhcore.content.worldgen.tags.CFHBlockTags;

public class CFHBlockTagsProvider extends IntrinsicHolderTagsProvider<Block> {
	private Preset preset;
	
	public CFHBlockTagsProvider(Preset preset, PackOutput packOutput, CompletableFuture<Provider> completableFuture) {
		super(packOutput, Registries.BLOCK, completableFuture, (block) -> block.builtInRegistryHolder().key());

		this.preset = preset;
	}

	@Override
	protected void addTags(HolderLookup.Provider provider) {
//		MiscellaneousSettings miscellaneousSettings = this.preset.miscellaneous();

		this.tag(CFHBlockTags.SOIL).add(Blocks.DIRT, Blocks.COARSE_DIRT);
		this.tag(CFHBlockTags.CLAY).add(Blocks.CLAY);
		this.tag(CFHBlockTags.SEDIMENT).add(Blocks.SAND, Blocks.GRAVEL);
		this.tag(CFHBlockTags.ERODIBLE).add(Blocks.SNOW_BLOCK).add(Blocks.POWDER_SNOW).add(Blocks.GRAVEL).addOptionalTag(BlockTags.DIRT.location());
		
//		if(!miscellaneousSettings.oreCompatibleStoneOnly) {
			this.tag(CFHBlockTags.ROCK).add(Blocks.GRANITE, Blocks.ANDESITE, Blocks.STONE, Blocks.DIORITE);
//		} else{
			//TODO
//		}
	}
}