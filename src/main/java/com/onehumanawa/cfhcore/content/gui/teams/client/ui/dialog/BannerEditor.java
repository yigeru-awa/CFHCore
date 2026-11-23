package com.onehumanawa.cfhcore.content.gui.teams.client.ui.dialog;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import com.onehumanawa.cfhcore.content.gui.teams.client.Banners;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.BannerArt;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsDraw;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.model.BannerData;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;

public class BannerEditor extends CFHGuiWidget {
	public static final int SMALL = 0;
	public static final int MEDIUM = 1;
	public static final int LARGE = 2;
	private static final int PATTERN_ROWS = 3;
	private static final int CAPTION = 11;
	private static final int[] WELL_W = new int[]{52, 78, 104};
	private static final int[] FLAG_MAX = new int[]{32, 48, 64};
	private static final int[] LAYER_W = new int[]{16, 22, 28};
	private static final int[] LAYER_H = new int[]{28, 40, 52};
	private static final int[] LAYER_GAP = new int[]{2, 3, 4};
	private static final int[] DYE = new int[]{13, 19, 26};
	private static final int[] DYE_GAP = new int[]{1, 2, 2};
	private static final int[] PATTERN_W = new int[]{12, 17, 22};
	private static final int[] PATTERN_H = new int[]{22, 32, 42};
	private static final int[] PATTERN_GAP = new int[]{2, 3, 4};
	private static final int[] TOOL = new int[]{14, 18, 22};
	private static final int[] ROW_GAP = new int[]{4, 6, 8};
	private static final int[] GLYPH = new int[]{8, 12, 12};
	private final BannerData value;
	private final List<ResourceLocation> patterns = BannerArt.patterns();
	private final boolean captions;
	private final int size;
	private int selected;
	private DyeColor armedColor = DyeColor.WHITE;
	private String armedPattern = "minecraft:border";
	private int patternScroll;
	private final int layersY;
	private final int dyesY;
	private final int patternsY;

	public BannerEditor(int x, int y, int width, BannerData initial, boolean captions) {
		this(x, y, width, initial, captions, 0);
	}

	public BannerEditor(int x, int y, int width, BannerData initial, boolean captions, int size) {
		super(x, y, width, 10, TeamsText.t("banner.title"));
		this.value = copy(initial);
		this.captions = captions;
		this.size = Mth.clamp(size, 0, 2);
		int cap = captions ? 11 : 0;
		this.layersY = cap;
		this.dyesY = this.layersY + LAYER_H[this.size] + ROW_GAP[this.size] + cap;
		this.patternsY = this.dyesY + DYE[this.size] + ROW_GAP[this.size] + cap;
		this.setHeight(heightFor(captions, this.size));
		if (!this.value.layers.isEmpty()) {
			BannerData.Layer last = this.value.layers.get(this.value.layers.size() - 1);
			this.armedColor = Banners.dye(last.color);
		} else if (Banners.dye(this.value.base) == DyeColor.WHITE) {
			this.armedColor = DyeColor.BLACK;
		}
	}

	public static int heightFor(boolean captions) {
		return heightFor(captions, 0);
	}

	public static int heightFor(boolean captions, int size) {
		int s = Mth.clamp(size, 0, 2);
		int cap = captions ? 11 : 0;
		return cap + LAYER_H[s] + ROW_GAP[s] + cap + DYE[s] + ROW_GAP[s] + cap + 3 * (PATTERN_H[s] + PATTERN_GAP[s]) - PATTERN_GAP[s];
	}

	public static int minWidth() {
		return minWidth(0);
	}

	public static int minWidth(int size) {
		int s = Mth.clamp(size, 0, 2);
		return columnX(s) + 16 * (DYE[s] + DYE_GAP[s]) - DYE_GAP[s];
	}

	public static int sizeFor(int width, int height) {
		for (int s = 2; s > 0; s--) {
			if (minWidth(s) <= width && heightFor(true, s) <= height) {
				return s;
			}
		}

		return 0;
	}

	public static BannerData copy(BannerData b) {
		BannerData out = new BannerData(b.base);

		for (BannerData.Layer l : b.layers) {
			out.layers.add(new BannerData.Layer(l.pattern, l.color));
		}

		return out;
	}

	public BannerData value() {
		return copy(this.value);
	}

	private static int columnX(int size) {
		return WELL_W[size] + 8;
	}

	private int columnX() {
		return this.getX() + columnX(this.size);
	}

	private int columnW() {
		return this.getWidth() - columnX(this.size);
	}

	private int layerW() {
		return LAYER_W[this.size];
	}

	private int layerH() {
		return LAYER_H[this.size];
	}

	private int dye() {
		return DYE[this.size];
	}

	private int dyeStep() {
		return DYE[this.size] + DYE_GAP[this.size];
	}

	private int patternW() {
		return PATTERN_W[this.size];
	}

	private int patternH() {
		return PATTERN_H[this.size];
	}

	private int patternStepX() {
		return PATTERN_W[this.size] + PATTERN_GAP[this.size];
	}

	private int patternStepY() {
		return PATTERN_H[this.size] + PATTERN_GAP[this.size];
	}

	private int tool() {
		return TOOL[this.size];
	}

	private int glyph() {
		return GLYPH[this.size];
	}

	private int layerCells() {
		return 1 + this.value.layers.size() + (this.value.layers.size() < 6 ? 1 : 0);
	}

	private int layerX(int i) {
		return this.columnX() + i * (this.layerW() + LAYER_GAP[this.size]);
	}

	private int toolX(int i) {
		return this.columnX() + this.columnW() - (3 - i) * (this.tool() + 1) + 1;
	}

	private int toolY() {
		return this.getY() + this.layersY + (this.layerH() - this.tool()) / 2;
	}

	private int dyeX(int i) {
		return this.columnX() + i * this.dyeStep();
	}

	private int perRow() {
		return Math.max(1, (this.columnW() + PATTERN_GAP[this.size]) / this.patternStepX());
	}

	private int patternRows() {
		return (this.patterns.size() + this.perRow() - 1) / this.perRow();
	}

	private int maxScroll() {
		return Math.max(0, this.patternRows() - 3);
	}

	private int patternX(int i) {
		return this.columnX() + i % this.perRow() * this.patternStepX();
	}

	private int patternY(int i) {
		return this.getY() + this.patternsY + (i / this.perRow() - this.patternScroll) * this.patternStepY();
	}

	private int layerAt(double mx, double my) {
		int y = this.getY() + this.layersY;
		if (!(my < (double)y) && !(my >= (double)(y + this.layerH()))) {
			for (int i = 0; i < this.layerCells(); i++) {
				if (mx >= (double)this.layerX(i) && mx < (double)(this.layerX(i) + this.layerW())) {
					return i;
				}
			}

			return -1;
		} else {
			return -1;
		}
	}

	private int toolAt(double mx, double my) {
		if (!(my < (double)this.toolY()) && !(my >= (double)(this.toolY() + this.tool()))) {
			for (int i = 0; i < 3; i++) {
				if (mx >= (double)this.toolX(i) && mx < (double)(this.toolX(i) + this.tool())) {
					return i;
				}
			}

			return -1;
		} else {
			return -1;
		}
	}

	private int dyeAt(double mx, double my) {
		int y = this.getY() + this.dyesY;
		if (!(my < (double)y) && !(my >= (double)(y + this.dye()))) {
			int i = (int)((mx - (double)this.columnX()) / (double)this.dyeStep());
			return mx >= (double)this.columnX() && i >= 0 && i < Banners.COLORS.size() ? i : -1;
		} else {
			return -1;
		}
	}

	private int patternAt(double mx, double my) {
		int top = this.getY() + this.patternsY;
		if (!(my < (double)top) && !(my >= (double)(top + 3 * this.patternStepY())) && !(mx < (double)this.columnX())) {
			int col = (int)((mx - (double)this.columnX()) / (double)this.patternStepX());
			int row = (int)((my - (double)top) / (double)this.patternStepY()) + this.patternScroll;
			if (col >= this.perRow()) {
				return -1;
			} else {
				int i = row * this.perRow() + col;
				return i >= 0 && i < this.patterns.size() ? i : -1;
			}
		} else {
			return -1;
		}
	}

	private DyeColor colorOf(int cell) {
		return cell > 0 && cell <= this.value.layers.size() ? Banners.dye(this.value.layers.get(cell - 1).color) : Banners.dye(this.value.base);
	}

	private void pickDye(DyeColor dye) {
		if (this.selected == 0) {
			this.value.base = dye.getName();
		} else {
			this.value.layers.get(this.selected - 1).color = dye.getName();
		}

		if (this.selected > 0) {
			this.armedColor = dye;
		}

		CFHGuiSounds.tick();
	}

	private void pickPattern(ResourceLocation pattern) {
		this.armedPattern = pattern.toString();
		if (this.selected > 0) {
			this.value.layers.get(this.selected - 1).pattern = this.armedPattern;
		} else {
			if (this.value.layers.size() >= 6) {
				return;
			}

			this.value.layers.add(new BannerData.Layer(this.armedPattern, this.contrast().getName()));
			this.selected = this.value.layers.size();
		}

		CFHGuiSounds.tick();
	}

	private DyeColor contrast() {
		DyeColor base = Banners.dye(this.value.base);
		if (this.armedColor != base) {
			return this.armedColor;
		} else {
			return Colors.luminance(BannerArt.rgb(base)) > 0.5F ? DyeColor.BLACK : DyeColor.WHITE;
		}
	}

	private void addLayer() {
		if (this.value.layers.size() < 6) {
			this.value.layers.add(new BannerData.Layer(this.armedPattern, this.contrast().getName()));
			this.selected = this.value.layers.size();
			CFHGuiSounds.tick();
		}
	}

	private void removeLayer(int cell) {
		if (cell > 0 && cell <= this.value.layers.size()) {
			this.value.layers.remove(cell - 1);
			this.selected = Math.min(this.selected, this.value.layers.size());
			CFHGuiSounds.tick();
		}
	}

	private void moveLayer(int by) {
		int from = this.selected - 1;
		int to = from + by;
		if (from >= 0 && to >= 0 && to < this.value.layers.size()) {
			BannerData.Layer l = this.value.layers.remove(from);
			this.value.layers.add(to, l);
			this.selected = to + 1;
			CFHGuiSounds.tick();
		}
	}

	private boolean toolEnabled(int tool) {
		return switch (tool) {
			case 0 -> this.selected > 1;
			case 1 -> this.selected > 0 && this.selected < this.value.layers.size();
			default -> this.selected > 0;
		};
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!this.active || !this.visible || !this.contains(mouseX, mouseY)) {
			return false;
		} else if (button == 1) {
			int cell = this.layerAt(mouseX, mouseY);
			if (cell > 0 && cell <= this.value.layers.size()) {
				this.removeLayer(cell);
			}

			return true;
		} else if (button != 0) {
			return true;
		} else {
			int tool = this.toolAt(mouseX, mouseY);
			if (tool >= 0) {
				if (this.toolEnabled(tool)) {
					if (tool == 0) {
						this.moveLayer(-1);
					} else if (tool == 1) {
						this.moveLayer(1);
					} else {
						this.removeLayer(this.selected);
					}
				}

				return true;
			} else {
				int cell = this.layerAt(mouseX, mouseY);
				if (cell >= 0) {
					if (cell > this.value.layers.size()) {
						this.addLayer();
					} else if (cell != this.selected) {
						this.selected = cell;
						CFHGuiSounds.tick();
					}

					return true;
				} else {
					int dye = this.dyeAt(mouseX, mouseY);
					if (dye >= 0) {
						this.pickDye(Banners.COLORS.get(dye));
						return true;
					} else {
						int pattern = this.patternAt(mouseX, mouseY);
						if (pattern >= 0) {
							this.pickPattern(this.patterns.get(pattern));
						}

						return true;
					}
				}
			}
		}
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (this.contains(mouseX, mouseY) && this.maxScroll() != 0) {
			this.patternScroll = Mth.clamp(this.patternScroll - (int)Math.signum(scrollY), 0, this.maxScroll());
			return true;
		} else {
			return false;
		}
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.active && this.visible) {
			switch (keyCode) {
				case 259:
				case 261:
					this.removeLayer(this.selected);
					break;
				case 260:
				default:
					return false;
				case 262:
					this.selected = Math.min(this.value.layers.size(), this.selected + 1);
					break;
				case 263:
					this.selected = Math.max(0, this.selected - 1);
			}

			return true;
		} else {
			return false;
		}
	}

	public void playDownSound(SoundManager handler) {
	}

	protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.draw(g, mouseX, mouseY, false);
	}

	protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.draw(g, mouseX, mouseY, true);
	}

	private static void cell(GuiGraphics g, int x, int y, int w, int h, boolean selected, boolean hover, boolean van, float a) {
		Palette p = Theme.current().palette();
		if (van) {
			CFHGuiDraw.rect(g, x, y, w, h, Colors.scaleAlpha(hover ? -1879048192 : 1610612736, a));
			CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(selected ? -1 : (hover ? -6250336 : -12566464), a), 0);
		} else {
			CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(selected ? p.surfaceActive() : (hover ? p.surfaceHover() : p.bg2()), a), 2);
			CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(selected ? p.accent() : (hover ? p.borderStrong() : p.border()), a), 2);
		}
	}

	private void draw(GuiGraphics g, int mouseX, int mouseY, boolean van) {
		float a = this.effectiveAlpha();
		if (!(a <= 0.004F)) {
			Palette p = Theme.current().palette();
			int x = this.getX();
			int y = this.getY();
			int h = this.getHeight();
			boolean over = this.isHovered() && this.active && g.containsPointInScissor(mouseX, mouseY);
			DyeColor base = Banners.dye(this.value.base);
			int lw = this.layerW();
			int lh = this.layerH();
			int glyph = this.glyph();
			TeamsDraw.well(g, x, y, WELL_W[this.size], h, a);
			int flagW = Math.min(FLAG_MAX[this.size], (h - 16) / 2);
			TeamsDraw.banner(g, this.value, x + (WELL_W[this.size] - flagW) / 2, y + (h - TeamsDraw.bannerHeight(flagW)) / 2, flagW, 1.0F, 1.4F, a);
			if (this.captions) {
				TeamsDraw.text(g, TeamsText.t("banner.layers", this.value.layers.size(), 6), this.columnX(), y + this.layersY - 11, TeamsDraw.caption(), a);
				TeamsDraw.text(g, TeamsText.t("banner.colour"), this.columnX(), y + this.dyesY - 11, TeamsDraw.caption(), a);
				TeamsDraw.text(g, TeamsText.t("banner.pattern"), this.columnX(), y + this.patternsY - 11, TeamsDraw.caption(), a);
			}

			int hoverLayer = over ? this.layerAt((double)mouseX, (double)mouseY) : -1;
			int artW = lw - 4;
			int artH = artW * 2;

			for (int i = 0; i < this.layerCells(); i++) {
				int cx = this.layerX(i);
				int cy = y + this.layersY;
				cell(g, cx, cy, lw, lh, i == this.selected, i == hoverLayer, van, a);
				if (i > this.value.layers.size()) {
					Icons.draw(g, Icon.PLUS, cx + (lw - glyph) / 2, cy + (lh - glyph) / 2, glyph, Colors.scaleAlpha(i == hoverLayer ? p.text() : p.textMuted(), a));
				} else if (i == 0) {
					BannerArt.flag(g, new BannerData(this.value.base), (float)(cx + 2), (float)(cy + 2), (float)artW, a);
				} else {
					BannerData.Layer l = this.value.layers.get(i - 1);
					ResourceLocation id = ResourceLocation.tryParse(l.pattern);
					if (id != null) {
						BannerArt.pattern(g, id, Banners.dye(l.color), base, (float)(cx + 2), (float)(cy + 2), (float)artW, (float)artH, a);
					}
				}
			}

			if (hoverLayer >= 0) {
				if (hoverLayer > this.value.layers.size()) {
					CFHGuiTooltips.request(TeamsText.t("banner.add"), this);
				} else if (hoverLayer == 0) {
					CFHGuiTooltips.request(TeamsText.t("banner.cloth", BannerArt.dyeName(base)), this);
				} else {
					BannerData.Layer l = this.value.layers.get(hoverLayer - 1);
					ResourceLocation id = ResourceLocation.tryParse(l.pattern);
					CFHGuiTooltips.request(
						List.of(
							id == null ? Component.literal(l.pattern) : BannerArt.patternName(id, Banners.dye(l.color)),
							TeamsText.t("banner.layer.hint").withStyle(ChatFormatting.GRAY)
						),
						this
					);
				}
			}

			int hoverTool = over ? this.toolAt((double)mouseX, (double)mouseY) : -1;
			Icon[] tools = new Icon[]{Icon.CHEVRON_LEFT, Icon.CHEVRON_RIGHT, Icon.TRASH};
			int ts = this.tool();

			for (int ix = 0; ix < 3; ix++) {
				boolean on = this.toolEnabled(ix);
				int tx = this.toolX(ix);
				int ty = this.toolY();
				if (van) {
					CFHGuiDraw.vanillaButton(g, tx, ty, ts, ts, on && ix == hoverTool ? 1.0F : 0.0F, on, a);
				} else {
					cell(g, tx, ty, ts, ts, false, on && ix == hoverTool, false, a);
				}

				int fg = !on ? p.textDim() : (ix == 2 && ix == hoverTool ? p.danger() : (ix == hoverTool ? p.text() : p.textMuted()));
				Icons.draw(g, tools[ix], tx + (ts - glyph) / 2, ty + (ts - glyph) / 2, glyph, Colors.scaleAlpha(fg, a * (on ? 1.0F : 0.6F)));
			}

			if (hoverTool >= 0) {
				CFHGuiTooltips.request(TeamsText.t(hoverTool == 0 ? "banner.under" : (hoverTool == 1 ? "banner.over" : "banner.remove")), this);
			}

			DyeColor current = this.colorOf(this.selected);
			int hoverDye = over ? this.dyeAt((double)mouseX, (double)mouseY) : -1;
			int ds = this.dye();
			int corner = van ? 0 : (this.size == 0 ? 2 : 3);
			int inset = Math.max(2, corner);

			for (int ix = 0; ix < Banners.COLORS.size(); ix++) {
				DyeColor dye = Banners.COLORS.get(ix);
				int dx = this.dyeX(ix);
				int dy = y + this.dyesY - (ix == hoverDye ? 1 : 0);
				CFHGuiDraw.pixelRound(g, dx, dy, ds, ds, Colors.scaleAlpha(BannerArt.rgb(dye), a), corner);
				CFHGuiDraw.hline(g, dx + inset, dy + 1, ds - inset * 2, Colors.scaleAlpha(822083583, a));
				int rgb = BannerArt.rgb(dye);
				int edge = dye == current ? (van ? -1 : p.text()) : (van ? -16777216 : Colors.brighten(rgb, -0.35F));
				CFHGuiDraw.outline(g, dx, dy, ds, ds, Colors.scaleAlpha(edge, a), corner);
				if (dye == current) {
					Icons.draw(g, Icon.CHECK, dx + (ds - glyph + 1) / 2, dy + (ds - glyph + 1) / 2, glyph, Colors.scaleAlpha(Colors.readableOn(rgb), a));
				}
			}

			if (hoverDye >= 0) {
				CFHGuiTooltips.request(BannerArt.dyeName(Banners.COLORS.get(hoverDye)), this);
			}

			DyeColor ink = this.selected > 0 ? current : this.contrast();
			int hoverPattern = over ? this.patternAt((double)mouseX, (double)mouseY) : -1;
			String worn = this.selected > 0 ? this.value.layers.get(this.selected - 1).pattern : "";
			int pw = this.patternW();
			int ph = this.patternH();
			int first = this.patternScroll * this.perRow();
			int last = Math.min(this.patterns.size(), first + 3 * this.perRow());

			for (int ixx = first; ixx < last; ixx++) {
				ResourceLocation id = this.patterns.get(ixx);
				int cx = this.patternX(ixx);
				int cy = this.patternY(ixx);
				boolean sel = id.toString().equals(worn);
				boolean hov = ixx == hoverPattern;
				if (sel || hov) {
					cell(g, cx - 1, cy - 1, pw + 2, ph + 2, sel, hov, van, a);
				}

				BannerArt.pattern(g, id, ink, base, (float)(cx + 1), (float)(cy + 1), (float)(pw - 2), (float)(ph - 2), a);
			}

			if (hoverPattern >= 0) {
				CFHGuiTooltips.request(BannerArt.patternName(this.patterns.get(hoverPattern), ink), this);
			}

			if (this.maxScroll() > 0) {
				int trackH = 3 * this.patternStepY() - PATTERN_GAP[this.size];
				int barH = Math.max(8, trackH * 3 / this.patternRows());
				int bx = x + this.getWidth() - 2;
				int by = y + this.patternsY + (trackH - barH) * this.patternScroll / this.maxScroll();
				CFHGuiDraw.rect(g, bx, y + this.patternsY, 2, trackH, Colors.scaleAlpha(van ? -16777216 : p.surface(), a));
				CFHGuiDraw.rect(g, bx, by, 2, barH, Colors.scaleAlpha(van ? -4144960 : p.borderStrong(), a));
			}

			if (this.patterns.isEmpty()) {
				TeamsDraw.text(g, TeamsText.t("banner.no_patterns"), this.columnX(), y + this.patternsY + 4, p.textMuted(), a);
			}

			CFHGuiDraw.focusRing(g, this.layerX(this.selected), y + this.layersY, lw, lh, this.focus() * a);
		}
	}

	protected void updateWidgetNarration(NarrationElementOutput out) {
		out.add(NarratedElementType.TITLE, this.getMessage());
	}
}
