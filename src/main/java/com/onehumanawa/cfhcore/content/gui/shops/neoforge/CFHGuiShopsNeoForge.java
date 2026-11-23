package com.onehumanawa.cfhcore.content.gui.shops.neoforge;

import com.onehumanawa.cfhcore.content.gui.core.module.Modules;
import com.onehumanawa.cfhcore.content.gui.shops.CFHGuiShops;
import com.onehumanawa.cfhcore.content.gui.shops.command.ShopCommands;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod("cfhcore")
public final class CFHGuiShopsNeoForge {
	public CFHGuiShopsNeoForge(IEventBus modBus, ModContainer container) {
		Modules.register(CFHGuiShops.MODULE);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOW, RegisterCommandsEvent.class, e -> ShopCommands.register(e.getDispatcher(), e.getBuildContext()));
	}
}
