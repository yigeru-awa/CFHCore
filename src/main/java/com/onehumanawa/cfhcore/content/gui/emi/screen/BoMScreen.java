package com.onehumanawa.cfhcore.content.gui.emi.screen;

import com.onehumanawa.cfhcore.content.gui.crafting.tree.PlannerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public class BoMScreen extends PlannerScreen {
	private boolean upstreamBackdrop;

	public BoMScreen(AbstractContainerScreen<?> old) {
		super(old);
	}

	@Override
	public void render(GuiGraphics raw, int mouseX, int mouseY, float delta) {
		this.upstreamBackdrop = true;
		this.renderMenuBackground(raw);
		this.upstreamBackdrop = false;
		super.render(raw, mouseX, mouseY, delta);
	}

	protected void renderMenuBackground(GuiGraphics raw) {
		if (!this.upstreamBackdrop) {
			super.renderMenuBackground(raw);
		}
	}
}
