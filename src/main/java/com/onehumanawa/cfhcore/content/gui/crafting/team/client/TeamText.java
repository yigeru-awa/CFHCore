package com.onehumanawa.cfhcore.content.gui.crafting.team.client;

import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.crafting.team.Pin;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

final class TeamText {
	static Component quiet(Component text) {
		int color = Theme.current().isVanilla() ? 10526880 : Theme.current().palette().textMuted() & 16777215;
		return text.copy().withStyle(s -> s.withColor(color));
	}

	static Component hint(String key, Object... args) {
		int color = Theme.current().isVanilla() ? 8421504 : Theme.current().palette().textDim() & 16777215;
		return Component.translatable(key, args).withStyle(s -> s.withColor(color));
	}

	static Component pinnedBy(Pin pin) {
		boolean mine = pin.by.equals(TeamPinsClient.self());
		Component who = mine ? Component.translatable("cfhgui_crafting.team.you") : Component.literal(pin.byName.isEmpty() ? "?" : pin.byName);
		return quiet(Component.translatable("cfhgui_crafting.team.pinned_by", new Object[]{who, ago(pin.at)}));
	}

	static Component ago(long at) {
		long s = Math.max(0L, (System.currentTimeMillis() - at) / 1000L);
		if (s < 60L) {
			return Component.translatable("cfhgui_crafting.team.ago.now");
		} else if (s < 3600L) {
			return count("minutes", s / 60L);
		} else {
			return s < 86400L ? count("hours", s / 3600L) : count("days", s / 86400L);
		}
	}

	private static MutableComponent count(String unit, long n) {
		return Component.translatable("cfhgui_crafting.team.ago." + unit + (n == 1L ? ".one" : ""), new Object[]{n});
	}

	static Component steps(int n) {
		return Component.translatable(n == 1 ? "cfhgui_crafting.team.steps.one" : "cfhgui_crafting.team.steps", new Object[]{n});
	}

	private TeamText() {
	}
}
