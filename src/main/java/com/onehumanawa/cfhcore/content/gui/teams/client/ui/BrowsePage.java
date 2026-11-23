package com.onehumanawa.cfhcore.content.gui.teams.client.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiDropdown;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiList;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSearchField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiList.RowRenderer;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamFlows;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsDraw;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamSummary;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class BrowsePage extends TeamsPage {
	private static BrowsePage.Sort sort = BrowsePage.Sort.QUESTS;
	private String query = "";
	private CFHGuiList<TeamSummary> list;

	BrowsePage(TeamsHubScreen screen) {
		super(screen, "browse", Icon.SEARCH);
	}

	@Override
	protected void buildPage(SidebarScreen s, Rect area) {
		int sortW = Math.min(112, Math.max(84, area.w() / 3));
		CFHGuiSearchField search = new CFHGuiSearchField(area.x(), area.y(), area.w() - sortW - 6, q -> {
			this.query = q;
			this.refresh();
		});
		search.placeholder(TeamsText.t("browse.search"));
		if (!this.query.isEmpty()) {
			search.setValue(this.query);
		}

		s.addPageWidget(search);
		s.addPageWidget(
			new CFHGuiDropdown<>(
					area.x() + area.w() - sortW,
					area.y(),
					sortW,
					List.of(BrowsePage.Sort.values()),
					sort,
					v -> TeamsText.t("browse.sort." + v.name().toLowerCase(Locale.ROOT)),
					v -> {
						sort = v;
						this.refresh();
					}
				)
				.tip(TeamsText.t("browse.sort"))
		);
		this.list = (CFHGuiList<TeamSummary>)s.addPageWidget(new CFHGuiList(area.x(), area.y() + 26, area.w(), area.h() - 26, 34, new BrowsePage.Row()).gap(2));
		this.refresh();
	}

	@Override
	void stateChanged() {
		if (this.list == null) {
			super.stateChanged();
		} else {
			this.refresh();
		}
	}

	private void refresh() {
		String q = this.query.trim().toLowerCase(Locale.ROOT);
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
		this.list.items(shown);
		this.list.emptyText(TeamsText.t(TeamsView.directory().isEmpty() ? "browse.empty" : "browse.no_match"));
	}

	private final class Row implements RowRenderer<TeamSummary> {
		private static final int BUTTON_W = 58;
		private static final int BUTTON_H = 18;

		private boolean overButton(int x, int y, int w, int h, double mx, double my) {
			return mx >= (double)(x + w - 58 - 6) && mx < (double)(x + w - 6) && my >= (double)(y + (h - 18) / 2) && my < (double)(y + (h + 18) / 2);
		}

		public void render(GuiGraphics g, TeamSummary s, int index, int x, int y, int w, int h, boolean hovered, boolean selected, int mouseX, int mouseY) {
			Theme th = Theme.current();
			Palette p = th.palette();
			int ink = TeamsDraw.ink(s.color);
			TeamFlows.Join state = BrowsePage.this.flows().joinState(s);
			int tx = x + 6;
			if (BrowsePage.sort != BrowsePage.Sort.NAME && BrowsePage.this.query.isBlank()) {
				Component place = Component.literal(Integer.toString(index + 1));
				TeamsDraw.textCentered(g, place, x + 11, y + 13, index < 3 ? p.warning() : p.textDim(), 1.0F);
				tx = x + 22;
			}

			TeamsDraw.bannerSmall(g, s.banner, tx, y + 5, 12, 1.0F);
			tx += 18;
			int right = x + w - 58 - 12;
			Component name = Fonts.heading(s.name);
			int room = right - tx;
			int used = CFHGuiDraw.width(CFHGuiDraw.truncate(name, room));
			g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(name, room), tx, y + 6, ink, th.isVanilla());
			if (!s.tag.isEmpty() && used + CFHGuiDraw.width(s.tag) + 16 < room) {
				TeamsDraw.chip(g, Component.literal(s.tag), tx + used + 5, y + 4, ink, 1.0F);
			}

			Component line = TeamsText.t("browse.line", s.level, s.members, s.claims, s.quests);
			TeamsDraw.textFit(g, line, tx, y + 20, room, p.textMuted(), 1.0F);
			Component label = BrowsePage.this.flows().joinLabel(state);
			int bx = x + w - 58 - 6;
			int by = y + (h - 18) / 2;
			boolean hot = hovered && this.overButton(x, y, w, h, (double)mouseX, (double)mouseY);
			if (label != null) {
				TeamsDraw.rowButton(g, bx, by, 58, 18, label, state != TeamFlows.Join.WITHDRAW, hot, 1.0F);
			} else if (state == TeamFlows.Join.OWN) {
				TeamsDraw.chipRight(g, TeamsText.t("browse.yours"), x + w - 6, y + (h - 12) / 2, ink, 1.0F);
			} else {
				Component why = TeamsText.privacy(s.privacy);
				int ww = CFHGuiDraw.width(why);
				if (state == TeamFlows.Join.CLOSED) {
					Icons.draw(g, Icon.LOCK, x + w - 8 - ww - 11, y + (h - 8) / 2, 8, p.textDim());
				}

				TeamsDraw.textRight(g, (Component)(state == TeamFlows.Join.FULL ? TeamsText.t("browse.full.short") : why), x + w - 8, y + (h - 8) / 2, p.textDim(), 1.0F);
			}

			if (hovered) {
				List<Component> tip = new ArrayList<>();
				if (hot && label != null) {
					tip.add(TeamsText.t(switch (state) {
						case JOIN -> "browse.join.desc";
						case ASK -> "browse.ask.desc";
						case WITHDRAW -> "browse.withdraw.desc";
						default -> "browse.accept.desc";
					}, s.name));
				} else {
					tip.add(Component.literal(s.name).withStyle(st -> st.withColor(ink & 16777215)));
					if (!s.motto.isEmpty()) {
						tip.add(Component.literal(s.motto).withStyle(new ChatFormatting[]{ChatFormatting.ITALIC, ChatFormatting.GRAY}));
					}

					tip.add(TeamsText.t("browse.tip.owner", s.ownerName, TeamsText.ago(s.createdAt)).withStyle(ChatFormatting.GRAY));
					tip.add(TeamsText.t("browse.tip.online", s.online, s.members).withStyle(ChatFormatting.GRAY));
					tip.add(Component.empty().append(TeamsText.privacy(s.privacy)).append(": ").append(TeamsText.privacyHint(s.privacy)).withStyle(ChatFormatting.GRAY));
					Component why = BrowsePage.this.flows().joinWhyNot(state);
					if (why != null && state != TeamFlows.Join.OWN) {
						tip.add(why.copy().withStyle(new ChatFormatting[]{ChatFormatting.ITALIC, ChatFormatting.GRAY}));
					}
				}

				CFHGuiTooltips.request(tip, BrowsePage.this.list);
			}
		}

		public boolean click(TeamSummary s, int index, int x, int y, int w, int h, double mouseX, double mouseY, int button) {
			if (button == 0 && this.overButton(x, y, w, h, mouseX, mouseY)) {
				if (BrowsePage.this.flows().joinLabel(BrowsePage.this.flows().joinState(s)) == null) {
					return false;
				} else {
					CFHGuiSounds.click();
					BrowsePage.this.flows().join(s);
					return true;
				}
			} else {
				return false;
			}
		}
	}

	static enum Sort {
		QUESTS,
		LEVEL,
		MEMBERS,
		LAND,
		NAME;
	}
}
