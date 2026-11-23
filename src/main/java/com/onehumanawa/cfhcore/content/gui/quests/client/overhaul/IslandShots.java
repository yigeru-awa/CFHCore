package com.onehumanawa.cfhcore.content.gui.quests.client.overhaul;

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
import com.onehumanawa.cfhcore.content.gui.quests.client.hud.QuestsHud;
import com.onehumanawa.cfhcore.content.gui.quests.client.ui.QuestBookScreen;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Ids;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

final class IslandShots {
	static final boolean ON = DevHarness.sampleData();
	private static final List<IslandShots.Due> LATER = new ArrayList<>();
	@Nullable
	private static QuestBook known;
	private static int knownChapters;

	static void init() {
		if (ON) {
			CFHGuiEvents.CLIENT_TICK_END.register(IslandShots::tick);
		}
	}

	private static void register(QuestBook book) {
		CoreActions.SCREEN_FACTORIES.put("cfhgui_quests:island/fresh", parent -> {
			QuestActions.reset("all", null, r -> {
			});
			CFHGuiToasts.clear();
			return null;
		});
		CoreActions.SCREEN_FACTORIES.put("cfhgui_quests:island/finished", parent -> {
			QuestBook now = QuestsState.book();
			if (now == null) {
				return null;
			} else {
				QuestsHud.quiet(true);

				try {
					for (int pass = 0; pass < 2; pass++) {
						for (Chapter cx : now.chapters.values()) {
							if (!cx.readOnly) {
								for (Quest qx : cx.quests) {
									String keyx = Ids.key(cx, qx);
									if (!QuestsState.status(keyx).done()) {
										QuestActions.complete(keyx, null, r -> {
										});
									}
								}
							}
						}
					}

					QuestActions.claimAll(false, r -> {
					});
				} finally {
					QuestsHud.quiet(false);
				}

				CFHGuiToasts.clear();
				return null;
			}
		});
		CoreActions.SCREEN_FACTORIES.put("cfhgui_quests:island/flat", parent -> {
			CFHGuiToasts.clear();
			IslandView v = IslandView.last;
			Chapter shown = v == null ? null : v.chapter();
			QuestsClient.target(shown == null ? null : shown.id, v == null ? null : v.selected());
			return new QuestBookScreen(parent instanceof QuestBookScreen open ? open.parentScreen() : parent);
		});

		for (Chapter c : book.chapters.values()) {
			shot(c.id, c, IslandView::fit);
			shot(c.id + "/near", c, v -> v.lookAt(null, 1.7F));

			for (Quest q : c.quests) {
				String key = Ids.key(c, q);
				shot(key, c, v -> v.lookAt(key, 1.6F));
			}
		}

		Chapter first = book.chapter("first_steps");
		if (first != null) {
			shot("lay", first, v -> {
				String next = null;

				for (String keyxx : new String[]{"first_steps/iron", "first_steps/night", "first_steps/shield", "first_steps/bucket"}) {
					if (!QuestsState.status(keyxx).done()) {
						next = keyxx;
						break;
					}
				}

				if (next != null) {
					String keyx = next;
					v.lookAt(keyx, 1.15F);
					later(700, () -> QuestActions.complete(keyx, null, r -> {
						}));
				}
			});
			shot("wheel", first, v -> {
				v.fit();
				later(600, () -> v.mouseScrolled((double)v.getX() + (double)v.getWidth() * 0.62, (double)v.getY() + (double)v.getHeight() * 0.4, 0.0, 4.0));
			});
			shot("drag", first, v -> {
				v.fit();
				later(600, () -> {
					double x = (double)v.getX() + (double)v.getWidth() * 0.62;
					double y = (double)v.getY() + (double)v.getHeight() * 0.4;
					v.mouseClicked(x, y, 0);

					for (int step = 1; step <= 10; step++) {
						v.mouseDragged(x - (double)step * 12.0, y + (double)step * 6.0, 0, -12.0, 6.0);
					}

					v.mouseReleased(x - 120.0, y + 60.0, 0);
				});
			});
		}
	}

	private static void shot(String name, Chapter chapter, Consumer<IslandView> then) {
		CoreActions.SCREEN_FACTORIES.put("cfhgui_quests:island/" + name, parent -> {
			CFHGuiToasts.clear();
			List<String> seen = QuestsConfig.get().seenChapters;
			if (!seen.contains(chapter.id)) {
				seen.add(chapter.id);
			}

			QuestsClient.target(chapter.id, null);
			Screen s = MenuSlots.create("cfhgui_quests:book", parent);
			later(1, () -> {
				IslandView v = IslandView.last;
				if (v != null && v.screen() == s && v.island() != null) {
					then.accept(v);
				}
			});
			return s;
		});
	}

	private static void later(int ms, Runnable run) {
		LATER.add(new IslandShots.Due(Clock.nowMs() + (long)ms, run));
	}

	private static void tick() {
		QuestBook book = QuestsState.book();
		if (book != null && (book != known || book.chapters.size() != knownChapters)) {
			known = book;
			knownChapters = book.chapters.size();
			register(book);
		}

		if (!LATER.isEmpty()) {
			long now = Clock.nowMs();
			List<IslandShots.Due> due = new ArrayList<>();

			for (IslandShots.Due d : LATER) {
				if (d.atMs() <= now) {
					due.add(d);
				}
			}

			LATER.removeAll(due);

			for (IslandShots.Due dx : due) {
				dx.run().run();
			}
		}
	}

	private IslandShots() {
	}

	private static record Due(long atMs, Runnable run) {
	}
}
