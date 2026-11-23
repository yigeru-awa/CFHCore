package com.onehumanawa.cfhcore.content.gui.crafting.team.client;

import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsState;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.BannerArt;
import com.onehumanawa.cfhcore.content.gui.teams.model.Rank;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

final class TeamsView {
	@Nullable
	private static TeamData team(@Nullable UUID id) {
		TeamData t = TeamsState.team();
		return t == null || id != null && !id.equals(t.id) ? null : t;
	}

	static TeamsView.Look look(@Nullable UUID id) {
		TeamData t = team(id);
		return t == null ? new TeamsView.Look("", -2523042) : new TeamsView.Look(t.name, 0xFF000000 | t.color);
	}

	static boolean officer(@Nullable UUID id) {
		TeamData t = team(id);
		Rank rank = t == null ? null : t.rankOf(TeamsState.self());
		return rank != null && rank.atLeast(Rank.MODERATOR);
	}

	static boolean flag(GuiGraphics g, @Nullable UUID id, float x, float y, float w) {
		TeamData t = team(id);
		if (t == null) {
			return false;
		} else {
			BannerArt.flag(g, t.banner, x, y, w, 1.0F);
			return true;
		}
	}

	private TeamsView() {
	}

	static record Look(String name, int color) {
	}
}
