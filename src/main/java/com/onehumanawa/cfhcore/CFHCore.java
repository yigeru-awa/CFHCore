package com.onehumanawa.cfhcore;

import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.registries.RegistriesDatapackGenerator;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import com.onehumanawa.cfhcore.content.gui.client.CFHClient;
import com.onehumanawa.cfhcore.content.gui.core.neoforge.CFHGuiNeoForge;
import com.onehumanawa.cfhcore.content.worldgen.biome.modifier.BiomeModifiers;
import com.onehumanawa.cfhcore.content.worldgen.biome.modifier.AddModifier;
import com.onehumanawa.cfhcore.content.worldgen.biome.modifier.ReplaceModifier;
import com.onehumanawa.cfhcore.content.worldgen.data.preset.settings.Preset;
import com.onehumanawa.cfhcore.content.worldgen.data.preset.settings.Presets;
import com.onehumanawa.cfhcore.content.worldgen.data.tags.CFHBlockTagsProvider;
import com.onehumanawa.cfhcore.content.worldgen.data.tags.CFHDensityFunctionTagsProvider;
import com.onehumanawa.cfhcore.content.worldgen.densityfunction.CFHDensityFunctions;
import com.onehumanawa.cfhcore.content.worldgen.feature.CFHFeatures;
import com.onehumanawa.cfhcore.content.worldgen.feature.chance.CFHChanceModifiers;
import com.onehumanawa.cfhcore.content.worldgen.feature.placement.CFHPlacementModifiers;
import com.onehumanawa.cfhcore.content.worldgen.feature.template.decorator.TemplateDecorators;
import com.onehumanawa.cfhcore.content.worldgen.feature.template.placement.TemplatePlacements;
import com.onehumanawa.cfhcore.content.worldgen.floatproviders.CFHFloatProviderTypes;
import com.onehumanawa.cfhcore.content.worldgen.heightproviders.CFHHeightProviderTypes;
import com.onehumanawa.cfhcore.content.worldgen.noise.domain.Domains;
import com.onehumanawa.cfhcore.content.worldgen.noise.function.CurveFunctions;
import com.onehumanawa.cfhcore.content.worldgen.noise.module.Noise;
import com.onehumanawa.cfhcore.content.worldgen.noise.module.Noises;
import com.onehumanawa.cfhcore.content.worldgen.platform.RegistryUtil;
import com.onehumanawa.cfhcore.content.worldgen.platform.RegistryUtilImpl;
import com.onehumanawa.cfhcore.content.worldgen.registries.CFHBuiltInRegistries;
import com.onehumanawa.cfhcore.content.worldgen.registries.CFHRegistries;
import com.onehumanawa.cfhcore.content.worldgen.structure.rule.StructureRule;
import com.onehumanawa.cfhcore.content.worldgen.structure.rule.StructureRules;
import com.onehumanawa.cfhcore.content.worldgen.surface.rule.CFHSurfaceRules;

@Mod(CFHCore.MOD_ID)
public class CFHCore {

	public static final String MOD_ID = "cfhcore";
	public static final Logger LOGGER = LogManager.getLogger("CFHCore");

	public CFHCore(IEventBus modEventBus, ModContainer container) {
		bootstrap();

		// Register CFH's biome modifier codec types into NeoForge's own serialiser
		// registry so NeoForge can decode neoforge/biome_modifier/*.json at runtime.
		// CFHBuiltInRegistries.BIOME_MODIFIER_TYPE (forge:biome_modifier_serializers)
		// is CFH's internal dispatch registry used by BiomeModifier.DIRECT_CODEC for
		// data-gen; it is separate and both registries must be populated.
		DeferredRegister<MapCodec<? extends BiomeModifier>> biomeModifierSerializers =
				DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, CFHCore.MOD_ID);
		biomeModifierSerializers.register("add",     () -> AddModifier.CODEC);
		biomeModifierSerializers.register("replace", () -> ReplaceModifier.CODEC);
		biomeModifierSerializers.register(modEventBus);

		modEventBus.addListener(CFHCore::gatherData);
		RegistryUtilImpl.register(modEventBus);
		CFHGuiNeoForge.register(modEventBus);

		if (FMLEnvironment.dist == Dist.CLIENT) {
			CFHClient.init(modEventBus, container);
		}
	}

	/**
	 * Bootstraps all of CFHCore's worldgen registries (builtin codec registries +
	 * datapack-backed registries + vanilla registry additions).
	 */
	public static void bootstrap() {
		CFHBuiltInRegistries.bootstrap();
		TemplatePlacements.bootstrap();
		TemplateDecorators.bootstrap();
		CFHChanceModifiers.bootstrap();
		CFHPlacementModifiers.bootstrap();
		CFHDensityFunctions.bootstrap();
		Noises.bootstrap();
		Domains.bootstrap();
		CurveFunctions.bootstrap();
		CFHFeatures.bootstrap();
		CFHHeightProviderTypes.bootstrap();
		CFHFloatProviderTypes.bootstrap();
		BiomeModifiers.bootstrap();
		CFHSurfaceRules.bootstrap();
		StructureRules.bootstrap();

		RegistryUtil.createDataRegistry(CFHRegistries.NOISE, Noise.DIRECT_CODEC, false);
		RegistryUtil.createDataRegistry(CFHRegistries.PRESET, Preset.DIRECT_CODEC, false);
		RegistryUtil.createDataRegistry(CFHRegistries.STRUCTURE_RULE, StructureRule.DIRECT_CODEC, false);
	}

	public static ResourceLocation location(String name) {
		if (name.contains(":")) return ResourceLocation.parse(name);
		return ResourceLocation.fromNamespaceAndPath(CFHCore.MOD_ID, name);
	}

	/**
	 * Generates the worldgen data derived from the single hardcoded preset into
	 * src/generated/resources so it ships with the mod jar and applies to every
	 * world without any configuration.
	 */
	private static void gatherData(GatherDataEvent event) {
		DataGenerator generator = event.getGenerator();
		PackOutput output = generator.getPackOutput();

		Preset preset = Presets.makeDefault();
		CompletableFuture<HolderLookup.Provider> patched = event.getLookupProvider().thenApply(preset::buildPatch);

		generator.addProvider(event.includeServer(), (DataProvider.Factory) (out) -> new RegistriesDatapackGenerator(out, patched));
		generator.addProvider(event.includeServer(), (DataProvider.Factory) (out) -> new CFHBlockTagsProvider(preset, out, patched));
		generator.addProvider(event.includeServer(), (DataProvider.Factory) (out) -> new CFHDensityFunctionTagsProvider(out, patched));
	}
}
