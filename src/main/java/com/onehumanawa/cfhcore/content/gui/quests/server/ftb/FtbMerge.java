package com.onehumanawa.cfhcore.content.gui.quests.server.ftb;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.content.gui.quests.model.BookJson;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

final class FtbMerge {
	static QuestBook merge(QuestBook base, QuestBook current, QuestBook next, FtbMerge.Tally tally) {
		JsonObject head = fields(head(base), head(current), head(next), tally, "the book");
		QuestBook out = (QuestBook)BookJson.GSON.fromJson(head, QuestBook.class);
		Set<String> ids = new LinkedHashSet<>(next.chapters.keySet());
		ids.addAll(current.chapters.keySet());

		for (String id : ids) {
			Chapter b = base.chapter(id);
			Chapter c = current.chapter(id);
			Chapter n = next.chapter(id);
			JsonObject merged;
			if (c == null) {
				if (b != null) {
					tally.notes.add("the chapter " + id + " was deleted in CFHGui's book and is not brought back");
					continue;
				}

				merged = FtbImport.compactTree(n);
				tally.chaptersAdded++;
			} else if (n == null) {
				if (b != null && FtbImport.compactTree(b).equals(FtbImport.compactTree(c))) {
					tally.chaptersRemoved++;
					continue;
				}

				if (b != null) {
					tally.notes.add("the chapter " + id + " is gone from the FTB book; it was edited in CFHGui's book and is kept");
				}

				merged = FtbImport.compactTree(c);
			} else {
				merged = chapter(b == null ? null : FtbImport.compactTree(b), FtbImport.compactTree(c), FtbImport.compactTree(n), id, tally);
			}

			Chapter made = (Chapter)BookJson.GSON.fromJson(merged, Chapter.class);
			made.id = id;
			out.chapters.put(id, made.sanitize());
		}

		return out.sanitize();
	}

	private static JsonObject head(QuestBook book) {
		JsonObject o = BookJson.GSON.toJsonTree(book).getAsJsonObject();
		o.remove("revision");
		return o;
	}

	private static JsonObject chapter(@Nullable JsonObject b, JsonObject c, JsonObject n, String id, FtbMerge.Tally tally) {
		JsonObject bq = strip(b, "quests");
		JsonObject cq = strip(c, "quests");
		JsonObject nq = strip(n, "quests");
		JsonObject out = fields(b, c, n, tally, id);
		Map<String, JsonObject> base = byId(bq);
		Map<String, JsonObject> current = byId(cq);
		Map<String, JsonObject> next = byId(nq);
		Set<String> ids = new LinkedHashSet<>(next.keySet());
		ids.addAll(current.keySet());
		JsonArray quests = new JsonArray();

		for (String q : ids) {
			JsonObject qb = base.get(q);
			JsonObject qc = current.get(q);
			JsonObject qn = next.get(q);
			String where = id + "/" + q;
			if (qc == null) {
				if (qb != null) {
					tally.notes.add("the quest " + where + " was deleted in CFHGui's book and is not brought back");
				} else {
					quests.add(qn);
					tally.questsAdded++;
				}
			} else if (qn == null) {
				if (qb != null && qb.equals(qc)) {
					tally.questsRemoved++;
				} else {
					if (qb != null) {
						tally.notes.add("the quest " + where + " is gone from the FTB book; it was edited in CFHGui's book and is kept");
					}

					quests.add(qc);
				}
			} else {
				quests.add(fields(qb, qc, qn, tally, where));
			}
		}

		if (!quests.isEmpty()) {
			out.add("quests", quests);
		}

		return out;
	}

	private static JsonObject strip(@Nullable JsonObject o, String field) {
		JsonObject holder = new JsonObject();
		if (o != null && o.has(field)) {
			holder.add(field, o.remove(field));
		}

		return holder;
	}

	private static Map<String, JsonObject> byId(JsonObject holder) {
		Map<String, JsonObject> out = new LinkedHashMap<>();
		JsonElement list = holder.get("quests");
		if (list instanceof JsonArray) {
			for (JsonElement e : (JsonArray)list) {
				if (e.isJsonObject() && e.getAsJsonObject().has("id")) {
					out.putIfAbsent(e.getAsJsonObject().get("id").getAsString(), e.getAsJsonObject());
				}
			}
		}

		return out;
	}

	private static JsonObject fields(@Nullable JsonObject b, JsonObject c, JsonObject n, FtbMerge.Tally tally, String where) {
		JsonObject out = new JsonObject();
		Set<String> keys = new LinkedHashSet<>(n.keySet());
		keys.addAll(c.keySet());

		for (String key : keys) {
			JsonElement vb = b == null ? null : b.get(key);
			JsonElement vc = c.get(key);
			JsonElement vn = n.get(key);
			boolean edited = b == null ? !Objects.equals(vc, vn) : !Objects.equals(vc, vb);
			JsonElement take = edited ? vc : vn;
			if (!Objects.equals(vc, vn) && !key.equals("id")) {
				if (edited) {
					tally.fieldsKept++;
				} else {
					tally.fieldsUpdated++;
				}
			}

			if (take != null) {
				out.add(key, take);
			}
		}

		return out;
	}

	private FtbMerge() {
	}

	static final class Tally {
		int chaptersAdded;
		int chaptersRemoved;
		int questsAdded;
		int questsRemoved;
		int fieldsUpdated;
		int fieldsKept;
		final List<String> notes = new ArrayList<>();

		boolean any() {
			return this.chaptersAdded + this.chaptersRemoved + this.questsAdded + this.questsRemoved + this.fieldsUpdated + this.fieldsKept > 0 || !this.notes.isEmpty();
		}
	}
}
