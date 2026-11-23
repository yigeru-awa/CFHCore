package com.onehumanawa.cfhcore.content.gui.quests.server.ftb;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;

final class FtbText {
	private static final int WRAPPED = 60;
	private static final String[] COLOR_NAMES = new String[]{
		"black",
		"dark_blue",
		"dark_green",
		"dark_aqua",
		"dark_red",
		"dark_purple",
		"gold",
		"gray",
		"dark_gray",
		"blue",
		"green",
		"aqua",
		"red",
		"light_purple",
		"yellow",
		"white"
	};
	static final String[] COLORS = new String[]{
		"#000000",
		"#0000AA",
		"#00AA00",
		"#00AAAA",
		"#AA0000",
		"#AA00AA",
		"#FFAA00",
		"#AAAAAA",
		"#555555",
		"#5555FF",
		"#55FF55",
		"#55FFFF",
		"#FF5555",
		"#FF55FF",
		"#FFFF55",
		"#FFFFFF"
	};
	private final FtbItems.Notes notes;
	private final Function<String, String> linkTo;
	int joined;

	FtbText(FtbItems.Notes notes, Function<String, String> linkTo) {
		this.notes = notes;
		this.linkTo = linkTo;
	}

	static String text(@Nullable String ftb) {
		return ftb == null ? "" : ftb.replace("\\&", "&&").replace('§', '&').trim();
	}

	static String plain(String text) {
		StringBuilder sb = new StringBuilder(text.length());

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '&' && i + 1 < text.length() && "0123456789abcdefklmnorABCDEFKLMNOR&".indexOf(text.charAt(i + 1)) >= 0) {
				if (text.charAt(i + 1) == '&') {
					sb.append('&');
				}

				i++;
			} else {
				sb.append(c);
			}
		}

		return sb.toString();
	}

	List<String> paragraphs(List<String> lines, String where) {
		List<String> out = new ArrayList<>();
		StringBuilder current = null;
		String last = "";

		for (String raw : lines) {
			for (String piece : raw.indexOf(10) >= 0 ? raw.split("\n", -1) : new String[]{raw}) {
				String line = this.line(piece, where);
				if (line.isBlank()) {
					if (current != null) {
						out.add(current.toString());
					}

					current = null;
				} else {
					boolean alone = line.equals("---") || line.startsWith("![") || line.startsWith("#") || key(line);
					if (current != null && !alone && !listed(line) && plain(last).length() >= 60) {
						current.append(' ').append(line);
						this.joined++;
					} else {
						if (current != null) {
							out.add(current.toString());
						}

						current = new StringBuilder(line);
					}

					if (alone) {
						out.add(current.toString());
						current = null;
					}

					last = line;
				}
			}
		}

		if (current != null) {
			out.add(current.toString());
		}

		return out;
	}

	private static boolean key(String line) {
		return line.length() > 2 && line.charAt(0) == '{' && line.charAt(line.length() - 1) == '}' && line.indexOf(32) < 0 && line.indexOf(58) < 0;
	}

	private static boolean listed(String line) {
		String p = plain(line).stripLeading();
		if (p.isEmpty()) {
			return false;
		} else {
			char c = p.charAt(0);
			return c != '-' && c != '*' && c != 8226 && c != 183 && c != '>' && c != 10004 && c != 10008 && c != 9654
				? p.length() > 2 && Character.isDigit(c) && (p.charAt(1) == '.' || p.charAt(1) == ')')
				: true;
		}
	}

	String line(String raw, String where) {
		String s = raw.trim();
		if (s.equals("{@pagebreak}")) {
			return "---";
		} else if (s.startsWith("{image:") && s.endsWith("}")) {
			String[] parts = s.substring("{image:".length(), s.length() - 1).trim().split("\\s+");
			String width = "";
			String height = "";

			for (String part : parts) {
				if (part.startsWith("width:")) {
					width = part.substring("width:".length());
				} else if (part.startsWith("height:")) {
					height = part.substring("height:".length());
				}
			}

			String size = (width.isBlank() ? "" : "w=" + width) + (height.isBlank() ? "" : (width.isBlank() ? "" : " ") + "h=" + height);
			return "![](" + FtbItems.texture(parts[0]) + ")" + (size.isEmpty() ? "" : "{" + size + "}");
		} else {
			if (s.startsWith("{") && s.endsWith("}") && s.indexOf(34) > 0 || (s.startsWith("[\"") || s.startsWith("[{")) && s.endsWith("]")) {
				try {
					StringBuilder said = new StringBuilder();
					this.json(JsonParser.parseString(s), said, where);
					this.notes.turn("A line written as a raw JSON text (here: its words, with their style and where a click on them leads)", where);
					return said.toString().trim();
				} catch (Exception var11) {
				}
			}

			return text(raw);
		}
	}

	private void json(JsonElement json, StringBuilder out, String where) {
		if (json.isJsonPrimitive()) {
			out.append(text(json.getAsString()));
		} else if (json.isJsonArray()) {
			for (JsonElement e : json.getAsJsonArray()) {
				this.json(e, out, where);
			}
		} else if (json.isJsonObject()) {
			JsonObject o = json.getAsJsonObject();
			StringBuilder own = new StringBuilder();
			if (o.has("text")) {
				own.append(text(o.get("text").getAsString()));
			} else if (o.has("translate")) {
				own.append('{').append(o.get("translate").getAsString()).append('}');
			}

			String words = own.toString();
			if (!words.isBlank()) {
				String color = o.has("color") && o.get("color").isJsonPrimitive() ? o.get("color").getAsString().toLowerCase(Locale.ROOT) : "";
				int code = -1;

				for (int i = 0; i < COLOR_NAMES.length; i++) {
					if (COLOR_NAMES[i].equals(color)) {
						code = i;
					}
				}

				if (flag(o, "bold")) {
					words = "**" + words + "**";
				}

				if (flag(o, "italic")) {
					words = "*" + words + "*";
				}

				if (flag(o, "strikethrough")) {
					words = "~~" + words + "~~";
				}

				String target = null;
				if (o.has("clickEvent") && o.get("clickEvent").isJsonObject()) {
					JsonObject click = o.getAsJsonObject("clickEvent");
					String action = click.has("action") ? click.get("action").getAsString() : "";
					String value = click.has("value") ? click.get("value").getAsString().trim() : "";
					if (action.equals("open_url") && !value.isEmpty()) {
						target = value;
					} else if (action.equals("change_page") && !value.isEmpty()) {
						String quest = this.linkTo.apply(value.toUpperCase(Locale.ROOT));
						if (quest != null) {
							target = "quest:" + quest;
						} else {
							this.notes.approx("A link in a text to something that is not in the book (here: its words, without the link)", where + ": " + value);
						}
					} else if (!action.isEmpty()) {
						this.notes.approx("A click in a text that does something else than open a page or an address (here: its words only)", where + ": " + action);
					}
				}

				if (target != null) {
					words = "[" + words + "](" + target + ")";
				} else if (flag(o, "underlined")) {
					words = "__" + words + "__";
				}

				if (code >= 0) {
					words = "&" + "0123456789abcdef".charAt(code) + words + "&r";
				}

				out.append(words);
			}

			if (o.has("extra")) {
				this.json(o.get("extra"), out, where);
			}
		}
	}

	private static boolean flag(JsonObject o, String name) {
		if (o.has(name) && o.get(name).isJsonPrimitive()) {
			String v = o.get(name).getAsString();
			return v.equalsIgnoreCase("true") || v.equals("1");
		} else {
			return false;
		}
	}

	static String readable(String id) {
		String path = id.indexOf(58) >= 0 ? id.substring(id.indexOf(58) + 1) : id;
		return path.replace('_', ' ').replace('/', ' ');
	}
}
