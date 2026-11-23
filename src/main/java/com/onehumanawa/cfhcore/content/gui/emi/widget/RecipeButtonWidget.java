package com.onehumanawa.cfhcore.content.gui.emi.widget;

import com.onehumanawa.cfhcore.content.gui.emi.EmiRenderHelper;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.widget.Bounds;
import com.onehumanawa.cfhcore.content.gui.emi.api.widget.Widget;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiDrawContext;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.crafting.look.LookButtons;
import net.minecraft.client.gui.GuiGraphics;

public class RecipeButtonWidget extends Widget {
	protected final EmiRecipe recipe;
	protected final int x;
	protected final int y;
	protected final int u;
	protected final int v;

	public RecipeButtonWidget(int x, int y, int u, int v, EmiRecipe recipe) {
		this.x = x;
		this.y = y;
		this.u = u;
		this.v = v;
		this.recipe = recipe;
	}

	public int getTextureOffset(int mouseX, int mouseY) {
		return this.getBounds().contains(mouseX, mouseY) ? 12 : 0;
	}

	public void playButtonSound() {
		CFHGuiSounds.click();
	}

	@Override
	public Bounds getBounds() {
		return new Bounds(this.x, this.y, 12, 12);
	}

	@Override
	public void render(GuiGraphics raw, int mouseX, int mouseY, float delta) {
		EmiDrawContext context = EmiDrawContext.wrap(raw);
		context.resetColor();
		if (!LookButtons.recipe(this, raw, this.x, this.y, this.u, this.v, this.getTextureOffset(mouseX, mouseY), this.getBounds().contains(mouseX, mouseY))) {
			context.drawTexture(
				EmiRenderHelper.BUTTONS, this.x, this.y, 12, 12, (float)this.u, (float)(this.v + this.getTextureOffset(mouseX, mouseY)), 12, 12, 256, 256
			);
		}
	}
}
