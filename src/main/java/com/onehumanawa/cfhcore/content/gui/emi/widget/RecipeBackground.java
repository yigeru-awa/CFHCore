package com.onehumanawa.cfhcore.content.gui.emi.widget;

import com.onehumanawa.cfhcore.content.gui.emi.EmiPort;
import com.onehumanawa.cfhcore.content.gui.emi.EmiRenderHelper;
import com.onehumanawa.cfhcore.content.gui.emi.api.widget.Bounds;
import com.onehumanawa.cfhcore.content.gui.emi.api.widget.Widget;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiDrawContext;
import com.onehumanawa.cfhcore.content.gui.crafting.look.RecipeLook;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class RecipeBackground extends Widget {
	private static final ResourceLocation TEXTURE = EmiPort.id("emi", "textures/gui/background.png");
	private final int x;
	private final int y;
	private final int width;
	private final int height;

	public RecipeBackground(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	@Override
	public Bounds getBounds() {
		return Bounds.EMPTY;
	}

	@Override
	public void render(GuiGraphics raw, int mouseX, int mouseY, float delta) {
		EmiDrawContext context = EmiDrawContext.wrap(raw);
		if (!RecipeLook.card(raw, this.x, this.y, this.width, this.height)) {
			EmiRenderHelper.drawNinePatch(context, TEXTURE, this.x, this.y, this.width, this.height, 27, 0, 4, 1);
		}
	}
}
