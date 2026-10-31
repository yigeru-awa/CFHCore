package com.onehumanawa.cfhcore.content.worldgen.data.preset;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.SurfaceRules;
import com.onehumanawa.cfhcore.CFHCore;
import com.onehumanawa.cfhcore.content.worldgen.data.preset.settings.Preset;
import com.onehumanawa.cfhcore.content.worldgen.tags.CFHBlockTags;
import com.onehumanawa.cfhcore.content.worldgen.noise.module.Noise;
import com.onehumanawa.cfhcore.content.worldgen.surface.rule.CFHSurfaceRules;
import com.onehumanawa.cfhcore.content.worldgen.surface.rule.StrataRule.Strata;

public class PresetSurfaceRuleData {
    
    public static SurfaceRules.RuleSource overworld(Preset preset, HolderGetter<DensityFunction> densityFunctions, HolderGetter<Noise> noise) {
		if (preset.miscellaneous().strataDecorator) {
			return SurfaceRules.sequence(SurfaceRuleData.overworld(), makeStrataRule(noise));
		}
		return SurfaceRules.sequence(SurfaceRuleData.overworld());
    }
    
	private static SurfaceRules.RuleSource makeStrataRule(HolderGetter<Noise> noise) {
		Holder<Noise> depth = noise.getOrThrow(PresetStrataNoise.STRATA_DEPTH);

		List<Strata> strata = new ArrayList<>();
		strata.add(new Strata(CFHBlockTags.SOIL, depth, 3, 0, 1, 0.1F, 0.25F));
		strata.add(new Strata(CFHBlockTags.SEDIMENT, depth, 3, 0, 2, 0.05F, 0.15F));
		strata.add(new Strata(CFHBlockTags.CLAY, depth, 3, 0, 2, 0.05F, 0.1F));
		strata.add(new Strata(CFHBlockTags.ROCK, depth, 3, 10, 30, 0.1F, 1.5F));
		return CFHSurfaceRules.strata(CFHCore.location("overworld_strata"), noise.getOrThrow(PresetStrataNoise.STRATA_SELECTOR), strata, 100);
	}
}
