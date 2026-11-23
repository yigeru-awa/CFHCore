package com.onehumanawa.cfhcore.content.gui.teams.model;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public enum Access {
	MEMBERS,
	ALLIES,
	EVERYONE;

	public boolean allows(Access.Relation relation) {
		return switch (relation) {
			case MEMBER -> true;
			case ALLY -> this != MEMBERS;
			case OUTSIDER -> this == EVERYONE;
		};
	}

	public String key() {
		return this.name().toLowerCase(Locale.ROOT);
	}

	public static Access parse(@Nullable String s, Access fallback) {
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

	public static enum Relation {
		MEMBER,
		ALLY,
		OUTSIDER;
	}
}
