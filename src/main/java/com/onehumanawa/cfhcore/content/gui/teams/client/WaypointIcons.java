package com.onehumanawa.cfhcore.content.gui.teams.client;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public final class WaypointIcons {
	private static final Map<String, Icon> ICONS = new LinkedHashMap<>();

	public static List<String> names() {
		return List.copyOf(ICONS.keySet());
	}

	public static Icon get(@Nullable String name) {
		Icon i = name == null ? null : ICONS.get(name);
		return i == null ? Icon.PIN : i;
	}

	private WaypointIcons() {
	}

	static {
		ICONS.put("flag", Icon.PIN);
		ICONS.put("home", Icon.HOME);
		ICONS.put("star", Icon.STAR_FILLED);
		ICONS.put("mine", Icon.BLOCK);
		ICONS.put("farm", Icon.HEART_FILLED);
		ICONS.put("portal", Icon.SPARKLE);
		ICONS.put("danger", Icon.WARNING);
		ICONS.put("loot", Icon.TROPHY);
		ICONS.put("fight", Icon.SWORD);
		ICONS.put("shield", Icon.SHIELD);
		ICONS.put("crown", Icon.CROWN);
		ICONS.put("map", Icon.MAP);
		ICONS.put("compass", Icon.COMPASS);
		ICONS.put("bolt", Icon.BOLT);
		ICONS.put("world", Icon.WORLD);
	}
}
