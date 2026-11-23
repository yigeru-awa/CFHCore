package com.onehumanawa.cfhcore.content.gui.quests.server.adv;

import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import com.onehumanawa.cfhcore.content.gui.quests.model.BookJson;
import com.onehumanawa.cfhcore.content.gui.quests.model.Ids;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

final class AdvStore {
	static Path dir() {
		return JsonConfig.dir().resolve("quests-adv");
	}

	private static Path file(String chapterId) {
		return dir().resolve(chapterId + ".json");
	}

	@Nullable
	static AdvStore.Kept read(String chapterId) {
		if (!Ids.valid(chapterId)) {
			return null;
		} else {
			String text = text(chapterId);
			if (text == null) {
				return null;
			} else {
				try {
					AdvStore.Kept kept = (AdvStore.Kept)BookJson.GSON.fromJson(text, AdvStore.Kept.class);
					if (kept == null) {
						return null;
					} else {
						if (kept.root == null) {
							kept.root = "";
						}

						if (kept.options == null) {
							kept.options = new AdvOptions();
						}

						kept.options.sanitize();
						return kept;
					}
				} catch (RuntimeException var3) {
					CFHGuiQuests.LOGGER.warn("[CFHGui Quests] {} cannot be read and is left aside: {}", file(chapterId), var3.toString());
					return null;
				}
			}
		}
	}

	@Nullable
	static String text(String chapterId) {
		if (!Ids.valid(chapterId)) {
			return null;
		} else {
			Path file = file(chapterId);
			if (!Files.isRegularFile(file)) {
				return null;
			} else {
				try {
					return Files.readString(file, StandardCharsets.UTF_8);
				} catch (IOException var3) {
					return null;
				}
			}
		}
	}

	static void write(String chapterId, AdvStore.Kept kept) {
		restore(chapterId, BookJson.PRETTY.toJson(kept));
	}

	static void restore(String chapterId, @Nullable String text) {
		if (Ids.valid(chapterId)) {
			Path file = file(chapterId);

			try {
				if (text == null) {
					Files.deleteIfExists(file);
					return;
				}

				Files.createDirectories(file.getParent());
				Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
				Files.writeString(tmp, text.endsWith("\n") ? text : text + "\n", StandardCharsets.UTF_8);
				Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (IOException var4) {
				CFHGuiQuests.LOGGER.warn("[CFHGui Quests] {} could not be written: {}", file, var4.toString());
			}
		}
	}

	static Map<String, AdvStore.Kept> all() {
		Map<String, AdvStore.Kept> out = new LinkedHashMap<>();
		Path dir = dir();
		if (!Files.isDirectory(dir)) {
			return out;
		} else {
			try (Stream<Path> files = Files.list(dir)) {
				for (Path p : files.sorted().toList()) {
					String name = p.getFileName().toString();
					if (name.endsWith(".json")) {
						String id = name.substring(0, name.length() - ".json".length());
						AdvStore.Kept kept = read(id);
						if (kept != null) {
							out.put(id, kept);
						}
					}
				}
			} catch (IOException var10) {
				CFHGuiQuests.LOGGER.warn("[CFHGui Quests] {} cannot be listed: {}", dir, var10.toString());
			}

			return out;
		}
	}

	private AdvStore() {
	}

	static final class Kept {
		String root = "";
		long at;
		AdvOptions options = new AdvOptions();
		@Nullable
		JsonObject chapter;
	}
}
