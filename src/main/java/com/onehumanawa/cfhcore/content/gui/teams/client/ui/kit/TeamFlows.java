package com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.teams.TeamsConfig;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamActions;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.dialog.TeamDialogs;
import com.onehumanawa.cfhcore.content.gui.teams.model.InviteView;
import com.onehumanawa.cfhcore.content.gui.teams.model.Permission;
import com.onehumanawa.cfhcore.content.gui.teams.model.Privacy;
import com.onehumanawa.cfhcore.content.gui.teams.model.Rank;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamSummary;
import com.onehumanawa.cfhcore.content.gui.teams.net.TeamsNet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class TeamFlows {
	private final TeamDialogs dialogs;

	public TeamFlows(TeamDialogs dialogs) {
		this.dialogs = dialogs;
	}

	public TeamDialogs dialogs() {
		return this.dialogs;
	}

	private static boolean other(TeamData.Member m) {
		return !m.id.equals(TeamsView.self());
	}

	public boolean canPromote(TeamData.Member m) {
		Rank mine = TeamsView.rank();
		Rank up = m.rank.promoted();
		return mine != null && other(m) && up != null && TeamsView.may(Permission.RANKS) && mine.above(up);
	}

	public boolean canDemote(TeamData.Member m) {
		Rank mine = TeamsView.rank();
		return mine != null && other(m) && m.rank.demoted() != null && TeamsView.may(Permission.RANKS) && mine.above(m.rank);
	}

	public boolean canKick(TeamData.Member m) {
		Rank mine = TeamsView.rank();
		return mine != null && other(m) && TeamsView.may(Permission.KICK) && mine.above(m.rank);
	}

	public boolean canTransfer(TeamData.Member m) {
		return other(m) && TeamsView.may(Permission.OWN);
	}

	public boolean canAct(TeamData.Member m) {
		return this.canPromote(m) || this.canDemote(m) || this.canKick(m) || this.canTransfer(m);
	}

	public void promote(TeamData.Member m) {
		Rank up = m.rank.promoted();
		if (up != null) {
			this.dialogs
				.confirm(
					TeamsText.t("ask.promote.title"),
					TeamsText.t("ask.promote", m.name, TeamsText.rank(up)),
					TeamsText.t("action.promote"),
					false,
					() -> TeamActions.promote(m.id)
				);
		}
	}

	public void demote(TeamData.Member m) {
		Rank down = m.rank.demoted();
		if (down != null) {
			this.dialogs
				.confirm(
					TeamsText.t("ask.demote.title"),
					TeamsText.t("ask.demote", m.name, TeamsText.rank(down)),
					TeamsText.t("action.demote"),
					false,
					() -> TeamActions.demote(m.id)
				);
		}
	}

	public void kick(TeamData.Member m) {
		this.dialogs.confirm(TeamsText.t("ask.kick.title"), TeamsText.t("ask.kick", m.name), TeamsText.t("action.kick"), true, () -> TeamActions.kick(m.id));
	}

	public void transfer(TeamData.Member m) {
		this.dialogs
			.confirm(
				TeamsText.t("ask.transfer.title"), TeamsText.t("ask.transfer", m.name), TeamsText.t("action.transfer"), true, () -> TeamActions.transfer(m.id, null)
			);
	}

	public void transferPick() {
		TeamData t = TeamsView.team();
		if (t != null) {
			List<TeamData.Member> others = new ArrayList<>();

			for (TeamData.Member m : t.members) {
				if (other(m)) {
					others.add(m);
				}
			}

			if (others.isEmpty()) {
				tell(TeamsText.t("transfer.nobody"));
			} else {
				this.dialogs.pickMember(TeamsText.t("ask.transfer.title"), others, this::transfer);
			}
		}
	}

	public boolean canLeave() {
		return TeamsView.hasTeam() && TeamsView.rank() != Rank.OWNER;
	}

	public void leave() {
		TeamData t = TeamsView.team();
		if (t != null) {
			this.dialogs.confirm(TeamsText.t("ask.leave.title"), TeamsText.t("ask.leave", t.name), TeamsText.t("action.leave"), true, () -> TeamActions.leave(null));
		}
	}

	public void disband() {
		TeamData t = TeamsView.team();
		if (t != null) {
			int land = TeamsView.held();
			this.dialogs
				.confirmTyped(
					TeamsText.t("ask.disband.title"),
					land > 0 ? TeamsText.t("ask.disband", t.name, TeamsText.chunks(land)) : TeamsText.t("ask.disband.no_land", t.name),
					t.name,
					TeamsText.t("action.disband"),
					() -> TeamActions.disband(null)
				);
		}
	}

	public void giveUpLand() {
		if (TeamsView.personal() != null) {
			this.dialogs
				.confirm(
					TeamsText.t("ask.give_up.title"),
					TeamsText.t("ask.give_up", TeamsText.chunks(TeamsView.held())),
					TeamsText.t("action.give_up"),
					true,
					() -> TeamActions.leave(null)
				);
		}
	}

	public void unally(TeamSummary other) {
		this.dialogs
			.confirm(TeamsText.t("ask.unally.title"), TeamsText.t("ask.unally", other.name), TeamsText.t("action.unally"), true, () -> TeamActions.unally(other.id));
	}

	public void ally(TeamSummary other) {
		TeamActions.ally(other.id);
	}

	public List<TeamSummary> allyCandidates() {
		TeamData mine = TeamsView.team();
		List<TeamSummary> out = new ArrayList<>();
		if (mine == null) {
			return out;
		} else {
			for (TeamSummary s : TeamsView.directory()) {
				if (!s.id.equals(mine.id) && !mine.isAlly(s.id)) {
					out.add(s);
				}
			}

			out.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
			return out;
		}
	}

	public void allyPick() {
		List<TeamSummary> from = this.allyCandidates();
		if (from.isEmpty()) {
			tell(TeamsText.t("allies.nobody"));
		} else {
			this.dialogs.pickTeam(TeamsText.t("allies.declare"), from, this::ally);
		}
	}

	public boolean canEdit(TeamData.Waypoint w) {
		return TeamsView.hasTeam() && (TeamsView.self().equals(w.by) || TeamsView.may(Permission.WAYPOINTS));
	}

	public void removeWaypoint(TeamData.Waypoint w) {
		this.dialogs
			.confirm(TeamsText.t("ask.waypoint.title"), TeamsText.t("ask.waypoint", w.name), TeamsText.t("action.remove"), true, () -> TeamActions.removeWaypoint(w.id));
	}

	public boolean shows(TeamData.Waypoint w) {
		return TeamsConfig.get().showsWaypoint(w.id);
	}

	public void setShown(TeamData.Waypoint w, boolean shown) {
		TeamsConfig c = TeamsConfig.get();
		if (c.hiddenWaypoints == null) {
			c.hiddenWaypoints = new ArrayList<>();
		}

		if (shown) {
			c.hiddenWaypoints.remove(w.id);
		} else if (!c.hiddenWaypoints.contains(w.id)) {
			c.hiddenWaypoints.add(w.id);
		}

		TeamsConfig.save();
	}

	public TeamFlows.Join joinState(TeamSummary s) {
		TeamData mine = TeamsView.team();
		if (mine != null) {
			return mine.id.equals(s.id) ? TeamFlows.Join.OWN : TeamFlows.Join.ELSEWHERE;
		} else if (TeamsView.requested().contains(s.id)) {
			return TeamFlows.Join.WITHDRAW;
		} else {
			for (InviteView invite : TeamsView.invites()) {
				if (invite.team.equals(s.id)) {
					return TeamFlows.Join.INVITED;
				}
			}

			if (s.members >= TeamsView.info().maxMembers) {
				return TeamFlows.Join.FULL;
			} else {
				return s.privacy == Privacy.OPEN ? TeamFlows.Join.JOIN : (s.privacy == Privacy.REQUEST ? TeamFlows.Join.ASK : TeamFlows.Join.CLOSED);
			}
		}
	}

	@Nullable
	public Component joinLabel(TeamFlows.Join state) {
		return switch (state) {
			case JOIN -> TeamsText.t("browse.join");
			case ASK -> TeamsText.t("browse.ask");
			case WITHDRAW -> TeamsText.t("browse.withdraw");
			default -> null;
			case INVITED -> TeamsText.t("browse.accept");
		};
	}

	@Nullable
	public Component joinWhyNot(TeamFlows.Join state) {
		return switch (state) {
			case OWN -> TeamsText.t("browse.own");
			default -> null;
			case CLOSED -> TeamsText.t("browse.closed");
			case ELSEWHERE -> TeamsText.t("browse.elsewhere");
			case FULL -> TeamsText.t("browse.full");
		};
	}

	public void join(TeamSummary s) {
		switch (this.joinState(s)) {
			case JOIN:
			case ASK:
				TeamActions.join(s.id, null);
				break;
			case WITHDRAW:
				TeamActions.cancelRequest(s.id);
			case CLOSED:
			default:
				break;
			case INVITED:
				TeamActions.acceptInvite(s.id, null);
		}
	}

	public void teamChat() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			unavailable();
		} else {
			mc.setScreen(new ChatScreen("/tc "));
		}
	}

	public void stash() {
		TeamActions.openStash();
	}

	public void goHome() {
		Minecraft mc = Minecraft.getInstance();
		Screen asked = mc.screen;
		TeamActions.send("go_home", new CompoundTag(), result -> {
			if (!result.ok()) {
				refused(result);
			} else {
				if (asked != null && mc.screen == asked && mc.level != null) {
					mc.setScreen(null);
				}
			}
		});
	}

	public void setHome() {
		TeamData t = TeamsView.team();
		if (t != null && t.home != null) {
			this.dialogs.confirm(TeamsText.t("ask.home.title"), TeamsText.t("ask.home"), TeamsText.t("action.set_home"), false, TeamActions::setHome);
		} else {
			TeamActions.setHome();
		}
	}

	public void clearHome() {
		this.dialogs.confirm(TeamsText.t("ask.clear_home.title"), TeamsText.t("ask.clear_home"), TeamsText.t("action.clear_home"), true, TeamActions::clearHome);
	}

	public static void set(String op, CompoundTag args, Runnable onRefused) {
		TeamActions.send(op, args, result -> {
			if (!result.ok()) {
				refused(result);
				onRefused.run();
			}
		});
	}

	public static void setLandRule(String rule, String value, Runnable onRefused) {
		CompoundTag args = new CompoundTag();
		args.putString("rule", rule);
		args.putString("value", value);
		set("set_land_rule", args, onRefused);
	}

	public static void refused(TeamsNet.Result result) {
		CFHGuiToasts.show(Component.translatable("cfhgui_teams.result.failed"), TeamActions.describe(result), Icon.WARNING);
	}

	public static void unavailable() {
		CFHGuiToasts.show(Component.translatable("cfhgui_teams.result.failed"), Component.translatable("cfhgui_teams.result.disabled"), Icon.WARNING);
	}

	public static void tell(Component what) {
		CFHGuiToasts.show(what, null, Icon.INFO);
	}

	public static Component reason(TeamsNet.Result result) {
		return TeamActions.describe(result);
	}

	public static boolean validName(String name) {
		String s = name.trim();
		if (s.length() >= 3 && s.length() <= 24) {
			for (int i = 0; i < s.length(); i++) {
				char c = s.charAt(i);
				if (!Character.isLetterOrDigit(c) && c != ' ' && c != '_' && c != '\'' && c != '-') {
					return false;
				}
			}

			return true;
		} else {
			return false;
		}
	}

	public static boolean validTag(String tag) {
		String s = tag.trim();
		if (s.isEmpty()) {
			return true;
		} else if (s.length() >= 2 && s.length() <= 5) {
			for (int i = 0; i < s.length(); i++) {
				if (!Character.isLetterOrDigit(s.charAt(i))) {
					return false;
				}
			}

			return true;
		} else {
			return false;
		}
	}

	public static enum Join {
		OWN,
		JOIN,
		ASK,
		WITHDRAW,
		CLOSED,
		INVITED,
		ELSEWHERE,
		FULL;
	}
}
