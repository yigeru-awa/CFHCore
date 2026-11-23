package com.onehumanawa.cfhcore.content.gui.veinminer.neoforge;

import com.onehumanawa.cfhcore.content.gui.config.CFHGuiConfigApi;
import com.onehumanawa.cfhcore.content.gui.veinminer.client.VeinClient;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.RenderHighlightEvent.Block;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

final class CFHGuiVeinminerNeoForgeClient {
	static void init(ModContainer container) {
		NeoForge.EVENT_BUS.addListener(Block.class, e -> {
			if (VeinClient.replacesBlockOutline(e.getTarget())) {
				e.setCanceled(true);
			}
		});
		if (ModList.get().isLoaded("cfhcore")) {
			container.registerExtensionPoint(IConfigScreenFactory.class, (IConfigScreenFactory)(mod, parent) -> CFHGuiVeinminerNeoForgeClient.ConfigScreen.open(parent));
		}
	}

	private CFHGuiVeinminerNeoForgeClient() {
	}

	private static final class ConfigScreen {
		static Screen open(Screen parent) {
			return CFHGuiConfigApi.hub(parent, "gameplay/veinminer");
		}
	}
}
