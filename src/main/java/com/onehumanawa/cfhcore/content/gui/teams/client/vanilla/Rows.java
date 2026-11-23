package com.onehumanawa.cfhcore.content.gui.teams.client.vanilla;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.teams.client.WaypointIcons;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsDraw;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import com.onehumanawa.cfhcore.content.gui.teams.model.Rank;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamSummary;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class Rows {
	static void twoLines(GuiGraphics g, int x, int y, int w, int h, Component title, int titleColor, Component line, @Nullable Component right, int rightColor) {
		Palette p = Theme.current().palette();
		int tx = x + 24;
		int rightW = right == null ? 0 : CFHGuiDraw.width(right) + 8;
		TeamsDraw.textFit(g, title, tx, y + 2, w - 28 - rightW, titleColor, 1.0F);
		TeamsDraw.textFit(g, line, tx, y + 12, w - 28 - rightW, p.textMuted(), 1.0F);
		if (right != null) {
			TeamsDraw.textRight(g, right, x + w - 4, y + (h - 8) / 2, rightColor, 1.0F);
		}
	}

	static void member(GuiGraphics g, TeamData.Member m, Component where, int x, int y, int w, int h) {
		Palette p = Theme.current().palette();
		boolean on = TeamsView.online(m.id);
		TeamsDraw.head(g, m.id, m.name, x + 3, y + (h - 16) / 2, 16, on, on ? 1.0F : 0.7F);
		Component name = m.id.equals(TeamsView.self()) ? Component.literal(m.name).append(" ").append(TeamsText.t("members.self")) : Component.literal(m.name);
		twoLines(g, x, y, w, h, name, on ? p.text() : p.textMuted(), where, TeamsText.rank(m.rank), m.rank == Rank.OWNER ? p.warning() : p.textMuted());
	}

	static void player(GuiGraphics g, UUID id, String name, Component line, @Nullable Component right, int x, int y, int w, int h) {
		Palette p = Theme.current().palette();
		TeamsDraw.head(g, id, name, x + 3, y + (h - 16) / 2, 16, null, 1.0F);
		twoLines(g, x, y, w, h, Component.literal(name), p.text(), line, right, p.textMuted());
	}

	static void team(GuiGraphics g, TeamSummary s, Component line, @Nullable Component right, int rightColor, int x, int y, int w, int h) {
		TeamsDraw.bannerSmall(g, s.banner, x + 5, y + (h - 20) / 2, 10, 1.0F);
		Component name = s.tag.isEmpty() ? Component.literal(s.name) : Component.literal(s.name + " " + TeamsText.tag(s.tag));
		twoLines(g, x, y, w, h, name, TeamsDraw.ink(s.color), line, right, rightColor);
	}

	static void waypoint(GuiGraphics g, TeamData.Waypoint p, Component line, Component right, boolean shown, int x, int y, int w, int h) {
		Palette pal = Theme.current().palette();
		TeamsDraw.glyphTile(g, WaypointIcons.get(p.icon), x + 2, y + (h - 18) / 2, 18, p.color, shown ? 1.0F : 0.5F);
		twoLines(g, x, y, w, h, Component.literal(p.name), shown ? pal.text() : pal.textMuted(), line, right, pal.textMuted());
		if (!shown) {
			Icons.draw(g, Icon.EYE_OFF, x + w - CFHGuiDraw.width(right) - 18, y + (h - 8) / 2, 8, pal.textDim());
		}
	}

	private Rows() {
	}
}
