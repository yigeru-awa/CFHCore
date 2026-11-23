package com.onehumanawa.cfhcore.content.gui.quests.client.editor;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiColorField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popup;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.CanvasDraw;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.Nullable;

abstract class WidgetPopup implements Popup {
	protected final List<AbstractWidget> widgets = new ArrayList<>();
	@Nullable
	protected AbstractWidget focused;
	protected int x;
	protected int y;
	protected int w;
	protected int h;
	private final Anim open = new Anim(0.0F, 180, Ease.OUT_CUBIC);
	private boolean closing;

	protected WidgetPopup() {
		this.open.snap(0.0F);
		this.open.set(1.0F);
	}

	protected abstract void layout();

	protected void drawUnder(GuiGraphics g, int mouseX, int mouseY, float alpha) {
	}

	protected void drawOver(GuiGraphics g, int mouseX, int mouseY, float alpha) {
	}

	protected boolean clicked(double mouseX, double mouseY, int button) {
		return false;
	}

	protected boolean scrolled(double mouseX, double mouseY, double amount) {
		return false;
	}

	protected boolean key(int keyCode, int scanCode, int modifiers) {
		return false;
	}

	protected void submit() {
	}

	protected <T extends AbstractWidget> T add(T widget) {
		this.widgets.add(widget);
		return widget;
	}

	protected void focus(@Nullable AbstractWidget widget) {
		if (this.focused != widget) {
			if (this.focused != null) {
				unfocus(this.focused);
			}

			this.focused = widget;
			if (widget != null) {
				widget.setFocused(true);
			}
		}
	}

	private static void unfocus(AbstractWidget widget) {
		if (widget instanceof ContainerEventHandler c) {
			c.setFocused((GuiEventListener)null);
		}

		widget.setFocused(false);
	}

	protected boolean typing() {
		return typing(this.focused);
	}

	static boolean typing(@Nullable GuiEventListener focus) {
		GuiEventListener f = deep(focus);
		if (f instanceof EditBox e && e.isFocused() || f instanceof TextArea a && a.isFocused() || f instanceof CFHGuiColorField color && color.hexField().isFocused()
			)
		 {
			return true;
		}

		return false;
	}

	@Nullable
	static GuiEventListener deep(@Nullable GuiEventListener focus) {
		GuiEventListener f = focus;

		for (int guard = 0; guard < 6 && f instanceof ContainerEventHandler; guard++) {
			f = ((ContainerEventHandler)f).getFocused();
		}

		return f;
	}

	public void show() {
		Popups.open(this);
	}

	public void close() {
		Popups.close(this);
	}

	public boolean isShowing() {
		return Popups.isOpen(this);
	}

	protected boolean vanilla() {
		return Theme.current().isVanilla();
	}

	protected int rise() {
		return Math.round((1.0F - Math.min(1.0F, this.open.get())) * 8.0F);
	}

	public boolean contains(double mouseX, double mouseY) {
		return this.closing
			? false
			: this.isModal()
				|| mouseX >= (double)this.x
					&& mouseX < (double)(this.x + this.w)
					&& mouseY >= (double)(this.y + this.rise())
					&& mouseY < (double)(this.y + this.h + this.rise());
	}

	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.layout();
		Theme t = Theme.current();
		Palette p = t.palette();
		float a = Math.min(1.0F, Math.max(0.0F, this.open.get()) * 1.5F);
		if (!(a <= 0.01F)) {
			int dy = this.rise();
			int mx = this.closing ? -1 : mouseX;
			int my = this.closing ? -1 : mouseY - dy;
			g.pose().pushPose();
			g.pose().translate(0.0F, (float)dy, 0.0F);
			if (t.isVanilla()) {
				CFHGuiDraw.rect(g, this.x, this.y, this.w, this.h, Colors.withAlpha(0, Math.round(255.0F * a)));
				CFHGuiDraw.vanillaDialog(g, this.x, this.y, this.w, this.h, a);
			} else {
				CFHGuiDraw.popupPlate(g, this.x, this.y, this.w, this.h, p.surface(), p.borderStrong(), 0.7F, a);
			}

			Theme.enterPanel();

			try {
				this.drawUnder(g, mx, my, a);

				for (AbstractWidget widget : this.widgets) {
					if (widget.visible) {
						widget.setAlpha(a);
						widget.render(g, mx, my, partialTick);
					}
				}

				this.drawOver(g, mx, my, a);
			} finally {
				Theme.exitPanel();
			}

			g.pose().popPose();
			CanvasDraw.layer(g);
		}
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		this.layout();
		double my = mouseY - (double)this.rise();

		for (int i = this.widgets.size() - 1; i >= 0; i--) {
			AbstractWidget widget = this.widgets.get(i);
			if (widget.visible && widget.mouseClicked(mouseX, my, button)) {
				if (this.isShowing()) {
					this.focus(widget);
				}

				return true;
			}
		}

		if (!this.clicked(mouseX, my, button)) {
			this.focus(null);
		}

		return true;
	}

	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		double my = mouseY - (double)this.rise();

		for (AbstractWidget widget : new ArrayList<>(this.widgets)) {
			widget.mouseReleased(mouseX, my, button);
		}

		return true;
	}

	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		return this.focused != null && this.focused.mouseDragged(mouseX, mouseY - (double)this.rise(), button, dragX, dragY);
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		double my = mouseY - (double)this.rise();

		for (int i = this.widgets.size() - 1; i >= 0; i--) {
			AbstractWidget widget = this.widgets.get(i);
			if (widget.visible && widget.isMouseOver(mouseX, my) && widget.mouseScrolled(mouseX, my, scrollX, scrollY)) {
				return true;
			}
		}

		this.scrolled(mouseX, my, scrollY);
		return true;
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 256) {
			return false;
		} else if (keyCode == 258) {
			this.step((modifiers & 1) != 0 ? -1 : 1);
			return true;
		} else {
			GuiEventListener at = deep(this.focused);
			if (keyCode != 257 && keyCode != 335 || at != null && !(at instanceof EditBox)) {
				if (this.focused != null && this.focused.keyPressed(keyCode, scanCode, modifiers)) {
					return true;
				} else if (this.key(keyCode, scanCode, modifiers)) {
					return true;
				} else {
					if (keyCode == 257 || keyCode == 335) {
						this.submit();
					}

					return true;
				}
			} else {
				if (at instanceof EditField f) {
					f.commit();
				}

				this.submit();
				return true;
			}
		}
	}

	protected void step(int direction) {
		List<AbstractWidget> stops = new ArrayList<>();

		for (AbstractWidget widget : this.widgets) {
			if (widget.visible && widget.active && this.stoppable(widget)) {
				stops.add(widget);
			}
		}

		if (!stops.isEmpty()) {
			int i = this.focused == null ? (direction > 0 ? -1 : 0) : stops.indexOf(this.focused);
			i = Math.floorMod(i + direction, stops.size());
			this.focus(stops.get(i));
		}
	}

	protected boolean stoppable(AbstractWidget widget) {
		return !(widget instanceof CFHGuiLabel) && !(widget instanceof Bits.RichBox);
	}

	public boolean charTyped(char c, int modifiers) {
		if (this.focused != null) {
			this.focused.charTyped(c, modifiers);
		}

		return true;
	}

	public boolean beginClose() {
		if (Theme.current().motion() <= 0.0F) {
			return false;
		} else {
			this.closing = true;
			this.open.set(0.0F, 110);
			return true;
		}
	}

	public boolean closeFinished() {
		return this.open.get() <= 0.02F;
	}
}
