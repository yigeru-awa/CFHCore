package com.onehumanawa.cfhcore.content.gui.teams.compat.xaero;

import com.onehumanawa.cfhcore.content.gui.teams.CFHGuiTeams;
import com.onehumanawa.cfhcore.content.gui.teams.compat.Linkage;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.jetbrains.annotations.Nullable;
import xaero.common.HudMod;
import xaero.common.minimap.MinimapProcessor;
import xaero.common.minimap.highlight.DimensionHighlighterHandler;
import xaero.common.minimap.highlight.HighlighterRegistry;
import xaero.common.minimap.write.MinimapWriter;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.Minimap;
import xaero.hud.minimap.module.MinimapSession;

public final class XaeroMinimapBridge implements XaeroBridge {
	private static volatile boolean off;

	static boolean off() {
		return off;
	}

	@Override
	public String id() {
		return "xaerominimap";
	}

	@Override
	public boolean attach() {
		try {
			HudMod mod = HudMod.INSTANCE;
			if (mod == null || !mod.isLoadedClient()) {
				return false;
			}

			Minimap minimap = mod.getMinimap();
			if (minimap == null) {
				return false;
			}

			if (!MinimapMarks.fits()) {
				throw new IllegalStateException("its element renderers ask for more than CFHGui Teams knows");
			}

			minimap.getOverMapRendererHandler().add(new MinimapMarks());
		} catch (Throwable var3) {
			CFHGuiTeams.LOGGER
				.warn("[CFHGui Teams] Xaero's Minimap has changed since this version was made; teammates and waypoints are not shown on it ({})", var3.toString());
		}

		return true;
	}

	@Override
	public void highlighters(Object registry) {
		if (!Linkage.complete(MinimapClaims.class)) {
			throw new IllegalStateException("its highlighters ask for more than CFHGui Teams knows");
		} else {
			((HighlighterRegistry)registry).register(new MinimapClaims());
		}
	}

	@Override
	public void claimsChanged(String dimension, @Nullable LongSet regions) {
		MinimapSession session = (MinimapSession)BuiltInHudModules.MINIMAP.getCurrentSession();
		if (session != null) {
			MinimapProcessor processor = session.getProcessor();
			MinimapWriter writer = processor == null ? null : processor.getMinimapWriter();
			DimensionHighlighterHandler handler = writer == null ? null : writer.getDimensionHighlightHandler();
			if (handler != null) {
				handler.requestRefresh();
			}
		}
	}

	@Override
	public void abandon() {
		off = true;
	}
}
