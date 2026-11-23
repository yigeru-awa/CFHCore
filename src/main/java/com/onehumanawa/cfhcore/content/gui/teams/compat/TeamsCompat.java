package com.onehumanawa.cfhcore.content.gui.teams.compat;

import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.teams.CFHGuiTeams;
import com.onehumanawa.cfhcore.content.gui.teams.TeamsConfig;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsState;
import com.onehumanawa.cfhcore.content.gui.teams.compat.xaero.XaeroBridge;
import com.onehumanawa.cfhcore.content.gui.teams.compat.xaero.XaeroMinimapBridge;
import com.onehumanawa.cfhcore.content.gui.teams.compat.xaero.XaeroWorldMapBridge;
import com.onehumanawa.cfhcore.content.gui.teams.model.PingData;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class TeamsCompat {
	private static final List<MapBridge> WAITING = new CopyOnWriteArrayList<>();
	private static final List<MapBridge> ATTACHED = new CopyOnWriteArrayList<>();
	private static boolean listening;
	private static final Set<String> STALE = new HashSet<>();
	private static final Set<String> SEEN = new HashSet<>();
	private static boolean looksStale = true;
	private static boolean shownLand;
	private static double shownOpacity = Double.NaN;
	private static final List<PingData> PINGS = new ArrayList<>();
	@Nullable
	private static ClientPacketListener connection;

	public static void init() {
		CFHGuiPlatform platform = CFHGuiPlatform.get();
		if (platform.isModLoaded("xaeroworldmap")) {
			add("xaeroworldmap", TeamsCompat.Xaero::worldMap);
		}

		if (platform.isModLoaded("xaerominimap") || platform.isModLoaded("xaerobetterpvp")) {
			add("xaerominimap", TeamsCompat.Xaero::minimap);
		}

		TeamsMaps.init();
		listen();
	}

	public static void register(MapBridge bridge) {
		WAITING.add(bridge);
		listen();
	}

	private static synchronized void listen() {
		if (!listening) {
			listening = true;
			TeamsState.CLAIMS_CHANGED.register((Consumer<String>)dimension -> {
				STALE.add(dimension);
				SEEN.add(dimension);
			});
			TeamsState.DIRECTORY_CHANGED.register((Runnable)() -> looksStale = true);
			TeamsState.TEAM_CHANGED.register((Runnable)() -> looksStale = true);
			TeamsState.PING.register(PINGS::add);
			CFHGuiEvents.CLIENT_LEFT_SERVER.register((Runnable)() -> {
				MapMarks.clear();
				looksStale = true;
				STALE.addAll(dimensions());
			});
			CFHGuiEvents.CLIENT_TICK_END.register(TeamsCompat::tick);
			MapDev.init();
		}
	}

	private static void add(String id, Supplier<MapBridge> make) {
		try {
			WAITING.add(make.get());
		} catch (Throwable var3) {
			CFHGuiTeams.LOGGER.warn("[CFHGui Teams] {} is installed but not as this version knows it; teams are not shown on its map ({})", id, var3.toString());
		}
	}

	private static void tick() {
		List<MapBridge> fresh = null;

		for (MapBridge bridge : WAITING) {
			try {
				if (bridge.attach()) {
					WAITING.remove(bridge);
					ATTACHED.add(bridge);
					if (fresh == null) {
						fresh = new ArrayList<>();
					}

					fresh.add(bridge);
					CFHGuiTeams.LOGGER.info("[CFHGui Teams] showing teams on {}", bridge.id());
				}
			} catch (Throwable var8) {
				fail(bridge, var8);
			}
		}

		TeamsMaps.tick();
		ClientPacketListener now = Minecraft.getInstance().getConnection();
		if (now != connection) {
			connection = now;
			looksStale = true;
			STALE.addAll(dimensions());
		}

		if (WAITING.isEmpty() && ATTACHED.isEmpty()) {
			land();
			PINGS.clear();
		} else {
			boolean marks = MapMarks.update();
			Map<String, LongSet> land = land();

			for (Entry<String, LongSet> e : land.entrySet()) {
				each(b -> b.claimsChanged(e.getKey(), e.getValue()));
			}

			if (marks) {
				each(MapBridge::marksChanged);
			}

			if (fresh != null) {
				for (MapBridge bridge : fresh) {
					if (ATTACHED.contains(bridge)) {
						try {
							for (String dimension : MapClaims.dimensions()) {
								bridge.claimsChanged(dimension, null);
							}

							bridge.marksChanged();
						} catch (Throwable var9) {
							fail(bridge, var9);
						}
					}
				}
			}

			if (!PINGS.isEmpty()) {
				List<PingData> pings = List.copyOf(PINGS);
				PINGS.clear();

				for (PingData ping : pings) {
					each(b -> b.ping(ping));
				}
			}

			each(MapBridge::tick);
		}
	}

	private static Map<String, LongSet> land() {
		TeamsConfig cfg = TeamsConfig.get();
		if (cfg.mapClaims != shownLand || cfg.mapClaimOpacity != shownOpacity) {
			shownLand = cfg.mapClaims;
			shownOpacity = cfg.mapClaimOpacity;
			looksStale = true;
		}

		if (looksStale) {
			looksStale = false;
			if (MapClaims.looksChanged()) {
				STALE.addAll(dimensions());
			}
		}

		if (STALE.isEmpty()) {
			return Map.of();
		} else {
			Map<String, LongSet> changed = MapClaims.rebuild(List.copyOf(STALE));
			STALE.clear();
			return changed;
		}
	}

	private static Set<String> dimensions() {
		Set<String> all = new HashSet<>(SEEN);
		all.addAll(MapClaims.dimensions());
		all.add(TeamsState.currentDimension());
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		if (connection != null) {
			for (ResourceKey<Level> level : connection.levels()) {
				all.add(level.location().toString());
			}
		}

		return all;
	}

	private static void each(Consumer<MapBridge> call) {
		for (MapBridge bridge : ATTACHED) {
			try {
				call.accept(bridge);
			} catch (Throwable var4) {
				fail(bridge, var4);
			}
		}
	}

	private static void fail(MapBridge bridge, Throwable t) {
		WAITING.remove(bridge);
		ATTACHED.remove(bridge);
		CFHGuiTeams.LOGGER.warn("[CFHGui Teams] {} has changed since this version was made; teams are no longer shown on its map ({})", bridge.id(), t.toString());

		try {
			bridge.abandon();
		} catch (Throwable var3) {
		}
	}

	@Nullable
	static MapBridge bridge(String id) {
		for (MapBridge b : ATTACHED) {
			if (b.id().equals(id)) {
				return b;
			}
		}

		for (MapBridge bx : WAITING) {
			if (bx.id().equals(id)) {
				return bx;
			}
		}

		return null;
	}

	static String describe() {
		StringBuilder out = new StringBuilder();
		out.append(MapMarks.marks().size()).append(" marks, land in ").append(MapClaims.dimensions());

		for (MapBridge b : ATTACHED) {
			String holds = b.describe();
			out.append("; ").append(b.id()).append(holds.isEmpty() ? " (draws what there is)" : ": " + holds);
		}

		for (MapBridge b : WAITING) {
			out.append("; ").append(b.id()).append(": waiting for its mod");
		}

		return out.toString();
	}

	public static void xaeroHighlighters(String id, Object registry) {
		if (bridge(id) instanceof XaeroBridge xaero) {
			try {
				xaero.highlighters(registry);
			} catch (Throwable var5) {
				CFHGuiTeams.LOGGER.warn("[CFHGui Teams] {} has changed since this version was made; land is not shown on its map ({})", id, var5.toString());
			}
		}
	}

	private TeamsCompat() {
	}

	private static final class Xaero {
		static MapBridge worldMap() {
			return new XaeroWorldMapBridge();
		}

		static MapBridge minimap() {
			return new XaeroMinimapBridge();
		}
	}
}
