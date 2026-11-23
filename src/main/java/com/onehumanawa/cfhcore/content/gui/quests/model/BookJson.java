package com.onehumanawa.cfhcore.content.gui.quests.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

public final class BookJson {
	private static final TypeAdapterFactory ENUMS = new TypeAdapterFactory() {
		public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
			Class<? super T> raw = type.getRawType();
			if (!raw.isEnum()) {
				return null;
			} else {
				final Map<String, T> byName = new HashMap<>();

				for (Object c : raw.getEnumConstants()) {
					byName.put(((Enum)c).name().toLowerCase(Locale.ROOT), (T)c);
				}

				return new TypeAdapter<T>() {
					public void write(JsonWriter out, T value) throws IOException {
						if (value == null) {
							out.nullValue();
						} else {
							out.value(((Enum)value).name().toLowerCase(Locale.ROOT));
						}
					}

					public T read(JsonReader in) throws IOException {
						if (in.peek() == JsonToken.NULL) {
							in.nextNull();
							return null;
						} else {
							return byName.get(in.nextString().trim().toLowerCase(Locale.ROOT));
						}
					}
				};
			}
		}
	};
	public static final Gson GSON = new GsonBuilder().registerTypeAdapterFactory(ENUMS).disableHtmlEscaping().create();
	public static final Gson PRETTY = new GsonBuilder().registerTypeAdapterFactory(ENUMS).disableHtmlEscaping().setPrettyPrinting().create();
	private static final String BUNDLED = "/cfhgui_quests/examples/";

	public static boolean exists(Path dir) {
		return Files.isRegularFile(dir.resolve("book.json"));
	}

	public static QuestBook load(Path dir) {
		QuestBook book = null;
		Path head = dir.resolve("book.json");
		if (Files.isRegularFile(head)) {
			try (Reader r = Files.newBufferedReader(head, StandardCharsets.UTF_8)) {
				book = (QuestBook)GSON.fromJson(r, QuestBook.class);
			} catch (Exception var19) {
				CFHGuiQuests.LOGGER.error("[CFHGui Quests] {} cannot be read: {}", head, var19.toString());
			}
		}

		if (book == null) {
			book = new QuestBook();
		}

		book.sanitize();
		Path chapters = dir.resolve("chapters");
		if (Files.isDirectory(chapters)) {
			List<Path> files = new ArrayList<>();

			try (Stream<Path> s = Files.list(chapters)) {
				s.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().forEach(files::add);
			} catch (IOException var17) {
				CFHGuiQuests.LOGGER.error("[CFHGui Quests] {} cannot be listed: {}", chapters, var17.toString());
			}

			for (Path file : files) {
				String name = file.getFileName().toString();

				try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
					Chapter c = (Chapter)GSON.fromJson(r, Chapter.class);
					if (c != null) {
						c.id = name.substring(0, name.length() - ".json".length());
						if (!Ids.valid(c.id)) {
							CFHGuiQuests.LOGGER.warn("[CFHGui Quests] {} is skipped: a chapter's file name has lower case letters, digits and _ - . only", file);
						} else {
							book.chapters.put(c.id, c.sanitize());
						}
					}
				} catch (Exception var15) {
					CFHGuiQuests.LOGGER.error("[CFHGui Quests] the chapter {} cannot be read and is left out: {}", file, var15.toString());
				}
			}
		}

		return book;
	}

	public static void saveHead(Path dir, QuestBook book) throws IOException {
		write(dir.resolve("book.json"), PRETTY.toJson(book));
	}

	public static void saveChapter(Path dir, Chapter chapter) throws IOException {
		write(dir.resolve("chapters").resolve(chapter.id + ".json"), PRETTY.toJson(chapter));
	}

	public static void deleteChapter(Path dir, String chapterId) throws IOException {
		if (Ids.valid(chapterId)) {
			Files.deleteIfExists(dir.resolve("chapters").resolve(chapterId + ".json"));
		}
	}

	public static void save(Path dir, QuestBook book) throws IOException {
		saveHead(dir, book);

		for (Chapter c : book.chapters.values()) {
			saveChapter(dir, c);
		}
	}

	private static void write(Path file, String json) throws IOException {
		Files.createDirectories(file.getParent());
		Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
		Files.writeString(tmp, json + "\n", StandardCharsets.UTF_8);
		Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	}

	public static QuestBook loadBundled() {
		QuestBook book = null;
		BookJson.BundledHead head = null;
		String json = resource("/cfhgui_quests/examples/book.json");
		if (json != null) {
			try {
				book = (QuestBook)GSON.fromJson(json, QuestBook.class);
				head = (BookJson.BundledHead)GSON.fromJson(json, BookJson.BundledHead.class);
			} catch (Exception var8) {
				CFHGuiQuests.LOGGER.error("[CFHGui Quests] the bundled book cannot be read: {}", var8.toString());
			}
		}

		if (book == null) {
			book = new QuestBook();
		}

		book.sanitize();
		if (head != null && head.chapters != null) {
			for (String id : head.chapters) {
				if (Ids.valid(id)) {
					String text = resource("/cfhgui_quests/examples/chapters/" + id + ".json");
					if (text == null) {
						CFHGuiQuests.LOGGER.warn("[CFHGui Quests] the bundled chapter {} is missing", id);
					} else {
						try {
							Chapter c = (Chapter)GSON.fromJson(text, Chapter.class);
							if (c != null) {
								c.id = id;
								book.chapters.put(id, c.sanitize());
							}
						} catch (Exception var7) {
							CFHGuiQuests.LOGGER.error("[CFHGui Quests] the bundled chapter {} cannot be read: {}", id, var7.toString());
						}
					}
				}
			}
		}

		return book;
	}

	@Nullable
	private static String resource(String path) {
		try {
			String var12;
			try (InputStream in = BookJson.class.getResourceAsStream(path)) {
				if (in == null) {
					return null;
				}

				try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
					StringBuilder sb = new StringBuilder();
					char[] buffer = new char[8192];

					int n;
					while ((n = r.read(buffer)) > 0) {
						sb.append(buffer, 0, n);
					}

					var12 = sb.toString();
				}
			}

			return var12;
		} catch (IOException var10) {
			return null;
		}
	}

	public static Chapter copy(Chapter chapter) {
		Chapter c = (Chapter)GSON.fromJson(GSON.toJson(chapter), Chapter.class);
		c.id = chapter.id;
		return c.sanitize();
	}

	public static Quest copy(Quest quest) {
		Quest q = (Quest)GSON.fromJson(GSON.toJson(quest), Quest.class);
		q.sanitize();
		return q;
	}

	private BookJson() {
	}

	private static final class BundledHead {
		List<String> chapters = new ArrayList<>();
	}
}
