package com.onehumanawa.cfhcore.content.gui.quests.client.editor;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiDropdown;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSearchField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSegmented;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.ItemSpecs;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

final class PickerPopup extends WidgetPopup {
	private static final int PAD = 8;
	private static final int CELL = 18;
	private static final int LINE = 14;
	private static final int HEAD = 15;
	private static final int COLS = 13;
	private static final int GUTTER = 6;
	private final Pickers.What what;
	private final String current;
	private final Consumer<String> onPick;
	private final CFHGuiSearchField search;
	@Nullable
	private final CFHGuiSegmented<Catalog.Kind> tabs;
	@Nullable
	private final CFHGuiDropdown<String> statType;
	private final EditField manual;
	private final CFHGuiButton use;
	private Catalog.Kind kind;
	private String arg = "";
	private String query = "";
	private List<Catalog.Entry> shown = List.of();
	private final List<Catalog.Entry> carried;
	private final Anim scroll = new Anim(0.0F, 140, Ease.OUT_CUBIC);
	private int cursor = -1;
	private int hover = -1;
	private int hoverCarried = -1;
	private int toReveal = -1;
	private long hoverSinceMs;
	private int listX;
	private int listY;
	private int listW;
	private int listH;
	private int carriedY;

	PickerPopup(Pickers.What what, @Nullable String current, Consumer<String> onPick) {
		this.what = what;
		this.current = current == null ? "" : current.trim();
		this.onPick = onPick;
		Catalog.Kind first = what.kinds.get(0);
		if (this.current.startsWith("#") && what.kinds.size() > 1) {
			first = what.kinds.get(1);
		}

		if (this.current.startsWith("icon:") && what.kinds.contains(Catalog.Kind.GLYPHS)) {
			first = Catalog.Kind.GLYPHS;
		}

		this.kind = first;
		this.search = this.add(new CFHGuiSearchField(0, 0, 100, q -> {
			this.query = q == null ? "" : q;
			this.refresh();
		}));
		this.search.placeholder(Tr.t("pick.search"));
		this.search.setMaxLength(64);
		this.tabs = what.kinds.size() > 1
			? this.add(new CFHGuiSegmented<>(0, 0, 100, what.kinds, this.kind, k -> Tr.t("pick.tab." + k.name().toLowerCase(Locale.ROOT)), k -> {
				this.kind = k;
				this.refresh();
			}))
			: null;
		if (this.tabs != null) {
			this.tabs.setHeight(18);
		}

		if (what == Pickers.What.STAT) {
			List<String> types = Catalog.statTypes();
			int slash = this.current.indexOf(47, Math.max(0, this.current.indexOf(58)));
			String had = slash > 0 ? this.current.substring(0, slash) : "";
			this.arg = types.contains(had) ? had : (types.isEmpty() ? "" : types.get(0));
			this.statType = this.add(new CFHGuiDropdown<>(0, 0, 100, types, this.arg, t -> Component.literal(ItemSpecs.readable(t)), t -> {
				this.arg = t;
				this.refresh();
			}).label(Tr.t("pick.stat_type")));
			this.statType.setHeight(18);
		} else {
			this.statType = null;
		}

		this.manual = this.add(new EditField(100, Tr.t("pick.manual"), this.current).max(2048).hint(Tr.t("pick.manual")).tooltip(Tr.t("pick.manual.tip")));
		this.manual.valid(text -> Pickers.readable(what, text));
		this.use = this.add(new CFHGuiButton(0, 0, 44, 18, Tr.t("pick.use"), this::useManual));
		this.use.tip(Tr.t("pick.use.tip"));
		this.carried = what.carried() ? Catalog.carried(what == Pickers.What.ICON || what == Pickers.What.GIFT, what == Pickers.What.BLOCK) : List.of();
		this.refresh();
		this.focus(this.search);
	}

	private void refresh() {
		this.shown = Catalog.find(Catalog.of(this.kind, this.arg), this.query);
		this.scroll.snap(0.0F);
		this.cursor = -1;
		this.hover = -1;
		this.toReveal = -1;
		if (this.query.isBlank() && !this.current.isEmpty()) {
			for (int i = 0; i < this.shown.size(); i++) {
				if (this.shown.get(i).value.equals(this.current)) {
					this.cursor = i;
					this.toReveal = i;
					break;
				}
			}
		}
	}

	private boolean grid() {
		return this.kind.grid();
	}

	private int cols() {
		return this.grid() ? Math.max(1, this.listW / 18) : 1;
	}

	private int rowHeight() {
		return this.grid() ? 18 : (this.listH < 14 ? 14 : this.listH / (this.listH / 14));
	}

	private int rows() {
		return (this.shown.size() + this.cols() - 1) / this.cols();
	}

	private int maxScroll() {
		return Math.max(0, this.rows() * this.rowHeight() - this.listH);
	}

	private boolean showsCarried() {
		return !this.carried.isEmpty() && this.kind == this.what.kinds.get(0);
	}

	@Override
	protected void layout() {
		Minecraft mc = Minecraft.getInstance();
		int sw = mc.getWindow().getGuiScaledWidth();
		int sh = mc.getWindow().getGuiScaledHeight();
		this.w = Math.min(256, sw - 12);
		int inner = this.w - 16;
		int top = 39 + (this.tabs == null && this.statType == null ? 0 : 22) + (this.showsCarried() ? 33 : 0);
		int foot = 23;
		int room = sh - 16 - 16 - top - 23;
		this.listH = Math.max(36, Math.min(126, room / 18 * 18));
		this.h = 8 + top + this.listH + 23 + 8;
		this.x = (sw - this.w) / 2;
		this.y = Math.max(4, (sh - this.h) / 2);
		int yy = this.y + 8 + 15;
		this.search.setX(this.x + 8);
		this.search.setY(yy);
		this.search.setWidth(inner);
		yy += 24;
		if (this.tabs != null) {
			this.tabs.setX(this.x + 8);
			this.tabs.setY(yy);
			this.tabs.setWidth(inner);
			yy += 22;
		}

		if (this.statType != null) {
			this.statType.setX(this.x + 8);
			this.statType.setY(yy);
			this.statType.setWidth(inner);
			yy += 22;
		}

		if (this.showsCarried()) {
			this.carriedY = yy + 11;
			yy += 33;
		}

		this.listX = this.x + 8;
		this.listY = yy;
		this.listW = inner - 6;
		int my = this.listY + this.listH + 5;
		this.manual.setX(this.x + 8);
		this.manual.setY(my);
		this.manual.setWidth(inner - this.use.getWidth() - 4);
		this.use.setX(this.x + this.w - 8 - this.use.getWidth());
		this.use.setY(my);
		if (this.toReveal >= 0) {
			int rh = this.rowHeight();
			int above = Math.max(0, (this.listH / rh - 1) / 2);
			this.scroll.snap((float)Mth.clamp((this.toReveal / this.cols() - above) * rh, 0, this.maxScroll()));
			this.toReveal = -1;
		}

		this.scroll.set(Mth.clamp(this.scroll.target(), 0.0F, (float)this.maxScroll()));
	}

	private int indexAt(double mx, double my) {
		if (!(mx < (double)this.listX) && !(mx >= (double)(this.listX + this.listW)) && !(my < (double)this.listY) && !(my >= (double)(this.listY + this.listH))) {
			int row = (int)((my - (double)this.listY + (double)this.scroll.get()) / (double)this.rowHeight());
			int col = this.grid() ? (int)((mx - (double)this.listX) / 18.0) : 0;
			if (col >= this.cols()) {
				return -1;
			} else {
				int i = row * this.cols() + col;
				return i >= 0 && i < this.shown.size() ? i : -1;
			}
		} else {
			return -1;
		}
	}

	private int carriedAt(double mx, double my) {
		if (this.showsCarried() && !(my < (double)this.carriedY) && !(my >= (double)(this.carriedY + 18)) && !(mx < (double)this.listX)) {
			int i = (int)((mx - (double)this.listX) / 18.0);
			return i >= 0 && i < Math.min(this.carried.size(), (this.listW + 6) / 18) ? i : -1;
		} else {
			return -1;
		}
	}

	private void reveal(int index, boolean snap) {
		if (index >= 0 && this.listH > 0) {
			int top = index / this.cols() * this.rowHeight();
			int bottom = top + this.rowHeight();
			float to = this.scroll.target();
			if ((float)top < to) {
				to = (float)top;
			} else if ((float)bottom > to + (float)this.listH) {
				to = (float)(bottom - this.listH);
			}

			to = Mth.clamp(to, 0.0F, (float)this.maxScroll());
			if (snap) {
				this.scroll.snap(to);
			} else {
				this.scroll.set(to);
			}
		}
	}

	private void move(int by) {
		if (!this.shown.isEmpty()) {
			this.cursor = Mth.clamp(this.cursor < 0 ? 0 : this.cursor + by, 0, this.shown.size() - 1);
			this.reveal(this.cursor, false);
			CFHGuiSounds.tick();
		}
	}

	private void pick(Catalog.Entry entry) {
		CFHGuiSounds.click();
		this.close();
		this.onPick.accept(entry.value);
	}

	private void useManual() {
		this.manual.commit();
		String typed = this.manual.getValue().trim();
		this.close();
		this.onPick.accept(typed);
	}

	@Override
	protected boolean clicked(double mouseX, double mouseY, int button) {
		if (button != 0) {
			return false;
		} else {
			int c = this.carriedAt(mouseX, mouseY);
			if (c >= 0) {
				this.pick(this.carried.get(c));
				return true;
			} else {
				int i = this.indexAt(mouseX, mouseY);
				if (i >= 0) {
					this.pick(this.shown.get(i));
					return true;
				} else {
					return false;
				}
			}
		}
	}

	@Override
	protected boolean scrolled(double mouseX, double mouseY, double amount) {
		this.scroll.set(Mth.clamp(this.scroll.target() - (float)amount * (float)this.rowHeight() * (float)(this.grid() ? 2 : 3), 0.0F, (float)this.maxScroll()));
		return true;
	}

	@Override
	protected boolean key(int keyCode, int scanCode, int modifiers) {
		int page = Math.max(1, this.listH / this.rowHeight()) * this.cols();
		switch (keyCode) {
			case 262:
				this.move(1);
				break;
			case 263:
				this.move(-1);
				break;
			case 264:
				this.move(this.cols());
				break;
			case 265:
				if (this.cursor >= 0 && this.cursor < this.cols()) {
					this.cursor = -1;
					this.focus(this.search);
				} else {
					this.move(-this.cols());
				}
				break;
			case 266:
				this.move(-page);
				break;
			case 267:
				this.move(page);
				break;
			default:
				return false;
		}

		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.cursor >= 0 && this.grid() && this.focused == this.search && (keyCode == 262 || keyCode == 263) && (modifiers & 7) == 0) {
			this.move(keyCode == 262 ? 1 : -1);
			return true;
		} else {
			return super.keyPressed(keyCode, scanCode, modifiers);
		}
	}

	List<Catalog.Entry> shownForTest() {
		return this.shown;
	}

	int[] placeForTest(int index) {
		this.layout();
		int col = index % this.cols();
		int row = index / this.cols();
		return new int[]{this.listX + (this.grid() ? col * 18 : 0) + 5, this.listY + row * this.rowHeight() - Math.round(this.scroll.get()) + 5};
	}

	void searchForTest(String typed) {
		this.search.setValue(typed);
	}

	void manualForTest(String written) {
		this.manual.setValue(written);
		this.useManual();
	}

	@Override
	protected void submit() {
		if (this.focused != this.manual && this.focused != this.use) {
			int i = this.cursor >= 0 ? this.cursor : (this.shown.size() != 1 && (this.query.isBlank() || this.shown.isEmpty()) ? -1 : 0);
			if (i >= 0 && i < this.shown.size()) {
				this.pick(this.shown.get(i));
			}
		} else {
			this.useManual();
		}
	}

	@Override
	protected void drawUnder(GuiGraphics g, int mouseX, int mouseY, float a) {
		Theme t = Theme.current();
		Palette p = t.palette();
		boolean van = t.isVanilla();
		Font font = CFHGuiDraw.font();
		Icons.draw(g, this.what.glyph(), this.x + 8, this.y + 8 - 1, 12, Colors.scaleAlpha(van ? -1 : t.accentInk(), a));
		g.drawString(font, Fonts.heading(Tr.t("pick.title." + this.what.key())), this.x + 8 + 16, this.y + 8 + 1, Colors.scaleAlpha(van ? -1 : p.text(), a), van);
		String count = Integer.toString(this.shown.size());
		g.drawString(font, count, this.x + this.w - 8 - font.width(count), this.y + 8 + 1, Colors.scaleAlpha(van ? -6250336 : p.textDim(), a), van);
		this.hoverCarried = this.carriedAt((double)mouseX, (double)mouseY);
		int now = this.hoverCarried >= 0 ? -1 : this.indexAt((double)mouseX, (double)mouseY);
		if (now != this.hover) {
			this.hover = now;
			this.hoverSinceMs = Clock.nowMs();
		}

		if (this.showsCarried()) {
			g.drawString(font, Tr.t("pick.carried"), this.listX, this.carriedY - 10, Colors.scaleAlpha(van ? -6250336 : p.textMuted(), a), van);
			int n = Math.min(this.carried.size(), (this.listW + 6) / 18);

			for (int i = 0; i < n; i++) {
				this.cell(g, this.carried.get(i), this.listX + i * 18, this.carriedY, i == this.hoverCarried, false, false, van, a);
			}
		}

		if (van) {
			g.fill(this.listX - 1, this.listY - 1, this.listX + this.listW + 6 + 1, this.listY + this.listH + 1, Colors.scaleAlpha(-16777216, a));
			g.fill(this.listX, this.listY, this.listX + this.listW + 6, this.listY + this.listH, Colors.scaleAlpha(-14013910, a));
		} else {
			CFHGuiDraw.pixelRound(g, this.listX - 1, this.listY - 1, this.listW + 6 + 2, this.listH + 2, Colors.scaleAlpha(Colors.mix(p.bg(), -16777216, 0.25F), a), 2);
			CFHGuiDraw.outline(g, this.listX - 1, this.listY - 1, this.listW + 6 + 2, this.listH + 2, Colors.scaleAlpha(p.border(), a), 2);
		}

		if (this.shown.isEmpty()) {
			Component words = !this.query.isBlank() ? Tr.t("pick.none", this.query) : Tr.t("pick.empty." + this.kind.name().toLowerCase(Locale.ROOT));
			int ty = this.listY + this.listH / 2 - 10;

			for (FormattedCharSequence line : font.split(words, this.listW - 12)) {
				g.drawString(font, line, this.listX + (this.listW + 6 - font.width(line)) / 2, ty, Colors.scaleAlpha(van ? -6250336 : p.textDim(), a), van);
				ty += 10;
			}
		} else {
			int s = Math.round(this.scroll.get());
			g.enableScissor(this.listX, this.listY + this.rise(), this.listX + this.listW, this.listY + this.listH + this.rise());
			int cols = this.cols();
			int rh = this.rowHeight();
			int firstRow = Math.max(0, s / rh);
			int lastRow = Math.min(this.rows() - 1, (s + this.listH) / rh);

			for (int row = firstRow; row <= lastRow; row++) {
				for (int col = 0; col < cols; col++) {
					int i = row * cols + col;
					if (i >= this.shown.size()) {
						break;
					}

					Catalog.Entry e = this.shown.get(i);
					int ex = this.listX + col * 18;
					int ey = this.listY + row * rh - s;
					if (this.grid()) {
						this.cell(g, e, ex, ey, i == this.hover, i == this.cursor, e.value.equals(this.current), van, a);
					} else {
						this.line(g, e, this.listX, ey, i == this.hover, i == this.cursor, e.value.equals(this.current), van, a);
					}
				}
			}

			g.disableScissor();
			if (this.maxScroll() > 0) {
				int barH = Math.max(10, this.listH * this.listH / (this.rows() * rh));
				int barY = this.listY + Math.round((float)(this.listH - barH) * (this.scroll.get() / (float)this.maxScroll()));
				g.fill(this.listX + this.listW + 2, barY, this.listX + this.listW + 4, barY + barH, Colors.scaleAlpha(van ? -4144960 : p.borderStrong(), a));
			}

			Catalog.Entry over = this.hoverCarried >= 0
				? this.carried.get(this.hoverCarried)
				: (this.hover >= 0 && this.hover < this.shown.size() ? this.shown.get(this.hover) : null);
			if (over != null && Clock.nowMs() - this.hoverSinceMs > (long)(!this.grid() && this.hoverCarried < 0 ? 700 : 250)) {
				List<Component> tip = new ArrayList<>();
				tip.add(Component.literal(over.name));
				tip.add(Component.literal(over.id).withStyle(st -> st.withColor(10592151)));
				CFHGuiTooltips.request(tip, null);
			}
		}
	}

	@Override
	protected void drawOver(GuiGraphics g, int mouseX, int mouseY, float a) {
		if (this.search.getValue().isEmpty() && this.search.isFocused()) {
			boolean van = this.vanilla();
			Component hint = Tr.t("pick.search");
			int left = this.search.getX() + 31;
			g.drawString(
				CFHGuiDraw.font(),
				CFHGuiDraw.truncate(hint, this.search.getX() + this.search.getWidth() - 8 - left),
				left,
				this.search.getY() + (this.search.getHeight() - 9) / 2 + 1,
				Colors.scaleAlpha(van ? -9408400 : Theme.current().palette().textDim(), a),
				false
			);
		}
	}

	private void cell(GuiGraphics g, Catalog.Entry e, int x, int y, boolean hovered, boolean marked, boolean held, boolean van, float a) {
		Theme t = Theme.current();
		Palette p = t.palette();
		int accent = van ? -1 : t.accent();
		if (hovered) {
			CFHGuiDraw.pixelRound(g, x, y, 18, 18, Colors.scaleAlpha(van ? 1627389951 : p.surfaceActive(), a), van ? 0 : 2);
		}

		if (held) {
			g.fill(x + 3, y + 18 - 2, x + 18 - 3, y + 18 - 1, Colors.scaleAlpha(accent, a));
		}

		if (marked) {
			CFHGuiDraw.outline(g, x, y, 18, 18, Colors.scaleAlpha(accent, a), van ? 0 : 2);
		}

		if (a > 0.6F) {
			e.draw(g, x + 9, y + 9, 16, van ? -1 : (hovered ? p.text() : p.textMuted()));
		}
	}

	private void line(GuiGraphics g, Catalog.Entry e, int x, int y, boolean hovered, boolean marked, boolean held, boolean van, float a) {
		Theme t = Theme.current();
		Palette p = t.palette();
		Font font = CFHGuiDraw.font();
		int accent = van ? -1 : t.accent();
		int rh = this.rowHeight();
		int ty = y + (rh - 8) / 2;
		if (hovered || marked) {
			CFHGuiDraw.pixelRound(
				g, x, y, this.listW, rh, Colors.scaleAlpha(van ? (marked ? 1627389951 : 822083583) : (marked ? p.surfaceActive() : p.surfaceHover()), a), van ? 0 : 2
			);
		}

		if (held) {
			g.fill(x, y + 3, x + 2, y + rh - 3, Colors.scaleAlpha(accent, a));
		}

		int tx = x + 4;
		if (e.hasPicture()) {
			if (a > 0.6F) {
				e.draw(g, tx + 6, y + rh / 2, 12, van ? -1 : p.textMuted());
			}

			tx += 16;
		}

		int room = x + this.listW - 4 - tx;
		FormattedCharSequence name = CFHGuiDraw.truncate(Component.literal(e.name), room * 3 / 5);
		g.drawString(font, name, tx, ty, Colors.scaleAlpha(van ? -1 : (!hovered && !marked ? Colors.mix(p.text(), p.textMuted(), 0.4F) : p.text()), a), van);
		int idRoom = room - font.width(name) - 8;
		if (idRoom > 24) {
			FormattedCharSequence id = CFHGuiDraw.truncate(Component.literal(e.id), idRoom);
			g.drawString(font, id, x + this.listW - 4 - font.width(id), ty, Colors.scaleAlpha(van ? -8355712 : p.textDim(), a), van);
		}
	}
}
