package com.onehumanawa.cfhcore.content.gui.server.client.ui.hub;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Node;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink.Board;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink.Row;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiList;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSearchField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSpinner;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerActions;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerClient;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerState;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.ServerPage;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.ServerScreen;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.StatWords;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Ui;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class RankingsPage extends ServerPage {
	private static final int ROWS = 50;
	private CFHGuiList<String> keys;
	private CFHGuiList<Row> rows;
	private String query = "";
	private String key = "";
	private int offset;
	@Nullable
	private Board board;
	private boolean loading;
	private int asked;
	private Rect boardArea = new Rect(0, 0, 0, 0);
	private CFHGuiIconButton prev;
	private CFHGuiIconButton next;

	RankingsPage(ServerScreen screen) {
		super(screen, "rankings", Txt.page("rankings"), Icon.TROPHY);
	}

	@Override
	protected String feature() {
		return "rankings";
	}

	@Override
	protected Node node() {
		return Nodes.RANKINGS_USE;
	}

	private List<String> allKeys() {
		Set<String> out = new LinkedHashSet<>(ServerState.hello().featuredRankings);
		out.addAll(StatWords.EXTRAS);
		out.addAll(StatWords.general());
		List<String> list = new ArrayList<>();

		for (String k : out) {
			if (this.query.isEmpty() || StatWords.name(k).getString().toLowerCase(Locale.ROOT).contains(this.query)) {
				list.add(k);
			}
		}

		return list;
	}

	@Override
	protected void buildPage(SidebarScreen s, Rect area) {
		this.keys = null;
		int leftW = Math.max(110, Math.min(190, area.w() * 32 / 100));
		Rect left = new Rect(area.x(), area.y(), leftW, area.h());
		this.boardArea = new Rect(area.x() + leftW + 8, area.y(), area.w() - leftW - 8, area.h());
		CFHGuiSearchField search = new CFHGuiSearchField(left.x(), left.y(), left.w(), q -> {
			String typed = q.trim().toLowerCase(Locale.ROOT);
			if (!typed.equals(this.query)) {
				this.query = typed;
				if (this.keys != null) {
					this.keys.items(this.allKeys());
					this.keys.select(this.key);
				}
			}
		});
		search.setValue(this.query);
		search.placeholder(Txt.t("rankings.search"));
		s.addPageWidget(search);
		int featured = ServerState.hello().featuredRankings.size();
		this.keys = new CFHGuiList<String>(
			left.x(),
			left.y() + 20 + 6,
			left.w(),
			left.h() - 20 - 6,
			18,
			(g, item, index, x, y, w, h, hovered, selected, mx, my) -> {
				Theme t = Theme.current();
				Palette p = t.palette();
				boolean star = this.query.isEmpty() && index < featured;
				Icons.draw(g, star ? Icon.STAR_FILLED : Icon.DOT, x + 5, y + 5, 8, selected ? p.accentInk() : (star ? p.warning() : p.textDim()));
				g.drawString(
					CFHGuiDraw.font(),
					CFHGuiDraw.truncate(StatWords.name(item), w - 22),
					x + 18,
					y + 5,
					selected ? Ui.strong() : (hovered ? p.text() : p.textMuted()),
					t.isVanilla()
				);
			}
		);
		this.keys.items(this.allKeys()).onSelect(this::choose).emptyText(Txt.t("rankings.no_match"));
		s.addPageWidget(this.keys);
		if (this.key.isEmpty() || !this.keys.items().contains(this.key)) {
			this.key = this.keys.items().isEmpty() ? "" : (String)this.keys.items().get(0);
		}

		this.keys.select(this.key);
		CFHGuiButton stats = new CFHGuiButton(0, 0, 100, 18, Txt.t("rankings.statistics"), () -> ServerClient.openStats(ServerState.selfId())).icon(Icon.HISTORY);
		stats.setWidth(stats.preferredWidth() + 12);
		stats.setX(this.boardArea.right() - stats.getWidth() - 6);
		stats.setY(this.boardArea.y() + 8);
		stats.enabled(Minecraft.getInstance().player != null);
		stats.tip(Txt.t(Minecraft.getInstance().player != null ? "rankings.statistics.tip" : "rankings.statistics.world"));
		s.addPageWidget(stats);
		int listTop = this.boardArea.y() + 40;
		int footH = 30;
		this.rows = new CFHGuiList<Row>(this.boardArea.x(), listTop, this.boardArea.w(), this.boardArea.bottom() - listTop - 30, 22, this::row);
		this.rows.onActivate(r -> ServerClient.openStats(r.player())).emptyText(Component.empty());
		s.addPageWidget(this.rows);
		this.prev = (CFHGuiIconButton)s.addPageWidget(
			new CFHGuiIconButton(this.boardArea.right() - 42, this.boardArea.bottom() - 22, 18, Icon.CHEVRON_LEFT, Txt.t("pager.prev"), () -> this.turn(-1))
		);
		this.next = (CFHGuiIconButton)s.addPageWidget(
			new CFHGuiIconButton(this.boardArea.right() - 20, this.boardArea.bottom() - 22, 18, Icon.CHEVRON_RIGHT, Txt.t("pager.next"), () -> this.turn(1))
		);
		if (this.board != null && this.board.key().equals(this.key)) {
			this.show(this.board);
		} else {
			this.ask();
		}
	}

	private void choose(String chosen) {
		if (chosen != null && !chosen.equals(this.key)) {
			this.key = chosen;
			this.offset = 0;
			this.ask();
		}
	}

	private void turn(int by) {
		int total = this.board == null ? 0 : this.board.total();
		int to = Math.max(0, Math.min(Math.max(0, (total - 1) / 50 * 50), this.offset + by * 50));
		if (to != this.offset) {
			this.offset = to;
			this.ask();
		}
	}

	private void ask() {
		if (this.key.isEmpty()) {
			this.board = null;
			this.rows.items(List.of());
		} else {
			this.loading = true;
			int request = ++this.asked;
			String wanted = this.key;
			ServerActions.board(wanted, this.offset, 50, answer -> {
				if (request == this.asked) {
					this.loading = false;
					this.show(answer.orElseGet(() -> new Board(wanted, List.of(), 0, null)));
				}
			});
		}
	}

	private void show(Board b) {
		this.board = b;
		if (this.rows != null) {
			this.rows.items(b.rows() == null ? List.of() : b.rows());
			this.rows.snapScroll(0.0);
		}
	}

	@Override
	public void stateChanged() {
	}

	private void row(GuiGraphics g, Row r, int index, int x, int y, int w, int h, boolean hovered, boolean selected, int mouseX, int mouseY) {
		this.line(g, r, x, y, w, h, r.player().equals(ServerState.selfId()), hovered, 1.0F);
	}

	private void line(GuiGraphics g, Row r, int x, int y, int w, int h, boolean own, boolean hot, float a) {
		Theme t = Theme.current();
		Palette p = t.palette();

		int medal = switch (r.rank()) {
			case 1 -> -1526982;
			case 2 -> -3617580;
			case 3 -> -3569584;
			default -> 0;
		};
		String place = "#" + r.rank();
		if (medal != 0) {
			CFHGuiDraw.pixelRound(g, x + 4, y + 4, 22, h - 8, Colors.scaleAlpha(Colors.withAlpha(medal, t.isVanilla() ? 96 : 56), a), 2);
			Ui.textCentered(g, Component.literal(place), x + 15, y + (h - 8) / 2, medal, a);
		} else {
			Ui.textCentered(g, Component.literal(place), x + 15, y + (h - 8) / 2, Ui.caption(), a);
		}

		Ui.head(g, r.player(), r.name(), x + 32, y + (h - 14) / 2, 14, null, a);
		Component value = Component.literal(StatWords.value(this.key, r.value()));
		int vw = CFHGuiDraw.width(value);
		Ui.text(g, value, x + w - vw - 8, y + (h - 8) / 2, own ? p.accentInk() : (hot ? Ui.strong() : p.textMuted()), a);
		Ui.textFit(g, Component.literal(r.name()), x + 52, y + (h - 8) / 2, w - 52 - vw - 16, own ? p.accentInk() : Ui.strong(), a);
	}

	public void render(SidebarScreen s, GuiGraphics g, Rect area, int mouseX, int mouseY, float partialTick) {
		if (this.rows != null && ServerState.available() && this.boardArea.w() > 0) {
			Theme t = Theme.current();
			Palette p = t.palette();
			Rect b = this.boardArea;
			Ui.card(g, b.x(), b.y(), b.w(), 34, 0.0F, 1.0F);
			if (!this.key.isEmpty()) {
				Ui.textFit(g, Fonts.heading(StatWords.name(this.key)), b.x() + 10, b.y() + 7, b.w() - 150, Ui.strong(), 1.0F);
				Component sub = this.loading
					? Txt.t("loading")
					: (this.board != null && this.board.total() != 0 ? Txt.t("rankings.ranked", Txt.number((long)this.board.total())) : Txt.t("rankings.nobody"));
				Ui.textFit(g, sub, b.x() + 10, b.y() + 19, b.w() - 150, p.textMuted(), 1.0F);
			}

			if (!this.loading || this.board != null && !this.board.rows().isEmpty()) {
				if (this.board != null && this.board.rows().isEmpty() && !this.loading) {
					Ui.empty(g, new Rect(b.x(), b.y() + 40, b.w(), b.h() - 70), Icon.TROPHY, Txt.t("rankings.empty.title"), Txt.t("rankings.empty"), 1.0F);
				}
			} else {
				CFHGuiSpinner.draw(g, b.centerX() - 6, b.y() + 80, 12, p.textMuted());
			}

			int fy = b.bottom() - 26;
			Ui.rule(g, b.x(), fy - 3, b.w(), 1.0F);
			int total = this.board == null ? 0 : this.board.total();
			boolean pages = total > 50;
			this.prev.visible = this.next.visible = pages;
			this.prev.active = this.offset > 0;
			this.next.active = this.offset + 50 < total;
			int room = b.w();
			if (pages) {
				Component range = Txt.t("pager.range", this.offset + 1, Math.min(total, this.offset + 50), Txt.number((long)total));
				Ui.textRight(g, range, b.right() - 48, fy + 8, Ui.caption(), 1.0F);
				room -= 56 + CFHGuiDraw.width(range);
			}

			if (this.board != null && this.board.self() != null) {
				this.line(g, this.board.self(), b.x(), fy, room, 24, true, false, 1.0F);
			} else if (this.board != null && !this.loading && total > 0) {
				Ui.textFit(g, Txt.t("rankings.self.none"), b.x() + 8, fy + 8, room - 12, Ui.caption(), 1.0F);
			}
		}
	}
}
