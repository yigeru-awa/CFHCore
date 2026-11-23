package com.onehumanawa.cfhcore.content.gui.teams.client.vanilla;

import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.screen.ScreenIds;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Layout;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;

public final class VanillaTeams {
	public static void init() {
		ScreenIds.register(VanillaTeamsScreen.class, "cfhgui_teams:hub_vanilla", "Teams (vanilla layout)");
		ScreenIds.register(VanillaFormScreen.class, "cfhgui_teams:form", "Teams form");
		ScreenIds.register(VanillaPrefsScreen.class, "cfhgui_teams:preferences", "Teams preferences");
		MenuSlots.provide("cfhgui_teams:hub", Layout.VANILLA, VanillaTeamsScreen::new);
		CoreActions.SCREEN_FACTORIES.put("cfhgui_teams:preferences", VanillaPrefsScreen::new);
	}

	private VanillaTeams() {
	}
}
