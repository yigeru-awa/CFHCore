package com.onehumanawa.cfhcore.content.gui.profile;

import java.util.List;
import java.util.Locale;

public enum Slot {
	SKIN(false),
	RIGHT_ARM(false),
	LEFT_ARM(false),
	RIGHT_LEG(false),
	LEFT_LEG(false),
	PET(false),
	HAT(true),
	FACE(true),
	SHIRT(true),
	PANTS(true),
	BACK(true);

	private final boolean worn;

	private Slot(final boolean worn) {
		this.worn = worn;
	}

	public boolean worn() {
		return this.worn;
	}

	public String key() {
		return this.name().toLowerCase(Locale.ROOT);
	}

	public static List<Slot> body() {
		return List.of(SKIN, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG, PET);
	}

	public static List<Slot> wear() {
		return List.of(HAT, FACE, SHIRT, PANTS, BACK);
	}
}
