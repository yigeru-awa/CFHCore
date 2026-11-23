package com.onehumanawa.cfhcore.content.gui.teams.api;

import com.onehumanawa.cfhcore.content.gui.core.event.Event;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;

public final class TeamsApi {
	public static final Event<TeamsApi.MemberListener> MEMBER_JOINED = new Event("TEAMS_MEMBER_JOINED");
	public static final Event<TeamsApi.MemberListener> MEMBER_LEFT = new Event("TEAMS_MEMBER_LEFT");
	public static final Event<Consumer<TeamData>> TEAM_CREATED = new Event("TEAMS_TEAM_CREATED");
	public static final Event<Consumer<TeamData>> TEAM_DISBANDED = new Event("TEAMS_TEAM_DISBANDED");
	@Nullable
	private static volatile TeamsApi.Backend backend;

	public static void install(@Nullable TeamsApi.Backend b) {
		backend = b;
	}

	public static boolean running() {
		return backend != null;
	}

	@Nullable
	public static TeamData teamOf(UUID player) {
		TeamsApi.Backend b = backend;
		return b == null ? null : b.teamOf(player);
	}

	@Nullable
	public static TeamData team(UUID team) {
		TeamsApi.Backend b = backend;
		return b == null ? null : b.team(team);
	}

	public static Collection<TeamData> teams() {
		TeamsApi.Backend b = backend;
		return (Collection<TeamData>)(b == null ? List.of() : b.teams());
	}

	public static UUID holderOf(UUID player) {
		TeamData t = teamOf(player);
		return t == null ? player : t.id;
	}

	public static boolean sameTeam(UUID a, UUID b) {
		TeamData t = teamOf(a);
		return t != null && t.isMember(b);
	}

	public static void addXp(UUID team, long amount) {
		TeamsApi.Backend b = backend;
		if (b != null && amount > 0L) {
			b.addXp(team, amount);
		}
	}

	public static void count(UUID team, String counter, long delta) {
		TeamsApi.Backend b = backend;
		if (b != null && delta != 0L) {
			b.count(team, counter, delta);
		}
	}

	public static void log(UUID team, String type, UUID actor, String actorName, String subject) {
		TeamsApi.Backend b = backend;
		if (b != null) {
			b.log(team, type, actor, actorName, subject);
		}
	}

	@Nullable
	public static UUID claimOwner(String dimension, long chunk) {
		TeamsApi.Backend b = backend;
		return b == null ? null : b.claimOwner(dimension, chunk);
	}

	public static int claimCount(UUID team) {
		TeamsApi.Backend b = backend;
		return b == null ? 0 : b.claimCount(team);
	}

	private TeamsApi() {
	}

	public interface Backend {
		@Nullable
		TeamData teamOf(UUID var1);

		@Nullable
		TeamData team(UUID var1);

		Collection<TeamData> teams();

		void addXp(UUID var1, long var2);

		void count(UUID var1, String var2, long var3);

		void log(UUID var1, String var2, UUID var3, String var4, String var5);

		@Nullable
		UUID claimOwner(String var1, long var2);

		int claimCount(UUID var1);
	}

	@FunctionalInterface
	public interface MemberListener {
		void changed(TeamData var1, UUID var2);
	}
}
