package com.onehumanawa.cfhcore.content.gui.quests.server.adv;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onehumanawa.cfhcore.content.gui.quests.model.BookJson;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

final class AdvMerge {
	private static final Set<String> PLACE = Set.of("x", "y");
	private static final float TOUCH = 0.6F;

	static Chapter merge(@Nullable JsonObject base, Chapter current, Chapter next, AdvAccount account) {
		JsonObject b = base == null ? null : base.deepCopy();
		JsonObject c = tree(current);
		JsonObject n = tree(next);
		Map<String, JsonObject> baseQuests = quests(b);
		Map<String, JsonObject> currentQuests = quests(c);
		Map<String, JsonObject> nextQuests = quests(n);
		JsonObject out = fields(b, c, n, account, "", Set.of());
		Set<String> ids = new LinkedHashSet<>(nextQuests.keySet());
		ids.addAll(currentQuests.keySet());
		JsonArray quests = new JsonArray();
		List<JsonObject> added = new ArrayList<>();

		for (String id : ids) {
			JsonObject qb = baseQuests.get(id);
			JsonObject qc = currentQuests.get(id);
			JsonObject qn = nextQuests.get(id);
			if (qc == null) {
				if (qb == null) {
					quests.add(qn);
					added.add(qn);
					account.added++;
					account.change("+" + id);
				}
			} else if (qn == null) {
				if (qb != null && qb.equals(qc)) {
					account.removed++;
					account.change("-" + id);
				} else {
					quests.add(qc);
				}
			} else {
				quests.add(fields(qb, qc, qn, account, id + ".", PLACE));
			}
		}

		for (JsonObject fresh : added) {
			for (int guard = 0; guard < 64 && taken(quests, fresh); guard++) {
				fresh.addProperty("y", number(fresh, "y") + 0.75F);
			}
		}

		out.add("quests", quests);
		Chapter merged = (Chapter)BookJson.GSON.fromJson(out, Chapter.class);
		merged.id = current.id;
		return merged.sanitize();
	}

	static JsonObject tree(Chapter chapter) {
		return JsonParser.parseString(BookJson.GSON.toJson(chapter)).getAsJsonObject();
	}

	private static Map<String, JsonObject> quests(@Nullable JsonObject chapter) {
		Map<String, JsonObject> out = new LinkedHashMap<>();
		if (chapter == null) {
			return out;
		} else {
			JsonElement list = chapter.remove("quests");
			if (list instanceof JsonArray) {
				for (JsonElement e : (JsonArray)list) {
					if (e.isJsonObject() && e.getAsJsonObject().has("id")) {
						out.putIfAbsent(e.getAsJsonObject().get("id").getAsString(), e.getAsJsonObject());
					}
				}
			}

			return out;
		}
	}

	private static float number(JsonObject quest, String key) {
		JsonElement v = quest.get(key);
		return v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber() ? v.getAsFloat() : 0.0F;
	}

	private static boolean taken(JsonArray quests, JsonObject quest) {
		float x = number(quest, "x");
		float y = number(quest, "y");

		for (JsonElement e : quests) {
			if (e != quest && e.isJsonObject() && Math.abs(number(e.getAsJsonObject(), "x") - x) < 0.6F && Math.abs(number(e.getAsJsonObject(), "y") - y) < 0.6F) {
				return true;
			}
		}

		return false;
	}

	private static JsonObject fields(@Nullable JsonObject b, JsonObject c, JsonObject n, AdvAccount account, String where, Set<String> stay) {
		JsonObject out = new JsonObject();
		Set<String> keys = new LinkedHashSet<>(n.keySet());
		keys.addAll(c.keySet());

		for (String key : keys) {
			JsonElement vb = b == null ? null : b.get(key);
			JsonElement vc = c.get(key);
			JsonElement vn = n.get(key);
			if (stay.contains(key)) {
				if (vc != null) {
					out.add(key, vc);
				}
			} else {
				boolean edited = b == null ? !Objects.equals(vc, vn) : !Objects.equals(vc, vb);
				JsonElement take = edited ? vc : vn;
				if (!Objects.equals(vc, vn) && !key.equals("id")) {
					if (edited) {
						account.edited++;
					} else {
						account.updated++;
						account.change(where + key);
					}
				}

				if (take != null) {
					out.add(key, take);
				}
			}
		}

		return out;
	}

	private AdvMerge() {
	}
}
