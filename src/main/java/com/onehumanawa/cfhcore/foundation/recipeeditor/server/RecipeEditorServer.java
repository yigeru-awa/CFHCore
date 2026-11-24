package com.onehumanawa.cfhcore.foundation.recipeeditor.server;

import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.foundation.recipeeditor.config.RecipeEditorServerConfig;
import net.minecraft.server.MinecraftServer;

public final class RecipeEditorServer {
	public static void onServerStarted(MinecraftServer server) {
		if (!RecipeEditorServerConfig.enabled()) {
			return;
		}
		// Initialize recipe store on server start
	}

	public static void onServerStopping(MinecraftServer server) {
		// Save recipe store on server stop
	}

	public static void onServerTick(MinecraftServer server) {
		// Periodic tasks
	}

	private RecipeEditorServer() {
	}
}