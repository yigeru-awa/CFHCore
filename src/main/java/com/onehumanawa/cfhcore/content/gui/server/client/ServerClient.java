package com.onehumanawa.cfhcore.content.gui.server.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiKeys;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.action.ActionType;
import com.onehumanawa.cfhcore.content.gui.core.layout.action.ActionType.Arg;
import com.onehumanawa.cfhcore.content.gui.core.module.CFHGuiModule.HubEntry;
import com.onehumanawa.cfhcore.content.gui.core.platform.Loader;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.ServerClientConfig;
import com.onehumanawa.cfhcore.content.gui.server.CFHGuiServer;
import com.onehumanawa.cfhcore.content.gui.server.client.hud.ServerHud;
import com.onehumanawa.cfhcore.content.gui.server.client.map.ServerMaps;
import com.onehumanawa.cfhcore.content.gui.server.client.stats.ServerStats;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.ServerUi;
import com.onehumanawa.cfhcore.content.gui.server.net.ServerNet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class ServerClient {
	public static final String HOMES = "homes";
	public static final String WARPS = "warps";
	public static final String PLAYERS = "players";
	public static final String RANKINGS = "rankings";
	public static final String MAIL = "mail";
	public static final String KITS = "kits";
	public static final String INFO = "info";
	public static final List<String> PAGES = List.of("homes", "warps", "players", "rankings", "mail", "kits", "info");
	public static final String A_DASHBOARD = "dashboard";
	public static final String A_PLAYERS = "players";
	public static final String A_ROLES = "roles";
	public static final String A_PLACES = "places";
	public static final String A_KITS = "kits";
	public static final String A_MODERATION = "moderation";
	public static final String A_RULES = "rules";
	public static final String A_WORLD = "world";
	public static final List<String> ADMIN_PAGES = List.of("dashboard", "players", "roles", "places", "kits", "moderation", "rules", "world");
	public static final KeyMapping HUB_KEY = new KeyMapping("key.cfhgui_server.hub", Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.cfh");
	public static final KeyMapping ADMIN_KEY = new KeyMapping("key.cfhgui_server.admin", Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.cfh");
	public static final KeyMapping ACCEPT_KEY = new KeyMapping("key.cfhgui_server.accept", Type.KEYSYM, answerKey(89), "key.categories.cfh");
	public static final KeyMapping DENY_KEY = new KeyMapping("key.cfhgui_server.deny", Type.KEYSYM, answerKey(78), "key.categories.cfh");
	private static boolean initialised;
	@Nullable
	private static String pendingPage;
	@Nullable
	private static String pendingAdminPage;

	private static int answerKey(int key) {
		return CFHGuiPlatform.get().loader() == Loader.NEOFORGE ? key : InputConstants.UNKNOWN.getValue();
	}

	public static synchronized void init() {
		if (!initialised) {
			initialised = true;
			ServerClientConfig.file();
			CFHGuiKeys.register(HUB_KEY);
			CFHGuiKeys.register(ADMIN_KEY);
			CFHGuiKeys.register(ACCEPT_KEY);
			CFHGuiKeys.register(DENY_KEY);
			CoreActions.SCREEN_FACTORIES.put("cfhgui_server:hub", p -> {
				ServerUi.freshPreview();
				return MenuSlots.create("cfhgui_server:hub", p);
			});

			for (String page : PAGES) {
				CoreActions.SCREEN_FACTORIES.put("cfhgui_server:hub/" + page, p -> {
					ServerUi.freshPreview();
					pendingPage = page;
					return MenuSlots.create("cfhgui_server:hub", p);
				});
			}

			CoreActions.SCREEN_FACTORIES.put("cfhgui_server:admin", p -> {
				ServerUi.freshPreview();
				return MenuSlots.create("cfhgui_server:admin", p);
			});

			for (String page : ADMIN_PAGES) {
				CoreActions.SCREEN_FACTORIES.put("cfhgui_server:admin/" + page, p -> {
					ServerUi.freshPreview();
					pendingAdminPage = page;
					return MenuSlots.create("cfhgui_server:admin", p);
				});
			}

			CFHGuiEvents.CLIENT_TICK_END.register(ServerClient::tick);
			CFHGuiEvents.CLIENT_LEFT_SERVER.register((Runnable)() -> {
				ServerActions.reset();
				ServerState.disconnected();
			});
			ServerUi.init();
			ServerHud.init();
			ServerMaps.init();
			ServerStats.init();
			ServerState.ensurePreview();
			CFHGuiServer.LOGGER.info("[CFHGui Server] client ready");
		}
	}

	private static void tick() {
		if (ServerState.preview()) {
			SampleServer.tick();
		}

		while (HUB_KEY.consumeClick()) {
			if (Minecraft.getInstance().screen == null && !MenuSlots.available("cfhgui_server:hub").isEmpty()) {
				open(null);
			}
		}

		while (ADMIN_KEY.consumeClick()) {
			if (Minecraft.getInstance().screen == null && adminAvailable()) {
				openAdmin(null);
			}
		}
	}

	public static boolean hubAvailable() {
		return ServerState.available() && !MenuSlots.available("cfhgui_server:hub").isEmpty();
	}

	public static boolean adminAvailable() {
		return ServerState.may(Nodes.ADMIN_MENU) && !MenuSlots.available("cfhgui_server:admin").isEmpty();
	}

	public static void open(@Nullable String page) {
		ServerState.ensurePreview();
		pendingPage = page;
		MenuSlots.open("cfhgui_server:hub", Minecraft.getInstance().screen);
	}

	public static void openAdmin(@Nullable String page) {
		ServerState.ensurePreview();
		pendingAdminPage = page;
		MenuSlots.open("cfhgui_server:admin", Minecraft.getInstance().screen);
	}

	@Nullable
	public static String takePendingPage() {
		String p = pendingPage;
		pendingPage = null;
		return p;
	}

	@Nullable
	public static String takePendingAdminPage() {
		String p = pendingAdminPage;
		pendingAdminPage = null;
		return p;
	}

	public static void onOpen(ServerNet.Open p) {
		Minecraft mc = Minecraft.getInstance();
		String arg = p.arg().isEmpty() ? null : p.arg();
		String var3 = p.what();
		switch (var3) {
			case "hub":
				if (hubAvailable()) {
					open(arg != null && PAGES.contains(arg) ? arg : null);
				}
				break;
			case "rules":
				if (hubAvailable()) {
					open("info");
				}
				break;
			case "admin":
				if (adminAvailable()) {
					openAdmin(arg != null && ADMIN_PAGES.contains(arg) ? arg : null);
				}
				break;
			case "stats":
				UUID who = null;
				if (arg != null) {
					try {
						who = UUID.fromString(arg);
					} catch (IllegalArgumentException var7) {
						who = null;
					}
				}

				openStats(who);
				break;
			default:
				CFHGuiServer.LOGGER.debug("[CFHGui Server] the server asked for a screen this client does not know: {}", p.what());
		}
	}

	public static void openStats(@Nullable UUID who) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			StatsLink.focus(who != null && !who.equals(ServerState.selfId()) ? who : null);
			mc.setScreen(new StatsScreen(mc.screen, mc.player.getStats()));
		}
	}

	public static List<HubEntry> hubEntries() {
		List<HubEntry> out = new ArrayList<>();
		if (hubAvailable()) {
			out.add(new HubEntry(Component.translatable("cfhgui_server.hub.menu"), Icon.HOME, () -> open(null)));
		}

		if (adminAvailable()) {
			out.add(new HubEntry(Component.translatable("cfhgui_server.hub.admin"), Icon.SHIELD, () -> openAdmin(null)));
		}

		return out;
	}

	public static List<ActionType> actions() {
		return List.of(
			new ActionType(
				"cfhgui_server:open",
				Component.translatable("cfhgui_server.action.open"),
				List.of(Arg.choice("page", Component.translatable("cfhgui_server.action.arg.page"), "homes", PAGES)),
				a -> open(a.getOrDefault("page", "homes"))
			),
			new ActionType(
				"cfhgui_server:open_admin",
				Component.translatable("cfhgui_server.action.open_admin"),
				List.of(Arg.choice("page", Component.translatable("cfhgui_server.action.arg.page"), "dashboard", ADMIN_PAGES)),
				a -> openAdmin(a.getOrDefault("page", "dashboard"))
			)
		);
	}

	private ServerClient() {
	}
}
