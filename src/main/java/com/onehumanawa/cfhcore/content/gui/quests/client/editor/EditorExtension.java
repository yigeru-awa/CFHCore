package com.onehumanawa.cfhcore.content.gui.quests.client.editor;

import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.ui.BookExtensions;
import com.onehumanawa.cfhcore.content.gui.quests.client.ui.QuestBookScreen;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestCanvas;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import org.jetbrains.annotations.Nullable;

final class EditorExtension implements BookExtensions.Extension {
	@Override
	public void header(QuestBookScreen screen, Consumer<AbstractWidget> add) {
		if (QuestsState.canEdit()) {
			EditSession.of(screen).header(add);
		}
	}

	@Override
	public void chapterShown(QuestBookScreen screen, @Nullable Chapter chapter) {
		if (QuestsState.canEdit() || EditSession.of(screen).editing()) {
			EditSession.of(screen).shown(chapter);
		}
	}

	@Override
	public void closed(QuestBookScreen screen) {
		EditSession.of(screen).closed();
	}

	@Override
	public boolean keyPressed(QuestBookScreen screen, int key, int scanCode, int modifiers) {
		if (!QuestsState.canEdit()) {
			return false;
		} else {
			EditSession s = EditSession.of(screen);
			boolean ctrl = (modifiers & (Minecraft.ON_OSX ? 8 : 2)) != 0;
			boolean shift = (modifiers & 1) != 0;
			boolean alt = (modifiers & 4) != 0;
			if (key == 69 && ctrl) {
				s.toggle();
				return true;
			} else if (!s.editing()) {
				return false;
			} else if (key == 256) {
				return s.tool.cancel() ? true : s.selection.clear();
			} else if (ctrl) {
				switch (key) {
					case 65:
						s.selectAll();
						return true;
					case 67:
						return s.copySelection();
					case 68:
						s.duplicateSelection();
						return true;
					case 86:
						float[] at = s.tool.pointer();
						s.paste(at[0], at[1]);
						return true;
					case 88:
						s.cutSelection();
						return true;
					case 89:
						s.redo();
						return true;
					case 90:
						if (shift) {
							s.redo();
						} else {
							s.undo();
						}

						return true;
					default:
						return false;
				}
			} else if (!s.panel.hasKeyboard() && s.active()) {
				float step = shift ? 1.0F : (alt ? 0.1F : 0.5F);
				switch (key) {
					case 257:
					case 335:
						s.panel.focusFirst();
						return true;
					case 258:
						next(s, shift ? -1 : 1);
						return true;
					case 260:
						float[] at = s.tool.pointer();
						float[] place = s.tool.place(at[0], at[1]);
						s.newQuest(place[0], place[1], null);
						return true;
					case 261:
						s.deleteSelection();
						return true;
					case 262:
						return nudge(s, step, 0.0F);
					case 263:
						return nudge(s, -step, 0.0F);
					case 264:
						return nudge(s, 0.0F, step);
					case 265:
						return nudge(s, 0.0F, -step);
					case 348:
						s.tool.menuByKey();
						return true;
					default:
						return false;
				}
			} else {
				return false;
			}
		}
	}

	private static boolean nudge(EditSession s, float dx, float dy) {
		if (s.selection.size() == 0) {
			return false;
		} else {
			s.nudge(dx, dy);
			return true;
		}
	}

	private static void next(EditSession s, int direction) {
		Chapter work = s.work();
		QuestCanvas canvas = s.canvas();
		if (work != null && canvas != null && !work.quests.isEmpty()) {
			List<Quest> order = new ArrayList<>(work.quests);
			order.sort(Comparator.<Quest>comparingDouble(q -> (double)q.y).thenComparingDouble(q -> (double)q.x));
			String chosen = s.selection.quest();
			int at = -1;

			for (int i = 0; i < order.size(); i++) {
				if (order.get(i).id.equals(chosen)) {
					at = i;
				}
			}

			int to = at < 0 ? (direction > 0 ? 0 : order.size() - 1) : Math.floorMod(at + direction, order.size());
			EditorMenus.choose(s, order.get(to).id);
			CFHGuiSounds.tick();
		}
	}
}
