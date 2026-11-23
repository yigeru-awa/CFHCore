package com.onehumanawa.cfhcore.content.gui.teams.compat.xaero;

import com.onehumanawa.cfhcore.content.gui.teams.CFHGuiTeams;
import com.onehumanawa.cfhcore.content.gui.teams.compat.Linkage;
import com.onehumanawa.cfhcore.content.gui.teams.compat.MapClaims;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.WorldMapSession;
import xaero.map.element.MapElementRenderHandler;
import xaero.map.gui.GuiMap;
import xaero.map.highlight.DimensionHighlighterHandler;
import xaero.map.highlight.HighlighterRegistry;
import xaero.map.world.MapDimension;
import xaero.map.world.MapWorld;

public final class XaeroWorldMapBridge implements XaeroBridge {
	private static final int MANY_REGIONS = 256;
	private static volatile boolean off;

	static boolean off() {
		return off;
	}

	@Override
	public String id() {
		return "xaeroworldmap";
	}

	@Override
	public boolean attach() {
		try {
			MapElementRenderHandler handler = WorldMap.mapElementRenderHandler;
			if (handler == null) {
				return false;
			}

			if (!WorldMapMarks.fits()) {
				throw new IllegalStateException("its element renderers ask for more than CFHGui Teams knows");
			}

			handler.add(new WorldMapMarks());
		} catch (Throwable var2) {
			CFHGuiTeams.LOGGER
				.warn("[CFHGui Teams] Xaero's World Map has changed since this version was made; teammates and waypoints are not shown on it ({})", var2.toString());
		}

		return true;
	}

	@Override
	public void highlighters(Object registry) {
		if (!Linkage.complete(WorldMapClaims.class)) {
			throw new IllegalStateException("its highlighters ask for more than CFHGui Teams knows");
		} else {
			((HighlighterRegistry)registry).register(new WorldMapClaims());
		}
	}

	@Override
	public void claimsChanged(String dimension, @Nullable LongSet regions) {
		ResourceLocation id = ResourceLocation.tryParse(dimension);
		WorldMapSession session = WorldMapSession.getCurrentSession();
		if (id != null && session != null) {
			MapProcessor processor = session.getMapProcessor();
			MapWorld world = processor == null ? null : processor.getMapWorld();
			if (world != null) {
				ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
				MapDimension mapped = world.getDimension(key);
				if (mapped != null) {
					DimensionHighlighterHandler handler = mapped.getHighlightHandler();
					if (handler != null) {
						if (regions != null && regions.size() <= 256) {
							LongIterator var10 = regions.iterator();

							while (var10.hasNext()) {
								long region = (Long)var10.next();
								handler.clearCachedHash(MapClaims.regionX(region), MapClaims.regionZ(region));
							}
						} else {
							handler.clearCachedHashes();
						}
					}
				}
			}
		}
	}

	@Override
	public void abandon() {
		off = true;
	}

	@Nullable
	@Override
	public Screen mapScreen() {
		WorldMapSession session = WorldMapSession.getCurrentSession();
		return session != null && session.getMapProcessor() != null
			? new GuiMap(null, null, session.getMapProcessor(), Minecraft.getInstance().getCameraEntity())
			: null;
	}
}
