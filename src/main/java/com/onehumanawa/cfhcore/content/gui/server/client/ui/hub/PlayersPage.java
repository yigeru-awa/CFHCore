package com.onehumanawa.cfhcore.content.gui.server.client.ui.hub;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSearchField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToggle;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiAvatar.Status;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerActions;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerClient;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerState;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.ServerPage;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.ServerScreen;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.dialog.Forms;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.PaintedCard;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Ui;
import com.onehumanawa.cfhcore.content.gui.server.model.OnlinePlayer;
import com.onehumanawa.cfhcore.content.gui.server.model.TeleportRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

final class PlayersPage extends ServerPage {
	private CFHGuiScrollPanel panel;
	private String query = "";

	PlayersPage(ServerScreen screen) {
		super(screen, "players", Txt.page("players"), Icon.FRIENDS);
	}

	public int badge() {
		int n = 0;

		for (TeleportRequest r : ServerState.requests()) {
			if (r.incoming) {
				n++;
			}
		}

		return n;
	}

	@Override
	protected void buildPage(SidebarScreen s, Rect area) {
		this.panel = null;
		ServerPage.Bar bar = new ServerPage.Bar(s, area);
		if (ServerState.feature("requests")) {
			CFHGuiToggle accept = new CFHGuiToggle(0, 0, 100, Txt.t("players.accept"), ServerState.self().acceptsRequests, ServerActions::tpaToggle);
			accept.setWidth(Minecraft.getInstance().font.width(Txt.t("players.accept")) + 36);
			accept.tip(Txt.t("players.accept.tip"));
			bar.right(accept);
		}

		CFHGuiSearchField search = bar.fill(new CFHGuiSearchField(0, 0, 160, q -> {
			String typed = q.trim().toLowerCase(Locale.ROOT);
			if (!typed.equals(this.query)) {
				this.query = typed;
				if (this.panel != null) {
					refill(this.panel, this::fill);
					this.panel.snapScroll(0.0);
				}
			}
		}), 220);
		search.setValue(this.query);
		search.placeholder(Txt.t("players.search"));
		Rect r = under(area);
		this.panel = (CFHGuiScrollPanel)s.addPageWidget(new CFHGuiScrollPanel(r.x(), r.y(), r.w(), r.h()));
		this.fill();
	}

	@Override
	public void stateChanged() {
		if (this.panel == null) {
			super.stateChanged();
		} else {
			refill(this.panel, this::fill);
		}
	}

	private void fill() {
		int w = this.panel.innerWidth();
		int y = 0;
		List<TeleportRequest> requests = new ArrayList<>(ServerState.requests());
		requests.sort(Comparator.<TeleportRequest, Boolean>comparing(r -> !r.incoming).thenComparingLong(r -> r.expiresAt));
		if (!requests.isEmpty()) {
			y = section(this.panel, y, w, Txt.t("players.requests", requests.size()));

			for (TeleportRequest r : requests) {
				this.panel.add(this.request(r, w), 0, y);
				y += 32;
			}

			y += 4;
		}

		UUID self = ServerState.selfId();
		List<OnlinePlayer> players = new ArrayList<>();

		for (OnlinePlayer p : ServerState.online()) {
			if (this.query.isEmpty() || p.name.toLowerCase(Locale.ROOT).contains(this.query) || Txt.plainNick(p.nick).toLowerCase(Locale.ROOT).contains(this.query)) {
				players.add(p);
			}
		}

		players.sort(
			Comparator.<OnlinePlayer, Boolean>comparing(px -> !px.id.equals(self)).thenComparing(px -> Txt.plainNick(px.shownName()), String.CASE_INSENSITIVE_ORDER)
		);
		y = section(this.panel, y, w, Txt.t("players.online", ServerState.online().size()));
		if (players.isEmpty()) {
			this.panel
				.add(
					new PaintedCard(
							w,
							90,
							(g, f) -> Ui.empty(
									g, new Rect(f.x(), f.y(), f.w(), f.h()), Icon.SEARCH, Txt.t("players.no_match.title"), Txt.t("players.no_match", this.query), f.alpha()
								)
						)
						.bare(),
					0,
					y
				);
			y += 90;
		}

		for (OnlinePlayer px : players) {
			this.panel.add(this.player(px, px.id.equals(self), w), 0, y);
			y += 30;
		}

		this.panel.setContentHeight(y + 2);
	}

	private PaintedCard request(TeleportRequest r, int w) {
		PaintedCard card = new PaintedCard(
				w,
				28,
				(g, f) -> {
					Palette p = Theme.current().palette();
					float a = f.alpha();
					Ui.wash(g, f.x() + 1, f.y() + 1, 100, f.h() - 2, (r.incoming ? p.accent() : p.textDim()) & 16777215, a);
					Ui.head(g, r.player, r.name, f.x() + 6, f.y() + 6, 16, null, a);
					long left = Math.max(0L, (r.expiresAt - System.currentTimeMillis()) / 1000L);
					int room = f.w() - 30 - (r.incoming ? 150 : 86);
					Component line = Txt.t(
						r.incoming ? (r.here ? "players.request.in_here" : "players.request.in") : (r.here ? "players.request.out_here" : "players.request.out"), r.name
					);
					int used = Ui.textFit(g, line, f.x() + 30, f.y() + 10, room - 40, Ui.strong(), a);
					if (left > 0L && used + 40 <= room) {
						Ui.chip(g, Component.literal(Txt.span(left)), f.x() + 36 + used, f.y() + 8, left <= 10L ? p.warning() : p.textMuted(), a);
					}
				}
			)
			.flat()
			.narrate(Component.literal(r.name));
		if (r.incoming) {
			card.add(
				new CFHGuiButton(0, 0, 70, 18, Txt.t("players.accept_one"), () -> ServerActions.tpaAccept(r.player)).variant(Variant.PRIMARY).icon(Icon.CHECK).iconSize(8),
				w - 136,
				5
			);
			card.add(new CFHGuiButton(0, 0, 56, 18, Txt.t("players.deny"), () -> ServerActions.tpaDeny(r.player)).variant(Variant.GHOST), w - 62, 5);
		} else {
			card.add(new CFHGuiButton(0, 0, 70, 18, Txt.t("players.take_back"), () -> ServerActions.tpaCancel(r.player)).variant(Variant.GHOST), w - 76, 5);
		}

		return card;
	}

	private PaintedCard player(OnlinePlayer who, boolean self, int w) {
		boolean requests = ServerState.feature("requests") && !self;
		boolean mayAsk = requests && ServerState.may(Nodes.TPA_USE);
		boolean mayAskHere = requests && ServerState.may(Nodes.TPA_HERE);
		boolean mail = !self && ServerState.feature("mail") && ServerState.may(Nodes.MAIL_USE);
		boolean stats = ServerState.feature("stats") && (self || ServerState.may(Nodes.STATS_OTHERS)) && Minecraft.getInstance().player != null;
		int buttons = (mayAsk ? 1 : 0) + (mayAskHere ? 1 : 0) + (mail ? 1 : 0) + (stats ? 1 : 0);
		boolean asked = ServerState.requests().stream().anyMatch(r -> !r.incoming && r.player.equals(who.id));
		Component shown = Txt.nick(who.shownName());
		String called = Txt.plainNick(who.shownName());
		PaintedCard card = new PaintedCard(
				w,
				26,
				(g, f) -> {
					Theme t = Theme.current();
					Palette p = t.palette();
					float a = f.alpha();
					Ui.head(g, who.id, who.name, f.x() + 6, f.y() + 5, 16, who.afk ? Status.AWAY : Status.ONLINE, a * (who.afk ? 0.75F : 1.0F));
					int tx = f.x() + 30;
					int right = f.x() + f.w() - 8 - buttons * 20;
					if (self) {
						right = Ui.chipRight(g, Txt.t("players.you"), right, f.y() + 7, p.textMuted(), a) - 4;
					}

					if (who.vanished) {
						right = Ui.chipRight(g, Txt.t("flag.vanished"), right, f.y() + 7, p.textMuted(), a) - 4;
					}

					if (who.afk) {
						right = Ui.chipRight(g, Txt.t("flag.afk"), right, f.y() + 7, p.warning(), a) - 4;
					}

					if (who.role != null && !who.role.name.isEmpty()) {
						right -= Ui.roleChipWidth(who.role);
						Ui.roleChip(g, who.role, right, f.y() + 7, a);
						right -= 4;
					}

					int room = right - tx - 4;
					int nameColor = who.role != null && !t.isVanilla() ? Ui.ink(who.role.color) : Ui.strong();
					Ui.textFit(g, shown, tx, f.y() + 4, room, who.afk ? Ui.soft() : nameColor, a);
					Component sub = who.nick.isEmpty() ? Txt.t("players.since", Txt.since(who.joinedAt)) : Txt.t("players.since.nick", who.name, Txt.since(who.joinedAt));
					Ui.textFit(g, sub, tx, f.y() + 15, room, Ui.caption(), a);
					f.tip(
						0,
						0,
						right - f.x(),
						f.h(),
						List.of(shown, Component.literal(who.name).withStyle(ChatFormatting.GRAY), Txt.t("players.since", Txt.since(who.joinedAt)).withStyle(ChatFormatting.GRAY))
					);
				}
			)
			.flat()
			.narrate(Component.literal(who.name));
		int x = w - 8 - 18;
		if (stats) {
			card.add(new CFHGuiIconButton(0, 0, 18, Icon.TROPHY, Txt.t("players.stats"), () -> ServerClient.openStats(who.id)), x, 4);
			x -= 20;
		}

		if (mail) {
			card.add(new CFHGuiIconButton(0, 0, 18, Icon.SEND, Txt.t("players.mail", called), () -> new Forms.MailForm(who.name).show()), x, 4);
			x -= 20;
		}

		if (mayAskHere) {
			CFHGuiIconButton here = new CFHGuiIconButton(0, 0, 18, Icon.DOWNLOAD, Txt.t("players.ask_here", called), () -> ServerActions.tpa(who.id, true, null));
			here.enabled(who.acceptsRequests && !asked);
			if (!who.acceptsRequests) {
				here.tip(Txt.t("players.no_requests", called));
			}

			card.add(here, x, 4);
			x -= 20;
		}

		if (mayAsk) {
			CFHGuiIconButton ask = new CFHGuiIconButton(
				0, 0, 18, Icon.ARROW_RIGHT, Txt.t(asked ? "players.asked" : "players.ask", called), () -> ServerActions.tpa(who.id, false, null)
			);
			ask.enabled(who.acceptsRequests && !asked);
			if (!who.acceptsRequests) {
				ask.tip(Txt.t("players.no_requests", called));
			}

			card.add(ask, x, 4);
		}

		return card;
	}
}
