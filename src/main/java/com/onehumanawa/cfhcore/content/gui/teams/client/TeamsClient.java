package com.onehumanawa.cfhcore.content.gui.teams.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.client.invrow.InventoryRow;
import com.onehumanawa.cfhcore.content.gui.core.client.invrow.InventoryRow.Entry;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiKeys;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.action.ActionType;
import com.onehumanawa.cfhcore.content.gui.core.layout.action.ActionType.Arg;
import com.onehumanawa.cfhcore.content.gui.core.module.CFHGuiModule.HubEntry;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.teams.CFHGuiTeams;
import com.onehumanawa.cfhcore.content.gui.teams.TeamsConfig;
import com.onehumanawa.cfhcore.content.gui.teams.client.hud.TeamsHud;
import com.onehumanawa.cfhcore.content.gui.teams.client.overhaul.OverhaulTeams;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.TeamsUi;
import com.onehumanawa.cfhcore.content.gui.teams.client.vanilla.VanillaTeams;
import com.onehumanawa.cfhcore.content.gui.teams.compat.TeamsCompat;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class TeamsClient {
	public static final String OVERVIEW = "overview";
	public static final String MEMBERS = "members";
	public static final String LAND = "land";
	public static final String ALLIES = "allies";
	public static final String WAYPOINTS = "waypoints";
	public static final String BROWSE = "browse";
	public static final String SETTINGS = "settings";
	public static final List<String> PAGES = List.of("overview", "members", "land", "allies", "waypoints", "browse", "settings");
	public static final KeyMapping HUB_KEY = new KeyMapping("key.cfhgui_teams.hub", Type.KEYSYM, 75, "key.categories.cfh");
	public static final KeyMapping PING_KEY = new KeyMapping("key.cfhgui_teams.ping", Type.KEYSYM, 88, "key.categories.cfh");
	public static final KeyMapping CHAT_KEY = new KeyMapping("key.cfhgui_teams.chat", Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.cfh");
	private static boolean initialised;
	@Nullable
	private static String pendingPage;

	public static synchronized void init() {
		if (!initialised) {
			initialised = true;
			TeamsConfig.file();
			CFHGuiKeys.register(HUB_KEY);
			CFHGuiKeys.register(PING_KEY);
			CFHGuiKeys.register(CHAT_KEY);
			CoreActions.SCREEN_FACTORIES.put("cfhgui_teams:hub", p -> MenuSlots.create("cfhgui_teams:hub", p));

			for (String page : PAGES) {
				CoreActions.SCREEN_FACTORIES.put("cfhgui_teams:hub/" + page, p -> {
					pendingPage = page;
					return MenuSlots.create("cfhgui_teams:hub", p);
				});
			}

			CFHGuiEvents.CLIENT_TICK_END.register(TeamsClient::tick);
			CFHGuiEvents.CLIENT_LEFT_SERVER.register((Runnable)() -> {
				TeamActions.reset();
				TeamsState.disconnected();
			});
			TeamsUi.init();
			VanillaTeams.init();
			OverhaulTeams.init();
			TeamsHud.init();
			InventoryRow.register(
				new Entry(
					"cfhgui_teams:hub",
					Icon.TEAM,
					() -> Component.translatable("cfhgui_teams.hub.team"),
					() -> HUB_KEY,
					400,
					() -> TeamsConfig.get().inventoryButton && TeamsState.available(),
					() -> open(null),
					null
				)
			);
			TeamsCompat.init();
			TeamsState.ensurePreview();
			CFHGuiTeams.LOGGER.info("[CFHGui Teams] client ready");
		}
	}

	private static void tick() {
		while (HUB_KEY.consumeClick()) {
			if (Minecraft.getInstance().screen == null) {
				open(null);
			}
		}
	}

	public static void open(@Nullable String page) {
		pendingPage = page;
		MenuSlots.open("cfhgui_teams:hub", Minecraft.getInstance().screen);
	}

	@Nullable
	public static String takePendingPage() {
		String p = pendingPage;
		pendingPage = null;
		return p;
	}

	public static List<HubEntry> hubEntries() {
		return List.of(
			new HubEntry(Component.translatable("cfhgui_teams.hub.team"), Icon.TEAM, () -> open("overview")),
			new HubEntry(Component.translatable("cfhgui_teams.hub.land"), Icon.MAP, () -> open("land"))
		);
	}

	public static List<ActionType> actions() {
		return List.of(
			new ActionType(
				"cfhgui_teams:open",
				Component.translatable("cfhgui_teams.action.open"),
				List.of(Arg.choice("page", Component.translatable("cfhgui_teams.action.arg.page"), "overview", PAGES)),
				a -> open(a.getOrDefault("page", "overview"))
			)
		);
	}

	private TeamsClient() {
	}
}
