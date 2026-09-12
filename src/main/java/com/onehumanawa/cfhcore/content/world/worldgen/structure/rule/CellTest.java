package com.onehumanawa.cfhcore.content.world.worldgen.structure.rule;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.RandomState;
import com.onehumanawa.cfhcore.content.world.worldgen.GeneratorContext;
import com.onehumanawa.cfhcore.content.world.worldgen.CFHWorldGenRandomState;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.Cell;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.heightmap.WorldLookup;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.terrain.Terrain;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.terrain.TerrainType;

record CellTest(float cutoff, Set<Terrain> terrainTypeBlacklist) implements StructureRule {
   public static final MapCodec<CellTest> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
               Codec.FLOAT.fieldOf("cutoff").forGetter(CellTest::cutoff),
               Codec.STRING
                  .<Terrain>xmap(TerrainType::get, Terrain::getName)
                  .listOf()
                  .fieldOf("terrain_type_blacklist")
                  .forGetter(set -> set.terrainTypeBlacklist().stream().toList())
            )
            .apply(instance, CellTest::new)
   );

   public CellTest(float cutoff, List<Terrain> terrainTypeBlacklist) {
      this(cutoff, ImmutableSet.copyOf(terrainTypeBlacklist));
   }

   @Override
   public boolean test(RandomState randomState, BlockPos pos) {
      if (randomState instanceof CFHWorldGenRandomState cfhWorldGenRandomState) {
         GeneratorContext generatorContext = cfhWorldGenRandomState.generatorContext();
         if (generatorContext != null) {
            WorldLookup worldLookup = generatorContext.lookup;
            Cell cell = new Cell();
            worldLookup.applyCell(cell.reset(), pos.getX(), pos.getZ(), false);
            if (cell.riverMask < this.cutoff) {
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
