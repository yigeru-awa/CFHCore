package com.onehumanawa.cfhcore.content.gui.teams.client.overhaul;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

final class Build {
	private static final Map<String, BlockState> CACHE = new HashMap<>();

	static BlockState of(String spec) {
		return CACHE.computeIfAbsent(spec, Build::parse);
	}

	private static BlockState parse(String spec) {
		String text = spec.trim();
		int open = text.indexOf(91);
		String id = open < 0 ? text : text.substring(0, open);
		ResourceLocation key = ResourceLocation.tryParse(id.contains(":") ? id : "minecraft:" + id);
		Optional<Block> block = key == null ? Optional.empty() : BuiltInRegistries.BLOCK.getOptional(key);
		if (block.isEmpty()) {
			return Blocks.AIR.defaultBlockState();
		} else {
			BlockState state = block.get().defaultBlockState();
			if (open >= 0) {
				int close = text.lastIndexOf(93);

				for (String pair : text.substring(open + 1, close > open ? close : text.length()).split(",")) {
					int eq = pair.indexOf(61);
					if (eq > 0) {
						Property<?> property = state.getBlock().getStateDefinition().getProperty(pair.substring(0, eq).trim());
						if (property != null) {
							state = with(state, property, pair.substring(eq + 1).trim().toLowerCase(Locale.ROOT));
						}
					}
				}
			}

			return state;
		}
	}

	private static <T extends Comparable<T>> BlockState with(BlockState state, Property<T> property, String value) {
		return property.getValue(value).map(v -> (BlockState)state.setValue(property, v)).orElse(state);
	}

	static DyeColor dye(int rgb) {
		DyeColor best = DyeColor.WHITE;
		long nearest = Long.MAX_VALUE;
		int r = rgb >> 16 & 0xFF;
		int g = rgb >> 8 & 0xFF;
		int b = rgb & 0xFF;

		for (DyeColor d : DyeColor.values()) {
			int c = d.getTextureDiffuseColor();
			long dr = (long)((c >> 16 & 0xFF) - r);
			long dg = (long)((c >> 8 & 0xFF) - g);
			long db = (long)((c & 0xFF) - b);
			long far = 3L * dr * dr + 4L * dg * dg + 2L * db * db;
			if (far < nearest) {
				nearest = far;
				best = d;
			}
		}

		return best;
	}

	static BlockState wool(DyeColor dye) {
		return of(dye.getName() + "_wool");
	}

	static BlockState carpet(DyeColor dye) {
		return of(dye.getName() + "_carpet");
	}

	static float chance(int x, int z, int salt) {
		long h = (long)x * -7046029254386353131L + (long)z * -4658895280553007687L + (long)salt * -7723592293110705685L + 2685821657736338717L;
		h = (h ^ h >>> 30) * -4658895280553007687L;
		h = (h ^ h >>> 27) * -7723592293110705685L;
		h ^= h >>> 31;
		return (float)(h >>> 40) / 1.6777216E7F;
	}

	static float smooth(float x, float z, float scale, int salt) {
		float fx = x / scale;
		float fz = z / scale;
		int x0 = (int)Math.floor((double)fx);
		int z0 = (int)Math.floor((double)fz);
		float tx = fx - (float)x0;
		float tz = fz - (float)z0;
		tx = tx * tx * (3.0F - 2.0F * tx);
		tz = tz * tz * (3.0F - 2.0F * tz);
		float a = chance(x0, z0, salt);
		float b = chance(x0 + 1, z0, salt);
		float c = chance(x0, z0 + 1, salt);
		float d = chance(x0 + 1, z0 + 1, salt);
		return (a + (b - a) * tx) * (1.0F - tz) + (c + (d - c) * tx) * tz;
	}

	private Build() {
	}
}
