package com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import com.onehumanawa.cfhcore.content.gui.teams.model.BannerData;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;

public class BannerChoice extends CFHGuiWidget {
	public static final int FLAG_W = 12;
	public static final int FLAG_H = 24;
	public static final int GAP = 8;
	public static final int PAD = 2;
	private final List<BannerData> options = new ArrayList<>();
	private final List<Anim> lift = new ArrayList<>();
	private final IntConsumer onPick;
	private int selected;

	public BannerChoice(int x, int y, List<BannerData> options, int selected, IntConsumer onPick) {
		super(x, y, widthFor(options.size()), 28, TeamsText.t("create.banner"));
		this.onPick = onPick;
		this.selected = selected;
		this.options(options);
	}

	public static int widthFor(int count) {
		return Math.max(0, count * 20 - 8 + 4);
	}

	public void options(List<BannerData> list) {
		this.options.clear();
		this.options.addAll(list);

		while (this.lift.size() < this.options.size()) {
			this.lift.add(new Anim(0.0F, 130, Ease.OUT_CUBIC));
		}

		this.setWidth(widthFor(this.options.size()));
		if (this.selected >= this.options.size()) {
			this.selected = 0;
		}
	}

	public int selected() {
		return this.selected;
	}

	private int cellX(int i) {
		return this.getX() + 2 + i * 20;
	}

	private int at(double mx, double my) {
		if (!(my < (double)this.getY()) && !(my >= (double)(this.getY() + this.getHeight()))) {
			for (int i = 0; i < this.options.size(); i++) {
				int cx = this.cellX(i);
				if (mx >= (double)(cx - 4) && mx < (double)(cx + 12 + 4)) {
					return i;
				}
			}

			return -1;
		} else {
			return -1;
		}
	}

	private void pick(int i) {
		if (i >= 0 && i < this.options.size() && i != this.selected) {
			this.selected = i;
			CFHGuiSounds.tick();
			this.onPick.accept(i);
		}
	}

	public void onClick(double mouseX, double mouseY) {
		super.onClick(mouseX, mouseY);
		this.pick(this.at(mouseX, mouseY));
	}

	public void playDownSound(SoundManager handler) {
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (!this.active || !this.visible) {
			return false;
		} else if (keyCode == 263) {
			this.pick(Math.max(0, this.selected - 1));
			return true;
		} else if (keyCode == 262) {
			this.pick(Math.min(this.options.size() - 1, this.selected + 1));
			return true;
		} else {
			return false;
		}
	}

	protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.draw(g, mouseX, mouseY, false);
	}

	protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.draw(g, mouseX, mouseY, true);
	}

	private void draw(GuiGraphics g, int mouseX, int mouseY, boolean van) {
		float a = this.effectiveAlpha();
		if (!(a <= 0.004F)) {
			Palette p = Theme.current().palette();
			int hover = this.isHovered() && this.active ? this.at((double)mouseX, (double)mouseY) : -1;
			int y = this.getY() + 2 + this.enterOffset();

			for (int i = 0; i < this.options.size(); i++) {
				Anim up = this.lift.get(i);
				up.set(i == hover && i != this.selected);
				int cx = this.cellX(i);
				int cy = y - Math.round(up.get());
				if (i == this.selected) {
					int ring = van ? -1 : p.accent();
					CFHGuiDraw.outline(g, cx - 2, cy - 2, 16, 28, Colors.scaleAlpha(ring, a), van ? 0 : 1);
				}

				TeamsDraw.bannerSmall(g, this.options.get(i), cx, cy, 12, a * (this.active ? 1.0F : 0.5F));
			}

			if (this.isFocused() && CFHGuiDraw.focusShown()) {
				CFHGuiDraw.focusRing(g, this.cellX(this.selected) - 2, y - 2, 16, 28, this.focus() * a);
			}
		}
	}

	protected void updateWidgetNarration(NarrationElementOutput out) {
		out.add(NarratedElementType.TITLE, this.getMessage());
		out.add(NarratedElementType.POSITION, Component.translatable("narrator.position.list", new Object[]{this.selected + 1, this.options.size()}));
	}
}
