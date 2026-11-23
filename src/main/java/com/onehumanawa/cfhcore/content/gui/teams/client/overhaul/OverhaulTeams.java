package com.onehumanawa.cfhcore.content.gui.teams.client.overhaul;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.screen.ScreenIds;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Layout;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsState;
import com.onehumanawa.cfhcore.content.gui.teams.model.Rank;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamSummary;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

public final class OverhaulTeams {
	public static void init() {
		ScreenIds.register(OverhaulTeamsScreen.class, "cfhgui_teams:hub_overhaul", "Teams (overhaul layout)");
		MenuSlots.provide("cfhgui_teams:hub", Layout.OVERHAUL, OverhaulTeamsScreen::new);
		if (CFHGuiPlatform.get().isDevelopmentEnvironment() || System.getProperty("cfh.autoScreens") != null || System.getProperty("cfh.autoWorldScreens") != null
			)
		 {
			developmentScreens();
		}
	}

	static MutableComponent t(String key, Object... args) {
		return Component.translatable("cfhgui_teams.overhaul." + key, args);
	}

	private static void developmentScreens() {
		for (int level = 1; level <= 12; level++) {
			int l = level;
			CoreActions.SCREEN_FACTORIES.put("cfhgui_teams:camp/level/" + l, parent -> {
				TeamData t = TeamsState.team();
				if (TeamsState.preview() && t != null) {
					t.level = l;
					t.xp = (long)Math.round((float)TeamData.xpForLevel(l) * 0.31F);

					for (TeamSummary s : TeamsState.directory()) {
						if (s.id.equals(t.id)) {
							s.level = l;
						}
					}
				}

				return open(parent, "overview");
			});
		}

		CoreActions.SCREEN_FACTORIES.put("cfhgui_teams:camp/crowd", parent -> {
			TeamData t = TeamsState.team();
			if (TeamsState.preview() && t != null && t.members.size() < 12) {
				long now = System.currentTimeMillis();

				for (String name : new String[]{"OldHarrow", "Cindermoth", "Waxwing", "Pinebarrow", "Mirelark", "Dunmoss", "Thistlewick", "Greyfinch", "Hobblecreek"}) {
					TeamData.Member m = new TeamData.Member();
					m.id = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
					m.name = name;
					m.rank = Rank.MEMBER;
					m.joinedAt = now - 86400000L * (long)(2 + t.members.size());
					m.lastSeen = now - 3600000L * (long)t.members.size();
					t.members.add(m);
				}
			}

			return open(parent, "members");
		});
		CoreActions.SCREEN_FACTORIES.put("cfhgui_teams:table/drag", parent -> {
			TeamData t = TeamsState.team();
			int cx = t != null && t.home != null ? (int)Math.floor(t.home.x) >> 4 : 0;
			int cz = t != null && t.home != null ? (int)Math.floor(t.home.z) >> 4 : 0;
			LandTableView.pretendDrag(cx + 1, cz - 4, cx + 5, cz - 1);
			return open(parent, "land");
		});

		for (String skin : new String[]{"DARK", "VANILLA"}) {
			CoreActions.SCREEN_FACTORIES.put("cfhgui_teams:look/overhaul/" + skin.toLowerCase(Locale.ROOT), parent -> {
				CFHGui.config().setLayout(Layout.OVERHAUL);
				CFHGui.config().skin = skin;
				Theme.reload();
				Reskin.invalidate();
				return open(parent, "overview");
			});
		}
	}

	@Nullable
	private static Screen open(@Nullable Screen parent, String page) {
		Function<Screen, Screen> factory = (Function<Screen, Screen>)CoreActions.SCREEN_FACTORIES.get("cfhgui_teams:hub/" + page);
		return factory == null ? MenuSlots.create("cfhgui_teams:hub", parent) : factory.apply(parent);
	}

	private OverhaulTeams() {
	}
}
