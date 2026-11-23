package com.onehumanawa.cfhcore.content.gui.quests.client.overhaul;

import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Layout;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.quests.client.ui.QuestBookScreen;

public final class OverhaulQuests {
	public static void init() {
		MenuSlots.provide(
			"cfhgui_quests:book", Layout.OVERHAUL, parent -> new QuestBookScreen(parent instanceof QuestBookScreen open ? open.parentScreen() : parent, IslandView::new)
		);
		IslandShots.init();
	}

	private OverhaulQuests() {
	}
}
