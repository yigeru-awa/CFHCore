package com.onehumanawa.cfhcore.content.worldgen.mixin;

import net.minecraft.world.level.levelgen.*;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.google.common.base.Suppliers;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction.NoiseHolder;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import com.onehumanawa.cfhcore.content.worldgen.concurrent.ThreadPools;
import com.onehumanawa.cfhcore.content.worldgen.data.preset.settings.Preset;
import com.onehumanawa.cfhcore.content.worldgen.registries.CFHRegistries;
import com.onehumanawa.cfhcore.content.worldgen.tags.CFHDensityFunctionTags;
import com.onehumanawa.cfhcore.content.worldgen.GeneratorContext;
import com.onehumanawa.cfhcore.content.worldgen.CFHRandomState;
import com.onehumanawa.cfhcore.content.worldgen.CFHWorldGenContext;
import com.onehumanawa.cfhcore.content.worldgen.densityfunction.CellSampler;
import com.onehumanawa.cfhcore.content.worldgen.densityfunction.MarkerFunction;
import com.onehumanawa.cfhcore.content.worldgen.densityfunction.NoiseFunction;
import com.onehumanawa.cfhcore.content.worldgen.noise.module.Noise;
import com.onehumanawa.cfhcore.content.worldgen.noise.module.Noises;

@Mixin(RandomState.class)
@Implements(@Interface(iface = CFHRandomState.class, prefix = "cfhcore$CFHRandomState$"))
class MixinRandomState {
	// Hardcoded performance settings (previously configurable via PerformanceConfig)
	private static final int TILE_SIZE = 3;
	private static final int BATCH_COUNT = 6;

	private DensityFunction.Visitor densityFunctionWrapper;
	@Shadow
	@Final
	private Climate.Sampler sampler;
	@Shadow
	@Final
	private SurfaceSystem surfaceSystem;

	private boolean hasContext;
	@Unique private boolean cfhcore$isCFHDimension = false; // Tracks if the BASE router belongs to CFH

	@Nullable
	private GeneratorContext generatorContext;
	@Nullable
	private Preset preset;

	private long seed;

	@Redirect(
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/levelgen/NoiseRouter;mapAll(Lnet/minecraft/world/level/levelgen/DensityFunction$Visitor;)Lnet/minecraft/world/level/levelgen/NoiseRouter;"
			),
			method = "<init>",
			require = 1
	)
	private NoiseRouter RandomState(NoiseRouter router, DensityFunction.Visitor visitor, NoiseGeneratorSettings noiseGeneratorSettings, HolderGetter<NormalNoise.NoiseParameters> params, final long seed) {
		this.seed = seed;

		final boolean isVanillaOverworld = CFHWorldGenContext.IS_VANILLA_OVERWORLD.get();
		this.densityFunctionWrapper = new DensityFunction.Visitor() {

			@Override
			public DensityFunction apply(DensityFunction function) {
				if(function instanceof NoiseFunction.Marker marker) {
					return new NoiseFunction(marker.noise(), (int) seed);
				}
				if(function instanceof CellSampler.Marker marker) {

					if (!isVanillaOverworld) {
						return DensityFunctions.zero();
					}

					MixinRandomState.this.hasContext = true;
					return new CellSampler(Suppliers.memoize(() -> MixinRandomState.this.generatorContext.lookup), marker.field());
				}

				if (function instanceof MarkerFunction && !isVanillaOverworld) {
					return DensityFunctions.zero();
				}

				return visitor.apply(function);
			}

			@Override
			public NoiseHolder visitNoise(NoiseHolder noiseHolder) {
				return visitor.visitNoise(noiseHolder);
			}
		};

		// Map the base router first. If the current dimension naturally utilizes CFH, hasContext flips to true here.
		NoiseRouter mappedRouter = router.mapAll(this.densityFunctionWrapper);
		if (this.hasContext && isVanillaOverworld) {
			this.cfhcore$isCFHDimension = true;
		}
		return mappedRouter;
	}

	public void cfhcore$CFHRandomState$initialize(RegistryAccess registries) {
		RegistryLookup<Preset> presets = registries.lookupOrThrow(CFHRegistries.PRESET);

		// Always assign the global preset. UI previews and legacy features in other dimensions
		// rely on this being non-null to read settings without throwing an NPE.
		presets.get(Preset.KEY).ifPresent((presetHolder) -> {
			this.preset = presetHolder.value();
		});

		// Only compile global density tags and build a heavy Overworld GeneratorContext
		// if the base router mapping verified that this instance is actually an CFH worldgen dimension.
		if (this.cfhcore$isCFHDimension) {
			RegistryLookup<Noise> noises = registries.lookupOrThrow(CFHRegistries.NOISE);
			RegistryLookup<DensityFunction> functions = registries.lookupOrThrow(Registries.DENSITY_FUNCTION);

			functions.get(CFHDensityFunctionTags.ADDITIONAL_NOISE_ROUTER_FUNCTIONS).ifPresent((set) -> {
				set.forEach((function) -> function.value().mapAll(this.densityFunctionWrapper));
			});

			if (this.preset != null) {
				// Hardcoded worldgen performance settings (former PerformanceConfig defaults).
				this.generatorContext = GeneratorContext.makeCached(this.preset, noises, (int) this.seed, TILE_SIZE, BATCH_COUNT, ThreadPools.availableProcessors() > 4);
			}
		}
	}

	@Nullable
	public Preset cfhcore$CFHRandomState$preset() {
		return this.preset;
	}

	@Nullable
	public GeneratorContext cfhcore$CFHRandomState$generatorContext() {
		return this.generatorContext;
	}

	@Nullable
	public DensityFunction cfhcore$CFHRandomState$wrap(DensityFunction function) {
		return this.densityFunctionWrapper != null ? function.mapAll(this.densityFunctionWrapper) : function;
	}

	public Noise cfhcore$CFHRandomState$seed(Noise noise) {
		return Noises.shiftSeed(noise, (int) this.seed);
	}
}