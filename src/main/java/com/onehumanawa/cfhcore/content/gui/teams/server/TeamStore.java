package com.onehumanawa.cfhcore.content.gui.teams.server;

import com.google.gson.reflect.TypeToken;
import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import com.onehumanawa.cfhcore.content.gui.teams.CFHGuiTeams;
import com.onehumanawa.cfhcore.content.gui.teams.model.Rank;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import org.jetbrains.annotations.Nullable;

public final class TeamStore {
	static final long SAVE_AFTER_MS = 5000L;
	@Nullable
	private final Path dir;
	private final Map<UUID, TeamData> teams = new LinkedHashMap<>();
	private final Map<UUID, TeamData> byMember = new HashMap<>();
	private final Map<String, TeamData> byName = new HashMap<>();
	private final Map<String, TeamData> byTag = new HashMap<>();
	private final Map<UUID, Set<UUID>> invitedBy = new HashMap<>();
	private final Map<UUID, Set<UUID>> askedOf = new HashMap<>();
	private final Map<UUID, String> names = new HashMap<>();
	private final Map<String, UUID> idsByName = new HashMap<>();
	private final Map<UUID, String> written = new HashMap<>();
	private boolean dirty;
	private boolean playersDirty;
	private long dirtySince;

	TeamStore(@Nullable Path dir) {
		this.dir = dir;
	}

	public Collection<TeamData> all() {
		return Collections.unmodifiableCollection(this.teams.values());
	}

	int size() {
		return this.teams.size();
	}

	@Nullable
	public TeamData get(@Nullable UUID id) {
		return id == null ? null : this.teams.get(id);
	}

	@Nullable
	public TeamData ofPlayer(@Nullable UUID player) {
		return player == null ? null : this.byMember.get(player);
	}

	@Nullable
	TeamData named(@Nullable String name) {
		return name == null ? null : this.byName.get(key(name));
	}

	@Nullable
	TeamData tagged(@Nullable String tag) {
		return tag != null && !tag.isEmpty() ? this.byTag.get(key(tag)) : null;
	}

	Set<UUID> invitersOf(UUID player) {
		Set<UUID> s = this.invitedBy.get(player);
		return s == null ? Set.of() : s;
	}

	List<UUID> invited() {
		return (List<UUID>)(this.invitedBy.isEmpty() ? List.of() : new ArrayList<>(this.invitedBy.keySet()));
	}

	Set<UUID> askedBy(UUID player) {
		Set<UUID> s = this.askedOf.get(player);
		return s == null ? Set.of() : s;
	}

	static String key(String name) {
		return name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
	}

	void add(TeamData team) {
		this.teams.put(team.id, team);
		if (!team.solo) {
			this.byName.put(key(team.name), team);
			if (!team.tag.isEmpty()) {
				this.byTag.put(key(team.tag), team);
			}
		}

		for (TeamData.Member m : team.members) {
			this.byMember.put(m.id, team);
		}

		for (TeamData.Invite i : team.invites) {
			this.invitedBy.computeIfAbsent(i.player, k -> new HashSet<>()).add(team.id);
		}

		for (TeamData.Request r : team.requests) {
			this.askedOf.computeIfAbsent(r.player, k -> new HashSet<>()).add(team.id);
		}

		this.touch(team);
	}

	void remove(TeamData team) {
		this.teams.remove(team.id);
		this.byName.remove(key(team.name), team);
		if (!team.tag.isEmpty()) {
			this.byTag.remove(key(team.tag), team);
		}

		for (TeamData.Member m : team.members) {
			this.byMember.remove(m.id, team);
		}

		for (TeamData.Invite i : team.invites) {
			unlink(this.invitedBy, i.player, team.id);
		}

		for (TeamData.Request r : team.requests) {
			unlink(this.askedOf, r.player, team.id);
		}

		this.written.remove(team.id);
		this.markDirty();
	}

	void rename(TeamData team, String name) {
		this.byName.remove(key(team.name), team);
		team.name = name;
		if (!team.solo) {
			this.byName.put(key(name), team);
		}

		this.touch(team);
	}

	void retag(TeamData team, String tag) {
		if (!team.tag.isEmpty()) {
			this.byTag.remove(key(team.tag), team);
		}

		team.tag = tag;
		if (!tag.isEmpty() && !team.solo) {
			this.byTag.put(key(tag), team);
		}

		this.touch(team);
	}

	void becomeTeam(TeamData team, String name, String tag) {
		team.solo = false;
		team.name = name;
		team.tag = tag;
		this.byName.put(key(name), team);
		if (!tag.isEmpty()) {
			this.byTag.put(key(tag), team);
		}

		this.touch(team);
	}

	void addMember(TeamData team, TeamData.Member member) {
		team.members.add(member);
		this.byMember.put(member.id, team);
		this.touch(team);
	}

	@Nullable
	TeamData.Member removeMember(TeamData team, UUID player) {
		TeamData.Member m = team.member(player);
		if (m == null) {
			return null;
		} else {
			team.members.remove(m);
			this.byMember.remove(player, team);
			this.touch(team);
			return m;
		}
	}

	void addInvite(TeamData team, TeamData.Invite invite) {
		team.invites.removeIf(i -> i.player.equals(invite.player));
		team.invites.add(invite);
		this.invitedBy.computeIfAbsent(invite.player, k -> new HashSet<>()).add(team.id);
		this.touch(team);
	}

	@Nullable
	TeamData.Invite removeInvite(TeamData team, UUID player) {
		TeamData.Invite found = null;

		for (TeamData.Invite i : team.invites) {
			if (i.player.equals(player)) {
				found = i;
				break;
			}
		}

		if (found == null) {
			return null;
		} else {
			team.invites.remove(found);
			unlink(this.invitedBy, player, team.id);
			this.touch(team);
			return found;
		}
	}

	void addRequest(TeamData team, TeamData.Request request) {
		team.requests.removeIf(r -> r.player.equals(request.player));
		team.requests.add(request);
		this.askedOf.computeIfAbsent(request.player, k -> new HashSet<>()).add(team.id);
		this.touch(team);
	}

	@Nullable
	TeamData.Request removeRequest(TeamData team, UUID player) {
		TeamData.Request found = null;

		for (TeamData.Request r : team.requests) {
			if (r.player.equals(player)) {
				found = r;
				break;
			}
		}

		if (found == null) {
			return null;
		} else {
			team.requests.remove(found);
			unlink(this.askedOf, player, team.id);
			this.touch(team);
			return found;
		}
	}

	private static void unlink(Map<UUID, Set<UUID>> index, UUID player, UUID team) {
		Set<UUID> s = index.get(player);
		if (s != null) {
			s.remove(team);
			if (s.isEmpty()) {
				index.remove(player);
			}
		}
	}

	void touch(TeamData team) {
		this.written.remove(team.id);
		this.markDirty();
	}

	private void markDirty() {
		if (!this.dirty && !this.playersDirty) {
			this.dirtySince = System.currentTimeMillis();
		}

		this.dirty = true;
	}

	void remember(UUID id, String name) {
		if (name != null && !name.isEmpty()) {
			String old = this.names.put(id, name);
			if (!name.equals(old)) {
				if (old != null) {
					this.idsByName.remove(old.toLowerCase(Locale.ROOT), id);
				}

				this.idsByName.put(name.toLowerCase(Locale.ROOT), id);
				if (!this.dirty && !this.playersDirty) {
					this.dirtySince = System.currentTimeMillis();
				}

				this.playersDirty = true;
			}
		}
	}

	@Nullable
	String nameOf(UUID id) {
		return this.names.get(id);
	}

	@Nullable
	UUID idOf(String name) {
		return name == null ? null : this.idsByName.get(name.toLowerCase(Locale.ROOT));
	}

	Collection<String> knownNames() {
		return Collections.unmodifiableCollection(this.names.values());
	}

	void load() {
		if (this.dir != null) {
			Path file = this.dir.resolve("teams.json");
			if (Files.isRegularFile(file)) {
				try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
					TeamStore.TeamsFile f = (TeamStore.TeamsFile)JsonConfig.GSON.fromJson(r, TeamStore.TeamsFile.class);
					if (f != null && f.teams != null) {
						for (TeamData t : f.teams) {
							if (t != null) {
								this.adopt(t.sanitize());
							}
						}
					}
				} catch (Exception var14) {
					keepAside(file);
					CFHGuiTeams.LOGGER.error("[CFHGui Teams] {} could not be read; it was kept as teams.json.broken and the server starts without teams", file, var14);
				}
			}

			Path players = this.dir.resolve("players.json");
			if (Files.isRegularFile(players)) {
				try (Reader rx = Files.newBufferedReader(players, StandardCharsets.UTF_8)) {
					Map<String, String> m = (Map<String, String>)JsonConfig.GSON.fromJson(rx, (new TypeToken<Map<String, String>>() {
					}).getType());
					if (m != null) {
						for (Entry<String, String> e : m.entrySet()) {
							try {
								this.remember(UUID.fromString(e.getKey()), e.getValue());
							} catch (IllegalArgumentException var9) {
							}
						}
					}
				} catch (Exception var12) {
					CFHGuiTeams.LOGGER.warn("[CFHGui Teams] {} could not be read: {}", players, var12.toString());
				}
			}

			for (TeamData tx : this.teams.values()) {
				for (TeamData.Member m : tx.members) {
					this.remember(m.id, m.name);
				}
			}

			this.dirty = false;
			this.playersDirty = false;
		}
	}

	private void adopt(TeamData t) {
		t.members.removeIf(m -> this.byMember.containsKey(m.id));
		Set<UUID> seen = new HashSet<>();
		t.members.removeIf(m -> !seen.add(m.id));
		if (!t.members.isEmpty() && !this.teams.containsKey(t.id)) {
			TeamData.Member owner = null;

			for (TeamData.Member m : t.members) {
				if (m.rank == Rank.OWNER) {
					if (owner == null) {
						owner = m;
					} else {
						m.rank = Rank.ADMIN;
					}
				}
			}

			if (owner == null) {
				t.members.get(0).rank = Rank.OWNER;
			}

			if (t.solo && t.members.size() > 1) {
				t.solo = false;
			}

			if (t.solo) {
				t.tag = "";
				if (t.name.isBlank()) {
					t.name = t.members.get(0).name;
				}
			} else {
				String name = t.name.isBlank() ? "Team" : t.name.trim();

				for (int n = 2; this.byName.containsKey(key(name)); n++) {
					name = t.name.trim() + " " + n;
				}

				t.name = name;
			}

			if (!t.tag.isEmpty() && this.byTag.containsKey(key(t.tag))) {
				t.tag = "";
			}

			t.invites.removeIf(i -> t.isMember(i.player));
			t.requests.removeIf(r -> t.isMember(r.player));
			this.add(t);
		} else {
			CFHGuiTeams.LOGGER.warn("[CFHGui Teams] the team \"{}\" in teams.json has no members of its own (or its id twice) and was left out", t.name);
		}
	}

	private static void keepAside(Path file) {
		try {
			Files.copy(file, file.resolveSibling(file.getFileName() + ".broken"), StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception var2) {
		}
	}

	void tick(Saver saver) {
		if ((this.dirty || this.playersDirty) && System.currentTimeMillis() - this.dirtySince >= 5000L) {
			this.save(saver);
		}
	}

	void save(Saver saver) {
		if (this.dir == null) {
			this.dirty = false;
			this.playersDirty = false;
		} else {
			if (this.dirty) {
				this.dirty = false;
				StringBuilder sb = new StringBuilder(4096 + this.teams.size() * 2048);
				sb.append("{\n  \"version\": 1,\n  \"teams\": [\n");
				boolean first = true;

				for (TeamData t : this.teams.values()) {
					if (!first) {
						sb.append(",\n");
					}

					first = false;
					sb.append(this.written.computeIfAbsent(t.id, id -> JsonConfig.GSON.toJson(t)));
				}

				sb.append("\n  ]\n}\n");
				saver.text(this.dir.resolve("teams.json"), sb.toString());
			}

			if (this.playersDirty) {
				this.playersDirty = false;
				Map<String, String> out = new LinkedHashMap<>();

				for (Entry<UUID, String> e : this.names.entrySet()) {
					out.put(e.getKey().toString(), e.getValue());
				}

				saver.text(this.dir.resolve("players.json"), JsonConfig.GSON.toJson(out));
			}
		}
	}

	private static final class TeamsFile {
		int version = 1;
		List<TeamData> teams = new ArrayList<>();
	}
}
