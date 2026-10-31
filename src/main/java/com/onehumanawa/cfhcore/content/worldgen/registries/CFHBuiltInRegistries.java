package com.onehumanawa.cfhcore.content.worldgen.registries;

import com.mojang.serialization.Codec;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import com.onehumanawa.cfhcore.content.worldgen.platform.RegistryUtil;
import com.onehumanawa.cfhcore.content.worldgen.biome.modifier.BiomeModifier;
import com.onehumanawa.cfhcore.content.worldgen.feature.chance.ChanceModifier;
import com.onehumanawa.cfhcore.content.worldgen.feature.template.decorator.TemplateDecorator;
import com.onehumanawa.cfhcore.content.worldgen.feature.template.placement.TemplatePlacement;
import com.onehumanawa.cfhcore.content.worldgen.noise.domain.Domain;
import com.onehumanawa.cfhcore.content.worldgen.noise.function.CurveFunction;
import com.onehumanawa.cfhcore.content.worldgen.noise.module.Noise;
import com.onehumanawa.cfhcore.content.worldgen.structure.rule.StructureRule;

public class CFHBuiltInRegistries {
	public static final Registry<MapCodec<? extends Noise>> NOISE_TYPE = RegistryUtil.createRegistry(CFHRegistries.NOISE_TYPE);
	public static final Registry<MapCodec<? extends Domain>> DOMAIN_TYPE = RegistryUtil.createRegistry(CFHRegistries.DOMAIN_TYPE);
	public static final Registry<MapCodec<? extends CurveFunction>> CURVE_FUNCTION_TYPE = RegistryUtil.createRegistry(CFHRegistries.CURVE_FUNCTION_TYPE);
	public static final Registry<MapCodec<? extends ChanceModifier>> CHANCE_MODIFIER_TYPE = RegistryUtil.createRegistry(CFHRegistries.CHANCE_MODIFIER_TYPE);
	public static final Registry<MapCodec<? extends TemplatePlacement<?>>> TEMPLATE_PLACEMENT_TYPE = RegistryUtil.createRegistry(CFHRegistries.TEMPLATE_PLACEMENT_TYPE);
	public static final Registry<MapCodec<? extends TemplateDecorator<?>>> TEMPLATE_DECORATOR_TYPE = RegistryUtil.createRegistry(CFHRegistries.TEMPLATE_DECORATOR_TYPE);
	public static final Registry<MapCodec<? extends BiomeModifier>> BIOME_MODIFIER_TYPE = RegistryUtil.createRegistry(CFHRegistries.BIOME_MODIFIER_TYPE);
	public static final Registry<MapCodec<? extends StructureRule>> STRUCTURE_RULE_TYPE = RegistryUtil.createRegistry(CFHRegistries.STRUCTURE_RULE_TYPE);

	public static void bootstrap() {
	}
}
