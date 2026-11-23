package com.onehumanawa.cfhcore.content.gui.crafting.team;

import com.onehumanawa.cfhcore.content.gui.teams.api.TeamsApi;
import com.onehumanawa.cfhcore.content.gui.teams.api.TeamsApi.MemberListener;
import com.onehumanawa.cfhcore.content.gui.teams.model.Rank;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData.Member;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;

final class TeamsBridge implements TeamsLink {
	static TeamsLink create() {
		return new TeamsBridge();
	}

	private TeamsBridge() {
	}

	@Nullable
	@Override
	public TeamsLink.TeamInfo teamOf(UUID player) {
		return info(TeamsApi.teamOf(player));
	}

	@Nullable
	@Override
	public TeamsLink.TeamInfo team(UUID team) {
		return info(TeamsApi.team(team));
	}

	@Override
	public void listen(BiConsumer<UUID, UUID> joined, BiConsumer<UUID, UUID> left, Consumer<UUID> disbanded) {
		TeamsApi.MEMBER_JOINED.register((MemberListener)(team, player) -> joined.accept(team.id, player));
		TeamsApi.MEMBER_LEFT.register((MemberListener)(team, player) -> left.accept(team.id, player));
		TeamsApi.TEAM_DISBANDED.register((Consumer<TeamData>)team -> disbanded.accept(team.id));
	}

	@Nullable
	private static TeamsLink.TeamInfo info(@Nullable TeamData team) {
		if (team == null) {
			return null;
		} else {
			List<UUID> members = new ArrayList<>(team.members.size());
			Set<UUID> officers = new HashSet<>();

			for (Member m : team.members) {
				members.add(m.id);
				if (m.rank != null && m.rank.atLeast(Rank.MODERATOR)) {
					officers.add(m.id);
				}
			}

			return new TeamsLink.TeamInfo(team.id, team.name, List.copyOf(members), Set.copyOf(officers));
		}
	}
}
