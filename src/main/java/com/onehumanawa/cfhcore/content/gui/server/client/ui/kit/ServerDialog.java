package com.onehumanawa.cfhcore.content.gui.server.client.ui.kit;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popup;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerActions;
import com.onehumanawa.cfhcore.content.gui.server.net.ServerNet;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

public abstract class ServerDialog implements Popup {
	protected static final int PAD = 12;
	protected static final int TITLE_H = 18;
	protected static final int BUTTON_H = 20;
	protected static final int ERROR_H = 12;
	protected static final int ROW_GAP = 7;
	private final Component title;
	@Nullable
	private final Icon icon;
	private final int width;
	private final List<ServerDialog.Placed> placed = new ArrayList<>();
	private final List<ServerDialog.Caption> captions = new ArrayList<>();
	private final List<AbstractWidget> widgets = new ArrayList<>();
	private final List<CFHGuiButton> buttons = new ArrayList<>();
	private final Anim open = new Anim(0.0F, 200, Ease.OUT_BACK);
	private boolean closing;
	@Nullable
	private AbstractWidget focused;
	@Nullable
	private AbstractWidget firstFocus;
	@Nullable
	private Component error;
	private long errorAtMs;
	protected boolean waiting;
	protected int cursor;
	protected int x;
	protected int y;
	protected int w;
	protected int h;
	private final List<int[]> wrapped = new ArrayList<>();
	private final List<FormattedCharSequence> wrappedLines = new ArrayList<>();

	protected ServerDialog(Component title, @Nullable Icon icon, int preferredWidth) {
		this.title = title;
		this.icon = icon;
		this.width = Math.min(preferredWidth, Minecraft.getInstance().getWindow().getGuiScaledWidth() - 12);
		this.open.snap(0.0F);
		this.open.set(1.0F);
	}

	protected int bodyWidth() {
		return this.width - 24;
	}

	protected int bodyHeight() {
		return Math.max(0, this.cursor - 7);
	}

	protected void renderBody(GuiGraphics g, int x, int y, int w, float alpha, int mouseX, int mouseY) {
	}

	protected void update() {
	}

	protected void submit() {
		for (CFHGuiButton b : this.buttons) {
			if (b.active && (b.variant() == Variant.PRIMARY || b.variant() == Variant.DANGER)) {
				b.activate();
				return;
			}
		}
	}

	protected <T extends AbstractWidget> T add(T widget, int relX, int relY) {
		this.placed.add(new ServerDialog.Placed(widget, relX, relY));
		this.widgets.add(widget);
		return widget;
	}

	protected void caption(Component text, int relX, int relY) {
		this.captions.add(new ServerDialog.Caption(text, relX, relY, false));
	}

	protected void hint(Component text, int relX, int relY) {
		this.captions.add(new ServerDialog.Caption(text, relX, relY, true));
	}

	protected <T extends AbstractWidget> T row(Component caption, T widget) {
		this.caption(caption, 0, this.cursor);
		widget.setWidth(this.bodyWidth());
		this.add(widget, 0, this.cursor + 11);
		this.cursor = this.cursor + 11 + widget.getHeight() + 7;
		return widget;
	}

	protected <T extends AbstractWidget> T line(T widget) {
		this.add(widget, 0, this.cursor);
		this.cursor = this.cursor + (widget.getHeight() + 7 - 2);
		return widget;
	}

	protected void words(Component text) {
		List<FormattedCharSequence> lines = CFHGuiDraw.font().split(text, this.bodyWidth());

		for (int i = 0; i < lines.size(); i++) {
			this.wrapped.add(new int[]{this.cursor + i * 10});
		}

		this.wrappedLines.addAll(lines);
		this.cursor = this.cursor + lines.size() * 10 + 7;
	}

	protected void firstFocus(AbstractWidget widget) {
		this.firstFocus = widget;
	}

	protected CFHGuiButton button(Component label, Variant variant, Runnable action) {
		CFHGuiButton b = new CFHGuiButton(0, 0, 80, label, action).variant(variant);
		this.buttons.add(b);
		this.widgets.add(b);
		return b;
	}

	protected CFHGuiButton buttons(Component okLabel, Variant variant, Runnable action) {
		this.button(Component.translatable("gui.cancel"), Variant.SECONDARY, this::close);
		return this.button(okLabel, variant, action);
	}

	protected void fail(Component reason) {
		this.error = reason;
		this.errorAtMs = Clock.nowMs();
	}

	protected void clearError() {
		this.error = null;
	}

	protected void send(Consumer<Consumer<ServerNet.Result>> action) {
		if (!this.waiting) {
			this.clearError();
			this.waiting = true;
			action.accept(this::answer);
		}
	}

	protected void answer(ServerNet.Result result) {
		this.waiting = false;
		if (result.ok()) {
			this.close();
			this.onDone();
		} else {
			this.fail(ServerActions.describe(result));
		}
	}

	protected void onDone() {
	}

	protected void focus(@Nullable AbstractWidget widget) {
		if (this.focused != null && this.focused != widget) {
			this.focused.setFocused(false);
		}

		this.focused = widget;
		if (widget != null) {
			widget.setFocused(true);
		}
	}

	public void show() {
		Popups.open(this);
		if (this.firstFocus != null) {
			this.focus(this.firstFocus);
		}
	}

	public void close() {
		Popups.close(this);
	}

	public boolean isShowing() {
		return Popups.isOpen(this);
	}

	private void layout() {
		Minecraft mc = Minecraft.getInstance();
		int sw = mc.getWindow().getGuiScaledWidth();
		int sh = mc.getWindow().getGuiScaledHeight();
		this.w = Math.min(this.width, sw - 12);
		this.h = 30 + this.bodyHeight() + 12 + (this.buttons.isEmpty() ? 0 : 20) + 12 - 2;
		this.x = (sw - this.w) / 2;
		this.y = Math.max(4, (sh - this.h) / 2);
		int bx = this.x + 12;
		int by = this.y + 12 + 18;

		for (ServerDialog.Placed p : this.placed) {
			p.widget().setX(bx + p.relX());
			p.widget().setY(by + p.relY());
		}

		int right = this.x + this.w - 12;

		for (int i = this.buttons.size() - 1; i >= 0; i--) {
			CFHGuiButton b = this.buttons.get(i);
			b.setWidth(Math.max(64, b.preferredWidth() + 12));
			right -= b.getWidth();
			b.setX(right);
			b.setY(this.y + this.h - 12 - 20 + 2);
			right -= 6;
		}
	}

	private int rise() {
		return Math.round((1.0F - Math.min(1.0F, this.open.get())) * 10.0F);
	}

	public boolean isModal() {
		return true;
	}

	public boolean contains(double mouseX, double mouseY) {
		return true;
	}

	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.update();
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
				CFHGuiDraw.pixelRound(g, this.x, this.y, this.w, this.h, Colors.withAlpha(p.bg(), Math.round(255.0F * a)), t.radius());
				CFHGuiDraw.popupPlate(g, this.x, this.y, this.w, this.h, p.surface(), p.borderStrong(), 0.7F, a);
			}

			int tx = this.x + 12;
			if (this.icon != null) {
				Icons.draw(g, this.icon, tx, this.y + 12 - 2, 12, Colors.scaleAlpha(this.icon == Icon.WARNING ? p.warning() : p.accentInk(), a));
				tx += 16;
			}

			g.drawString(
				CFHGuiDraw.font(),
				CFHGuiDraw.truncate(Fonts.heading(this.title), this.x + this.w - 12 - tx),
				tx,
				this.y + 12,
				Colors.scaleAlpha(Ui.strong(), a),
				t.isVanilla()
			);
			int bx = this.x + 12;
			int by = this.y + 12 + 18;
			Theme.enterPanel();

			try {
				for (ServerDialog.Caption c : this.captions) {
					Ui.textFit(g, c.text(), bx + c.relX(), by + c.relY(), this.bodyWidth() - c.relX(), c.dim() ? Ui.caption() : p.textMuted(), a);
				}

				for (int i = 0; i < this.wrappedLines.size(); i++) {
					Ui.text(g, this.wrappedLines.get(i), bx, by + this.wrapped.get(i)[0], p.text(), a);
				}

				this.renderBody(g, bx, by, this.bodyWidth(), a, mx, my);

				for (AbstractWidget wd : this.widgets) {
					if (wd.visible) {
						wd.setAlpha(a);
						wd.render(g, mx, my, partialTick);
					}
				}
			} finally {
				Theme.exitPanel();
			}

			if (this.error != null) {
				float since = (float)(Clock.nowMs() - this.errorAtMs) / 240.0F;
				int nudge = !(t.motion() <= 0.0F) && !(since >= 1.0F) ? Math.round((float)Math.sin((double)since * Math.PI * 3.0) * 3.0F * (1.0F - since)) : 0;
				int ey = this.y + this.h - 12 - 20 - 12 + 3;
				Icons.draw(g, Icon.WARNING, bx + nudge, ey, 8, Colors.scaleAlpha(p.danger(), a));
				g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(this.error, this.bodyWidth() - 12), bx + 11 + nudge, ey, Colors.scaleAlpha(p.danger(), a), t.isVanilla());
			}

			g.pose().popPose();
		}
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		this.layout();
		double my = mouseY - (double)this.rise();

		for (int i = this.widgets.size() - 1; i >= 0; i--) {
			AbstractWidget wd = this.widgets.get(i);
			if (wd.visible && wd.mouseClicked(mouseX, my, button)) {
				if (this.isShowing()) {
					this.focus(wd);
				}

				return true;
			}
		}

		this.focus(null);
		return true;
	}

	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		double my = mouseY - (double)this.rise();

		for (AbstractWidget wd : this.widgets) {
			wd.mouseReleased(mouseX, my, button);
		}

		return true;
	}

	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		return this.focused != null && this.focused.mouseDragged(mouseX, mouseY - (double)this.rise(), button, dragX, dragY);
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		double my = mouseY - (double)this.rise();

		for (int i = this.widgets.size() - 1; i >= 0; i--) {
			AbstractWidget wd = this.widgets.get(i);
			if (wd.visible && wd.isMouseOver(mouseX, my) && wd.mouseScrolled(mouseX, my, scrollX, scrollY)) {
				return true;
			}
		}

		return true;
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 256) {
			return false;
		} else if (this.focused != null && this.focused.keyPressed(keyCode, scanCode, modifiers)) {
			return true;
		} else if (keyCode != 257 && keyCode != 335) {
			if (keyCode == 258) {
				List<AbstractWidget> stops = new ArrayList<>();

				for (AbstractWidget wd : this.widgets) {
					if (wd.visible && wd.active) {
						stops.add(wd);
					}
				}

				if (stops.isEmpty()) {
					return true;
				} else {
					boolean back = (modifiers & 1) != 0;
					int i = this.focused == null ? (back ? 0 : -1) : stops.indexOf(this.focused);
					i = ((i + (back ? -1 : 1)) % stops.size() + stops.size()) % stops.size();
					this.focus(stops.get(i));
					return true;
				}
			} else {
				return true;
			}
		} else {
			this.submit();
			return true;
		}
	}

	public boolean charTyped(char c, int modifiers) {
		return this.focused != null && this.focused.charTyped(c, modifiers);
	}

	public boolean beginClose() {
		if (Theme.current().motion() <= 0.0F) {
			return false;
		} else {
			this.closing = true;
			this.open.ease(Ease.OUT_CUBIC);
			this.open.set(0.0F, 120);
			return true;
		}
	}

	public boolean closeFinished() {
		return this.open.get() <= 0.02F;
	}

	private static record Caption(Component text, int relX, int relY, boolean dim) {
	}

	private static record Placed(AbstractWidget widget, int relX, int relY) {
	}
}
