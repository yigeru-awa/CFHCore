package com.onehumanawa.cfhcore.content.registries;

import com.onehumanawa.cfhcore.content.burner.BurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;

import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * CFHCore's block registry. Assets (blockstate / models) are hand-written, so this class
 * only registers the block and lets the data-generation callbacks reference those files.
 */
public class CFHBlocks {

	private static final Registrate REGISTRATE = CFHContentRegistries.registrate();

	public static final BlockEntry<BurnerBlock> BURNER = REGISTRATE
			.block("burner", BurnerBlock::new)
			.properties(p -> p.mapColor(MapColor.METAL)
					.sound(SoundType.METAL)
					.strength(3.5F)
					.noOcclusion()
					// Same light curve as the blaze burner
					.lightLevel(state -> switch (state.getValue(BurnerBlock.HEAT_LEVEL)) {
						case NONE -> 0;
						case SMOULDERING -> 8;
						default -> 15;
					}))
			// BlockState is a hand-written multipart (base model per axis + fire overlay by
			// heat level), nothing to generate here.
			.blockstate((ctx, prov) -> {})
			// Default loot table = drop self (src/main/resources/data/cfhcore/loot_table/blocks/burner.json)
			.item()
			.model((ctx, prov) -> prov.getExistingFile(prov.modLoc("item/burner")))
			.build()
			.register();

	public static void register() {}

	private CFHBlocks() {}
}
