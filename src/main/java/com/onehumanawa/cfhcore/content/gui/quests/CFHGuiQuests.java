package com.onehumanawa.cfhcore.content.gui.quests;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.action.ActionType;
import com.onehumanawa.cfhcore.content.gui.core.module.CFHGuiModule;
import com.onehumanawa.cfhcore.content.gui.core.module.CFHGuiModule.HubEntry;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsClient;
import com.onehumanawa.cfhcore.content.gui.quests.net.QuestsNet;
import com.onehumanawa.cfhcore.content.gui.quests.server.QuestsServer;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CFHGuiQuests implements CFHGuiModule {
	public static final String MOD_ID = "cfhgui_quests";
	public static final Logger LOGGER = LoggerFactory.getLogger("CFHGui Quests");
	public static final CFHGuiQuests MODULE = new CFHGuiQuests();

	private CFHGuiQuests() {
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath("cfhgui_quests", path);
	}

	public String id() {
		return "cfhgui_quests";
	}

	public Component displayName() {
		return Component.translatable("cfhgui_quests.name");
	}

	public Icon icon() {
		return Icon.QUEST;
	}

	public void init() {
		CFHGui.init();
		QuestsNet.register();
		QuestsServer.init();
		LOGGER.info("[CFHGui Quests] init");
	}

	public void initClient() {
		CFHGuiQuests.ClientSide.init();
	}

	public List<HubEntry> hubEntries() {
		return CFHGuiPlatform.get().isClient() ? CFHGuiQuests.ClientSide.hubEntries() : List.of();
	}

	public List<ActionType> actions() {
		return CFHGuiPlatform.get().isClient() ? CFHGuiQuests.ClientSide.actions() : List.of();
	}

	private static final class ClientSide {
		static void init() {
			QuestsClient.init();
		}

		static List<HubEntry> hubEntries() {
			return QuestsClient.hubEntries();
		}

		static List<ActionType> actions() {
			return QuestsClient.actions();
		}
	}
}
