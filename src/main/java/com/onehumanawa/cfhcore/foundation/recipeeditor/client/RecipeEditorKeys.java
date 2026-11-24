package com.onehumanawa.cfhcore.foundation.recipeeditor.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public final class RecipeEditorKeys {
	public static final String CATEGORY = "key.categories.cfhgui_recipeeditor";
	public static final KeyMapping OPEN_EDITOR = new KeyMapping(
		"key.cfhgui_recipeeditor.open_editor",
		Type.KEYSYM,
		InputConstants.UNKNOWN.getValue(),
		"key.categories.cfhgui_recipeeditor"
	);

	private static boolean registered;

	public static synchronized void init() {
		if (!registered) {
			registered = true;
		}
	}

	private RecipeEditorKeys() {
	}
}