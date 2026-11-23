package com.onehumanawa.cfhcore.content.gui.server.service;

import com.mojang.authlib.GameProfile;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink.Board;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink.PlayerRef;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink.Row;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink.Snapshot;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.mixin.server.StatsCounterAccessor;
import com.onehumanawa.cfhcore.content.gui.server.model.PlayerRecord;
import com.onehumanawa.cfhcore.content.gui.server.storage.WorldStore;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2LongMap.Entry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

public final class Statistics {
	static final int MAX_REFS = 2000;
	static final int BOARD_MAX = 100;
	private static final long RANKING_MS = 15000L;
	private static final int KEY_MAX = 200;
	private final ServerService s;
	private final Map<String, Statistics.Ranking> rankings = new LinkedHashMap<>();
	private final Map<String, Optional<Stat<?>>> parsed = new HashMap<>();
	private int changes;

	Statistics(ServerService s) {
		this.s = s;
	}

	void refresh() {
		if (this.s.live) {
			this.s.statsFiles.refresh(this.s.server.getWorldPath(LevelResource.PLAYER_STATS_DIR), this.s.saver, this.s.now());
		}
	}

	void tick() {
		if (this.s.live && this.s.now() - this.s.statsFiles.readAt() >= (long)this.s.rules().statsRefreshMinutes * 60000L) {
			this.refresh();
		}
	}

	void changed() {
		this.changes++;
		this.rankings.clear();
	}

	void left(ServerPlayer player) {
		this.s.statsFiles.put(player.getUUID(), this.live(player));
	}

	public OpResult players(Actor a) {
		if (!this.s.rules().stats) {
			return OpResult.fail("disabled");
		} else if (!this.s.may(a, Nodes.STATS_OTHERS)) {
			return OpResult.fail("no_permission");
		} else {
			List<PlayerRef> out = new ArrayList<>();
			Set<UUID> seen = new HashSet<>();

			for (Session session : this.s.online()) {
				if (this.s.visible(a, session)) {
					out.add(new PlayerRef(session.id, session.name, true));
					seen.add(session.id);
				}
			}

			List<WorldStore.Known> away = new ArrayList<>(this.s.store.all());
			away.sort(Comparator.<WorldStore.Known>comparingLong(kx -> this.s.lastSeen(a, kx)).reversed());

			for (WorldStore.Known k : away) {
				if (out.size() >= 2000) {
					break;
				}

				if (seen.add(k.id) && !k.name.isEmpty()) {
					out.add(new PlayerRef(k.id, k.name, false));
				}
			}

			return OpResult.answer(out.toArray(new PlayerRef[0]));
		}
	}

	public OpResult stats(Actor a, @Nullable ServerService.Target target, String wanted) {
		boolean own = target != null && target.id().equals(a.id());
		if (target == null) {
			return OpResult.fail("no_such_player", Text.line(wanted, 40));
		} else {
			if (!own) {
				if (!this.s.rules().stats) {
					return OpResult.fail("disabled");
				}

				if (!this.s.may(a, Nodes.STATS_OTHERS)) {
					return OpResult.fail("no_permission");
				}

				if (target.record().statsHidden && !this.s.may(a, Nodes.STATS_HIDDEN)) {
					return OpResult.fail("stats_hidden", Text.shown(target.record()));
				}
			}

			Session on = target.session();
			boolean unseen = on != null && !this.s.visible(a, on);
			Object2LongMap<String> numbers = this.numbers(target.id(), unseen);
			if (numbers == null) {
				return OpResult.fail("no_stats");
			} else {
				Map<String, Long> stats = new HashMap<>(numbers.size() * 2);
				ObjectIterator rec = numbers.object2LongEntrySet().iterator();

				while (rec.hasNext()) {
					Entry<String> e = (Entry<String>)rec.next();
					stats.put((String)e.getKey(), e.getLongValue());
				}

				PlayerRecord recx = target.record();
				Map<String, Long> extras = new LinkedHashMap<>();
				extras.put("afk_ticks", recx.afkTicks);
				extras.put("first_seen_ms", recx.firstSeen);
				extras.put("last_seen_ms", on == null ? recx.lastSeen : (unseen ? on.seenBefore : this.s.now()));
				extras.put("sessions", recx.sessions);
				return OpResult.answer(new Snapshot(target.id(), target.name(), stats, extras, this.s.now()));
			}
		}
	}

	public OpResult board(Actor a, @Nullable String rawKey, int offset, int count) {
		if (!this.s.rules().rankings) {
			return OpResult.fail("disabled");
		} else if (!this.s.may(a, Nodes.RANKINGS_USE)) {
			return OpResult.fail("no_permission");
		} else {
			String key = rawKey == null ? "" : rawKey.trim();
			if (!key.isEmpty() && key.length() <= 200 && knownKey(key)) {
				boolean hidden = this.s.may(a, Nodes.STATS_HIDDEN);
				Statistics.Ranking ranking = this.ranking(key, hidden);
				int from = Math.max(0, offset);
				int to = Math.min(ranking.ids.size(), from + Math.max(1, Math.min(100, count)));
				List<Row> rows = new ArrayList<>(Math.max(0, to - from));

				for (int i = from; i < to; i++) {
					rows.add(new Row(ranking.ids.get(i), this.name(ranking.ids.get(i)), ranking.values.get(i), i + 1));
				}

				Row self = null;
				if (!a.console()) {
					int at = ranking.ids.indexOf(a.id());
					if (at >= 0) {
						self = new Row(a.id(), this.name(a.id()), ranking.values.get(at), at + 1);
					} else {
						long mine = this.value(a.id(), key);
						if (mine > 0L) {
							int above = 0;

							for (long v : ranking.values) {
								if (v > mine) {
									above++;
								}
							}

							self = new Row(a.id(), this.name(a.id()), mine, above + 1);
						}
					}
				}

				return OpResult.answer(new Board(key, rows, ranking.ids.size(), self));
			} else {
				return OpResult.fail("invalid");
			}
		}
	}

	private static boolean extra(String key) {
		return key.equals("afk_ticks") || key.equals("first_seen_ms") || key.equals("last_seen_ms") || key.equals("sessions");
	}

	private static boolean knownKey(String key) {
		if (extra(key)) {
			return true;
		} else {
			int bar = key.indexOf(124);
			return bar > 0 && ResourceLocation.tryParse(key.substring(0, bar)) != null && ResourceLocation.tryParse(key.substring(bar + 1)) != null;
		}
	}

	long value(UUID id, String key) {
		return this.value(id, key, false);
	}

	long value(UUID id, String key, boolean unseen) {
		Session session = this.s.session(id);
		if (extra(key)) {
			long first;
			long last;
			long sessions;
			long afk;
			if (session != null) {
				afk = session.record.afkTicks;
				first = session.record.firstSeen;
				last = unseen ? session.seenBefore : this.s.now();
				sessions = session.record.sessions;
			} else {
				WorldStore.Known k = this.s.store.known(id);
				if (k == null) {
					return 0L;
				}

				afk = k.afkTicks;
				first = k.firstSeen;
				last = k.lastSeen;
				sessions = k.sessions;
			}
			return switch (key) {
				case "afk_ticks" -> afk;
				case "first_seen_ms" -> first;
				case "last_seen_ms" -> last;
				default -> sessions;
			};
		} else if (session != null && !unseen && session.player != null && this.s.live) {
			Stat<?> stat = this.stat(key);
			return stat == null ? 0L : (long)session.player.getStats().getValue(stat);
		} else {
			Object2LongMap<String> numbers = this.s.statsFiles.of(id);
			return numbers == null ? 0L : numbers.getLong(key);
		}
	}

	@Nullable
	private Object2LongMap<String> numbers(UUID id, boolean unseen) {
		Session session = this.s.session(id);
		if (session != null && !unseen && session.player != null && this.s.live) {
			return this.live(session.player);
		} else {
			Object2LongMap<String> read = this.s.statsFiles.of(id);
			if (read != null) {
				return read;
			} else {
				return this.s.store.known(id) != null ? new Object2LongOpenHashMap() : null;
			}
		}
	}

	Object2LongMap<String> live(ServerPlayer player) {
		Object2IntMap<Stat<?>> counted = ((StatsCounterAccessor)player.getStats()).cfhServer$stats();
		Object2LongOpenHashMap<String> out = new Object2LongOpenHashMap(counted.size() * 2);
		ObjectIterator var4 = counted.object2IntEntrySet().iterator();

		while (var4.hasNext()) {
			it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<Stat<?>> e = (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<Stat<?>>)var4.next();
			if (e.getIntValue() != 0) {
				String key = key((Stat)e.getKey());
				if (key != null) {
					out.put(this.s.statsFiles.key(key), (long)e.getIntValue());
				}
			}
		}

		return out;
	}

	@Nullable
	private static <T> String key(Stat<T> stat) {
		ResourceLocation type = BuiltInRegistries.STAT_TYPE.getKey(stat.getType());
		ResourceLocation value = stat.getType().getRegistry().getKey(stat.getValue());
		return type != null && value != null ? StatsLink.key(type.toString(), value.toString()) : null;
	}

	@Nullable
	private Stat<?> stat(String key) {
		Optional<Stat<?>> known = this.parsed.get(key);
		if (known == null) {
			if (this.parsed.size() > 2048) {
				this.parsed.clear();
			}

			known = Optional.ofNullable(parse(key));
			this.parsed.put(key, known);
		}

		return known.orElse(null);
	}

	@Nullable
	private static Stat<?> parse(String key) {
		int bar = key.indexOf(124);
		if (bar <= 0) {
			return null;
		} else {
			ResourceLocation typeId = ResourceLocation.tryParse(key.substring(0, bar));
			ResourceLocation valueId = ResourceLocation.tryParse(key.substring(bar + 1));
			if (typeId != null && valueId != null) {
				StatType<?> type = (StatType<?>)BuiltInRegistries.STAT_TYPE.get(typeId);
				return type == null ? null : of(type, valueId);
			} else {
				return null;
			}
		}
	}

	@Nullable
	private static <T> Stat<T> of(StatType<T> type, ResourceLocation valueId) {
		Registry<T> registry = type.getRegistry();
		if (!registry.containsKey(valueId)) {
			return null;
		} else {
			T value = (T)registry.get(valueId);
			return value == null ? null : type.get(value);
		}
	}

	private Statistics.Ranking ranking(String key, boolean withHidden) {
		String cacheKey = (withHidden ? "h|" : "v|") + key;
		long now = this.s.now();
		Statistics.Ranking r = this.rankings.get(cacheKey);
		if (r != null && now - r.madeAt < 15000L && now >= r.madeAt && r.generation == this.s.statsFiles.generation() + this.changes) {
			return r;
		} else {
			Set<UUID> candidates = new HashSet<>(this.s.statsFiles.players());
			candidates.addAll(this.s.sessions.keySet());
			if (extra(key)) {
				for (WorldStore.Known k : this.s.store.all()) {
					candidates.add(k.id);
				}
			}

			List<java.util.Map.Entry<UUID, Long>> rows = new ArrayList<>(candidates.size());

			for (UUID id : candidates) {
				Session session = this.s.session(id);
				if (!withHidden) {
					WorldStore.Known k = session == null ? this.s.store.known(id) : null;
					if (session != null ? session.record.statsHidden : k != null && k.statsHidden) {
						continue;
					}
				}

				long v = this.value(id, key, session != null && session.record.vanished);
				if (v > 0L) {
					rows.add(Map.entry(id, v));
				}
			}

			rows.sort((x, y) -> {
				int byValue = Long.compare(y.getValue(), x.getValue());
				return byValue != 0 ? byValue : x.getKey().compareTo(y.getKey());
			});
			r = new Statistics.Ranking();

			for (java.util.Map.Entry<UUID, Long> e : rows) {
				r.ids.add(e.getKey());
				r.values.add(e.getValue());
			}

			r.madeAt = now;
			r.generation = this.s.statsFiles.generation() + this.changes;
			if (this.rankings.size() >= 64) {
				this.rankings.clear();
			}

			this.rankings.put(cacheKey, r);
			return r;
		}
	}

	private String name(UUID id) {
		Session session = this.s.session(id);
		if (session != null) {
			return Text.shown(session.record);
		} else {
			WorldStore.Known k = this.s.store.known(id);
			if (k != null && !k.name.isEmpty()) {
				return Text.plainNick(k.shownName());
			} else {
				if (this.s.live && this.s.server.getProfileCache() != null) {
					Optional<GameProfile> profile = this.s.server.getProfileCache().get(id);
					if (profile.isPresent()) {
						return profile.get().getName();
					}
				}

				return id.toString().substring(0, 8);
			}
		}
	}

	private static final class Ranking {
		final List<UUID> ids = new ArrayList<>();
		final List<Long> values = new ArrayList<>();
		long madeAt;
		int generation;
	}
}
