package com.onehumanawa.cfhcore.content.gui.teams.client.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
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
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.PaintedCard;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsDraw;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import com.onehumanawa.cfhcore.content.gui.teams.model.Permission;
import com.onehumanawa.cfhcore.content.gui.teams.model.Rank;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

final class MembersPage extends TeamsPage {
	private CFHGuiScrollPanel panel;
	private CFHGuiLabel count;

	MembersPage(TeamsHubScreen screen) {
		super(screen, "members", Icon.FRIENDS);
	}

	public int badge() {
		TeamData t = TeamsView.team();
		return t != null && TeamsView.may(Permission.INVITE) ? t.requests.size() : 0;
	}

	@Override
	protected void buildPage(SidebarScreen s, Rect area) {
		this.panel = null;
		TeamData t = TeamsView.team();
		if (t == null) {
			this.noTeam(s, area, Icon.FRIENDS, TeamsText.t("members.no_team"));
		} else {
			boolean mayInvite = TeamsView.may(Permission.INVITE);
			CFHGuiButton invite = new CFHGuiButton(0, 0, 96, TeamsText.t("members.invite"), () -> this.flows().dialogs().invite())
				.variant(Variant.PRIMARY)
				.icon(Icon.INVITE);
			invite.setWidth(Math.max(84, invite.preferredWidth() + 12));
			invite.setX(area.right() - invite.getWidth());
			invite.setY(area.y());
			invite.tip(TeamsText.t(mayInvite ? "overview.invite.desc" : "members.invite.rank", TeamsText.rank(Permission.INVITE.min()))).enabled(mayInvite);
			this.count = (CFHGuiLabel)s.addPageWidget(new CFHGuiLabel(area.x() + 2, area.y() + 6, area.w() - invite.getWidth() - 10, Component.empty()).style(Style.MUTED));
			s.addPageWidget(invite);
			this.panel = (CFHGuiScrollPanel)s.addPageWidget(new CFHGuiScrollPanel(area.x(), area.y() + 26, area.w(), area.h() - 26));
			this.fill();
		}
	}

	@Override
	void stateChanged() {
		if (this.panel != null && TeamsView.team() != null) {
			refill(this.panel, this::fill);
		} else {
			super.stateChanged();
		}
	}

	private void fill() {
		TeamData t = TeamsView.team();
		if (t != null) {
			int w = this.panel.innerWidth();
			this.count.text(TeamsText.t("members.count", t.members.size(), TeamsView.info().maxMembers, TeamsView.onlineCount(t)));
			int y = 0;
			List<TeamData.Member> members = new ArrayList<>(t.members);
			members.sort(
				Comparator.<TeamData.Member, Integer>comparing(mx -> -mx.rank.ordinal())
					.thenComparing(mx -> !TeamsView.online(mx.id))
					.thenComparing(mx -> mx.name, String.CASE_INSENSITIVE_ORDER)
			);

			for (TeamData.Member m : members) {
				this.panel.add(this.member(t, m, w), 0, y);
				y += 30;
			}

			boolean mayInvite = TeamsView.may(Permission.INVITE);
			if (!t.invites.isEmpty()) {
				y = section(this.panel, y + 4, w, TeamsText.t("members.invited", t.invites.size()));

				for (TeamData.Invite invite : t.invites) {
					this.panel.add(this.invited(invite, w, mayInvite), 0, y);
					y += 28;
				}
			}

			if (!t.requests.isEmpty()) {
				y = section(this.panel, y + 4, w, TeamsText.t("overview.requests", t.requests.size()));

				for (TeamData.Request r : t.requests) {
					this.panel.add(this.request(r, w, mayInvite), 0, y);
					y += 28;
				}
			}

			this.panel.setContentHeight(y + 2);
		}
	}

	private static int rankColor(Rank rank, Palette p) {
		return switch (rank) {
			case OWNER -> p.warning();
			case ADMIN -> p.accent();
			case MODERATOR -> p.success();
			default -> p.textMuted();
		};
	}

	private PaintedCard member(TeamData t, TeamData.Member m, int w) {
		boolean self = m.id.equals(TeamsView.self());
		boolean act = this.flows().canAct(m);
		PaintedCard card = new PaintedCard(
				w,
				26,
				(g, f) -> {
					Theme th = Theme.current();
					Palette p = th.palette();
					float a = f.alpha();
					boolean on = TeamsView.online(m.id);
					TeamsDraw.head(g, m.id, m.name, f.x() + 6, f.y() + 5, 16, on, a * (on ? 1.0F : 0.7F));
					int tx = f.x() + 30;
					Component rank = TeamsText.rank(m.rank);
					int chipLeft = TeamsDraw.chipRight(g, rank, f.x() + f.w() - (act ? 28 : 8), f.y() + 7, rankColor(m.rank, p), a);
					if (m.rank == Rank.OWNER) {
						Icons.draw(g, Icon.CROWN, chipLeft - 11, f.y() + 9, 8, Colors.scaleAlpha(p.warning(), a));
					}

					int room = chipLeft - tx - (m.rank == Rank.OWNER ? 16 : 6);
					int nameColor = th.isVanilla() ? (on ? -1 : -4144960) : (on ? p.text() : p.textMuted());
					int used = TeamsDraw.textFit(g, Component.literal(m.name), tx, f.y() + 4, room, nameColor, a);
					if (self && used + 30 < room) {
						TeamsDraw.text(g, TeamsText.t("members.self"), tx + used + 4, f.y() + 4, p.textDim(), a);
					}

					Component where = TeamsText.where(m);
					TeamsDraw.textFit(g, where, tx, f.y() + 15, room, on ? p.textMuted() : p.textDim(), a);
					f.tip(
						0,
						0,
						f.w() - 30,
						f.h(),
						List.of(
							Component.literal(m.name),
							rank.copy().withStyle(ChatFormatting.GRAY),
							TeamsText.t("members.joined", TeamsText.ago(m.joinedAt)).withStyle(ChatFormatting.GRAY)
						)
					);
				}
			)
			.narrate(Component.literal(m.name));
		if (!act) {
			return card.flat();
		} else {
			CFHGuiIconButton more = new CFHGuiIconButton(0, 0, 18, Icon.DOTS, TeamsText.t("members.actions"), null);
			more.onPress(() -> this.menu(m, more.getX() - 80, more.getY() + 19));
			card.add(more, w - 23, 4);
			card.onRightClick(
				() -> {
					Minecraft mc = Minecraft.getInstance();
					this.menu(
						m,
						(int)(mc.mouseHandler.xpos() * (double)mc.getWindow().getGuiScaledWidth() / (double)mc.getWindow().getScreenWidth()),
						(int)(mc.mouseHandler.ypos() * (double)mc.getWindow().getGuiScaledHeight() / (double)mc.getWindow().getScreenHeight())
					);
				}
			);
			return card;
		}
	}

	private void menu(TeamData.Member m, int x, int y) {
		List<Item> items = new ArrayList<>();
		Rank up = m.rank.promoted();
		Rank down = m.rank.demoted();
		if (this.flows().canPromote(m) && up != null) {
			items.add(Item.of(TeamsText.t("members.promote", TeamsText.rank(up)), Icon.ARROW_UP, () -> this.flows().promote(m)));
		}

		if (this.flows().canDemote(m) && down != null) {
			items.add(Item.of(TeamsText.t("members.demote", TeamsText.rank(down)), Icon.ARROW_DOWN, () -> this.flows().demote(m)));
		}

		if (this.flows().canTransfer(m)) {
			items.add(Item.of(TeamsText.t("action.transfer"), Icon.CROWN, () -> this.flows().transfer(m)));
		}

		if (this.flows().canKick(m)) {
			if (!items.isEmpty()) {
				items.add(Item.sep());
			}

			items.add(Item.danger(TeamsText.t("action.kick"), Icon.BLOCKED, () -> this.flows().kick(m)));
		}

		if (!items.isEmpty()) {
			CFHGuiContextMenu.open((double)x, (double)y, items);
		}
	}

	private PaintedCard invited(TeamData.Invite invite, int w, boolean mayCancel) {
		PaintedCard card = new PaintedCard(
				w,
				24,
				(g, f) -> {
					Palette p = Theme.current().palette();
					TeamsDraw.head(g, invite.player, invite.playerName, f.x() + 6, f.y() + 4, 16, null, f.alpha() * 0.8F);
					int tx = f.x() + 30;
					int room = f.w() - 30 - (mayCancel ? 30 : 8);
					int used = TeamsDraw.textFit(g, Component.literal(invite.playerName), tx, f.y() + 8, room, Theme.current().isVanilla() ? -1 : p.text(), f.alpha());
					TeamsDraw.textFit(
						g, TeamsText.t("members.invited_by", invite.byName, TeamsText.until(invite.expiresAt)), tx + used + 6, f.y() + 8, room - used - 6, p.textDim(), f.alpha()
					);
				}
			)
			.flat();
		if (mayCancel) {
			card.add(new CFHGuiIconButton(0, 0, 16, Icon.CLOSE, TeamsText.t("members.cancel_invite"), () -> TeamActions.cancelInvite(invite.player)), w - 22, 4);
		}

		return card;
	}

	private PaintedCard request(TeamData.Request r, int w, boolean mayAnswer) {
		PaintedCard card = new PaintedCard(w, 24, (g, f) -> {
			Palette p = Theme.current().palette();
			TeamsDraw.head(g, r.player, r.playerName, f.x() + 6, f.y() + 4, 16, null, f.alpha());
			int tx = f.x() + 30;
			int room = f.w() - 30 - (mayAnswer ? 118 : 8);
			int used = TeamsDraw.textFit(g, Component.literal(r.playerName), tx, f.y() + 8, room, Theme.current().isVanilla() ? -1 : p.text(), f.alpha());
			TeamsDraw.textFit(g, TeamsText.t("overview.asked", TeamsText.ago(r.at)), tx + used + 6, f.y() + 8, room - used - 6, p.textDim(), f.alpha());
		}).flat();
		if (mayAnswer) {
			card.add(
				new CFHGuiButton(0, 0, 66, 16, TeamsText.t("action.accept"), () -> TeamActions.acceptRequest(r.player))
					.variant(Variant.PRIMARY)
					.icon(Icon.CHECK)
					.iconSize(8),
				w - 112,
				4
			);
			card.add(new CFHGuiButton(0, 0, 40, 16, TeamsText.t("action.deny"), () -> TeamActions.denyRequest(r.player)).variant(Variant.GHOST), w - 44, 4);
		}

		return card;
	}
}
