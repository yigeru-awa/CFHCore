package com.onehumanawa.cfhcore.content.gui.veinminer.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiKeys;
import com.onehumanawa.cfhcore.content.gui.veinminer.CFHGuiVeinminer;
import net.minecraft.client.KeyMapping;

public final class VeinKeys {
	public static final String CATEGORY = "key.categories.cfhgui_veinminer";
	public static final KeyMapping ACTIVATE = new KeyMapping("key.cfhgui_veinminer.activate", Type.KEYSYM, 96, "key.categories.cfhgui_veinminer");
	public static final KeyMapping NEXT_SHAPE = new KeyMapping(
		"key.cfhgui_veinminer.next_shape", Type.KEYSYM, devKey("nextShapeKey"), "key.categories.cfhgui_veinminer"
	);
	public static final KeyMapping PREVIOUS_SHAPE = new KeyMapping(
		"key.cfhgui_veinminer.previous_shape", Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.cfhgui_veinminer"
	);
	private static boolean registered;

	private static int devKey(String name) {
		try {
			String v = CFHGuiVeinminer.devProperty(name);
			return v.isEmpty() ? InputConstants.UNKNOWN.getValue() : Integer.parseInt(v);
		} catch (NumberFormatException var2) {
			return InputConstants.UNKNOWN.getValue();
		}
	}

	public static synchronized void init() {
		if (!registered) {
			registered = true;
			CFHGuiKeys.register(ACTIVATE);
			CFHGuiKeys.register(NEXT_SHAPE);
			CFHGuiKeys.register(PREVIOUS_SHAPE);
		}
	}

	private VeinKeys() {
	}
}
