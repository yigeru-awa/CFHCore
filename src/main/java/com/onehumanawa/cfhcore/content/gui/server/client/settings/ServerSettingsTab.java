package com.onehumanawa.cfhcore.content.gui.server.client.settings;

import com.onehumanawa.cfhcore.content.gui.config.CFHGuiConfigApi;
import com.onehumanawa.cfhcore.content.gui.config.api.SettingsTabs;
import com.onehumanawa.cfhcore.content.gui.config.api.SettingsTabs.Tab;
import com.onehumanawa.cfhcore.content.gui.config.option.Binding;
import com.onehumanawa.cfhcore.content.gui.config.option.KeyBinding;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionType;
import com.onehumanawa.cfhcore.content.gui.config.ui.Section;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.server.ServerClientConfig;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerClient;
import com.onehumanawa.cfhcore.content.gui.server.client.map.ServerMaps;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Txt;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

public final class ServerSettingsTab {
	public static final String TAB_ID = "server";
	public static final String TAB_PATH = "gameplay/server";
	private static final ServerClientConfig DEFAULTS = new ServerClientConfig();

	public static void register() {
		SettingsTabs.register("gameplay", new Tab("server", Txt.t("settings.tab"), Icon.SERVER, ServerSettingsTab::sections, 130));
		CFHGuiConfigApi.registerReloadHook("server-client", () -> {
			ServerClientConfig.file().load();
			ServerMaps.refresh();
		});
	}

	private static List<Section> sections() {
		List<Section> out = new ArrayList<>();
		Section menus = new Section("menus", text("section.menus"), text("section.menus.about"));
		menus.add(bool("inventoryButtons", c -> c.inventoryButtons, (c, v) -> c.inventoryButtons = v));
		menus.add(bool("inventoryAdmin", c -> c.inventoryAdmin, (c, v) -> c.inventoryAdmin = v));
		menus.add(key(ServerClient.HUB_KEY));
		menus.add(key(ServerClient.ADMIN_KEY));
		menus.add(bool("confirmDeletes", c -> c.confirmDeletes, (c, v) -> c.confirmDeletes = v));
		menus.add(
			Binding.of("cfhgui_server:open", OptionType.ACTION, text("open"))
				.tooltip(text("open.tip"))
				.action(text("open.button"), () -> ServerClient.open(null))
				.actionIcon(Icon.HOME)
				.searchWords("server cfhgui_server menu homes warps")
		);
		out.add(menus);
		Section notices = new Section("notices", text("section.notices"), text("section.notices.about"));
		notices.add(bool("notices", c -> c.notices, (c, v) -> c.notices = v));
		notices.add(bool("requestToast", c -> c.requestToast, (c, v) -> c.requestToast = v).enabledIf(() -> ServerClientConfig.get().notices));
		notices.add(key(ServerClient.ACCEPT_KEY));
		notices.add(key(ServerClient.DENY_KEY));
		notices.add(bool("mailToast", c -> c.mailToast, (c, v) -> c.mailToast = v).enabledIf(() -> ServerClientConfig.get().notices));
		notices.add(bool("noticeSound", c -> c.noticeSound, (c, v) -> c.noticeSound = v).enabledIf(() -> ServerClientConfig.get().notices));
		out.add(notices);
		Section hud = new Section("hud", text("section.hud"), text("section.hud.about"));
		hud.add(bool("hudWarmup", c -> c.hudWarmup, (c, v) -> c.hudWarmup = v));
		hud.add(bool("hudStatus", c -> c.hudStatus, (c, v) -> c.hudStatus = v));
		hud.add(bool("mutedNote", c -> c.mutedNote, (c, v) -> c.mutedNote = v));
		out.add(hud);
		Section maps = new Section("maps", text("section.maps"), text("section.maps.about"));
		maps.add(bool("mapHomes", c -> c.mapHomes, (c, v) -> c.mapHomes = v));
		maps.add(bool("mapWarps", c -> c.mapWarps, (c, v) -> c.mapWarps = v));
		maps.add(bool("mapSpawn", c -> c.mapSpawn, (c, v) -> c.mapSpawn = v));
		maps.add(bool("mapDeath", c -> c.mapDeath, (c, v) -> c.mapDeath = v));
		out.add(maps);
		return out;
	}

	private static Component text(String key) {
		return Txt.t("settings." + key);
	}

	private static Binding key(KeyMapping mapping) {
		KeyBinding b = new KeyBinding(mapping);
		b.searchWords("server cfhgui_server key " + mapping.getName());
		return b;
	}

	private static Binding bool(String field, Function<ServerClientConfig, Boolean> get, BiConsumer<ServerClientConfig, Boolean> put) {
		return Binding.of("cfhgui_server:" + field, OptionType.BOOLEAN, text(field))
			.tooltip(text(field + ".tip"))
			.searchWords("server cfhgui_server " + field)
			.getter(() -> get.apply(ServerClientConfig.get()))
			.setter(v -> {
				put.accept(ServerClientConfig.get(), Boolean.TRUE.equals(v));
				ServerClientConfig.save();
				ServerMaps.refresh();
			})
			.def(get.apply(DEFAULTS));
	}

	private ServerSettingsTab() {
	}
}
