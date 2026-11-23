package com.onehumanawa.cfhcore.content.gui.teams.client.overhaul;

import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import com.onehumanawa.cfhcore.content.gui.teams.model.Rank;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

final class CampRoster {
	private int first;
	@Nullable
	private UUID chosen;
	private List<CampScene.Person> all = List.of();

	private static int rankColor(Rank rank, Palette p) {
		return switch (rank) {
			case OWNER -> p.warning();
			case ADMIN -> p.accent();
			case MODERATOR -> p.success();
			default -> p.textMuted();
		};
	}

	void refresh() {
		Palette p = Theme.current().palette();
		List<CampScene.Person> out = new ArrayList<>();
		TeamData t = TeamsView.team();
		if (t == null) {
			if (TeamsView.available()) {
				out.add(
					new CampScene.Person(
						TeamsView.self(),
						TeamsView.selfName(),
						CampScene.Kind.ALONE,
						true,
						TeamsView.personal() != null ? TeamsText.t("solo.share") : TeamsText.shared("state.no_team"),
						p.textDim(),
						false
					)
				);
			}

			this.all = out;
		} else {
			List<TeamData.Member> members = new ArrayList<>(t.members);
			members.sort(
				Comparator.<TeamData.Member, Integer>comparing(m -> -m.rank.ordinal())
					.thenComparing(m -> !TeamsView.online(m.id))
					.thenComparing(m -> m.name, String.CASE_INSENSITIVE_ORDER)
			);

			for (TeamData.Member m : members) {
				out.add(
					new CampScene.Person(m.id, m.name, CampScene.Kind.MEMBER, TeamsView.online(m.id), TeamsText.rank(m.rank), rankColor(m.rank, p), m.rank == Rank.OWNER)
				);
			}

			for (TeamData.Request r : t.requests) {
				out.add(new CampScene.Person(r.player, r.playerName, CampScene.Kind.REQUEST, true, OverhaulTeams.t("yard.asks"), p.warning(), false));
			}

			for (TeamData.Invite i : t.invites) {
				out.add(new CampScene.Person(i.player, i.playerName, CampScene.Kind.INVITE, true, OverhaulTeams.t("yard.invited"), p.textDim(), false));
			}

			this.all = out;
		}
	}

	List<CampScene.Person> all() {
		return this.all;
	}

	List<CampScene.Person> yard(int fitting) {
		this.first = Math.max(0, Math.min(this.first, Math.max(0, this.all.size() - fitting)));
		return this.all.subList(this.first, Math.min(this.all.size(), this.first + fitting));
	}

	int first() {
		return this.first;
	}

	boolean canTurn(int direction, int fitting) {
		return direction < 0 ? this.first > 0 : this.first + fitting < this.all.size();
	}

	void turn(int direction, int fitting) {
		if (this.canTurn(direction, fitting)) {
			this.first = this.first + direction * Math.max(1, fitting - 1);
		}
	}

	@Nullable
	UUID chosen() {
		return this.chosen;
	}

	void choose(@Nullable UUID id) {
		this.chosen = id;
	}

	@Nullable
	CampScene.Person chosenPerson() {
		if (this.chosen == null) {
			return null;
		} else {
			for (CampScene.Person p : this.all) {
				if (p.id().equals(this.chosen)) {
					return p;
				}
			}

			return null;
		}
	}

	void reveal(UUID id, int fitting) {
		for (int i = 0; i < this.all.size(); i++) {
			if (this.all.get(i).id().equals(id)) {
				if (i < this.first || i >= this.first + fitting) {
					this.first = Math.max(0, Math.min(i, Math.max(0, this.all.size() - fitting)));
				}

				return;
			}
		}
	}
}
