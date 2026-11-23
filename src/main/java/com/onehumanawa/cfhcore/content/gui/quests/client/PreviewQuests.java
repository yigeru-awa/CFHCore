package com.onehumanawa.cfhcore.content.gui.quests.client;

import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.logic.Rules;
import com.onehumanawa.cfhcore.content.gui.quests.model.BookJson;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Ids;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestState;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import com.onehumanawa.cfhcore.content.gui.quests.net.QuestsNet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.function.UnaryOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

final class PreviewQuests {
	static void seed(QuestBook book, BookIndex index, final Map<String, QuestState> states) {
		Random rnd = new Random(20261001L);
		long now = System.currentTimeMillis();
		Rules.Progress progress = new Rules.Progress() {
			@Override
			public boolean done(String key) {
				QuestState s = states.get(key);
				return s != null && s.done;
			}

			@Override
			public long task(String key, int i) {
				QuestState s = states.get(key);
				return s == null ? 0L : s.task(i);
			}

			@Override
			public boolean claimable(String key) {
				return false;
			}
		};
		List<Chapter> order = new ArrayList<>(book.topChapters());

		for (Chapter c : book.chapters.values()) {
			if (!order.contains(c)) {
				order.add(c);
			}
		}

		int n = 0;

		for (Chapter cx : order) {
			float share = cx.isSubtree() ? 0.55F : Math.max(0.08F, 0.95F - 0.3F * (float)(n++));

			for (int pass = 0; pass < 2; pass++) {
				for (BookIndex.Entry e : index.of(cx)) {
					QuestState had = states.get(e.key());
					if ((had == null || !had.done) && Rules.questOpen(index, e, progress)) {
						Quest q = e.quest();
						QuestState s = new QuestState(e.key());
						s.tasks = new long[q.tasks.size()];
						float roll = rnd.nextFloat();
						if (roll < share && !q.tasks.isEmpty()) {
							for (int i = 0; i < s.tasks.length; i++) {
								s.tasks[i] = q.tasks.get(i).count;
							}

							s.done = true;
							s.doneAt = now - (long)(rnd.nextFloat() * 6.0F * 8.64E7F);
							s.doneBy = Minecraft.getInstance().getUser().getName();
							if (rnd.nextFloat() < 0.75F) {
								s.claimed = -1L;
							}
						} else if (roll < share + 0.25F && !q.tasks.isEmpty()) {
							Task t = q.tasks.get(0);
							s.tasks[0] = (long)Math.max(t.count > 1L ? 1 : 0, Math.round((float)t.count * (0.2F + 0.6F * rnd.nextFloat())));
							if (s.tasks[0] >= t.count) {
								s.tasks[0] = Math.max(0L, t.count - 1L);
							}
						} else if (q.tasks.isEmpty() && !q.isGateway()) {
							s.done = true;
							s.doneAt = now;
							s.claimed = -1L;
						}

						states.put(e.key(), s);
					}
				}
			}
		}

		settleGateways(index, states);
	}

	private static void settleGateways(BookIndex index, Map<String, QuestState> states) {
		Rules.Progress progress = PreviewQuests.QuestsStateAccess.progress(states);

		for (BookIndex.Entry e : index.all()) {
			Quest q = e.quest();
			if (q.isGateway() && q.tasks.isEmpty()) {
				Chapter sub = index.book().chapter(q.subtree);
				if (sub != null && Rules.chapterDone(index, sub, progress)) {
					QuestState s = states.computeIfAbsent(e.key(), QuestState::new);
					if (!s.done) {
						s.done = true;
						s.doneAt = System.currentTimeMillis();
					}
				}
			}
		}
	}

	static QuestsNet.Result play(int request, String op, CompoundTag args) {
		QuestBook book = QuestsState.book();
		BookIndex index = QuestsState.index();
		if (book != null && index != null) {
			Map<String, QuestState> states = QuestsState.states();
			switch (op) {
				case "claim":
					String key = args.getString("quest");
					BookIndex.Entry ex = index.get(key);
					if (ex == null) {
						return QuestsNet.Result.fail(request, "no_such_quest");
					} else {
						QuestState s = states.get(key);
						if (s != null && s.done) {
							int reward = args.contains("reward") ? args.getInt("reward") : -1;
							if (reward < 0) {
								s.claimed = -1L;
							} else {
								s.claimed |= 1L << reward;
							}

							QuestsState.progressChanged(List.of(key));
							return QuestsNet.Result.ok(request);
						}

						return QuestsNet.Result.fail(request, "not_done");
					}
				case "claim_all":
					List<String> changed = new ArrayList<>();

					for (BookIndex.Entry e : index.all()) {
						QuestState s = states.get(e.key());
						if (s != null && s.claimable(e.quest())) {
							s.claimed = -1L;
							changed.add(e.key());
						}
					}

					if (changed.isEmpty()) {
						return QuestsNet.Result.fail(request, "nothing_to_claim");
					}

					QuestsState.progressChanged(changed);
					return QuestsNet.Result.ok(request);
				case "submit":
				case "complete":
					String keyx = args.getString("quest");
					BookIndex.Entry exx = index.get(keyx);
					if (exx == null) {
						return QuestsNet.Result.fail(request, "no_such_quest");
					} else if (!Rules.questOpen(index, exx, QuestsState.progress()) && op.equals("submit")) {
						return QuestsNet.Result.fail(request, "not_open");
					} else {
						Quest q = exx.quest();
						QuestState s = states.computeIfAbsent(keyx, QuestState::new);
						if (s.tasks.length < q.tasks.size()) {
							s.tasks = Arrays.copyOf(s.tasks, q.tasks.size());
						}

						if (op.equals("complete")) {
							for (int i = 0; i < q.tasks.size(); i++) {
								s.tasks[i] = q.tasks.get(i).count;
							}
						} else {
							int task = args.getInt("task");
							if (task < 0 || task >= q.tasks.size()) {
								return QuestsNet.Result.fail(request, "invalid");
							}

							s.tasks[task] = q.tasks.get(task).count;
						}

						List<String> submitChanged = new ArrayList<>(List.of(keyx));
						if (!s.done && (Rules.tasksDone(q, s.tasks) || op.equals("complete"))) {
							finish(index, states, exx, submitChanged);
						}

						QuestsState.progressChanged(submitChanged);
						return QuestsNet.Result.ok(request);
					}
				case "reset":
					states.clear();
					QuestsState.overlayStates();
					QuestsState.progressChanged(List.of());
					return QuestsNet.Result.ok(request);
				case "edit_chapter":
					String idx = args.getString("id");
					Chapter c = (Chapter)JsonWire.decode(BookJson.GSON, args.getByteArray("chapter"), Chapter.class);
					if (c != null && Ids.valid(idx)) {
						c.id = idx;
						book.chapters.put(idx, c.sanitize());
						QuestsState.reindex();
						return QuestsNet.Result.ok(request);
					}

					return QuestsNet.Result.fail(request, "invalid");
				case "delete_chapter":
					if (book.chapters.remove(args.getString("id")) == null) {
						return QuestsNet.Result.fail(request, "no_such_chapter");
					}

					QuestsState.reindex();
					return QuestsNet.Result.ok(request);
				case "edit_head":
					QuestBook head = (QuestBook)JsonWire.decode(BookJson.GSON, args.getByteArray("book"), QuestBook.class);
					if (head == null) {
						return QuestsNet.Result.fail(request, "invalid");
					}

					head.sanitize();
					book.title = head.title;
					book.subtitle = head.subtitle;
					book.groups = head.groups;
					book.advancements = head.advancements;
					book.closed = head.closed;
					book.emergency = head.emergency;
					book.emergencyCooldownSeconds = head.emergencyCooldownSeconds;
					QuestsState.reindex();
					return QuestsNet.Result.ok(request);
				case "rename_chapter":
					String cid = args.getString("id");
					String cto = args.getString("to");
					if (!book.chapters.containsKey(cid)) {
						return QuestsNet.Result.fail(request, "no_such_chapter");
					} else if (!Ids.valid(cto)) {
						return QuestsNet.Result.fail(request, "invalid");
					} else if (cid.equals(cto)) {
						return QuestsNet.Result.ok(request);
					} else {
						if (book.chapters.containsKey(cto)) {
							return QuestsNet.Result.fail(request, "id_taken");
						}

						renameChapter(book, cid, cto);
						String from = cid + "/";
						String into = cto + "/";
						rekey(states, kk -> kk.startsWith(from) ? into + kk.substring(from.length()) : kk);
						QuestsState.reindex();
						return QuestsNet.Result.ok(request);
					}
				case "rename_quest":
					String qkey = args.getString("quest");
					String qto = args.getString("to");
					Chapter home = book.chapter(Ids.chapterOf(qkey));
					if (home != null && home.quest(Ids.questOf(qkey)) != null) {
						if (!Ids.valid(qto)) {
							return QuestsNet.Result.fail(request, "invalid");
						}

						if (Ids.questOf(qkey).equals(qto)) {
							return QuestsNet.Result.ok(request);
						}

						if (home.quest(qto) != null) {
							return QuestsNet.Result.fail(request, "id_taken");
						}

						renameQuest(book, qkey, qto);
						String renamed = Ids.key(home.id, qto);
						rekey(states, k -> k.equals(qkey) ? renamed : k);
						QuestsState.reindex();
						return QuestsNet.Result.ok(request);
					}

					return QuestsNet.Result.fail(request, "no_such_quest");
				case "sync":
				case "reload":
				case "emergency":
					return QuestsNet.Result.ok(request);
				default:
					return QuestsNet.Result.fail(request, "not_implemented");
			}
		} else {
			return QuestsNet.Result.fail(request, "disabled");
		}
	}

	private static void renameChapter(QuestBook book, String id, String to) {
		Chapter renamed = book.chapters.get(id);
		Map<String, Chapter> next = new LinkedHashMap<>();

		for (Entry<String, Chapter> e : book.chapters.entrySet()) {
			next.put(e.getKey().equals(id) ? to : e.getKey(), e.getValue());
		}

		book.chapters = next;
		renamed.id = to;
		String from = id + "/";
		String into = to + "/";

		for (Chapter c : book.chapters.values()) {
			if (id.equals(c.parent)) {
				c.parent = to;
			}

			c.requires.replaceAll(r -> r.startsWith(from) ? into + r.substring(from.length()) : r);
			c.description.replaceAll(l -> l.replace("{quest:" + from, "{quest:" + into));

			for (Quest q : c.quests) {
				if (id.equals(q.subtree)) {
					q.subtree = to;
				}

				q.deps.replaceAll(r -> r.startsWith(from) ? into + r.substring(from.length()) : r);
				q.description.replaceAll(l -> l.replace("{quest:" + from, "{quest:" + into));

				for (Task t : q.tasks) {
					if (t.quest.startsWith(from)) {
						t.quest = into + t.quest.substring(from.length());
					}

					if (id.equals(t.chapter)) {
						t.chapter = to;
					}
				}
			}
		}
	}

	private static void renameQuest(QuestBook book, String key, String to) {
		String chapterId = Ids.chapterOf(key);
		String old = Ids.questOf(key);
		String renamed = Ids.key(chapterId, to);
		Chapter home = book.chapter(chapterId);
		home.quest(old).id = to;

		for (Chapter c : book.chapters.values()) {
			boolean same = c == home;
			c.requires.replaceAll(r -> r.equals(key) ? renamed : (same && r.equals(old) ? to : r));
			c.description.replaceAll(l -> l.replace("{quest:" + key + "}", "{quest:" + renamed + "}"));

			for (Quest q : c.quests) {
				q.deps.replaceAll(r -> r.equals(key) ? renamed : (same && r.equals(old) ? to : r));
				q.description.replaceAll(l -> l.replace("{quest:" + key + "}", "{quest:" + renamed + "}"));

				for (Task t : q.tasks) {
					if (t.quest.equals(key)) {
						t.quest = renamed;
					} else if (same && t.quest.equals(old)) {
						t.quest = to;
					}
				}
			}
		}
	}

	private static void rekey(Map<String, QuestState> states, UnaryOperator<String> rename) {
		List<QuestState> moved = new ArrayList<>();
		Iterator<Entry<String, QuestState>> it = states.entrySet().iterator();

		while (it.hasNext()) {
			Entry<String, QuestState> e = it.next();
			String now = rename.apply(e.getKey());
			if (!now.equals(e.getKey())) {
				e.getValue().key = now;
				moved.add(e.getValue());
				it.remove();
			}
		}

		for (QuestState s : moved) {
			states.put(s.key, s);
		}
	}

	private static void finish(BookIndex index, Map<String, QuestState> states, BookIndex.Entry e, List<String> changed) {
		String me = Minecraft.getInstance().getUser().getName();
		QuestState s = states.computeIfAbsent(e.key(), QuestState::new);
		s.done = true;
		s.doneAt = System.currentTimeMillis();
		s.doneBy = me;
		if (e.quest().autoClaim || e.quest().isAdvancement()) {
			s.claimed = -1L;
		}

		QuestsState.happened(new QuestsState.Happening("quest", e.key(), me));
		Rules.Progress progress = PreviewQuests.QuestsStateAccess.progress(states);
		if (Rules.chapterDone(index, e.chapter(), progress)) {
			QuestsState.happened(new QuestsState.Happening("chapter", e.chapter().id, me));

			for (String gateway : index.gateways(e.chapter().id)) {
				BookIndex.Entry g = index.get(gateway);
				QuestState gs = states.get(gateway);
				if (g != null && g.quest().tasks.isEmpty() && (gs == null || !gs.done)) {
					changed.add(gateway);
					finish(index, states, g, changed);
				}
			}
		}

		for (String next : index.dependents(e.key())) {
			BookIndex.Entry d = index.get(next);
			QuestState ds = states.get(next);
			if (d != null && (ds == null || !ds.done) && d.quest().tasks.isEmpty() && !d.quest().isGateway() && Rules.questOpen(index, d, progress)) {
				changed.add(next);
				finish(index, states, d, changed);
			}
		}
	}

	private PreviewQuests() {
	}

	private static final class QuestsStateAccess {
		static Rules.Progress progress(final Map<String, QuestState> states) {
			return new Rules.Progress() {
				@Override
				public boolean done(String key) {
					QuestState s = states.get(key);
					return s != null && s.done;
				}

				@Override
				public long task(String key, int i) {
					QuestState s = states.get(key);
					return s == null ? 0L : s.task(i);
				}

				@Override
				public boolean claimable(String key) {
					return false;
				}
			};
		}
	}
}
