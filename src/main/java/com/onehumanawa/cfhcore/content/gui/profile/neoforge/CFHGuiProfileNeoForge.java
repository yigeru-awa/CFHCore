package com.onehumanawa.cfhcore.content.gui.profile.neoforge;

import com.onehumanawa.cfhcore.content.gui.core.module.Modules;
import com.onehumanawa.cfhcore.content.gui.profile.CFHGuiProfile;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod("cfhcore")
public final class CFHGuiProfileNeoForge {
	public CFHGuiProfileNeoForge(IEventBus modBus, ModContainer container) {
		Modules.register(CFHGuiProfile.MODULE);
	}
}
