package com.onehumanawa.cfhcore.content.gui.teams.client.overhaul;

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
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamActions;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsDraw;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import com.onehumanawa.cfhcore.content.gui.teams.model.Permission;
import com.onehumanawa.cfhcore.content.gui.teams.model.Rank;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class MembersCampPage extends CampPage {
	@Nullable
	private CFHGuiIconButton previous;
	@Nullable
	private CFHGuiIconButton next;

	MembersCampPage(OverhaulTeamsScreen screen) {
		super(screen, "members", Icon.FRIENDS);
	}

	public int badge() {
		TeamData t = TeamsView.team();
		return t != null && TeamsView.may(Permission.INVITE) ? t.requests.size() : 0;
	}

	@Override
	protected CampScene.View framing() {
		return CampScene.View.YARD;
	}

	@Override
	protected float sceneShare() {
		return 0.6F;
	}

	private int fitting() {
		return CampScene.fitting(this.view);
	}

	@Override
	public void build(SidebarScreen s, Rect area) {
		this.previous = this.next = null;
		this.screen.headerInset(TeamsView.hasTeam() ? 26 : 0);
		super.build(s, area);
	}

	@Override
	protected void populate() {
		CampRoster roster = this.screen.roster();
		roster.refresh();
		if (roster.chosen() != null && roster.chosenPerson() == null) {
			roster.choose(null);
		}

		if (roster.chosen() != null) {
			roster.reveal(roster.chosen(), this.fitting());
		}

		super.populate();
	}

	@Override
	protected void picked(CampScene.Person who) {
		if (who.kind() == CampScene.Kind.ALONE) {
			this.flows().dialogs().foundTeam();
		} else {
			CampRoster roster = this.screen.roster();
			roster.choose(who.id().equals(roster.chosen()) ? null : who.id());
			this.refresh();
		}
	}

	@Override
	protected void wheel(int direction) {
		this.turn(direction);
	}

	private void turn(int direction) {
		CampRoster roster = this.screen.roster();
		if (roster.canTurn(direction, this.fitting())) {
			roster.turn(direction, this.fitting());
			roster.choose(null);
			this.refresh();
		}
	}

	@Override
	protected void panel(SidebarScreen s, Rect r, TeamData team) {
		CampRoster roster = this.screen.roster();
		boolean mayInvite = TeamsView.may(Permission.INVITE);
		CFHGuiIconButton invite = new CFHGuiIconButton(s.width - 12 - 20, 6, 20, Icon.PLUS, TeamsText.t("members.invite"), () -> this.flows().dialogs().invite())
			.variant(Variant.PRIMARY);
		invite.tip(TeamsText.t(mayInvite ? "overview.invite.desc" : "members.invite.rank", TeamsText.rank(Permission.INVITE.min()))).enabled(mayInvite);
		s.addPageWidget(invite);
		int fit = this.fitting();
		this.previous = (CFHGuiIconButton)s.addPageWidget(
			new CFHGuiIconButton(this.view.x() + 4, this.view.bottom() - 22, 18, Icon.CHEVRON_LEFT, OverhaulTeams.t("yard.previous"), () -> this.turn(-1))
				.variant(Variant.SECONDARY)
		);
		this.next = (CFHGuiIconButton)s.addPageWidget(
			new CFHGuiIconButton(this.view.right() - 22, this.view.bottom() - 22, 18, Icon.CHEVRON_RIGHT, OverhaulTeams.t("yard.next"), () -> this.turn(1))
				.variant(Variant.SECONDARY)
		);
		boolean turns = roster.canTurn(-1, fit) || roster.canTurn(1, fit);
		this.previous.active = roster.canTurn(-1, fit);
		this.next.active = roster.canTurn(1, fit);
		this.previous.visible = this.next.visible = turns;
		CampScene.Person who = roster.chosenPerson();
		List<MembersCampPage.Act> acts = new ArrayList<>();
		Variant plain = Variant.SECONDARY;
		Variant danger = Variant.DANGER;
		if (who == null || who.kind() == CampScene.Kind.REQUEST) {
			UUID asking = who != null ? who.id() : (team.requests.isEmpty() ? null : team.requests.get(0).player);
			if (mayInvite && asking != null) {
				acts.add(new MembersCampPage.Act(TeamsText.t("action.accept"), Icon.CHECK, Variant.PRIMARY, null, () -> TeamActions.acceptRequest(asking)));
				acts.add(new MembersCampPage.Act(TeamsText.t("action.deny"), Icon.CLOSE, Variant.GHOST, null, () -> TeamActions.denyRequest(asking)));
			}
		} else if (who.kind() == CampScene.Kind.INVITE) {
			if (mayInvite) {
				acts.add(
					new MembersCampPage.Act(
						OverhaulTeams.t("yard.cancel_invite"), Icon.CLOSE, plain, TeamsText.t("members.cancel_invite"), () -> TeamActions.cancelInvite(who.id())
					)
				);
			}
		} else {
			TeamData.Member m = team.member(who.id());
			if (m != null) {
				Rank up = m.rank.promoted();
				Rank down = m.rank.demoted();
				if (this.flows().canPromote(m) && up != null) {
					acts.add(
						new MembersCampPage.Act(
							TeamsText.t("action.promote"), Icon.ARROW_UP, plain, TeamsText.t("members.promote", TeamsText.rank(up)), () -> this.flows().promote(m)
						)
					);
				}

				if (this.flows().canDemote(m) && down != null) {
					acts.add(
						new MembersCampPage.Act(
							TeamsText.t("action.demote"), Icon.ARROW_DOWN, plain, TeamsText.t("members.demote", TeamsText.rank(down)), () -> this.flows().demote(m)
						)
					);
				}

				if (this.flows().canTransfer(m)) {
					acts.add(
						new MembersCampPage.Act(TeamsText.t("action.transfer"), Icon.CROWN, plain, OverhaulTeams.t("yard.transfer.desc", m.name), () -> this.flows().transfer(m))
					);
				}

				if (this.flows().canKick(m)) {
					acts.add(new MembersCampPage.Act(TeamsText.t("action.remove"), Icon.BLOCKED, danger, TeamsText.t("action.kick"), () -> this.flows().kick(m)));
				}

				if (m.id.equals(TeamsView.self()) && this.flows().canLeave()) {
					acts.add(new MembersCampPage.Act(TeamsText.t("action.leave"), Icon.EXIT, danger, null, () -> this.flows().leave()));
				}
			}
		}

		place(s, acts, r.x(), r.bottom() - 20, r.w());
	}

	private static void place(SidebarScreen s, List<MembersCampPage.Act> acts, int x0, int y, int room) {
		if (!acts.isEmpty()) {
			List<CFHGuiButton> buttons = new ArrayList<>();
			int total = -4;

			for (MembersCampPage.Act a : acts) {
				CFHGuiButton b = new CFHGuiButton(0, y, 60, a.label(), a.run()).icon(a.icon()).variant(a.variant());
				if (a.tip() != null) {
					b.tip(a.tip());
				}

				b.setWidth(Math.max(52, b.preferredWidth() + 8));
				buttons.add(b);
				total += b.getWidth() + 4;
			}

			int x = x0;

			for (int i = 0; i < acts.size(); i++) {
				MembersCampPage.Act a = acts.get(i);
				if (total > room) {
					CFHGuiIconButton sign = new CFHGuiIconButton(x, y, 20, a.icon(), a.label(), a.run()).variant(a.variant());
					if (a.tip() != null) {
						sign.tip(List.of(a.label(), a.tip().copy().withStyle(ChatFormatting.GRAY)));
					}

					s.addPageWidget(sign);
					x += 24;
				} else {
					CFHGuiButton b = buttons.get(i);
					b.setX(x);
					s.addPageWidget(b);
					x += b.getWidth() + 4;
				}
			}
		}
	}

	@Override
	public void render(SidebarScreen s, GuiGraphics g, Rect area, int mouseX, int mouseY, float partialTick) {
		super.render(s, g, area, mouseX, mouseY, partialTick);
		TeamData team = TeamsView.team();
		if (team != null) {
			Theme t = Theme.current();
			Palette p = t.palette();
			boolean van = t.isVanilla();
			Font font = CFHGuiDraw.font();
			Rect r = this.below;
			int white = van ? -1 : p.text();
			CampScene.Person who = this.screen.roster().chosenPerson();
			int foot = r.bottom() - 24;
			if (who == null) {
				Icons.draw(g, Icon.FRIENDS, r.x(), r.y() + 1, 12, van ? -1 : p.accentInk());
				Component count = Fonts.heading(TeamsText.t("members.count", team.members.size(), TeamsView.info().maxMembers, TeamsView.onlineCount(team)));
				g.drawString(font, CFHGuiDraw.truncate(count, r.w() - 17), r.x() + 17, r.y() + 3, white, van);
				int y = r.y() + 16;
				boolean mayInvite = TeamsView.may(Permission.INVITE);
				if (!team.requests.isEmpty() && y + 9 <= foot) {
					TeamData.Request first = team.requests.get(0);
					Component asks = team.requests.size() == 1
						? OverhaulTeams.t("yard.request", first.playerName, TeamsText.ago(first.at))
						: OverhaulTeams.t("yard.requests", first.playerName, team.requests.size() - 1);
					g.drawString(font, CFHGuiDraw.truncate(asks, r.w()), r.x(), y, van ? -8054 : p.warning(), van);
					y += 11;
					if (!mayInvite && y + 9 <= foot) {
						g.drawString(
							font, CFHGuiDraw.truncate(TeamsText.t("members.invite.rank", TeamsText.rank(Permission.INVITE.min())), r.w()), r.x(), y, TeamsDraw.caption(), van
						);
						y += 11;
					}
				}

				if (!team.invites.isEmpty() && y + 9 <= foot) {
					g.drawString(
						font,
						CFHGuiDraw.truncate(OverhaulTeams.t(team.invites.size() == 1 ? "yard.invite_out" : "yard.invites_out", team.invites.size()), r.w()),
						r.x(),
						y,
						van ? -4144960 : p.textMuted(),
						van
					);
					y += 11;
				}

				if (y + 9 <= (!team.requests.isEmpty() && mayInvite ? foot : r.bottom())) {
					g.drawString(font, CFHGuiDraw.truncate(OverhaulTeams.t("yard.pick"), r.w()), r.x(), y, TeamsDraw.caption(), van);
				}
			} else {
				UUID id = who.id();
				TeamData.Member m = who.kind() == CampScene.Kind.MEMBER ? team.member(id) : null;
				Boolean online = m == null ? null : TeamsView.online(id);
				TeamsDraw.head(g, id, who.name(), r.x(), r.y() + 1, 20, online, 1.0F);
				int tx = r.x() + 26;
				int right = r.right();
				Component rank = m != null ? TeamsText.rank(m.rank) : who.line();
				right = TeamsDraw.chipRight(g, rank, right, r.y() + 1, who.lineColor(), 1.0F) - 4;
				if (who.crown()) {
					Icons.draw(g, Icon.CROWN, right - 9, r.y() + 3, 8, van ? -10402 : p.warning());
					right -= 12;
				}

				Component name = Fonts.heading(who.name());
				int used = CFHGuiDraw.width(CFHGuiDraw.truncate(name, right - tx));
				g.drawString(font, CFHGuiDraw.truncate(name, right - tx), tx, r.y() + 2, white, van);
				if (id.equals(TeamsView.self()) && tx + used + 6 + CFHGuiDraw.width(TeamsText.t("members.self")) < right) {
					g.drawString(font, TeamsText.t("members.self"), tx + used + 5, r.y() + 2, TeamsDraw.caption(), van);
				}

				Component where;
				if (m != null) {
					where = TeamsText.where(m);
				} else if (who.kind() == CampScene.Kind.REQUEST) {
					where = requestLine(team, id);
				} else {
					where = inviteLine(team, id);
				}

				boolean lively = Boolean.TRUE.equals(online) || who.kind() == CampScene.Kind.REQUEST;
				g.drawString(
					font, CFHGuiDraw.truncate(where, r.right() - tx), tx, r.y() + 13, lively ? (van ? -6496088 : p.success()) : (van ? -4144960 : p.textMuted()), van
				);
				int yx = r.y() + 27;
				if (m != null && yx + 9 <= foot) {
					g.drawString(font, CFHGuiDraw.truncate(TeamsText.t("members.joined", TeamsText.ago(m.joinedAt)), r.w()), r.x(), yx, TeamsDraw.caption(), van);
					yx += 11;
				}

				if (m != null && !m.id.equals(TeamsView.self()) && !this.flows().canAct(m) && yx + 9 <= r.bottom()) {
					g.drawString(font, CFHGuiDraw.truncate(OverhaulTeams.t("yard.no_actions"), r.w()), r.x(), Math.max(yx, r.bottom() - 13), TeamsDraw.caption(), van);
				}
			}
		}
	}

	private static Component requestLine(TeamData team, UUID id) {
		for (TeamData.Request r : team.requests) {
			if (r.player.equals(id)) {
				return OverhaulTeams.t("yard.asked", TeamsText.ago(r.at));
			}
		}

		return OverhaulTeams.t("yard.asks");
	}

	private static Component inviteLine(TeamData team, UUID id) {
		for (TeamData.Invite i : team.invites) {
			if (i.player.equals(id)) {
				return TeamsText.t("members.invited_by", i.byName, TeamsText.until(i.expiresAt));
			}
		}

		return OverhaulTeams.t("yard.invited");
	}

	@Override
	protected void overlay(GuiGraphics g, int mouseX, int mouseY) {
		super.overlay(g, mouseX, mouseY);
		CampRoster roster = this.screen.roster();
		int fit = this.fitting();
		int all = roster.all().size();
		if (all > fit) {
			Theme t = Theme.current();
			String at = roster.first() + 1 + " – " + Math.min(all, roster.first() + fit) + " / " + all;
			int w = CFHGuiDraw.width(at) + 8;
			CFHGuiDraw.pixelRound(
				g, this.view.centerX() - w / 2, this.view.bottom() - 15, w, 12, t.isVanilla() ? -1610612736 : Colors.withAlpha(t.bg(), 192), t.isVanilla() ? 0 : 2
			);
			CFHGuiDraw.textCentered(g, at, this.view.centerX(), this.view.bottom() - 13, t.isVanilla() ? -2039584 : t.palette().textMuted());
		}
	}

	private static record Act(Component label, Icon icon, Variant variant, @Nullable Component tip, Runnable run) {
	}
}
