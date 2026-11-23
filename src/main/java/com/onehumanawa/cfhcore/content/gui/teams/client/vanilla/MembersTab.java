package com.onehumanawa.cfhcore.content.gui.teams.client.vanilla;

import com.onehumanawa.cfhcore.content.gui.teams.client.TeamActions;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import com.onehumanawa.cfhcore.content.gui.teams.model.Permission;
import com.onehumanawa.cfhcore.content.gui.teams.model.Rank;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class MembersTab extends TeamsTab {
	@Nullable
	private RowList list;
	@Nullable
	private Button first;
	@Nullable
	private Button second;
	@Nullable
	private Button third;

	MembersTab(VanillaTeamsScreen screen) {
		super(screen, "members");
	}

	@Override
	protected void build() {
		this.list = ((RowList)this.add(new RowList(this.mc, 26))).onSelect(this::update);
		enable(
			this.footButton(TeamsText.t("members.invite"), () -> this.flows().dialogs().invite()),
			TeamsView.may(Permission.INVITE),
			TeamsText.t("members.invite.rank", TeamsText.rank(Permission.INVITE.min()))
		);
		this.first = this.footButton(TeamsText.t("action.promote"), () -> this.act(0));
		this.second = this.footButton(TeamsText.t("action.demote"), () -> this.act(1));
		this.third = this.footButton(TeamsText.t("vanilla.cancel_invite"), () -> this.act(2));
		this.fill();
	}

	private void fill() {
		TeamData t = TeamsView.team();
		if (t != null && this.list != null) {
			RowList l = this.list;
			List<RowList.Row> rows = new ArrayList<>();
			List<TeamData.Member> members = new ArrayList<>(t.members);
			members.sort(
				Comparator.<TeamData.Member, Integer>comparing(m -> -m.rank.ordinal())
					.thenComparing(m -> !TeamsView.online(m.id))
					.thenComparing(m -> m.name, String.CASE_INSENSITIVE_ORDER)
			);

			for (TeamData.Member m : members) {
				rows.add(
					l.row(
						"member:" + m.id, m, Component.literal(m.name), (g, x, y, w, h, hovered, selected, mouseX, mouseY) -> Rows.member(g, m, TeamsText.where(m), x, y, w, h)
					)
				);
			}

			if (!t.invites.isEmpty()) {
				rows.add(l.caption("invited", TeamsText.t("members.invited", t.invites.size())));

				for (TeamData.Invite invite : t.invites) {
					rows.add(
						l.row(
							"invite:" + invite.player,
							invite,
							Component.literal(invite.playerName),
							(g, x, y, w, h, hovered, selected, mouseX, mouseY) -> Rows.player(
									g, invite.player, invite.playerName, TeamsText.t("members.invited_by", invite.byName, TeamsText.until(invite.expiresAt)), null, x, y, w, h
								)
						)
					);
				}
			}

			if (!t.requests.isEmpty()) {
				rows.add(l.caption("requests", TeamsText.t("overview.requests", t.requests.size())));

				for (TeamData.Request r : t.requests) {
					rows.add(
						l.row(
							"request:" + r.player,
							r,
							Component.literal(r.playerName),
							(g, x, y, w, h, hovered, selected, mouseX, mouseY) -> Rows.player(
									g, r.player, r.playerName, TeamsText.t("overview.asked", TeamsText.ago(r.at)), null, x, y, w, h
								)
						)
					);
				}
			}

			l.rows(rows);
		}
	}

	private void update() {
		if (this.list != null && this.first != null && this.second != null && this.third != null) {
			TeamData.Member m = this.list.chosen(TeamData.Member.class);
			TeamData.Invite invite = this.list.chosen(TeamData.Invite.class);
			TeamData.Request request = this.list.chosen(TeamData.Request.class);
			boolean mayInvite = TeamsView.may(Permission.INVITE);
			if (invite != null) {
				label(this.first, TeamsText.t("action.promote"), false);
				label(this.second, TeamsText.t("action.demote"), false);
				label(this.third, TeamsText.t("vanilla.cancel_invite"), mayInvite);
			} else if (request != null) {
				label(this.first, TeamsText.t("action.accept"), mayInvite);
				label(this.second, TeamsText.t("action.deny"), mayInvite);
				label(this.third, TeamsText.t("vanilla.remove"), false);
			} else {
				Rank up = m == null ? null : m.rank.promoted();
				Rank down = m == null ? null : m.rank.demoted();
				label(this.first, TeamsText.t("action.promote"), m != null && this.flows().canPromote(m));
				label(this.second, TeamsText.t("action.demote"), m != null && this.flows().canDemote(m));
				label(this.third, TeamsText.t("vanilla.remove"), m != null && this.flows().canKick(m));
				if (this.first.active && up != null) {
					this.first.setTooltip(Tooltip.create(TeamsText.t("members.promote", TeamsText.rank(up))));
				}

				if (this.second.active && down != null) {
					this.second.setTooltip(Tooltip.create(TeamsText.t("members.demote", TeamsText.rank(down))));
				}
			}
		}
	}

	private static void label(Button button, Component text, boolean active) {
		button.setMessage(text);
		button.active = active;
		button.setTooltip(null);
	}

	private void act(int which) {
		if (this.list != null) {
			TeamData.Member m = this.list.chosen(TeamData.Member.class);
			TeamData.Invite invite = this.list.chosen(TeamData.Invite.class);
			TeamData.Request request = this.list.chosen(TeamData.Request.class);
			if (invite != null) {
				if (which == 2) {
					TeamActions.cancelInvite(invite.player);
				}
			} else if (request != null) {
				if (which == 0) {
					TeamActions.acceptRequest(request.player);
				} else if (which == 1) {
					TeamActions.denyRequest(request.player);
				}
			} else if (m != null) {
				if (which == 0) {
					this.flows().promote(m);
				} else if (which == 1) {
					this.flows().demote(m);
				} else {
					this.flows().kick(m);
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

	@Override
	protected void layout(ScreenRectangle area) {
		if (this.list != null) {
			this.list.place(area.left(), area.top(), area.width(), area.height());
		}
	}
}
