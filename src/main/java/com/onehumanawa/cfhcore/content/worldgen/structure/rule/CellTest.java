package com.onehumanawa.cfhcore.content.worldgen.structure.rule;

import java.util.List;
import java.util.Set;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.RandomState;
import com.onehumanawa.cfhcore.content.worldgen.GeneratorContext;
import com.onehumanawa.cfhcore.content.worldgen.CFHRandomState;
import com.onehumanawa.cfhcore.content.worldgen.cell.Cell;
import com.onehumanawa.cfhcore.content.worldgen.cell.heightmap.WorldLookup;
import com.onehumanawa.cfhcore.content.worldgen.cell.terrain.Terrain;
import com.onehumanawa.cfhcore.content.worldgen.cell.terrain.TerrainType;

record CellTest(float cutoff, Set<Terrain> terrainTypeBlacklist) implements StructureRule {
	public static final MapCodec<CellTest> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Codec.FLOAT.fieldOf("cutoff").forGetter(CellTest::cutoff),
		Codec.STRING.xmap(TerrainType::get, Terrain::getName).listOf().fieldOf("terrain_type_blacklist").forGetter((set) -> set.terrainTypeBlacklist().stream().toList())
	).apply(instance, CellTest::new));

	public CellTest(float cutoff, List<Terrain> terrainTypeBlacklist) {
		this(cutoff, ImmutableSet.copyOf(terrainTypeBlacklist));
	}
	
	@Override
	public boolean test(RandomState randomState, BlockPos pos) {
		if((Object) randomState instanceof CFHRandomState rtfRandomState) {
			@Nullable
			GeneratorContext generatorContext = rtfRandomState.generatorContext();
			if(generatorContext != null) {
				WorldLookup worldLookup = generatorContext.lookup;
				Cell cell = new Cell();
				worldLookup.applyCell(cell.reset(), pos.getX(), pos.getZ(), false);
				if(cell.riverMask < this.cutoff) {//FIXME this breaks ancient city generation || this.terrainTypeBlacklist.contains(cell.terrain)) {
					return false;
				}
			}
			return true;
		} else {
			throw new IllegalStateException();
		}
	}

	@Override
	public MapCodec<CellTest> codec() {
		return CODEC;
	}
}
