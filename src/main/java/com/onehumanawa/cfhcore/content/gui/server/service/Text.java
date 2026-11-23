package com.onehumanawa.cfhcore.content.gui.server.service;

import com.onehumanawa.cfhcore.content.gui.server.model.PlayerRecord;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;

public final class Text {
	public static final int MAX_SECONDS = 315360000;
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT).withZone(ZoneId.systemDefault());

	@Nullable
	public static String name(@Nullable String raw) {
		if (raw == null) {
			return null;
		} else {
			String name = raw.trim();
			if (name.length() >= 1 && name.length() <= 24) {
				for (int i = 0; i < name.length(); i++) {
					char c = name.charAt(i);
					if (!latinOrDigit(c) && c != '_' && c != '-') {
						return null;
					}
				}

				return name;
			} else {
				return null;
			}
		}
	}

	@Nullable
	public static String id(@Nullable String raw) {
		if (raw == null) {
			return null;
		} else {
			String id = raw.trim().toLowerCase(Locale.ROOT);
			if (!id.isEmpty() && id.length() <= 24) {
				for (int i = 0; i < id.length(); i++) {
					char c = id.charAt(i);
					if ((c < 'a' || c > 'z') && (c < '0' || c > '9') && c != '_') {
						return null;
					}
				}

				return id;
			} else {
				return null;
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

	public static boolean fits(@Nullable String raw, int max) {
		if (raw == null) {
			return false;
		} else {
			String tidy = line(raw, max + 1);
			return !tidy.isEmpty() && tidy.codePointCount(0, tidy.length()) <= max;
		}
	}

	@Nullable
	public static String nick(@Nullable String raw, boolean colours) {
		if (raw == null) {
			return null;
		} else {
			String nick = raw.trim();
			if (nick.length() > 64) {
				return null;
			} else {
				String plain = plainNick(nick);
				if (!colours && !plain.equals(nick)) {
					return null;
				} else if (plain.length() >= 3 && plain.length() <= 16) {
					for (int i = 0; i < plain.length(); i++) {
						char c = plain.charAt(i);
						if (!latinOrDigit(c) && c != '_') {
							return null;
						}
					}

					return nick;
				} else {
					return null;
				}
			}
		}
	}

	public static String shown(PlayerRecord record) {
		return plainNick(record.shownName());
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

	public static MutableComponent styled(String nick) {
		if (nick.indexOf(38) < 0) {
			return Component.literal(nick);
		} else {
			MutableComponent out = Component.empty();
			Style style = Style.EMPTY;
			StringBuilder run = new StringBuilder();

			for (int i = 0; i < nick.length(); i++) {
				char c = nick.charAt(i);
				ChatFormatting f = c == '&' && i + 1 < nick.length() ? ChatFormatting.getByCode(Character.toLowerCase(nick.charAt(i + 1))) : null;
				if (f == null) {
					run.append(c);
				} else {
					if (run.length() > 0) {
						out.append(Component.literal(run.toString()).setStyle(style));
						run.setLength(0);
					}

					style = f != ChatFormatting.RESET && !f.isColor() ? style.applyFormat(f) : (f == ChatFormatting.RESET ? Style.EMPTY : Style.EMPTY.withColor(f));
					i++;
				}
			}

			if (run.length() > 0) {
				out.append(Component.literal(run.toString()).setStyle(style));
			}

			return out;
		}
	}

	public static int seconds(@Nullable String raw) {
		if (raw == null) {
			return -1;
		} else {
			String s = raw.trim().toLowerCase(Locale.ROOT);
			if (!s.isEmpty() && s.length() <= 24) {
				long total = 0L;
				long number = -1L;

				for (int i = 0; i < s.length(); i++) {
					char c = s.charAt(i);
					if (c >= '0' && c <= '9') {
						number = (number < 0L ? 0L : number) * 10L + (long)(c - '0');
						if (number > 315360000L) {
							return -1;
						}
					} else {
						if (number < 0L) {
							return -1;
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
							return -1;
						}

						total += number * unit;
						if (total > 315360000L) {
							return -1;
						}

						number = -1L;
					}
				}

				return number < 0L && total > 0L ? (int)total : -1;
			} else {
				return -1;
			}
		}
	}

	public static String span(long seconds) {
		long left = Math.max(0L, seconds);
		long days = left / 86400L;
		left %= 86400L;
		long hours = left / 3600L;
		left %= 3600L;
		long minutes = left / 60L;
		long secs = left % 60L;
		if (days > 0L) {
			return hours > 0L ? days + "d " + hours + "h" : days + "d";
		} else if (hours > 0L) {
			return minutes > 0L ? hours + "h " + minutes + "m" : hours + "h";
		} else if (minutes > 0L) {
			return secs > 0L ? minutes + "m " + secs + "s" : minutes + "m";
		} else {
			return secs + "s";
		}
	}

	public static String until(long untilMs, long nowMs) {
		return span((Math.max(0L, untilMs - nowMs) + 999L) / 1000L);
	}

	public static String date(long epochMs) {
		return DATE.format(Instant.ofEpochMilli(epochMs));
	}

	public static int color(@Nullable String raw) {
		if (raw == null) {
			return -1;
		} else {
			String s = raw.trim();
			ChatFormatting named = ChatFormatting.getByName(s);
			if (named != null && named.isColor() && named.getColor() != null) {
				return named.getColor();
			} else {
				String hex = s.startsWith("#") ? s.substring(1) : s;
				if (hex.length() != 6) {
					return -1;
				} else {
					try {
						return Integer.parseInt(hex, 16) & 16777215;
					} catch (NumberFormatException var5) {
						return -1;
					}
				}
			}
		}
	}

	public static String hex(int rgb) {
		return String.format(Locale.ROOT, "#%06X", rgb & 16777215);
	}

	private static boolean latinOrDigit(char c) {
		return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9';
	}

	private Text() {
	}
}
