package com.onehumanawa.cfhcore.content.gui.quests.client.editor;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

abstract class EditorDialog extends WidgetPopup {
	protected static final int PAD = 10;
	protected static final int TITLE_H = 17;
	protected static final int FOOT_H = 20;
	private final Component title;
	@Nullable
	private final Icon icon;
	private final int width;
	protected final EditorDialog.Body body = new EditorDialog.Body();
	@Nullable
	protected Form form;
	private final List<CFHGuiButton> buttons = new ArrayList<>();
	private boolean stale = true;
	private boolean entrance = true;
	private boolean settle;
	private double keep;
	@Nullable
	private String refocus;
	@Nullable
	private Component error;

	protected EditorDialog(Component title, @Nullable Icon icon, int width) {
		this.title = title;
		this.icon = icon;
		this.width = width;
		this.add(this.body);
	}

	protected abstract void fill(Form var1);

	protected CFHGuiButton button(Component label, Variant variant, Runnable action) {
		CFHGuiButton b = this.add(new CFHGuiButton(0, 0, 70, 20, label, action).variant(variant));
		this.buttons.add(b);
		return b;
	}

	protected void rebuild() {
		if (this.form != null && this.refocus == null) {
			this.refocus = this.form.nameOf(this.focusedField());
		}

		this.stale = true;
	}

	protected void fail(@Nullable Component reason) {
		this.error = reason;
	}

	@Nullable
	private AbstractWidget focusedField() {
		return this.focused == this.body && this.body.getFocused() instanceof AbstractWidget w ? w : null;
	}

	protected void focusField(@Nullable AbstractWidget field) {
		if (field != null) {
			this.focus(this.body);
			this.body.setFocused(field);
		}
	}

	private void build(int innerWidth) {
		this.stale = false;
		this.keep = this.body.scrollAmount();
		this.body.clear();
		Form f = new Form(innerWidth, (widget, fx, fy) -> this.body.add(widget, fx, fy), this::rebuild).live();
		this.form = f;
		this.fill(f);
		this.body.setContentHeight(f.height());
		this.settle = true;
	}

	private void settle() {
		this.settle = false;
		Form f = this.form;
		if (f != null) {
			if (this.entrance) {
				this.entrance = false;
				int i = 0;

				for (GuiEventListener child : this.body.children()) {
					if (child instanceof Renderable r) {
						i = CFHGuiScreen.entrance(r, i);
					}
				}

				if (f.first() != null) {
					this.focusField(f.first());
				}
			} else {
				this.body.snapScroll(this.keep);
				AbstractWidget again = f.named(this.refocus);
				if (again != null) {
					this.focusField(again);
				}
			}

			this.refocus = null;
		}
	}

	public boolean isModal() {
		return true;
	}

	@Override
	protected void layout() {
		Minecraft mc = Minecraft.getInstance();
		int sw = mc.getWindow().getGuiScaledWidth();
		int sh = mc.getWindow().getGuiScaledHeight();
		this.w = Math.min(this.width, sw - 12);
		int inner = this.w - 20;
		this.body.setWidth(inner);
		if (this.stale) {
			this.build(this.body.innerWidth());
		}

		int bodyH = Math.min(this.form == null ? 0 : this.form.height(), Math.max(40, sh - 24 - 20 - 17 - 20 - 16));
		this.h = 27 + bodyH + 14 + 20 + 10;
		this.x = (sw - this.w) / 2;
		this.y = Math.max(4, (sh - this.h) / 2);
		this.body.setX(this.x + 10);
		this.body.setY(this.y + 10 + 17);
		this.body.setHeight(bodyH);
		if (this.settle) {
			this.settle();
		}

		if (this.body.scrollAmount() > (double)this.body.maxScroll()) {
			this.body.snapScroll((double)this.body.maxScroll());
		}

		int right = this.x + this.w - 10;

		for (int i = this.buttons.size() - 1; i >= 0; i--) {
			CFHGuiButton b = this.buttons.get(i);
			b.setWidth(Math.max(60, b.preferredWidth() + 10));
			right -= b.getWidth();
			b.setX(right);
			b.setY(this.y + this.h - 10 - 20);
			right -= 6;
		}
	}

	@Override
	protected void drawUnder(GuiGraphics g, int mouseX, int mouseY, float a) {
		Theme t = Theme.current();
		Palette p = t.palette();
		boolean van = t.isVanilla();
		int tx = this.x + 10;
		if (this.icon != null) {
			Icons.draw(g, this.icon, tx, this.y + 10 - 2, 12, Colors.scaleAlpha(this.icon == Icon.WARNING ? p.warning() : (van ? -1 : p.accentInk()), a));
			tx += 16;
		}

		g.drawString(
			CFHGuiDraw.font(), CFHGuiDraw.truncate(Fonts.heading(this.title), this.x + this.w - 10 - tx), tx, this.y + 10, Colors.scaleAlpha(van ? -1 : p.text(), a), van
		);
		if (this.error != null) {
			int ey = this.y + this.h - 10 - 20 - 11;
			Icons.draw(g, Icon.WARNING, this.x + 10, ey, 8, Colors.scaleAlpha(p.danger(), a));
			g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(this.error, this.w - 20 - 12), this.x + 10 + 11, ey, Colors.scaleAlpha(p.danger(), a), van);
		}
	}

	@Override
	protected void step(int direction) {
		List<AbstractWidget> stops = new ArrayList<>();
		if (this.form != null) {
			for (AbstractWidget widget : this.form.stops()) {
				if (widget.visible && widget.active) {
					stops.add(widget);
				}
			}
		}

		int fields = stops.size();

		for (CFHGuiButton b : this.buttons) {
			if (b.visible && b.active) {
				stops.add(b);
			}
		}

		if (!stops.isEmpty()) {
			AbstractWidget at = this.focused == this.body ? this.focusedField() : this.focused;
			int i = at == null ? (direction > 0 ? -1 : 0) : stops.indexOf(at);
			i = Math.floorMod(i + direction, stops.size());
			if (i < fields) {
				this.focusField(stops.get(i));
			} else {
				this.body.setFocused((GuiEventListener)null);
				this.focus(stops.get(i));
			}
		}
	}

	static final class Body extends CFHGuiScrollPanel {
		Body() {
			super(0, 0, 10, 10);
			this.edgeFades(false);
		}

		public void setFocused(boolean focused) {
			if (!focused) {
				this.setFocused((GuiEventListener)null);
			}
		}
	}
}
