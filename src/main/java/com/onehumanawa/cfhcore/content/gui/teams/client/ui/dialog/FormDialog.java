package com.onehumanawa.cfhcore.content.gui.teams.client.ui.dialog;

import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public class FormDialog extends TeamDialog implements FormPart.Host {
	private final FormPart form;
	private final CFHGuiButton submit;
	private final int bodyHeight;
	private boolean waiting;

	public FormDialog(FormPart form) {
		super(form.title(), form.icon(), width(form));
		this.form = form;
		this.bodyHeight = form.build(width(form) - 24, this::add);
		this.submit = this.buttons(form.submitLabel(), form.submitVariant(), this::send);
	}

	private static int width(FormPart form) {
		int screen = Minecraft.getInstance().getWindow().getGuiScaledWidth();
		return Math.min(form.preferredWidth() + 24, screen - 12);
	}

	public static FormDialog open(FormPart form) {
		FormDialog d = new FormDialog(form);
		d.show();
		return d;
	}

	@Override
	public void show() {
		super.show();
		AbstractWidget first = this.form.firstFocus();
		if (first != null) {
			this.focus(first);
		}
	}

	private void send() {
		if (!this.waiting && this.form.ready()) {
			this.clearError();
			this.waiting = true;
			this.form.submit(this);
		}
	}

	@Override
	protected void submit() {
		this.send();
	}

	@Override
	protected int bodyHeight() {
		return this.bodyHeight;
	}

	@Override
	protected void update() {
		this.submit.active = this.form.ready() && !this.waiting;
	}

	@Override
	protected void renderBody(GuiGraphics g, int x, int y, int w, float alpha, int mouseX, int mouseY) {
		this.form.render(g, x, y, w, alpha, mouseX, mouseY);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		return keyCode != 256 && this.form.keyPressed(keyCode, scanCode, modifiers) ? true : super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void done() {
		this.waiting = false;
		this.close();
	}

	@Override
	public void fail(Component reason) {
		this.waiting = false;
		super.fail(reason);
	}

	@Override
	public void cancel() {
		this.close();
	}
}
