package com.onehumanawa.cfhcore.content.gui.teams.client.vanilla;

import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamFlows;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamSummary;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class BrowseTab extends TeamsTab {
	private static BrowseTab.Sort sort = BrowseTab.Sort.QUESTS;
	private static String query = "";
	@Nullable
	private RowList list;
	@Nullable
	private EditBox search;
	@Nullable
	private CycleButton<BrowseTab.Sort> order;
	@Nullable
	private Button join;

	BrowseTab(VanillaTeamsScreen screen) {
		super(screen, "browse");
	}

	@Override
	protected boolean needsTeam() {
		return false;
	}

	@Override
	protected void build() {
		this.search = this.add(new EditBox(this.mc.font, 0, 0, 160, 20, TeamsText.t("browse.search")));
		this.search.setHint(TeamsText.t("browse.search"));
		this.search.setValue(query);
		this.search.setResponder(q -> {
			query = q;
			this.fill();
		});
		this.order = this.add(
			CycleButton.<BrowseTab.Sort>builder(s -> TeamsText.t("browse.sort." + s.name().toLowerCase(Locale.ROOT)))
				.withValues(List.of(BrowseTab.Sort.values()))
				.withInitialValue(sort)
				.create(0, 0, 150, 20, TeamsText.t("vanilla.order"), (button, value) -> {
					sort = value;
					this.fill();
				})
		);
		this.list = ((RowList)this.add(new RowList(this.mc, 26))).onSelect(this::update);
		this.list.onActivate(row -> this.act());
		this.join = this.footButton(TeamsText.t("browse.withdraw"), this::act);
		this.fill();
	}

	private void fill() {
		if (this.list != null) {
			RowList l = this.list;
			String q = query.trim().toLowerCase(Locale.ROOT);
			List<TeamSummary> shown = new ArrayList<>();

			for (TeamSummary s : TeamsView.directory()) {
				if (q.isEmpty()
					|| s.name.toLowerCase(Locale.ROOT).contains(q)
					|| s.tag.toLowerCase(Locale.ROOT).contains(q)
					|| s.ownerName.toLowerCase(Locale.ROOT).contains(q)) {
					shown.add(s);
				}
			}

			Comparator<TeamSummary> byName = Comparator.comparing(sx -> sx.name, String.CASE_INSENSITIVE_ORDER);

			shown.sort(switch (sort) {
				case QUESTS -> Comparator.<TeamSummary>comparingLong(sx -> -sx.quests).thenComparing(byName);
				case LEVEL -> Comparator.<TeamSummary>comparingInt(sx -> -sx.level).thenComparing(byName);
				case MEMBERS -> Comparator.<TeamSummary>comparingInt(sx -> -sx.members).thenComparing(byName);
				case LAND -> Comparator.<TeamSummary>comparingInt(sx -> -sx.claims).thenComparing(byName);
				case NAME -> byName;
			});
			List<RowList.Row> rows = new ArrayList<>();

			for (TeamSummary sx : shown) {
				rows.add(
					l.row(
						"team:" + sx.id,
						sx,
						Component.literal(sx.name),
						(g, x, y, w, h, hovered, selected, mouseX, mouseY) -> {
							Palette p = Theme.current().palette();
							TeamFlows.Join state = this.flows().joinState(sx);
							Component right = (Component)(state == TeamFlows.Join.OWN
								? TeamsText.t("browse.yours")
								: (
									state == TeamFlows.Join.WITHDRAW
										? TeamsText.t("vanilla.asked")
										: (
											state == TeamFlows.Join.INVITED
												? TeamsText.t("vanilla.invited")
												: (state == TeamFlows.Join.FULL ? TeamsText.t("browse.full.short") : TeamsText.privacy(sx.privacy))
										)
								));
							Rows.team(
								g,
								sx,
								TeamsText.t("browse.line", sx.level, sx.members, sx.claims, sx.quests),
								right,
								state != TeamFlows.Join.OWN && state != TeamFlows.Join.INVITED ? p.textMuted() : p.text(),
								x,
								y,
								w,
								h
							);
						}
					)
				);
			}

			l.emptyText(TeamsText.t(TeamsView.directory().isEmpty() ? "browse.empty" : "browse.no_match"));
			l.rows(rows);
		}
	}

	private void update() {
		if (this.list != null && this.join != null) {
			TeamSummary s = this.list.chosen(TeamSummary.class);
			TeamFlows.Join state = s == null ? null : this.flows().joinState(s);
			Component label = state == null ? null : this.flows().joinLabel(state);
			this.join.setMessage((Component)(label == null ? TeamsText.t("browse.join") : label));
			this.join.active = label != null;
			Component why = state == null ? null : this.flows().joinWhyNot(state);
			this.join.setTooltip(why == null ? null : Tooltip.create(why));
		}
	}

	private void act() {
		TeamSummary s = this.list == null ? null : this.list.chosen(TeamSummary.class);
		if (s != null && this.flows().joinLabel(this.flows().joinState(s)) != null) {
			this.flows().join(s);
		}
	}

	@Override
	void refresh() {
		if (this.list != null) {
			this.fill();
		} else {
			super.refresh();
		}
	}

	@Override
	protected void layout(ScreenRectangle area) {
		if (this.list != null && this.search != null && this.order != null) {
			int orderW = Math.min(150, area.width() / 3);
			int searchW = Math.min(220, area.width() - orderW - 28);
			int total = searchW + 6 + orderW;
			int x = area.left() + (area.width() - total) / 2;
			this.search.setX(x);
			this.search.setY(area.top() + 6);
			this.search.setWidth(searchW);
			this.order.setX(x + searchW + 6);
			this.order.setY(area.top() + 6);
			this.order.setWidth(orderW);
			this.list.place(area.left(), area.top() + 34, area.width(), area.height() - 34);
		}
	}

	@Override
	void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		if (this.built() && this.list != null) {
			Ground.rule(g, this.area.left(), this.list.getY() - 2, this.area.width(), true);
		}
	}

	private static enum Sort {
		QUESTS,
		LEVEL,
		MEMBERS,
		LAND,
		NAME;
	}
}
