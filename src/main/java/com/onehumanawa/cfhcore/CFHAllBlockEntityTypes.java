package com.onehumanawa.cfhcore;

import com.onehumanawa.cfhcore.content.burner.BurnerBlockEntity;
import com.onehumanawa.cfhcore.content.burner.BurnerRenderer;

import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * CFHCore's block entity registry.
 */
public class CFHAllBlockEntityTypes {

	private static final Registrate REGISTRATE = CFHAllContentRegistries.registrate();

	public static final BlockEntityEntry<BurnerBlockEntity> BURNER = REGISTRATE
			.blockEntity("burner", BurnerBlockEntity::new)
			.validBlocks(CFHAllBlocks.BURNER)
			.renderer(() -> BurnerRenderer::new)
			.register();

	/**
	 * Registers the item handler capability so hoppers and Create pipes can insert
	 * solid fuel from any side.
	 */
	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BURNER.get(),
				(be, context) -> be.getFuelInventory());
	}

	public static void register(IEventBus modEventBus) {
		modEventBus.addListener(CFHAllBlockEntityTypes::registerCapabilities);
	}

	private CFHAllBlockEntityTypes() {}
}
