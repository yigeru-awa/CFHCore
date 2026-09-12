package com.onehumanawa.cfhcore.content.world.preset;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceRules.RuleSource;
import com.onehumanawa.cfhcore.content.world.CFHWorldGenCommon;
import com.onehumanawa.cfhcore.content.world.preset.settings.Preset;
import com.onehumanawa.cfhcore.content.world.tags.CFHWorldGenBlockTags;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noise;
import com.onehumanawa.cfhcore.content.world.worldgen.surface.rule.CFHWorldGenSurfaceRules;
import com.onehumanawa.cfhcore.content.world.worldgen.surface.rule.StrataRule;

public class PresetSurfaceRuleData {
   public static RuleSource overworld(Preset preset, HolderGetter<DensityFunction> densityFunctions, HolderGetter<Noise> noise) {
      return preset.miscellaneous().strataDecorator
         ? SurfaceRules.sequence(SurfaceRuleData.overworld(), makeStrataRule(noise))
         : SurfaceRules.sequence(SurfaceRuleData.overworld());
   }

   private static RuleSource makeStrataRule(HolderGetter<Noise> noise) {
      Holder<Noise> depth = noise.getOrThrow(PresetStrataNoise.STRATA_DEPTH);
      List<StrataRule.Strata> strata = new ArrayList<>();
      strata.add(new StrataRule.Strata(CFHWorldGenBlockTags.SOIL, depth, 3, 0, 1, 0.1F, 0.25F));
      strata.add(new StrataRule.Strata(CFHWorldGenBlockTags.SEDIMENT, depth, 3, 0, 2, 0.05F, 0.15F));
      strata.add(new StrataRule.Strata(CFHWorldGenBlockTags.CLAY, depth, 3, 0, 2, 0.05F, 0.1F));
      strata.add(new StrataRule.Strata(CFHWorldGenBlockTags.ROCK, depth, 3, 10, 30, 0.1F, 1.5F));
      return CFHWorldGenSurfaceRules.strata(CFHWorldGenCommon.location("overworld_strata"), noise.getOrThrow(PresetStrataNoise.STRATA_SELECTOR), strata, 100);
   }
}
