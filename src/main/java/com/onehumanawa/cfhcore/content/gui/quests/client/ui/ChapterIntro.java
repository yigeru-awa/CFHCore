package com.onehumanawa.cfhcore.content.gui.quests.client.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.quests.QuestsConfig;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.RichText;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.CanvasDraw;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestIcons;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestPainter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

final class ChapterIntro {
	private static final int PAD = 8;
	private static final int HEAD = 20;
	private static final int MAX_W = 460;
	private static final int MARGIN = 8;
	@Nullable
	private Chapter chapter;
	@Nullable
	private RichText.Layout layout;
	private final Anim shown = new Anim(0.0F, 240, Ease.OUT_CUBIC);
	private boolean wanted;
	private int x;
	private int y;
	private int w;
	private int h;
	private int scroll;
	private Rect tree = new Rect(0, 0, 0, 0);
	private RichText.Host host;
	private GuiGraphics frameG;
	private int frameMouseX;
	private int frameMouseY;
	private final Runnable paint = this::paint;

	void host(RichText.Host h) {
		this.host = h;
	}

	void place(Rect treeRect) {
		this.tree = treeRect;
		if (this.chapter != null) {
			this.lay();
		}
	}

	void arrive(Chapter c) {
		List<String> seen = QuestsConfig.get().seenChapters;
		if (!c.description.isEmpty() && !seen.contains(c.id)) {
			seen.add(c.id);
			this.open(c);
		} else {
			this.dismiss();
			this.chapter = c;
			this.layout = null;
		}
	}

	void toggle(@Nullable Chapter c) {
		if (c != null) {
			if (this.wanted && this.chapter == c) {
				this.dismiss();
			} else {
				this.open(c);
			}
		}
	}

	private void open(Chapter c) {
		this.chapter = c;
		this.lay();
		this.wanted = true;
	}

	void dismiss() {
		this.wanted = false;
	}

	boolean isOpen() {
		return this.wanted;
	}

	int cover() {
		return this.wanted && this.layout != null ? this.h + 16 : 0;
	}

	private void lay() {
		Chapter c = this.chapter;
		if (c != null) {
			this.w = Math.max(160, Math.min(460, this.tree.w() - 16));
			this.x = this.tree.x() + (this.tree.w() - this.w) / 2;
			this.layout = RichText.of(c.description).layout(this.w - 16);
			this.h = Math.min(Math.max(48, this.tree.h() * 2 / 5), 24 + this.layout.height() + 5);
			this.y = this.tree.bottom() - 8 - this.h;
			this.scroll = 0;
		}
	}

	private int maxScroll() {
		return this.layout == null ? 0 : Math.max(0, this.layout.height() + 9 - (this.h - 20));
	}

	boolean contains(double mx, double my) {
		return this.wanted && mx >= (double)this.x && mx < (double)(this.x + this.w) && my >= (double)this.y && my < (double)(this.y + this.h);
	}

	boolean mouseClicked(double mx, double my, int button) {
		if (!this.wanted) {
			return false;
		} else if (!this.contains(mx, my)) {
			this.dismiss();
			return false;
		} else if (button == 0
			&& this.layout != null
			&& this.host != null
			&& my >= (double)(this.y + 20)
			&& this.layout.click(mx - (double)this.x - 8.0, my - (double)this.y - 20.0 - 4.0 + (double)this.scroll, this.host)) {
			return true;
		} else {
			this.dismiss();
			return true;
		}
	}

	boolean mouseScrolled(double mx, double my, double amount) {
		if (!this.contains(mx, my)) {
			return false;
		} else {
			this.scroll = Mth.clamp(this.scroll - (int)Math.round(amount * 12.0), 0, this.maxScroll());
			return true;
		}
	}

	void render(GuiGraphics g, int mouseX, int mouseY, boolean hold) {
		this.shown.set(this.wanted && !hold ? 1.0F : 0.0F);
		if (!(this.shown.get() <= 0.02F) && this.chapter != null && this.layout != null) {
			this.frameG = g;
			this.frameMouseX = mouseX;
			this.frameMouseY = mouseY;
			CanvasDraw.layer(g);
			CanvasDraw.batch(g, this.paint);
			this.frameG = null;
		}
	}

	private void paint() {
		GuiGraphics g = this.frameG;
		Chapter c = this.chapter;
		RichText.Layout text = this.layout;
		if (c != null && text != null) {
			Theme t = Theme.current();
			Palette p = t.palette();
			boolean van = t.isVanilla();
			Font font = CFHGuiDraw.font();
			float o = Mth.clamp(this.shown.get(), 0.0F, 1.0F);
			float a = Math.min(1.0F, o * 1.3F);
			int yy = this.y + Math.round((1.0F - o) * 10.0F);
			int accent = van ? -1 : QuestPainter.accentOf(c);
			g.enableScissor(this.tree.x(), this.tree.y(), this.tree.right(), this.tree.bottom());
			if (van) {
				g.fill(this.x - 1, yy - 1, this.x + this.w + 1, yy + this.h + 1, Colors.scaleAlpha(-16777216, a));
				g.fill(this.x, yy, this.x + this.w, yy + this.h, Colors.scaleAlpha(-267382768, a));
				CFHGuiDraw.outline(g, this.x, yy, this.w, this.h, Colors.scaleAlpha(-7631989, a), 0);
			} else {
				CFHGuiDraw.popupPlate(g, this.x, yy, this.w, this.h, p.surface(), Colors.mix(p.borderStrong(), accent, 0.4F), 0.6F, a);
				g.fill(this.x + 3, yy, this.x + this.w - 3, yy + 2, Colors.scaleAlpha(accent, a));
			}

			if (a > 0.6F) {
				QuestIcons.of(c.icon).draw(g, (float)(this.x + 8 + 7), (float)(yy + 11), 14.0F, accent);
			}

			Component title = Fonts.heading(QuestText.title(c));
			FormattedCharSequence hint = Component.translatable("cfhgui_quests.intro.hint").getVisualOrderText();
			int left = this.x + 8 + 18;
			int right = this.x + this.w - 8;
			int hintW = font.width(hint) <= (right - left) / 2 ? font.width(hint) : 0;
			int titleW = Math.min(font.width(title), right - left - (hintW > 0 ? hintW + 8 : 0));
			g.drawString(font, CFHGuiDraw.truncate(title, titleW), left, yy + 7, Colors.scaleAlpha(van ? -1 : p.text(), a), van);
			if (hintW > 0) {
				g.drawString(font, hint, right - hintW, yy + 7, Colors.scaleAlpha(van ? -8355712 : p.textDim(), a), van);
			}

			if (!c.subtitle.isBlank()) {
				int room = right - left - titleW - 8 - (hintW > 0 ? hintW + 10 : 0);
				if (room > 40) {
					g.drawString(
						font, CFHGuiDraw.truncate(QuestText.text(c.subtitle), room), left + titleW + 8, yy + 7, Colors.scaleAlpha(van ? -6250336 : p.textMuted(), a), van
					);
				}
			}

			int ty = yy + 20;
			g.fill(this.x + 8, ty, this.x + this.w - 8, ty + 1, Colors.scaleAlpha(van ? -11513776 : p.border(), a));
			g.enableScissor(this.x + 1, ty + 2, this.x + this.w - 1, yy + this.h - 2);
			text.draw(g, this.x + 8, ty + 4 - this.scroll, this.frameMouseX, this.frameMouseY, a);
			g.disableScissor();
			int more = this.maxScroll();
			if (more > 0) {
				int track = this.h - 20 - 6;
				int bar = Math.max(8, track * (this.h - 20) / (this.h - 20 + more));
				int by = ty + 3 + Math.round((float)(track - bar) * ((float)this.scroll / (float)more));
				g.fill(this.x + this.w - 4, by, this.x + this.w - 3, by + bar, Colors.scaleAlpha(van ? -6250336 : p.textDim(), a));
			}

			g.disableScissor();
		}
	}
}
