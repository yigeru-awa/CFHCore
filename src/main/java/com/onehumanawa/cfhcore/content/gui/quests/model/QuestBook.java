package com.onehumanawa.cfhcore.content.gui.quests.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public final class QuestBook {
	public String title = "Quests";
	public String subtitle = "";
	public List<QuestBook.Group> groups = new ArrayList<>();
	public boolean advancements = true;
	public String source = "";
	public List<Reward> emergency = new ArrayList<>();
	public int emergencyCooldownSeconds = 300;
	public boolean closed;
	public long revision;
	public transient boolean showUnlisted;
	public transient Map<String, Chapter> chapters = new LinkedHashMap<>();
	public transient List<QuestBook.Group> clientGroups = new ArrayList<>();

	@Nullable
	public Chapter chapter(@Nullable String id) {
		return id == null ? null : this.chapters.get(id);
	}

	@Nullable
	public QuestBook.Group group(@Nullable String id) {
		if (id == null) {
			return null;
		} else {
			for (QuestBook.Group g : this.groups) {
				if (id.equals(g.id)) {
					return g;
				}
			}

			for (QuestBook.Group gx : this.clientGroups) {
				if (id.equals(gx.id)) {
					return gx;
				}
			}

			return null;
		}
	}

	@Nullable
	public Quest quest(@Nullable String key) {
		Chapter c = this.chapter(Ids.chapterOf(key));
		return c == null ? null : c.quest(Ids.questOf(key));
	}

	public List<Chapter> topChapters() {
		List<Chapter> out = new ArrayList<>();

		for (Chapter c : this.chapters.values()) {
			if (!c.isSubtree() && (!c.unlisted || this.showUnlisted)) {
				out.add(c);
			}
		}

		out.sort(Comparator.<Chapter>comparingInt(cx -> this.groupIndex(cx.group)).thenComparingInt(cx -> cx.order).thenComparing(cx -> cx.title));
		return out;
	}

	public List<Chapter> subtrees(String chapterId) {
		List<Chapter> out = new ArrayList<>();
		Chapter parent = this.chapter(chapterId);
		if (parent != null) {
			for (Quest q : parent.quests) {
				Chapter sub = q.isGateway() ? this.chapter(q.subtree) : null;
				if (sub != null && !out.contains(sub)) {
					out.add(sub);
				}
			}
		}

		for (Chapter c : this.chapters.values()) {
			if (chapterId.equals(c.parent) && !out.contains(c)) {
				out.add(c);
			}
		}

		return out;
	}

	public int groupIndex(@Nullable String groupId) {
		if (groupId != null && !groupId.isEmpty()) {
			for (int i = 0; i < this.groups.size(); i++) {
				if (groupId.equals(this.groups.get(i).id)) {
					return i;
				}
			}

			for (int ix = 0; ix < this.clientGroups.size(); ix++) {
				if (groupId.equals(this.clientGroups.get(ix).id)) {
					return this.groups.size() + 1 + ix;
				}
			}

			return this.groups.size();
		} else {
			return -1;
		}
	}

	public int questCount() {
		int n = 0;

		for (Chapter c : this.chapters.values()) {
			n += c.quests.size();
		}

		return n;
	}

	public QuestBook sanitize() {
		if (this.title == null || this.title.isBlank()) {
			this.title = "Quests";
		}

		if (this.subtitle == null) {
			this.subtitle = "";
		}

		if (this.groups == null) {
			this.groups = new ArrayList<>();
		}

		this.groups.removeIf(g -> g == null || g.id == null || g.id.isBlank());

		for (QuestBook.Group g : this.groups) {
			if (g.title == null || g.title.isBlank()) {
				g.title = g.id;
			}

			if (g.icon == null) {
				g.icon = "";
			}
		}

		if (this.chapters == null) {
			this.chapters = new LinkedHashMap<>();
		}

		if (this.clientGroups == null) {
			this.clientGroups = new ArrayList<>();
		}

		if (this.source == null) {
			this.source = "";
		}

		if (this.emergency == null) {
			this.emergency = new ArrayList<>();
		}

		this.emergency.removeIf(rx -> rx == null);

		for (Reward r : this.emergency) {
			r.sanitize();
		}

		if (this.emergencyCooldownSeconds < 0) {
			this.emergencyCooldownSeconds = 0;
		}

		return this;
	}

	public static final class Group {
		public String id = "";
		public String title = "";
		public String icon = "";
	}
}
