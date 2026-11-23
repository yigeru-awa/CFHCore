package com.onehumanawa.cfhcore.content.gui.teams.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import com.onehumanawa.cfhcore.content.gui.teams.CFHGuiTeams;
import com.onehumanawa.cfhcore.content.gui.teams.model.ClaimsUpdate;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class ClaimMap {
	private static final Gson COMPACT = new GsonBuilder().disableHtmlEscaping().create();
	private static final int UPDATE_ENTRIES = 30000;
	@Nullable
	private final Path file;
	private final Map<String, ClaimMap.Dim> dims = new LinkedHashMap<>();
	private final Reference2ObjectOpenHashMap<ResourceKey<Level>, ClaimMap.Dim> byKey = new Reference2ObjectOpenHashMap();
	private final Object2IntOpenHashMap<UUID> counts = new Object2IntOpenHashMap();
	private final Object2IntOpenHashMap<UUID> forcedCounts = new Object2IntOpenHashMap();
	private final Map<UUID, List<ClaimMap.Entry>> forcedByTeam = new HashMap<>();
	private int total;
	private boolean dirty;
	private long dirtySince;

	ClaimMap(@Nullable Path file) {
		this.file = file;
	}

	public boolean isEmpty() {
		return this.total == 0;
	}

	@Nullable
	public ClaimMap.Dim dim(ResourceKey<Level> key) {
		return (ClaimMap.Dim)this.byKey.get(key);
	}

	@Nullable
	ClaimMap.Dim dim(String id) {
		return this.dims.get(id);
	}

	Collection<ClaimMap.Dim> dims() {
		return this.dims.values();
	}

	@Nullable
	public ClaimMap.Claim at(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
		ClaimMap.Dim d = (ClaimMap.Dim)this.byKey.get(dimension);
		return d == null ? null : (ClaimMap.Claim)d.chunks.get(ChunkPos.asLong(chunkX, chunkZ));
	}

	@Nullable
	ClaimMap.Claim at(String dimension, long chunk) {
		ClaimMap.Dim d = this.dims.get(dimension);
		return d == null ? null : (ClaimMap.Claim)d.chunks.get(chunk);
	}

	int count(UUID team) {
		return this.counts.getInt(team);
	}

	int forced(UUID team) {
		return this.forcedCounts.getInt(team);
	}

	int total() {
		return this.total;
	}

	boolean touches(String dimension, long chunk, TeamData team) {
		ClaimMap.Dim d = this.dims.get(dimension);
		if (d == null) {
			return false;
		} else {
			int x = ChunkPos.getX(chunk);
			int z = ChunkPos.getZ(chunk);
			return owns(d, x + 1, z, team) || owns(d, x - 1, z, team) || owns(d, x, z + 1, team) || owns(d, x, z - 1, team);
		}
	}

	private static boolean owns(ClaimMap.Dim d, int x, int z, TeamData team) {
		ClaimMap.Claim c = (ClaimMap.Claim)d.chunks.get(ChunkPos.asLong(x, z));
		return c != null && c.team == team;
	}

	void each(TeamData team, ClaimMap.Visitor visitor) {
		for (ClaimMap.Dim d : this.dims.values()) {
			ObjectIterator var5 = d.chunks.long2ObjectEntrySet().iterator();

			while (var5.hasNext()) {
				it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<ClaimMap.Claim> e = (it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<ClaimMap.Claim>)var5.next();
				if (((ClaimMap.Claim)e.getValue()).team == team) {
					visitor.claim(d, e.getLongKey(), (ClaimMap.Claim)e.getValue());
				}
			}
		}
	}

	@Nullable
	ClaimMap.Claim put(String dimension, long chunk, TeamData team) {
		ClaimMap.Dim d = this.dimFor(dimension);
		if (d != null && !d.chunks.containsKey(chunk)) {
			ClaimMap.Claim c = new ClaimMap.Claim(team);
			d.chunks.put(chunk, c);
			d.pending.add(chunk);
			this.counts.addTo(team.id, 1);
			this.total++;
			this.markDirty();
			return c;
		} else {
			return null;
		}
	}

	@Nullable
	ClaimMap.Claim remove(String dimension, long chunk) {
		ClaimMap.Dim d = this.dims.get(dimension);
		if (d == null) {
			return null;
		} else {
			ClaimMap.Claim c = (ClaimMap.Claim)d.chunks.remove(chunk);
			if (c == null) {
				return null;
			} else {
				d.pending.add(chunk);
				this.counts.addTo(c.team.id, -1);
				if (c.forced) {
					this.forcedCounts.addTo(c.team.id, -1);
					this.unlist(c);
				}

				this.total--;
				this.markDirty();
				return c;
			}
		}
	}

	void setForced(ClaimMap.Dim d, long chunk, ClaimMap.Claim claim, boolean forced) {
		if (claim.forced != forced) {
			claim.forced = forced;
			this.forcedCounts.addTo(claim.team.id, forced ? 1 : -1);
			if (forced) {
				this.forcedByTeam.computeIfAbsent(claim.team.id, k -> new ArrayList<>(4)).add(new ClaimMap.Entry(d, chunk, claim));
			} else {
				this.unlist(claim);
			}

			d.pending.add(chunk);
			this.markDirty();
		}
	}

	private void unlist(ClaimMap.Claim claim) {
		List<ClaimMap.Entry> list = this.forcedByTeam.get(claim.team.id);
		if (list != null) {
			list.removeIf(e -> e.claim() == claim);
			if (list.isEmpty()) {
				this.forcedByTeam.remove(claim.team.id);
			}
		}
	}

	List<ClaimMap.Entry> forcedOf(UUID team) {
		List<ClaimMap.Entry> list = this.forcedByTeam.get(team);
		return list == null ? List.of() : list;
	}

	Collection<List<ClaimMap.Entry>> allForced() {
		return this.forcedByTeam.values();
	}

	void heldChanged() {
		this.markDirty();
	}

	List<ClaimMap.Entry> of(TeamData team) {
		List<ClaimMap.Entry> out = new ArrayList<>();
		this.each(team, (d, chunk, claim) -> out.add(new ClaimMap.Entry(d, chunk, claim)));
		return out;
	}

	@Nullable
	private ClaimMap.Dim dimFor(String id) {
		ClaimMap.Dim d = this.dims.get(id);
		if (d != null) {
			return d;
		} else {
			ResourceLocation location = ResourceLocation.tryParse(id);
			if (location == null) {
				return null;
			} else {
				ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, location);
				d = new ClaimMap.Dim(id, key);
				this.dims.put(id, d);
				this.byKey.put(key, d);
				return d;
			}
		}
	}

	private void markDirty() {
		if (!this.dirty) {
			this.dirtySince = System.currentTimeMillis();
		}

		this.dirty = true;
	}

	List<ClaimsUpdate> whole() {
		List<ClaimsUpdate> out = new ArrayList<>();

		for (ClaimMap.Dim d : this.dims.values()) {
			LongArrayList chunks = new LongArrayList(d.chunks.size());
			chunks.addAll(d.chunks.keySet());
			boolean first = true;

			for (int from = 0; from < chunks.size() || first; from += 30000) {
				out.add(update(d, first, chunks.subList(from, Math.min(chunks.size(), from + 30000))));
				first = false;
			}
		}

		return out;
	}

	List<ClaimsUpdate> drain() {
		List<ClaimsUpdate> out = List.of();

		for (ClaimMap.Dim d : this.dims.values()) {
			if (!d.pending.isEmpty()) {
				if (out.isEmpty()) {
					out = new ArrayList<>();
				}

				LongArrayList all = new LongArrayList(d.pending);
				d.pending.clear();

				for (int from = 0; from < all.size(); from += 30000) {
					out.add(update(d, false, all.subList(from, Math.min(all.size(), from + 30000))));
				}
			}
		}

		return out;
	}

	private static ClaimsUpdate update(ClaimMap.Dim d, boolean reset, List<Long> chunks) {
		List<UUID> palette = new ArrayList<>();
		Object2IntOpenHashMap<UUID> index = new Object2IntOpenHashMap();
		index.defaultReturnValue(-1);
		LongArrayList set = new LongArrayList();
		LongArrayList removed = new LongArrayList();
		IntArrayList owners = new IntArrayList();
		IntArrayList flags = new IntArrayList();

		for (long chunk : chunks) {
			ClaimMap.Claim c = (ClaimMap.Claim)d.chunks.get(chunk);
			if (c == null) {
				removed.add(chunk);
			} else {
				int i = index.getInt(c.team.id);
				if (i < 0) {
					i = palette.size();
					palette.add(c.team.id);
					index.put(c.team.id, i);
				}

				set.add(chunk);
				owners.add(i);
				flags.add(c.forced ? 1 : 0);
			}
		}

		byte[] f = new byte[flags.size()];

		for (int i = 0; i < f.length; i++) {
			f[i] = (byte)flags.getInt(i);
		}

		return new ClaimsUpdate(d.id, reset, palette, set.toLongArray(), owners.toIntArray(), f, removed.toLongArray());
	}

	void load(Function<UUID, TeamData> holders) {
		if (this.file != null && Files.isRegularFile(this.file)) {
			try {
				try (Reader r = Files.newBufferedReader(this.file, StandardCharsets.UTF_8)) {
					ClaimMap.ClaimsFile f = (ClaimMap.ClaimsFile)JsonConfig.GSON.fromJson(r, ClaimMap.ClaimsFile.class);
					if (f != null && f.dimensions != null) {
						int orphans = 0;

						for (Map.Entry<String, Map<String, ClaimMap.TeamChunks>> dim : f.dimensions.entrySet()) {
							if (dim.getValue() != null) {
								for (Map.Entry<String, ClaimMap.TeamChunks> e : dim.getValue().entrySet()) {
									ClaimMap.TeamChunks h = e.getValue();
									if (h != null && h.chunks != null) {
										TeamData team = null;

										try {
											team = holders.apply(UUID.fromString(e.getKey()));
										} catch (IllegalArgumentException var17) {
										}

										if (team == null) {
											orphans += h.chunks.length / 2;
										} else {
											for (int i = 0; i + 1 < h.chunks.length; i += 2) {
												this.put(dim.getKey(), ChunkPos.asLong(h.chunks[i], h.chunks[i + 1]), team);
											}

											ClaimMap.Dim d = this.dims.get(dim.getKey());
											if (d != null) {
												if (h.forced != null) {
													for (int i = 0; i + 1 < h.forced.length; i += 2) {
														long chunk = ChunkPos.asLong(h.forced[i], h.forced[i + 1]);
														ClaimMap.Claim c = (ClaimMap.Claim)d.chunks.get(chunk);
														if (c != null && c.team == team) {
															this.setForced(d, chunk, c, true);
														}
													}
												}

												if (h.held != null) {
													for (int ix = 0; ix + 1 < h.held.length; ix += 2) {
														ClaimMap.Claim c = (ClaimMap.Claim)d.chunks.get(ChunkPos.asLong(h.held[ix], h.held[ix + 1]));
														if (c != null && c.team == team) {
															c.held = true;
														}
													}
												}
											}
										}
									}
								}
							}
						}

						if (orphans > 0) {
							CFHGuiTeams.LOGGER.warn("[CFHGui Teams] {} claimed chunk(s) belonged to teams that are gone and were freed", orphans);
						}

						for (ClaimMap.Dim d : this.dims.values()) {
							d.pending.clear();
						}

						this.dirty = orphans > 0;
						this.dirtySince = System.currentTimeMillis();
						return;
					}
				}
			} catch (Exception var19) {
				CFHGuiTeams.LOGGER.error("[CFHGui Teams] {} could not be read; the server starts without claims", this.file, var19);
			}
		}
	}

	void tick(Saver saver) {
		if (this.dirty && System.currentTimeMillis() - this.dirtySince >= 5000L) {
			this.save(saver);
		}
	}

	void save(Saver saver) {
		if (this.dirty) {
			this.dirty = false;
			if (this.file != null) {
				ClaimMap.ClaimsFile f = new ClaimMap.ClaimsFile();

				for (ClaimMap.Dim d : this.dims.values()) {
					if (!d.chunks.isEmpty()) {
						Map<UUID, IntArrayList[]> perTeam = new LinkedHashMap<>();
						ObjectIterator out = d.chunks.long2ObjectEntrySet().iterator();

						while (out.hasNext()) {
							it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<ClaimMap.Claim> e = (it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<ClaimMap.Claim>)out.next();
							ClaimMap.Claim c = (ClaimMap.Claim)e.getValue();
							IntArrayList[] lists = perTeam.computeIfAbsent(c.team.id, k -> new IntArrayList[]{new IntArrayList(), new IntArrayList(), new IntArrayList()});
							int x = ChunkPos.getX(e.getLongKey());
							int z = ChunkPos.getZ(e.getLongKey());
							lists[0].add(x);
							lists[0].add(z);
							if (c.forced) {
								lists[1].add(x);
								lists[1].add(z);
							}

							if (c.held) {
								lists[2].add(x);
								lists[2].add(z);
							}
						}

						Map<String, ClaimMap.TeamChunks> outx = new LinkedHashMap<>();

						for (Map.Entry<UUID, IntArrayList[]> ex : perTeam.entrySet()) {
							ClaimMap.TeamChunks h = new ClaimMap.TeamChunks();
							h.chunks = ex.getValue()[0].toIntArray();
							h.forced = ex.getValue()[1].toIntArray();
							h.held = ex.getValue()[2].toIntArray();
							outx.put(ex.getKey().toString(), h);
						}

						f.dimensions.put(d.id, outx);
					}
				}

				saver.text(this.file, COMPACT.toJson(f));
			}
		}
	}

	public static final class Claim {
		public final TeamData team;
		boolean forced;
		boolean held;

		Claim(TeamData team) {
			this.team = team;
		}
	}

	private static final class ClaimsFile {
		int version = 1;
		Map<String, Map<String, ClaimMap.TeamChunks>> dimensions = new LinkedHashMap<>();
	}

	public static final class Dim {
		public final String id;
		public final ResourceKey<Level> key;
		public final Long2ObjectOpenHashMap<ClaimMap.Claim> chunks = new Long2ObjectOpenHashMap();
		final LongOpenHashSet pending = new LongOpenHashSet();

		Dim(String id, ResourceKey<Level> key) {
			this.id = id;
			this.key = key;
		}
	}

	static record Entry(ClaimMap.Dim dim, long chunk, ClaimMap.Claim claim) {
	}

	private static final class TeamChunks {
		int[] chunks = new int[0];
		int[] forced = new int[0];
		int[] held = new int[0];
	}

	@FunctionalInterface
	interface Visitor {
		void claim(ClaimMap.Dim var1, long var2, ClaimMap.Claim var4);
	}
}
