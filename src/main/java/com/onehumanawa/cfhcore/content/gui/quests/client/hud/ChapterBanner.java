package com.onehumanawa.cfhcore.content.gui.quests.client.hud;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestIcons;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestPainter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

final class ChapterBanner {
	private static final int IN_MS = 420;
	private static final int HOLD_MS = 3800;
	private static final int OUT_MS = 380;
	private static final int HEIGHT = 34;
	@Nullable
	private static Chapter chapter;
	private static Component title = Component.empty();
	private static Component caption = Component.empty();
	private static long startMs;

	static void show(Chapter c, String who) {
		chapter = c;
		title = Fonts.heading(QuestText.title(c));
		caption = Fonts.heading(
			who.isBlank() ? Component.translatable("cfhgui_quests.banner.chapter") : Component.translatable("cfhgui_quests.banner.chapter.by", new Object[]{who})
		);
		startMs = Clock.nowMs();
	}

	static void clear() {
		chapter = null;
	}

	static void render(GuiGraphics g, int screenWidth) {
		Chapter c = chapter;
		if (c != null) {
			long age = Clock.nowMs() - startMs;
			if (age > 4600L) {
				chapter = null;
			} else {
				Theme t = Theme.current();
				Palette p = t.palette();
				boolean van = t.isVanilla();
				Font font = CFHGuiDraw.font();
				boolean still = t.motion() <= 0.0F;
				float open = still
					? 1.0F
					: (age < 420L ? Ease.OUT_BACK.apply((float)age / 420.0F) : (age > 4220L ? 1.0F - Ease.IN_OUT_CUBIC.apply((float)(age - 420L - 3800L) / 380.0F) : 1.0F));
				float alpha = still ? 1.0F : Mth.clamp(age < 420L ? (float)age / 210.0F : (age > 4220L ? 1.0F - (float)(age - 420L - 3800L) / 380.0F : 1.0F), 0.0F, 1.0F);
				if (!(alpha <= 0.02F) && !(open <= 0.02F)) {
					int accent = van ? -171 : QuestPainter.accentOf(c);
					FormattedCharSequence name = CFHGuiDraw.truncate(title, 230);
					int full = Mth.clamp(Math.max(font.width(name), font.width(caption)) + 64, 170, Math.min(310, screenWidth - 24));
					int w = Math.max(34, Math.round((float)full * Math.min(1.04F, open)));
					int h = 34;
					int x = (screenWidth - w) / 2;
					int y = 26 - Math.round((1.0F - Math.min(1.0F, open)) * 8.0F);
					g.pose().pushPose();
					g.pose().translate(0.0F, 0.0F, 380.0F);
					int reach = Math.round(70.0F * Math.min(1.0F, open));
					CFHGuiDraw.hgradient(g, x - reach, y + 17, reach, 1, Colors.withAlpha(accent, 0), Colors.scaleAlpha(accent, alpha));
					CFHGuiDraw.hgradient(g, x + w, y + 17, reach, 1, Colors.scaleAlpha(accent, alpha), Colors.withAlpha(accent, 0));
					if (van) {
						g.fill(x - 1, y - 1, x + w + 1, y + 34 + 1, Colors.scaleAlpha(-16777216, alpha));
						g.fill(x, y, x + w, y + 34, Colors.scaleAlpha(-266856424, alpha));
						CFHGuiDraw.outline(g, x, y, w, 34, Colors.scaleAlpha(accent, alpha), 0);
					} else {
						CFHGuiDraw.popupPlate(g, x, y, w, 34, p.surface(), Colors.mix(p.borderStrong(), accent, 0.7F), 0.7F, alpha);
						g.fill(x + 3, y + 34 - 2, x + w - 3, y + 34 - 1, Colors.scaleAlpha(accent, alpha));
					}

					float words = Mth.clamp((open - 0.6F) / 0.4F, 0.0F, 1.0F) * alpha;
					if (words > 0.02F) {
						g.enableScissor(x + 1, y + 1, x + w - 1, y + 34 - 1);
						int cx = screenWidth / 2;
						int left = cx - full / 2;
						CFHGuiDraw.pixelCircle(g, left + 20, y + 17, 12, Colors.scaleAlpha(Colors.withAlpha(accent, van ? 64 : 52), words));
						if (words > 0.6F) {
							QuestIcons.of(c.icon).draw(g, (float)(left + 20), (float)(y + 17), 16.0F, accent);
						}

						g.drawString(font, caption, left + 40, y + 7, Colors.scaleAlpha(accent, words), van);
						g.drawString(font, name, left + 40, y + 19, Colors.scaleAlpha(van ? -1 : p.text(), words), van);
						g.disableScissor();
					}

					if (!still && age < 2620L) {
						float ts = (float)age / 1000.0F;
						int[] colors = new int[]{accent, -1, van ? -11141291 : p.warning()};

						for (int i = 0; i < 22; i++) {
							float born = 0.15F + rnd(i, 1) * 1.6F;
							float life = 0.7F + rnd(i, 2) * 0.6F;
							float tk = (ts - born) / life;
							if (!(tk <= 0.0F) && !(tk >= 1.0F)) {
								boolean top = (i & 1) == 0;
								float sx = (float)x + rnd(i, 3) * (float)w + (rnd(i, 4) - 0.5F) * 30.0F * tk;
								float sy = (float)(top ? y : y + 34) + (top ? -1.0F : 1.0F) * (4.0F + 16.0F * rnd(i, 5)) * (1.0F - (1.0F - tk) * (1.0F - tk)) + 8.0F * tk * tk;
								int ix = Math.round(sx);
								int iy = Math.round(sy);
								int size = tk < 0.4F ? 2 : 1;
								g.fill(ix, iy, ix + size, iy + size, Colors.withAlpha(colors[i % colors.length], Math.round(255.0F * (1.0F - tk) * alpha)));
							}
						}
					}

					g.pose().popPose();
				}
			}
		}
	}

	private static float rnd(int i, int salt) {
		int h = i * -1640531535 + salt * -2048144789;
		h ^= h >>> 15;
		h *= 739982445;
		h ^= h >>> 12;
		return (float)(h & 65535) / 65536.0F;
	}

	private ChapterBanner() {
	}
}
