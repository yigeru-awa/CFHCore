package com.onehumanawa.cfhcore.content.gui.crafting.neoforge;

import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.module.Modules;
import com.onehumanawa.cfhcore.content.gui.crafting.CFHGuiCrafting;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.ModListScreen;

@Mod("cfhcore")
public final class CFHGuiCraftingNeoForge {
	public CFHGuiCraftingNeoForge(IEventBus modBus, ModContainer container) {
		Modules.register(CFHGuiCrafting.MODULE);
		if (FMLEnvironment.dist.isClient()) {
			CFHGuiCraftingNeoForge.ClientSide.init();
		}
	}

	private static final class ClientSide {
		static void init() {
			if (System.getProperty("cfh.autoScreens") != null || Boolean.getBoolean("cfh.autoWorld")) {
				CoreActions.SCREEN_FACTORIES.put("cfhgui_crafting:mods", ModListScreen::new);
			}
		}
	}
}
