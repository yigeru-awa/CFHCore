package com.onehumanawa.cfhcore.content.worldgen.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import com.onehumanawa.cfhcore.CFHCore;

public final class CFHBlockTags {
	public static final TagKey<Block> SOIL = resolve("soil");
	public static final TagKey<Block> ROCK = resolve("rock");
	public static final TagKey<Block> CLAY = resolve("clay");
	public static final TagKey<Block> SEDIMENT = resolve("sediment");
	public static final TagKey<Block> ERODIBLE = resolve("erodible");
	
    private static TagKey<Block> resolve(String path) {
    	return TagKey.create(Registries.BLOCK, CFHCore.location(path));
    }
}
