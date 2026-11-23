package com.onehumanawa.cfhcore.content.gui.veinminer.neoforge;

import com.onehumanawa.cfhcore.content.gui.core.module.Modules;
import com.onehumanawa.cfhcore.content.gui.veinminer.CFHGuiVeinminer;
import com.onehumanawa.cfhcore.content.gui.veinminer.server.VeinCommands;
import com.onehumanawa.cfhcore.content.gui.veinminer.vein.VeinPlanner;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod("cfhcore")
public final class CFHGuiVeinminerNeoForge {
	public CFHGuiVeinminerNeoForge(IEventBus modBus, ModContainer container) {
		VeinPlanner.setHarvestCheck((player, state, level, pos) -> player.hasCorrectToolForDrops(state, level, pos));
		Modules.register(CFHGuiVeinminer.MODULE);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOW, RegisterCommandsEvent.class, e -> VeinCommands.register(e.getDispatcher()));
		if (FMLEnvironment.dist.isClient()) {
			CFHGuiVeinminerNeoForgeClient.init(container);
		}
	}
}
