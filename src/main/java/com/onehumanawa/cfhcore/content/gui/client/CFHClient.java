package com.onehumanawa.cfhcore.content.gui.client;

import com.onehumanawa.cfhcore.CFHCore;
import com.onehumanawa.cfhcore.content.gui.config.CFHGuiConfig;
import com.onehumanawa.cfhcore.content.gui.config.neoforge.CFHGuiConfigNeoForge;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.module.Modules;
import com.onehumanawa.cfhcore.content.gui.core.neoforge.CFHGuiNeoForgeClient;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

// Client bootstrap for the CFHGui menu stack, ported from the upstream Slate sources.
public final class CFHClient {

	private CFHClient() {}

	public static void init(IEventBus modBus, ModContainer container) {
		CFHGui.init();
		Modules.register(CFHGuiMenu.MODULE);
		CFHGuiConfigNeoForge.register(modBus, container);
		CFHGuiNeoForgeClient.init(modBus, container);
		CFHCore.LOGGER.info("CFHGui menus are active");
	}
}
