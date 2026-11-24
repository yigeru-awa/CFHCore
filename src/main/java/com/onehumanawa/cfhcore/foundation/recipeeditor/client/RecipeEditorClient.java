package com.onehumanawa.cfhcore.foundation.recipeeditor.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.foundation.recipeeditor.CFHGuiRecipeEditor;
import com.onehumanawa.cfhcore.foundation.recipeeditor.config.RecipeEditorClientConfig;
import com.onehumanawa.cfhcore.foundation.recipeeditor.config.RecipeEditorServerConfig;
import com.onehumanawa.cfhcore.foundation.recipeeditor.net.RecipeEditorNet;
import com.onehumanawa.cfhcore.foundation.recipeeditor.server.RecipeEditorServer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public final class RecipeEditorClient {
	private static boolean initialised;

	public static synchronized void init() {
		if (!initialised) {
			initialised = true;
			// Initialize keys first
			RecipeEditorKeys.init();
			// Register key binding handler
			CFHGuiEvents.KEY_PRESSED.register((key, scan, mods) -> {
				if (RecipeEditorKeys.OPEN_EDITOR.matches(key, scan) && !RecipeEditorKeys.OPEN_EDITOR.isUnbound()) {
					openEditor();
					return true;
				}
				return false;
			});
		}
	}

	public static void openEditor() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen == null) {
			mc.setScreen(new RecipeEditorScreen(null));
		}
	}

	private RecipeEditorClient() {
	}
}