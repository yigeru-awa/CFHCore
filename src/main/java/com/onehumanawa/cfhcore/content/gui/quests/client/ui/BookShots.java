package com.onehumanawa.cfhcore.content.gui.quests.client.ui;

import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.client.DevHarness;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.quests.QuestsConfig;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestActions;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsClient;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.NodeView;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestCanvas;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.TreeFilter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Ids;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestState;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

final class BookShots {
	private static final List<BookShots.Due> LATER = new ArrayList<>();

	static void init() {
		if (DevHarness.sampleData()) {
			QuestsConfig.get().seenChapters.clear();
			QuestsConfig.get().pinned.clear();
			QuestsConfig.get().openTrees.clear();
			QuestsConfig.get().overview = false;
			CFHGuiEvents.CLIENT_TICK_END.register(BookShots::tick);

			for (int scale = 0; scale <= 5; scale++) {
				int to = scale;
				action("scale/" + to, () -> {
					Minecraft mc = Minecraft.getInstance();
					mc.options.guiScale().set(to);
					mc.resizeDisplay();
				});
			}

			action("overview/on", () -> QuestsConfig.get().overview = true);
			action("overview/off", () -> QuestsConfig.get().overview = false);
			String lode = "into_the_deep/ores";
			action("inline/keep", () -> {
				if (!QuestsConfig.get().openTrees.contains("into_the_deep/ores")) {
					QuestsConfig.get().openTrees.add("into_the_deep/ores");
				}
			});
			action("inline/forget", () -> QuestsConfig.get().openTrees.clear());
			inline("folded", "into_the_deep", List.of(), null, BookShots::first);
			inline("open", "into_the_deep", List.of("into_the_deep/ores"), null, BookShots::first);
			inline("fitted", "into_the_deep", List.of("into_the_deep/ores"), null, BookShots::whole);
			inline("opening", "into_the_deep", List.of(), null, s -> {
				first(s);
				later(500, () -> click(s, "into_the_deep/ores"));
			});
			inline("folding", "into_the_deep", List.of("into_the_deep/ores"), null, s -> {
				first(s);
				later(500, () -> click(s, "into_the_deep/ores"));
			});
			inline("cycle", "into_the_deep", List.of(), null, s -> {
				first(s);
				later(500, () -> click(s, "into_the_deep/ores"));
				later(2300, () -> click(s, "into_the_deep/ores"));
			});
			inline("twice", "into_the_deep", List.of(), null, s -> {
				first(s);
				later(500, () -> {
					click(s, "into_the_deep/ores");
					click(s, "into_the_deep/ores");
				});
			});
			inline("nested", "into_the_deep", List.of("into_the_deep/ores", "ores/lapis"), BookShots::nested, BookShots::whole);
			inline("nesting", "into_the_deep", List.of(), BookShots::nested, s -> {
				first(s);
				later(500, () -> click(s, "into_the_deep/ores"));
				later(1900, () -> click(s, "ores/lapis"));
			});
			inline("sheet", "into_the_deep", List.of(), null, s -> {
				first(s);
				later(500, () -> s.goTo("ores/iron"));
			});
			inline("end", "the_end", List.of(), null, s -> {
				first(s);
				later(500, () -> click(s, "the_end/cities"));
			});
			inline("card", "into_the_deep", List.of(), null, s -> {
				first(s);
				QuestCanvas c = s.canvas();
				if (c != null) {
					c.pointAt("into_the_deep/ores");
				}
			});
			inline("inner", "into_the_deep", List.of("into_the_deep/ores"), null, s -> {
				first(s);
				QuestCanvas c = s.canvas();
				if (c != null && c.focus("ores/coal")) {
					c.select(null);
					c.pointAt("ores/coal");
				}
			});
			inline("tab", "into_the_deep", List.of("into_the_deep/ores"), BookShots::tabbed, s -> {
				first(s);
				QuestCanvas c = s.canvas();
				if (c != null && c.focus("ores/coal")) {
					c.select(null);
					c.pointAt("ores/coal");
				}
			});
			inline("into", "into_the_deep", List.of("into_the_deep/ores"), BookShots::tabbed, s -> {
				first(s);
				later(500, () -> s.enterTree("ores/coal"));
			});
			book("search", null, s -> {
				s.hideIntro();
				s.demoSearch("pick");
			});
			inline("search", "into_the_deep", List.of(), null, s -> {
				first(s);
				s.demoSearch("Diamonds!");
			});
			book("filter", "first_steps", s -> {
				whole(s);
				s.filter(TreeFilter.TODO);
			});
			book("zoom", "first_steps", s -> {
				s.hideIntro();
				QuestCanvas c = s.canvas();
				if (c != null && c.focus("first_steps/furnace")) {
					c.select(null);
					c.zoomAbout(1.7F, (double)c.getX() + (double)c.getWidth() / 2.0, (double)c.getY() + (double)c.getHeight() / 2.0);
					c.zoomAbout(1.7F, (double)c.getX() + (double)c.getWidth() / 2.0, (double)c.getY() + (double)c.getHeight() / 2.0);
				}
			});
			book("unfold", "first_steps", s -> {
				s.hideIntro();
				QuestCanvas c = s.canvas();
				if (c != null) {
					c.pointAt("first_steps/tools");
				}
			});
			book("unfold2", "into_the_deep", s -> {
				s.hideIntro();
				QuestCanvas c = s.canvas();
				if (c != null) {
					c.pointAt("into_the_deep/ores");
				}
			});
			book("card", "first_steps", s -> {
				s.hideIntro();
				QuestCanvas c = s.canvas();
				if (c != null) {
					c.pointAt("first_steps/night");
				}
			});
			book("into", "first_steps", s -> {
				whole(s);
				later(500, () -> s.enterTree("first_steps/tools"));
			});
			book("out", "tools", s -> {
				whole(s);
				later(500, s::leaveTree);
			});
			book("done", "first_steps", s -> {
				whole(s);
				later(500, () -> QuestActions.complete("first_steps/iron", null, r -> {
					}));
			});
			book("challenge", "feats", s -> {
				whole(s);
				later(500, () -> QuestActions.complete("feats/crown", null, r -> {
					}));
			});
			book("chapter", "nether", s -> {
				whole(s);
				later(400, () -> completeChapter("nether"));
			});
			book("long", null, s -> s.goTo("feats/long_road"));
			book("rest", "field_and_kitchen", s -> QuestActions.complete("field_and_kitchen/market", null, r -> {
					QuestState state = QuestsState.state("field_and_kitchen/market");
					state.times = 3;
					state.readyAt = System.currentTimeMillis() + 754000L;
					s.goTo("field_and_kitchen/market");
					s.sheet().scrollToEnd();
				}));
			action("pin", () -> {
				List<String> pins = QuestsConfig.get().pinned;

				for (String key : List.of("tools/stone_pickaxe", "tools/wooden_shovel", "first_steps/iron")) {
					if (!pins.contains(key)) {
						QuestsState.pin(key, true);
					}
				}
			});
			action("progress", () -> QuestActions.submit("tools/stone_pickaxe", 0, r -> {
				}));
			action("feat", () -> {
				QuestActions.complete("feats/century", null, r -> {
				});
				QuestActions.complete("feats/hunted", null, r -> {
				});
			});
			action("finish", () -> completeChapter("the_end"));
			action("unpin", () -> {
				for (String key : new ArrayList<>(QuestsConfig.get().pinned)) {
					QuestsState.pin(key, false);
				}
			});
			CoreActions.SCREEN_FACTORIES.put("cfhgui_quests:shot/settings", QuestsUi::settingsScreen);
		}
	}

	private static void whole(QuestBookScreen s) {
		s.hideIntro();
		s.tree().fit();
	}

	private static void first(QuestBookScreen s) {
		s.hideIntro();
		QuestCanvas c = s.canvas();
		if (c != null) {
			c.lookAfresh();
		}
	}

	private static void book(String name, @Nullable String chapter, Consumer<QuestBookScreen> then) {
		CoreActions.SCREEN_FACTORIES.put("cfhgui_quests:shot/" + name, parent -> {
			CFHGuiToasts.clear();
			QuestsClient.target(chapter, null);
			Screen s = MenuSlots.create("cfhgui_quests:book", parent);
			if (s instanceof QuestBookScreen q) {
				q.onReady(then);
			}

			return s;
		});
	}

	private static void inline(String name, String chapter, List<String> open, @Nullable Runnable before, Consumer<QuestBookScreen> then) {
		CoreActions.SCREEN_FACTORIES.put("cfhgui_quests:shot/inline/" + name, parent -> {
			CFHGuiToasts.clear();
			if (before != null) {
				before.run();
			}

			QuestsConfig.get().openTrees.clear();
			QuestsConfig.get().openTrees.addAll(open);
			QuestsClient.target(chapter, null);
			Screen s = MenuSlots.create("cfhgui_quests:book", parent);
			if (s instanceof QuestBookScreen q) {
				q.onReady(then);
			}

			return s;
		});
	}

	private static void nested() {
		madeUp(
			"shot_gems",
			"Stones of the Deep",
			"A tree inside a tree inside a chapter",
			"minecraft:amethyst_shard",
			"#B48CFF",
			"end",
			"lapis",
			true,
			new String[][]{
				{"amethyst", "minecraft:amethyst_shard", "Ringing Stone"},
				{"quartz", "minecraft:quartz", "Pale Fire"},
				{"prism", "minecraft:prismarine_crystals", "Sea Glass"},
				{"echo", "minecraft:echo_shard", "What the Dark Kept"}
			}
		);
	}

	private static void tabbed() {
		madeUp(
			"shot_cellar",
			"The Coal Cellar",
			"A tree with a tab, inside a tree that opens in place",
			"minecraft:charcoal",
			"#FFB454",
			"nether",
			"coal",
			false,
			new String[][]{
				{"charcoal", "minecraft:charcoal", "Burnt Offerings"},
				{"campfire", "minecraft:campfire", "Camp Out"},
				{"lantern", "minecraft:lantern", "Light the Way"},
				{"smoker", "minecraft:smoker", "Smoke Signals"}
			}
		);
	}

	private static void madeUp(
		String id, String title, String subtitle, String icon, String accent, String backdrop, String behind, boolean inPlace, String[][] quests
	) {
		QuestBook book = QuestsState.book();
		Chapter ores = book == null ? null : book.chapter("ores");
		Quest way = ores == null ? null : ores.quest(behind);
		if (book != null && way != null && QuestsState.preview() && book.chapter(id) == null) {
			Chapter gems = new Chapter();
			gems.id = id;
			gems.title = title;
			gems.subtitle = subtitle;
			gems.icon = icon;
			gems.parent = ores.id;
			gems.accent = accent;
			gems.backdrop = backdrop;

			for (int i = 0; i < quests.length; i++) {
				Quest q = new Quest();
				q.id = quests[i][0];
				q.title = quests[i][2];
				q.x = i < 3 ? (float)i * 2.0F : 2.0F;
				q.y = i < 3 ? 0.0F : 1.8F;
				Task task = new Task();
				task.item = quests[i][1];
				task.count = 4L;
				q.tasks.add(task);
				if (i > 0) {
					q.deps.add(quests[i < 3 ? i - 1 : 1][0]);
				}

				gems.quests.add(q);
			}

			book.chapters.put(gems.id, gems.sanitize());
			way.subtree = gems.id;
			way.inline = inPlace;
			QuestsState.reindex();
		}
	}

	private static void click(QuestBookScreen s, String key) {
		QuestCanvas c = s.canvas();
		NodeView n = c == null ? null : c.graph().shown(key);
		if (n != null) {
			double x = (double)c.sx(n.x);
			double y = (double)c.sy(n.y);
			if (c.nodeAt(x, y) != n) {
				c.unfold(key, !c.unfolded(key));
			} else {
				s.mouseClicked(x, y, 0);
				s.mouseReleased(x, y, 0);
			}
		}
	}

	private static void action(String name, Runnable run) {
		CoreActions.SCREEN_FACTORIES.put("cfhgui_quests:shot/" + name, parent -> {
			run.run();
			return null;
		});
	}

	private static void completeChapter(String chapterId) {
		QuestBook book = QuestsState.book();
		Chapter c = book == null ? null : book.chapter(chapterId);
		if (c != null) {
			for (Quest q : c.quests) {
				QuestActions.complete(Ids.key(c, q), null, r -> {
				});
			}
		}
	}

	private static void later(int ms, Runnable run) {
		LATER.add(new BookShots.Due(Clock.nowMs() + (long)ms, run));
	}

	private static void tick() {
		if (!LATER.isEmpty()) {
			long now = Clock.nowMs();
			List<BookShots.Due> due = new ArrayList<>();

			for (BookShots.Due d : LATER) {
				if (d.atMs() <= now) {
					due.add(d);
				}
			}

			LATER.removeAll(due);

			for (BookShots.Due dx : due) {
				dx.run().run();
			}
		}
	}

	private BookShots() {
	}

	private static record Due(long atMs, Runnable run) {
	}
}
