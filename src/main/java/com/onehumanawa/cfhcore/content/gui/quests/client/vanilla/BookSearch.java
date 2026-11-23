package com.onehumanawa.cfhcore.content.gui.quests.client.vanilla;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.client.ui.QuestSearch;
import com.onehumanawa.cfhcore.content.gui.quests.client.ui.Trees;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.CanvasDraw;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestIcons;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestPainter;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.logic.Rules;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

final class BookSearch {
	private static final int ROW = 24;
	private static final int MAX_ROWS = 7;
	private static final int LIST_W = 214;
	private static final int GLASS = 14;
	private final Consumer<String> goTo;
	private final EditBox field;
	private String query = "";
	private List<QuestSearch.Hit> hits = List.of();
	private boolean open;
	private int marked;
	private int first;
	private int lastMouseX = Integer.MIN_VALUE;
	private int lastMouseY = Integer.MIN_VALUE;
	private final Anim shown = new Anim(0.0F, 150, Ease.OUT_CUBIC);
	private final Anim focus = new Anim(0.0F, 140, Ease.OUT_CUBIC);
	private int wx;
	private int wy;
	private int ww;
	private int x;
	private int y;
	private int w;
	private int h;
	@Nullable
	private Component hinted;
	private boolean hintedVanilla;
	private GuiGraphics frameG;
	private int frameMouseX;
	private int frameMouseY;
	private final Runnable paint = this::paint;

	BookSearch(Consumer<String> goTo) {
		this.goTo = goTo;
		this.field = new EditBox(Minecraft.getInstance().font, 0, 0, 60, 9, Component.translatable("cfhgui_quests.vanilla.search"));
		this.field.setBordered(false);
		this.field.setMaxLength(64);
		this.field.setResponder(this::typed);
	}

	EditBox field() {
		return this.field;
	}

	void place(int right, int top, int width) {
		this.ww = width - 14;
		this.wx = right - this.ww;
		this.wy = top;
		this.field.setX(this.wx + 3);
		this.field.setY(this.wy + 2);
		this.field.setWidth(this.ww - 5);
	}

	int left() {
		return this.wx - 14;
	}

	private void typed(String text) {
		this.query = text == null ? "" : text;
		this.hits = QuestSearch.find(this.query, 40);
		this.marked = 0;
		this.first = 0;
		this.open = !this.query.isBlank();
		this.shown.set(this.open ? 1.0F : 0.0F);
	}

	boolean isOpen() {
		return this.open;
	}

	void close() {
		this.open = false;
		this.shown.set(0.0F);
	}

	void demo(String text) {
		this.field.setFocused(true);
		this.field.setValue(text);
	}

	private int rows() {
		return Math.max(1, Math.min(7, this.hits.size()));
	}

	private void lay(int screenWidth, int bottom) {
		this.w = Math.min(214, screenWidth - 16);
		this.y = this.wy + 15;
		int rows = Math.max(1, Math.min(this.rows(), (bottom - this.y - 16) / 24));
		this.h = rows * 24 + 6 + (this.hits.size() > rows ? 10 : 0);
		this.x = Mth.clamp(this.wx + this.ww - this.w, 8, Math.max(8, screenWidth - 8 - this.w));
	}

	private int visibleRows() {
		return Math.max(1, (this.h - 6 - (this.hits.size() > this.rows() ? 10 : 0)) / 24);
	}

	boolean contains(double mouseX, double mouseY) {
		return this.open && mouseX >= (double)this.x && mouseX < (double)(this.x + this.w) && mouseY >= (double)this.y && mouseY < (double)(this.y + this.h);
	}

	private int rowAt(double mouseY) {
		int i = (int)Math.floor((mouseY - (double)this.y - 3.0) / 24.0);
		return i >= 0 && i < this.visibleRows() && this.first + i < this.hits.size() ? this.first + i : -1;
	}

	private void pick(int i) {
		if (i >= 0 && i < this.hits.size()) {
			String key = this.hits.get(i).entry().key();
			CFHGuiSounds.click();
			this.field.setValue("");
			this.close();
			this.field.setFocused(false);
			this.goTo.accept(key);
		}
	}

	boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!this.open) {
			return false;
		} else if (this.contains(mouseX, mouseY)) {
			if (button == 0) {
				this.pick(this.rowAt(mouseY));
			}

			return true;
		} else {
			if (!this.overWell(mouseX, mouseY)) {
				this.close();
			}

			return false;
		}
	}

	boolean overWell(double mouseX, double mouseY) {
		return mouseX >= (double)(this.wx - 14) && mouseX < (double)(this.wx + this.ww) && mouseY >= (double)this.wy && mouseY < (double)(this.wy + 12);
	}

	boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
		if (!this.contains(mouseX, mouseY)) {
			return false;
		} else {
			this.first = Mth.clamp(this.first - (int)Math.signum(scrollY), 0, Math.max(0, this.hits.size() - this.visibleRows()));
			return true;
		}
	}

	boolean keyPressed(int keyCode) {
		if (!this.field.isFocused()) {
			return false;
		} else if (!this.open) {
			if (keyCode == 264 && !this.query.isBlank()) {
				this.open = true;
				this.shown.set(1.0F);
				return true;
			} else {
				return false;
			}
		} else {
			switch (keyCode) {
				case 256:
					this.close();
					return true;
				case 257:
				case 335:
					this.pick(this.marked);
					return true;
				case 264:
					this.mark(this.marked + 1);
					return true;
				case 265:
					this.mark(this.marked - 1);
					return true;
				default:
					return false;
			}
		}
	}

	private void mark(int i) {
		if (!this.hits.isEmpty()) {
			this.marked = Math.floorMod(i, this.hits.size());
			int rows = this.visibleRows();
			if (this.marked < this.first) {
				this.first = this.marked;
			} else if (this.marked >= this.first + rows) {
				this.first = this.marked - rows + 1;
			}
		}
	}

	void renderField(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		Theme t = Theme.current();
		boolean van = t.isVanilla();
		this.focus.set(this.field.isFocused());
		WindowArt.well(g, this.wx, this.wy, this.ww, 12, this.focus.get(), 1.0F);
		if (van) {
			WindowArt.sprite(g, WindowArt.SEARCH, this.wx - 14 + 1, this.wy, 12, 12, 1.0F);
		} else {
			int ink = Colors.lerp(t.palette().textDim(), t.palette().textMuted(), this.focus.get());
			CFHGuiDraw.pixelRing(g, this.wx - 8, this.wy + 5, 3, ink);
			g.fill(this.wx - 6, this.wy + 8, this.wx - 4, this.wy + 10, ink);
		}

		if (this.hinted == null || this.hintedVanilla != van) {
			this.hintedVanilla = van;
			int tone = (van ? -2565928 : t.palette().textDim()) & 16777215;
			this.hinted = Component.translatable("cfhgui_quests.vanilla.search.hint").withStyle(s -> s.withItalic(van).withColor(tone));
			this.field.setHint(this.hinted);
		}

		this.field.setTextColor(van ? 16777215 : t.palette().text() & 16777215);
		this.field.render(g, mouseX, mouseY, partialTick);
	}

	void renderList(GuiGraphics g, int mouseX, int mouseY, int screenWidth, int bottom) {
		if (this.open && !this.field.isFocused()) {
			this.close();
		}

		if (!(this.shown.get() <= 0.02F)) {
			this.lay(screenWidth, bottom);
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
		int mouseX = this.frameMouseX;
		int mouseY = this.frameMouseY;
		Theme t = Theme.current();
		Palette p = t.palette();
		boolean van = t.isVanilla();
		Font font = CFHGuiDraw.font();
		float o = Mth.clamp(this.shown.get(), 0.0F, 1.0F);
		float a = Math.min(1.0F, o * 1.4F);
		int yy = this.y - Math.round((1.0F - o) * 4.0F);
		WindowArt.box(g, this.x, yy - 3, this.w, this.h + 6, a);
		if (this.hits.isEmpty()) {
			g.drawString(
				font,
				CFHGuiDraw.truncate(Component.translatable("cfhgui_quests.search.none", new Object[]{this.query.trim()}), this.w - 16),
				this.x + 8,
				yy + 11,
				Colors.scaleAlpha(van ? -5592406 : p.textMuted(), a),
				false
			);
		} else {
			if (this.open && (mouseX != this.lastMouseX || mouseY != this.lastMouseY)) {
				this.lastMouseX = mouseX;
				this.lastMouseY = mouseY;
				if (this.contains((double)mouseX, (double)mouseY)) {
					int r = this.rowAt((double)mouseY);
					if (r >= 0) {
						this.marked = r;
					}
				}
			}

			QuestBook book = QuestsState.book();
			int rows = this.visibleRows();

			for (int i = 0; i < rows && this.first + i < this.hits.size(); i++) {
				BookIndex.Entry e = this.hits.get(this.first + i).entry();
				int ry = yy + 3 + i * 24;
				boolean on = this.first + i == this.marked;
				if (on) {
					g.fill(this.x + 2, ry, this.x + this.w - 2, ry + 24 - 1, Colors.scaleAlpha(van ? -12566464 : p.surfaceHover(), a));
				}

				Rules.Status status = QuestsState.status(e.key());
				Quest q = e.quest();
				boolean secret = status == Rules.Status.LOCKED && q.visibility == Quest.Visibility.SECRET;
				boolean locked = status == Rules.Status.LOCKED || status == Rules.Status.HIDDEN;
				WindowArt.frame(g, q.frame, status, QuestPainter.accentOf(e.chapter()), this.x + 15, ry + 11, 10, locked ? 0.6F : 1.0F, 0.0F, a);
				if (a > 0.6F && !secret) {
					QuestIcons.of(e).draw(g, (float)(this.x + 15), (float)(ry + 11), 12.0F, van ? -13949406 : p.text());
				}

				int tx = this.x + 30;
				int tw = this.w - 30 - 8;
				g.drawString(
					font,
					CFHGuiDraw.truncate(secret ? QuestText.secret() : QuestText.title(e), tw),
					tx,
					ry + 3,
					Colors.scaleAlpha(on ? (van ? -96 : p.text()) : (van ? -1 : p.textMuted()), a),
					van
				);
				Component where = QuestText.title(e.chapter());
				if (book != null && e.chapter().isSubtree()) {
					Chapter top = Trees.top(book, e.chapter());
					if (top != e.chapter()) {
						where = Component.empty().append(QuestText.title(top)).append(" › ").append(where);
					}
				}

				g.drawString(font, CFHGuiDraw.truncate(where, tw), tx, ry + 13, Colors.scaleAlpha(van ? -5592406 : p.textDim(), a), false);
			}

			if (this.hits.size() > rows) {
				Component more = Component.translatable(
					"cfhgui_quests.search.more", new Object[]{this.first + 1, Math.min(this.hits.size(), this.first + rows), this.hits.size()}
				);
				CFHGuiDraw.textCentered(g, more, this.x + this.w / 2, yy + this.h - 11, Colors.scaleAlpha(van ? -8355712 : p.textDim(), a), false);
			}
		}
	}
}
