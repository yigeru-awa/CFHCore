package com.onehumanawa.cfhcore.content.gui.server.client.ui.kit;

import com.onehumanawa.cfhcore.content.gui.server.model.Place;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;

public final class Txt {
	private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);
	@Nullable
	private static DateTimeFormatter dateFormat;
	@Nullable
	private static Locale dateLocale;

	public static MutableComponent t(String key, Object... args) {
		return Component.translatable("cfhgui_server.ui." + key, args);
	}

	public static String s(String key, Object... args) {
		return I18n.get("cfhgui_server.ui." + key, args);
	}

	public static Component page(String id) {
		return Component.translatable("cfhgui_server.page." + id);
	}

	public static Component adminPage(String id) {
		return Component.translatable("cfhgui_server.admin.page." + id);
	}

	public static String span(long seconds) {
		long s = Math.max(0L, seconds);
		if (s < 60L) {
			return unit("seconds", s);
		} else if (s >= 3600L) {
			return s < 86400L
				? unit("hours", s / 3600L) + (s % 3600L < 60L ? "" : " " + unit("minutes", s % 3600L / 60L))
				: unit("days", s / 86400L) + (s % 86400L < 3600L ? "" : " " + unit("hours", s % 86400L / 3600L));
		} else {
			return unit("minutes", s / 60L) + (s % 60L != 0L && s < 600L ? " " + unit("seconds", s % 60L) : "");
		}
	}

	private static String unit(String name, long n) {
		return s("unit." + name, n);
	}

	public static String spanTyped(long seconds) {
		long s = Math.max(0L, seconds);
		StringBuilder out = new StringBuilder();
		long[] sizes = new long[]{86400L, 3600L, 60L, 1L};
		char[] letters = new char[]{'d', 'h', 'm', 's'};

		for (int i = 0; i < sizes.length; i++) {
			if (s >= sizes[i]) {
				out.append(s / sizes[i]).append(letters[i]);
			}

			s %= sizes[i];
		}

		return out.isEmpty() ? "0s" : out.toString();
	}

	public static String ticks(long ticks) {
		return span(ticks / 20L);
	}

	public static Component ago(long epochMs) {
		if (epochMs <= 0L) {
			return t("time.never");
		} else {
			long s = (System.currentTimeMillis() - epochMs) / 1000L;
			return s < 45L ? t("time.now") : t("time.ago", span(s < 3600L ? s / 60L * 60L : s));
		}
	}

	public static String since(long epochMs) {
		long s = Math.max(0L, (System.currentTimeMillis() - epochMs) / 1000L);
		return span(s < 3600L ? s / 60L * 60L : s / 3600L * 3600L);
	}

	public static Component until(long epochMs) {
		if (epochMs == 0L) {
			return t("time.forever");
		} else {
			long s = (epochMs - System.currentTimeMillis()) / 1000L;
			return s <= 0L ? t("time.over") : t("time.in", span(s));
		}
	}

	public static String left(long epochMs) {
		return span(Math.max(0L, (epochMs - System.currentTimeMillis() + 999L) / 1000L));
	}

	private static Locale gameLocale() {
		try {
			String[] parts = Minecraft.getInstance().getLanguageManager().getSelected().split("_");
			return parts.length >= 2 ? Locale.of(parts[0], parts[1].toUpperCase(Locale.ROOT)) : Locale.of(parts[0]);
		} catch (RuntimeException var1) {
			return Locale.getDefault();
		}
	}

	public static String date(long epochMs) {
		if (epochMs <= 0L) {
			return "";
		} else {
			Locale locale = gameLocale();
			if (dateFormat == null || !locale.equals(dateLocale)) {
				dateLocale = locale;
				dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", locale);
			}

			return dateFormat.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()));
		}
	}

	public static String clock(long epochMs) {
		return CLOCK.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()));
	}

	public static long parseSpan(String text) {
		String s = text == null ? "" : text.trim().toLowerCase(Locale.ROOT).replace(" ", "");
		if (s.isEmpty()) {
			return -1L;
		} else {
			long total = 0L;
			long number = -1L;

			for (int i = 0; i < s.length(); i++) {
				char c = s.charAt(i);
				if (c >= '0' && c <= '9') {
					number = (number < 0L ? 0L : number) * 10L + (long)(c - '0');
					if (number > 100000000L) {
						return -1L;
					}
				} else {
					if (number < 0L) {
						return -1L;
					}
					long unit = switch (c) {
						case 'd' -> 86400L;
						case 'h' -> 3600L;
						case 'm' -> 60L;
						case 's' -> 1L;
						case 'w' -> 604800L;
						default -> -1L;
					};
					if (unit < 0L) {
						return -1L;
					}

					total += number * unit;
					number = -1L;
				}
			}

			if (number >= 0L) {
				total += number * 60L;
			}

			return total > 0L && total <= 315360000L ? total : -1L;
		}
	}

	public static Component dimension(String id) {
		String key = "cfhgui_server.ui.dim." + id.replace(':', '.');
		if (I18n.exists(key)) {
			return Component.translatable(key);
		} else {
			String path = id.contains(":") ? id.substring(id.indexOf(58) + 1) : id;
			StringBuilder sb = new StringBuilder(path.length());
			boolean up = true;

			for (char c : path.toCharArray()) {
				if (c != '_' && c != '/') {
					sb.append(up ? Character.toUpperCase(c) : c);
					up = false;
				} else {
					sb.append(' ');
					up = true;
				}
			}

			return Component.literal(sb.toString());
		}
	}

	public static String coords(Place p) {
		return p.blockX() + ", " + p.blockY() + ", " + p.blockZ();
	}

	public static String distance(double blocks) {
		if (blocks < 1000.0) {
			return s("unit.blocks", Math.round(blocks));
		} else {
			return blocks < 10000.0 ? s("unit.kilometres", String.format(Locale.ROOT, "%.1f", blocks / 1000.0)) : s("unit.kilometres", Math.round(blocks / 1000.0));
		}
	}

	public static String number(long n) {
		return String.format(Locale.ROOT, "%,d", n);
	}

	public static MutableComponent nick(String nick) {
		if (nick.indexOf(38) < 0) {
			return Component.literal(nick);
		} else {
			MutableComponent out = Component.empty();
			Style style = Style.EMPTY;
			StringBuilder run = new StringBuilder();

			for (int i = 0; i < nick.length(); i++) {
				char c = nick.charAt(i);
				ChatFormatting code = c == '&' && i + 1 < nick.length() ? ChatFormatting.getByCode(Character.toLowerCase(nick.charAt(i + 1))) : null;
				if (code == null) {
					run.append(c);
				} else {
					if (!run.isEmpty()) {
						out.append(Component.literal(run.toString()).setStyle(style));
						run.setLength(0);
					}

					if (code == ChatFormatting.RESET) {
						style = Style.EMPTY;
					} else if (code.isColor()) {
						style = Style.EMPTY.withColor(code);
					} else if (code != ChatFormatting.OBFUSCATED) {
						style = style.applyFormat(code);
					}

					i++;
				}
			}

			if (!run.isEmpty()) {
				out.append(Component.literal(run.toString()).setStyle(style));
			}

			return out;
		}
	}

	public static String plainNick(String nick) {
		if (nick.indexOf(38) < 0) {
			return nick;
		} else {
			StringBuilder sb = new StringBuilder(nick.length());

			for (int i = 0; i < nick.length(); i++) {
				char c = nick.charAt(i);
				if (c == '&' && i + 1 < nick.length() && ChatFormatting.getByCode(Character.toLowerCase(nick.charAt(i + 1))) != null) {
					i++;
				} else {
					sb.append(c);
				}
			}

			return sb.toString();
		}
	}

	private Txt() {
	}
}
