package com.onehumanawa.cfhcore.content.gui.quests.client.ui;

import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestActions;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.hud.QuestsHud;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Ids;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import com.onehumanawa.cfhcore.content.gui.quests.model.Reward;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class StressBook {
	private static final int COUNT = Integer.getInteger("cfh.quests.stress", 0);
	private static final int NESTED = Integer.getInteger("cfh.quests.stressNested", 0);
	private static final boolean TALL = Boolean.getBoolean("cfh.quests.stressTall");
	private static boolean adding;

	static void init() {
		if (COUNT > 0 || NESTED > 0) {
			QuestsState.BOOK_CHANGED.register(StressBook::add);
		}
	}

	private static void add() {
		QuestBook book = QuestsState.book();
		if (!adding && book != null && QuestsState.preview() && !book.chapters.containsKey("stress")) {
			adding = true;
			QuestsHud.quiet(true);

			try {
				Chapter c = chapter(Math.max(8, COUNT));
				book.chapters.put(c.id, c);
				List<String> done = new ArrayList<>();

				for (int i = 0; i < c.quests.size() / 3; i++) {
					done.add(Ids.key(c, c.quests.get(i)));
				}

				Chapter parent = c;

				for (int depth = 1; depth <= NESTED; depth++) {
					Chapter sub = new Chapter();
					sub.id = "stress_deep_" + depth;
					sub.title = "Deeper " + depth;
					sub.subtitle = "A tree " + depth + " quests in";
					sub.parent = parent.id;
					sub.backdrop = new String[]{"cave", "ocean", "end", "parchment", "grid"}[depth % 5];
					sub.accent = new String[]{"#5BA8E0", "#9B86E0", "#E0A458", "#5CC8A8", "#E5698A"}[depth % 5];
					sub.icon = "minecraft:ender_pearl";

					for (int i = 0; i < 6; i++) {
						Quest q = quest("d" + i, (float)i * 1.6F, (float)(i % 2) * 1.4F - 0.7F);
						if (i > 0) {
							q.deps.add("d" + (i - 1));
						}

						sub.quests.add(q);
					}

					sub.sanitize();
					book.chapters.put(sub.id, sub);
					Quest way = parent.quests.get(parent == c ? 0 : 2);
					way.subtree = sub.id;
					way.shape = Quest.Shape.GEAR;
					way.size = 1.5F;
					parent = sub;
				}

				QuestsState.reindex();

				for (String key : done) {
					QuestActions.complete(key, null, r -> {
					});
				}

				CFHGuiQuests.LOGGER.info("[CFHGui Quests] stress: a chapter of {} quests, {} trees deep, was added to the preview's book", c.quests.size(), NESTED);
			} finally {
				QuestsHud.quiet(false);
				adding = false;
			}
		}
	}

	private static Chapter chapter(int n) {
		Chapter c = new Chapter();
		c.id = "stress";
		c.title = "A Thousand Quests";
		c.subtitle = n + " quests in one tree";
		c.icon = "minecraft:nether_star";
		c.group = "beyond";
		c.order = 99;
		c.accent = "#5BA8E0";
		c.backdrop = "stars";
		Random rnd = new Random(42L);
		int rows = Math.max(4, (int)Math.round(Math.sqrt((double)n / 2.4)));
		int cols = (int)Math.ceil((double)n / (double)rows);
		String[] items = new String[]{
			"minecraft:iron_ingot",
			"minecraft:diamond",
			"minecraft:oak_log",
			"minecraft:redstone",
			"minecraft:gold_ingot",
			"minecraft:emerald",
			"minecraft:coal",
			"minecraft:copper_ingot",
			"minecraft:bread",
			"minecraft:ender_pearl",
			"minecraft:blaze_rod",
			"minecraft:book"
		};
		Quest.Shape[] shapes = Quest.Shape.values();
		int made = 0;

		for (int col = 0; col < cols && made < n; col++) {
			for (int row = 0; row < rows && made < n; row++) {
				float along = (float)col * 1.5F;
				float across = (float)row * 1.5F + (float)(col % 2) * 0.35F;
				Quest q = quest("q" + col + "_" + row, TALL ? across : along, TALL ? along : across);
				q.title = "Quest " + (made + 1);
				Task t = new Task();
				t.item = items[rnd.nextInt(items.length)];
				t.count = (long)(1 + rnd.nextInt(16));
				q.tasks.clear();
				q.tasks.add(t);
				if (rnd.nextInt(9) == 0) {
					q.shape = shapes[rnd.nextInt(shapes.length)];
					q.size = 1.3F;
				}

				if (col > 0) {
					q.deps.add("q" + (col - 1) + "_" + row);
				}

				if (col > 0 && row > 0 && rnd.nextInt(4) == 0) {
					q.deps.add("q" + (col - 1) + "_" + (row - 1));
				}

				c.quests.add(q);
				made++;
			}
		}

		return c.sanitize();
	}

	private static Quest quest(String id, float x, float y) {
		Quest q = new Quest();
		q.id = id;
		q.title = "Quest " + id;
		q.x = x;
		q.y = y;
		Task t = new Task();
		t.item = "minecraft:stone";
		t.count = 8L;
		q.tasks.add(t);
		Reward r = new Reward();
		r.xp = 10;
		q.rewards.add(r);
		return q;
	}

	private StressBook() {
	}
}
