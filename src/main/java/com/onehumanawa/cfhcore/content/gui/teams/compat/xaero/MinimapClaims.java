package com.onehumanawa.cfhcore.content.gui.teams.compat.xaero;

import com.onehumanawa.cfhcore.content.gui.teams.compat.MapClaims;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import xaero.common.minimap.highlight.ChunkHighlighter;
import xaero.hud.minimap.info.render.compile.InfoDisplayCompiler;

final class MinimapClaims extends ChunkHighlighter {
	@Nullable
	private MapClaims.Team tipTeam;
	private boolean tipKept;
	private int tipWidth;
	private List<Component> tip = List.of();

	MinimapClaims() {
		super(true);
	}

	private static MapClaims.Dim land(ResourceKey<Level> dimension) {
		return XaeroMinimapBridge.off() ? MapClaims.Dim.EMPTY : MapClaims.of(dimension);
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

	public void addChunkHighlightTooltips(InfoDisplayCompiler compiler, ResourceKey<Level> dimension, int chunkX, int chunkZ, int width) {
		MapClaims.Dim land = land(dimension);
		MapClaims.Team team = land.at(chunkX, chunkZ);
		if (team != null) {
			boolean kept = land.forced(chunkX, chunkZ) && team.relation == MapClaims.Relation.OWN;
			if (this.tipTeam != team || this.tipKept != kept || this.tipWidth != width) {
				this.tipTeam = team;
				this.tipKept = kept;
				this.tipWidth = width;
				this.tip = lines(XaeroLand.line(team, kept), width);
			}

			for (Component line : this.tip) {
				compiler.addLine(line);
			}
		}
	}

	private static List<Component> lines(Component text, int width) {
		List<Component> out = new ArrayList<>();

		for (FormattedText part : Minecraft.getInstance().font.getSplitter().splitLines(text, Math.max(40, width), Style.EMPTY)) {
			MutableComponent line = Component.empty();
			part.visit((style, words) -> {
				line.append(Component.literal(words).withStyle(style));
				return Optional.empty();
			}, Style.EMPTY);
			out.add(line);
		}

		return out;
	}
}
