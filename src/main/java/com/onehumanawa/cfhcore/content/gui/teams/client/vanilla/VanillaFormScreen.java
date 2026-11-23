package com.onehumanawa.cfhcore.content.gui.teams.client.vanilla;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.dialog.FormPart;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsDraw;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class VanillaFormScreen extends Screen implements FormPart.Host {
	private static final int SIDE = 12;
	private static final int HEAD = 22;
	private static final int FOOT = 14;
	private static final int FOOT_BARE = 9;
	private static final int BELOW = 7;
	private static final int BUTTON_H = 20;
	private static final int BUTTON_GAP = 8;
	private static final int BUTTON_MAX = 220;
	private static final int BUTTON_PAIR = 150;
	private static final int BODY_MIN = 284;
	private static final int BODY_WIDE = 340;
	@Nullable
	private final Screen parent;
	private final FormPart form;
	private final List<VanillaFormScreen.Placed> placed = new ArrayList<>();
	private boolean made;
	private int bodyW;
	private int bodyH;
	private int bodyX;
	private int bodyY;
	private int plateX;
	private int plateY;
	private int plateW;
	private int plateH;
	@Nullable
	private Button submit;
	@Nullable
	private Component error;
	private long errorAtMs;
	private boolean waiting;

	public VanillaFormScreen(@Nullable Screen parent, FormPart form) {
		super(form.title());
		this.parent = parent;
		this.form = form;
	}

	protected void init() {
		if (!this.made) {
			this.made = true;
			int air = Math.max(8, this.height / 12);
			int roomW = Math.max(120, this.width - 24 - 16);
			int roomH = Math.max(60, this.height - 22 - this.foot() - 7 - 20 - air * 2);
			int column = Mth.clamp(this.width / 2, 284, 340);
			this.bodyW = Math.min(roomW, Math.max(column, this.form.widthFor(roomW, roomH)));
			this.bodyH = this.form.build(this.bodyW, (widget, relX, relY) -> this.placed.add(new VanillaFormScreen.Placed(widget, relX, relY)));
		}

		this.plateW = this.bodyW + 24;
		this.plateH = 22 + this.bodyH + this.foot();
		this.plateX = (this.width - this.plateW) / 2;
		this.plateY = Math.max(4, (this.height - (this.plateH + 7 + 20)) / 2);
		this.bodyX = this.plateX + 12;
		this.bodyY = this.plateY + 22;

		for (VanillaFormScreen.Placed p : this.placed) {
			p.widget().setX(this.bodyX + p.relX());
			p.widget().setY(this.bodyY + p.relY());
			this.addRenderableWidget(p.widget());
		}

		int span = (this.plateW - 8) / 2;
		int buttonW = span <= 220 ? Math.max(100, span) : 150;
		int y = Math.min(this.plateY + this.plateH + 7, this.height - 20 - 2);
		this.submit = (Button)this.addRenderableWidget(
			Button.builder(this.form.submitLabel(), b -> this.send()).bounds(this.width / 2 - 4 - buttonW, y, buttonW, 20).build()
		);
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> this.cancel()).bounds(this.width / 2 + 4, y, buttonW, 20).build());
		AbstractWidget first = this.form.firstFocus();
		if (first != null) {
			this.setInitialFocus(first);
		}
	}

	private int foot() {
		return this.form.mayBeRefused() ? 14 : 9;
	}

	private void send() {
		if (!this.waiting && this.form.ready()) {
			this.error = null;
			this.waiting = true;
			this.form.submit(this);
		}
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode != 256 && this.form.keyPressed(keyCode, scanCode, modifiers)) {
			return true;
		} else if (super.keyPressed(keyCode, scanCode, modifiers)) {
			return true;
		} else if ((keyCode == 257 || keyCode == 335) && this.form.firstFocus() != null) {
			this.send();
			return true;
		} else {
			return false;
		}
	}

	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(g, mouseX, mouseY, partialTick);
		Ground.plate(g, this.plateX, this.plateY, this.plateW, this.plateH);
	}

	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		if (this.submit != null) {
			this.submit.active = this.form.ready() && !this.waiting;
		}

		super.render(g, mouseX, mouseY, partialTick);
		Theme t = Theme.current();
		FormattedCharSequence heading = CFHGuiDraw.truncate(this.title, this.bodyW);
		TeamsDraw.text(g, heading, this.width / 2 - this.font.width(heading) / 2, this.plateY + 8, t.palette().text(), 1.0F);
		this.form.render(g, this.bodyX, this.bodyY, this.bodyW, 1.0F, mouseX, mouseY);
		if (this.error != null) {
			float since = (float)(Clock.nowMs() - this.errorAtMs) / 240.0F;
			int nudge = !(t.motion() <= 0.0F) && !(since >= 1.0F) ? Math.round((float)Math.sin((double)since * Math.PI * 3.0) * 3.0F * (1.0F - since)) : 0;
			FormattedCharSequence line = CFHGuiDraw.truncate(this.error, this.bodyW);
			TeamsDraw.text(g, line, this.width / 2 - this.font.width(line) / 2 + nudge, this.plateY + this.plateH - 11, t.palette().danger(), 1.0F);
		}
	}

	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}

	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void done() {
		this.waiting = false;
		if (this.minecraft != null && this.minecraft.screen == this) {
			this.minecraft.setScreen(this.parent);
		}
	}

	@Override
	public void fail(Component reason) {
		this.waiting = false;
		this.error = reason;
		this.errorAtMs = Clock.nowMs();
	}

	@Override
	public void cancel() {
		this.onClose();
	}

	private static record Placed(AbstractWidget widget, int relX, int relY) {
	}
}
