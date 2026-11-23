package com.onehumanawa.cfhcore.content.gui.teams.client.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiContextMenu;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel.Style;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup.Item;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamActions;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.dialog.CreateTeamForm;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.dialog.FormCard;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.ActivityFeed;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.PaintedCard;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsDraw;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import com.onehumanawa.cfhcore.content.gui.teams.model.InviteView;
import com.onehumanawa.cfhcore.content.gui.teams.model.Permission;
import com.onehumanawa.cfhcore.content.gui.teams.model.ServerInfo;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamSummary;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class OverviewPage extends TeamsPage {
	private CFHGuiScrollPanel panel;
	@Nullable
	private FormCard founding;

	OverviewPage(TeamsHubScreen screen) {
		super(screen, "overview", Icon.TEAM);
	}

	public int badge() {
		TeamData t = TeamsView.team();
		if (t == null) {
			return TeamsView.invites().size();
		} else {
			return TeamsView.may(Permission.INVITE) ? t.requests.size() : 0;
		}
	}

	@Override
	protected void buildPage(SidebarScreen s, Rect area) {
		this.founding = null;
		this.panel = (CFHGuiScrollPanel)s.addPageWidget(new CFHGuiScrollPanel(area.x(), area.y(), area.w(), area.h()));
		this.fill();
	}

	@Override
	void stateChanged() {
		if (this.panel == null) {
			super.stateChanged();
		} else {
			refill(this.panel, this::fill);
		}
	}

	private void fill() {
		int w = this.panel.innerWidth();
		TeamData t = TeamsView.team();
		this.panel.setContentHeight(t == null ? this.fillNoTeam(w) : this.fillTeam(t, w));
	}

	private int fillTeam(TeamData t, int w) {
		int y = 0;
		this.panel.add(this.hero(t, w), 0, y);
		y += 66;
		ServerInfo info = TeamsView.info();
		int gap = 4;
		int across = (w - 12) / 4 >= 84 ? 4 : 2;
		int tw = (w - 4 * (across - 1)) / across;
		int online = TeamsView.onlineCount(t);
		List<PaintedCard> figures = List.of(
			figure(
				tw, Icon.FRIENDS, online, " / " + t.members.size(), TeamsText.t("overview.online"), TeamsText.t("overview.online.desc"), () -> this.screen.go("members")
			),
			figure(tw, Icon.MAP, TeamsView.held(), " / " + info.maxClaims, TeamsText.t("overview.land"), TeamsText.t("overview.land.desc"), () -> this.screen.go("land")),
			figure(tw, Icon.QUEST, (int)Math.min(2147483647L, t.counter("quests")), "", TeamsText.t("overview.quests"), TeamsText.t("overview.quests.desc"), null),
			figure(tw, Icon.SHIELD, t.allies.size(), "", TeamsText.t("overview.allies"), TeamsText.t("overview.allies.desc"), () -> this.screen.go("allies"))
		);

		for (int i = 0; i < figures.size(); i++) {
			PaintedCard f = figures.get(i);
			if (i % across == across - 1) {
				f.setWidth(w - (tw + 4) * (across - 1));
			}

			this.panel.add(f, i % across * (tw + 4), y + i / across * 34);
		}

		y += 34 * (4 / across);
		int bw = (w - 12) / 4;
		this.panel
			.add(new CFHGuiButton(0, 0, bw, TeamsText.t("overview.chat"), () -> this.flows().teamChat()).icon(Icon.CHAT).tip(TeamsText.t("overview.chat.desc")), 0, y);
		CFHGuiButton stash = new CFHGuiButton(0, 0, bw, TeamsText.t("overview.stash"), () -> this.flows().stash()).icon(Icon.BACKUP);
		stash.tip(TeamsText.t(info.stash ? "overview.stash.desc" : "overview.stash.off")).enabled(info.stash);
		this.panel.add(stash, bw + 4, y);
		CFHGuiButton home = new CFHGuiButton(0, 0, bw, TeamsText.t("overview.home"), null).icon(Icon.HOME);
		home.onPress(() -> this.home(t, home));
		home.tip(TeamsText.t(t.home == null ? "overview.home.none" : "overview.home.desc"));
		this.panel.add(home, (bw + 4) * 2, y);
		boolean mayInvite = TeamsView.may(Permission.INVITE);
		CFHGuiButton invite = new CFHGuiButton(0, 0, w - (bw + 4) * 3, TeamsText.t("overview.invite"), () -> this.flows().dialogs().invite())
			.icon(Icon.INVITE)
			.variant(Variant.PRIMARY);
		invite.tip(TeamsText.t(mayInvite ? "overview.invite.desc" : "members.invite.rank", TeamsText.rank(Permission.INVITE.min()))).enabled(mayInvite);
		this.panel.add(invite, (bw + 4) * 3, y);
		y += 28;
		if (mayInvite && !t.requests.isEmpty()) {
			y = section(this.panel, y, w, TeamsText.t("overview.requests", t.requests.size()));

			for (TeamData.Request r : t.requests) {
				this.panel.add(this.request(r, w), 0, y);
				y += 28;
			}

			y += 4;
		}

		y = section(this.panel, y, w, TeamsText.t("overview.activity"));
		ActivityFeed feed = new ActivityFeed(0, 0, w, t.log);
		this.panel.add(feed, 0, y);
		return y + feed.getHeight() + 4;
	}

	private PaintedCard hero(TeamData t, int w) {
		TeamData.Member owner = t.owner();
		return new PaintedCard(
				w,
				62,
				(g, f) -> {
					Theme th = Theme.current();
					Palette p = th.palette();
					float a = f.alpha();
					int x = f.x();
					int y = f.y();
					int ink = TeamsDraw.ink(t.color);
					TeamsDraw.wash(g, x + 1, y + 1, Math.min(w - 2, 180), 60, t.color, a);
					CFHGuiDraw.rect(g, x, y + 4, 2, 54, Colors.scaleAlpha(ink, a));
					TeamsDraw.banner(g, t.banner, x + 11, y + 9, 20, f.grow(), 1.6F, a);
					int tx = x + 42;
					int right = x + w - 8;
					right = TeamsDraw.chipRight(g, TeamsText.privacy(t.privacy), right, y + 7, p.textMuted(), a) - 4;
					f.tip(right + 4 - x, 7, x + w - 8 - right - 4, 12, TeamsText.privacyHint(t.privacy));
					if (!t.tag.isEmpty()) {
						right = TeamsDraw.chipRight(g, Component.literal(t.tag), right, y + 7, ink, a) - 4;
					}

					g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(Fonts.heading(t.name), right - tx - 2), tx, y + 9, Colors.scaleAlpha(ink, a), th.isVanilla());
					if (t.motto.isEmpty()) {
						TeamsDraw.text(g, TeamsText.t("overview.no_motto"), tx, y + 22, p.textDim(), a);
					} else {
						Component motto = Component.literal(t.motto);
						TeamsDraw.textFit(g, motto, tx, y + 22, x + w - 8 - tx, p.textMuted(), a);
						if (TeamsDraw.cut(motto, x + w - 8 - tx)) {
							f.tip(tx - x, 21, w - 8 - (tx - x), 10, motto);
						}
					}

					Component level = TeamsText.t("overview.level", t.level);
					long need = TeamData.xpForLevel(t.level);
					Component xp = TeamsText.t("overview.xp", TeamsText.number((long)Math.round((float)t.xp * f.grow())), TeamsText.number(need));
					int levelW = CFHGuiDraw.width(level);
					int xpW = CFHGuiDraw.width(xp);
					TeamsDraw.text(g, level, tx, y + 36, th.isVanilla() ? -1 : p.text(), a);
					int bx = tx + levelW + 6;
					int bw = x + w - 8 - xpW - 6 - bx;
					if (bw >= 16) {
						TeamsDraw.bar(g, bx, y + 38, bw, 4, t.levelProgress() * f.grow(), ink, a);
						TeamsDraw.text(g, xp, x + w - 8 - xpW, y + 36, p.textMuted(), a);
					}

					f.tip(tx - x, 34, w - 8 - (tx - x), 12, TeamsText.t("overview.xp.desc", t.level + 1, TeamsText.number(Math.max(0L, need - t.xp))));
					Component founded = owner == null
						? TeamsText.t("overview.founded", TeamsText.ago(t.createdAt))
						: TeamsText.t("overview.founded.owner", TeamsText.ago(t.createdAt), owner.name);
					TeamsDraw.textFit(g, founded, tx, y + 49, x + w - 8 - tx, p.textDim(), a);
				}
			)
			.flat()
			.narrate(Component.literal(t.name));
	}

	private static PaintedCard figure(int w, Icon icon, int value, String suffix, Component label, Component tip, @Nullable Runnable onClick) {
		PaintedCard card = new PaintedCard(
				w,
				30,
				(g, f) -> {
					Theme th = Theme.current();
					Palette p = th.palette();
					float a = f.alpha();
					Component number = Fonts.heading(Math.round((float)value * f.grow()) + suffix);
					g.drawString(
						CFHGuiDraw.font(), CFHGuiDraw.truncate(number, f.w() - 20), f.x() + 6, f.y() + 5, Colors.scaleAlpha(th.isVanilla() ? -1 : p.text(), a), th.isVanilla()
					);
					TeamsDraw.textFit(g, label, f.x() + 6, f.y() + 17, f.w() - 10, p.textDim(), a);
					Icons.draw(g, icon, f.x() + f.w() - 14, f.y() + 5, 8, Colors.scaleAlpha(Colors.lerp(p.textDim(), th.accentInk(), f.hover()), a));
					f.tip(0, 0, f.w(), f.h(), tip);
				}
			)
			.narrate(label);
		if (onClick != null) {
			card.onClick(onClick);
		} else {
			card.flat();
		}

		return card;
	}

	private void home(TeamData t, CFHGuiButton anchor) {
		ServerInfo info = TeamsView.info();
		if (!TeamsView.may(Permission.HOME)) {
			if (t.home == null) {
				TeamActions.goHome();
			} else {
				this.flows().goHome();
			}
		} else {
			List<Item> items = new ArrayList<>();
			if (t.home != null && info.homeTeleport) {
				items.add(Item.of(TeamsText.t("overview.home.go"), Icon.HOME, () -> this.flows().goHome()));
			} else {
				items.add(Item.disabled(TeamsText.t(t.home == null ? "overview.home.none" : "overview.home.off"), Icon.HOME));
			}

			items.add(Item.sep());
			items.add(Item.of(TeamsText.t("action.set_home"), Icon.PIN, () -> this.flows().setHome()));
			if (t.home != null) {
				items.add(Item.danger(TeamsText.t("action.clear_home"), Icon.TRASH, () -> this.flows().clearHome()));
			}

			CFHGuiContextMenu.open((double)anchor.getX(), (double)(anchor.getY() + anchor.getHeight() + 1), items);
		}
	}

	private PaintedCard request(TeamData.Request r, int w) {
		PaintedCard card = new PaintedCard(w, 24, (g, f) -> {
			Palette p = Theme.current().palette();
			TeamsDraw.head(g, r.player, r.playerName, f.x() + 5, f.y() + 4, 16, null, f.alpha());
			int tx = f.x() + 27;
			int room = f.w() - 27 - 118;
			int used = TeamsDraw.textFit(g, Component.literal(r.playerName), tx, f.y() + 8, room, Theme.current().isVanilla() ? -1 : p.text(), f.alpha());
			TeamsDraw.textFit(g, TeamsText.t("overview.asked", TeamsText.ago(r.at)), tx + used + 6, f.y() + 8, room - used - 6, p.textDim(), f.alpha());
		}).flat();
		card.add(
			new CFHGuiButton(0, 0, 66, 16, TeamsText.t("action.accept"), () -> TeamActions.acceptRequest(r.player)).variant(Variant.PRIMARY).icon(Icon.CHECK).iconSize(8),
			w - 112,
			4
		);
		card.add(new CFHGuiButton(0, 0, 40, 16, TeamsText.t("action.deny"), () -> TeamActions.denyRequest(r.player)).variant(Variant.GHOST), w - 44, 4);
		return card;
	}

	private int fillNoTeam(int w) {
		int y = 0;
		ServerInfo info = TeamsView.info();
		TeamData own = TeamsView.personal();
		if (own != null) {
			this.panel.add(this.ownLand(own, w), 0, y);
			y += 50;
		}

		List<InviteView> invites = TeamsView.invites();
		if (!invites.isEmpty()) {
			y = section(this.panel, y, w, TeamsText.t("overview.invitations", invites.size()));

			for (InviteView invite : invites) {
				this.panel.add(this.invitation(invite, w), 0, y);
				y += 40;
			}

			y += 2;
		}

		if (this.founding == null) {
			this.founding = new FormCard(w, new CreateTeamForm());
			if (!info.playersCreateTeams && !info.admin) {
				this.founding.locked(TeamsText.t("create.operators_only"));
			}
		}

		this.panel.add(this.founding, 0, y);
		y += this.founding.getHeight() + 6;
		List<UUID> asked = new ArrayList<>(TeamsView.requested());
		if (!asked.isEmpty()) {
			y = section(this.panel, y, w, TeamsText.t("overview.asked_teams", asked.size()));

			for (UUID id : asked) {
				TeamSummary s = TeamsView.summary(id);
				if (s != null) {
					this.panel.add(this.asked(s, w), 0, y);
					y += 28;
				}
			}

			y += 2;
		}

		CFHGuiLabel or = new CFHGuiLabel(0, 0, w - 110, TeamsText.t("overview.or_join")).style(Style.MUTED);
		this.panel.add(or, 2, y + 6);
		this.panel.add(new CFHGuiButton(0, 0, 104, TeamsText.t("state.browse"), () -> this.screen.go("browse")).icon(Icon.SEARCH), w - 104, y);
		return y + 24;
	}

	private PaintedCard ownLand(TeamData own, int w) {
		CFHGuiButton show = new CFHGuiButton(0, 0, 62, 18, TeamsText.t("solo.land"), () -> this.screen.go("land")).icon(Icon.MAP).iconSize(8);
		show.setWidth(Math.max(62, show.preferredWidth() + 8));
		CFHGuiButton giveUp = new CFHGuiButton(0, 0, 62, 18, TeamsText.t("action.give_up"), () -> this.flows().giveUpLand()).variant(Variant.DANGER);
		giveUp.setWidth(Math.max(62, giveUp.preferredWidth() + 8));
		giveUp.tip(TeamsText.t("solo.give_up.desc"));
		int buttons = show.getWidth() + giveUp.getWidth() + 12;
		PaintedCard card = new PaintedCard(w, 44, (g, f) -> {
			Theme th = Theme.current();
			Palette p = th.palette();
			float a = f.alpha();
			int ink = TeamsDraw.ink(own.color);
			TeamsDraw.wash(g, f.x() + 1, f.y() + 1, Math.min(f.w() - 2, 140), f.h() - 2, own.color, a);
			Icons.draw(g, Icon.MAP, f.x() + 8, f.y() + 6, 16, Colors.scaleAlpha(ink, a));
			int tx = f.x() + 32;
			int room = f.w() - 32 - buttons - 4;
			TeamsDraw.textFit(g, Fonts.heading(TeamsText.t("solo.card.title")), tx, f.y() + 6, room, ink, a);
			TeamsDraw.textFit(g, TeamsText.t("solo.card.held", TeamsView.held(), TeamsView.info().maxClaims), tx, f.y() + 18, room, th.isVanilla() ? -1 : p.text(), a);
			TeamsDraw.textFit(g, TeamsText.t("solo.card.note"), tx, f.y() + 29, room, p.textMuted(), a);
			f.tip(0, 0, f.w() - buttons, f.h(), List.of(TeamsText.t("solo.card.title"), TeamsText.t("solo.land_note").withStyle(ChatFormatting.GRAY)));
		}).flat();
		card.add(show, w - buttons + 4, 13);
		card.add(giveUp, w - giveUp.getWidth() - 4, 13);
		return card;
	}

	private PaintedCard invitation(InviteView invite, int w) {
		PaintedCard card = new PaintedCard(
				w,
				36,
				(g, f) -> {
					Theme th = Theme.current();
					Palette p = th.palette();
					float a = f.alpha();
					int ink = TeamsDraw.ink(invite.color);
					TeamsDraw.wash(g, f.x() + 1, f.y() + 1, Math.min(f.w() - 2, 140), f.h() - 2, invite.color, a);
					TeamsDraw.bannerSmall(g, invite.banner, f.x() + 8, f.y() + 6, 12, a);
					int tx = f.x() + 28;
					int room = f.w() - 28 - 124;
					int used = CFHGuiDraw.width(CFHGuiDraw.truncate(Fonts.heading(invite.teamName), room));
					g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(Fonts.heading(invite.teamName), room), tx, f.y() + 7, Colors.scaleAlpha(ink, a), th.isVanilla());
					if (!invite.tag.isEmpty() && used + 40 < room) {
						TeamsDraw.chip(g, Component.literal(invite.tag), tx + used + 5, f.y() + 5, ink, a);
					}

					TeamsDraw.textFit(
						g, TeamsText.t("overview.invited_by", invite.byName, invite.members, TeamsText.until(invite.expiresAt)), tx, f.y() + 20, room, p.textMuted(), a
					);
				}
			)
			.flat();
		card.add(
			new CFHGuiButton(0, 0, 62, 18, TeamsText.t("action.accept"), () -> TeamActions.acceptInvite(invite.team, null))
				.variant(Variant.PRIMARY)
				.icon(Icon.CHECK)
				.iconSize(8),
			w - 118,
			9
		);
		card.add(new CFHGuiButton(0, 0, 48, 18, TeamsText.t("action.decline"), () -> TeamActions.declineInvite(invite.team)).variant(Variant.GHOST), w - 52, 9);
		return card;
	}

	private PaintedCard asked(TeamSummary s, int w) {
		PaintedCard card = new PaintedCard(w, 24, (g, f) -> {
			Palette p = Theme.current().palette();
			TeamsDraw.bannerSmall(g, s.banner, f.x() + 7, f.y() + 3, 9, f.alpha());
			int used = TeamsDraw.textFit(g, Component.literal(s.name), f.x() + 22, f.y() + 8, f.w() - 22 - 80, TeamsDraw.ink(s.color), f.alpha());
			TeamsDraw.textFit(g, TeamsText.t("overview.waiting"), f.x() + 28 + used, f.y() + 8, f.w() - 28 - used - 80, p.textDim(), f.alpha());
		}).flat();
		card.add(new CFHGuiIconButton(0, 0, 16, Icon.CLOSE, TeamsText.t("browse.withdraw"), () -> TeamActions.cancelRequest(s.id)), w - 22, 4);
		return card;
	}
}
