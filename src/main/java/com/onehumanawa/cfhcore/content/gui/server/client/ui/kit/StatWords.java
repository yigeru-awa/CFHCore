package com.onehumanawa.cfhcore.content.gui.server.client.ui.kit;

import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class StatWords {
	public static final List<String> EXTRAS = List.of("afk_ticks", "sessions", "first_seen_ms", "last_seen_ms");

	public static Component name(String key) {
		int bar = key.indexOf(124);
		if (bar < 0) {
			return Txt.t("stat." + key);
		} else {
			ResourceLocation type = ResourceLocation.tryParse(key.substring(0, bar));
			ResourceLocation value = ResourceLocation.tryParse(key.substring(bar + 1));
			if (type == null || value == null) {
				return Component.literal(key);
			} else if (type.equals(BuiltInRegistries.STAT_TYPE.getKey(Stats.CUSTOM))) {
				return Component.translatable("stat." + value.getNamespace() + "." + value.getPath().replace('/', '.'));
			} else {
				StatType<?> statType = (StatType<?>)BuiltInRegistries.STAT_TYPE.get(type);
				return statType == null
					? Component.literal(key)
					: Component.literal("").append(thing(statType.getRegistry(), value)).append(": ").append(statType.getDisplayName());
			}
		}
	}

	private static Component thing(Registry<?> registry, ResourceLocation id) {
		Object o = registry.get(id);
		if (o instanceof Item item) {
			return item.getDescription();
		} else if (o instanceof Block block) {
			return block.getName();
		} else {
			return (Component)(o instanceof EntityType<?> entity ? entity.getDescription() : Component.literal(id.getPath()));
		}
	}

	public static String value(String key, long value) {
		int bar = key.indexOf(124);
		if (bar < 0) {
			return switch (key) {
				case "afk_ticks" -> Txt.ticks(value);
				case "first_seen_ms", "last_seen_ms" -> Txt.date(value);
				default -> Txt.number(value);
			};
		} else {
			ResourceLocation type = ResourceLocation.tryParse(key.substring(0, bar));
			ResourceLocation id = ResourceLocation.tryParse(key.substring(bar + 1));
			int v = (int)Math.max(-2147483648L, Math.min(2147483647L, value));
			if (type != null && id != null && type.equals(BuiltInRegistries.STAT_TYPE.getKey(Stats.CUSTOM))) {
				ResourceLocation registered = (ResourceLocation)BuiltInRegistries.CUSTOM_STAT.get(id);
				if (registered != null) {
					return Stats.CUSTOM.get(registered).format(v);
				}
			}

			return Txt.number(value);
		}
	}

	public static List<String> general() {
		List<String> out = new ArrayList<>();
		ResourceLocation custom = BuiltInRegistries.STAT_TYPE.getKey(Stats.CUSTOM);

		for (ResourceLocation id : BuiltInRegistries.CUSTOM_STAT.keySet()) {
			out.add(StatsLink.key(String.valueOf(custom), id.toString()));
		}

		return out;
	}

	private StatWords() {
	}
}
