package com.onehumanawa.cfhcore.content.gui.teams.server;

import com.onehumanawa.cfhcore.content.gui.teams.api.TeamsApi;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import java.util.Collection;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

final class Backend implements TeamsApi.Backend {
	private final TeamService teams;

	Backend(TeamService teams) {
		this.teams = teams;
	}

	@Nullable
	@Override
	public TeamData teamOf(UUID player) {
		return this.teams.teamOf(player);
	}

	@Nullable
	@Override
	public TeamData team(UUID team) {
		return this.teams.team(team);
	}

	@Override
	public Collection<TeamData> teams() {
		return this.teams.teams();
	}

	@Override
	public void addXp(UUID team, long amount) {
		TeamData t = this.teams.team(team);
		if (t != null) {
			this.teams.addXp(t, amount);
		}
	}

	@Override
	public void count(UUID team, String counter, long delta) {
		TeamData t = this.teams.team(team);
		if (t != null) {
			this.teams.count(t, counter, delta);
		}
	}

	@Override
	public void log(UUID team, String type, UUID actor, String actorName, String subject) {
		TeamData t = this.teams.team(team);
		if (t != null && type != null && !type.isEmpty()) {
			this.teams.log(t, type, actor == null ? new UUID(0L, 0L) : actor, actorName, subject);
		}
	}

	@Nullable
	@Override
	public UUID claimOwner(String dimension, long chunk) {
		ClaimMap.Claim c = this.teams.claims.at(dimension, chunk);
		return c == null ? null : c.team.id;
	}

	@Override
	public int claimCount(UUID team) {
		return this.teams.claims.count(team);
	}
}
