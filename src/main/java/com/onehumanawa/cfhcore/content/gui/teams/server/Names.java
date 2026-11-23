package com.onehumanawa.cfhcore.content.gui.teams.server;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

public final class Names {
	private static final int[] PALETTE = new int[]{
		14254174, 6061306, 6074742, 14722136, 13007069, 5682882, 15026253, 12100067, 9420634, 14846896, 5219289, 13939802
	};

	@Nullable
	public static String team(@Nullable String raw) {
		if (raw == null) {
			return null;
		} else {
			String name = raw.trim().replaceAll(" +", " ");
			int length = name.codePointCount(0, name.length());
			if (length >= 3 && length <= 24) {
				boolean solid = false;
				int i = 0;

				while (i < name.length()) {
					int cp = name.codePointAt(i);
					i += Character.charCount(cp);
					if (Character.isLetterOrDigit(cp)) {
						solid = true;
					} else if (cp != 32 && cp != 95 && cp != 39 && cp != 45) {
						return null;
					}
				}

				return solid ? name : null;
			} else {
				return null;
			}
		}
	}

	@Nullable
	public static String tag(@Nullable String raw) {
		if (raw == null) {
			return "";
		} else {
			String tag = raw.trim();
			if (tag.isEmpty()) {
				return "";
			} else {
				int length = tag.codePointCount(0, tag.length());
				if (length >= 2 && length <= 5) {
					int i = 0;

					while (i < tag.length()) {
						int cp = tag.codePointAt(i);
						i += Character.charCount(cp);
						if (!Character.isLetterOrDigit(cp)) {
							return null;
						}
					}

					return tag;
				} else {
					return null;
				}
			}
		}
	}

	public static String line(@Nullable String raw, int max) {
		if (raw == null) {
			return "";
		} else {
			StringBuilder sb = new StringBuilder(Math.min(raw.length(), max));
			int count = 0;
			int i = 0;

			while (i < raw.length() && count < max) {
				int cp = raw.codePointAt(i);
				i += Character.charCount(cp);
				if (cp == 167) {
					if (i < raw.length()) {
						i += Character.charCount(raw.codePointAt(i));
					}
				} else if (!Character.isISOControl(cp)) {
					sb.appendCodePoint(cp);
					count++;
				}
			}

			return sb.toString().trim();
		}
	}

	public static int colorFor(String name) {
		return PALETTE[Math.floorMod(TeamStore.key(name).hashCode(), PALETTE.length)];
	}

	public static DyeColor nearestDye(int rgb) {
		DyeColor best = DyeColor.WHITE;
		int bestDistance = Integer.MAX_VALUE;

		for (DyeColor dye : DyeColor.values()) {
			int d = distance(rgb, dye.getTextureDiffuseColor());
			if (d < bestDistance) {
				bestDistance = d;
				best = dye;
			}
		}

		return best;
	}

	public static ChatFormatting nearestFormatting(int rgb) {
		ChatFormatting best = ChatFormatting.WHITE;
		int bestDistance = Integer.MAX_VALUE;

		for (ChatFormatting f : ChatFormatting.values()) {
			Integer color = f.getColor();
			if (f.isColor() && color != null) {
				int d = distance(rgb, color);
				if (d < bestDistance) {
					bestDistance = d;
					best = f;
				}
			}
		}

		return best;
	}

	private static int distance(int a, int b) {
		int dr = (a >> 16 & 0xFF) - (b >> 16 & 0xFF);
		int dg = (a >> 8 & 0xFF) - (b >> 8 & 0xFF);
		int db = (a & 0xFF) - (b & 0xFF);
		return 3 * dr * dr + 4 * dg * dg + 2 * db * db;
	}

	private Names() {
	}
}
