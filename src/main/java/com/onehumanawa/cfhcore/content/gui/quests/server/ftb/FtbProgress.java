package com.onehumanawa.cfhcore.content.gui.quests.server.ftb;

import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.model.Reward;
import com.onehumanawa.cfhcore.content.gui.quests.server.Foreign;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

final class FtbProgress {
	private static final UUID NOBODY = new UUID(0L, 0L);

	static Map<UUID, Path> files(Path world) {
		Map<UUID, Path> out = new LinkedHashMap<>();
		Path dir = world.resolve("ftbquests");
		if (!Files.isDirectory(dir)) {
			return out;
		} else {
			try {
				try (Stream<Path> s = Files.list(dir)) {
					for (Path p : s.sorted().toList()) {
						String name = p.getFileName().toString();
						if (Files.isRegularFile(p) && name.endsWith(".snbt")) {
							UUID id = uuid(name.substring(0, name.length() - 5));
							if (id != null) {
								out.put(id, p);
							}
						}
					}
				}

				return out;
			} catch (IOException var10) {
				return out;
			}
		}
	}

	@Nullable
	static FtbProgress.Team read(UUID id, Path file, FtbImport.IdMap ids, BookIndex index, Collection<UUID> members) {
		Map<String, Object> data;
		String fingerprint;
		try {
			byte[] bytes = Files.readAllBytes(file);
			fingerprint = Integer.toHexString(Arrays.hashCode(bytes)) + "-" + bytes.length;
			data = Snbt.map(Snbt.parse(new String(bytes, StandardCharsets.UTF_8)));
		} catch (Exception var34) {
			return null;
		}

		Map<String, Object> counts = Snbt.map(data.get("task_progress"));
		Map<String, Object> started = Snbt.map(data.get("started"));
		Map<String, Object> completed = Snbt.map(data.get("completed"));
		Map<String, Object> claimed = Snbt.map(data.get("claimed_rewards"));
		Map<String, Object> times = Snbt.map(data.get("completion_count"));
		Map<String, long[]> tasks = new HashMap<>();
		Map<String, long[]> stamps = new HashMap<>();
		Map<String, Map<UUID, Long>> claims = new HashMap<>();
		int unknown = 0;

		for (Entry<String, Object> e : counts.entrySet()) {
			String at = ids.tasks.get(e.getKey().toUpperCase(Locale.ROOT));
			int hash = at == null ? -1 : at.lastIndexOf(35);
			if (hash < 0) {
				unknown++;
			} else {
				String key = at.substring(0, hash);
				int n = number(at.substring(hash + 1));
				if (n >= 0 && n < 64) {
					long[] row = tasks.computeIfAbsent(key, k -> new long[n + 1]);
					if (row.length <= n) {
						row = Arrays.copyOf(row, n + 1);
						tasks.put(key, row);
					}

					row[n] = Math.max(row[n], value(e.getValue()));
				}
			}
		}

		for (Entry<String, Object> ex : started.entrySet()) {
			String key = ids.quests.get(ex.getKey().toUpperCase(Locale.ROOT));
			if (key != null) {
				stamps.computeIfAbsent(key, k -> new long[3])[0] = Math.max(1L, value(ex.getValue()));
			}
		}

		for (Entry<String, Object> exx : completed.entrySet()) {
			String ftb = exx.getKey().toUpperCase(Locale.ROOT);
			String key = ids.quests.get(ftb);
			if (key != null) {
				stamps.computeIfAbsent(key, k -> new long[3])[1] = Math.max(1L, value(exx.getValue()));
			} else if (!ids.tasks.containsKey(ftb) && !ids.chapters.containsKey(ftb) && !ftb.matches("0*1")) {
				unknown++;
			}
		}

		for (Entry<String, Object> exxx : times.entrySet()) {
			String key = ids.quests.get(exxx.getKey().toUpperCase(Locale.ROOT));
			if (key != null) {
				stamps.computeIfAbsent(key, k -> new long[3])[2] = value(exxx.getValue());
			}
		}

		for (String claim : claimed.keySet()) {
			int colon = claim.indexOf(58);
			if (colon >= 0) {
				UUID who = uuid(claim.substring(0, colon));
				String at = ids.rewards.get(claim.substring(colon + 1).toUpperCase(Locale.ROOT));
				int hash = at == null ? -1 : at.lastIndexOf(35);
				if (who != null && hash >= 0) {
					String key = at.substring(0, hash);
					int n = number(at.substring(hash + 1));
					if (n >= 0 && n < 64) {
						claims.computeIfAbsent(key, k -> new HashMap<>()).merge(who, 1L << n, (a, b) -> a | b);
					}
				} else {
					unknown++;
				}
			}
		}

		Set<String> keys = new TreeSet<>(tasks.keySet());
		keys.addAll(stamps.keySet());
		keys.addAll(claims.keySet());
		List<Foreign.Quest> quests = new ArrayList<>(keys.size());

		for (String key : keys) {
			if (index.get(key) == null) {
				unknown++;
			} else {
				long[] stamp = stamps.getOrDefault(key, new long[3]);
				long teamBits = 0L;
				Map<String, Long> own = new HashMap<>();
				List<Reward> rewards = index.get(key).quest().rewards;

				for (Entry<UUID, Long> claimx : claims.getOrDefault(key, Map.of()).entrySet()) {
					UUID who = claimx.getKey();
					boolean everyone = who.equals(NOBODY) || who.equals(id) && !members.contains(id);

					for (int i = 0; i < rewards.size() && i < 64; i++) {
						long bit = 1L << i;
						if ((claimx.getValue() & bit) != 0L) {
							if (rewards.get(i).team) {
								teamBits |= bit;
							} else if (!everyone) {
								own.merge(who.toString(), bit, (a, b) -> a | b);
							} else {
								for (UUID member : members) {
									own.merge(member.toString(), bit, (a, b) -> a | b);
								}
							}
						}
					}
				}

				quests.add(
					new Foreign.Quest(
						key, tasks.getOrDefault(key, new long[0]), stamp[1] != 0L, stamp[0], stamp[1], (int)Math.min(2147483647L, Math.max(0L, stamp[2])), own, teamBits
					)
				);
			}
		}

		return new FtbProgress.Team(id, Snbt.string(data, "name", ""), fingerprint, quests, unknown);
	}

	static List<Foreign.Party> parties(Path world, Map<UUID, String> names) {
		List<Foreign.Party> out = new ArrayList<>();
		Path dir = world.resolve("ftbteams");

		for (Path p : snbt(dir.resolve("player"))) {
			Map<String, Object> data = file(p);
			UUID id = uuid(Snbt.string(data, "id", ""));
			String name = Snbt.string(data, "player_name", "");
			if (id != null && !name.isBlank()) {
				names.put(id, name);
			}
		}

		for (Path px : snbt(dir.resolve("party"))) {
			Map<String, Object> data = file(px);
			UUID id = uuid(Snbt.string(data, "id", ""));
			if (id != null) {
				Map<String, Object> properties = Snbt.map(data.get("properties"));
				Map<UUID, Foreign.Member> members = new LinkedHashMap<>();
				UUID owner = null;

				for (Entry<String, Object> rank : Snbt.map(data.get("ranks")).entrySet()) {
					UUID who = uuid(rank.getKey());
					String what = String.valueOf(rank.getValue()).toLowerCase(Locale.ROOT);
					if (who != null && (what.equals("owner") || what.equals("officer") || what.equals("member"))) {
						members.put(who, new Foreign.Member(names.getOrDefault(who, who.toString().substring(0, 8)), what.equals("officer")));
						if (what.equals("owner")) {
							owner = who;
						}
					}
				}

				if (owner != null && !members.isEmpty()) {
					String color = Snbt.string(properties, "ftbteams:color", "");
					int rgb = -1;

					try {
						if (color.startsWith("#") && color.length() == 7) {
							rgb = Integer.parseInt(color.substring(1), 16);
						}
					} catch (NumberFormatException var15) {
						rgb = -1;
					}

					out.add(new Foreign.Party(id, Snbt.string(properties, "ftbteams:display_name", ""), rgb, owner, members));
				}
			}
		}

		return out;
	}

	private static List<Path> snbt(Path dir) {
		if (!Files.isDirectory(dir)) {
			return List.of();
		} else {
			try {
				List var2;
				try (Stream<Path> s = Files.list(dir)) {
					var2 = s.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".snbt")).sorted().toList();
				}

				return var2;
			} catch (IOException var6) {
				return List.of();
			}
		}
	}

	private static Map<String, Object> file(Path path) {
		try {
			return Snbt.map(Snbt.parse(Files.readString(path, StandardCharsets.UTF_8)));
		} catch (Exception var2) {
			return Map.of();
		}
	}

	private static long value(@Nullable Object v) {
		return v instanceof Number n ? n.longValue() : 0L;
	}

	private static int number(String s) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException var2) {
			return -1;
		}
	}

	@Nullable
	static UUID uuid(String text) {
		String s = text.trim();
		if (s.length() == 32) {
			s = s.substring(0, 8) + "-" + s.substring(8, 12) + "-" + s.substring(12, 16) + "-" + s.substring(16, 20) + "-" + s.substring(20);
		}

		try {
			return s.length() == 36 ? UUID.fromString(s) : null;
		} catch (IllegalArgumentException var3) {
			return null;
		}
	}

	private FtbProgress() {
	}

	static record Team(UUID id, String name, String fingerprint, List<Foreign.Quest> quests, int unknown) {
	}
}
