package com.onehumanawa.cfhcore.content.gui.teams.client.ui;

import com.onehumanawa.cfhcore.content.gui.config.CFHGuiConfigApi;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.screen.ScreenIds;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Layout;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette.AccentPreset;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsClient;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsState;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamFlows;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.map.TerrainTiles;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.settings.TeamsSettingsTab;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamSummary;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public final class TeamsUi {
	@Nullable
	private static Consumer<TeamFlows> pendingDialog;

	public static void init() {
		ScreenIds.register(TeamsHubScreen.class, "cfhgui_teams:hub", "Teams");
		MenuSlots.provide("cfhgui_teams:hub", Layout.CUSTOM, TeamsHubScreen::new);
		CFHGuiEvents.CLIENT_TICK_END.register(TerrainTiles::tick);
		if (CFHGuiPlatform.get().isModLoaded("cfhgui_config")) {
			TeamsUi.ConfigSide.init();
		}

		if (CFHGuiPlatform.get().isDevelopmentEnvironment() || System.getProperty("cfh.autoScreens") != null || System.getProperty("cfh.autoWorldScreens") != null
			)
		 {
			developmentScreens();
		}
	}

	@Nullable
	public static Consumer<TeamFlows> takePendingDialog() {
		Consumer<TeamFlows> d = pendingDialog;
		pendingDialog = null;
		return d;
	}

	private static void developmentScreens() {
		for (String page : TeamsClient.PAGES) {
			state("cfhgui_teams:state/noteam/" + page, TeamsView.Pretend.NO_TEAM, page);
			state("cfhgui_teams:state/noserver/" + page, TeamsView.Pretend.NO_SERVER, page);
		}

		state("cfhgui_teams:state/noteam", TeamsView.Pretend.NO_TEAM, "overview");
		state("cfhgui_teams:state/noserver", TeamsView.Pretend.NO_SERVER, "overview");
		dialog("create", TeamsView.Pretend.NO_TEAM, "overview", flows -> flows.dialogs().foundTeam());
		dialog("invite", TeamsView.Pretend.NOTHING, "members", flows -> flows.dialogs().invite());
		dialog("waypoint", TeamsView.Pretend.NOTHING, "waypoints", flows -> flows.dialogs().waypoint(null));
		dialog("waypoint_edit", TeamsView.Pretend.NOTHING, "waypoints", flows -> {
			TeamData t = TeamsView.team();
			if (t != null && !t.waypoints.isEmpty()) {
				flows.dialogs().waypoint(t.waypoints.get(0));
			}
		});
		dialog("banner", TeamsView.Pretend.NOTHING, "settings", flows -> {
			TeamData t = TeamsView.team();
			if (t != null) {
				flows.dialogs().banner(t.banner, banner -> {
				});
			}
		});
		dialog("kick", TeamsView.Pretend.NOTHING, "members", flows -> {
			TeamData t = TeamsView.team();
			if (t != null && t.members.size() > 1) {
				flows.kick(t.members.get(t.members.size() - 1));
			}
		});
		dialog("disband", TeamsView.Pretend.NOTHING, "settings", TeamFlows::disband);
		dialog("ally", TeamsView.Pretend.NOTHING, "allies", TeamFlows::allyPick);
		dialog("transfer", TeamsView.Pretend.NOTHING, "settings", TeamFlows::transferPick);

		for (Layout layout : new Layout[]{Layout.CUSTOM, Layout.VANILLA}) {
			for (String skin : new String[]{"DARK", "VANILLA"}) {
				CoreActions.SCREEN_FACTORIES.put("cfhgui_teams:look/" + layout.name().toLowerCase(Locale.ROOT) + "/" + skin.toLowerCase(Locale.ROOT), parent -> {
					CFHGui.config().setLayout(layout);
					CFHGui.config().skin = skin;
					Theme.reload();
					Reskin.invalidate();
					return open(parent, "overview");
				});
			}
		}

		Map<String, Integer> colours = Map.of(
			"navy", 1055580, "black", 657930, "white", 16777215, "sample", ((AccentPreset)Palette.ACCENTS.get(0)).color() & 16777215
		);
		colours.forEach((name, rgb) -> CoreActions.SCREEN_FACTORIES.put("cfhgui_teams:look/colour/" + name, parent -> {
				TeamData t = TeamsState.team();
				if (TeamsState.preview() && t != null) {
					t.color = rgb;

					for (TeamSummary s : TeamsState.directory()) {
						if (s.id.equals(t.id)) {
							s.color = rgb;
						}
					}
				}

				return open(parent, "overview");
			}));

		for (int scale = 0; scale <= 4; scale++) {
			int s = scale;
			CoreActions.SCREEN_FACTORIES.put("cfhgui_teams:look/scale/" + s, parent -> {
				Minecraft mc = Minecraft.getInstance();
				mc.options.guiScale().set(s);
				mc.resizeDisplay();
				return open(parent, "overview");
			});
		}
	}

	private static void state(String id, TeamsView.Pretend pretend, String page) {
		CoreActions.SCREEN_FACTORIES.put(id, parent -> {
			TeamsView.pretendNext(pretend);
			return open(parent, page);
		});
	}

	private static void dialog(String name, TeamsView.Pretend pretend, String page, Consumer<TeamFlows> open) {
		CoreActions.SCREEN_FACTORIES.put("cfhgui_teams:dialog/" + name, parent -> {
			TeamsView.pretendNext(pretend);
			pendingDialog = open;
			return open(parent, page);
		});
	}

	private static Screen open(@Nullable Screen parent, String page) {
		Function<Screen, Screen> factory = (Function<Screen, Screen>)CoreActions.SCREEN_FACTORIES.get("cfhgui_teams:hub/" + page);
		return factory == null ? MenuSlots.create("cfhgui_teams:hub", parent) : factory.apply(parent);
	}

	private TeamsUi() {
	}

	private static final class ConfigSide {
		static void init() {
			TeamsSettingsTab.register();
			CoreActions.SCREEN_FACTORIES.put("cfhgui_teams:config", parent -> CFHGuiConfigApi.hub(parent, "gameplay/teams"));
		}
	}
}
