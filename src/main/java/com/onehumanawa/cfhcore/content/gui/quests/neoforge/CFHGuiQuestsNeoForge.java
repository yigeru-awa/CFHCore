package com.onehumanawa.cfhcore.content.gui.quests.neoforge;

import com.onehumanawa.cfhcore.content.gui.core.module.Modules;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import com.onehumanawa.cfhcore.content.gui.quests.server.Carried;
import com.onehumanawa.cfhcore.content.gui.quests.server.QuestCommands;
import com.onehumanawa.cfhcore.content.gui.quests.server.QuestEvents;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent;

@Mod("cfhcore")
public final class CFHGuiQuestsNeoForge {
	public CFHGuiQuestsNeoForge(IEventBus modBus, ModContainer container) {
		Modules.register(CFHGuiQuests.MODULE);
		NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, e -> QuestCommands.register(e.getDispatcher()));
		Carried.install(new NeoForgeCarried());
		QuestEvents.placesByLoader(true);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, EntityPlaceEvent.class, e -> {
			if (e.getEntity() instanceof ServerPlayer player) {
				QuestEvents.placed(player, e.getPlacedBlock());
			}
		});
	}
}
