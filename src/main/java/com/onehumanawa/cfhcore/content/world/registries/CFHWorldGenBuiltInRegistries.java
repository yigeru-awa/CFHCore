package com.onehumanawa.cfhcore.content.world.registries;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import com.onehumanawa.cfhcore.content.world.platform.RegistryUtil;
import com.onehumanawa.cfhcore.content.world.worldgen.biome.modifier.BiomeModifier;
import com.onehumanawa.cfhcore.content.world.worldgen.feature.chance.ChanceModifier;
import com.onehumanawa.cfhcore.content.world.worldgen.feature.template.decorator.TemplateDecorator;
import com.onehumanawa.cfhcore.content.world.worldgen.feature.template.placement.TemplatePlacement;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.domain.Domain;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.function.CurveFunction;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noise;
import com.onehumanawa.cfhcore.content.world.worldgen.structure.rule.StructureRule;

public class CFHWorldGenBuiltInRegistries {
   public static final Registry<MapCodec<? extends Noise>> NOISE_TYPE = RegistryUtil.createRegistry(CFHWorldGenRegistries.NOISE_TYPE);
   public static final Registry<MapCodec<? extends Domain>> DOMAIN_TYPE = RegistryUtil.createRegistry(CFHWorldGenRegistries.DOMAIN_TYPE);
   public static final Registry<MapCodec<? extends CurveFunction>> CURVE_FUNCTION_TYPE = RegistryUtil.createRegistry(CFHWorldGenRegistries.CURVE_FUNCTION_TYPE);
   public static final Registry<MapCodec<? extends ChanceModifier>> CHANCE_MODIFIER_TYPE = RegistryUtil.createRegistry(CFHWorldGenRegistries.CHANCE_MODIFIER_TYPE);
   public static final Registry<MapCodec<? extends TemplatePlacement<?>>> TEMPLATE_PLACEMENT_TYPE = RegistryUtil.createRegistry(
      CFHWorldGenRegistries.TEMPLATE_PLACEMENT_TYPE
   );
   public static final Registry<MapCodec<? extends TemplateDecorator<?>>> TEMPLATE_DECORATOR_TYPE = RegistryUtil.createRegistry(
      CFHWorldGenRegistries.TEMPLATE_DECORATOR_TYPE
   );
   public static final Registry<MapCodec<? extends BiomeModifier>> BIOME_MODIFIER_TYPE = RegistryUtil.createRegistry(CFHWorldGenRegistries.BIOME_MODIFIER_TYPE);
   public static final Registry<MapCodec<? extends StructureRule>> STRUCTURE_RULE_TYPE = RegistryUtil.createRegistry(CFHWorldGenRegistries.STRUCTURE_RULE_TYPE);

   public static void bootstrap() {
   }
}
