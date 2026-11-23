package com.onehumanawa.cfhcore.content.gui.server.service;

import com.google.gson.reflect.TypeToken;
import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.server.CFHGuiServer;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

public final class Msg {
	private static final String LANG = "/assets/cfhgui_server/lang/en_us.json";
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
			try (InputStream in = CFHGuiServer.class.getResourceAsStream("/assets/cfhgui_server/lang/en_us.json")) {
				if (in == null) {
					CFHGuiServer.LOGGER.warn("[CFHGui Server] {} is not in the jar: players without the mod will read translation keys", "/assets/cfhgui_server/lang/en_us.json");
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
			CFHGuiServer.LOGGER.warn("[CFHGui Server] could not read {}: {}", "/assets/cfhgui_server/lang/en_us.json", var8.toString());
			return Map.of();
		}
	}

	private Msg() {
	}
}
