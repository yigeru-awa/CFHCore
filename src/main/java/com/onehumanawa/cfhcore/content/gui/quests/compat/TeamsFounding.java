package com.onehumanawa.cfhcore.content.gui.quests.compat;

import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import com.onehumanawa.cfhcore.content.gui.quests.server.Foreign;
import com.onehumanawa.cfhcore.content.gui.teams.api.TeamsApi;
import com.onehumanawa.cfhcore.content.gui.teams.model.Privacy;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData.Member;
import com.onehumanawa.cfhcore.content.gui.teams.server.Actor;
import com.onehumanawa.cfhcore.content.gui.teams.server.Names;
import com.onehumanawa.cfhcore.content.gui.teams.server.OpResult;
import com.onehumanawa.cfhcore.content.gui.teams.server.TeamService;
import com.onehumanawa.cfhcore.content.gui.teams.server.TeamsServer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.server.MinecraftServer;

public final class TeamsFounding {
	public static Map<UUID, UUID> found(MinecraftServer server, Collection<Foreign.Party> parties) {
		Map<UUID, UUID> out = new HashMap<>();
		TeamService service = TeamsServer.service();
		if (service == null) {
			return out;
		} else {
			for (Foreign.Party party : parties) {
				try {
					UUID team = found(service, party);
					if (team != null) {
						out.put(party.id(), team);
					}
				} catch (RuntimeException var7) {
					CFHGuiQuests.LOGGER.warn("[CFHGui Quests] the party \"{}\" could not be made a team: {}", party.name(), var7.toString());
				}
			}

			return out;
		}
	}

	private static UUID found(TeamService service, Foreign.Party party) {
		Foreign.Member leader = party.members().get(party.owner());
		if (leader == null) {
			return null;
		} else {
			Actor owner = Actor.fake(party.owner(), leader.name(), "minecraft:overworld", 0, 0).asAdmin();
			TeamData team = TeamsApi.teamOf(party.owner());
			boolean made = false;
			if (team != null) {
				for (Member m : team.members) {
					if (!party.members().containsKey(m.id)) {
						CFHGuiQuests.LOGGER
							.info(
								"[CFHGui Quests] the leader of the party \"{}\" is in the team \"{}\" with others: the party's progress stays with each of its members",
								party.name(),
								team.name
							);
						return null;
					}
				}
			} else {
				String name = Names.team(party.name());
				OpResult r = name == null ? null : service.create(owner, name, "", party.color() < 0 ? null : party.color());

				for (int n = 1; (r == null || !r.ok()) && n <= 9; n++) {
					name = Names.team(cut(leader.name(), 20) + (n == 1 ? "" : " " + n));
					if (name == null) {
						name = "Party " + party.id().toString().substring(0, 8) + (n == 1 ? "" : " " + n);
					}

					r = service.create(owner, name, "", party.color() < 0 ? null : party.color());
				}

				team = TeamsApi.teamOf(party.owner());
				if (team == null) {
					return null;
				}

				made = true;
			}

			UUID id = team.id;
			Privacy before = team.privacy;
			boolean opened = false;

			for (Entry<UUID, Foreign.Member> mx : party.members().entrySet()) {
				if (!mx.getKey().equals(party.owner()) && TeamsApi.teamOf(mx.getKey()) == null) {
					if (!opened && before != Privacy.OPEN) {
						service.setPrivacy(owner, Privacy.OPEN);
						opened = true;
					}

					OpResult joined = service.join(Actor.fake(mx.getKey(), mx.getValue().name(), "minecraft:overworld", 0, 0), id);
					if (joined.ok() && mx.getValue().officer()) {
						service.promote(owner, mx.getKey());
					}

					if (!joined.ok()) {
						CFHGuiQuests.LOGGER
							.info(
								"[CFHGui Quests] {} of the party \"{}\" did not get into the team ({}): their progress stays their own",
								new Object[]{mx.getValue().name(), party.name(), joined.code()}
							);
					}
				}
			}

			if (opened) {
				service.setPrivacy(owner, made ? Privacy.INVITE : before);
			}

			return id;
		}
	}

	private static String cut(String s, int max) {
		return s.length() > max ? s.substring(0, max) : s;
	}

	private TeamsFounding() {
	}
}
