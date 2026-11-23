package com.onehumanawa.cfhcore.content.gui.quests.client.editor;

import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.client.DevHarness;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsClient;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.client.ui.QuestBookScreen;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestCanvas;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

final class EditorShots {
	private static final List<EditorShots.Due> LATER = new ArrayList<>();
	@Nullable
	private static QuestBook named;

	static void init() {
		if (DevHarness.sampleData() || Boolean.getBoolean("cfh.quests.editorShots")) {
			CFHGuiEvents.CLIENT_TICK_END.register(EditorShots::tick);
			QuestsState.BOOK_CHANGED.register(EditorShots::chapters);
			chapters();
			shot("book", "first_steps", s -> EditSession.bookTab = true);
			shot("several", "first_steps", s -> s.selection.set(List.of("stone", "torches", "furnace", "meal"), List.of()));
			shot("label", "first_steps", s -> {
				if (s.work() != null && !s.work().decorations.isEmpty()) {
					s.selection.onlyDecoration(0);
				}
			});
			shot("line", "first_steps", s -> s.selection.link("table", "stone"));
			shot("link", "first_steps", s -> s.tool.demoLink("stone"));
			shot("drag", "first_steps", s -> {
				QuestCanvas canvas = s.canvas();
				Quest q = s.quest("torches");
				if (canvas != null && q != null) {
					s.selection.only("torches");
					s.tool.demoDrag(canvas, "torches", q.x + 1.02F, q.y + 0.6F);
				}
			});
			shot("band", "first_steps", s -> {
				QuestCanvas canvas = s.canvas();
				if (canvas != null) {
					s.tool.demoBand(canvas, 3.2F, -2.6F, 7.6F, 0.7F);
				}
			});
			shot("menu", "first_steps", s -> {
				QuestCanvas canvas = s.canvas();
				Quest q = s.quest("stone");
				if (canvas != null && q != null) {
					s.selection.only("stone");
					EditorMenus.open(s, EditorTool.Hit.quest("stone", false), (double)(canvas.sx(q.x) + 8.0F), (double)(canvas.sy(q.y) + 6.0F), q.x, q.y);
				}
			});
			shot("task", "first_steps", s -> {
				s.selection.only("logs");
				Quest q = s.quest("logs");
				if (q != null && !q.tasks.isEmpty()) {
					new TaskDialog(q.tasks.get(0), false, t -> {
					}).show();
				}
			});
			shot("task-location", "first_steps", s -> {
				Task t = Kinds.task("location");
				t.dimension = "minecraft:the_nether";
				t.structure = "minecraft:fortress";
				t.x = 120;
				t.y = 64;
				t.z = -340;
				t.radius = 24;
				new TaskDialog(t, true, x -> {
				}).show();
			});
			shot("reward", "first_steps", s -> {
				s.selection.only("bed");
				Quest q = s.quest("bed");
				if (q != null && !q.rewards.isEmpty()) {
					new RewardDialog(q.rewards.get(0), false, false, 0, r -> {
					}).show();
				}
			});
			shot("text", "first_steps", s -> {
				s.selection.only("logs");
				Quest q = s.quest("logs");
				if (q != null) {
					new TextDialog(Tr.t("text.of", QuestText.title(q)), q.description, lines -> {
					}).show();
				}
			});
			shot("items", "first_steps", s -> {
				s.selection.only("logs");
				Pickers.open(Pickers.What.ICON, "minecraft:oak_log", v -> {
				});
			});
			shot("glyphs", "first_steps", s -> {
				s.selection.only("logs");
				Pickers.open(Pickers.What.ICON, "icon:trophy", v -> {
				});
			});
			shot("mobs", "first_steps", s -> Pickers.open(Pickers.What.MOB, "minecraft:zombie", v -> {
				}));
			shot("stats", "first_steps", s -> Pickers.open(Pickers.What.STAT, "minecraft:custom/minecraft:jump", v -> {
				}));
			shot("blocks", "first_steps", s -> Pickers.open(Pickers.What.BLOCK, "minecraft:furnace", v -> {
				}));
			shot("tags", "first_steps", s -> Pickers.open(Pickers.What.ITEM, "#minecraft:logs", v -> {
				}));
			shot("advancements", "first_steps", s -> Pickers.open(Pickers.What.ADVANCEMENT, "minecraft:story/mine_stone", v -> {
				}));
			shot("biomes", "first_steps", s -> Pickers.open(Pickers.What.BIOME, "minecraft:plains", v -> {
				}));
			shot("structures", "first_steps", s -> Pickers.open(Pickers.What.STRUCTURE, "minecraft:fortress", v -> {
				}));
			shot("loot", "first_steps", s -> Pickers.open(Pickers.What.LOOT, "minecraft:chests/simple_dungeon", v -> {
				}));
			shot("quests", "first_steps", s -> Pickers.open(Pickers.What.QUEST, "first_steps/stone", v -> {
				}));
			shot("chapter-look", "first_steps", s -> {
				Form.foldOnly("chapter", "chapter.text");
				s.panel.invalidate();
			});
			shot("color", "first_steps", s -> {
				Form.foldOnly("chapter", "chapter.text");
				s.panel.invalidate();
				later(300, () -> s.panel.pressForTest("chapter.accent"));
			});
			shot("quest-lists", "first_steps", s -> {
				Form.foldOnly("quest", "tasks", "text", "look", "behaviour", "place");
				s.selection.only("iron");
			});
			shot("quest-behaviour", "first_steps", s -> {
				Form.foldOnly("quest", "text", "tasks", "rewards", "look", "deps", "tree");
				s.selection.only("bed");
			});
			shot("gateway", "first_steps", s -> {
				Form.foldOnly("quest", "text", "look", "rewards", "behaviour", "place");
				s.selection.only("tools");
			});
			shot("inline", "into_the_deep", s -> {
				Form.foldOnly("quest", "tasks", "rewards", "text", "look", "deps", "behaviour", "place");
				s.selection.only("ores");
			});
			shot("folded", "first_steps", s -> s.panel.fold(true));
			shot("empty", "first_steps", s -> s.newChapter("A New Chapter"));
			shot("check", "first_steps", EditorSelfCheck::run);
		}
	}

	private static void chapters() {
		QuestBook book = QuestsState.book();
		if (book != null && book != named) {
			named = book;

			for (Chapter c : book.chapters.values()) {
				if (!c.readOnly) {
					CoreActions.SCREEN_FACTORIES.put("cfhgui_quests:edit/" + c.id, parent -> open(parent, c.id, s -> EditSession.bookTab = false));

					for (Quest q : c.quests) {
						CoreActions.SCREEN_FACTORIES.put("cfhgui_quests:edit/" + c.id + "/" + q.id, parent -> open(parent, c.id, s -> s.selection.only(q.id)));
					}
				}
			}
		}
	}

	private static void shot(String name, String chapter, Consumer<EditSession> then) {
		CoreActions.SCREEN_FACTORIES.put("cfhgui_quests:editor/" + name, parent -> open(parent, chapter, then));
	}

	@Nullable
	private static Screen open(@Nullable Screen parent, String chapter, Consumer<EditSession> then) {
		if (!QuestsState.canEdit()) {
			return parent;
		} else {
			CFHGuiToasts.clear();
			Popups.closeAll();
			EditSession.collapsed = false;
			EditSession.bookTab = false;
			Form.foldOnly();
			EditSession.want(true);
			QuestsClient.target(chapter, null);
			QuestBookScreen screen = new QuestBookScreen(parent instanceof QuestBookScreen ? null : parent);
			EditSession s = EditSession.of(screen);
			s.beforeFirstBuild();
			later(350, () -> {
				if (Minecraft.getInstance().screen == screen) {
					QuestCanvas canvas = s.canvas();
					if (canvas != null) {
						canvas.fit();
					}

					later(250, () -> {
						if (Minecraft.getInstance().screen == screen) {
							then.accept(s);
						}
					});
				}
			});
			return screen;
		}
	}

	static void later(int ms, Runnable run) {
		LATER.add(new EditorShots.Due(Clock.nowMs() + (long)ms, run));
	}

	private static void tick() {
		if (!LATER.isEmpty()) {
			long now = Clock.nowMs();
			List<EditorShots.Due> due = new ArrayList<>();

			for (EditorShots.Due d : LATER) {
				if (d.atMs() <= now) {
					due.add(d);
				}
			}

			LATER.removeAll(due);

			for (EditorShots.Due dx : due) {
				try {
					dx.run().run();
				} catch (RuntimeException var6) {
					CFHGuiQuests.LOGGER.error("[CFHGui Quests] an editor shot failed", var6);
				}
			}
		}
	}

	private EditorShots() {
	}

	private static record Due(long atMs, Runnable run) {
	}
}
