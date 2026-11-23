package com.onehumanawa.cfhcore.content.gui.quests.client.editor;

import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

final class Selection {
	private final Set<String> quests = new LinkedHashSet<>();
	private final Set<Integer> decorations = new LinkedHashSet<>();
	@Nullable
	private String linkFrom;
	@Nullable
	private String linkTo;
	private int revision;

	Set<String> quests() {
		return this.quests;
	}

	Set<Integer> decorations() {
		return this.decorations;
	}

	int revision() {
		return this.revision;
	}

	boolean isEmpty() {
		return this.quests.isEmpty() && this.decorations.isEmpty() && this.linkFrom == null;
	}

	int size() {
		return this.quests.size() + this.decorations.size();
	}

	boolean has(String questId) {
		return this.quests.contains(questId);
	}

	boolean hasDecoration(int index) {
		return this.decorations.contains(index);
	}

	boolean hasLink(String from, String to) {
		return from.equals(this.linkFrom) && to.equals(this.linkTo);
	}

	@Nullable
	String linkFrom() {
		return this.linkFrom;
	}

	@Nullable
	String linkTo() {
		return this.linkTo;
	}

	Selection.Kind kind() {
		if (this.linkFrom != null) {
			return Selection.Kind.LINK;
		} else if (this.quests.size() == 1 && this.decorations.isEmpty()) {
			return Selection.Kind.QUEST;
		} else if (this.decorations.size() == 1 && this.quests.isEmpty()) {
			return Selection.Kind.DECORATION;
		} else {
			return this.isEmpty() ? Selection.Kind.NOTHING : Selection.Kind.SEVERAL;
		}
	}

	@Nullable
	String quest() {
		return this.kind() == Selection.Kind.QUEST ? this.quests.iterator().next() : null;
	}

	int decoration() {
		return this.kind() == Selection.Kind.DECORATION ? this.decorations.iterator().next() : -1;
	}

	boolean clear() {
		if (this.isEmpty()) {
			return false;
		} else {
			this.quests.clear();
			this.decorations.clear();
			this.linkFrom = this.linkTo = null;
			this.revision++;
			return true;
		}
	}

	void only(String questId) {
		if (this.kind() != Selection.Kind.QUEST || !this.quests.contains(questId)) {
			this.clear();
			this.quests.add(questId);
			this.revision++;
		}
	}

	void onlyDecoration(int index) {
		if (this.kind() != Selection.Kind.DECORATION || !this.decorations.contains(index)) {
			this.clear();
			this.decorations.add(index);
			this.revision++;
		}
	}

	void link(String from, String to) {
		if (!this.hasLink(from, to)) {
			this.clear();
			this.linkFrom = from;
			this.linkTo = to;
			this.revision++;
		}
	}

	void toggle(String questId) {
		this.linkFrom = this.linkTo = null;
		if (!this.quests.remove(questId)) {
			this.quests.add(questId);
		}

		this.revision++;
	}

	void toggleDecoration(int index) {
		this.linkFrom = this.linkTo = null;
		if (!this.decorations.remove(index)) {
			this.decorations.add(index);
		}

		this.revision++;
	}

	void set(Collection<String> questIds, Collection<Integer> decorationIndices) {
		if (this.linkFrom != null
			|| this.quests.size() != questIds.size()
			|| this.decorations.size() != decorationIndices.size()
			|| !this.quests.containsAll(questIds)
			|| !this.decorations.containsAll(decorationIndices)) {
			this.quests.clear();
			this.decorations.clear();
			this.linkFrom = this.linkTo = null;
			this.quests.addAll(questIds);
			this.decorations.addAll(decorationIndices);
			this.revision++;
		}
	}

	void retain(Chapter chapter) {
		boolean changed = this.quests.removeIf(id -> chapter.quest(id) == null);
		changed |= this.decorations.removeIf(i -> i < 0 || i >= chapter.decorations.size());
		if (this.linkFrom != null) {
			Quest to = chapter.quest(this.linkTo);
			if (to == null || chapter.quest(this.linkFrom) == null || !EditorOps.dependsOn(chapter, to, this.linkFrom)) {
				this.linkFrom = this.linkTo = null;
				changed = true;
			}
		}

		if (changed) {
			this.revision++;
		}
	}

	static enum Kind {
		NOTHING,
		QUEST,
		DECORATION,
		SEVERAL,
		LINK;
	}
}
