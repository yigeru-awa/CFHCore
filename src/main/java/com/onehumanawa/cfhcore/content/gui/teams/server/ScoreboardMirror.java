package com.onehumanawa.cfhcore.content.gui.teams.server;

import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

final class ScoreboardMirror {
	private static final Pattern OURS = Pattern.compile("cfhgui_[0-9a-f]{8}");
	private final TeamService teams;

	ScoreboardMirror(TeamService teams) {
		this.teams = teams;
	}

	private boolean on() {
		return this.teams.live && this.teams.cfg().scoreboardTeams;
	}

	static String nameOf(TeamData t) {
		return "cfhgui_" + t.id.toString().substring(0, 8);
	}

	void syncAll() {
		if (this.teams.live) {
			Scoreboard board = this.teams.server.getScoreboard();
			Set<String> wanted = new HashSet<>();
			if (this.on()) {
				for (TeamData t : this.teams.store.all()) {
					if (!t.solo) {
						wanted.add(nameOf(t));
						this.update(t);
					}
				}
			}

			for (PlayerTeam pt : new ArrayList<PlayerTeam>(board.getPlayerTeams())) {
				if (OURS.matcher(pt.getName()).matches() && !wanted.contains(pt.getName())) {
					board.removePlayerTeam(pt);
				}
			}
		}
	}

	void update(TeamData t) {
		if (this.on() && !t.solo) {
			Scoreboard board = this.teams.server.getScoreboard();
			String name = nameOf(t);
			PlayerTeam pt = board.getPlayerTeam(name);
			if (pt == null) {
				pt = board.addPlayerTeam(name);
			}

			Component display = Component.literal(t.name);
			if (!pt.getDisplayName().equals(display)) {
				pt.setDisplayName(display);
			}

			Component prefix = t.tag.isEmpty()
				? Component.empty()
				: Component.literal("[" + t.tag + "] ").withStyle(Style.EMPTY.withColor(Names.nearestFormatting(t.color)));
			if (!pt.getPlayerPrefix().equals(prefix)) {
				pt.setPlayerPrefix(prefix);
			}

			if (pt.isAllowFriendlyFire() != t.friendlyFire) {
				pt.setAllowFriendlyFire(t.friendlyFire);
			}

			Set<String> members = new HashSet<>();

			for (TeamData.Member m : t.members) {
				if (!m.name.isEmpty()) {
					members.add(m.name);
				}
			}

			for (String have : new ArrayList<String>(pt.getPlayers())) {
				if (!members.contains(have)) {
					board.removePlayerFromTeam(have, pt);
				}
			}

			for (String member : members) {
				PlayerTeam current = board.getPlayersTeam(member);
				if (current != pt && (current == null || OURS.matcher(current.getName()).matches())) {
					board.addPlayerToTeam(member, pt);
				}
			}
		}
	}

	void rename(TeamData t, String oldName, String newName) {
		if (this.on()) {
			Scoreboard board = this.teams.server.getScoreboard();
			PlayerTeam pt = board.getPlayerTeam(nameOf(t));
			if (pt != null && pt.getPlayers().contains(oldName)) {
				board.removePlayerFromTeam(oldName, pt);
			}
		}
	}

	void remove(TeamData t) {
		if (this.teams.live) {
			Scoreboard board = this.teams.server.getScoreboard();
			PlayerTeam pt = board.getPlayerTeam(nameOf(t));
			if (pt != null) {
				board.removePlayerTeam(pt);
			}
		}
	}
}
