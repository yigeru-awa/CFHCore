package com.onehumanawa.cfhcore.content.gui.teams.model;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public enum Rank {
	MEMBER,
	MODERATOR,
	ADMIN,
	OWNER;

	public boolean atLeast(Rank other) {
		return this.ordinal() >= other.ordinal();
	}

	public boolean above(Rank other) {
		return this.ordinal() > other.ordinal();
	}

	@Nullable
	public Rank promoted() {
		return this == MEMBER ? MODERATOR : (this == MODERATOR ? ADMIN : null);
	}

	@Nullable
	public Rank demoted() {
		return this == ADMIN ? MODERATOR : (this == MODERATOR ? MEMBER : null);
	}

	public String key() {
		return this.name().toLowerCase(Locale.ROOT);
	}

	public static Rank parse(@Nullable String s, Rank fallback) {
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
