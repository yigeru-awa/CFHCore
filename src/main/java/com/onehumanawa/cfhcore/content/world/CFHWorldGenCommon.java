package com.onehumanawa.cfhcore.content.world;

import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.onehumanawa.cfhcore.CFHCore;
import com.onehumanawa.cfhcore.content.world.platform.RegistryUtil;
import com.onehumanawa.cfhcore.content.world.registries.CFHWorldGenBuiltInRegistries;
import com.onehumanawa.cfhcore.content.world.registries.CFHWorldGenRegistries;
import com.onehumanawa.cfhcore.content.world.worldgen.biome.modifier.BiomeModifiers;
import com.onehumanawa.cfhcore.content.world.worldgen.densityfunction.CFHWorldGenDensityFunctions;
import com.onehumanawa.cfhcore.content.world.worldgen.feature.CFHWorldGenFeatures;
import com.onehumanawa.cfhcore.content.world.worldgen.feature.chance.CFHWorldGenChanceModifiers;
import com.onehumanawa.cfhcore.content.world.worldgen.feature.placement.CFHWorldGenPlacementModifiers;
import com.onehumanawa.cfhcore.content.world.worldgen.feature.template.decorator.TemplateDecorators;
import com.onehumanawa.cfhcore.content.world.worldgen.feature.template.placement.TemplatePlacements;
import com.onehumanawa.cfhcore.content.world.worldgen.floatproviders.CFHWorldGenFloatProviderTypes;
import com.onehumanawa.cfhcore.content.world.worldgen.heightproviders.CFHWorldGenHeightProviderTypes;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.domain.Domains;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.function.CurveFunctions;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noise;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noises;
import com.onehumanawa.cfhcore.content.world.worldgen.structure.rule.StructureRule;
import com.onehumanawa.cfhcore.content.world.worldgen.structure.rule.StructureRules;
import com.onehumanawa.cfhcore.content.world.worldgen.surface.rule.CFHWorldGenSurfaceRules;

public class CFHWorldGenCommon {
   public static final String MOD_ID = CFHCore.ID;
   public static final Logger LOGGER = LogManager.getLogger("CFHWorldGen");

   /**
    * Tile size used by the cached generator context. Replaces CFHWorldGen's
    * {@code performance_internal.conf}, which is not exposed in CFHCore.
    */
   public static final int TILE_SIZE = 3;
   public static final int BATCH_COUNT = 6;

   public static void bootstrap() {
      CFHWorldGenBuiltInRegistries.bootstrap();
      TemplatePlacements.bootstrap();
      TemplateDecorators.bootstrap();
      CFHWorldGenChanceModifiers.bootstrap();
      CFHWorldGenPlacementModifiers.bootstrap();
      CFHWorldGenDensityFunctions.bootstrap();
      Noises.bootstrap();
      Domains.bootstrap();
      CurveFunctions.bootstrap();
      CFHWorldGenFeatures.bootstrap();
      CFHWorldGenHeightProviderTypes.bootstrap();
      CFHWorldGenFloatProviderTypes.bootstrap();
      BiomeModifiers.bootstrap();
      CFHWorldGenSurfaceRules.bootstrap();
      StructureRules.bootstrap();
      RegistryUtil.createDataRegistry(CFHWorldGenRegistries.NOISE, Noise.DIRECT_CODEC, false);
      RegistryUtil.createDataRegistry(CFHWorldGenRegistries.STRUCTURE_RULE, StructureRule.DIRECT_CODEC, false);
   }

   public static ResourceLocation location(String name) {
      return name.contains(":") ? ResourceLocation.parse(name) : ResourceLocation.fromNamespaceAndPath(MOD_ID, name);
   }
}
