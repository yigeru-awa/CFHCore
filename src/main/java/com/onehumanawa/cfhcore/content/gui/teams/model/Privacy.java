package com.onehumanawa.cfhcore.content.gui.teams.model;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public enum Privacy {
	OPEN,
	REQUEST,
	INVITE;

	public String key() {
		return this.name().toLowerCase(Locale.ROOT);
	}

	public static Privacy parse(@Nullable String s, Privacy fallback) {
		if (s == null) {
			return fallback;
		} else {
			try {
				return valueOf(s.trim().toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException var3) {
				return fallback;
			}
		}
	}
}
