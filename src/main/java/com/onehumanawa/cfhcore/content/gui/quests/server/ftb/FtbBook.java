package com.onehumanawa.cfhcore.content.gui.quests.server.ftb;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

public final class FtbBook {
	public static final int DATA_1_12 = 1343;
	public static final int DATA_1_20 = 3465;
	public final Path dir;
	public final FtbBook.Format format;
	public final Map<String, Object> data;
	public final List<Map<String, Object>> groups = new ArrayList<>();
	public final List<Map<String, Object>> chapters = new ArrayList<>();
	public final Map<String, Map<String, Object>> tables = new LinkedHashMap<>();
	public final Map<String, Map<String, Object>> lang = new TreeMap<>();
	public final List<String> unreadable = new ArrayList<>();
	public final String hash;
	private final MessageDigest digest;

	private FtbBook(Path dir, FtbBook.Format format) throws IOException {
		this.dir = dir;
		this.format = format;

		try {
			this.digest = MessageDigest.getInstance("SHA-256");
		} catch (Exception var8) {
			throw new IOException(var8);
		}

		this.data = this.file(dir.resolve(format == FtbBook.Format.LEGACY ? "file.snbt" : "data.snbt"));
		if (format == FtbBook.Format.LEGACY) {
			this.readLegacy();
		} else {
			this.readModern();
		}

		StringBuilder hex = new StringBuilder();

		for (byte b : this.digest.digest()) {
			hex.append(String.format("%02x", b));
		}

		this.hash = hex.toString();
	}

	@Nullable
	public static Path locate(Path folder) {
		for (Path dir : List.of(folder, folder.resolve("quests"), folder.resolve("normal"))) {
			if (Files.isDirectory(dir.resolve("chapters"))
				&& (Files.isRegularFile(dir.resolve("data.snbt")) || Files.isRegularFile(dir.resolve("file.snbt")) || hasChapters(dir))) {
				return dir;
			}
		}

		return null;
	}

	private static boolean hasChapters(Path dir) {
		try {
			boolean var2;
			try (Stream<Path> s = Files.list(dir.resolve("chapters"))) {
				var2 = s.anyMatch(p -> Files.isDirectory(p) || p.getFileName().toString().endsWith(".snbt"));
			}

			return var2;
		} catch (IOException var6) {
			return false;
		}
	}

	@Nullable
	public static FtbBook read(Path folder) throws IOException {
		Path dir = locate(folder);
		if (dir == null) {
			return null;
		} else {
			boolean legacy = Files.isRegularFile(dir.resolve("chapters").resolve("index.snbt"))
				|| !Files.isRegularFile(dir.resolve("data.snbt")) && Files.isRegularFile(dir.resolve("file.snbt"));
			boolean lang = Files.isDirectory(dir.resolve("lang"));
			return new FtbBook(dir, legacy ? FtbBook.Format.LEGACY : (lang ? FtbBook.Format.LANG : FtbBook.Format.INLINE));
		}
	}

	@Nullable
	public static String hashOf(Path folder) {
		try {
			FtbBook book = read(folder);
			return book == null ? null : book.hash;
		} catch (IOException var2) {
			return null;
		}
	}

	public static String quick(Path dir) {
		StringBuilder sb = new StringBuilder();

		try (Stream<Path> s = Files.walk(dir)) {
			for (Path p : s.filter(q -> Files.isRegularFile(q) && q.getFileName().toString().endsWith(".snbt")).sorted().toList()) {
				sb.append(dir.relativize(p).toString().replace('\\', '/'))
					.append(':')
					.append(Files.size(p))
					.append(':')
					.append(Files.getLastModifiedTime(p).toMillis())
					.append('\n');
			}
		} catch (IOException var7) {
			return "";
		}

		return Integer.toHexString(sb.toString().hashCode()) + "-" + sb.length();
	}

	public int itemVersion() {
		return this.format == FtbBook.Format.LEGACY ? 1343 : 3465;
	}

	public String locale() {
		return Snbt.string(this.data, "fallback_locale", "en_us").toLowerCase(Locale.ROOT);
	}

	private void readModern() throws IOException {
		for (Object g : Snbt.list(this.file(this.dir.resolve("chapter_groups.snbt")).get("chapter_groups"))) {
			Map<String, Object> group = Snbt.map(g);
			if (!id(group).isEmpty()) {
				this.groups.add(upper(group));
			}
		}

		for (Path p : this.snbtFiles(this.dir.resolve("chapters"))) {
			Map<String, Object> chapter = this.file(p);
			if (!chapter.isEmpty()) {
				upper(chapter);
				if (id(chapter).isEmpty()) {
					chapter.put("id", stem(p).toUpperCase(Locale.ROOT));
				}

				if (Snbt.string(chapter, "filename", "").isBlank()) {
					chapter.put("filename", stem(p));
				}

				this.chapters.add(chapter);
			}
		}

		for (Path px : this.snbtFiles(this.dir.resolve("reward_tables"))) {
			Map<String, Object> table = this.file(px);
			if (!table.isEmpty()) {
				upper(table);
				if (id(table).isEmpty()) {
					table.put("id", stem(px).toUpperCase(Locale.ROOT));
				}

				this.tables.put(id(table), table);
			}
		}

		Path langDir = this.dir.resolve("lang");
		if (Files.isDirectory(langDir)) {
			try (Stream<Path> s = Files.list(langDir)) {
				for (Path pxx : s.sorted().toList()) {
					String name = pxx.getFileName().toString().toLowerCase(Locale.ROOT);
					if (Files.isRegularFile(pxx) && name.endsWith(".snbt")) {
						this.lang.computeIfAbsent(name.substring(0, name.length() - 5), k -> new LinkedHashMap<>()).putAll(this.file(pxx));
					} else if (Files.isDirectory(pxx)) {
						Map<String, Object> words = this.lang.computeIfAbsent(name, k -> new LinkedHashMap<>());

						try (Stream<Path> files = Files.walk(pxx)) {
							for (Path f : files.filter(q -> q.getFileName().toString().endsWith(".snbt")).sorted().toList()) {
								words.putAll(this.file(f));
							}
						}
					}
				}
			}
		}
	}

	private void readLegacy() throws IOException {
		Path chapterDir = this.dir.resolve("chapters");
		List<String> order = new ArrayList<>();

		for (Object o : Snbt.list(this.file(chapterDir.resolve("index.snbt")).get("index"))) {
			order.add(String.valueOf(o).toLowerCase(Locale.ROOT));
		}

		List<Path> folders = new ArrayList<>();

		try (Stream<Path> s = Files.list(chapterDir)) {
			s.filter(x$0 -> Files.isDirectory(x$0)).sorted().forEach(folders::add);
		}

		folders.sort((a, b) -> Integer.compare(place(order, a), place(order, b)));
		int n = 0;

		for (Path folder : folders) {
			Map<String, Object> chapter = this.file(folder.resolve("chapter.snbt"));
			String name = folder.getFileName().toString();
			chapter.put("id", name.toUpperCase(Locale.ROOT));
			chapter.put("filename", name.toLowerCase(Locale.ROOT));
			chapter.put("order_index", new Snbt.Num(Integer.toString(n), (double)n));
			n++;
			if (chapter.get("description") != null) {
				chapter.put("subtitle", chapter.remove("description"));
			}

			List<Object> quests = new ArrayList<>();

			for (Path q : this.snbtFiles(folder)) {
				if (!q.getFileName().toString().equals("chapter.snbt")) {
					Map<String, Object> quest = this.file(q);
					if (!quest.isEmpty()) {
						quest.put("id", stem(q).toUpperCase(Locale.ROOT));
						Object subtitle = quest.remove("description");
						Object text = quest.remove("text");
						if (subtitle != null) {
							quest.put("subtitle", subtitle);
						}

						if (text != null) {
							quest.put("description", text);
						}

						if (quest.containsKey("hide") && !quest.containsKey("hide_until_deps_visible")) {
							quest.put("hide_until_deps_visible", quest.remove("hide"));
						}

						for (String list : List.of("tasks", "rewards")) {
							for (Object o : Snbt.list(quest.get(list))) {
								Map<String, Object> entry = Snbt.map(o);
								if (entry.containsKey("uid") && !entry.containsKey("id")) {
									entry.put("id", entry.remove("uid"));
								}
							}
						}

						upper(quest);
						quests.add(quest);
					}
				}
			}

			chapter.put("quests", quests);
			this.chapters.add(chapter);
		}

		Path tableDir = this.dir.resolve("reward_tables");

		for (Path p : this.snbtFiles(tableDir)) {
			if (!p.getFileName().toString().equals("index.snbt")) {
				Map<String, Object> table = this.file(p);
				if (!table.isEmpty()) {
					table.put("id", stem(p).toUpperCase(Locale.ROOT));
					this.tables.put(id(table), table);
				}
			}
		}
	}

	private static int place(List<String> order, Path folder) {
		int at = order.indexOf(folder.getFileName().toString().toLowerCase(Locale.ROOT));
		return at < 0 ? Integer.MAX_VALUE : at;
	}

	private static String stem(Path file) {
		String name = file.getFileName().toString();
		return name.endsWith(".snbt") ? name.substring(0, name.length() - 5) : name;
	}

	static String id(Map<String, Object> map) {
		return Snbt.string(map, "id", "").toUpperCase(Locale.ROOT);
	}

	private static Map<String, Object> upper(Map<String, Object> map) {
		if (map.get("id") instanceof String s) {
			map.put("id", s.toUpperCase(Locale.ROOT));
		}

		for (String list : List.of("quests", "quest_links", "tasks", "rewards", "images")) {
			for (Object o : Snbt.list(map.get(list))) {
				if (o instanceof Map) {
					upper((Map<String, Object>)o);
				}
			}
		}

		return map;
	}

	private List<Path> snbtFiles(Path folder) throws IOException {
		if (!Files.isDirectory(folder)) {
			return List.of();
		} else {
			List var3;
			try (Stream<Path> s = Files.list(folder)) {
				var3 = s.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".snbt")).sorted().toList();
			}

			return var3;
		}
	}

	private Map<String, Object> file(Path path) {
		if (!Files.isRegularFile(path)) {
			return new LinkedHashMap<>();
		} else {
			try {
				byte[] bytes = Files.readAllBytes(path);
				this.digest.update(this.dir.relativize(path).toString().replace('\\', '/').getBytes(StandardCharsets.UTF_8));
				this.digest.update(bytes);
				Object parsed = Snbt.parse(new String(bytes, StandardCharsets.UTF_8));
				return parsed instanceof Map ? new LinkedHashMap<>(Snbt.map(parsed)) : new LinkedHashMap<>();
			} catch (Exception var4) {
				this.unreadable.add(this.dir.relativize(path) + ": " + var4.getMessage());
				return new LinkedHashMap<>();
			}
		}
	}

	public static enum Format {
		LANG("1.21 (words in the lang folder)"),
		INLINE("1.16 to 1.20 (words in the chapter files)"),
		LEGACY("1.12 (a folder per chapter)");

		public final String words;

		private Format(final String words) {
			this.words = words;
		}
	}
}
