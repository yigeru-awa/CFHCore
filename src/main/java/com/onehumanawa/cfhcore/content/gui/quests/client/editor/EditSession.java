package com.onehumanawa.cfhcore.content.gui.quests.client.editor;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiModal;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestActions;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.client.ui.QuestBookScreen;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.GraphModel;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestCanvas;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestPainter;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.CFHGuiQuestPainter;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.TreeView;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.model.BookJson;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Ids;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import com.onehumanawa.cfhcore.content.gui.quests.model.Reward;
import com.onehumanawa.cfhcore.content.gui.quests.net.QuestsNet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class EditSession {
	static boolean wanted;
	static boolean snapping = true;
	static boolean collapsed;
	static boolean bookTab;
	private static final Map<QuestBookScreen, EditSession> SESSIONS = new WeakHashMap<>();
	private static final Map<String, History> HISTORIES = new HashMap<>();
	@Nullable
	private static Object historiesOf;
	private static final String HEAD = "<book>";
	private static final Map<String, EditSession.Sync> SENDING = new HashMap<>();
	private static final List<EditSession.Retry> RETRIES = new ArrayList<>();
	private static final int RETRY_MS = 1100;
	@Nullable
	private static String clipboard;
	final QuestBookScreen screen;
	final Selection selection = new Selection();
	final EditorTool tool;
	final EditorPanel panel;
	@Nullable
	private Chapter work;
	@Nullable
	private EditSession.Sync sync;
	private boolean canvasAttached;
	private boolean flushing;
	private boolean busy;
	private boolean refused;
	@Nullable
	private Component refusal;
	private int opsPending;
	private long savedAtMs;
	private int structure;
	private final Set<String> fresh = new HashSet<>();
	@Nullable
	private QuestPainter painterBefore;
	@Nullable
	private CFHGuiButton switchButton;
	@Nullable
	private Component notice;
	private long noticeAtMs;
	private final Map<String, Runnable> soon = new LinkedHashMap<>();
	private final Map<String, Long> soonAtMs = new HashMap<>();

	private EditSession(QuestBookScreen screen) {
		this.screen = screen;
		this.tool = new EditorTool(this);
		this.panel = new EditorPanel(this);
	}

	static EditSession of(QuestBookScreen screen) {
		return SESSIONS.computeIfAbsent(screen, EditSession::new);
	}

	@Nullable
	static EditSession current() {
		return Minecraft.getInstance().screen instanceof QuestBookScreen s ? SESSIONS.get(s) : null;
	}

	@Nullable
	Chapter work() {
		return this.work;
	}

	boolean active() {
		return this.canvasAttached && this.work != null;
	}

	boolean editing() {
		return this.canvasAttached;
	}

	@Nullable
	Quest quest(@Nullable String id) {
		return this.work == null ? null : this.work.quest(id);
	}

	@Nullable
	QuestBook book() {
		return QuestsState.book();
	}

	@Nullable
	QuestCanvas canvas() {
		return this.screen.canvas();
	}

	int structure() {
		return this.structure;
	}

	String key(String questId) {
		return this.work == null ? questId : Ids.key(this.work.id, questId);
	}

	boolean isFresh(String questId) {
		return this.fresh.contains(questId) && EditorOps.generated(questId);
	}

	EditSession.Save save() {
		if (this.refused) {
			return EditSession.Save.REFUSED;
		} else {
			return (this.sync == null || !this.sync.pending()) && this.opsPending <= 0 && !this.busy ? EditSession.Save.SAVED : EditSession.Save.SAVING;
		}
	}

	@Nullable
	Component refusal() {
		return this.refusal;
	}

	long savedAtMs() {
		return this.savedAtMs;
	}

	@Nullable
	Component notice() {
		if (this.notice != null && Clock.nowMs() - this.noticeAtMs > 1600L) {
			this.notice = null;
		}

		return this.notice;
	}

	long noticeAtMs() {
		return this.noticeAtMs;
	}

	void notice(Component words) {
		this.notice = words;
		this.noticeAtMs = Clock.nowMs();
	}

	private static String json(Chapter c) {
		return BookJson.GSON.toJson(c);
	}

	private static Chapter parse(String text, String id) {
		Chapter c = (Chapter)BookJson.GSON.fromJson(text, Chapter.class);
		c.id = id;
		return c.sanitize();
	}

	private History history() {
		return histories().computeIfAbsent(this.work == null ? "<book>" : this.work.id, k -> new History());
	}

	private static History headHistory() {
		return histories().computeIfAbsent("<book>", k -> new History());
	}

	private static Map<String, History> histories() {
		Object server = Minecraft.getInstance().getConnection();
		if (server != historiesOf) {
			historiesOf = server;
			HISTORIES.clear();
		}

		return HISTORIES;
	}

	static void tickRetries() {
		if (!RETRIES.isEmpty()) {
			long now = Clock.nowMs();
			Object server = Minecraft.getInstance().getConnection();
			List<EditSession.Retry> due = new ArrayList<>();

			for (EditSession.Retry r : RETRIES) {
				if (r.atMs() <= now || r.server() != server) {
					due.add(r);
				}
			}

			RETRIES.removeAll(due);

			for (EditSession.Retry rx : due) {
				if (rx.server() == server) {
					try {
						rx.run().run();
					} catch (RuntimeException var7) {
						CFHGuiQuests.LOGGER.error("[CFHGui Quests] the editor could not send a change again", var7);
					}
				}
			}
		}
	}

	private static void retry(Runnable run) {
		RETRIES.add(new EditSession.Retry(Clock.nowMs() + 1100L, Minecraft.getInstance().getConnection(), run));
	}

	private static boolean tooFast(QuestsNet.Result result) {
		return !result.ok() && "too_fast".equals(result.code());
	}

	private boolean headScope() {
		return this.work == null || bookTab && this.selection.isEmpty();
	}

	boolean canUndo() {
		return this.headScope() ? headHistory().canUndo() : this.history().canUndo();
	}

	boolean canRedo() {
		return this.headScope() ? headHistory().canRedo() : this.history().canRedo();
	}

	boolean possible() {
		return QuestsState.canEdit() && this.screen.canvas() != null;
	}

	private boolean editable() {
		if (QuestsState.canEdit() && this.screen.canvas() != null) {
			QuestBook book = QuestsState.book();
			if (book == null) {
				return false;
			} else {
				Chapter shown = this.screen.shown();
				return shown != null && !shown.readOnly || !hasOwnChapters(book);
			}
		} else {
			return false;
		}
	}

	private static boolean hasOwnChapters(@Nullable QuestBook book) {
		if (book == null) {
			return false;
		} else {
			for (Chapter c : book.chapters.values()) {
				if (!c.readOnly) {
					return true;
				}
			}

			return false;
		}
	}

	private boolean keptByTheGame(@Nullable Chapter shown) {
		return shown != null && shown.readOnly && hasOwnChapters(QuestsState.book());
	}

	void header(Consumer<AbstractWidget> add) {
		boolean wide = this.screen.width >= 600;
		Component off = Tr.t("switch.off");
		Component on = Tr.t("switch.on");
		CFHGuiButton b = new CFHGuiButton(0, 0, 20, 20, (Component)(wide ? off : Component.empty()), this::toggle).icon(Icon.EDIT);
		if (wide) {
			b.setMessage(on);
			int w = b.preferredWidth();
			b.setMessage(off);
			b.setWidth(Math.max(w, b.preferredWidth()) + 6);
		}

		this.switchButton = b;
		this.updateSwitch();
		add.accept(b);
		this.later();
	}

	private void updateSwitch() {
		CFHGuiButton b = this.switchButton;
		if (b != null) {
			boolean wide = !b.getMessage().getString().isEmpty() || this.screen.width >= 600;
			Chapter shown = this.screen.shown();
			if (this.screen.canvas() == null) {
				b.active = true;
				b.variant(Variant.SECONDARY);
				b.tip(Tr.t("switch.elsewhere"));
				if (wide) {
					b.setMessage(Tr.t("switch.off"));
				}
			} else {
				boolean readOnly = this.keptByTheGame(shown);
				boolean on = wanted && !readOnly;
				b.active = !readOnly;
				b.variant(on ? Variant.PRIMARY : Variant.SECONDARY);
				if (wide) {
					b.setMessage(Tr.t(on ? "switch.on" : "switch.off"));
				}

				b.tip(readOnly ? Tr.t("switch.readonly") : Tr.t(on ? "switch.on.tip" : "switch.off.tip"));
			}
		}
	}

	void toggle() {
		if (this.screen.canvas() == null) {
			Chapter shown = this.screen.shown();
			QuestEditor.open(this.screen, shown == null ? null : shown.id);
		} else if (!this.keptByTheGame(this.screen.shown())) {
			boolean on = !wanted;
			if (on) {
				want(true);
			} else {
				wanted = false;
			}

			this.apply(true);
			if (!on) {
				QuestsState.written(false);
			}
		}
	}

	static void want(boolean on) {
		wanted = on;
		QuestsState.written(on);
	}

	private void later() {
		Minecraft.getInstance().tell(() -> {
			if (Minecraft.getInstance().screen == this.screen) {
				this.apply(false);
			}
		});
	}

	void beforeFirstBuild() {
		if (this.possible()) {
			this.screen.sidePanel(this.panel);
		}
	}

	private void apply(boolean byHand) {
		boolean edit = wanted && this.editable();
		QuestCanvas canvas = this.screen.canvas();
		boolean whole = wholeInView(canvas);
		if (edit) {
			this.attachCanvas();
			if (this.screen.sidePanel() != this.panel) {
				this.screen.sidePanel(this.panel);
				room(canvas, whole, this.panel.widthOnScreen());
			}
		} else {
			boolean had = this.screen.sidePanel() == this.panel;
			this.detachCanvas();
			if (had) {
				int w = this.panel.widthOnScreen();
				this.screen.sidePanel(null);
				room(canvas, whole, -w);
			}
		}

		this.updateSwitch();
		if (byHand) {
			CFHGuiSounds.tick();
		}
	}

	static boolean wholeInView(@Nullable QuestCanvas canvas) {
		if (canvas == null) {
			return false;
		} else {
			GraphModel graph = canvas.graph();
			if (graph.isEmpty()) {
				return true;
			} else {
				Rect free = canvas.free();
				return canvas.sx(graph.minX) >= (float)free.x()
					&& canvas.sx(graph.maxX) <= (float)free.right()
					&& canvas.sy(graph.minY) >= (float)free.y()
					&& canvas.sy(graph.maxY) <= (float)free.bottom();
			}
		}
	}

	static void room(@Nullable QuestCanvas canvas, boolean whole, int taken) {
		if (canvas != null) {
			if (whole) {
				canvas.fit();
			} else {
				canvas.pan((float)taken / 2.0F / Math.max(1.0F, canvas.unit()), 0.0F);
			}
		}
	}

	private void attachCanvas() {
		QuestCanvas canvas = this.screen.canvas();
		if (canvas != null) {
			if (!this.canvasAttached) {
				this.canvasAttached = true;
				String chosen = canvas.selected();
				this.screen.tree().select(null);
				this.screen.tree().showHidden(true);
				canvas.tool(this.tool);
				if (canvas.painter().getClass() == CFHGuiQuestPainter.class) {
					this.painterBefore = canvas.painter();
					canvas.painter(new EditorPainter());
				}

				this.adopt(this.screen.shown());
				if (chosen != null && this.work != null && Ids.chapterOf(chosen).equals(this.work.id) && this.work.quest(Ids.questOf(chosen)) != null) {
					this.selection.only(Ids.questOf(chosen));
				}
			} else if (canvas.tool() != this.tool) {
				canvas.tool(this.tool);
			}
		}
	}

	private void detachCanvas() {
		if (this.canvasAttached) {
			this.panel.flush();
			this.canvasAttached = false;
			this.tool.reset();
			QuestCanvas canvas = this.screen.canvas();
			if (canvas != null) {
				if (canvas.tool() == this.tool) {
					canvas.tool(null);
				}

				if (this.painterBefore != null) {
					canvas.painter(this.painterBefore);
				}
			}

			this.painterBefore = null;
			this.screen.tree().showHidden(false);
			this.work = null;
			this.sync = null;
			this.selection.clear();
			this.structure++;
		}
	}

	void closed() {
		this.panel.flush();
		this.tool.reset();
	}

	private void adopt(@Nullable Chapter shown) {
		this.tool.reset();
		this.selection.clear();
		this.fresh.clear();
		this.structure++;
		if (shown != null && !shown.readOnly) {
			EditSession.Sync sending = SENDING.get(shown.id);
			if (sending != null && !sending.dead && sending.pending() && sending.server == Minecraft.getInstance().getConnection()) {
				sending.owner = this;
				this.sync = sending;
				this.work = BookJson.copy(sending.mine);
				if (shown != sending.mine) {
					this.reput(sending);
				}
			} else {
				this.work = BookJson.copy(shown);
				this.sync = new EditSession.Sync(this, shown.id, shown, json(shown));
			}
		} else {
			this.work = null;
			this.sync = null;
		}
	}

	void shown(@Nullable Chapter shown) {
		this.updateSwitch();
		if (wanted && this.editable()) {
			if (!this.canvasAttached) {
				this.attachCanvas();
				this.later();
			} else if (shown != null && this.work != null && this.sync != null && shown.id.equals(this.work.id)) {
				EditSession.Sync s = this.sync;
				if (shown != s.mine) {
					String theirs = json(shown);
					if (s.pending()) {
						s.latest = shown;
						if (theirs.equals(json(s.mine))) {
							s.mine = shown;
						} else {
							this.reput(s);
						}
					} else {
						this.settle(s, shown, theirs);
					}
				}

				this.tool.rebind();
			} else {
				this.panel.flush();
				this.adopt(shown);
				this.tool.rebind();
			}
		} else {
			if (this.canvasAttached) {
				this.detachCanvas();
				this.later();
			}
		}
	}

	private void reput(EditSession.Sync s) {
		Minecraft.getInstance().tell(() -> {
			QuestBook book = QuestsState.book();
			if (this.sync == s && s.pending() && book != null && book.chapter(s.id) != null && book.chapter(s.id) != s.mine) {
				this.putLocal(s.mine);
			}
		});
	}

	private void settle(EditSession.Sync s, Chapter theirs, String text) {
		s.base = text;
		s.mine = theirs;
		s.latest = null;
		QuestBook book = QuestsState.book();
		if (book != null && book.chapter(s.id) != theirs) {
			this.putLocal(theirs);
		}

		if (this.sync == s && this.work != null) {
			if (!text.equals(json(this.work))) {
				this.work = BookJson.copy(theirs);
				this.selection.retain(this.work);
				this.history().breakRun();
				this.structure++;
				this.tool.rebind();
			}
		}
	}

	private void putLocal(Chapter chapter) {
		QuestBook book = QuestsState.book();
		if (book != null) {
			book.chapters.put(chapter.id, chapter);
			reindexLocal();
		}
	}

	private static void reindexLocal() {
		QuestsState.reindex();
	}

	boolean change(@Nullable String kind, Runnable mutation) {
		return this.change(kind, mutation, true);
	}

	boolean change(@Nullable String kind, Runnable mutation, boolean structural) {
		if (this.work == null || this.sync == null) {
			return false;
		} else if (this.busy) {
			this.notice(Tr.t("busy"));
			return false;
		} else {
			if (!this.flushing) {
				this.flushing = true;

				try {
					this.panel.flush();
				} finally {
					this.flushing = false;
				}
			}

			Chapter c = this.work;
			EditSession.Sync s = this.sync;
			if (c != null && s != null) {
				String before = json(c);

				try {
					mutation.run();
					c.sanitize();
				} catch (RuntimeException var10) {
					CFHGuiQuests.LOGGER.error("[CFHGui Quests] an edit of the chapter {} failed and was taken back", c.id, var10);
					this.work = parse(before, c.id);
					this.structure++;
					return false;
				}

				String after = json(c);
				if (after.equals(before)) {
					return false;
				} else {
					this.history().record(before, kind);
					this.selection.retain(c);
					if (structural) {
						this.structure++;
					}

					this.commit(s);
					return true;
				}
			} else {
				return false;
			}
		}
	}

	boolean edit(String questId, @Nullable String kind, Consumer<Quest> change, boolean structural) {
		return this.change(kind, () -> {
			Quest q = this.quest(questId);
			if (q != null) {
				change.accept(q);
			}
		}, structural);
	}

	private void commit(EditSession.Sync s) {
		Chapter c = this.work;
		if (c != null) {
			Chapter copy = BookJson.copy(c);
			s.mine = copy;
			this.refused = false;
			this.putLocal(copy);
			if (s.flying != null) {
				s.dirty = true;
			} else {
				this.send(s);
			}
		}
	}

	private void send(EditSession.Sync s) {
		if (!s.dead) {
			Chapter chapter = s.mine;
			s.flying = json(chapter);
			s.dirty = false;
			s.sent++;
			SENDING.put(s.id, s);
			QuestActions.editChapter(chapter, result -> s.owner.answered(s, result));
		}
	}

	private void answered(EditSession.Sync s, QuestsNet.Result result) {
		s.acked++;
		String sent = s.flying;
		s.flying = null;
		if (s.dead || !tooFast(result) && (!result.ok() || !s.dirty)) {
			SENDING.remove(s.id, s);
		}

		if (!s.dead) {
			if (result.ok()) {
				if (sent != null) {
					s.base = sent;
				}

				if (s.dirty) {
					this.send(s);
				} else {
					if (s.latest != null) {
						this.settle(s, s.latest, json(s.latest));
					}

					if (this.sync == s) {
						this.savedAtMs = Clock.nowMs();
						this.refusal = null;
					}

					Runnable then = s.then;
					s.then = null;
					if (then != null) {
						then.run();
					}
				}
			} else if (tooFast(result)) {
				s.dirty = true;
				retry(() -> {
					if (s.flying == null && s.dirty) {
						s.owner.send(s);
					}
				});
			} else {
				s.dirty = false;
				if (s.then != null) {
					s.then = null;
					this.busy = false;
				}

				s.refusal = QuestActions.describe(result);
				Chapter back = parse(s.base, s.id);
				if (this.sync == s && this.work != null) {
					this.history().refused(json(this.work), s.base);
					this.work = BookJson.copy(back);
					this.selection.retain(this.work);
					this.structure++;
				}

				s.mine = back;
				s.latest = null;
				QuestBook book = QuestsState.book();
				if (book != null && book.chapter(s.id) != null) {
					this.putLocal(back);
				}

				this.refuse(s.refusal);
				this.tool.rebind();
			}
		}
	}

	private void whenAnswered(Runnable run) {
		EditSession.Sync s = this.sync;
		if (s != null && s.pending()) {
			s.then = run;
		} else {
			run.run();
		}
	}

	private void refuse(@Nullable Component reason) {
		this.refused = true;
		this.refusal = (Component)(reason == null ? Tr.t("save.refused") : reason);
		CFHGuiToasts.show(Tr.t("save.refused"), this.refusal, Icon.WARNING);
		QuestActions.sync();
	}

	void undo() {
		if (this.headScope()) {
			this.stepHead(true);
		} else {
			this.step(true);
		}
	}

	void redo() {
		if (this.headScope()) {
			this.stepHead(false);
		} else {
			this.step(false);
		}
	}

	private void step(boolean back) {
		if (this.work != null && this.sync != null) {
			if (this.busy) {
				this.notice(Tr.t("busy"));
			} else {
				this.panel.flush();
				Chapter c = this.work;
				EditSession.Sync s = this.sync;
				String to = back ? this.history().undo(json(c)) : this.history().redo(json(c));
				if (to == null) {
					this.notice(Tr.t(back ? "undo.none" : "redo.none"));
				} else {
					Set<String> had = new HashSet<>();

					for (Quest q : c.quests) {
						had.add(q.id);
					}

					this.work = parse(to, c.id);
					this.selection.retain(this.work);
					List<String> returned = new ArrayList<>();

					for (Quest q : this.work.quests) {
						if (!had.contains(q.id)) {
							returned.add(q.id);
						}
					}

					if (!returned.isEmpty()) {
						this.selection.set(returned, List.of());
					}

					this.structure++;
					this.commit(s);
					this.tool.rebind();
					this.notice(Tr.t(back ? "undo.done" : "redo.done"));
				}
			}
		}
	}

	void nudge(float dx, float dy) {
		if (this.selection.size() != 0) {
			Set<String> ids = new LinkedHashSet<>(this.selection.quests());
			Set<Integer> decorations = new LinkedHashSet<>(this.selection.decorations());
			this.change("nudge", () -> {
				for (String id : ids) {
					Quest q = this.quest(id);
					if (q != null) {
						q.x = EditorOps.tidy(q.x + dx);
						q.y = EditorOps.tidy(q.y + dy);
					}
				}

				for (int i : decorations) {
					Chapter.Decoration d = this.work.decorations.get(i);
					d.x = EditorOps.tidy(d.x + dx);
					d.y = EditorOps.tidy(d.y + dy);
				}
			});
		}
	}

	void deleteSelection() {
		Chapter c = this.work;
		if (c != null) {
			if (this.selection.kind() == Selection.Kind.LINK) {
				this.unlink(this.selection.linkFrom(), this.selection.linkTo());
			} else if (this.selection.size() != 0) {
				Set<String> ids = new LinkedHashSet<>(this.selection.quests());
				Set<Integer> decorations = new LinkedHashSet<>(this.selection.decorations());
				Runnable go = () -> {
					if (this.change(null, () -> EditorOps.delete(this.work, ids, decorations))) {
						this.notice(Tr.t("deleted", ids.size() + decorations.size()));
					}
				};
				BookIndex index = QuestsState.index();
				Set<String> waiting = new LinkedHashSet<>();
				int trees = 0;

				for (String id : ids) {
					Quest q = c.quest(id);
					if (q != null && q.isGateway()) {
						trees++;
					}

					if (index != null) {
						for (String dependent : index.dependents(Ids.key(c.id, id))) {
							if (!Ids.chapterOf(dependent).equals(c.id) || !ids.contains(Ids.questOf(dependent))) {
								waiting.add(dependent);
							}
						}
					}
				}

				if (waiting.isEmpty() && trees == 0) {
					go.run();
				} else {
					Quest first = c.quest(ids.iterator().next());
					Component what = ids.size() == 1 && first != null ? Tr.t("delete.one", QuestText.title(first)) : Tr.t("delete.many", ids.size());
					Component why = !waiting.isEmpty() ? Tr.t(waiting.size() == 1 ? "delete.waiting.one" : "delete.waiting", waiting.size()) : Tr.t("delete.tree");
					CFHGuiModal.confirmDanger(Tr.t("delete.title"), Component.empty().append(what).append(" ").append(why), Tr.t("delete.ok"), go);
				}
			}
		}
	}

	boolean copySelection() {
		Chapter c = this.work;
		if (c != null && this.selection.size() != 0) {
			clipboard = EditorOps.write(EditorOps.copy(c, this.selection.quests(), this.selection.decorations()));

			try {
				Minecraft.getInstance().keyboardHandler.setClipboard(clipboard);
			} catch (RuntimeException var3) {
			}

			this.notice(Tr.t("copied", this.selection.size()));
			return true;
		} else {
			return false;
		}
	}

	void cutSelection() {
		if (this.copySelection()) {
			this.deleteSelection();
		}
	}

	boolean canPaste() {
		return clip() != null;
	}

	@Nullable
	private static EditorOps.Clip clip() {
		String text = null;

		try {
			text = Minecraft.getInstance().keyboardHandler.getClipboard();
		} catch (RuntimeException var2) {
		}

		EditorOps.Clip fromSystem = EditorOps.read(text);
		return fromSystem != null ? fromSystem : EditorOps.read(clipboard);
	}

	void paste(float x, float y) {
		EditorOps.Clip clip = clip();
		if (this.work != null && clip != null && !clip.isEmpty()) {
			float[] centre = clip.centre();
			float dx = snapping ? EditorOps.snap(x - centre[0]) : EditorOps.tidy(x - centre[0]);
			float dy = snapping ? EditorOps.snap(y - centre[1]) : EditorOps.tidy(y - centre[1]);
			this.insert(clip, dx, dy);
		} else {
			this.notice(Tr.t("paste.none"));
		}
	}

	void duplicateSelection() {
		Chapter c = this.work;
		if (c != null && this.selection.size() != 0) {
			this.insert(EditorOps.copy(c, this.selection.quests(), this.selection.decorations()), 1.0F, 1.0F);
		}
	}

	private void insert(EditorOps.Clip clip, float dx, float dy) {
		EditorOps.Inserted[] made = new EditorOps.Inserted[1];
		if (this.change(null, () -> made[0] = EditorOps.insert(this.work, clip, dx, dy)) && made[0] != null) {
			this.selection.set(made[0].quests(), made[0].decorations());

			for (String id : made[0].quests()) {
				this.tool.pop(id);
			}

			this.notice(Tr.t("pasted", made[0].quests().size() + made[0].decorations().size()));
		}
	}

	void selectAll() {
		Chapter c = this.work;
		if (c != null) {
			List<String> ids = new ArrayList<>();

			for (Quest q : c.quests) {
				ids.add(q.id);
			}

			List<Integer> decorations = new ArrayList<>();

			for (int i = 0; i < c.decorations.size(); i++) {
				decorations.add(i);
			}

			this.selection.set(ids, decorations);
		}
	}

	@Nullable
	String newQuest(float x, float y, @Nullable String dependsOn) {
		String[] made = new String[1];
		boolean done = this.change(null, () -> {
			Quest q = EditorOps.newQuest(this.work, x, y);
			if (dependsOn != null && this.work.quest(dependsOn) != null) {
				q.deps.add(dependsOn);
			}

			made[0] = q.id;
		});
		if (done && made[0] != null) {
			this.fresh.add(made[0]);
			this.selection.only(made[0]);
			this.tool.pop(made[0]);
			this.panel.focusFirst();
			return made[0];
		} else {
			return null;
		}
	}

	EditorOps.Link link(String from, String to) {
		Chapter c = this.work;
		if (c == null) {
			return EditorOps.Link.GONE;
		} else {
			EditorOps.Link can = EditorOps.canLink(c, from, to);
			if (can == EditorOps.Link.OK) {
				this.change(null, () -> EditorOps.link(this.work, from, to));
				this.selection.link(from, to);
			} else if (can == EditorOps.Link.EXISTS) {
				this.selection.link(from, to);
				this.notice(Tr.t("link.exists"));
			} else if (can == EditorOps.Link.CIRCLE) {
				this.notice(Tr.t("link.circle"));
			}

			return can;
		}
	}

	void unlink(@Nullable String from, @Nullable String to) {
		if (from != null && to != null) {
			if (this.change(null, () -> EditorOps.unlink(this.work, from, to))) {
				this.notice(Tr.t("link.removed"));
			}
		}
	}

	void newDecoration(boolean image, float x, float y) {
		int[] at = new int[]{-1};
		boolean done = this.change(null, () -> {
			Chapter.Decoration d = new Chapter.Decoration();
			d.type = image ? "image" : "label";
			d.x = EditorOps.tidy(x);
			d.y = EditorOps.tidy(y);
			if (image) {
				d.image = "minecraft:textures/item/compass_00.png";
				d.w = d.h = 1.07F;
			} else {
				d.text = Tr.t("decoration.new").getString();
			}

			this.work.decorations.add(d);
			at[0] = this.work.decorations.size() - 1;
		});
		if (done) {
			this.selection.onlyDecoration(at[0]);
			this.panel.focusFirst();
		}
	}

	void editSelected(@Nullable String kind, Consumer<Quest> change) {
		Set<String> ids = new LinkedHashSet<>(this.selection.quests());
		this.change(kind, () -> {
			for (String id : ids) {
				Quest q = this.quest(id);
				if (q != null) {
					change.accept(q);
				}
			}
		});
	}

	void renamed(String from, String to, boolean wasChosen) {
		this.fresh.remove(from);
		if (wasChosen && this.work != null && this.work.quest(to) != null && !this.selection.has(to)) {
			this.selection.toggle(to);
		}

		this.panel.renamed(from, to);
	}

	void soon(String key, Runnable change) {
		this.soon.put(key, change);
		this.soonAtMs.put(key, Clock.nowMs());
	}

	void tick() {
		if (!this.soon.isEmpty()) {
			long now = Clock.nowMs();

			for (String key : new ArrayList<>(this.soon.keySet())) {
				if (now - this.soonAtMs.getOrDefault(key, 0L) >= 260L) {
					Runnable run = this.soon.remove(key);
					this.soonAtMs.remove(key);
					if (run != null) {
						run.run();
					}
				}
			}
		}
	}

	void flushSoon() {
		while (!this.soon.isEmpty()) {
			String key = this.soon.keySet().iterator().next();
			Runnable run = this.soon.remove(key);
			this.soonAtMs.remove(key);
			if (run != null) {
				run.run();
			}
		}
	}

	void adoptTree(String questId, String chapterId) {
		QuestBook book = QuestsState.book();
		Chapter c = this.work;
		Chapter tree = book == null ? null : book.chapter(chapterId);
		if (c != null && tree != null && !tree.readOnly && !tree.id.equals(c.id) && this.quest(questId) != null) {
			Chapter up = c;

			for (int guard = 0; up != null && up.isSubtree() && guard < 32; guard++) {
				if (tree.id.equals(up.parent)) {
					this.notice(Tr.t("tree.circle"));
					return;
				}

				up = book.chapter(up.parent);
			}

			if (!c.id.equals(tree.parent)) {
				Chapter copy = BookJson.copy(tree);
				copy.parent = c.id;
				this.sendOther(copy);
			}

			this.change(null, () -> {
				Quest now = this.quest(questId);
				if (now != null) {
					now.subtree = chapterId;
					EditorOps.dropPlaceholder(now);
				}
			});
		}
	}

	void makeGateway(String questId) {
		Chapter c = this.work;
		QuestBook book = QuestsState.book();
		Quest q = this.quest(questId);
		if (c != null && book != null && q != null && !q.isGateway()) {
			Chapter tree = EditorOps.newChapter(book, q.title.isBlank() ? Tr.t("tree.new").getString() : QuestText.title(q).getString(), "", c);
			if (!q.icon.isBlank()) {
				tree.icon = q.icon;
			}

			this.sendOther(tree);
			String key = this.key(questId);
			this.change(null, () -> {
				Quest now = this.quest(questId);
				if (now != null) {
					now.subtree = tree.id;
					EditorOps.dropPlaceholder(now);
				}
			});
			CFHGuiToasts.show(Tr.t("tree.made"), Tr.t("tree.made.body", QuestText.title(tree)), Icon.LAYERS, () -> this.enter(key));
		}
	}

	void enter(String questKey) {
		QuestBook book = QuestsState.book();
		BookIndex.Entry e = QuestsState.entry(questKey);
		if (book != null && e != null && e.quest().isGateway() && book.chapter(e.quest().subtree) != null) {
			if (Minecraft.getInstance().screen == this.screen) {
				Chapter shown = this.screen.shown();
				TreeView.Travel var10002 = shown != null && shown.id.equals(e.chapter().id) ? TreeView.Travel.INTO : TreeView.Travel.ENTRANCE;
				this.screen.showChapter(e.quest().subtree, var10002, questKey);
			}
		}
	}

	private void sendOther(Chapter chapter) {
		QuestBook book = QuestsState.book();
		if (book != null) {
			chapter.sanitize();
			book.chapters.put(chapter.id, chapter);
			reindexLocal();
			EditSession.Sync sending = SENDING.get(chapter.id);
			if (sending != null && !sending.dead && sending.pending() && sending.server == Minecraft.getInstance().getConnection()) {
				sending.mine = chapter;
				sending.dirty = true;
			} else {
				this.op(then -> QuestActions.editChapter(chapter, then));
			}
		}
	}

	private void op(Consumer<Consumer<QuestsNet.Result>> send) {
		this.opsPending++;
		this.refused = false;
		send.accept(result -> {
			if (tooFast(result)) {
				retry(() -> {
					this.opsPending = Math.max(0, this.opsPending - 1);
					this.op(send);
				});
			} else {
				this.opsPending = Math.max(0, this.opsPending - 1);
				if (result.ok()) {
					if (this.opsPending == 0) {
						this.savedAtMs = Clock.nowMs();
					}
				} else {
					this.refuse(QuestActions.describe(result));
				}
			}
		});
	}

	void newChapter(String title) {
		QuestBook book = QuestsState.book();
		if (book != null && !title.isBlank()) {
			this.panel.flush();
			Chapter shown = this.screen.shown();
			Chapter top = shown == null ? null : topOf(book, shown);
			Chapter made = EditorOps.newChapter(book, title.trim(), top != null && !top.readOnly ? top.group : "", null);
			this.sendOther(made);
			bookTab = false;
			this.goTo(made.id);
		}
	}

	private static Chapter topOf(QuestBook book, Chapter chapter) {
		Chapter top = chapter;

		for (int guard = 0; top.isSubtree() && guard < 32; guard++) {
			Chapter parent = book.chapter(top.parent);
			if (parent == null || parent == top) {
				break;
			}

			top = parent;
		}

		return top;
	}

	void goTo(String chapterId) {
		if (Minecraft.getInstance().screen == this.screen) {
			QuestBook book = QuestsState.book();
			if (book != null && book.chapter(chapterId) != null) {
				this.panel.flush();
				this.screen.showChapter(chapterId);
			}
		}
	}

	void duplicateChapter() {
		QuestBook book = QuestsState.book();
		Chapter c = this.work;
		if (book != null && c != null) {
			this.panel.flush();
			Chapter copy = EditorOps.duplicate(book, this.work, Tr.t("chapter.copy_of", QuestText.title(c).getString()).getString());
			this.sendOther(copy);
			this.goTo(copy.id);
		}
	}

	void deleteChapter() {
		QuestBook book = QuestsState.book();
		Chapter c = this.work;
		if (book != null && c != null) {
			List<Chapter> all = new ArrayList<>();
			collect(book, book.chapter(c.id), all);
			if (!all.isEmpty()) {
				int quests = 0;

				for (Chapter x : all) {
					quests += x.quests.size();
				}

				Component body = all.size() > 1
					? Tr.t("chapter.delete.body.trees", QuestText.title(c), quests, all.size() - 1)
					: Tr.t("chapter.delete.body", QuestText.title(c), quests);
				CFHGuiModal.confirmDanger(Tr.t("chapter.delete.title"), body, Tr.t("chapter.delete.ok"), this::deleteChapterNow);
			}
		}
	}

	void deleteChapterNow() {
		QuestBook book = QuestsState.book();
		Chapter c = this.work;
		if (book != null && c != null) {
			List<Chapter> all = new ArrayList<>();
			collect(book, book.chapter(c.id), all);
			if (!all.isEmpty()) {
				this.removeChapters(c, all);
			}
		}
	}

	private static void collect(QuestBook book, @Nullable Chapter chapter, List<Chapter> into) {
		if (chapter != null && !into.contains(chapter) && into.size() <= 512) {
			into.add(chapter);

			for (Chapter sub : book.subtrees(chapter.id)) {
				collect(book, sub, into);
			}
		}
	}

	private void removeChapters(Chapter chapter, List<Chapter> all) {
		QuestBook book = QuestsState.book();
		if (book != null) {
			this.panel.flush();
			List<String> ids = new ArrayList<>();
			List<String> texts = new ArrayList<>();

			for (Chapter c : all) {
				ids.add(c.id);
				texts.add(json(c));
			}

			String next = null;
			Chapter parent = chapter.isSubtree() ? book.chapter(chapter.parent) : null;
			if (parent != null) {
				next = parent.id;
				boolean led = false;

				for (Quest q : parent.quests) {
					if (chapter.id.equals(q.subtree)) {
						led = true;
					}
				}

				if (led) {
					Chapter p = BookJson.copy(parent);

					for (Quest qx : p.quests) {
						if (chapter.id.equals(qx.subtree)) {
							qx.subtree = "";
						}
					}

					this.sendOther(p);
				}
			} else {
				for (Chapter c : book.topChapters()) {
					if (!c.readOnly && !ids.contains(c.id)) {
						next = c.id;
						break;
					}
				}
			}

			HISTORIES.keySet().removeAll(ids);
			if (this.sync != null && ids.contains(this.sync.id)) {
				dead(this.sync);
			}

			for (String id : ids) {
				dead(SENDING.get(id));
			}

			if (next != null) {
				this.goTo(next);
			}

			for (String id : ids) {
				book.chapters.remove(id);
			}

			reindexLocal();

			for (String id : ids) {
				this.op(
					then -> QuestActions.deleteChapter(
							id, result -> then.accept(!result.ok() && "no_such_chapter".equals(result.code()) ? QuestsNet.Result.ok(result.request()) : result)
						)
				);
			}

			String title = QuestText.title(chapter).getString();
			CFHGuiToasts.show(Tr.t("chapter.deleted"), Tr.t("chapter.deleted.body", title), Icon.UNDO, () -> {
				EditSession now = current();
				if (now != null && QuestsState.book() != null) {
					for (int i = 0; i < ids.size(); i++) {
						now.sendOther(parse(texts.get(i), ids.get(i)));
					}

					now.goTo(ids.get(0));
				}
			}, 9000);
		}
	}

	private static void dead(@Nullable EditSession.Sync s) {
		if (s != null) {
			s.dead = true;
			s.dirty = false;
			s.then = null;
		}
	}

	void renameChapter(String to) {
		Chapter c = this.work;
		if (c != null && !to.equals(c.id)) {
			this.panel.flush();
			String from = c.id;
			this.busy = true;
			this.whenAnswered(() -> this.askRenameChapter(from, to));
		}
	}

	private void askRenameChapter(String from, String to) {
		EditSession.Sync was = this.sync;
		if (!this.refused && was != null && was.id.equals(from)) {
			QuestActions.renameChapter(from, to, result -> {
				if (tooFast(result)) {
					retry(() -> this.askRenameChapter(from, to));
				} else {
					this.busy = false;
					if (!result.ok()) {
						this.refuse(QuestActions.describe(result));
					} else {
						was.dead = true;
						HISTORIES.remove(from);
						this.savedAtMs = Clock.nowMs();
						this.goTo(to);
					}
				}
			});
		} else {
			this.busy = false;
		}
	}

	void renameQuest(String questId, String to) {
		Chapter c = this.work;
		if (c != null && !to.equals(questId) && c.quest(questId) != null) {
			this.panel.flush();
			String chapterId = c.id;
			this.busy = true;
			this.whenAnswered(() -> this.askRenameQuest(chapterId, questId, to));
		}
	}

	private void askRenameQuest(String chapterId, String questId, String to) {
		if (!this.refused && this.sync != null && this.sync.id.equals(chapterId)) {
			QuestActions.renameQuest(Ids.key(chapterId, questId), to, result -> {
				if (tooFast(result)) {
					retry(() -> this.askRenameQuest(chapterId, questId, to));
				} else {
					this.busy = false;
					if (!result.ok()) {
						this.refuse(QuestActions.describe(result));
					} else {
						HISTORIES.remove(chapterId);
						this.savedAtMs = Clock.nowMs();
						this.fresh.remove(questId);
						QuestBook book = QuestsState.book();
						Chapter now = book == null ? null : book.chapter(chapterId);
						if (now != null && this.work != null && this.work.id.equals(chapterId)) {
							this.work = BookJson.copy(now);
							this.sync = new EditSession.Sync(this, chapterId, now, json(now));
							this.selection.retain(this.work);
							if (this.work.quest(to) != null) {
								this.selection.only(to);
							}

							this.tool.rebind();
						}

						this.structure++;
					}
				}
			});
		} else {
			this.busy = false;
		}
	}

	boolean canMoveChapter(int direction) {
		QuestBook book = QuestsState.book();
		Chapter c = this.work;
		if (book != null && c != null && !c.isSubtree()) {
			List<Chapter> row = EditorOps.siblings(book, book.chapter(c.id) == null ? c : book.chapter(c.id));
			int at = -1;

			for (int i = 0; i < row.size(); i++) {
				if (row.get(i).id.equals(c.id)) {
					at = i;
				}
			}

			return at >= 0 && at + direction >= 0 && at + direction < row.size();
		} else {
			return false;
		}
	}

	void moveChapter(String chapterId, int direction) {
		QuestBook book = QuestsState.book();
		Chapter chapter = book == null ? null : book.chapter(chapterId);
		if (book != null && chapter != null && !chapter.isSubtree()) {
			this.panel.flush();
			List<Chapter> row = EditorOps.siblings(book, chapter);
			int at = row.indexOf(chapter);
			int to = at + direction;
			if (at >= 0 && to >= 0 && to < row.size()) {
				row.set(at, row.get(to));
				row.set(to, chapter);

				for (int i = 0; i < row.size(); i++) {
					Chapter c = row.get(i);
					if (c.order != i) {
						int order = i;
						if (this.work != null && c.id.equals(this.work.id)) {
							this.change(null, () -> this.work.order = order);
						} else {
							Chapter copy = BookJson.copy(c);
							copy.order = order;
							this.sendOther(copy);
						}
					}
				}

				this.structure++;
			}
		}
	}

	private static QuestBook headOf(QuestBook book) {
		QuestBook head = new QuestBook();
		head.title = book.title;
		head.subtitle = book.subtitle;
		head.advancements = book.advancements;
		head.source = book.source;
		head.closed = book.closed;
		head.emergencyCooldownSeconds = book.emergencyCooldownSeconds;

		for (Reward r : book.emergency) {
			head.emergency.add(Kinds.copy(r));
		}

		for (QuestBook.Group g : book.groups) {
			QuestBook.Group copy = new QuestBook.Group();
			copy.id = g.id;
			copy.title = g.title;
			copy.icon = g.icon;
			head.groups.add(copy);
		}

		return head;
	}

	boolean changeHead(@Nullable String kind, Consumer<QuestBook> change, boolean structural) {
		QuestBook book = QuestsState.book();
		if (book == null) {
			return false;
		} else {
			QuestBook head = headOf(book);
			String before = BookJson.GSON.toJson(head);
			change.accept(head);
			head.sanitize();
			if (BookJson.GSON.toJson(head).equals(before)) {
				return false;
			} else {
				headHistory().record(before, kind);
				this.sendHead(head);
				if (structural) {
					this.structure++;
				}

				return true;
			}
		}
	}

	private void sendHead(QuestBook head) {
		QuestBook book = QuestsState.book();
		if (book != null) {
			book.title = head.title;
			book.subtitle = head.subtitle;
			book.groups = headOf(head).groups;
			book.advancements = head.advancements;
			book.closed = head.closed;
			book.emergencyCooldownSeconds = head.emergencyCooldownSeconds;
			book.emergency = headOf(head).emergency;
			reindexLocal();
			this.op(then -> QuestActions.editHead(head, then));
		}
	}

	private void stepHead(boolean back) {
		QuestBook book = QuestsState.book();
		if (book != null) {
			this.panel.flush();
			String now = BookJson.GSON.toJson(headOf(book));
			String to = back ? headHistory().undo(now) : headHistory().redo(now);
			if (to == null) {
				this.notice(Tr.t(back ? "undo.none" : "redo.none"));
			} else {
				this.sendHead(((QuestBook)BookJson.GSON.fromJson(to, QuestBook.class)).sanitize());
				this.structure++;
				this.notice(Tr.t(back ? "undo.done" : "redo.done"));
			}
		}
	}

	void deleteGroup(String groupId) {
		QuestBook book = QuestsState.book();
		if (book != null) {
			List<Chapter> members = new ArrayList<>();

			for (Chapter c : book.chapters.values()) {
				if (!c.readOnly && groupId.equals(c.group)) {
					members.add(c);
				}
			}

			for (Chapter cx : members) {
				if (this.work != null && cx.id.equals(this.work.id)) {
					this.change(null, () -> this.work.group = "");
				} else {
					Chapter copy = BookJson.copy(cx);
					copy.group = "";
					this.sendOther(copy);
				}
			}

			this.changeHead(null, head -> head.groups.removeIf(g -> groupId.equals(g.id)), true);
		}
	}

	void askDeleteGroup(QuestBook.Group group) {
		QuestBook book = QuestsState.book();
		if (book != null) {
			int members = 0;

			for (Chapter c : book.chapters.values()) {
				if (!c.readOnly && group.id.equals(c.group)) {
					members++;
				}
			}

			if (members == 0) {
				this.deleteGroup(group.id);
			} else {
				CFHGuiModal.confirmDanger(
					Tr.t("group.delete.title"), Tr.t("group.delete.body", QuestText.text(group.title), members), Tr.t("group.delete.ok"), () -> this.deleteGroup(group.id)
				);
			}
		}
	}

	int steps() {
		return this.work == null ? 0 : this.history().steps();
	}

	Map<String, Object> debug() {
		Map<String, Object> out = new HashMap<>();
		out.put("work", this.work == null ? "" : this.work.id);
		out.put("save", this.save());
		out.put("sent", this.sync == null ? 0 : this.sync.sent);
		out.put("acked", this.sync == null ? 0 : this.sync.acked);
		out.put("waiting", this.sync != null && this.sync.dirty);
		out.put("others", this.opsPending);
		out.put("quests", this.work == null ? 0 : this.work.quests.size());
		return out;
	}

	private static record Retry(long atMs, @Nullable Object server, Runnable run) {
	}

	static enum Save {
		SAVED,
		SAVING,
		REFUSED;
	}

	private static final class Sync {
		final String id;
		Chapter mine;
		@Nullable
		Chapter latest;
		String base;
		@Nullable
		String flying;
		boolean dirty;
		boolean dead;
		@Nullable
		Runnable then;
		int sent;
		int acked;
		@Nullable
		Component refusal;
		@Nullable
		final Object server = Minecraft.getInstance().getConnection();
		EditSession owner;

		Sync(EditSession owner, String id, Chapter mine, String base) {
			this.owner = owner;
			this.id = id;
			this.mine = mine;
			this.base = base;
		}

		boolean pending() {
			return this.flying != null || this.dirty;
		}
	}
}
