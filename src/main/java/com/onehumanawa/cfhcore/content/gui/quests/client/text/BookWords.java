package com.onehumanawa.cfhcore.content.gui.quests.client.text;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Ids;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import com.onehumanawa.cfhcore.content.gui.quests.model.Reward;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public final class BookWords {
	public static void apply(QuestBook book, @Nullable JsonObject words, List<Runnable> back) {
		if (words != null && words.size() != 0) {
			String title = string(words, "book.title");
			String subtitle = string(words, "book.subtitle");
			if (title != null) {
				String own = book.title;
				back.add(() -> book.title = own);
				book.title = title;
			}

			if (subtitle != null) {
				String own = book.subtitle;
				back.add(() -> book.subtitle = own);
				book.subtitle = subtitle;
			}

			for (QuestBook.Group g : book.groups) {
				String t = string(words, "group." + g.id + ".title");
				if (t != null) {
					String own = g.title;
					back.add(() -> g.title = own);
					g.title = t;
				}
			}

			for (Chapter c : book.chapters.values()) {
				apply(c, words, back);
			}
		}
	}

	public static void apply(Chapter c, @Nullable JsonObject words, List<Runnable> back) {
		if (words != null && words.size() != 0 && !c.readOnly) {
			String base = "chapter." + c.id + ".";
			String title = string(words, base + "title");
			String subtitle = string(words, base + "subtitle");
			if (title != null) {
				String own = c.title;
				back.add(() -> c.title = own);
				c.title = title;
			}

			if (subtitle != null) {
				String own = c.subtitle;
				back.add(() -> c.subtitle = own);
				c.subtitle = subtitle;
			}

			List<String> description = lines(words, base + "description");
			if (description != null) {
				List<String> own = c.description;
				back.add(() -> c.description = own);
				c.description = description;
			}

			for (Quest q : c.quests) {
				String key = Ids.key(c, q);
				String at = "quest." + key + ".";
				String qt = string(words, at + "title");
				String qs = string(words, at + "subtitle");
				if (qt != null) {
					String own = q.title;
					back.add(() -> q.title = own);
					q.title = qt;
				}

				if (qs != null) {
					String own = q.subtitle;
					back.add(() -> q.subtitle = own);
					q.subtitle = qs;
				}

				List<String> text = lines(words, at + "description");
				if (text != null) {
					List<String> own = q.description;
					back.add(() -> q.description = own);
					q.description = text;
				}

				for (int i = 0; i < q.tasks.size(); i++) {
					String t = string(words, "task." + key + "." + i + ".title");
					if (t != null) {
						Task task = q.tasks.get(i);
						String own = task.title;
						back.add(() -> task.title = own);
						task.title = t;
					}
				}

				for (int ix = 0; ix < q.rewards.size(); ix++) {
					String t = string(words, "reward." + key + "." + ix + ".title");
					if (t != null) {
						Reward reward = q.rewards.get(ix);
						String own = reward.title;
						back.add(() -> reward.title = own);
						reward.title = t;
					}
				}
			}
		}
	}

	@Nullable
	private static String string(JsonObject words, String key) {
		JsonElement e = words.get(key);
		return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
	}

	@Nullable
	private static List<String> lines(JsonObject words, String key) {
		JsonElement e = words.get(key);
		if (e == null) {
			return null;
		} else {
			List<String> out = new ArrayList<>();
			if (e.isJsonPrimitive()) {
				out.add(e.getAsString());
			} else if (e instanceof JsonArray) {
				for (JsonElement line : (JsonArray)e) {
					if (line.isJsonPrimitive()) {
						out.add(line.getAsString());
					}
				}
			}

			return out;
		}
	}

	private BookWords() {
	}
}
