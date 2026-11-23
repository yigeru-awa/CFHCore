package com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import com.onehumanawa.cfhcore.content.gui.teams.client.WaypointIcons;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;

public class GlyphChoice extends CFHGuiWidget {
	public static final int CELL = 14;
	public static final int GAP = 1;
	private final List<String> names = new ArrayList<>(WaypointIcons.names());
	private final List<Anim> hover = new ArrayList<>();
	private final Consumer<String> onPick;
	private final IntSupplier color;
	private int selected;

	public GlyphChoice(int x, int y, int width, String selected, IntSupplier color, Consumer<String> onPick) {
		super(x, y, width, 14, TeamsText.t("waypoint.icon"));
		this.onPick = onPick;
		this.color = color;
		this.selected = Math.max(0, this.names.indexOf(selected));

		for (int i = 0; i < this.names.size(); i++) {
			this.hover.add(new Anim(0.0F, 120, Ease.OUT_CUBIC));
		}

		this.setHeight(this.rows() * 15 - 1);
	}

	public static int heightFor(int width) {
		int per = Math.max(1, (width + 1) / 15);
		return (WaypointIcons.names().size() + per - 1) / per * 15 - 1;
	}

	public String selected() {
		return this.names.get(this.selected);
	}

	private int perRow() {
		return Math.max(1, (this.getWidth() + 1) / 15);
	}

	private int rows() {
		return (this.names.size() + this.perRow() - 1) / this.perRow();
	}

	private int cellX(int i) {
		return this.getX() + i % this.perRow() * 15;
	}

	private int cellY(int i) {
		return this.getY() + i / this.perRow() * 15;
	}

	private int at(double mx, double my) {
		for (int i = 0; i < this.names.size(); i++) {
			if (mx >= (double)this.cellX(i) && mx < (double)(this.cellX(i) + 14) && my >= (double)this.cellY(i) && my < (double)(this.cellY(i) + 14)) {
				return i;
			}
		}

		return -1;
	}

	private void pick(int i) {
		if (i >= 0 && i < this.names.size() && i != this.selected) {
			this.selected = i;
			CFHGuiSounds.tick();
			this.onPick.accept(this.names.get(i));
		}
	}

	public void onClick(double mouseX, double mouseY) {
		super.onClick(mouseX, mouseY);
		this.pick(this.at(mouseX, mouseY));
	}

	public void playDownSound(SoundManager handler) {
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.active && this.visible) {
			switch (keyCode) {
				case 262:
					this.pick(Math.min(this.names.size() - 1, this.selected + 1));
					break;
				case 263:
					this.pick(Math.max(0, this.selected - 1));
					break;
				case 264:
					this.pick(Math.min(this.names.size() - 1, this.selected + this.perRow()));
					break;
				case 265:
					this.pick(Math.max(0, this.selected - this.perRow()));
					break;
				default:
					return false;
			}

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
			int over = this.isHovered() && this.active ? this.at((double)mouseX, (double)mouseY) : -1;
			int ink = TeamsDraw.ink(this.color.getAsInt() & 16777215);
			int dy = this.enterOffset();

			for (int i = 0; i < this.names.size(); i++) {
				Anim h = this.hover.get(i);
				h.set(i == over);
				float hv = h.get();
				int x = this.cellX(i);
				int y = this.cellY(i) + dy;
				boolean sel = i == this.selected;
				if (van) {
					CFHGuiDraw.rect(g, x, y, 14, 14, Colors.scaleAlpha(sel ? -1073741824 : Colors.lerp(1610612736, -1879048192, hv), a));
					CFHGuiDraw.outline(g, x, y, 14, 14, Colors.scaleAlpha(sel ? -1 : Colors.lerp(-12566464, -6250336, hv), a), 0);
				} else {
					CFHGuiDraw.pixelRound(g, x, y, 14, 14, Colors.scaleAlpha(sel ? Colors.withAlpha(ink, 56) : Colors.lerp(p.bg2(), p.surfaceHover(), hv), a), 2);
					CFHGuiDraw.outline(g, x, y, 14, 14, Colors.scaleAlpha(sel ? ink : Colors.lerp(p.border(), p.borderStrong(), hv), a), 2);
				}

				Icon icon = WaypointIcons.get(this.names.get(i));
				Icons.draw(g, icon, x + 3, y + 3, 8, Colors.scaleAlpha(sel ? ink : Colors.lerp(p.textMuted(), p.text(), hv), a));
			}

			if (over >= 0) {
				CFHGuiTooltips.request(TeamsText.t("waypoint.icon." + this.names.get(over)), this);
			}

			if (this.isFocused() && CFHGuiDraw.focusShown()) {
				CFHGuiDraw.focusRing(g, this.cellX(this.selected), this.cellY(this.selected) + dy, 14, 14, this.focus() * a);
			}
		}
	}

	protected void updateWidgetNarration(NarrationElementOutput out) {
		out.add(NarratedElementType.TITLE, this.getMessage());
		out.add(NarratedElementType.HINT, TeamsText.t("waypoint.icon." + this.names.get(this.selected)));
	}
}
