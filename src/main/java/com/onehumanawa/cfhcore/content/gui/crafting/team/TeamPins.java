package com.onehumanawa.cfhcore.content.gui.crafting.team;

import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.crafting.CFHGuiCrafting;
import com.onehumanawa.cfhcore.content.gui.crafting.team.client.TeamPinsClient;

public final class TeamPins {
	public static final String TEAMS_ID = "cfhgui_teams";
	private static boolean enabled;

	public static synchronized void init() {
		if (!enabled && CFHGuiPlatform.get().isModLoaded("cfhgui_teams")) {
			TeamsLink link;
			try {
				link = TeamPins.TeamsSide.link();
			} catch (Throwable var2) {
				CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] CFHGui Teams is installed but could not be linked; team pins are off ({})", var2.toString());
				return;
			}

			enabled = true;
			TeamPinsNet.register();
			TeamPinsServer.init(link);
			if (CFHGuiPlatform.get().isClient()) {
				TeamPins.ClientSide.init();
			}

			CFHGuiCrafting.LOGGER.info("[CFHGui Crafting] CFHGui Teams is here: recipes and trees can be pinned to a team");
		}
	}

	public static boolean enabled() {
		return enabled;
	}

	private TeamPins() {
	}

	private static final class ClientSide {
		static void init() {
			TeamPinsClient.init();
		}
	}

	private static final class TeamsSide {
		static TeamsLink link() {
			return TeamsBridge.create();
		}
	}
}
