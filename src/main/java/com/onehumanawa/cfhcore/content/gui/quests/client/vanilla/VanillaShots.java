package com.onehumanawa.cfhcore.content.gui.quests.client.vanilla;

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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.jetbrains.annotations.Nullable;

final class VanillaShots {
	private static final List<VanillaShots.Due> LATER = new ArrayList<>();
	private static final Set<String> REGISTERED = new HashSet<>();

	static void init() {
		if (development()) {
			CFHGuiEvents.CLIENT_TICK_END.register(VanillaShots::tick);
			CoreActions.SCREEN_FACTORIES.putIfAbsent("minecraft:advancements", parent -> {
				ClientPacketListener connection = Minecraft.getInstance().getConnection();
				return (Screen)(connection == null ? parent : new AdvancementsScreen(connection.getAdvancements(), parent));
			});
			QuestsState.BOOK_CHANGED.register(VanillaShots::chapterIds);
			book("search", null, s -> s.search().demo("pick"));
			book("filter", "first_steps", s -> s.filter(TreeFilter.TODO));
			book("about", "first_steps", VanillaBookScreen::about);
			book("zoom", "first_steps", s -> {
				QuestCanvas c = s.canvas();
				if (c.focus("first_steps/furnace")) {
					c.select(null);
					c.zoomAbout(1.7F, (double)c.getX() + (double)c.getWidth() / 2.0, (double)c.getY() + (double)c.getHeight() / 2.0);
					c.zoomAbout(1.7F, (double)c.getX() + (double)c.getWidth() / 2.0, (double)c.getY() + (double)c.getHeight() / 2.0);
				}
			});
			book("unfold", "first_steps", s -> {
				s.canvas().focus("first_steps/tools");
				s.canvas().select(null);
			});
			book("into", "first_steps", s -> later(500, () -> s.enterTree("first_steps/tools")));
			book("done", "first_steps", s -> later(500, () -> QuestActions.complete("first_steps/iron", null, r -> {
					})));
			book("challenge", "feats", s -> later(500, () -> QuestActions.complete("feats/crown", null, r -> {
					})));
			book("choice", "first_steps", s -> {
				QuestState state = QuestsState.state("first_steps/bed");
				state.done = true;
				state.claimed = 0L;
				QuestsState.reindex();
				s.goTo("first_steps/bed");
			});
			book("long", null, s -> s.goTo("feats/long_road"));
			book("rest", "field_and_kitchen", s -> QuestActions.complete("field_and_kitchen/market", null, r -> {
					QuestState state = QuestsState.state("field_and_kitchen/market");
					state.times = 3;
					state.readyAt = System.currentTimeMillis() + 754000L;
					s.goTo("field_and_kitchen/market");
					s.page().scrollToEnd();
				}));
			String lode = "into_the_deep/ores";
			inline("folded", "into_the_deep", List.of(), s -> {
			});
			inline("card", "into_the_deep", List.of(), s -> s.canvas().pointAt("into_the_deep/ores"));
			inline("open", "into_the_deep", List.of("into_the_deep/ores"), s -> s.canvas().fit());
			inline("opening", "into_the_deep", List.of(), s -> later(500, () -> click(s, "into_the_deep/ores")));
			inline("folding", "into_the_deep", List.of("into_the_deep/ores"), s -> later(500, () -> click(s, "into_the_deep/ores")));
			inline("page", "into_the_deep", List.of(), s -> later(500, () -> {
					click(s, "into_the_deep/ores");
					click(s, "into_the_deep/ores");
				}));
			inline("goto", "into_the_deep", List.of(), s -> later(500, () -> s.goTo("ores/iron")));
			inline("end", "the_end", List.of(), s -> later(500, () -> click(s, "the_end/cities")));
			CoreActions.SCREEN_FACTORIES.put("cfhgui_quests:vanilla/advancement", parent -> {
				String chapter = AdvancementChapters.startChapter();
				if (chapter == null) {
					return parent;
				} else {
					QuestsClient.target(chapter, null);
					Screen s = MenuSlots.create("cfhgui_quests:book", parent);
					if (s instanceof VanillaBookScreen v) {
						v.onReady(screen -> {
							Chapter c = screen.shown();
							if (c != null && !c.quests.isEmpty()) {
								screen.canvas().focus(Ids.key(c, c.quests.get(0)));
								screen.canvas().select(null);
							}
						});
					}

					return s;
				}
			});
			CoreActions.SCREEN_FACTORIES.put("cfhgui_quests:vanilla/many", parent -> {
				many();
				QuestsClient.target("many_3_07", null);
				return MenuSlots.create("cfhgui_quests:book", parent);
			});
		}
	}

	private static boolean development() {
		return DevHarness.sampleData()
			|| Boolean.getBoolean("cfh.autoWorld")
			|| Boolean.getBoolean("cfh.quests.forcePreview")
			|| !System.getProperty("cfh.autoScreens", "").isBlank();
	}

	private static void book(String name, @Nullable String chapter, Consumer<VanillaBookScreen> then) {
		CoreActions.SCREEN_FACTORIES.put("cfhgui_quests:vanilla/" + name, parent -> {
			CFHGuiToasts.clear();
			QuestsClient.target(chapter, null);
			Screen s = MenuSlots.create("cfhgui_quests:book", parent);
			if (s instanceof VanillaBookScreen v) {
				v.onReady(then);
			}

			return s;
		});
	}

	private static void inline(String name, String chapter, List<String> open, Consumer<VanillaBookScreen> then) {
		CoreActions.SCREEN_FACTORIES.put("cfhgui_quests:vanilla/inline/" + name, parent -> {
			CFHGuiToasts.clear();
			QuestsConfig.get().openTrees.clear();
			QuestsConfig.get().openTrees.addAll(open);
			QuestsClient.target(chapter, null);
			Screen s = MenuSlots.create("cfhgui_quests:book", parent);
			if (s instanceof VanillaBookScreen v) {
				v.onReady(screen -> {
					screen.canvas().lookAfresh();
					then.accept(screen);
				});
			}

			return s;
		});
	}

	private static void click(VanillaBookScreen s, String key) {
		QuestCanvas c = s.canvas();
		NodeView n = c.graph().shown(key);
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

	private static void chapterIds() {
		QuestBook book = QuestsState.book();
		if (book != null) {
			for (Chapter c : book.chapters.values()) {
				if (REGISTERED.add(c.id + "#" + c.quests.size())) {
					CoreActions.SCREEN_FACTORIES.put("cfhgui_quests:book/" + c.id, p -> {
						QuestsClient.target(c.id, null);
						return MenuSlots.create("cfhgui_quests:book", p);
					});

					for (Quest q : c.quests) {
						String key = Ids.key(c, q);
						CoreActions.SCREEN_FACTORIES.put("cfhgui_quests:quest/" + key, p -> {
							QuestsClient.target(c.id, key);
							return MenuSlots.create("cfhgui_quests:book", p);
						});
					}
				}
			}
		}
	}

	private static void many() {
		QuestBook book = QuestsState.book();
		if (book != null && QuestsState.preview() && book.chapter("many_0_00") == null) {
			String[] icons = new String[]{
				"minecraft:oak_sapling",
				"minecraft:iron_pickaxe",
				"minecraft:wheat",
				"minecraft:diamond",
				"minecraft:blaze_rod",
				"minecraft:ender_eye",
				"minecraft:enchanting_table",
				"minecraft:brewing_stand",
				"minecraft:redstone",
				"minecraft:elytra",
				"minecraft:bell",
				"minecraft:trident"
			};
			String[] backdrops = new String[]{
				"minecraft:textures/gui/advancements/backgrounds/stone.png",
				"minecraft:textures/gui/advancements/backgrounds/nether.png",
				"minecraft:textures/gui/advancements/backgrounds/end.png",
				"minecraft:textures/gui/advancements/backgrounds/adventure.png",
				"minecraft:textures/gui/advancements/backgrounds/husbandry.png"
			};

			for (int g = 0; g < 8; g++) {
				QuestBook.Group group = new QuestBook.Group();
				group.id = "many_" + g;
				group.title = "Volume " + (g + 1) + " of a very long book indeed";
				group.icon = icons[g];
				book.groups.add(group);

				for (int i = 0; i < 24; i++) {
					Chapter c = new Chapter();
					c.id = "many_%d_%02d".formatted(g, i);
					c.title = i == 7 ? "A chapter whose name was written by somebody paid by the letter" : "Chapter " + (g * 24 + i + 1);
					c.subtitle = "One of very many";
					c.icon = icons[(g + i) % icons.length];
					c.group = group.id;
					c.order = 100 + g * 24 + i;
					c.backdrop = backdrops[g % backdrops.length];

					for (int q = 0; q < 12; q++) {
						Quest quest = new Quest();
						quest.id = "q" + q;
						quest.title = q == 5 ? "A quest with a remarkably long and winding title that goes on" : "Step " + (q + 1);
						quest.subtitle = "Made up, to fill the page";
						quest.icon = icons[(q + i) % icons.length];
						quest.x = (float)(q % 6) * 1.5F;
						quest.y = (float)(q / 6) * 2.0F + (float)(q % 2) * 0.75F;
						quest.frame = q % 5 == 4 ? Quest.Frame.CHALLENGE : (q % 3 == 2 ? Quest.Frame.GOAL : Quest.Frame.TASK);
						if (q > 0) {
							quest.deps.add("q" + (q - 1));
						}

						c.quests.add(quest);
					}

					c.sanitize();
					book.chapters.put(c.id, c);
				}
			}

			QuestsState.reindex();
		}
	}

	private static void later(int ms, Runnable run) {
		LATER.add(new VanillaShots.Due(Clock.nowMs() + (long)ms, run));
	}

	private static void tick() {
		if (!LATER.isEmpty()) {
			long now = Clock.nowMs();
			List<VanillaShots.Due> due = new ArrayList<>();

			for (VanillaShots.Due d : LATER) {
				if (d.atMs() <= now) {
					due.add(d);
				}
			}

			LATER.removeAll(due);

			for (VanillaShots.Due dx : due) {
				dx.run().run();
			}
		}
	}

	private VanillaShots() {
	}

	private static record Due(long atMs, Runnable run) {
	}
}
