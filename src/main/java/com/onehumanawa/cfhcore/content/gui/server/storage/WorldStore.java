package com.onehumanawa.cfhcore.content.gui.server.storage;

import com.google.gson.JsonParseException;
import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import com.onehumanawa.cfhcore.content.gui.server.CFHGuiServer;
import com.onehumanawa.cfhcore.content.gui.server.model.Place;
import com.onehumanawa.cfhcore.content.gui.server.model.PlayerRecord;
import com.onehumanawa.cfhcore.content.gui.server.model.Punishment;
import com.onehumanawa.cfhcore.content.gui.server.model.Role;
import com.onehumanawa.cfhcore.content.gui.server.model.Warp;
import com.onehumanawa.cfhcore.content.gui.server.model.WarpList;
import com.onehumanawa.cfhcore.content.gui.server.service.Text;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Nullable;

public final class WorldStore {
	public static final long SAVE_AFTER_MS = 4000L;
	public static final int CACHE = 128;
	private static final long RETRY_MS = 10000L;
	@Nullable
	private final Path dir;
	private final Map<UUID, PlayerRecord> held = new HashMap<>();
	private final LinkedHashMap<UUID, PlayerRecord> cache = new LinkedHashMap<>(64, 0.75F, true);
	private final Map<UUID, WorldStore.Known> known = new ConcurrentHashMap<>();
	private final Map<String, UUID> byName = new HashMap<>();
	private final Map<String, UUID> byNick = new HashMap<>();
	private final Set<UUID> dirty = new LinkedHashSet<>();
	private final Map<UUID, String> writing = new ConcurrentHashMap<>();
	private final Set<UUID> unsaved = new HashSet<>();
	private final Map<UUID, Long> unreadable = new HashMap<>();
	private WarpList warps = new WarpList();
	private boolean namesDirty;
	private boolean warpsDirty;
	private long dirtySince;

	public WorldStore(@Nullable Path dir) {
		this.dir = dir;
	}

	@Nullable
	public Path dir() {
		return this.dir;
	}

	public Collection<WorldStore.Known> all() {
		return Collections.unmodifiableCollection(this.known.values());
	}

	public int size() {
		return this.known.size();
	}

	@Nullable
	public WorldStore.Known known(@Nullable UUID id) {
		return id == null ? null : this.known.get(id);
	}

	@Nullable
	public UUID idOf(@Nullable String name) {
		if (name != null && !name.isEmpty()) {
			String key = name.toLowerCase(Locale.ROOT);
			UUID id = this.byName.get(key);
			return id != null ? id : this.byNick.get(key);
		} else {
			return null;
		}
	}

	@Nullable
	public UUID idOfAccount(@Nullable String name) {
		return name != null && !name.isEmpty() ? this.byName.get(name.toLowerCase(Locale.ROOT)) : null;
	}

	public boolean inMemory(UUID id) {
		return this.held.containsKey(id) || this.cache.containsKey(id) || !this.known.containsKey(id);
	}

	public boolean nameTaken(String plain, UUID except) {
		String key = plain.toLowerCase(Locale.ROOT);
		UUID a = this.byName.get(key);
		UUID b = this.byNick.get(key);
		return a != null && !a.equals(except) || b != null && !b.equals(except);
	}

	@Nullable
	public PlayerRecord record(@Nullable UUID id) {
		if (id == null) {
			return null;
		} else {
			PlayerRecord rec = this.held.get(id);
			if (rec != null) {
				return rec;
			} else {
				rec = this.cache.get(id);
				if (rec != null) {
					return rec;
				} else if (!this.known.containsKey(id)) {
					return null;
				} else {
					long clock = System.currentTimeMillis();
					Long failed = this.unreadable.get(id);
					if (failed != null && clock >= failed && clock - failed < 10000L) {
						return null;
					} else {
						try {
							rec = this.read(id);
						} catch (IOException var7) {
							if (this.unreadable.put(id, clock) == null) {
								CFHGuiServer.LOGGER
									.error(
										"[CFHGui Server] {} could not be read ({}). It is left as it is and tried again later; until then the player counts as unknown",
										this.file(id),
										var7.toString()
									);
							}

							return null;
						}

						this.unreadable.remove(id);
						if (rec == null) {
							rec = fresh(id, this.known.get(id));
						}

						this.remember(rec);
						return rec;
					}
				}
			}
		}
	}

	public PlayerRecord open(UUID id, String name, long now) {
		PlayerRecord rec = this.held.get(id);
		if (rec != null) {
			return rec;
		} else {
			rec = this.cache.remove(id);
			if (rec == null) {
				try {
					rec = this.read(id);
					this.unreadable.remove(id);
				} catch (IOException var7) {
					this.unsaved.add(id);
					CFHGuiServer.LOGGER
						.error(
							"[CFHGui Server] {} could not be read ({}). It is left as it is: {} plays this visit on an empty record that is not saved, and the file is read again when they next come in",
							new Object[]{this.file(id), var7.toString(), name}
						);
				}
			}

			if (rec == null) {
				rec = fresh(id, this.known.get(id));
				if (rec.firstSeen == 0L) {
					rec.firstSeen = now;
				}
			}

			this.held.put(id, rec);
			if (!name.isEmpty()) {
				rec.name = name;
			}

			this.touch(rec, now);
			return rec;
		}
	}

	public void release(UUID id) {
		PlayerRecord rec = this.held.remove(id);
		if (rec != null && !this.unsaved.remove(id)) {
			this.remember(rec);
		}
	}

	public boolean holds(UUID id) {
		return this.held.containsKey(id);
	}

	private void remember(PlayerRecord rec) {
		this.cache.put(rec.id, rec);
		if (this.cache.size() > 128) {
			Iterator<Entry<UUID, PlayerRecord>> it = this.cache.entrySet().iterator();

			while (it.hasNext() && this.cache.size() > 128) {
				if (!this.dirty.contains(it.next().getKey())) {
					it.remove();
				}
			}
		}
	}

	private static PlayerRecord fresh(UUID id, @Nullable WorldStore.Known k) {
		PlayerRecord rec = new PlayerRecord();
		rec.id = id;
		if (k != null) {
			rec.name = k.name;
			rec.nick = k.nick;
			rec.firstSeen = k.firstSeen;
			rec.lastSeen = k.lastSeen;
			rec.sessions = k.sessions;
			rec.afkTicks = k.afkTicks;
			rec.statsHidden = k.statsHidden;
		}

		return rec;
	}

	public void touch(PlayerRecord rec, long now) {
		if (!this.unsaved.contains(rec.id)) {
			if (this.dirty.isEmpty() && !this.namesDirty && !this.warpsDirty) {
				this.dirtySince = System.currentTimeMillis();
			}

			this.dirty.add(rec.id);
			this.index(rec, now);
		}
	}

	private void index(PlayerRecord rec, long now) {
		WorldStore.Known k = new WorldStore.Known();
		k.id = rec.id;
		k.name = rec.name;
		k.nick = rec.nick;
		k.firstSeen = rec.firstSeen;
		k.lastSeen = rec.lastSeen;
		k.sessions = rec.sessions;
		k.afkTicks = rec.afkTicks;
		k.statsHidden = rec.statsHidden;
		k.vanished = rec.vanished;
		k.mutedUntil = until(rec.active(Punishment.Kind.MUTE, now));
		k.bannedUntil = until(rec.active(Punishment.Kind.BAN, now));
		k.frozenUntil = until(rec.active(Punishment.Kind.FREEZE, now));
		k.punishments = rec.punishments.size();
		k.punishedAt = rec.punishments.isEmpty() ? 0L : rec.punishments.get(rec.punishments.size() - 1).at;

		for (Role.Grant g : rec.roles) {
			if (!g.expired(now)) {
				if (!k.roles.contains(g.role)) {
					k.roles.add(g.role);
				}

				if (g.expiresAt > 0L) {
					k.rolesUntil = k.rolesUntil == 0L ? g.expiresAt : Math.min(k.rolesUntil, g.expiresAt);
				}
			}
		}

		WorldStore.Known old = this.known.get(rec.id);
		if (old == null || !old.same(k)) {
			if (old != null) {
				this.byName.remove(old.name.toLowerCase(Locale.ROOT), rec.id);
				if (!old.nick.isEmpty()) {
					this.byNick.remove(Text.plainNick(old.nick).toLowerCase(Locale.ROOT), rec.id);
				}
			}

			this.known.put(rec.id, k);
			this.link(k);
			if (this.dirty.isEmpty() && !this.namesDirty && !this.warpsDirty) {
				this.dirtySince = System.currentTimeMillis();
			}

			this.namesDirty = true;
		}
	}

	private void link(WorldStore.Known k) {
		if (!k.name.isEmpty()) {
			this.byName.put(k.name.toLowerCase(Locale.ROOT), k.id);
		}

		if (!k.nick.isEmpty()) {
			this.byNick.put(Text.plainNick(k.nick).toLowerCase(Locale.ROOT), k.id);
		}
	}

	private static long until(@Nullable Punishment p) {
		return p == null ? 0L : (p.expiresAt == 0L ? -1L : p.expiresAt);
	}

	public List<Warp> warps() {
		return this.warps.warps;
	}

	@Nullable
	public Warp warp(@Nullable String name) {
		if (name == null) {
			return null;
		} else {
			for (Warp w : this.warps.warps) {
				if (w.name.equalsIgnoreCase(name)) {
					return w;
				}
			}

			return null;
		}
	}

	public void addWarp(Warp warp) {
		this.warps.warps.add(warp);
		this.touchWarps();
	}

	public void removeWarp(Warp warp) {
		this.warps.warps.remove(warp);
		this.touchWarps();
	}

	@Nullable
	public Place spawn() {
		return this.warps.spawn;
	}

	public void spawn(@Nullable Place place) {
		this.warps.spawn = place;
		this.touchWarps();
	}

	public void touchWarps() {
		if (this.dirty.isEmpty() && !this.namesDirty && !this.warpsDirty) {
			this.dirtySince = System.currentTimeMillis();
		}

		this.warpsDirty = true;
	}

	private Path file(UUID id) {
		return this.dir.resolve("players").resolve(id + ".json");
	}

	@Nullable
	private PlayerRecord read(UUID id) throws IOException {
		if (this.dir == null) {
			return null;
		}

		Path file = this.file(id);
		String json = this.writing.get(id);

		while (json == null) {
			if (!Files.isRegularFile(file)) {
				return null;
			}

			try {
				json = text(file);
			} catch (NoSuchFileException e) {
				return null;
			}
		}

		try {
			PlayerRecord rec = (PlayerRecord)JsonConfig.GSON.fromJson(json, PlayerRecord.class);
			if (rec == null) {
				throw new JsonParseException("the file is empty");
			}

			rec.sanitize();
			rec.id = id;
			return rec;
		} catch (RuntimeException e) {
			Path kept = keepAside(file);
			if (kept == null) {
				throw new IOException("it is no record, and could not be copied aside", e);
			}

			CFHGuiServer.LOGGER.error("[CFHGui Server] {} is no record; it was kept as {} and the player starts with a new record", file, kept.getFileName(), e);
			return null;
		}
	}

	private static String text(Path file) throws IOException {
		byte[] bytes;
		try {
			bytes = Files.readAllBytes(file);
		} catch (NoSuchFileException var3) {
			throw var3;
		} catch (IOException var4) {
			bytes = Files.readAllBytes(file);
		}

		return new String(bytes, StandardCharsets.UTF_8);
	}

	@Nullable
	private static Path keepAside(Path file) {
		try {
			if (!Files.isRegularFile(file)) {
				return null;
			}

			for (int n = 1; n < 100; n++) {
				Path copy = file.resolveSibling(file.getFileName() + (n == 1 ? ".broken" : ".broken." + n));
				if (!Files.exists(copy, LinkOption.NOFOLLOW_LINKS)) {
					Files.copy(file, copy);
					return copy;
				}
			}
		} catch (RuntimeException | IOException var3) {
			CFHGuiServer.LOGGER.warn("[CFHGui Server] {} could not be copied aside: {}", file, var3.toString());
		}

		return null;
	}

	private static String keptAs(Path file) {
		Path kept = keepAside(file);
		return kept == null ? "it could not be copied aside" : "it was kept as " + kept.getFileName();
	}

	public void load(long now) {
		if (this.dir != null) {
			Path names = this.dir.resolve("names.json");
			boolean rebuilt = false;
			if (Files.isRegularFile(names)) {
				try {
					WorldStore.NamesFile f = (WorldStore.NamesFile)JsonConfig.GSON.fromJson(Files.readString(names, StandardCharsets.UTF_8), WorldStore.NamesFile.class);
					if (f == null || f.players == null) {
						throw new IOException("the file is empty");
					}

					for (WorldStore.Known k : f.players) {
						this.adopt(k);
					}
				} catch (Exception var9) {
					CFHGuiServer.LOGGER.error("[CFHGui Server] {} could not be read; {} and it is made again from the players' files", new Object[]{names, keptAs(names), var9});
					this.known.clear();
					this.byName.clear();
					this.byNick.clear();
					rebuilt = this.rebuild(now);
				}
			} else {
				rebuilt = this.rebuild(now);
			}

			Path warpsFile = this.dir.resolve("warps.json");
			if (Files.isRegularFile(warpsFile)) {
				try {
					WarpList w = (WarpList)JsonConfig.GSON.fromJson(Files.readString(warpsFile, StandardCharsets.UTF_8), WarpList.class);
					if (w == null) {
						throw new IOException("the file is empty");
					}

					w.sanitize();
					Set<String> seen = new LinkedHashSet<>();
					w.warps.removeIf(x -> Text.name(x.name) == null || !seen.add(x.name.toLowerCase(Locale.ROOT)));
					this.warps = w;
				} catch (Exception var8) {
					CFHGuiServer.LOGGER.error("[CFHGui Server] {} could not be read; {} and the server starts without warps", new Object[]{warpsFile, keptAs(warpsFile), var8});
					this.warps = new WarpList();
				}
			}

			this.dirty.clear();
			this.warpsDirty = false;
			this.namesDirty = rebuilt;
			if (rebuilt) {
				this.dirtySince = System.currentTimeMillis();
			}
		}
	}

	private void adopt(@Nullable WorldStore.Known k) {
		if (k != null && k.id != null && !this.known.containsKey(k.id)) {
			if (k.name == null) {
				k.name = "";
			}

			if (k.nick == null) {
				k.nick = "";
			}

			if (k.roles == null) {
				k.roles = new ArrayList<>();
			}

			this.known.put(k.id, k);
			this.link(k);
		}
	}

	private boolean rebuild(long now) {
		Path players = this.dir.resolve("players");
		if (!Files.isDirectory(players)) {
			return false;
		} else {
			int n = 0;

			try (DirectoryStream<Path> files = Files.newDirectoryStream(players, "*.json")) {
				for (Path file : files) {
					String name = file.getFileName().toString();

					UUID id;
					try {
						id = UUID.fromString(name.substring(0, name.length() - 5));
					} catch (IllegalArgumentException var14) {
						continue;
					}

					PlayerRecord rec;
					try {
						rec = this.read(id);
					} catch (IOException var13) {
						CFHGuiServer.LOGGER.error("[CFHGui Server] {} could not be read ({}) and is not in the index that was made", file, var13.toString());
						continue;
					}

					if (rec != null) {
						this.index(rec, now);
						n++;
					}
				}
			} catch (IOException var16) {
				CFHGuiServer.LOGGER.warn("[CFHGui Server] the players' files could not be listed: {}", var16.toString());
			}

			if (n > 0) {
				CFHGuiServer.LOGGER.info("[CFHGui Server] names.json was made again from {} players' files", n);
			}

			return n > 0;
		}
	}

	public void tick(Saver saver) {
		if ((this.namesDirty || this.warpsDirty || !this.dirty.isEmpty()) && System.currentTimeMillis() - this.dirtySince >= 4000L) {
			this.save(saver);
		}
	}

	public void save(Saver saver) {
		if (this.dir == null) {
			this.dirty.clear();
			this.namesDirty = false;
			this.warpsDirty = false;
		} else {
			if (!this.dirty.isEmpty()) {
				for (UUID id : this.dirty) {
					PlayerRecord rec = this.held.get(id);
					if (rec == null) {
						rec = this.cache.get(id);
					}

					if (rec != null) {
						String json = JsonConfig.GSON.toJson(rec);
						this.writing.put(id, json);
						saver.text(this.file(id), json);
						saver.run(() -> this.writing.remove(id, json));
					}
				}

				this.dirty.clear();
			}

			if (this.namesDirty) {
				this.namesDirty = false;
				WorldStore.NamesFile f = new WorldStore.NamesFile();
				f.players.addAll(this.known.values());
				saver.write(this.dir.resolve("names.json"), tmp -> Files.writeString(tmp, JsonConfig.GSON.toJson(f), StandardCharsets.UTF_8));
			}

			if (this.warpsDirty) {
				this.warpsDirty = false;
				saver.text(this.dir.resolve("warps.json"), JsonConfig.GSON.toJson(this.warps));
			}
		}
	}

	public boolean pending() {
		return this.namesDirty || this.warpsDirty || !this.dirty.isEmpty();
	}

	public static final class Known {
		public UUID id = new UUID(0L, 0L);
		public String name = "";
		public String nick = "";
		public long firstSeen;
		public long lastSeen;
		public long sessions;
		public long afkTicks;
		public boolean statsHidden;
		public boolean vanished;
		public long mutedUntil;
		public long bannedUntil;
		public long frozenUntil;
		public int punishments;
		public long punishedAt;
		public List<String> roles = new ArrayList<>();
		public long rolesUntil;

		public boolean muted(long now) {
			return holds(this.mutedUntil, now);
		}

		public boolean banned(long now) {
			return holds(this.bannedUntil, now);
		}

		public boolean frozen(long now) {
			return holds(this.frozenUntil, now);
		}

		private static boolean holds(long until, long now) {
			return until == -1L || until > now;
		}

		public String shownName() {
			return this.nick != null && !this.nick.isEmpty() ? this.nick : this.name;
		}

		boolean same(WorldStore.Known o) {
			return this.name.equals(o.name)
				&& this.nick.equals(o.nick)
				&& this.firstSeen == o.firstSeen
				&& this.lastSeen == o.lastSeen
				&& this.sessions == o.sessions
				&& this.afkTicks == o.afkTicks
				&& this.statsHidden == o.statsHidden
				&& this.vanished == o.vanished
				&& this.rolesUntil == o.rolesUntil
				&& this.mutedUntil == o.mutedUntil
				&& this.bannedUntil == o.bannedUntil
				&& this.frozenUntil == o.frozenUntil
				&& this.punishments == o.punishments
				&& this.punishedAt == o.punishedAt
				&& this.roles.equals(o.roles);
		}
	}

	private static final class NamesFile {
		int version = 1;
		List<WorldStore.Known> players = new ArrayList<>();
	}
}
