package com.onehumanawa.cfhcore.content.gui.teams.server;

import com.google.gson.reflect.TypeToken;
import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.teams.CFHGuiTeams;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;

public final class Msg {
	private static final String LANG = "/assets/cfhgui_teams/lang/en_us.json";
	@Nullable
	private static volatile Map<String, String> english;

	public static MutableComponent tr(String key, Object... args) {
		for (int i = 0; i < args.length; i++) {
			Object a = args[i];
			if (!(a instanceof Component) && !(a instanceof Number) && !(a instanceof Boolean) && !(a instanceof String)) {
				args[i] = String.valueOf(a);
			}
		}

		return Component.translatableWithFallback(key, english(key), args);
	}

	@Nullable
	public static String english(String key) {
		Map<String, String> map = english;
		if (map == null) {
			map = load();
			english = map;
		}

		return map.get(key);
	}

	private static Map<String, String> load() {
		try {
			Map var3;
			try (InputStream in = CFHGuiTeams.class.getResourceAsStream("/assets/cfhgui_teams/lang/en_us.json")) {
				if (in == null) {
					CFHGuiTeams.LOGGER.warn("[CFHGui Teams] {} is not in the jar: players without the mod will read translation keys", "/assets/cfhgui_teams/lang/en_us.json");
					return Map.of();
				}

				try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
					Map<String, String> map = (Map<String, String>)JsonWire.GSON.fromJson(r, (new TypeToken<Map<String, String>>() {
					}).getType());
					var3 = map == null ? Map.of() : map;
				}
			}

			return var3;
		} catch (Exception var8) {
			CFHGuiTeams.LOGGER.warn("[CFHGui Teams] could not read {}: {}", "/assets/cfhgui_teams/lang/en_us.json", var8.toString());
			return Map.of();
		}
	}

	public static MutableComponent team(TeamData team) {
		MutableComponent name = team.solo ? tr("cfhgui_teams.land.personal", team.name) : Component.literal(team.name);
		return name.withStyle(Style.EMPTY.withColor(team.color & 16777215));
	}

	private Msg() {
	}
}
