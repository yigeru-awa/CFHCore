package com.onehumanawa.cfhcore.content.world.preset;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import com.onehumanawa.cfhcore.content.world.CFHWorldGenCommon;
import com.onehumanawa.cfhcore.content.world.preset.settings.Preset;
import com.onehumanawa.cfhcore.content.world.registries.CFHWorldGenRegistries;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.terrain.TerrainType;
import com.onehumanawa.cfhcore.content.world.worldgen.structure.rule.StructureRule;
import com.onehumanawa.cfhcore.content.world.worldgen.structure.rule.StructureRules;

public class PresetStructureRuleData {
   public static final ResourceKey<StructureRule> CELL_TEST = createKey("cell_test");

   public static void bootstrap(Preset preset, BootstrapContext<StructureRule> ctx) {
      ctx.register(
         CELL_TEST, StructureRules.cellTest(0.225F, TerrainType.MOUNTAIN_CHAIN, TerrainType.MOUNTAINS_1, TerrainType.MOUNTAINS_2, TerrainType.MOUNTAINS_3)
      );
   }

   private static ResourceKey<StructureRule> createKey(String name) {
      return ResourceKey.create(CFHWorldGenRegistries.STRUCTURE_RULE, CFHWorldGenCommon.location(name));
   }
}
