package com.onehumanawa.cfhcore.content.gui.teams.compat.xaero;

import com.onehumanawa.cfhcore.content.gui.teams.compat.MapClaims;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

final class XaeroLand {
	static int color(int rgb, int alpha) {
		return (rgb & 0xFF) << 24 | (rgb >> 8 & 0xFF) << 16 | (rgb >> 16 & 0xFF) << 8 | alpha & 0xFF;
	}

	static void markKept(int[] chunk, MapClaims.Team team) {
		int r = team.rgb >> 16 & 0xFF;
		int g = team.rgb >> 8 & 0xFF;
		int b = team.rgb & 0xFF;
		int light = r + (255 - r) * 2 / 3 << 16 | g + (255 - g) * 2 / 3 << 8 | b + (255 - b) * 2 / 3;
		int mark = color(light, 235);

		for (int i = 6; i <= 9; i++) {
			chunk[96 | i] = mark;
			chunk[144 | i] = mark;
			chunk[i << 4 | 6] = mark;
			chunk[i << 4 | 9] = mark;
		}
	}

	static MutableComponent line(MapClaims.Team team, boolean kept) {
		MutableComponent line = Component.literal("□ ").withStyle(s -> s.withColor(team.rgb));
		MutableComponent name = team.name.isEmpty() ? Component.translatable("cfhgui_teams.map.claimed") : Component.literal(team.name);
		line.append(name.withStyle(ChatFormatting.WHITE));
		if (team.relation == MapClaims.Relation.OWN) {
			line.append(note(team.personal ? "cfhgui_teams.map.own_land" : "cfhgui_teams.map.own"));
		} else if (team.relation == MapClaims.Relation.ALLY) {
			line.append(note("cfhgui_teams.map.ally"));
		} else if (team.server) {
			line.append(note("cfhgui_teams.map.server"));
		}

		if (kept) {
			line.append(note("cfhgui_teams.map.kept_loaded"));
		}

		return line;
	}

	private static MutableComponent note(String key) {
		return Component.literal(" · ").append(Component.translatable(key)).withStyle(ChatFormatting.GRAY);
	}

	private XaeroLand() {
	}
}
