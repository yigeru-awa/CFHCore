package com.onehumanawa.cfhcore.content.gui.server.client.ui.kit;

import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class ChipChoice<T> extends CFHGuiWidget {
	private static final int H = 16;
	private static final int GAP = 4;
	private final List<T> options;
	private final Function<T, Component> labeler;
	private final Consumer<T> onPick;
	private final List<int[]> boxes = new ArrayList<>();
	private int selected;

	public ChipChoice(int width, List<T> options, @Nullable T value, Function<T, Component> labeler, Consumer<T> onPick) {
		super(0, 0, width, 16, Component.empty());
		this.options = List.copyOf(options);
		this.labeler = labeler;
		this.onPick = onPick;
		this.selected = value == null ? -1 : this.options.indexOf(value);
		this.setWidth(width);
	}

	public void setWidth(int width) {
		super.setWidth(width);
		if (this.options != null) {
			this.boxes.clear();
			int x = 0;
			int y = 0;

			for (T option : this.options) {
				int w = CFHGuiDraw.width(this.labeler.apply(option)) + 12;
				if (x > 0 && x + w > width) {
					x = 0;
					y += 20;
				}

				this.boxes.add(new int[]{x, y, w});
				x += w + 4;
			}

			this.setHeight(y + 16);
		}
	}

	@Nullable
	public T value() {
		return this.selected < 0 ? null : this.options.get(this.selected);
	}

	public void clear() {
		this.selected = -1;
	}

	public void select(@Nullable T value) {
		this.selected = value == null ? -1 : this.options.indexOf(value);
	}

	private int at(double mx, double my) {
		for (int i = 0; i < this.boxes.size(); i++) {
			int[] b = this.boxes.get(i);
			if (mx >= (double)(this.getX() + b[0])
				&& mx < (double)(this.getX() + b[0] + b[2])
				&& my >= (double)(this.getY() + b[1])
				&& my < (double)(this.getY() + b[1] + 16)) {
				return i;
			}
		}

		return -1;
	}

	private void pick(int i) {
		if (i >= 0 && i < this.options.size() && i != this.selected) {
			this.selected = i;
			CFHGuiSounds.tick();
			this.onPick.accept(this.options.get(i));
		}
	}

	public void onClick(double mouseX, double mouseY) {
		super.onClick(mouseX, mouseY);
		this.pick(this.at(mouseX, mouseY));
	}

	public void playDownSound(SoundManager handler) {
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 262) {
			this.pick(Math.min(this.options.size() - 1, this.selected + 1));
			return true;
		} else if (keyCode == 263) {
			this.pick(Math.max(0, this.selected - 1));
			return true;
		} else {
			return false;
		}
	}

	private void draw(GuiGraphics g, int mouseX, int mouseY) {
		Theme t = Theme.current();
		Palette p = t.palette();
		float a = this.effectiveAlpha();
		if (!(a <= 0.004F)) {
			int hot = this.isHovered() && this.active ? this.at((double)mouseX, (double)mouseY) : -1;

			for (int i = 0; i < this.boxes.size(); i++) {
				int[] b = this.boxes.get(i);
				int x = this.getX() + b[0];
				int y = this.getY() + b[1];
				boolean sel = i == this.selected;
				boolean hov = i == hot;
				Component label = this.labeler.apply(this.options.get(i));
				if (t.isVanilla()) {
					CFHGuiDraw.vanillaButton(g, x, y, b[2], 16, hov ? 1.0F : 0.0F, this.active, a);
					if (sel) {
						CFHGuiDraw.outline(g, x, y, b[2], 16, Colors.scaleAlpha(-1, a), 0);
					}

					g.drawString(CFHGuiDraw.font(), label, x + 6, y + 4, Colors.scaleAlpha(sel ? -96 : (this.active ? -1 : -6250336), a), true);
				} else {
					int fill = sel ? Colors.withAlpha(p.accent(), 96) : (hov ? p.surfaceActive() : p.surfaceHover());
					CFHGuiDraw.pixelRound(g, x, y, b[2], 16, Colors.scaleAlpha(fill, a), 3);
					CFHGuiDraw.outline(g, x, y, b[2], 16, Colors.scaleAlpha(sel ? p.accent() : (hov ? p.borderStrong() : p.border()), a), 3);
					g.drawString(
						CFHGuiDraw.font(), label, x + 6, y + 4, Colors.scaleAlpha(sel ? p.text() : (hov ? p.text() : p.textMuted()), a * (this.active ? 1.0F : 0.5F)), false
					);
				}
			}

			if (this.isFocused() && this.selected >= 0) {
				int[] b = this.boxes.get(this.selected);
				CFHGuiDraw.focusRing(g, this.getX() + b[0], this.getY() + b[1], b[2], 16, this.focus() * a);
			}
		}
	}

	protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.draw(g, mouseX, mouseY);
	}

	protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.draw(g, mouseX, mouseY);
	}

	protected void updateWidgetNarration(NarrationElementOutput out) {
		if (this.selected >= 0) {
			out.add(NarratedElementType.TITLE, this.labeler.apply(this.options.get(this.selected)));
		}
	}
}
