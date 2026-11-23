package com.onehumanawa.cfhcore.content.gui.quests.server.ftb;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import org.jetbrains.annotations.Nullable;

final class FtbItems {
	private static final int LONGEST_SPEC = 1200;
	@Nullable
	static volatile BiFunction<Map<String, Object>, Integer, Map<String, Object>> upgrader;
	private final int version;
	private final boolean oldTags;
	private final FtbItems.Notes notes;
	private static final String[][] ORE_PREFIXES = new String[][]{
		{"ingot", "ingots"},
		{"nugget", "nuggets"},
		{"dust", "dusts"},
		{"gem", "gems"},
		{"ore", "ores"},
		{"block", "storage_blocks"},
		{"plate", "plates"},
		{"gear", "gears"},
		{"rod", "rods"},
		{"stick", "rods"},
		{"wire", "wires"},
		{"crop", "crops"},
		{"seed", "seeds"},
		{"dye", "dyes"},
		{"log", "logs"},
		{"plank", "planks"},
		{"glass", "glass_blocks"},
		{"crystal", "gems"}
	};

	FtbItems(int version, boolean oldTags, FtbItems.Notes notes) {
		this.version = version;
		this.oldTags = oldTags;
		this.notes = notes;
	}

	FtbItems.Item read(@Nullable Object raw, String where) {
		if (raw instanceof String s) {
			String text = s.trim();
			if (text.isEmpty()) {
				return FtbItems.Item.NONE;
			} else if (text.indexOf(32) >= 0 || this.version == 1343) {
				String[] parts = text.split("\\s+");
				Map<String, Object> made = new LinkedHashMap<>();
				made.put("id", parts[0]);
				if (parts.length > 1) {
					made.put("Count", number(parts[1], "b"));
				}

				if (parts.length > 2) {
					made.put("Damage", number(parts[2], "s"));
				}

				return this.read(made, where);
			} else if (this.oldTags && upgrader != null) {
				Map<String, Object> bare = new LinkedHashMap<>();
				bare.put("id", text);
				return this.read(bare, where);
			} else {
				return new FtbItems.Item(text, text, 1L, false);
			}
		} else {
			Map<String, Object> map = Snbt.map(raw);
			if (!map.containsKey("id") && map.containsKey("item")) {
				FtbItems.Item inner = this.read(map.get("item"), where);
				long n = Snbt.number(map, "count", Snbt.number(map, "Count", 0L));
				return n > 0L && !inner.isEmpty() ? new FtbItems.Item(inner.spec(), inner.full(), n, inner.filter()) : inner;
			} else {
				String id = Snbt.string(map, "id", "").trim();
				if (id.isEmpty()) {
					return FtbItems.Item.NONE;
				} else {
					Map<String, Object> components = Snbt.map(map.get("components"));
					Map<String, Object> tag = Snbt.map(map.get("tag"));
					long count = Math.max(1L, Snbt.number(map, "count", Snbt.number(map, "Count", 1L)));
					switch (id) {
						case "ftbquests:missing_item":
							Object kept = components.containsKey("ftbquests:missing_item") ? components.get("ftbquests:missing_item") : tag.get("Item");
							FtbItems.Item inner = this.read(kept, where);
							return inner.isEmpty() ? FtbItems.Item.NONE : new FtbItems.Item(inner.spec(), inner.full(), Math.max(count, inner.count()), inner.filter());
						case "ftbfiltersystem:smart_filter":
							String filter = Snbt.string(components, "ftbfiltersystem:filter", Snbt.string(tag, "ftbfiltersystem:filter", "")).trim();
							String spec = this.filter(filter, where);
							return new FtbItems.Item(spec, spec, count, true);
						default:
							if (id.startsWith("itemfilters:")) {
								String specx = this.oldFilter(id, map, where);
								return specx.isEmpty() ? FtbItems.Item.NONE : new FtbItems.Item(specx, specx, count, true);
							} else {
								Map<String, Object> item = map;
								if (components.isEmpty() && (this.oldTags || !tag.isEmpty() || map.containsKey("Damage"))) {
									BiFunction<Map<String, Object>, Integer, Map<String, Object>> up = upgrader;
									Map<String, Object> upgraded = null;
									if (up != null) {
										try {
											upgraded = up.apply(map, this.version);
										} catch (RuntimeException var16) {
											upgraded = null;
										}
									}

									if (upgraded != null && !Snbt.string(upgraded, "id", "").isBlank()) {
										item = upgraded;
										if (upgraded.containsKey("\u0000left")) {
											this.notes
												.approx(
													"An item with data of a mod, written for an older version of the game (here: the item with what the game itself knows of that data; the mod's part is left behind)",
													where + ": " + id
												);
										}
									} else if (!tag.isEmpty() || Snbt.number(map, "Damage", 0L) != 0L) {
										this.notes.approx("An item with data of an older version of the game (here: the item by its id, without that data)", where + ": " + id);
									}
								}

								String nowId = Snbt.string(item, "id", id).trim();
								Map<String, Object> data = Snbt.map(item.get("components"));
								if (data.isEmpty()) {
									return new FtbItems.Item(nowId, nowId, count, false);
								} else {
									StringBuilder sb = new StringBuilder(nowId).append('[');
									boolean first = true;

									for (Entry<String, Object> e : data.entrySet()) {
										if (!first) {
											sb.append(',');
										}

										first = false;
										sb.append(e.getKey()).append('=').append(Snbt.write(e.getValue()));
									}

									sb.append(']');
									if (sb.length() > 1200) {
										this.notes.approx("An item with a great deal of data (here: the item without it)", where + ": " + nowId);
										return new FtbItems.Item(nowId, nowId, count, false);
									} else {
										return new FtbItems.Item(nowId, sb.toString(), count, false);
									}
								}
							}
					}
				}
			}
		}
	}

	private static Snbt.Num number(String text, String suffix) {
		try {
			long n = Long.parseLong(text.trim());
			return new Snbt.Num(n + suffix, (double)n);
		} catch (NumberFormatException var4) {
			return new Snbt.Num("0" + suffix, 0.0);
		}
	}

	String icon(@Nullable Object icon, String where) {
		Map<String, Object> map = Snbt.map(icon);
		if (Snbt.string(map, "id", "").equals("ftbquests:custom_icon")) {
			String texture = Snbt.string(Snbt.map(map.get("components")), "ftbquests:icon", Snbt.string(Snbt.map(map.get("tag")), "Icon", "")).trim();
			return texture.isEmpty() ? "" : "texture:" + texture(texture);
		} else {
			FtbItems.Item item = this.read(icon, where);
			if (item.isEmpty()) {
				return "";
			} else {
				String spec = item.filter() ? first(item.spec()) : item.full();
				if (!spec.startsWith("ftbfilter:") && !spec.startsWith("@")) {
					if (spec.length() > 200 && spec.indexOf(91) > 0) {
						spec = spec.substring(0, spec.indexOf(91));
					}

					return spec;
				} else {
					return "";
				}
			}
		}
	}

	private static String first(String spec) {
		int bar = spec.indexOf(" | ");
		return bar < 0 ? spec : spec.substring(0, bar);
	}

	static String texture(String ftb) {
		String t = ftb.trim();
		if (t.startsWith("item:")) {
			return t;
		} else {
			int colon = t.indexOf(58);
			String namespace = colon < 0 ? "minecraft" : t.substring(0, colon);
			String path = colon < 0 ? t : t.substring(colon + 1);
			if (!path.endsWith(".png")) {
				path = (path.startsWith("textures/") ? path : "textures/" + path) + ".png";
			}

			return namespace + ":" + path;
		}
	}

	String tag(String tag) {
		String t = tag.trim();
		return this.oldTags && t.startsWith("forge:") ? "c:" + t.substring("forge:".length()) : t;
	}

	String filter(String expression, String where) {
		if (expression.isEmpty()) {
			return "";
		} else {
			Set<String> out = new LinkedHashSet<>();
			if (this.plain(expression, out) && !out.isEmpty()) {
				return String.join(" | ", out);
			} else {
				this.notes
					.needs(
						"An item filter that asks more than items, tags and mods (kept as it is: it works where FTB Filter System is installed)", where + ": " + clip(expression)
					);
				return "ftbfilter:" + expression;
			}
		}
	}

	private boolean plain(String expression, Set<String> out) {
		String e = expression.trim();
		int open = e.indexOf(40);
		if (open > 0 && e.endsWith(")")) {
			String name = e.substring(0, open).trim().toLowerCase(Locale.ROOT);
			String arg = e.substring(open + 1, e.length() - 1).trim();
			String kind = name.indexOf(58) >= 0 ? name.substring(name.indexOf(58) + 1) : name;
			switch (kind) {
				case "item":
					if (!arg.isEmpty() && arg.indexOf(40) < 0) {
						out.add(arg);
						return true;
					}

					return false;
				case "item_tag":
					if (!arg.isEmpty() && arg.indexOf(40) < 0) {
						out.add("#" + this.tag(arg.startsWith("#") ? arg.substring(1) : arg));
						return true;
					}

					return false;
				case "mod":
					if (!arg.isEmpty() && arg.indexOf(40) < 0) {
						out.add("@" + arg);
						return true;
					}

					return false;
				case "or":
				case "and":
					List<String> children = children(arg);
					if (children != null && !children.isEmpty()) {
						if (kind.equals("and") && children.size() != 1) {
							return false;
						}

						for (String child : children) {
							if (!this.plain(child, out)) {
								return false;
							}
						}

						return true;
					}

					return false;
				default:
					return false;
			}
		} else {
			return false;
		}
	}

	@Nullable
	private static List<String> children(String arg) {
		List<String> out = new ArrayList<>();
		int depth = 0;
		int start = 0;

		for (int i = 0; i < arg.length(); i++) {
			char c = arg.charAt(i);
			if (c == '(') {
				depth++;
			} else if (c == ')') {
				if (--depth < 0) {
					return null;
				}

				if (depth == 0) {
					out.add(arg.substring(start, i + 1).trim());
					start = i + 1;
				}
			}
		}

		return depth == 0 && arg.substring(start).isBlank() ? out : null;
	}

	private String oldFilter(String id, Map<String, Object> map, String where) {
		Map<String, Object> tag = Snbt.map(map.get("tag"));
		String value = Snbt.string(tag, "value", "").trim();
		switch (id) {
			case "itemfilters:tag":
				return value.isEmpty() ? "" : "#" + this.tag(value);
			case "itemfilters:mod":
				return value.isEmpty() ? "" : "@" + value;
			case "itemfilters:or":
			case "itemfilters:and":
			case "itemfilters:xor":
			case "itemfilters:not":
				Set<String> any = new LinkedHashSet<>();

				for (Object o : Snbt.list(tag.get("items"))) {
					FtbItems.Item inner = this.read(o, where);
					if (!inner.isEmpty()) {
						any.add(inner.filter() ? inner.spec() : first(inner.spec()));
					}
				}

				if (any.isEmpty()) {
					return "";
				} else {
					if (!id.equals("itemfilters:or") && any.size() > 1 || id.equals("itemfilters:not")) {
						this.notes.approx("An item filter that is not a plain \"any of these\" (here: any of the items it names)", where + ": " + id);
					}

					return String.join(" | ", any);
				}
			case "itemfilters:filter":
				Map<String, Object> parent = Snbt.map(Snbt.map(map.get("ForgeCaps")).get("Parent"));
				String kind = Snbt.string(parent, "id", "");
				String data = Snbt.string(parent, "data", "").trim();
				if (kind.equals("ore") && !data.isEmpty()) {
					String now = oreTag(data);
					this.notes.approx("An ore dictionary name of 1.12 (here: the tag that took its place, as near as its name tells)", where + ": " + data + " -> #" + now);
					return "#" + now;
				}

				return "";
			default:
				return "";
		}
	}

	static String oreTag(String ore) {
		for (String[] p : ORE_PREFIXES) {
			if (ore.length() > p[0].length() && ore.startsWith(p[0]) && Character.isUpperCase(ore.charAt(p[0].length()))) {
				return "c:" + p[1] + "/" + snake(ore.substring(p[0].length()));
			}
		}

		return "c:" + snake(ore);
	}

	private static String snake(String camel) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < camel.length(); i++) {
			char c = camel.charAt(i);
			if (Character.isUpperCase(c)) {
				if (i > 0 && !Character.isUpperCase(camel.charAt(i - 1))) {
					sb.append('_');
				}

				sb.append(Character.toLowerCase(c));
			} else if (Character.isLetterOrDigit(c) || c == '_') {
				sb.append(c);
			}
		}

		return sb.toString();
	}

	private static String clip(String s) {
		return s.length() > 100 ? s.substring(0, 100) + "..." : s;
	}

	static record Item(String spec, String full, long count, boolean filter) {
		static final FtbItems.Item NONE = new FtbItems.Item("", "", 1L, false);

		boolean isEmpty() {
			return this.spec.isEmpty();
		}
	}

	interface Notes {
		void approx(String var1, String var2);

		void turn(String var1, String var2);

		void needs(String var1, String var2);
	}
}
