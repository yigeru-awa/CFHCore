package com.onehumanawa.cfhcore.content.gui.quests.client.editor;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import org.jetbrains.annotations.Nullable;

final class History {
	private static final int LIMIT = 120;
	private static final long LIMIT_CHARS = 24000000L;
	private static final int LEAST = 6;
	private static final int RUN_MS = 900;
	private final Deque<String> undo = new ArrayDeque<>();
	private final Deque<String> redo = new ArrayDeque<>();
	@Nullable
	private String runOf;
	private long runAtMs;

	void record(String before, @Nullable String kind) {
		long now = Clock.nowMs();
		boolean run = kind != null && kind.equals(this.runOf) && now - this.runAtMs < 900L && !this.undo.isEmpty();
		this.runOf = kind;
		this.runAtMs = now;
		this.redo.clear();
		if (!run) {
			this.undo.push(before);
			this.trim();
		}
	}

	private void trim() {
		long chars = 0L;

		for (String step : this.undo) {
			chars += (long)step.length();
		}

		while (this.undo.size() > 120 || chars > 24000000L && this.undo.size() > 6) {
			chars -= (long)this.undo.removeLast().length();
		}
	}

	@Nullable
	String undo(String current) {
		if (this.undo.isEmpty()) {
			return null;
		} else {
			this.runOf = null;
			this.redo.push(current);
			return this.undo.pop();
		}
	}

	@Nullable
	String redo(String current) {
		if (this.redo.isEmpty()) {
			return null;
		} else {
			this.runOf = null;
			this.undo.push(current);
			this.trim();
			return this.redo.pop();
		}
	}

	void refused(String refusedState, String now) {
		this.runOf = null;
		if (!this.undo.isEmpty() && this.undo.peek().equals(now)) {
			this.undo.pop();
		}

		this.redo.clear();
		this.redo.push(refusedState);
	}

	void breakRun() {
		this.runOf = null;
	}

	boolean canUndo() {
		return !this.undo.isEmpty();
	}

	boolean canRedo() {
		return !this.redo.isEmpty();
	}

	int steps() {
		return this.undo.size();
	}

	void clear() {
		this.undo.clear();
		this.redo.clear();
		this.runOf = null;
	}
}
