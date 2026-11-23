package com.onehumanawa.cfhcore.content.gui.quests.server.adv;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class AdvAccount {
	public static final String PARTLY = "partly";
	public static final String KEPT = "kept";
	public static final String OFF = "off";
	public static final String TRIGGER = "trigger";
	public static final String REQUIREMENTS = "requirements";
	public static final String TOO_MANY = "too_many";
	public static final String UNREADABLE = "unreadable";
	public static final String NO_CRITERIA = "no_criteria";
	public static final String NEW = "new";
	public static final String REPLACED = "replaced";
	public static final String MERGED = "merged";
	public static final String COPIED = "copy";
	public String root = "";
	public String chapter = "";
	public String title = "";
	public int quests;
	public int translated;
	public int partly;
	public int kept;
	public int hidden;
	public int skipped;
	public int tasks;
	public int rewards;
	public Map<String, String> marks = new LinkedHashMap<>();
	public Map<String, String> why = new LinkedHashMap<>();
	public Map<String, Integer> triggers = new LinkedHashMap<>();
	public String mode = "new";
	public int added;
	public int updated;
	public int edited;
	public int removed;
	public List<String> changes = new ArrayList<>();

	void change(String what) {
		if (this.changes.size() < 12) {
			this.changes.add(what);
		}
	}

	void mark(String quest, String how, String reason) {
		this.marks.put(quest, how);
		this.why.put(quest, reason);
	}

	void trigger(String id) {
		this.triggers.merge(id, 1, Integer::sum);
	}

	public String triggersInWords(int most) {
		StringBuilder sb = new StringBuilder();
		List<Entry<String, Integer>> all = new ArrayList<>(this.triggers.entrySet());
		all.sort((a, b) -> b.getValue() - a.getValue());

		for (int i = 0; i < all.size() && i < most; i++) {
			if (sb.length() > 0) {
				sb.append(", ");
			}

			String id = all.get(i).getKey();
			sb.append(id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id).append(" ×").append(all.get(i).getValue());
		}

		if (all.size() > most) {
			sb.append(", +").append(all.size() - most);
		}

		return sb.toString();
	}
}
