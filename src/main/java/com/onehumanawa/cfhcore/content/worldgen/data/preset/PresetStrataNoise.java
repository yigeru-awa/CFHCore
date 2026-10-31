package com.onehumanawa.cfhcore.content.worldgen.data.preset;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import com.onehumanawa.cfhcore.content.worldgen.data.preset.settings.Preset;
import com.onehumanawa.cfhcore.content.worldgen.noise.module.Noise;
import com.onehumanawa.cfhcore.content.worldgen.noise.module.Noises;
import com.onehumanawa.cfhcore.content.worldgen.util.Seed;

public class PresetStrataNoise {
	public static final ResourceKey<Noise> STRATA_SELECTOR = createKey("selector");
	public static final ResourceKey<Noise> STRATA_DEPTH = createKey("depth");

	public static void bootstrap(Preset preset, BootstrapContext<Noise> ctx) {
		Seed seed = new Seed(1234153);
		int strataScale = preset.miscellaneous().strataRegionSize;
		
		Noise selector = Noises.worley(seed.next(), strataScale);
		selector = Noises.warpPerlin(selector, seed.next(), strataScale / 4, 2, strataScale / 2F);
		selector = Noises.warpPerlin(selector, seed.next(), 15, 2, 30);
		ctx.register(STRATA_SELECTOR, selector);
		ctx.register(STRATA_DEPTH, Noises.perlin(seed.next(), strataScale, 3));
	}
	
	private static ResourceKey<Noise> createKey(String name) {
		return PresetNoiseData.createKey("strata/" + name);
	}
}
