package com.onehumanawa.cfhcore.content.registries;

import com.onehumanawa.cfhcore.CFHCore;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import com.tterrag.registrate.Registrate;

/**
 * CFHCore's Registrate entry point. Every registry class in {@code content} shares this
 * single instance, the same way Create uses its own {@code CreateRegistrate}.
 */
public final class CFHContentRegistries {

	private static final Registrate REGISTRATE = Registrate.create(CFHCore.MOD_ID)
			.defaultCreativeTab(CreativeModeTabs.FUNCTIONAL_BLOCKS);

	public static Registrate registrate() {
		return REGISTRATE;
	}

	/**
	 * Called from the mod constructor. Registrate.attach() already hooked the bus
	 * listeners when the instance was created, so this only triggers class loading of
	 * the registry classes so their static fields enqueue their entries.
	 */
	public static void register(IEventBus modEventBus) {
		CFHBlocks.register();
		CFHBlockEntityTypes.register(modEventBus);
	}

	private CFHContentRegistries() {}
}
