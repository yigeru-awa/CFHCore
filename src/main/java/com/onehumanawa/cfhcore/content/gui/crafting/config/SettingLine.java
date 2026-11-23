package com.onehumanawa.cfhcore.content.gui.crafting.config;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

abstract class SettingLine extends AbstractWidget {
	static final int HEIGHT = 24;
	static final int LABEL_X = 22;
	static final int RESET_ROOM = 20;
	static final int CELL_H = 16;
	static final int CELL_GAP = 3;
	@Nullable
	private final Component tooltip;
	private final Anim glow = new Anim(0.0F, 150, Ease.OUT_CUBIC);
	private final List<SettingLine.Cell> cells = new ArrayList<>();
	private long labelSince;

	SettingLine(int width, Component label, @Nullable Component tooltip) {
		super(0, 0, width, 24, label);
		this.tooltip = tooltip;
	}

	protected abstract int cellCount();

	protected abstract int cellWidth(int var1);

	protected abstract void drawCell(GuiGraphics var1, SettingLine.Cell var2, int var3, boolean var4, int var5, int var6);

	protected abstract void pressCell(int var1, int var2);

	protected abstract boolean isDefault();

	protected abstract void reset();

	private int resetX() {
		return this.getX() + this.getWidth() - 2 - 16;
	}

	private int cellY() {
		return this.getY() + 4;
	}

	private List<SettingLine.Cell> layout() {
		this.cells.clear();
		int n = this.cellCount();
		int total = 0;
		int[] widths = new int[n];

		for (int i = 0; i < n; i++) {
			widths[i] = this.cellWidth(i);
			total += widths[i] + (i > 0 ? 3 : 0);
		}

		int x = this.getX() + this.getWidth() - 20 - 2 - total;

		for (int i = 0; i < n; i++) {
			this.cells.add(new SettingLine.Cell(i, x, widths[i]));
			x += widths[i] + 3;
		}

		return this.cells;
	}

	private int labelRight() {
		List<SettingLine.Cell> c = this.layout();
		return (c.isEmpty() ? this.resetX() : c.get(0).x()) - 6;
	}

	private boolean inside(double mx, double my) {
		return mx >= (double)this.getX()
			&& mx < (double)(this.getX() + this.getWidth())
			&& my >= (double)this.getY()
			&& my < (double)(this.getY() + this.getHeight());
	}

	private boolean overReset(double mx, double my) {
		return !this.isDefault()
			&& mx >= (double)this.resetX()
			&& mx < (double)(this.resetX() + 16)
			&& my >= (double)(this.getY() + 4)
			&& my < (double)(this.getY() + 20);
	}

	protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		Theme t = Theme.current();
		Palette p = t.palette();
		boolean vanilla = t.isVanilla();
		int x = this.getX();
		int y = this.getY();
		int w = this.getWidth();
		boolean hov = this.inside((double)mouseX, (double)mouseY) && g.containsPointInScissor(mouseX, mouseY);
		this.glow.set(hov);
		float hv = this.glow.get();
		if (hv > 0.01F) {
			if (vanilla) {
				g.fill(x, y, x + w, y + 24, Colors.scaleAlpha(419430399, hv));
			} else {
				CFHGuiDraw.pixelRound(g, x, y, w, 24, Colors.scaleAlpha(Colors.withAlpha(p.surfaceHover(), 112), hv), t.radius());
			}
		}

		int lx = x + 22;
		int room = Math.max(10, this.labelRight() - lx);
		g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(this.getMessage(), room), lx, CFHGuiDraw.textY(y, 24), vanilla ? -1 : p.text(), vanilla);
		int cy = this.cellY();

		for (SettingLine.Cell cell : this.layout()) {
			boolean over = hov && mouseX >= cell.x() && mouseX < cell.x() + cell.width() && mouseY >= cy && mouseY < cy + 16;
			this.drawCell(g, cell, cy, over, mouseX, mouseY);
		}

		if (!this.isDefault()) {
			boolean over = hov && this.overReset((double)mouseX, (double)mouseY);
			if (over && !vanilla) {
				CFHGuiDraw.pixelRound(g, this.resetX(), y + 4, 16, 16, p.surfaceHover(), 2);
			}

			Icons.draw(g, Icon.UNDO, this.resetX() + 4, y + 8, 8, vanilla ? (over ? -1 : -4144960) : (over ? p.text() : p.textMuted()));
		}

		boolean overLabel = hov && mouseX >= lx && mouseX < lx + room;
		if (!overLabel) {
			this.labelSince = 0L;
		} else if (this.tooltip != null) {
			if (this.labelSince == 0L) {
				this.labelSince = Clock.nowMs();
			}

			if (Clock.nowMs() - this.labelSince > 350L) {
				List<Component> lines = new ArrayList<>();

				for (String line : this.tooltip.getString().split("\n")) {
					lines.add(Component.literal(line));
				}

				CFHGuiTooltips.request(lines, this);
			}
		}
	}

	protected static int face(GuiGraphics g, int x, int y, int w, boolean hovered, boolean lit, boolean quiet) {
		Theme t = Theme.current();
		Palette p = t.palette();
		if (t.isVanilla()) {
			CFHGuiDraw.vanillaButton(g, x, y, w, 16, hovered ? 1.0F : 0.0F, !quiet, 1.0F);
			return lit ? -96 : (quiet ? -6250336 : -1);
		} else {
			int fill = lit
				? Colors.mix(p.surface(), p.accent(), hovered ? 0.32F : 0.22F)
				: (quiet ? Colors.withAlpha(p.surface(), hovered ? 255 : 144) : (hovered ? p.surfaceHover() : p.surface()));
			int border = lit ? Colors.withAlpha(p.accent(), 192) : (hovered ? p.borderStrong() : p.border());
			CFHGuiDraw.pixelRound(g, x, y, w, 16, fill, 2);
			CFHGuiDraw.outline(g, x, y, w, 16, border, 2);
			return lit ? Colors.brighten(p.accentInk(), 0.3F) : (quiet ? (hovered ? p.textMuted() : p.textDim()) : (hovered ? -1 : p.text()));
		}
	}

	protected boolean capturePress(int button) {
		return false;
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.visible && this.active) {
			if (this.capturePress(button)) {
				return true;
			} else if (!this.inside(mouseX, mouseY)) {
				return false;
			} else if (button == 0 && this.overReset(mouseX, mouseY)) {
				this.reset();
				CFHGuiSounds.tick();
				return true;
			} else {
				int cy = this.cellY();

				for (SettingLine.Cell cell : this.layout()) {
					if (mouseX >= (double)cell.x() && mouseX < (double)(cell.x() + cell.width()) && mouseY >= (double)cy && mouseY < (double)(cy + 16)) {
						CFHGuiSounds.click(Minecraft.getInstance().getSoundManager());
						this.pressCell(cell.index(), button);
						return true;
					}
				}

				return false;
			}
		} else {
			return false;
		}
	}

	public void playDownSound(SoundManager handler) {
	}

	protected void updateWidgetNarration(NarrationElementOutput out) {
		out.add(NarratedElementType.TITLE, this.getMessage());
	}

	protected static record Cell(int index, int x, int width) {
	}
}
