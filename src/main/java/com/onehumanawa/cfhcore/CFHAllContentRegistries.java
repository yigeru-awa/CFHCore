package com.onehumanawa.cfhcore;

import com.simibubi.create.AllItems;

import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.entry.RegistryEntry;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;

/**
 * CFHCore's Registrate entry point. Every registry class in {@code content} shares this
 * single instance, the same way Create uses its own {@code CreateRegistrate}.
 */
public final class CFHAllContentRegistries {

	private static final Registrate REGISTRATE = Registrate.create(CFHCore.MOD_ID);

	/**
	 * The mod's own creative tab, "CFH Core", carrying the burner and everything else
	 * registered through this Registrate instance (items join the tab automatically).
	 */
	public static final RegistryEntry<CreativeModeTab, CreativeModeTab> CREATIVE_TAB = REGISTRATE
			.defaultCreativeTab("core", tab -> tab
					.icon(() -> AllItems.BUILDERS_TEA.asStack())
					.withTabsBefore(CreativeModeTabs.SPAWN_EGGS))
			.register();

	public static Registrate registrate() {
		return REGISTRATE;
	}

	/**
	 * Called from the mod constructor. Registrate.attach() already hooked the bus
	 * listeners when the instance was created, so this only triggers class loading of
	 * the registry classes so their static fields enqueue their entries.
	 */
	public static void register(IEventBus modEventBus) {
		CFHAllBlocks.register();
		CFHAllBlockEntityTypes.register(modEventBus);
	}

	private CFHAllContentRegistries() {}
}
