package com.onehumanawa.cfhcore.content.gui.teams;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.action.ActionType;
import com.onehumanawa.cfhcore.content.gui.core.module.CFHGuiModule;
import com.onehumanawa.cfhcore.content.gui.core.module.CFHGuiModule.HubEntry;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsClient;
import com.onehumanawa.cfhcore.content.gui.teams.net.TeamsNet;
import com.onehumanawa.cfhcore.content.gui.teams.server.TeamsServer;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CFHGuiTeams implements CFHGuiModule {
	public static final String MOD_ID = "cfhgui_teams";
	public static final Logger LOGGER = LoggerFactory.getLogger("CFHGui Teams");
	public static final CFHGuiTeams MODULE = new CFHGuiTeams();

	private CFHGuiTeams() {
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath("cfhgui_teams", path);
	}

	public String id() {
		return "cfhgui_teams";
	}

	public Component displayName() {
		return Component.translatable("cfhgui_teams.name");
	}

	public Icon icon() {
		return Icon.TEAM;
	}

	public void init() {
		CFHGui.init();
		TeamsNet.register();
		TeamsServer.init();
		LOGGER.info("[CFHGui Teams] init");
	}

	public void initClient() {
		CFHGuiTeams.ClientSide.init();
	}

	public List<HubEntry> hubEntries() {
		return CFHGuiPlatform.get().isClient() ? CFHGuiTeams.ClientSide.hubEntries() : List.of();
	}

	public List<ActionType> actions() {
		return CFHGuiPlatform.get().isClient() ? CFHGuiTeams.ClientSide.actions() : List.of();
	}

	private static final class ClientSide {
		static void init() {
			TeamsClient.init();
		}

		static List<HubEntry> hubEntries() {
			return TeamsClient.hubEntries();
		}

		static List<ActionType> actions() {
			return TeamsClient.actions();
		}
	}
}
