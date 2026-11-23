package com.onehumanawa.cfhcore.content.gui.quests.client.editor;

import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

final class Tr {
	static final String PREFIX = "cfhgui_quests.editor.";

	static MutableComponent t(String key, Object... args) {
		return Component.translatable("cfhgui_quests.editor." + key, args);
	}

	static MutableComponent field(String name) {
		return t("f." + name);
	}

	static MutableComponent tip(String name) {
		return t("f." + name + ".tip");
	}

	static MutableComponent value(String group, Enum<?> word) {
		return t("v." + group + "." + word.name().toLowerCase(Locale.ROOT));
	}

	static MutableComponent value(String group, String word) {
		return t("v." + group + "." + word.toLowerCase(Locale.ROOT));
	}

	private Tr() {
	}
}
