package com.onehumanawa.cfhcore.content.gui.quests.client.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSearchField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.CanvasDraw;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestPainter;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.logic.Rules;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class SearchBox {
	private static final int ROW = 24;
	private static final int MAX_ROWS = 8;
	private static final int PAD = 3;
	private static final int WIDTH = 236;
	private final Consumer<String> goTo;
	@Nullable
	private CFHGuiSearchField field;
	private String query = "";
	private List<QuestSearch.Hit> hits = List.of();
	private boolean open;
	private int marked;
	private int first;
	private int lastMouseX = Integer.MIN_VALUE;
	private int lastMouseY = Integer.MIN_VALUE;
	private final Anim shown = new Anim(0.0F, 150, Ease.OUT_CUBIC);
	private int x;
	private int y;
	private int w;
	private int h;
	private GuiGraphics frameG;
	private int frameMouseX;
	private int frameMouseY;
	private final Runnable paint = this::paint;

	public SearchBox(Consumer<String> goTo) {
		this.goTo = goTo;
	}

	public CFHGuiSearchField field(int width) {
		CFHGuiSearchField f = new CFHGuiSearchField(0, 0, width, this::typed);
		f.placeholder(Component.translatable("cfhgui_quests.search.placeholder"));
		f.setMaxLength(64);
		this.field = f;
		if (!this.query.isEmpty()) {
			f.text(this.query);
		}

		return f;
	}

	private void typed(String text) {
		this.query = text == null ? "" : text;
		this.hits = QuestSearch.find(this.query, 40);
		this.marked = 0;
		this.first = 0;
		this.open = !this.query.isBlank();
		this.shown.set(this.open ? 1.0F : 0.0F);
	}

	public boolean isOpen() {
		return this.open;
	}

	public void close() {
		this.open = false;
		this.shown.set(0.0F);
	}

	private int rows() {
		return Math.max(1, Math.min(8, this.hits.size()));
	}

	private void place(int screenWidth) {
		if (this.field != null) {
			this.w = Math.min(236, screenWidth - 16);
			this.h = this.rows() * 24 + 6 + (this.hits.size() > 8 ? 10 : 0);
			this.x = Mth.clamp(this.field.getX() + this.field.getWidth() - this.w, 8, Math.max(8, screenWidth - 8 - this.w));
			this.y = this.field.getY() + this.field.getHeight() + 3;
		}
	}

	public boolean contains(double mouseX, double mouseY) {
		return this.open && mouseX >= (double)this.x && mouseX < (double)(this.x + this.w) && mouseY >= (double)this.y && mouseY < (double)(this.y + this.h);
	}

	private int rowAt(double mouseY) {
		int i = (int)Math.floor((mouseY - (double)this.y - 3.0) / 24.0);
		return i >= 0 && i < this.rows() && this.first + i < this.hits.size() ? this.first + i : -1;
	}

	private void pick(int i) {
		if (i >= 0 && i < this.hits.size()) {
			String key = this.hits.get(i).entry().key();
			CFHGuiSounds.click();
			this.close();
			if (this.field != null) {
				this.field.setFocused(false);
			}

			this.goTo.accept(key);
		}
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!this.open) {
			return false;
		} else if (this.contains(mouseX, mouseY)) {
			if (button == 0) {
				this.pick(this.rowAt(mouseY));
			}

			return true;
		} else {
			if (this.field == null || !this.field.isMouseOver(mouseX, mouseY)) {
				this.close();
			}

			return false;
		}
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
		if (!this.contains(mouseX, mouseY)) {
			return false;
		} else {
			this.first = Mth.clamp(this.first - (int)Math.signum(scrollY), 0, Math.max(0, this.hits.size() - 8));
			return true;
		}
	}

	public boolean keyPressed(int keyCode) {
		if (this.field == null || !this.field.isFocused()) {
			return false;
		} else if (this.open) {
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
		} else if (keyCode == 264 && !this.query.isBlank()) {
			this.open = true;
			this.shown.set(1.0F);
			return true;
		} else {
			return false;
		}
	}

	private void mark(int i) {
		if (!this.hits.isEmpty()) {
			this.marked = Math.floorMod(i, this.hits.size());
			if (this.marked < this.first) {
				this.first = this.marked;
			} else if (this.marked >= this.first + 8) {
				this.first = this.marked - 8 + 1;
			}
		}
	}

	public void render(GuiGraphics g, int mouseX, int mouseY, int screenWidth) {
		if (this.open && this.field != null && !this.field.isFocused()) {
			this.close();
		}

		float a = this.shown.get();
		if (!(a <= 0.02F)) {
			this.place(screenWidth);
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
		if (van) {
			g.fill(this.x, yy, this.x + this.w, yy + this.h, Colors.withAlpha(0, Math.round(240.0F * a)));
			CFHGuiDraw.outline(g, this.x, yy, this.w, this.h, Colors.withAlpha(16777215, Math.round(255.0F * a)), 0);
		} else {
			CFHGuiDraw.popupPlate(g, this.x, yy, this.w, this.h, p.surface(), p.borderStrong(), 0.6F, a);
		}

		if (this.hits.isEmpty()) {
			g.drawString(
				font,
				CFHGuiDraw.truncate(Component.translatable("cfhgui_quests.search.none", new Object[]{this.query.trim()}), this.w - 16),
				this.x + 8,
				yy + 3 + 8,
				Colors.scaleAlpha(van ? -6250336 : p.textMuted(), a),
				van
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

			for (int i = 0; i < this.rows() && this.first + i < this.hits.size(); i++) {
				BookIndex.Entry e = this.hits.get(this.first + i).entry();
				int ry = yy + 3 + i * 24;
				boolean on = this.first + i == this.marked;
				if (on) {
					CFHGuiDraw.pixelRound(g, this.x + 2, ry, this.w - 4, 23, Colors.scaleAlpha(van ? -12566464 : p.surfaceHover(), a), van ? 0 : 2);
				}

				Rules.Status status = QuestsState.status(e.key());
				int accent = QuestPainter.accentOf(e.chapter());
				UiBits.medallion(g, e, status, accent, this.x + 3 + 12, ry + 11, 0.78F, a);
				int tx = this.x + 3 + 26;
				int tw = this.w - 6 - 26 - 10;
				g.drawString(
					font, CFHGuiDraw.truncate(QuestText.title(e), tw), tx, ry + 3, Colors.scaleAlpha(on ? (van ? -96 : p.text()) : (van ? -2039584 : p.textMuted()), a), van
				);
				Component where = QuestText.title(e.chapter());
				if (book != null && e.chapter().isSubtree()) {
					Chapter top = Trees.top(book, e.chapter());
					if (top != e.chapter()) {
						where = Component.empty().append(QuestText.title(top)).append(" › ").append(where);
					}
				}

				g.drawString(font, CFHGuiDraw.truncate(where, tw), tx, ry + 13, Colors.scaleAlpha(van ? -8355712 : p.textDim(), a), van);
				CFHGuiDraw.pixelCircle(g, this.x + this.w - 3 - 6, ry + 11, 2, Colors.scaleAlpha(UiBits.statusColor(status, accent), a));
			}

			if (this.hits.size() > 8) {
				Component more = Component.translatable(
					"cfhgui_quests.search.more", new Object[]{this.first + 1, Math.min(this.hits.size(), this.first + 8), this.hits.size()}
				);
				CFHGuiDraw.textCentered(g, more, this.x + this.w / 2, yy + this.h - 11, Colors.scaleAlpha(van ? -8355712 : p.textDim(), a), van);
			}
		}
	}
}
