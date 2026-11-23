package com.onehumanawa.cfhcore.content.gui.teams.client.ui.dialog;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiCard;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class FormCard extends CFHGuiCard implements FormPart.Host {
	private static final int PAD = 8;
	private static final int TITLE_H = 16;
	private static final int FOOT_H = 20;
	private final FormPart form;
	private final CFHGuiButton submit;
	private final int bodyY;
	private final int bodyH;
	@Nullable
	private Component error;
	private long errorAtMs;
	private boolean waiting;
	@Nullable
	private Component locked;

	public FormCard(int width, FormPart form) {
		super(0, 0, width, 10);
		this.flat();
		this.form = form;
		this.bodyY = 24;
		this.bodyH = form.build(width - 16, (widget, relX, relY) -> this.add(widget, 8 + relX, this.bodyY + relY));
		this.submit = new CFHGuiButton(0, 0, 80, form.submitLabel(), this::send).variant(form.submitVariant());
		this.submit.setWidth(Math.max(72, this.submit.preferredWidth() + 14));
		this.add(this.submit, width - 8 - this.submit.getWidth(), this.bodyY + this.bodyH + 6);
		this.setHeight(this.bodyY + this.bodyH + 6 + 20 + 8);
	}

	public FormCard locked(@Nullable Component why) {
		this.locked = why;
		return this;
	}

	private void send() {
		if (!this.waiting && this.locked == null && this.form.ready()) {
			this.error = null;
			this.waiting = true;
			this.form.submit(this);
		}
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.form.keyPressed(keyCode, scanCode, modifiers)) {
			return true;
		} else if (super.keyPressed(keyCode, scanCode, modifiers)) {
			return true;
		} else if ((keyCode == 257 || keyCode == 335) && this.getFocused() != null && this.getFocused() != this.submit) {
			this.send();
			return true;
		} else {
			return false;
		}
	}

	protected void renderContent(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, float partialTick) {
		Theme t = Theme.current();
		Palette p = t.palette();
		float a = this.alpha * this.enterProgress();
		this.submit.active = this.locked == null && this.form.ready() && !this.waiting;
		int tx = x + 8;
		if (this.form.icon() != null) {
			Icons.draw(g, this.form.icon(), tx, y + 8 - 2, 12, Colors.scaleAlpha(p.accentInk(), a));
			tx += 16;
		}

		g.drawString(
			CFHGuiDraw.font(),
			CFHGuiDraw.truncate(Fonts.heading(this.form.title()), x + w - 8 - tx),
			tx,
			y + 8,
			Colors.scaleAlpha(t.isVanilla() ? -1 : p.text(), a),
			t.isVanilla()
		);
		this.form.render(g, x + 8, y + this.bodyY, w - 16, a, mouseX, mouseY);
		Component note = this.locked != null ? this.locked : this.error;
		if (note != null) {
			int color = this.locked != null ? p.textMuted() : p.danger();
			float since = this.locked != null ? 1.0F : (float)(Clock.nowMs() - this.errorAtMs) / 240.0F;
			int nudge = !(t.motion() <= 0.0F) && !(since >= 1.0F) ? Math.round((float)Math.sin((double)since * Math.PI * 3.0) * 3.0F * (1.0F - since)) : 0;
			int ny = y + this.bodyY + this.bodyH + 6 + 6;
			Icons.draw(g, this.locked != null ? Icon.LOCK : Icon.WARNING, x + 8 + nudge, ny, 8, Colors.scaleAlpha(color, a));
			g.drawString(
				CFHGuiDraw.font(), CFHGuiDraw.truncate(note, w - 16 - this.submit.getWidth() - 20), x + 8 + 11 + nudge, ny, Colors.scaleAlpha(color, a), t.isVanilla()
			);
		}
	}

	@Override
	public void done() {
		this.waiting = false;
	}

	@Override
	public void fail(Component reason) {
		this.waiting = false;
		this.error = reason;
		this.errorAtMs = Clock.nowMs();
	}

	@Override
	public void cancel() {
	}
}
