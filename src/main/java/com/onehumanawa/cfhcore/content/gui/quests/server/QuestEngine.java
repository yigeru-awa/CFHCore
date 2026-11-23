package com.onehumanawa.cfhcore.content.gui.quests.server;

import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.logic.Rules;
import com.onehumanawa.cfhcore.content.gui.quests.model.BookJson;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Ids;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestState;
import com.onehumanawa.cfhcore.content.gui.quests.model.Reward;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import com.onehumanawa.cfhcore.content.gui.quests.net.QuestsNet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.UnaryOperator;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

final class QuestEngine {
	static final String NO_SUCH_TASK = "no_such_task";
	static final String NO_SUCH_PLAYER = "no_such_player";
	static final String TASK_LOCKED = "task_locked";
	static final String MISSING_FLUID = "missing_fluid";
	static final String MISSING_ENERGY = "missing_energy";
	static final String NO_ENERGY_HERE = "no_energy_here";
	static final String EMERGENCY_WAIT = "emergency_wait";
	static final String NO_EMERGENCY = "no_emergency";
	private static final long[] NOTHING = new long[0];
	private static final int STATES_PER_PAYLOAD = 1500;
	private static final int OBSERVE_EVERY = 5;
	private static final int ACTIONS_PER_SECOND = 60;
	private final World world;
	private final HolderStore store;
	private final Provider registries;
	private final Random random;
	private QuestBook book = new QuestBook();
	private BookIndex index = new BookIndex(this.book);
	private TriggerIndex triggers;
	@Nullable
	private byte[] packedHead;
	private final Map<String, byte[]> packedChapters = new HashMap<>();
	private final Map<UUID, Seat> seats = new LinkedHashMap<>();
	private final Set<Holder> active = new LinkedHashSet<>();
	private final ArrayDeque<Runnable> work = new ArrayDeque<>();
	private boolean working;
	private int ticks;
	@Nullable
	private Object datapacks;
	private final Map<TriggerIndex.Ref, Long> carried = new HashMap<>();
	private final Set<Holder> looked = new LinkedHashSet<>();
	private final Set<Holder> unsent = new LinkedHashSet<>();
	private final List<QuestEngine.Later> later = new ArrayList<>();
	private final List<QuestEngine.Told> telling = new ArrayList<>();

	QuestEngine(World world, HolderStore store, Provider registries, long seed) {
		this.world = world;
		this.store = store;
		this.registries = registries;
		this.random = new Random(seed);
		this.triggers = new TriggerIndex(this.index, registries);
	}

	QuestBook book() {
		return this.book;
	}

	BookIndex index() {
		return this.index;
	}

	TriggerIndex triggers() {
		return this.triggers;
	}

	HolderStore store() {
		return this.store;
	}

	Collection<Seat> seats() {
		return this.seats.values();
	}

	@Nullable
	Seat seat(UUID id) {
		return this.seats.get(id);
	}

	void book(QuestBook next) {
		boolean other = next != this.book;
		this.book = next;
		this.index = new BookIndex(next);
		this.triggers = new TriggerIndex(this.index, this.registries);
		if (other) {
			this.packedHead = null;
			this.packedChapters.clear();
		}

		this.work.clear();
		if (!this.triggers.unreadable.isEmpty()) {
			CFHGuiQuests.LOGGER
				.warn(
					"[CFHGui Quests] {} task(s) name something that cannot be read and will not be noticed by themselves: {}{}",
					new Object[]{
						this.triggers.unreadable.size(),
						String.join("; ", this.triggers.unreadable.subList(0, Math.min(12, this.triggers.unreadable.size()))),
						this.triggers.unreadable.size() > 12 ? "; ..." : ""
					}
				);
		}

		for (Holder h : this.active) {
			h.levels.clear();
			h.looking.clear();
			this.rest(h);
		}

		if (other) {
			for (Seat s : this.seats.values()) {
				this.sendBook(s);
			}
		}

		for (Seat s : this.seats.values()) {
			this.reopen(s);
		}

		this.run();
		if (other) {
			for (Seat s : this.seats.values()) {
				this.sendProgress(s);
			}
		}
	}

	void repack(String chapterId) {
		this.packedChapters.remove(chapterId);
		this.packedHead = null;
	}

	void tellAll(CustomPacketPayload payload) {
		for (Seat s : this.seats.values()) {
			this.world.send(s, payload);
		}
	}

	byte[] packedHead() {
		if (this.packedHead == null) {
			this.packedHead = JsonWire.encode(BookJson.GSON, this.book);
		}

		return this.packedHead;
	}

	byte[] packed(Chapter chapter) {
		return this.packedChapters.computeIfAbsent(chapter.id, id -> JsonWire.encode(BookJson.GSON, chapter));
	}

	Seat join(UUID id, String name, @Nullable ServerPlayer player) {
		this.leave(id);
		Seat s = new Seat(id, name, player, this);
		this.seats.put(id, s);
		boolean merged = this.bind(s);
		this.sendBook(s);
		this.reopen(s);
		if (merged) {
			for (Seat mate : new ArrayList<>(s.team.seats)) {
				if (mate != s) {
					this.reopen(mate);
				}
			}
		}

		this.settle(s);
		this.run();
		this.sendProgress(s);
		return s;
	}

	void leave(UUID id) {
		Seat s = this.seats.remove(id);
		if (s != null) {
			this.detach(s.own, s);
			if (s.team != s.own) {
				this.detach(s.team, s);
			}
		}
	}

	void leaveAll() {
		for (UUID id : new ArrayList<>(this.seats.keySet())) {
			this.leave(id);
		}
	}

	private boolean bind(Seat s) {
		s.own = this.store.get(s.id);
		UUID teamId = this.world.config().teamProgress ? this.world.teams().holderOf(s.id) : s.id;
		if (teamId == null) {
			teamId = s.id;
		}

		s.team = teamId.equals(s.id) ? s.own : this.store.get(teamId);
		this.attach(s.own, s);
		if (s.team == s.own) {
			return false;
		} else {
			this.attach(s.team, s);
			if (!s.team.members.add(s.id.toString())) {
				return false;
			} else {
				this.store.touch(s.team, this.world.now());
				if (!this.world.config().mergeOnJoin && !s.team.quests.isEmpty()) {
					return false;
				} else {
					this.merge(s.own, s.team, s.id);
					return true;
				}
			}
		}
	}

	private void attach(Holder h, Seat s) {
		h.seats.add(s);
		if (this.active.add(h)) {
			this.rest(h);
		}
	}

	private void detach(Holder h, Seat s) {
		h.seats.remove(s);
		if (h.seats.isEmpty()) {
			for (TriggerIndex.Ref r : h.levels.keySet()) {
				this.level(h, r, 0L, null);
			}

			h.levels.clear();
			h.looking.clear();
			h.changed.clear();
			this.unsent.remove(h);
			this.active.remove(h);
			this.store.release(h);
		}
	}

	void rebind(Seat s) {
		Holder oldOwn = s.own;
		Holder oldTeam = s.team;
		oldOwn.seats.remove(s);
		oldTeam.seats.remove(s);
		boolean merged = this.bind(s);

		for (Holder old : oldOwn == oldTeam ? List.of(oldOwn) : List.of(oldOwn, oldTeam)) {
			if (old != s.own && old != s.team && old.seats.isEmpty()) {
				old.seats.add(s);
				this.detach(old, s);
			}
		}

		this.rest(s.own);
		if (s.team != s.own) {
			this.rest(s.team);
		}

		this.reopen(s);
		if (merged) {
			for (Seat mate : new ArrayList<>(s.team.seats)) {
				if (mate != s) {
					this.reopen(mate);
				}
			}
		}

		this.settle(s);
		this.run();
		this.sendProgress(s);
	}

	private void rest(Holder h) {
		h.resting.clear();

		for (Entry<String, QuestProgress> en : h.quests.entrySet()) {
			if (en.getValue().done) {
				BookIndex.Entry e = this.index.get(en.getKey());
				if (e != null && repeats(e.quest())) {
					h.resting.add(en.getKey());
				}
			}
		}
	}

	private static boolean repeats(Quest q) {
		return q.repeatable && !q.tasks.isEmpty();
	}

	@Nullable
	QuestProgress peek(Seat s, String key) {
		BookIndex.Entry e = this.index.get(key);
		return e == null ? null : s.holder(e).get(key);
	}

	private List<Seat> covered(Holder h, BookIndex.Entry e) {
		List<Seat> out = new ArrayList<>(h.seats.size());

		for (Seat s : h.seats) {
			if (s.holder(e) == h) {
				out.add(s);
			}
		}

		return out;
	}

	private void touch(Holder h, String key) {
		this.store.touch(h, this.world.now());
		if (!h.seats.isEmpty()) {
			h.changed.add(key);
			this.unsent.add(h);
		}
	}

	private static Seat by(Seat s, BookIndex.Entry e, Seat doer) {
		return e.sync() == Quest.Sync.TEAM ? doer : s;
	}

	private void reopen(Seat s) {
		s.open.clear();
		s.looking.clear();
		s.observing.clear();
		s.gazes.clear();
		s.finished.clear();
		Map<String, Boolean> chapters = new HashMap<>();
		List<BookIndex.Entry> opening = new ArrayList<>();

		for (BookIndex.Entry e : this.index.all()) {
			QuestProgress p = s.holder(e).get(e.key());
			if (p == null || !p.done) {
				boolean chapterOpen = chapters.computeIfAbsent(e.chapter().id, id -> Rules.chapterOpen(this.index, e.chapter(), s.progress));
				if (chapterOpen && Rules.depsMet(this.index, e, s.progress)) {
					opening.add(e);
				}
			}
		}

		for (BookIndex.Entry ex : opening) {
			this.opened(s, ex, s);
		}
	}

	private void tryOpen(Seat s, String key, Seat doer) {
		if (!s.open.contains(key)) {
			BookIndex.Entry e = this.index.get(key);
			if (e != null) {
				QuestProgress p = s.holder(e).get(key);
				if (p == null || !p.done) {
					if (Rules.questOpen(this.index, e, s.progress)) {
						this.opened(s, e, doer);
					}
				}
			}
		}
	}

	private void openChapter(Seat s, String chapterId, Seat doer, Set<String> walked) {
		Chapter c = this.book.chapter(chapterId);
		if (c != null && walked.add(chapterId)) {
			for (Quest q : c.quests) {
				this.tryOpen(s, Ids.key(c, q), doer);
			}

			for (String tree : this.triggers.looseTrees(chapterId)) {
				this.openChapter(s, tree, doer, walked);
			}
		}
	}

	private void opened(Seat s, BookIndex.Entry e, Seat doer) {
		String key = e.key();
		if (s.open.add(key)) {
			TriggerIndex.Tasks tasks = this.triggers.tasks(key);
			if (tasks != null) {
				if (tasks.looked.length > 0) {
					s.looking.add(key);
				}

				if (tasks.observed.length > 0) {
					s.observing.add(key);
				}
			}

			Quest q = e.quest();
			Seat by = by(s, e, doer);
			if (q.tasks.isEmpty()) {
				if (!q.isGateway()) {
					this.finish(by, e);
				} else {
					Chapter tree = this.book.chapter(q.subtree);
					if (tree != null && Rules.chapterDone(this.index, tree, s.progress)) {
						this.finish(by, e);
					}
				}
			} else if (tasks != null) {
				for (TriggerIndex.Ref r : tasks.all) {
					if (r.task.is("quest")) {
						if (s.progress.everDone(Ids.resolve(e.chapter().id, r.task.quest))) {
							this.count(by, r, 1L, false);
						}
					} else if (r.task.is("chapter")) {
						Chapter c = this.book.chapter(r.task.chapter);
						if (c != null && Rules.chapterDone(this.index, c, s.progress)) {
							this.count(by, r, 1L, false);
						}
					}
				}

				QuestProgress p = s.holder(e).get(key);
				if (p != null && !p.done && Rules.tasksDone(q, p.tasks)) {
					this.finish(by, e);
				}
			}

			if (q.isGateway()) {
				this.work.add(() -> this.openChapter(s, q.subtree, doer, new HashSet<>()));
			}
		}
	}

	private void count(Seat doer, TriggerIndex.Ref ref, long value, boolean add) {
		Holder h = doer.holder(ref.entry);
		QuestProgress p = h.get(ref.key);
		if (p == null || !p.done) {
			long current = p == null ? 0L : p.task(ref.index);
			long max = ref.task.count;
			if (current < max) {
				Quest q = ref.entry.quest();
				if (!q.sequential || Rules.taskOpen(q, p == null ? NOTHING : p.tasks, ref.index)) {
					long next = Math.max(0L, Math.min(max, add ? current + value : value));
					if (next != current) {
						if (p == null) {
							p = h.progress(ref.key);
						}

						p.task(ref.index, next);
						this.begun(h, p, ref.entry, doer);
						this.touch(h, ref.key);
						if (next >= max && Rules.tasksDone(q, p.tasks)) {
							this.finish(doer, ref.entry);
						}
					}
				}
			}
		}
	}

	private void begun(Holder h, QuestProgress p, BookIndex.Entry e, Seat doer) {
		if (p.startedAt == 0L) {
			p.startedAt = Math.max(1L, this.world.now());
			List<String> waiting = this.index.dependents(e.key());
			if (!waiting.isEmpty()) {
				List<String> early = null;

				for (String key : waiting) {
					BookIndex.Entry d = this.index.get(key);
					if (d != null && d.quest().depsStarted) {
						if (early == null) {
							early = new ArrayList<>();
						}

						early.add(key);
					}
				}

				if (early != null) {
					List<String> keys = early;

					for (Seat s : this.covered(h, e)) {
						this.work.add(() -> {
							for (String keyx : keys) {
								this.tryOpen(s, keyx, doer);
							}
						});
					}
				}
			}
		}
	}

	private void happened(Seat s, TriggerIndex.Ref[] refs, long amount, @Nullable ItemStack stack) {
		if (refs.length != 0 && !s.open.isEmpty() && amount > 0L) {
			for (TriggerIndex.Ref r : refs) {
				if (s.open.contains(r.key) && (stack == null || r.item == null || r.item.carries(stack))) {
					this.count(s, r, amount, true);
				}
			}

			this.run();
		}
	}

	void killed(Seat s, EntityType<?> type) {
		this.killed(s, type, null);
	}

	void killed(Seat s, EntityType<?> type, @Nullable String name) {
		TriggerIndex.Ref[] refs = this.triggers.kills.of(type);
		if (refs.length != 0 && !s.open.isEmpty()) {
			for (TriggerIndex.Ref r : refs) {
				if (s.open.contains(r.key) && (r.task.name.isEmpty() || name != null && r.task.name.equalsIgnoreCase(name.trim()))) {
					this.count(s, r, 1L, true);
				}
			}

			this.run();
		}
	}

	void crafted(Seat s, ItemStack stack, int amount) {
		this.happened(s, this.triggers.crafted.of(stack.getItem()), (long)amount, stack);
	}

	void broke(Seat s, Block block) {
		this.happened(s, about(this.triggers.breaks, block), 1L, null);
	}

	void placed(Seat s, Block block) {
		this.happened(s, about(this.triggers.places, block), 1L, null);
	}

	private static TriggerIndex.Ref[] about(TriggerIndex.ByThing<Block> index, Block block) {
		TriggerIndex.Ref[] own = index.of(block);
		Block named = block.asItem() instanceof BlockItem item ? item.getBlock() : block;
		if (named == block) {
			return own;
		} else {
			TriggerIndex.Ref[] more = index.of(named);
			if (more.length == 0) {
				return own;
			} else if (own.length == 0) {
				return more;
			} else {
				Set<TriggerIndex.Ref> both = new LinkedHashSet<>(List.of(own));
				both.addAll(List.of(more));
				return both.toArray(new TriggerIndex.Ref[0]);
			}
		}
	}

	int custom(Seat s, String name, long amount) {
		int n = 0;

		for (TriggerIndex.Ref r : this.triggers.customs(name)) {
			if (s.open.contains(r.key)) {
				this.count(s, r, amount, true);
				n++;
			}
		}

		this.run();
		return n;
	}

	QuestEngine.Outcome task(Seat s, String key, String task, long amount) {
		BookIndex.Entry e = this.index.get(key);
		if (e == null) {
			return QuestEngine.Outcome.fail("no_such_quest");
		} else {
			TriggerIndex.Tasks tasks = this.triggers.tasks(key);
			TriggerIndex.Ref ref = null;
			if (tasks != null) {
				for (TriggerIndex.Ref r : tasks.all) {
					if (r.task.is("custom") && r.task.value.equals(task)) {
						ref = r;
						break;
					}
				}

				if (ref == null) {
					try {
						int n = Integer.parseInt(task.trim());
						if (n >= 1 && n <= tasks.all.length) {
							ref = tasks.all[n - 1];
						}
					} catch (NumberFormatException var13) {
					}
				}
			}

			if (ref == null) {
				return QuestEngine.Outcome.fail("no_such_task");
			} else if (!s.open.contains(key)) {
				return QuestEngine.Outcome.fail("not_open");
			} else {
				this.count(s, ref, Math.max(1L, amount), true);
				this.run();
				return QuestEngine.Outcome.OK;
			}
		}
	}

	void tick() {
		this.ticks++;
		if (!this.seats.isEmpty()) {
			int every = Math.max(1, this.world.config().checkIntervalTicks);
			int turn = this.ticks % every;
			this.lookFor(every, turn);
			if (this.ticks % 5 == 0) {
				this.observe(5);
			}

			if (this.ticks % 20 == 0) {
				this.rested();
			}

			this.flush();
		}

		this.store.tick(this.world.now());
	}

	void lookFor() {
		this.lookFor(1, 0);
	}

	private void lookFor(int every, int turn) {
		if (!this.seats.isEmpty()) {
			Object packs = this.world.datapacks();
			if (packs != this.datapacks) {
				this.datapacks = packs;
				this.triggers.forget();
			}

			boolean items = !this.triggers.held.isEmpty();
			this.looked.clear();

			for (Seat s : this.seats.values()) {
				if (every <= 1 || Math.floorMod(s.team.id.hashCode(), every) == turn) {
					this.looked.add(s.own);
					this.looked.add(s.team);
					if (!s.open.isEmpty()) {
						if (items) {
							this.carried.clear();
							this.world.inventory(s, stack -> {
								for (TriggerIndex.Ref rx : this.triggers.held.of(stack.getItem())) {
									if (s.open.contains(rx.key) && rx.item.carries(stack)) {
										this.carried.merge(rx, (long)stack.getCount(), Long::sum);
									}
								}
							});

							for (Entry<TriggerIndex.Ref, Long> c : this.carried.entrySet()) {
								best(s, c.getKey(), c.getValue());
							}
						}

						for (String key : s.looking) {
							TriggerIndex.Tasks tasks = this.triggers.tasks(key);
							if (tasks != null) {
								for (TriggerIndex.Ref r : tasks.looked) {
									this.look(s, r);
								}
							}
						}
					}
				}
			}

			if (!this.looked.isEmpty()) {
				for (Holder h : this.looked.toArray(new Holder[0])) {
					this.levels(h);
				}

				this.raise();
				this.run();
			}
		}
	}

	private void look(Seat s, TriggerIndex.Ref r) {
		Holder h = s.holder(r.entry);
		QuestProgress p = h.get(r.key);
		long current = p == null ? 0L : p.task(r.index);
		if (current < r.task.count) {
			String var7 = r.task.type;
			switch (var7) {
				case "stat":
					long vx = this.world.statistic(s, r.stat);
					if (vx > current) {
						this.later.add(new QuestEngine.Later(s, r, vx));
					}
					break;
				case "advancement":
					if (this.world.advancement(s, r.id, r.task.criterion)) {
						this.later.add(new QuestEngine.Later(s, r, 1L));
					}
					break;
				case "stage":
					if (this.world.stage(s, r.task.value)) {
						this.later.add(new QuestEngine.Later(s, r, 1L));
					}
					break;
				case "counter":
					long v = s.team.counter(r.task.value);
					if (v > current) {
						this.later.add(new QuestEngine.Later(s, r, v));
					}
					break;
				case "fluid":
					if (r.thing != null) {
						best(s, r, this.world.fluid(s, r.thing));
					}
					break;
				case "location":
					if (this.world.at(s, r)) {
						this.later.add(new QuestEngine.Later(s, r, 1L));
					}
					break;
				case "xp":
					best(s, r, (long)this.world.experience(s, r.task.points));
					break;
				case "team":
					best(s, r, this.world.teams().figure(s.team.id, r.task.value.isBlank() ? "members" : r.task.value));
			}
		}
	}

	private static void best(Seat s, TriggerIndex.Ref r, long value) {
		Holder h = s.holder(r.entry);
		Holder.Best b = h.looking.get(r);
		if (b == null) {
			h.looking.put(r, new Holder.Best(value, s));
		} else if (value > b.value) {
			b.value = value;
			b.seat = s;
		}
	}

	private void levels(Holder h) {
		if (!h.levels.isEmpty() || !h.looking.isEmpty()) {
			for (TriggerIndex.Ref r : h.levels.keySet()) {
				if (!h.looking.containsKey(r)) {
					this.level(h, r, 0L, null);
				}
			}

			for (Entry<TriggerIndex.Ref, Holder.Best> en : h.looking.entrySet()) {
				this.level(h, en.getKey(), en.getValue().value, en.getValue().seat);
			}

			Map<TriggerIndex.Ref, Holder.Best> last = h.levels;
			h.levels = h.looking;
			h.looking = last;
			last.clear();
		}
	}

	private void level(Holder h, TriggerIndex.Ref r, long value, @Nullable Seat seat) {
		QuestProgress p = h.get(r.key);
		if (p == null || !p.done) {
			long current = p == null ? 0L : p.task(r.index);
			long max = r.task.count;
			if (current < max) {
				Quest q = r.entry.quest();
				if (!q.sequential || Rules.taskOpen(q, p == null ? NOTHING : p.tasks, r.index)) {
					long next = Math.max(0L, Math.min(max, value));
					if (next != current) {
						if (p == null) {
							p = h.progress(r.key);
						}

						p.task(r.index, next);
						if (seat != null && next > 0L) {
							this.begun(h, p, r.entry, seat);
						}

						this.touch(h, r.key);
						if (next >= max && seat != null && Rules.tasksDone(q, p.tasks)) {
							this.finish(seat, r.entry);
						}
					}
				}
			}
		}
	}

	private void raise() {
		if (!this.later.isEmpty()) {
			for (QuestEngine.Later l : this.later) {
				if (l.seat.open.contains(l.ref.key)) {
					QuestProgress p = l.seat.holder(l.ref.entry).get(l.ref.key);
					if (l.value > (p == null ? 0L : p.task(l.ref.index))) {
						this.count(l.seat, l.ref, l.value, false);
					}
				}
			}

			this.later.clear();
		}
	}

	void observe(int step) {
		for (Seat s : this.seats.values()) {
			if (!s.observing.isEmpty()) {
				World.Sight sight = this.world.sight(s);

				for (String key : s.observing) {
					TriggerIndex.Tasks tasks = this.triggers.tasks(key);
					if (tasks != null) {
						for (TriggerIndex.Ref r : tasks.observed) {
							boolean seen = sight != null
								&& (
									r.block
										? sight.block() != null && r.thing.matches(BuiltInRegistries.BLOCK.wrapAsHolder(sight.block()), Registries.BLOCK)
										: sight.entity() != null && r.thing.matches(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(sight.entity()), Registries.ENTITY_TYPE)
								);
							if (!seen) {
								s.gazes.remove(r);
							} else {
								this.later.add(new QuestEngine.Later(s, r, (long)s.gazes.merge(r, step, Integer::sum).intValue()));
							}
						}
					}
				}
			}
		}

		this.raise();
		this.run();
	}

	void rested() {
		long now = this.world.now();
		List<Holder> resting = null;

		for (Holder h : this.active) {
			if (!h.resting.isEmpty()) {
				if (resting == null) {
					resting = new ArrayList<>();
				}

				resting.add(h);
			}
		}

		if (resting != null) {
			for (Holder hx : resting) {
				for (String key : new ArrayList<>(hx.resting)) {
					this.startOver(hx, key, now);
				}
			}

			this.run();
		}
	}

	private void startOver(Holder h, String key, long now) {
		BookIndex.Entry e = this.index.get(key);
		QuestProgress p = h.get(key);
		if (e == null || p == null || !p.done || !repeats(e.quest())) {
			h.resting.remove(key);
		} else if (now >= p.readyAt) {
			List<Seat> covered = this.covered(h, e);
			long atOnce = !e.quest().autoClaim && !e.quest().isAdvancement() ? atOnce(e.quest()) : -1L;

			for (Seat s : covered) {
				long waiting = this.unclaimed(s, e, p);
				if (waiting != 0L) {
					if ((waiting & ~atOnce) != 0L) {
						return;
					}

					this.handOut(s, e, h, p, atOnce);
				}
			}

			p.startOver();
			this.touch(h, key);
			h.resting.remove(key);

			for (Seat sx : covered) {
				this.work.add(() -> this.tryOpen(sx, key, sx));
			}
		}
	}

	private void finish(Seat doer, BookIndex.Entry e) {
		String key = e.key();
		Quest q = e.quest();
		Holder h = doer.holder(e);
		QuestProgress p = h.progress(key);
		if (!p.done) {
			long now = this.world.now();
			p.done = true;
			p.doneAt = now;
			p.doneBy = doer.name;
			p.doneById = doer.id.toString();
			p.times++;
			p.readyAt = q.repeatable && q.cooldownSeconds > 0 ? now + (long)q.cooldownSeconds * 1000L : 0L;
			this.touch(h, key);
			boolean first = p.times == 1;
			List<Seat> covered = this.covered(h, e);

			for (Seat s : covered) {
				s.open.remove(key);
				s.looking.remove(key);
				s.observing.remove(key);
			}

			if (repeats(q)) {
				h.resting.add(key);
			}

			if (p.startedAt == 0L) {
				p.startedAt = now;
			}

			long atOnce = !q.autoClaim && !q.isAdvancement() ? atOnce(q) : -1L;
			if (atOnce != 0L) {
				this.handOut(doer, e, h, p, atOnce);

				for (Seat s : covered) {
					if (s != doer) {
						this.handOut(s, e, h, p, atOnce);
					}
				}
			}

			for (String dep : e.deps()) {
				BookIndex.Entry fork = this.index.get(dep);
				if (fork != null && fork.quest().maxDependents > 0) {
					for (String other : this.index.dependents(dep)) {
						if (!other.equals(key)) {
							BookIndex.Entry o = this.index.get(other);
							if (o != null) {
								for (Seat sx : covered) {
									if (sx.open.contains(other) && Rules.shut(this.index, o, sx.progress)) {
										sx.open.remove(other);
										sx.looking.remove(other);
										sx.observing.remove(other);
									}
								}
							}
						}
					}
				}
			}

			for (Seat sxx : covered) {
				this.telling.add(new QuestEngine.Told(sxx, new QuestsNet.Happened("quest", key, doer.name)));
			}

			this.announce(doer, e, h, covered);
			TeamLink teams = this.world.teams();
			if (e.sync() == Quest.Sync.TEAM && teams.isTeam(h.id)) {
				teams.addXp(h.id, q.teamXp >= 0 ? (long)q.teamXp : (long)this.world.config().defaultTeamXp);
				if (first) {
					teams.count(h.id, "quests", 1L);
				}

				teams.log(h.id, "quest", doer.id, doer.name, ServerText.plainTitle(e));
			}

			this.world.questDone(doer, e);
			if (first) {
				for (Seat sxx : covered) {
					this.work.add(() -> this.follow(sxx, e, doer));
				}
			}
		}
	}

	private void follow(Seat s, BookIndex.Entry e, Seat doer) {
		String key = e.key();

		for (String dependent : this.index.dependents(key)) {
			this.tryOpen(s, dependent, doer);
		}

		for (TriggerIndex.Ref r : this.triggers.questTasks(key)) {
			if (s.open.contains(r.key)) {
				this.count(by(s, r.entry, doer), r, 1L, false);
			}
		}

		for (String chapterId : this.triggers.requiredBy(key)) {
			Chapter c = this.book.chapter(chapterId);
			if (c != null && Rules.chapterOpen(this.index, c, s.progress)) {
				if (!c.isSubtree()) {
					this.telling.add(new QuestEngine.Told(s, new QuestsNet.Happened("unlocked", c.id, doer.name)));
				}

				this.openChapter(s, chapterId, doer, new HashSet<>());
			}
		}

		Chapter chapter = e.chapter();
		if (!e.quest().optional && Rules.chapterDone(this.index, chapter, s.progress) && s.finished.add(chapter.id)) {
			this.chapterDone(s, chapter, doer);
		}
	}

	private void chapterDone(Seat s, Chapter chapter, Seat doer) {
		this.telling.add(new QuestEngine.Told(s, new QuestsNet.Happened("chapter", chapter.id, doer.name)));
		this.world.chapterDone(s, chapter);
		TeamLink teams = this.world.teams();
		if (s == doer && chapter.sync == Quest.Sync.TEAM && teams.isTeam(s.team.id)) {
			teams.count(s.team.id, "chapters", 1L);
			teams.log(s.team.id, "chapter", doer.id, doer.name, ServerText.plain(chapter.title.isBlank() ? chapter.id : chapter.title));
		}

		for (String gateway : this.index.gateways(chapter.id)) {
			BookIndex.Entry g = this.index.get(gateway);
			if (g != null && g.quest().tasks.isEmpty() && s.open.contains(gateway)) {
				this.finish(by(s, g, doer), g);
			}
		}

		for (TriggerIndex.Ref r : this.triggers.chapterTasks(chapter.id)) {
			if (s.open.contains(r.key)) {
				this.count(by(s, r.entry, doer), r, 1L, false);
			}
		}
	}

	private void run() {
		if (!this.working) {
			this.working = true;

			try {
				int n = 0;

				Runnable r;
				while ((r = this.work.poll()) != null) {
					r.run();
					if (++n > 2000000) {
						CFHGuiQuests.LOGGER.error("[CFHGui Quests] the book keeps setting quests off without end; giving up on this round");
						this.work.clear();
						break;
					}
				}
			} finally {
				this.working = false;
			}
		}
	}

	private void announce(Seat doer, BookIndex.Entry e, Holder h, List<Seat> covered) {
		Quest q = e.quest();
		if (q.isAdvancement()) {
			if (this.world.config().announceAdvancementQuests && this.world.announcesAdvancements()) {
				String frame = q.frame == Quest.Frame.CHALLENGE ? "challenge" : (q.frame == Quest.Frame.GOAL ? "goal" : "task");
				ChatFormatting color = q.frame == Quest.Frame.CHALLENGE ? ChatFormatting.DARK_PURPLE : ChatFormatting.GREEN;
				this.world
					.broadcast(Component.translatable("chat.type.advancement." + frame, new Object[]{this.world.displayName(doer), ServerText.badge(this.book, q, color)}));
			}
		} else if (!q.silent) {
			TeamLink teams = this.world.teams();
			if (e.sync() == Quest.Sync.TEAM && teams.isTeam(h.id) && this.world.config().announceTeamQuests && teams.figure(h.id, "members") > 1L) {
				Component line = ServerText.msg(
					"cfhgui_quests.announce.team", "%s completed the quest %s", this.world.displayName(doer), ServerText.badge(this.book, q, ChatFormatting.YELLOW)
				);

				for (Seat s : covered) {
					this.world.tell(s, line);
				}
			} else {
				for (Seat s : covered) {
					if (!this.world.hasClient(s)) {
						this.world.tell(s, ServerText.msg("cfhgui_quests.announce.done", "Quest complete: %s", ServerText.badge(this.book, q, ChatFormatting.YELLOW)));
					}
				}
			}
		}
	}

	private QuestState state(Seat s, BookIndex.Entry e, QuestProgress p) {
		QuestState st = new QuestState(e.key());
		st.tasks = (long[])p.tasks.clone();
		st.done = p.done;
		st.doneAt = p.doneAt;
		st.doneBy = p.doneBy;
		st.times = p.times;
		st.readyAt = p.readyAt;
		st.started = p.startedAt != 0L;
		long team = teamRewards(e.quest());
		st.claimed = p.claimsOf(s.id) & ~team | p.teamClaims & team;
		return st;
	}

	private static long teamRewards(Quest q) {
		long bits = 0L;

		for (int i = 0; i < q.rewards.size() && i < 64; i++) {
			if (q.rewards.get(i).team) {
				bits |= 1L << i;
			}
		}

		return bits;
	}

	void sendBook(Seat s) {
		if (this.world.hasClient(s)) {
			this.world.send(s, new QuestsNet.BookStart(this.packedHead(), this.book.chapters.size(), this.world.mayEdit(s)));

			for (Chapter c : this.book.chapters.values()) {
				this.world.send(s, new QuestsNet.ChapterData(c.id, this.packed(c)));
			}

			this.world.send(s, new QuestsNet.BookEnd(this.book.revision));
			this.world.bookSent(s);
		}
	}

	void sendProgress(Seat s) {
		if (this.world.hasClient(s)) {
			List<QuestState> out = new ArrayList<>();

			for (Holder h : s.own == s.team ? List.of(s.own) : List.of(s.own, s.team)) {
				for (Entry<String, QuestProgress> en : h.quests.entrySet()) {
					BookIndex.Entry e = this.index.get(en.getKey());
					if (e != null && s.holder(e) == h && en.getValue().any()) {
						out.add(this.state(s, e, en.getValue()));
					}
				}
			}

			boolean reset = true;

			for (int from = 0; reset || from < out.size(); from += 1500) {
				List<QuestState> part = out.subList(Math.min(from, out.size()), Math.min(out.size(), from + 1500));
				this.world.send(s, new QuestsNet.Progress(reset, Wire.pack(part.toArray(new QuestState[0]))));
				reset = false;
			}
		}
	}

	void sync(Seat s) {
		this.sendBook(s);
		this.sendProgress(s);
	}

	void flush() {
		if (!this.unsent.isEmpty()) {
			for (Holder h : this.unsent) {
				if (!h.changed.isEmpty()) {
					for (Seat s : h.seats) {
						if (this.world.hasClient(s)) {
							List<QuestState> out = new ArrayList<>(h.changed.size());

							for (String key : h.changed) {
								BookIndex.Entry e = this.index.get(key);
								if (e != null && s.holder(e) == h) {
									QuestProgress p = h.get(key);
									out.add(p == null ? new QuestState(key) : this.state(s, e, p));
								}
							}

							if (!out.isEmpty()) {
								this.world.send(s, new QuestsNet.Progress(false, Wire.pack(out.toArray(new QuestState[0]))));
							}
						}
					}

					h.changed.clear();
				}
			}

			this.unsent.clear();
		}

		if (!this.telling.isEmpty()) {
			for (QuestEngine.Told t : this.telling) {
				if (this.seats.get(t.seat.id) == t.seat) {
					this.world.send(t.seat, t.payload);
				}
			}

			this.telling.clear();
		}
	}

	long unclaimed(Seat s, BookIndex.Entry e, QuestProgress p) {
		int n = Math.min(64, e.quest().rewards.size());
		if (n == 0) {
			return 0L;
		} else {
			long all = n == 64 ? -1L : (1L << n) - 1L;
			long team = teamRewards(e.quest());
			return all & ~(p.claimsOf(s.id) & ~team | p.teamClaims & team);
		}
	}

	static long atOnce(Quest q) {
		long bits = 0L;

		for (int i = 0; i < q.rewards.size() && i < 64; i++) {
			if (q.rewards.get(i).atOnce()) {
				bits |= 1L << i;
			}
		}

		return bits;
	}

	private void handOut(Seat s, BookIndex.Entry e, Holder h, QuestProgress p) {
		this.handOut(s, e, h, p, -1L);
	}

	private void handOut(Seat s, BookIndex.Entry e, Holder h, QuestProgress p, long which) {
		long waiting = this.unclaimed(s, e, p) & which;

		for (int i = 0; i < 64 && waiting >>> i != 0L; i++) {
			if ((waiting >>> i & 1L) != 0L) {
				this.grant(s, e, h, p, i, 0);
			}
		}
	}

	private void grant(Seat s, BookIndex.Entry e, Holder h, QuestProgress p, int i, int choice) {
		Reward r = e.quest().rewards.get(i);
		if (r.team) {
			p.teamClaims |= 1L << i;
		} else {
			p.claim(s.id, i);
		}

		this.touch(h, e.key());

		try {
			this.give(s, r, choice, 0, e.key());
		} catch (Exception var9) {
			CFHGuiQuests.LOGGER.error("[CFHGui Quests] reward {} of {} could not be handed to {}", new Object[]{i + 1, e.key(), s.name, var9});
		}
	}

	private void give(Seat s, Reward r, int choice, int depth, String quest) {
		String var6 = r.type;
		switch (var6) {
			case "choice":
				if (r.choices.isEmpty() || depth > 4) {
					return;
				}

				this.give(s, r.choices.get(choice >= 0 && choice < r.choices.size() ? choice : 0), 0, depth + 1, quest);
				break;
			case "all":
				if (depth > 4) {
					return;
				}

				for (Reward c : r.choices) {
					this.give(s, c, 0, depth + 1, quest);
				}
				break;
			case "random":
				if (r.choices.isEmpty() || depth > 4) {
					return;
				}

				long total = (long)Math.max(0, r.empty);

				for (Reward c : r.choices) {
					total += (long)Math.max(1, c.weight);
				}

				int draw = 0;

				for (int draws = Math.max(1, Math.min(64, r.rolls)); draw < draws; draw++) {
					long roll = (long)(this.random.nextDouble() * (double)total);
					Reward picked = null;

					for (Reward c : r.choices) {
						roll -= (long)Math.max(1, c.weight);
						if (roll < 0L) {
							picked = c;
							break;
						}
					}

					if (picked != null) {
						this.give(s, picked, 0, depth + 1, quest);
					}
				}
				break;
			case "team_xp":
				if (this.world.teams().isTeam(s.team.id)) {
					this.world.teams().addXp(s.team.id, (long)r.xp);
				}
				break;
			case "counter":
				this.counter(s, r.value, (long)r.count, false);
				break;
			case "item":
				if (r.bonus <= 0) {
					this.world.give(s, r, quest);
					return;
				}

				Reward more = new Reward();
				more.type = "item";
				more.item = r.item;
				more.count = (int)Math.min(2147483647L, (long)r.count + (long)this.random.nextInt(r.bonus + 1));
				this.world.give(s, more, quest);
				break;
			default:
				this.world.give(s, r, quest);
		}
	}

	long counter(Seat s, String name, long amount, boolean set) {
		String n = name == null ? "" : name.trim();
		if (n.isEmpty()) {
			return 0L;
		} else {
			Holder h = s.team;
			long next = Math.max(0L, set ? amount : h.counter(n) + amount);
			if (next == 0L) {
				h.counters.remove(n);
			} else {
				h.counters.put(n, next);
			}

			this.store.touch(h, this.world.now());
			return next;
		}
	}

	QuestEngine.Outcome emergency(Seat s) {
		if (this.book.emergency.isEmpty()) {
			return QuestEngine.Outcome.fail("no_emergency");
		} else {
			long now = this.world.now();
			long ready = s.own.emergencyAt + (long)this.book.emergencyCooldownSeconds * 1000L;
			if (s.own.emergencyAt != 0L && now < ready) {
				return QuestEngine.Outcome.fail("emergency_wait", Long.toString((ready - now + 999L) / 1000L));
			} else {
				s.own.emergencyAt = now;
				this.store.touch(s.own, now);

				for (Reward r : this.book.emergency) {
					try {
						this.give(s, r, 0, 0, "");
					} catch (Exception var9) {
						CFHGuiQuests.LOGGER.error("[CFHGui Quests] an emergency item could not be handed to {}", s.name, var9);
					}
				}

				this.run();
				return QuestEngine.Outcome.OK;
			}
		}
	}

	long emergencyWait(Seat s) {
		if (s.own.emergencyAt == 0L) {
			return 0L;
		} else {
			long left = s.own.emergencyAt + (long)this.book.emergencyCooldownSeconds * 1000L - this.world.now();
			return left <= 0L ? 0L : (left + 999L) / 1000L;
		}
	}

	private void settle(Seat s) {
		List<BookIndex.Entry> waiting = null;

		for (Holder h : s.own == s.team ? List.of(s.own) : List.of(s.own, s.team)) {
			for (Entry<String, QuestProgress> en : h.quests.entrySet()) {
				QuestProgress p = en.getValue();
				if (p.done) {
					BookIndex.Entry e = this.index.get(en.getKey());
					if (e != null && s.holder(e) == h) {
						long atOnce = !e.quest().autoClaim && !e.quest().isAdvancement() ? atOnce(e.quest()) : -1L;
						if (atOnce != 0L && (this.unclaimed(s, e, p) & atOnce) != 0L) {
							if (waiting == null) {
								waiting = new ArrayList<>();
							}

							waiting.add(e);
						}
					}
				}
			}
		}

		if (waiting != null) {
			for (BookIndex.Entry e : waiting) {
				Holder h = s.holder(e);
				QuestProgress p = h.get(e.key());
				if (p != null && p.done) {
					this.handOut(s, e, h, p, !e.quest().autoClaim && !e.quest().isAdvancement() ? atOnce(e.quest()) : -1L);
				}
			}
		}
	}

	QuestEngine.Outcome claim(Seat s, String key, int reward, int choice) {
		BookIndex.Entry e = this.index.get(key);
		if (e == null) {
			return QuestEngine.Outcome.fail("no_such_quest");
		} else {
			Holder h = s.holder(e);
			QuestProgress p = h.get(key);
			if (p != null && p.done) {
				List<Reward> rewards = e.quest().rewards;
				if (reward >= -1 && reward < Math.min(64, rewards.size())) {
					long waiting = this.unclaimed(s, e, p);
					if (reward >= 0) {
						if ((waiting >>> reward & 1L) == 0L) {
							return QuestEngine.Outcome.fail("already_claimed");
						}

						Reward r = rewards.get(reward);
						if (r.is("choice") && !r.choices.isEmpty() && (choice < 0 || choice >= r.choices.size())) {
							return QuestEngine.Outcome.fail("choose");
						}

						this.grant(s, e, h, p, reward, choice);
					} else {
						if (waiting == 0L) {
							return QuestEngine.Outcome.fail("nothing_to_claim");
						}

						if (this.claimWaiting(s, e, h, p, waiting, choice, false, false) == 0) {
							return QuestEngine.Outcome.fail("choose");
						}
					}

					this.claimed(h, key);
					this.run();
					return QuestEngine.Outcome.OK;
				} else {
					return QuestEngine.Outcome.fail("invalid");
				}
			} else {
				return QuestEngine.Outcome.fail("not_done");
			}
		}
	}

	private int claimWaiting(Seat s, BookIndex.Entry e, Holder h, QuestProgress p, long waiting, int choice, boolean sweeping, boolean pick) {
		List<Reward> rewards = e.quest().rewards;
		int given = 0;
		int answer = choice;

		for (int i = 0; i < 64 && waiting >>> i != 0L; i++) {
			if ((waiting >>> i & 1L) != 0L) {
				Reward r = rewards.get(i);
				if (!sweeping || !r.excluded) {
					if (!r.is("choice") || r.choices.isEmpty()) {
						this.grant(s, e, h, p, i, 0);
					} else if (answer >= 0 && answer < r.choices.size()) {
						this.grant(s, e, h, p, i, answer);
						answer = -1;
					} else {
						if (!pick) {
							continue;
						}

						this.grant(s, e, h, p, i, this.picked(r));
					}

					given++;
				}
			}
		}

		return given;
	}

	private int picked(Reward r) {
		long total = 0L;

		for (Reward c : r.choices) {
			total += (long)Math.max(1, c.weight);
		}

		long roll = (long)(this.random.nextDouble() * (double)total);

		for (int i = 0; i < r.choices.size(); i++) {
			roll -= (long)Math.max(1, r.choices.get(i).weight);
			if (roll < 0L) {
				return i;
			}
		}

		return 0;
	}

	private void claimed(Holder h, String key) {
		if (h.resting.contains(key)) {
			this.startOver(h, key, this.world.now());
		}
	}

	QuestEngine.Outcome claimAll(Seat s, boolean pick) {
		int given = 0;
		boolean asking = false;

		for (Holder h : s.own == s.team ? List.of(s.own) : List.of(s.own, s.team)) {
			for (String key : new ArrayList<>(h.quests.keySet())) {
				QuestProgress p = h.get(key);
				if (p != null && p.done) {
					BookIndex.Entry e = this.index.get(key);
					if (e != null && s.holder(e) == h && !e.quest().rewards.isEmpty()) {
						long waiting = this.unclaimed(s, e, p);
						if (waiting != 0L) {
							int n = this.claimWaiting(s, e, h, p, waiting, -1, true, pick);
							if (n == 0) {
								for (int i = 0; i < 64 && waiting >>> i != 0L; i++) {
									if ((waiting >>> i & 1L) != 0L && !e.quest().rewards.get(i).excluded) {
										asking = true;
									}
								}
							} else {
								given += n;
								this.claimed(h, key);
							}
						}
					}
				}
			}
		}

		this.run();
		return given == 0 ? QuestEngine.Outcome.fail(asking ? "choose" : "nothing_to_claim") : QuestEngine.Outcome.OK;
	}

	QuestEngine.Outcome submit(Seat s, String key, int task) {
		BookIndex.Entry e = this.index.get(key);
		if (e == null) {
			return QuestEngine.Outcome.fail("no_such_quest");
		} else {
			TriggerIndex.Ref r = this.triggers.ref(key, task);
			if (r == null) {
				return QuestEngine.Outcome.fail("invalid");
			} else if (!s.open.contains(key)) {
				return QuestEngine.Outcome.fail("not_open");
			} else {
				Task t = r.task;
				QuestProgress p = s.holder(e).get(key);
				long current = p == null ? 0L : p.task(task);
				long need = t.count - current;
				if (need <= 0L) {
					return QuestEngine.Outcome.OK;
				} else if (e.quest().sequential && !Rules.taskOpen(e.quest(), p == null ? NOTHING : p.tasks, task)) {
					return QuestEngine.Outcome.fail("task_locked");
				} else {
					if (t.is("checkmark")) {
						this.count(s, r, t.count, false);
					} else if (t.is("fluid") && t.consume) {
						long taken = r.thing == null ? 0L : this.world.drain(s, r.thing, need);
						if (taken <= 0L) {
							return QuestEngine.Outcome.fail("missing_fluid", Long.toString(need));
						}

						this.count(s, r, taken, true);
					} else if (t.is("energy")) {
						long taken = this.world.takeEnergy(s, need);
						if (taken < 0L) {
							return QuestEngine.Outcome.fail("no_energy_here");
						}

						if (taken == 0L) {
							return QuestEngine.Outcome.fail("missing_energy", Long.toString(need));
						}

						this.count(s, r, taken, true);
					} else if (t.is("item") && t.consume) {
						long takenx = r.item == null ? 0L : this.world.take(s, r.item, need);
						if (takenx <= 0L) {
							return QuestEngine.Outcome.fail("missing_items", Long.toString(need));
						}

						this.count(s, r, takenx, true);
					} else {
						if (!t.is("xp") || !t.consume) {
							return QuestEngine.Outcome.fail("not_manual");
						}

						long pay = Math.min(need, (long)this.world.experience(s, t.points));
						if (pay <= 0L) {
							return QuestEngine.Outcome.fail("missing_xp");
						}

						this.world.pay(s, (int)pay, t.points);
						this.count(s, r, pay, true);
					}

					this.run();
					return QuestEngine.Outcome.OK;
				}
			}
		}
	}

	boolean tooFast(Seat s) {
		if (this.ticks - s.actionsSince >= 20) {
			s.actionsSince = this.ticks;
			s.actions = 0;
		}

		return ++s.actions > 60;
	}

	QuestEngine.Outcome reset(Seat s, String target) {
		Set<Holder> touched = new LinkedHashSet<>();
		if ("all".equals(target)) {
			for (Holder h : s.own == s.team ? List.of(s.own) : List.of(s.own, s.team)) {
				if (!h.quests.isEmpty()) {
					h.quests.clear();
					touched.add(h);
				}
			}
		} else if (target.startsWith("chapter:")) {
			Chapter c = this.book.chapter(target.substring("chapter:".length()));
			if (c == null) {
				return QuestEngine.Outcome.fail("no_such_chapter");
			}

			for (BookIndex.Entry e : this.index.of(c)) {
				if (s.holder(e).quests.remove(e.key()) != null) {
					touched.add(s.holder(e));
				}
			}
		} else {
			BookIndex.Entry ex = this.index.get(target);
			if (ex == null) {
				return QuestEngine.Outcome.fail("no_such_quest");
			}

			if (s.holder(ex).quests.remove(target) != null) {
				touched.add(s.holder(ex));
			}
		}

		Set<Seat> again = new LinkedHashSet<>();
		again.add(s);

		for (Holder hx : touched) {
			this.store.touch(hx, this.world.now());
			hx.changed.clear();
			this.unsent.remove(hx);
			this.rest(hx);
			again.addAll(hx.seats);
		}

		for (Seat a : again) {
			this.reopen(a);
		}

		this.run();

		for (Seat a : again) {
			this.sendProgress(a);
		}

		return QuestEngine.Outcome.OK;
	}

	QuestEngine.Outcome complete(Seat s, String key) {
		BookIndex.Entry e = this.index.get(key);
		if (e == null) {
			return QuestEngine.Outcome.fail("no_such_quest");
		} else {
			Holder h = s.holder(e);
			QuestProgress p = h.progress(key);
			if (!p.done) {
				List<Task> tasks = e.quest().tasks;

				for (int i = 0; i < tasks.size(); i++) {
					p.task(i, tasks.get(i).count);
				}

				this.touch(h, key);
				this.finish(s, e);
				this.run();
			}

			return QuestEngine.Outcome.OK;
		}
	}

	Rules.Status status(Seat s, String key) {
		BookIndex.Entry e = this.index.get(key);
		return e == null ? Rules.Status.HIDDEN : Rules.status(this.index, e, s.progress);
	}

	void renamed(UnaryOperator<String> to) {
		this.store.rename(to);

		for (Holder h : this.active) {
			h.changed.clear();
		}

		this.unsent.clear();
		this.book(this.book);

		for (Seat s : this.seats.values()) {
			this.sendProgress(s);
		}
	}

	private void merge(Holder own, Holder team, UUID player) {
		int became = 0;

		for (Entry<String, QuestProgress> en : own.quests.entrySet()) {
			BookIndex.Entry e = this.index.get(en.getKey());
			if (e != null && e.sync() == Quest.Sync.TEAM && en.getValue().any()) {
				if (team.progress(en.getKey()).merge(en.getValue(), player)) {
					became++;
				}

				this.touch(team, en.getKey());
			}
		}

		if (became > 0) {
			this.world.teams().count(team.id, "quests", (long)became);
		}

		if (further(own.counters, team)) {
			this.store.touch(team, this.world.now());
		}

		this.rest(team);
	}

	private static boolean further(Map<String, Long> counters, Holder into) {
		boolean any = false;

		for (Entry<String, Long> c : counters.entrySet()) {
			if (c.getValue() > into.counter(c.getKey())) {
				into.counters.put(c.getKey(), c.getValue());
				any = true;
			}
		}

		return any;
	}

	int absorb(UUID holderId, Collection<Foreign.Quest> quests) {
		Holder h = this.store.get(holderId);
		int n = 0;

		for (Foreign.Quest q : quests) {
			BookIndex.Entry e = this.index.get(q.key());
			if (e != null) {
				List<Task> tasks = e.quest().tasks;
				QuestProgress p = h.progress(q.key());

				for (int i = 0; i < tasks.size(); i++) {
					long had = q.done() ? tasks.get(i).count : (i < q.tasks().length ? Math.min(tasks.get(i).count, Math.max(0L, q.tasks()[i])) : 0L);
					if (had > p.task(i)) {
						p.task(i, had);
					}
				}

				if (q.startedAt() != 0L && (p.startedAt == 0L || q.startedAt() < p.startedAt)) {
					p.startedAt = q.startedAt();
				}

				if (q.done() && !p.done) {
					p.done = true;
					p.doneAt = q.doneAt();
					p.times = Math.max(Math.max(1, q.times()), p.times);
					if (p.startedAt == 0L) {
						p.startedAt = Math.max(1L, q.doneAt());
					}
				}

				for (Entry<String, Long> c : q.claims().entrySet()) {
					p.claims.merge(c.getKey(), c.getValue(), (a, b) -> a | b);
				}

				p.teamClaims = p.teamClaims | q.teamClaims();
				if (!p.any()) {
					h.quests.remove(q.key());
				}

				n++;
			}
		}

		this.store.touch(h, this.world.now());
		if (h.seats.isEmpty()) {
			this.store.release(h);
			return n;
		} else {
			h.changed.clear();
			this.unsent.remove(h);
			this.rest(h);
			List<Seat> again = new ArrayList<>(h.seats);

			for (Seat s : again) {
				this.reopen(s);
			}

			this.run();

			for (Seat s : again) {
				this.sendProgress(s);
			}

			return n;
		}
	}

	void memberJoined(UUID team, UUID player) {
		Seat s = this.seats.get(player);
		if (s != null) {
			this.rebind(s);
		}
	}

	void memberLeft(UUID team, UUID player) {
		Holder t = team.equals(player) ? null : this.store.get(team);
		if (t != null && t.members.remove(player.toString())) {
			Holder own = this.store.get(player);
			Iterator<Entry<String, QuestProgress>> it = own.quests.entrySet().iterator();

			while (it.hasNext()) {
				BookIndex.Entry e = this.index.get(it.next().getKey());
				if (e != null && e.sync() == Quest.Sync.TEAM) {
					it.remove();
				}
			}

			if (this.world.config().keepOnLeave) {
				for (Entry<String, QuestProgress> en : t.quests.entrySet()) {
					BookIndex.Entry e = this.index.get(en.getKey());
					if (e != null && e.sync() == Quest.Sync.TEAM && en.getValue().any()) {
						own.quests.put(en.getKey(), en.getValue().copy(player));
					}
				}

				further(t.counters, own);
			}

			this.store.touch(t, this.world.now());
			this.store.touch(own, this.world.now());
			if (own.seats.isEmpty()) {
				this.store.release(own);
			}
		}

		Seat s = this.seats.get(player);
		if (s != null) {
			this.rebind(s);
		}

		if (t != null && t.seats.isEmpty()) {
			this.store.release(t);
		}
	}

	void disbanded(UUID team) {
		Holder t = this.store.peek(team);
		if (t != null) {
			for (Seat s : new ArrayList<>(t.seats)) {
				this.rebind(s);
			}

			this.active.remove(t);
		}

		this.store.delete(team);
	}

	private static record Later(Seat seat, TriggerIndex.Ref ref, long value) {
	}

	static record Outcome(boolean ok, String code, List<String> args) {
		static final QuestEngine.Outcome OK = new QuestEngine.Outcome(true, "ok", List.of());

		static QuestEngine.Outcome fail(String code, String... args) {
			return new QuestEngine.Outcome(false, code, List.of(args));
		}
	}

	private static record Told(Seat seat, CustomPacketPayload payload) {
	}
}
