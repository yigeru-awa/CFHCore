package com.onehumanawa.cfhcore.content.gui.quests.client.hud;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.quests.QuestsConfig;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.BookStats;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestIcons;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestPainter;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.logic.Rules;
import com.onehumanawa.cfhcore.content.gui.quests.mixin.client.ToastStackAccessor;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestState;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

final class QuestTracker {
	private static final int WIDTH = 150;
	private static final int PAD = 5;
	private static final int TITLE_H = 13;
	private static final int LINE_H = 13;
	private static final int GAP = 4;
	private static final int MAX_TASKS = 4;
	private static final int DONE_MS = 2600;
	private static final int LEAVE_MS = 320;
	private static final List<QuestTracker.Entry> ENTRIES = new ArrayList<>();
	private static final Map<String, QuestTracker.Entry> BY_KEY = new HashMap<>();
	private static int builtFor = -1;
	private static final Anim lowered = new Anim(0.0F, 260, Ease.OUT_CUBIC);
	private static final int UNDER_SLATE_TOAST = 58;
	private static final int GAME_TOAST = 32;

	private static int toastsReach(Minecraft mc) {
		int game = 0;

		try {
			game = ((ToastStackAccessor)mc.getToasts()).slateQuests$visible().size() * 32;
		} catch (RuntimeException var3) {
		}

		if (CFHGuiToasts.anyVisible()) {
			return game + 58;
		} else {
			return game > 0 ? game + 6 : 0;
		}
	}

	static void clear() {
		ENTRIES.clear();
		BY_KEY.clear();
		builtFor = -1;
	}

	static void progress(List<String> keys) {
		QuestsConfig config = QuestsConfig.get();
		if (config.tracker && config.trackerAuto && !keys.isEmpty()) {
			for (String key : keys) {
				if (config.pinned.size() >= config.trackerMax) {
					return;
				}

				if (!config.pinned.contains(key)) {
					BookIndex.Entry e = QuestsState.entry(key);
					if (e != null && QuestsState.status(key) == Rules.Status.IN_PROGRESS) {
						QuestsState.pin(key, true);
					}
				}
			}
		}
	}

	private static void rebuild() {
		builtFor = BookStats.revision();
		QuestsConfig config = QuestsConfig.get();
		Font font = CFHGuiDraw.font();
		long now = Clock.nowMs();
		List<QuestTracker.Entry> next = new ArrayList<>();
		int textW = 134;

		for (String key : config.pinned) {
			BookIndex.Entry e = QuestsState.entry(key);
			if (e != null) {
				Rules.Status status = QuestsState.status(key);
				if (status != Rules.Status.HIDDEN) {
					QuestTracker.Entry t = BY_KEY.get(key);
					boolean fresh = t == null;
					if (fresh) {
						t = new QuestTracker.Entry();
						t.key = key;
					}

					Quest q = e.quest();
					QuestState state = QuestsState.state(key);
					t.accent = QuestPainter.accentOf(e.chapter());
					t.icon = QuestIcons.of(e);
					t.title = CFHGuiDraw.truncate(Fonts.heading(QuestText.title(e)), 126);
					t.locked = status == Rules.Status.LOCKED;
					boolean done = status.done();
					if (done && t.doneAtMs == 0L) {
						t.doneAtMs = fresh && status == Rules.Status.COMPLETE ? now - 2600L : now;
					} else if (!done) {
						t.doneAtMs = 0L;
					}

					t.note = status == Rules.Status.CLAIMABLE ? CFHGuiDraw.truncate(Component.translatable("cfhgui_quests.status.claimable"), 126) : null;
					List<QuestTracker.Line> old = new ArrayList<>(t.lines);
					t.lines.clear();
					if (!done && !t.locked) {
						int at = 0;

						for (int i = 0; i < q.tasks.size() && t.lines.size() < 4; i++) {
							Task task = q.tasks.get(i);
							long counted = state.task(i);
							if (!Rules.taskDone(task, counted)) {
								QuestTracker.Line line = at < old.size() ? old.get(at) : new QuestTracker.Line();
								at++;
								float fraction = Rules.taskFraction(task, counted);
								if (!fresh && fraction > line.fraction + 5.0E-4F) {
									line.movedAtMs = now;
								}

								line.fraction = fraction;
								line.fill.set(fraction);
								line.count = QuestText.count(task, counted).getString();
								line.text = CFHGuiDraw.truncate(QuestText.label(task), 134 - (line.count.isEmpty() ? 0 : font.width(line.count) + 6));
								t.lines.add(line);
							}
						}
					}

					next.add(t);
					if (next.size() >= Math.max(1, config.trackerMax)) {
						break;
					}
				}
			}
		}

		ENTRIES.clear();
		BY_KEY.clear();

		for (QuestTracker.Entry tx : next) {
			ENTRIES.add(tx);
			BY_KEY.put(tx.key, tx);
		}
	}

	static void render(GuiGraphics g) {
		QuestsConfig config = QuestsConfig.get();
		Minecraft mc = Minecraft.getInstance();
		if (config.tracker && QuestsState.available() && !mc.getDebugOverlay().showDebugScreen()) {
			if (builtFor != BookStats.revision()) {
				rebuild();
			}

			if (!ENTRIES.isEmpty()) {
				Theme t = Theme.current();
				Palette p = t.palette();
				boolean van = t.isVanilla();
				Font font = CFHGuiDraw.font();
				float scale = (float)Mth.clamp(config.trackerScale, 0.5, 2.0);
				long now = Clock.nowMs();
				boolean right = config.trackerAnchor.endsWith("RIGHT");
				boolean bottom = config.trackerAnchor.startsWith("BOTTOM");
				float sw = (float)g.guiWidth() / scale;
				float sh = (float)g.guiHeight() / scale;
				float total = 0.0F;
				String leaving = null;

				for (QuestTracker.Entry e : ENTRIES) {
					if (!e.seen) {
						e.seen = true;
						e.shown.snap(0.0F);
						e.shown.set(1.0F);
					}

					if (e.doneAtMs != 0L && now - e.doneAtMs > 2600L) {
						e.shown.set(0.0F, 320);
						if (now - e.doneAtMs > 2960L && leaving == null) {
							leaving = e.key;
						}
					}

					total += (float)(e.height() + 4) * Mth.clamp(e.shown.get(), 0.0F, 1.0F);
				}

				lowered.set(right && !bottom ? (float)toastsReach(mc) : 0.0F);
				float all = 1.0F;
				g.pose().pushPose();
				g.pose().scale(scale, scale, 1.0F);
				int x = right ? Math.round(sw - (float)config.trackerX / scale - 150.0F) : Math.round((float)config.trackerX / scale);
				float y = bottom ? sh - (float)config.trackerY / scale - total : (float)config.trackerY / scale;
				if (!bottom) {
					y = Math.max(y, lowered.get() / scale);
				}

				for (QuestTracker.Entry e : ENTRIES) {
					float in = Mth.clamp(e.shown.get(), 0.0F, 1.0F);
					if (!(in <= 0.01F)) {
						int h = e.height();
						float a = in * 1.0F;
						int ex = x + Math.round((1.0F - in) * (float)(right ? 18 : -18));
						int ey = Math.round(y);
						boolean done = e.doneAtMs != 0L;
						int accent = van ? -1 : e.accent;
						int good = van ? -11141291 : p.success();
						if (van) {
							g.fill(ex, ey, ex + 150, ey + h, Colors.scaleAlpha(-1879048192, a));
						} else {
							CFHGuiDraw.pixelRound(g, ex, ey, 150, h, Colors.scaleAlpha(Colors.withAlpha(p.bg(), 204), a), 2);
						}

						g.fill(ex, ey + 2, ex + 2, ey + h - 2, Colors.scaleAlpha(done ? good : (e.locked ? (van ? -8355712 : p.textDim()) : accent), a));
						if (a > 0.5F) {
							e.icon.draw(g, (float)(ex + 5 + 6), (float)(ey + 5 + 4), 10.0F, Colors.scaleAlpha(van ? -1 : p.textMuted(), a));
						}

						int tx = ex + 5 + 14;
						g.drawString(
							font, e.title, tx, ey + 5, Colors.scaleAlpha(done ? good : (e.locked ? (van ? -6250336 : p.textDim()) : (van ? -1 : p.text())), Math.max(0.03F, a)), van
						);
						if (done) {
							float tick = Mth.clamp((float)(now - e.doneAtMs) / 360.0F, 0.0F, 1.0F);
							g.fill(tx - 1, ey + 5 + 4, tx - 1 + Math.round((float)(font.width(e.title) + 2) * tick), ey + 5 + 5, Colors.scaleAlpha(good, a));
							Icons.draw(g, Icon.CHECK, ex + 150 - 5 - 8, ey + 5, 8, Colors.scaleAlpha(good, a * tick));
						} else if (e.locked) {
							Icons.draw(g, Icon.LOCK, ex + 150 - 5 - 8, ey + 5, 8, Colors.scaleAlpha(van ? -6250336 : p.textDim(), a));
						}

						int ly = ey + 5 + 13;

						for (QuestTracker.Line line : e.lines) {
							float flash = line.movedAtMs != 0L && now - line.movedAtMs < 800L ? 1.0F - (float)(now - line.movedAtMs) / 800.0F : 0.0F;
							if (flash > 0.0F) {
								g.fill(ex + 3, ly - 2, ex + 150 - 2, ly + 13 - 3, Colors.scaleAlpha(Colors.withAlpha(accent, 56), a * flash));
							}

							int text = Colors.lerp(van ? -2039584 : p.textMuted(), van ? -1 : p.text(), flash);
							g.drawString(font, line.text, ex + 5 + 4, ly, Colors.scaleAlpha(text, Math.max(0.03F, a)), van);
							if (!line.count.isEmpty()) {
								g.drawString(
									font,
									line.count,
									ex + 150 - 5 - font.width(line.count),
									ly,
									Colors.scaleAlpha(Colors.lerp(van ? -6250336 : p.textDim(), accent, flash), Math.max(0.03F, a)),
									van
								);
							}

							int bw = 136;
							g.fill(ex + 5 + 4, ly + 9, ex + 5 + 4 + 136, ly + 10, Colors.scaleAlpha(van ? -16777216 : p.surfaceActive(), a));
							float f = line.fill.get();
							if (f > 0.0F) {
								g.fill(ex + 5 + 4, ly + 9, ex + 5 + 4 + Math.max(1, Math.round(136.0F * f)), ly + 10, Colors.scaleAlpha(Colors.lerp(accent, -1, flash * 0.6F), a));
							}

							ly += 13;
						}

						if (e.note != null) {
							g.drawString(font, e.note, ex + 5 + 14, ly - 2, Colors.scaleAlpha(van ? -22016 : p.warning(), Math.max(0.03F, a)), van);
						}

						y += (float)(h + 4) * in;
					}
				}

				g.pose().popPose();
				if (leaving != null) {
					QuestsState.pin(leaving, false);
				}
			}
		}
	}

	private QuestTracker() {
	}

	private static final class Entry {
		String key = "";
		FormattedCharSequence title = FormattedCharSequence.EMPTY;
		QuestIcons.Drawable icon = QuestIcons.of((String)null);
		final List<QuestTracker.Line> lines = new ArrayList<>();
		int accent;
		boolean locked;
		long doneAtMs;
		@Nullable
		FormattedCharSequence note;
		final Anim shown = new Anim(0.0F, 240, Ease.OUT_CUBIC);
		boolean seen;

		int height() {
			return 18 + this.lines.size() * 13 + (this.lines.isEmpty() ? 0 : 1) + (this.note != null ? 10 : 0) + 5 - 2;
		}
	}

	private static final class Line {
		FormattedCharSequence text = FormattedCharSequence.EMPTY;
		String count = "";
		float fraction;
		final Anim fill = new Anim(0.0F, 420, Ease.OUT_CUBIC);
		long movedAtMs;
	}
}
