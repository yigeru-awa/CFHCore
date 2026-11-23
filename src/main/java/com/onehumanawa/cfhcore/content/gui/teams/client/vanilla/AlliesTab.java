package com.onehumanawa.cfhcore.content.gui.teams.client.vanilla;

import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamActions;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsDraw;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import com.onehumanawa.cfhcore.content.gui.teams.model.Permission;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamSummary;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

final class AlliesTab extends TeamsTab {
	@Nullable
	private RowList list;
	@Nullable
	private Button act;

	AlliesTab(VanillaTeamsScreen screen) {
		super(screen, "allies");
	}

	@Override
	protected void build() {
		this.list = ((RowList)this.add(new RowList(this.mc, 26))).onSelect(this::update).emptyText(TeamsText.t("allies.empty"));
		TeamData t = TeamsView.team();
		boolean may = TeamsView.may(Permission.ALLIES);
		boolean room = t != null && t.allies.size() < TeamsView.info().maxAllies;
		enable(
			this.footButton(TeamsText.t("allies.declare"), () -> this.flows().allyPick()),
			may && room,
			may ? TeamsText.shared("result.ally_limit") : TeamsText.t("allies.rank", TeamsText.rank(Permission.ALLIES.min()))
		);
		this.act = this.footButton(TeamsText.t("action.unally"), this::act);
		this.fill();
	}

	private void fill() {
		TeamData t = TeamsView.team();
		if (t != null && this.list != null) {
			RowList l = this.list;
			Palette p = Theme.current().palette();
			List<RowList.Row> rows = new ArrayList<>();
			if (!t.allies.isEmpty()) {
				rows.add(l.caption("ours", TeamsText.t("allies.ours", t.allies.size())));
			}

			for (UUID id : t.allies) {
				TeamSummary s = TeamsView.summary(id);
				if (s == null) {
					rows.add(
						l.row(
							"ally:" + id,
							new AlliesTab.Gone(id),
							TeamsText.t("map.unknown_team"),
							(g, x, y, w, h, hovered, selected, mouseX, mouseY) -> TeamsDraw.text(
									g, TeamsText.t("map.unknown_team"), x + 24, y + (h - 8) / 2, Theme.current().palette().textMuted(), 1.0F
								)
						)
					);
				} else {
					boolean mutual = TeamsView.relation(s) == TeamsView.Relation.MUTUAL;
					rows.add(
						l.row(
							"ally:" + id,
							s,
							Component.literal(s.name),
							(g, x, y, w, h, hovered, selected, mouseX, mouseY) -> Rows.team(
									g,
									s,
									TeamsText.t(mutual ? "allies.mutual" : "allies.one_way"),
									TeamsText.t(mutual ? "vanilla.mutual" : "vanilla.one_way"),
									mutual ? Theme.current().palette().success() : Theme.current().palette().textMuted(),
									x,
									y,
									w,
									h
								)
						)
					);
				}
			}

			List<TeamSummary> theirs = new ArrayList<>();

			for (TeamSummary s : TeamsView.directory()) {
				if (TeamsView.relation(s) == TeamsView.Relation.THEIRS) {
					theirs.add(s);
				}
			}

			if (!theirs.isEmpty()) {
				rows.add(l.caption("theirs", TeamsText.t("allies.theirs", theirs.size())));
			}

			for (TeamSummary sx : theirs) {
				rows.add(
					l.row(
						"their:" + sx.id,
						sx,
						Component.literal(sx.name),
						(g, x, y, w, h, hovered, selected, mouseX, mouseY) -> Rows.team(g, sx, TeamsText.t("allies.waiting"), null, p.textMuted(), x, y, w, h)
					)
				);
			}

			l.rows(rows);
		}
	}

	private void update() {
		if (this.list != null && this.act != null) {
			boolean may = TeamsView.may(Permission.ALLIES);
			TeamSummary s = this.list.chosen(TeamSummary.class);
			boolean theirs = s != null && TeamsView.relation(s) == TeamsView.Relation.THEIRS;
			this.act.setMessage(TeamsText.t(theirs ? "allies.back" : "action.unally"));
			this.act.active = may && (s != null || this.list.chosen(AlliesTab.Gone.class) != null);
		}
	}

	private void act() {
		if (this.list != null) {
			TeamSummary s = this.list.chosen(TeamSummary.class);
			AlliesTab.Gone gone = this.list.chosen(AlliesTab.Gone.class);
			if (gone != null) {
				TeamActions.unally(gone.id());
			} else {
				if (s == null) {
					return;
				}

				if (TeamsView.relation(s) == TeamsView.Relation.THEIRS) {
					this.flows().ally(s);
				} else {
					this.flows().unally(s);
				}
			}
		}
	}

	@Override
	void refresh() {
		if (this.list != null && TeamsView.hasTeam()) {
			this.fill();
		} else {
			super.refresh();
		}
	}

	private List<FormattedCharSequence> note(ScreenRectangle area) {
		List<FormattedCharSequence> lines = this.mc.font.split(TeamsText.t("vanilla.allies.note"), area.width() - 20);
		return lines.size() > 2 ? lines.subList(0, 2) : lines;
	}

	@Override
	protected void layout(ScreenRectangle area) {
		if (this.list != null) {
			this.list.place(area.left(), area.top(), area.width(), area.height() - 12 - this.note(area).size() * 10);
		}
	}

	@Override
	void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		if (this.built() && this.list != null) {
			Ground.rule(g, this.area.left(), this.list.getBottom(), this.area.width(), false);
			List<FormattedCharSequence> lines = this.note(this.area);
			int y = this.area.bottom() - 4 - lines.size() * 10;

			for (FormattedCharSequence l : lines) {
				TeamsDraw.text(g, l, this.area.left() + (this.area.width() - this.mc.font.width(l)) / 2, y, Theme.current().palette().textMuted(), 1.0F);
				y += 10;
			}
		}
	}

	private static record Gone(UUID id) {
	}
}
