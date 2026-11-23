package com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class Txt {
	public static MutableComponent t(String key, Object... args) {
		return Component.translatable("cfhgui_shops.ui." + key, args);
	}

	public static String s(String key, Object... args) {
		return I18n.get("cfhgui_shops.ui." + key, args);
	}

	public static Component ago(long epochMs) {
		long s = Math.max(0L, (System.currentTimeMillis() - epochMs) / 1000L);
		if (s < 60L) {
			return t("ago.now");
		} else if (s < 3600L) {
			return t("ago.minutes", s / 60L);
		} else {
			return s < 86400L ? t("ago.hours", s / 3600L) : t("ago.days", s / 86400L);
		}
	}

	private Txt() {
	}
}
