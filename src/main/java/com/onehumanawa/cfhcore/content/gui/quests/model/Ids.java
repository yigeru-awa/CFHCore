package com.onehumanawa.cfhcore.content.gui.quests.model;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public final class Ids {
	public static String key(String chapterId, String questId) {
		return chapterId + "/" + questId;
	}

	public static String key(Chapter chapter, Quest quest) {
		return chapter.id + "/" + quest.id;
	}

	public static String resolve(String chapterId, String reference) {
		return reference.indexOf(47) >= 0 ? reference : chapterId + "/" + reference;
	}

	public static String chapterOf(@Nullable String key) {
		if (key == null) {
			return "";
		} else {
			int slash = key.indexOf(47);
			return slash < 0 ? "" : key.substring(0, slash);
		}
	}

	public static String questOf(@Nullable String key) {
		if (key == null) {
			return "";
		} else {
			int slash = key.indexOf(47);
			return slash < 0 ? key : key.substring(slash + 1);
		}
	}

	public static boolean valid(@Nullable String id) {
		if (id != null && !id.isEmpty() && id.length() <= 64) {
			for (int i = 0; i < id.length(); i++) {
				char c = id.charAt(i);
				if ((c < 'a' || c > 'z') && (c < '0' || c > '9') && c != '_' && c != '-' && c != '.') {
					return false;
				}
			}

			return true;
		} else {
			return false;
		}
	}

	public static String slug(@Nullable String title, String fallback) {
		if (title == null) {
			return fallback;
		} else {
			StringBuilder sb = new StringBuilder();

			for (char raw : title.trim().toLowerCase(Locale.ROOT).toCharArray()) {
				if ((raw < 'a' || raw > 'z') && (raw < '0' || raw > '9')) {
					if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '_') {
						sb.append('_');
					}
				} else {
					sb.append(raw);
				}
			}

			while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '_') {
				sb.setLength(sb.length() - 1);
			}

			if (sb.length() > 48) {
				sb.setLength(48);
			}

			return sb.length() == 0 ? fallback : sb.toString();
		}
	}

	private Ids() {
	}
}
