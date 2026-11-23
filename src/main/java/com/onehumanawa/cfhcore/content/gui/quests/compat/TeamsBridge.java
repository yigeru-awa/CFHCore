package com.onehumanawa.cfhcore.content.gui.quests.compat;

import com.onehumanawa.cfhcore.content.gui.quests.server.QuestsServer;
import com.onehumanawa.cfhcore.content.gui.quests.server.TeamLink;
import com.onehumanawa.cfhcore.content.gui.teams.api.TeamsApi;
import com.onehumanawa.cfhcore.content.gui.teams.api.TeamsApi.MemberListener;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import java.util.UUID;
import java.util.function.Consumer;

public final class TeamsBridge implements TeamLink {
	private static TeamsBridge instance;

	public static synchronized TeamLink install() {
		if (instance == null) {
			instance = new TeamsBridge();
			TeamsApi.MEMBER_JOINED.register((MemberListener)(team, player) -> QuestsServer.teamJoined(team.id, player));
			TeamsApi.MEMBER_LEFT.register((MemberListener)(team, player) -> QuestsServer.teamLeft(team.id, player));
			TeamsApi.TEAM_DISBANDED.register((Consumer<TeamData>)team -> QuestsServer.teamDisbanded(team.id));
		}

		return instance;
	}

	private TeamsBridge() {
	}

	@Override
	public UUID holderOf(UUID player) {
		return TeamsApi.holderOf(player);
	}

	@Override
	public boolean isTeam(UUID holder) {
		return TeamsApi.team(holder) != null;
	}

	@Override
	public String name(UUID holder) {
		TeamData team = TeamsApi.team(holder);
		return team == null ? "" : team.name;
	}

	@Override
	public long figure(UUID holder, String what) {
		TeamData team = TeamsApi.team(holder);
		if (team == null) {
			return TeamLink.NONE.figure(holder, what);
		} else {
			return switch (what) {
				case "members" -> (long)team.members.size();
				case "level" -> (long)team.level;
				case "claims" -> (long)TeamsApi.claimCount(team.id);
				default -> team.counter(what);
			};
		}
	}

	@Override
	public void addXp(UUID team, long amount) {
		TeamsApi.addXp(team, amount);
	}

	@Override
	public void count(UUID team, String counter, long delta) {
		TeamsApi.count(team, counter, delta);
	}

	@Override
	public void log(UUID team, String type, UUID actor, String actorName, String subject) {
		TeamsApi.log(team, type, actor, actorName, subject);
	}
}
