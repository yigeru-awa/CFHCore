package com.onehumanawa.cfhcore.content.gui.teams.compat.xaero;

import com.onehumanawa.cfhcore.content.gui.teams.compat.MapClaims;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import xaero.map.highlight.ChunkHighlighter;

final class WorldMapClaims extends ChunkHighlighter {
	@Nullable
	private MapClaims.Team tipTeam;
	private boolean tipKept;
	@Nullable
	private Component tip;

	WorldMapClaims() {
		super(true);
	}

	private static MapClaims.Dim land(ResourceKey<Level> dimension) {
		return XaeroWorldMapBridge.off() ? MapClaims.Dim.EMPTY : MapClaims.of(dimension);
	}

	public int calculateRegionHash(ResourceKey<Level> dimension, int regionX, int regionZ) {
		return land(dimension).regionLook(regionX, regionZ);
	}

	public boolean regionHasHighlights(ResourceKey<Level> dimension, int regionX, int regionZ) {
		return land(dimension).regionLook(regionX, regionZ) != 0;
	}

	public boolean chunkIsHighlit(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
		return land(dimension).at(chunkX, chunkZ) != null;
	}

	protected int[] getColors(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
		MapClaims.Dim land = land(dimension);
		MapClaims.Team team = land.at(chunkX, chunkZ);
		if (team == null) {
			return null;
		} else {
			int fill = XaeroLand.color(team.rgb, team.fillAlpha);
			int edge = XaeroLand.color(team.rgb, team.borderAlpha);
			int[] out = this.resultStore;
			out[0] = fill;
			out[1] = land.at(chunkX, chunkZ - 1) == team ? fill : edge;
			out[2] = land.at(chunkX + 1, chunkZ) == team ? fill : edge;
			out[3] = land.at(chunkX, chunkZ + 1) == team ? fill : edge;
			out[4] = land.at(chunkX - 1, chunkZ) == team ? fill : edge;
			return out;
		}
	}

	public int[] getChunkHighlitColor(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
		int[] out = super.getChunkHighlitColor(dimension, chunkX, chunkZ);
		if (out != null && out.length >= 256) {
			MapClaims.Dim land = land(dimension);
			if (land.forced(chunkX, chunkZ)) {
				MapClaims.Team team = land.at(chunkX, chunkZ);
				if (team != null && team.relation == MapClaims.Relation.OWN) {
					XaeroLand.markKept(out, team);
				}
			}

			return out;
		} else {
			return out;
		}
	}

	public Component getChunkHighlightSubtleTooltip(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
		MapClaims.Dim land = land(dimension);
		MapClaims.Team team = land.at(chunkX, chunkZ);
		if (team == null) {
			return null;
		} else {
			boolean kept = land.forced(chunkX, chunkZ) && team.relation == MapClaims.Relation.OWN;
			if (this.tip == null || this.tipTeam != team || this.tipKept != kept) {
				this.tipTeam = team;
				this.tipKept = kept;
				this.tip = XaeroLand.line(team, kept);
			}

			return this.tip;
		}
	}

	public Component getChunkHighlightBluntTooltip(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
		return null;
	}

	public void addMinimapBlockHighlightTooltips(List<Component> lines, ResourceKey<Level> dimension, int blockX, int blockZ, int width) {
	}
}
