package com.onehumanawa.cfhcore.content.gui.server.client.ui.kit;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;

public final class MarkChoice extends CFHGuiWidget {
	private static final List<String> MARKS = List.of(
		"HOME",
		"PIN",
		"STAR_FILLED",
		"HEART_FILLED",
		"MAP",
		"WORLD",
		"CROWN",
		"SHIELD",
		"SWORD",
		"BLOCK",
		"TROPHY",
		"SUN",
		"MOON",
		"minecraft:red_bed",
		"minecraft:chest",
		"minecraft:crafting_table",
		"minecraft:furnace",
		"minecraft:iron_pickaxe",
		"minecraft:wheat",
		"minecraft:oak_sapling",
		"minecraft:diamond",
		"minecraft:emerald",
		"minecraft:ender_eye",
		"minecraft:blaze_rod",
		"minecraft:fishing_rod",
		"minecraft:enchanting_table"
	);
	private static final int CELL = 20;
	private final List<String> marks = new ArrayList<>();
	private final Icon fallback;
	private final IntSupplier color;
	private final Consumer<String> onPick;
	private String selected;
	private int cols;

	public MarkChoice(int width, String selected, Icon fallback, IntSupplier color, Consumer<String> onPick) {
		super(0, 0, width, 20, Txt.t("form.icon"));
		this.selected = selected == null ? "" : selected;
		this.fallback = fallback;
		this.color = color;
		this.onPick = onPick;
		if (!this.selected.isEmpty() && !MARKS.contains(this.selected)) {
			this.marks.add(this.selected);
		}

		this.marks.addAll(MARKS);
		this.setWidth(width);
	}

	public void setWidth(int width) {
		super.setWidth(width);
		this.cols = Math.max(1, width / 20);
		this.setHeight((this.marks == null ? 1 : (this.marks.size() + this.cols - 1) / this.cols) * 20);
	}

	public String value() {
		return this.selected;
	}

	private int indexAt(double mx, double my) {
		int cx = (int)((mx - (double)this.getX()) / 20.0);
		int cy = (int)((my - (double)this.getY()) / 20.0);
		if (!(mx < (double)this.getX()) && !(my < (double)this.getY()) && cx < this.cols) {
			int i = cy * this.cols + cx;
			return i >= 0 && i < this.marks.size() ? i : -1;
		} else {
			return -1;
		}
	}

	private void pick(int index) {
		if (index >= 0 && index < this.marks.size()) {
			this.selected = this.marks.get(index);
			CFHGuiSounds.tick();
			this.onPick.accept(this.selected);
		}
	}

	public void onClick(double mouseX, double mouseY) {
		super.onClick(mouseX, mouseY);
		this.pick(this.indexAt(mouseX, mouseY));
	}

	public void playDownSound(SoundManager handler) {
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		int at = Math.max(0, this.marks.indexOf(this.selected));

		int to = switch (keyCode) {
			case 262 -> at + 1;
			case 263 -> at - 1;
			case 264 -> at + this.cols;
			case 265 -> at - this.cols;
			default -> -1;
		};
		if (to >= 0 && to < this.marks.size()) {
			this.pick(to);
			return true;
		} else {
			return false;
		}
	}

	private void draw(GuiGraphics g, int mouseX, int mouseY) {
		Theme t = Theme.current();
		Palette p = t.palette();
		float a = this.effectiveAlpha();
		int hover = this.isHovered() ? this.indexAt((double)mouseX, (double)mouseY) : -1;

		for (int i = 0; i < this.marks.size(); i++) {
			int cx = this.getX() + i % this.cols * 20;
			int cy = this.getY() + i / this.cols * 20;
			boolean sel = this.marks.get(i).equals(this.selected);
			int rgb = sel ? this.color.getAsInt() & 16777215 : (t.isVanilla() ? 9145227 : p.borderStrong() & 16777215);
			Ui.markTile(
				g,
				this.marks.get(i),
				this.fallback,
				cx + 1,
				cy + 1,
				18,
				!sel && i != hover ? rgb : this.color.getAsInt() & 16777215,
				a * (!sel && i != hover ? 0.75F : 1.0F)
			);
			if (sel) {
				CFHGuiDraw.outline(g, cx, cy, 20, 20, Colors.scaleAlpha(t.isVanilla() ? -1 : p.accent(), a), t.isVanilla() ? 0 : 3);
			}
		}

		if (this.isFocused()) {
			CFHGuiDraw.focusRing(g, this.getX(), this.getY(), this.cols * 20, this.getHeight(), this.focus() * a);
		}
	}

	protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.draw(g, mouseX, mouseY);
	}

	protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.draw(g, mouseX, mouseY);
	}

	protected void updateWidgetNarration(NarrationElementOutput out) {
		out.add(NarratedElementType.TITLE, Component.literal(this.getMessage().getString() + ": " + this.selected));
	}
}
