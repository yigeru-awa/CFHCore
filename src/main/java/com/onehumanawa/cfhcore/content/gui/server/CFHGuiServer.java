package com.onehumanawa.cfhcore.content.gui.server;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.action.ActionType;
import com.onehumanawa.cfhcore.content.gui.core.module.CFHGuiModule;
import com.onehumanawa.cfhcore.content.gui.core.module.CFHGuiModule.HubEntry;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerClient;
import com.onehumanawa.cfhcore.content.gui.server.net.ServerNet;
import com.onehumanawa.cfhcore.content.gui.server.service.ServerRuntime;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CFHGuiServer implements CFHGuiModule {
	public static final String MOD_ID = "cfhgui_server";
	public static final Logger LOGGER = LoggerFactory.getLogger("CFHGui Server");
	public static final CFHGuiServer MODULE = new CFHGuiServer();

	private CFHGuiServer() {
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath("cfhgui_server", path);
	}

	public String id() {
		return "cfhgui_server";
	}

	public Component displayName() {
		return Component.translatable("cfhgui_server.name");
	}

	public Icon icon() {
		return Icon.SHIELD;
	}

	public void init() {
		CFHGui.init();
		Nodes.init();
		ServerNet.register();
		ServerRuntime.init();
		LOGGER.info("[CFHGui Server] init");
	}

	public void initClient() {
		CFHGuiServer.ClientSide.init();
	}

	public List<HubEntry> hubEntries() {
		return CFHGuiPlatform.get().isClient() ? CFHGuiServer.ClientSide.hubEntries() : List.of();
	}

	public List<ActionType> actions() {
		return CFHGuiPlatform.get().isClient() ? CFHGuiServer.ClientSide.actions() : List.of();
	}

	private static final class ClientSide {
		static void init() {
			ServerClient.init();
		}

		static List<HubEntry> hubEntries() {
			return ServerClient.hubEntries();
		}

		static List<ActionType> actions() {
			return ServerClient.actions();
		}
	}
}
