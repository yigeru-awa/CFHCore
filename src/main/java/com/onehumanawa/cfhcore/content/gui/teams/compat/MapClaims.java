package com.onehumanawa.cfhcore.content.gui.teams.compat;

import com.onehumanawa.cfhcore.content.gui.core.map.MapAreas.Area;
import com.onehumanawa.cfhcore.content.gui.teams.TeamsConfig;
import com.onehumanawa.cfhcore.content.gui.teams.client.LandNames;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsState;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamSummary;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class MapClaims {
	private static final int UNKNOWN_RGB = 10134964;
	private static volatile Map<String, MapClaims.Dim> dims = Map.of();
	private static final Map<ResourceKey<Level>, String> NAMES = new ConcurrentHashMap<>();
	private static final Map<String, Long> REVISIONS = new ConcurrentHashMap<>();
	private static int looksStamp;

	public static long regionKey(int regionX, int regionZ) {
		return ChunkPos.asLong(regionX, regionZ);
	}

	public static int regionX(long key) {
		return ChunkPos.getX(key);
	}

	public static int regionZ(long key) {
		return ChunkPos.getZ(key);
	}

	public static MapClaims.Dim of(String dimension) {
		MapClaims.Dim d = dims.get(dimension);
		return d == null ? MapClaims.Dim.EMPTY : d;
	}

	public static MapClaims.Dim of(@Nullable ResourceKey<Level> dimension) {
		if (dimension == null) {
			return MapClaims.Dim.EMPTY;
		} else {
			String name = NAMES.get(dimension);
			if (name == null) {
				name = dimension.location().toString();
				NAMES.put(dimension, name);
			}

			return of(name);
		}
	}

	static boolean looksChanged() {
		return looksStamp() != looksStamp;
	}

	private static int looksStamp() {
		TeamsConfig cfg = TeamsConfig.get();
		int h = cfg.mapClaims ? 1 : 0;
		h = h * 31 + Double.hashCode(opacity(cfg));
		TeamData own = TeamsState.land();
		h = h * 31 + (own == null ? 0 : (own.id.hashCode() * 31 + own.color) * 31 + own.name.hashCode() + (own.solo ? 1 : 0));

		for (TeamSummary s : TeamsState.holders()) {
			h = h * 31 + s.id.hashCode();
			h = h * 31 + s.color;
			h = h * 31 + s.name.hashCode();
			h = h * 31 + s.tag.hashCode();
			h = h * 31 + (s.solo ? 2 : 0) + (TeamsState.isAlly(s.id) ? 1 : 0);
		}

		return h;
	}

	private static double opacity(TeamsConfig cfg) {
		double o = cfg.mapClaimOpacity;
		return Double.isNaN(o) ? 0.35 : Math.max(0.0, Math.min(1.0, o));
	}

	static Map<String, LongSet> rebuild(Collection<String> dimensions) {
		TeamsConfig cfg = TeamsConfig.get();
		boolean shown = cfg.mapClaims;
		double opacity = opacity(cfg);
		looksStamp = looksStamp();
		Map<UUID, MapClaims.Team> teams = new HashMap<>();
		Map<String, MapClaims.Dim> next = new HashMap<>(dims);
		Map<String, LongSet> changed = new HashMap<>();

		for (String dimension : dimensions) {
			MapClaims.Dim before = of(dimension);
			MapClaims.Dim after = shown ? build(TeamsState.claims(dimension), teams, opacity) : MapClaims.Dim.EMPTY;
			if (after.isEmpty()) {
				next.remove(dimension);
			} else {
				next.put(dimension, after);
			}

			LongSet diff = differing(before, after);
			if (!diff.isEmpty()) {
				changed.put(dimension, diff);
			}
		}

		dims = Map.copyOf(next);

		for (String dimension : changed.keySet()) {
			REVISIONS.merge(dimension, 1L, Long::sum);
		}

		return changed;
	}

	public static long revision(String dimension) {
		Long r = REVISIONS.get(dimension);
		return r == null ? 0L : r;
	}

	public static Collection<String> dimensions() {
		return dims.keySet();
	}

	private static MapClaims.Dim build(Long2ObjectMap<TeamsState.Claim> claims, Map<UUID, MapClaims.Team> teams, double opacity) {
		if (claims.isEmpty()) {
			return MapClaims.Dim.EMPTY;
		} else {
			Long2ObjectOpenHashMap<MapClaims.Team> owners = new Long2ObjectOpenHashMap(claims.size());
			LongOpenHashSet forced = new LongOpenHashSet();
			Long2LongOpenHashMap inside = new Long2LongOpenHashMap();
			Long2LongOpenHashMap outside = new Long2LongOpenHashMap();
			ObjectIterator regions = Long2ObjectMaps.fastIterable(claims).iterator();

			while (regions.hasNext()) {
				Entry<TeamsState.Claim> e = (Entry<TeamsState.Claim>)regions.next();
				long pos = e.getLongKey();
				TeamsState.Claim claim = (TeamsState.Claim)e.getValue();
				if (claim != null && claim.team() != null) {
					MapClaims.Team team = teams.computeIfAbsent(claim.team(), id -> team(id, opacity));
					owners.put(pos, team);
					boolean kept = claim.forced();
					if (kept) {
						forced.add(pos);
					}

					int cx = ChunkPos.getX(pos);
					int cz = ChunkPos.getZ(pos);
					int rx = cx >> 5;
					int rz = cz >> 5;
					long h = mix(pos) * 31L + (long)team.look + (kept ? 40503L : 0L);
					inside.addTo(regionKey(rx, rz), h);
					int lx = cx & 31;
					int lz = cz & 31;
					if (lx == 0) {
						outside.addTo(regionKey(rx - 1, rz), h);
					}

					if (lx == 31) {
						outside.addTo(regionKey(rx + 1, rz), h);
					}

					if (lz == 0) {
						outside.addTo(regionKey(rx, rz - 1), h);
					}

					if (lz == 31) {
						outside.addTo(regionKey(rx, rz + 1), h);
					}
				}
			}

			Long2IntOpenHashMap regionsx = new Long2IntOpenHashMap(inside.size());
			ObjectIterator var24 = inside.long2LongEntrySet().iterator();

			while (var24.hasNext()) {
				it.unimi.dsi.fastutil.longs.Long2LongMap.Entry e = (it.unimi.dsi.fastutil.longs.Long2LongMap.Entry)var24.next();
				long v = e.getLongValue() + -7046029254386353131L * outside.get(e.getLongKey());
				int look = (int)(v ^ v >>> 32);
				regionsx.put(e.getLongKey(), look == 0 ? 1 : look);
			}

			return new MapClaims.Dim(owners, forced, regionsx);
		}
	}

	private static MapClaims.Team team(UUID id, double opacity) {
		TeamData own = TeamsState.land();
		if (own != null && own.id.equals(id)) {
			return new MapClaims.Team(id, LandNames.of(own).getString(), own.tag, own.color, MapClaims.Relation.OWN, opacity, own.solo);
		} else {
			TeamSummary s = TeamsState.summary(id);
			return s == null
				? new MapClaims.Team(id, "", "", 10134964, MapClaims.Relation.OTHER, opacity)
				: new MapClaims.Team(
					id, LandNames.of(s).getString(), s.tag, s.color, TeamsState.isAlly(id) ? MapClaims.Relation.ALLY : MapClaims.Relation.OTHER, opacity, s.solo
				);
		}
	}

	private static LongSet differing(MapClaims.Dim before, MapClaims.Dim after) {
		LongSet out = new LongOpenHashSet();
		if (before == after) {
			return out;
		} else {
			ObjectIterator var3 = after.regions.long2IntEntrySet().iterator();

			while (var3.hasNext()) {
				it.unimi.dsi.fastutil.longs.Long2IntMap.Entry e = (it.unimi.dsi.fastutil.longs.Long2IntMap.Entry)var3.next();
				if (before.regions.get(e.getLongKey()) != e.getIntValue()) {
					out.add(e.getLongKey());
				}
			}

			LongIterator var6 = before.regions.keySet().iterator();

			while (var6.hasNext()) {
				long key = (Long)var6.next();
				if (!after.regions.containsKey(key)) {
					out.add(key);
				}
			}

			return out;
		}
	}

	private static long mix(long x) {
		x = (x ^ x >>> 30) * -4658895280553007687L;
		x = (x ^ x >>> 27) * -7723592293110705685L;
		return x ^ x >>> 31;
	}

	private MapClaims() {
	}

	public static final class Dim {
		public static final MapClaims.Dim EMPTY = new MapClaims.Dim(new Long2ObjectOpenHashMap(0), new LongOpenHashSet(0), new Long2IntOpenHashMap(0));
		private final Long2ObjectOpenHashMap<MapClaims.Team> owners;
		private final LongOpenHashSet forced;
		private final Long2IntOpenHashMap regions;

		private Dim(Long2ObjectOpenHashMap<MapClaims.Team> owners, LongOpenHashSet forced, Long2IntOpenHashMap regions) {
			this.owners = owners;
			this.forced = forced;
			this.regions = regions;
		}

		public boolean isEmpty() {
			return this.owners.isEmpty();
		}

		@Nullable
		public MapClaims.Team at(int chunkX, int chunkZ) {
			return (MapClaims.Team)this.owners.get(ChunkPos.asLong(chunkX, chunkZ));
		}

		public boolean forced(int chunkX, int chunkZ) {
			return !this.forced.isEmpty() && this.forced.contains(ChunkPos.asLong(chunkX, chunkZ));
		}

		public int regionLook(int regionX, int regionZ) {
			return this.regions.get(MapClaims.regionKey(regionX, regionZ));
		}

		public LongSet regionKeys() {
			return this.regions.keySet();
		}

		public Long2ObjectMap<MapClaims.Team> chunks() {
			return this.owners;
		}

		public LongSet forcedChunks() {
			return this.forced;
		}
	}

	public static enum Relation {
		OWN,
		ALLY,
		OTHER;
	}

	public static final class Team {
		public final UUID id;
		public final String name;
		public final String tag;
		public final int rgb;
		public final MapClaims.Relation relation;
		public final boolean server;
		public final boolean personal;
		public final int fillAlpha;
		public final int borderAlpha;
		public final int look;
		public final Area area;

		Team(UUID id, String name, String tag, int rgb, MapClaims.Relation relation, double opacity) {
			this(id, name, tag, rgb, relation, opacity, false);
		}

		Team(UUID id, String name, String tag, int rgb, MapClaims.Relation relation, double opacity, boolean personal) {
			this.id = id;
			this.name = name;
			this.tag = tag;
			this.rgb = rgb & 16777215;
			this.relation = relation;
			this.server = TeamSummary.SERVER_LAND.equals(id);
			this.personal = personal;
			double fill = relation == MapClaims.Relation.OWN ? Math.min(1.0, opacity * 1.3 + 0.05) : opacity;
			double border = Math.min(1.0, fill * 1.6 + 0.25);
			this.fillAlpha = (int)Math.round(fill * 255.0);
			this.borderAlpha = (int)Math.round(border * 255.0);
			this.look = ((((id.hashCode() * 31 + this.rgb) * 31 + this.fillAlpha) * 31 + this.borderAlpha) * 31 + relation.ordinal()) * 31 + name.hashCode();
			this.area = new Area(
				id.toString(), name.isEmpty() ? Component.translatable("cfhgui_teams.map.claimed") : Component.literal(name), this.rgb, this.fillAlpha, this.borderAlpha
			);
		}
	}
}
