package com.onehumanawa.cfhcore.foundation.recipeeditor;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.module.CFHGuiModule;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.foundation.recipeeditor.client.RecipeEditorClient;
import com.onehumanawa.cfhcore.foundation.recipeeditor.config.RecipeEditorServerConfig;
import com.onehumanawa.cfhcore.foundation.recipeeditor.net.RecipeEditorNet;
import com.onehumanawa.cfhcore.foundation.recipeeditor.server.RecipeEditorServer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CFHGuiRecipeEditor implements CFHGuiModule {
	public static final String MOD_ID = "cfhgui_recipeeditor";
	public static final Logger LOGGER = LoggerFactory.getLogger("CFHGui RecipeEditor");
	public static final CFHGuiRecipeEditor MODULE = new CFHGuiRecipeEditor();
	public static final String USE = "cfhgui_recipeeditor.use";
	public static final String ADMIN = "cfhgui_recipeeditor.admin";

	private CFHGuiRecipeEditor() {
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath("cfhgui_recipeeditor", path);
	}

	public static String version() {
		return CFHGuiPlatform.get().modInfo("cfhcore").map(i -> i.version()).orElse("dev");
	}

	public static String devProperty(String name) {
		String v = System.getProperty("cfh.recipeeditor." + name);
		return v == null ? "" : v.trim();
	}

	public String id() {
		return "cfhgui_recipeeditor";
	}

	public Component displayName() {
		return Component.translatable("cfhgui_recipeeditor.name");
	}

	public Icon icon() {
		return Icon.GRID;
	}

	public void init() {
		CFHGui.init();
		Perms.declare("cfhgui_recipeeditor.use", 0);
		Perms.declare("cfhgui_recipeeditor.admin", 2);
		RecipeEditorServerConfig.file();
		RecipeEditorNet.register();
		CFHGuiEvents.SERVER_STARTED.register(RecipeEditorServer::onServerStarted);
		CFHGuiEvents.SERVER_STOPPING.register(RecipeEditorServer::onServerStopping);
		CFHGuiEvents.SERVER_TICK_END.register(RecipeEditorServer::onServerTick);
		LOGGER.info("[CFHGui RecipeEditor] init");
	}

	public void initClient() {
		RecipeEditorClient.init();
	}
}