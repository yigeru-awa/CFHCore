package com.onehumanawa.cfhcore.content.gui.quests.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

public final class Chapter {
	public String id = "";
	public String title = "";
	public String subtitle = "";
	public List<String> description = new ArrayList<>();
	public String icon = "minecraft:book";
	public String group = "";
	public int order;
	public String accent = "";
	public String backdrop = "";
	public String parent = "";
	public Quest.Sync sync = Quest.Sync.TEAM;
	public Quest.Shape shape = Quest.Shape.CIRCLE;
	public List<String> requires = new ArrayList<>();
	public boolean hidden;
	public boolean unlisted;
	public String focus = "";
	public List<Quest> quests = new ArrayList<>();
	public List<Chapter.Decoration> decorations = new ArrayList<>();
	public transient boolean readOnly;

	public boolean isSubtree() {
		return this.parent != null && !this.parent.isEmpty();
	}

	@Nullable
	public Quest quest(@Nullable String questId) {
		if (questId == null) {
			return null;
		} else {
			for (Quest q : this.quests) {
				if (questId.equals(q.id)) {
					return q;
				}
			}

			return null;
		}
	}

	public Chapter sanitize() {
		if (this.id == null) {
			this.id = "";
		}

		if (this.title == null || this.title.isBlank()) {
			this.title = this.id;
		}

		if (this.subtitle == null) {
			this.subtitle = "";
		}

		if (this.description == null) {
			this.description = new ArrayList<>();
		}

		this.description.removeIf(s -> s == null);
		if (this.icon == null || this.icon.isBlank()) {
			this.icon = "minecraft:book";
		}

		if (this.group == null) {
			this.group = "";
		}

		if (this.accent == null) {
			this.accent = "";
		}

		if (this.backdrop == null) {
			this.backdrop = "";
		}

		if (this.parent == null) {
			this.parent = "";
		}

		if (this.sync == null) {
			this.sync = Quest.Sync.TEAM;
		}

		if (this.shape == null) {
			this.shape = Quest.Shape.CIRCLE;
		}

		if (this.requires == null) {
			this.requires = new ArrayList<>();
		}

		this.requires.removeIf(s -> s == null || s.isBlank());
		if (this.quests == null) {
			this.quests = new ArrayList<>();
		}

		this.quests.removeIf(q -> q == null);
		int n = 0;
		Set<String> seen = new HashSet<>();

		for (Quest q : this.quests) {
			q.sanitize();
			if (q.id.isBlank() || !seen.add(q.id)) {
				String made;
				do {
					made = "q" + ++n;
				} while (seen.contains(made));

				q.id = made;
				seen.add(made);
			}
		}

		if (this.decorations == null) {
			this.decorations = new ArrayList<>();
		}

		this.decorations.removeIf(dx -> dx == null);

		for (Chapter.Decoration d : this.decorations) {
			if (d.type == null) {
				d.type = "label";
			}

			if (d.text == null) {
				d.text = "";
			}

			if (d.image == null) {
				d.image = "";
			}

			if (d.color == null) {
				d.color = "";
			}

			if (d.scale <= 0.0F) {
				d.scale = 1.0F;
			}

			if (d.hover == null) {
				d.hover = new ArrayList<>();
			}

			d.hover.removeIf(s -> s == null);
			if (d.link == null) {
				d.link = "";
			}

			if (d.requires == null) {
				d.requires = "";
			}
		}

		if (this.focus == null) {
			this.focus = "";
		}

		return this;
	}

	public static final class Decoration {
		public String type = "label";
		public String text = "";
		public String image = "";
		public float x;
		public float y;
		public float w = 2.0F;
		public float h = 2.0F;
		public float scale = 1.0F;
		public String color = "";
		public boolean heading;
		public float alpha = 1.0F;
		public float rotation;
		public int order;
		public List<String> hover = new ArrayList<>();
		public String link = "";
		public boolean editorOnly;
		public String requires = "";
	}
}
