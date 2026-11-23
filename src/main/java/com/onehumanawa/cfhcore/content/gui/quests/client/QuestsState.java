package com.onehumanawa.cfhcore.content.gui.quests.client;

import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.content.gui.core.client.DevHarness;
import com.onehumanawa.cfhcore.content.gui.core.event.Event;
import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.quests.QuestsConfig;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.BookWords;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.logic.Rules;
import com.onehumanawa.cfhcore.content.gui.quests.model.BookJson;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Ids;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestState;
import com.onehumanawa.cfhcore.content.gui.quests.net.QuestsNet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;

public final class QuestsState {
	public static final Event<Runnable> BOOK_CHANGED = new Event("QUESTS_BOOK_CHANGED");
	public static final Event<Consumer<List<String>>> PROGRESS_CHANGED = new Event("QUESTS_PROGRESS_CHANGED");
	public static final Event<Consumer<QuestsState.Happening>> HAPPENED = new Event("QUESTS_HAPPENED");
	public static final Event<Consumer<QuestsState.Note>> NOTICE = new Event("QUESTS_NOTICE");
	@Nullable
	private static JsonObject words;
	private static final List<Runnable> OWN_WORDS = new ArrayList<>();
	private static boolean editorOn;
	@Nullable
	private static QuestBook book;
	@Nullable
	private static BookIndex index;
	private static final Map<String, QuestState> STATES = new HashMap<>();
	private static final Map<String, Rules.Status> STATUS = new HashMap<>();
	private static boolean editor;
	private static boolean preview;
	@Nullable
	private static QuestBook incoming;
	private static boolean incomingEditor;
	private static final boolean FORCE_PREVIEW = Boolean.getBoolean("cfh.quests.forcePreview");
	@Nullable
	private static QuestsState.Overlay overlay;
	private static final List<String> OVERLAY_KEYS = new ArrayList<>();
	private static final Rules.Progress PROGRESS = new Rules.Progress() {
		@Override
		public boolean done(String key) {
			QuestState s = QuestsState.STATES.get(key);
			return s != null && s.done;
		}

		@Override
		public boolean everDone(String key) {
			QuestState s = QuestsState.STATES.get(key);
			return s != null && (s.done || s.times > 0);
		}

		@Override
		public boolean started(String key) {
			QuestState s = QuestsState.STATES.get(key);
			return s != null && s.begun();
		}

		@Override
		public long task(String key, int i) {
			QuestState s = QuestsState.STATES.get(key);
			return s == null ? 0L : s.task(i);
		}

		@Override
		public boolean claimable(String key) {
			QuestState s = QuestsState.STATES.get(key);
			BookIndex.Entry e = QuestsState.index == null ? null : QuestsState.index.get(key);
			return s != null && e != null && s.claimable(e.quest());
		}
	};

	public static boolean connected() {
		return book != null && !preview;
	}

	public static boolean preview() {
		return preview;
	}

	public static boolean available() {
		return book != null;
	}

	@Nullable
	public static QuestBook book() {
		return book;
	}

	@Nullable
	public static BookIndex index() {
		return index;
	}

	public static boolean canEdit() {
		return editor;
	}

	public static Rules.Progress progress() {
		return PROGRESS;
	}

	public static QuestState state(String key) {
		QuestState s = STATES.get(key);
		return s != null ? s : new QuestState(key);
	}

	@Nullable
	public static BookIndex.Entry entry(@Nullable String key) {
		return index == null ? null : index.get(key);
	}

	public static Rules.Status status(@Nullable String key) {
		BookIndex.Entry e = entry(key);
		return e != null && index != null ? STATUS.computeIfAbsent(key, k -> Rules.status(index, e, PROGRESS)) : Rules.Status.HIDDEN;
	}

	public static float fraction(@Nullable String key) {
		BookIndex.Entry e = entry(key);
		return e != null && index != null ? Rules.fraction(index, e, PROGRESS) : 0.0F;
	}

	public static boolean chapterOpen(Chapter chapter) {
		return index != null && Rules.chapterOpen(index, chapter, PROGRESS);
	}

	public static boolean chapterDone(Chapter chapter) {
		return index != null && Rules.chapterDone(index, chapter, PROGRESS);
	}

	public static int[] chapterCount(Chapter chapter) {
		return Rules.chapterCount(chapter, PROGRESS);
	}

	public static int claimable(Chapter chapter) {
		int n = 0;

		for (Quest q : chapter.quests) {
			if (PROGRESS.claimable(Ids.key(chapter, q))) {
				n++;
			}
		}

		return n;
	}

	public static int claimable() {
		int n = 0;
		if (book != null) {
			for (Chapter c : book.chapters.values()) {
				n += claimable(c);
			}
		}

		return n;
	}

	public static void overlay(@Nullable QuestsState.Overlay o) {
		overlay = o;
		if (book != null) {
			reindex();
		}
	}

	public static void overlayChanged() {
		if (book != null && (overlay != null || !OVERLAY_KEYS.isEmpty())) {
			reindex();
		}
	}

	public static void overlayProgress(List<String> keys) {
		if (book != null && overlay != null && !OVERLAY_KEYS.isEmpty()) {
			overlayStates();
			progressChanged(keys);
		}
	}

	private static void applyOverlay() {
		for (String key : OVERLAY_KEYS) {
			STATES.remove(key);
		}

		OVERLAY_KEYS.clear();
		if (book != null) {
			book.chapters.values().removeIf(cx -> cx.readOnly);
			book.clientGroups.clear();
			if (overlay != null) {
				try {
					overlay.chapters(book);
				} catch (Exception var4) {
					CFHGuiQuests.LOGGER.error("[CFHGui Quests] the chapters laid beside the book could not be added", var4);
					book.chapters.values().removeIf(cx -> cx.readOnly);
					book.clientGroups.clear();
					return;
				}

				for (Chapter c : book.chapters.values()) {
					if (c.readOnly) {
						for (Quest q : c.quests) {
							OVERLAY_KEYS.add(Ids.key(c, q));
						}
					}
				}

				overlayStates();
			}
		}
	}

	static void overlayStates() {
		if (overlay != null && !OVERLAY_KEYS.isEmpty()) {
			try {
				overlay.states(STATES);
			} catch (Exception var1) {
				CFHGuiQuests.LOGGER.error("[CFHGui Quests] the progress of the chapters laid beside the book could not be read", var1);
			}
		}
	}

	public static boolean pinned(String key) {
		return QuestsConfig.get().pinned.contains(key);
	}

	public static void pin(String key, boolean on) {
		List<String> pins = QuestsConfig.get().pinned;
		if (on != pins.contains(key)) {
			if (on) {
				pins.add(key);
			} else {
				pins.remove(key);
			}

			QuestsConfig.save();
			PROGRESS_CHANGED.invoke(l -> l.accept(List.of(key)));
		}
	}

	public static void onBookStart(QuestsNet.BookStart p) {
		if (!FORCE_PREVIEW) {
			QuestBook head = (QuestBook)JsonWire.decode(BookJson.GSON, p.head(), QuestBook.class);
			incoming = head == null ? new QuestBook() : head.sanitize();
			incomingEditor = p.editor();
		}
	}

	public static void onChapter(QuestsNet.ChapterData p) {
		if (!FORCE_PREVIEW) {
			Chapter c = (Chapter)JsonWire.decode(BookJson.GSON, p.chapter(), Chapter.class);
			if (c != null && Ids.valid(p.id())) {
				c.id = p.id();
				c.sanitize();
				if (incoming != null) {
					incoming.chapters.put(c.id, c);
				} else if (book != null) {
					if (!asWritten()) {
						BookWords.apply(c, words, OWN_WORDS);
					}

					book.chapters.put(c.id, c);
					reindex();
				}
			} else {
				CFHGuiQuests.LOGGER.warn("[CFHGui Quests] the chapter {} the server sent cannot be read", p.id());
			}
		}
	}

	public static void onWords(QuestsNet.Words p) {
		if (!FORCE_PREVIEW) {
			boolean lain = !OWN_WORDS.isEmpty();
			ownWords();
			words = p.words().length == 0 ? null : (JsonObject)JsonWire.decode(BookJson.GSON, p.words(), JsonObject.class);
			layWords();
			if (book != null && (lain || !OWN_WORDS.isEmpty())) {
				reindex();
			}
		}
	}

	public static void written(boolean on) {
		if (editorOn != on) {
			editorOn = on;
			if (book != null && words != null && editor) {
				if (on) {
					ownWords();
				} else {
					layWords();
				}

				reindex();
			}
		}
	}

	private static boolean asWritten() {
		return editorOn && editor;
	}

	private static void layWords() {
		if (book != null && words != null && !asWritten() && OWN_WORDS.isEmpty()) {
			BookWords.apply(book, words, OWN_WORDS);
		}
	}

	private static void ownWords() {
		for (int i = OWN_WORDS.size() - 1; i >= 0; i--) {
			OWN_WORDS.get(i).run();
		}

		OWN_WORDS.clear();
	}

	public static void onNotice(QuestsNet.Notice p) {
		QuestsState.Note note = new QuestsState.Note(p.title(), p.text(), p.icon());
		NOTICE.invoke(l -> l.accept(note));
	}

	public static boolean replacesFtb() {
		return book != null && !preview && "ftbquests".equals(book.source) && QuestsConfig.get().ftbTakeover;
	}

	public static void onChapterGone(QuestsNet.ChapterGone p) {
		if (!FORCE_PREVIEW) {
			if (book != null && book.chapters.remove(p.id()) != null) {
				reindex();
			}
		}
	}

	public static void onBookEnd(QuestsNet.BookEnd p) {
		if (!FORCE_PREVIEW && incoming != null) {
			boolean wasPreview = preview;
			incoming.revision = p.revision();
			book = incoming;
			editor = incomingEditor;
			book.showUnlisted = editor;
			incoming = null;
			preview = false;
			words = null;
			OWN_WORDS.clear();
			if (wasPreview) {
				STATES.clear();
			}

			reindex();
		}
	}

	public static void onProgress(QuestsNet.Progress p) {
		if (!FORCE_PREVIEW) {
			QuestState[] states = (QuestState[])JsonWire.decode(BookJson.GSON, p.states(), QuestState[].class);
			if (p.reset()) {
				STATES.clear();
				overlayStates();
			}

			ArrayList<String> changed = new ArrayList<>();
			if (states != null) {
				for (QuestState s : states) {
					if (s != null && s.key != null) {
						STATES.put(s.key, s.sanitize());
						changed.add(s.key);
					}
				}
			}

			progressChanged((List<String>)(p.reset() ? List.of() : changed));
		}
	}

	public static void onHappened(QuestsNet.Happened p) {
		QuestsState.Happening h = new QuestsState.Happening(p.kind(), p.subject(), p.who());
		HAPPENED.invoke(l -> l.accept(h));
	}

	public static void reindex() {
		applyOverlay();
		index = book == null ? null : new BookIndex(book);
		STATUS.clear();
		BOOK_CHANGED.invoke(Runnable::run);
	}

	static void progressChanged(List<String> keys) {
		STATUS.clear();
		PROGRESS_CHANGED.invoke(l -> l.accept(keys));
	}

	static Map<String, QuestState> states() {
		return STATES;
	}

	static void happened(QuestsState.Happening h) {
		HAPPENED.invoke(l -> l.accept(h));
	}

	static void disconnected() {
		if (!FORCE_PREVIEW) {
			book = null;
			index = null;
			incoming = null;
			words = null;
			OWN_WORDS.clear();
			editor = false;
			preview = false;
			STATES.clear();
			OVERLAY_KEYS.clear();
			STATUS.clear();
			BOOK_CHANGED.invoke(Runnable::run);
			ensurePreview();
		}
	}

	static void ensurePreview() {
		if (book == null && (DevHarness.sampleData() || FORCE_PREVIEW)) {
			try {
				QuestBook example = BookJson.loadBundled();
				if (example.chapters.isEmpty()) {
					return;
				}

				book = example;
				preview = true;
				editor = true;
				example.showUnlisted = true;
				index = new BookIndex(example);
				STATES.clear();
				OVERLAY_KEYS.clear();
				PreviewQuests.seed(example, index, STATES);
				reindex();
			} catch (Exception var1) {
				CFHGuiQuests.LOGGER.warn("[CFHGui Quests] the example book could not be shown: {}", var1.toString());
			}
		}
	}

	private QuestsState() {
	}

	public static record Happening(String kind, String subject, String who) {
	}

	public static record Note(String title, String text, String icon) {
	}

	public interface Overlay {
		void chapters(QuestBook var1);

		void states(Map<String, QuestState> var1);
	}
}
