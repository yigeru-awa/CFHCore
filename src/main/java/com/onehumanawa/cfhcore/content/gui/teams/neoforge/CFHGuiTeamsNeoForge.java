package com.onehumanawa.cfhcore.content.gui.teams.neoforge;

import com.onehumanawa.cfhcore.content.gui.core.module.Modules;
import com.onehumanawa.cfhcore.content.gui.teams.CFHGuiTeams;
import com.onehumanawa.cfhcore.content.gui.teams.server.TeamCommands;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod("cfhcore")
public final class CFHGuiTeamsNeoForge {
	public CFHGuiTeamsNeoForge(IEventBus modBus, ModContainer container) {
		Modules.register(CFHGuiTeams.MODULE);
		// TeamCommands.register() is now called via CoreCommands.init() in CFHGuiServerNeoForge
		NeoTeamsProtection.register();
	}
}