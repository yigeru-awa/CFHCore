package com.onehumanawa.cfhcore.content.gui.veinminer;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.module.CFHGuiModule;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.veinminer.client.VeinClient;
import com.onehumanawa.cfhcore.content.gui.veinminer.client.VeinSettingsTab;
import com.onehumanawa.cfhcore.content.gui.veinminer.config.VeinServerConfig;
import com.onehumanawa.cfhcore.content.gui.veinminer.net.VeinNet;
import com.onehumanawa.cfhcore.content.gui.veinminer.server.VeinServer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CFHGuiVeinminer implements CFHGuiModule {
	public static final String MOD_ID = "cfhgui_veinminer";
	public static final Logger LOGGER = LoggerFactory.getLogger("CFHGui Veinminer");
	public static final CFHGuiVeinminer MODULE = new CFHGuiVeinminer();
	public static final String USE = "cfhgui_veinminer.use";
	public static final String ADMIN = "cfhgui_veinminer.admin";

	private CFHGuiVeinminer() {
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath("cfhgui_veinminer", path);
	}

	public static String version() {
		return CFHGuiPlatform.get().modInfo("cfhcore").map(i -> i.version()).orElse("dev");
	}

	public static String devProperty(String name) {
		String v = System.getProperty("cfh.veinminer." + name);
		return v == null ? "" : v.trim();
	}

	public String id() {
		return "cfhgui_veinminer";
	}

	public Component displayName() {
		return Component.translatable("cfhgui_veinminer.name");
	}

	public Icon icon() {
		return Icon.BOLT;
	}

	public void init() {
		CFHGui.init();
		Perms.declare("cfhgui_veinminer.use", 0);
		Perms.declare("cfhgui_veinminer.admin", 2);
		VeinServerConfig.file();
		VeinNet.register();
		CFHGuiEvents.SERVER_STARTED.register(VeinServer::onServerStarted);
		CFHGuiEvents.SERVER_STOPPING.register(VeinServer::onServerStopping);
		CFHGuiEvents.SERVER_TICK_END.register(VeinServer::onServerTick);
		CFHGuiEvents.PLAYER_JOINED.register(VeinServer::onJoin);
		CFHGuiEvents.PLAYER_LEFT.register(VeinServer::onLeave);
		LOGGER.info("[CFHGui Veinminer] init");
	}

	public void initClient() {
		CFHGuiVeinminer.ClientSide.init();
	}

	private static final class ClientSide {
		static void init() {
			VeinClient.init();
			if (CFHGuiPlatform.get().isModLoaded("cfhgui_config")) {
				VeinSettingsTab.register();
			}
		}
	}
}
