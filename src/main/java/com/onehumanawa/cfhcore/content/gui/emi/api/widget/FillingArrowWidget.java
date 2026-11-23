package com.onehumanawa.cfhcore.content.gui.emi.api.widget;

import com.onehumanawa.cfhcore.content.gui.emi.EmiRenderHelper;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiDrawContext;
import net.minecraft.client.gui.GuiGraphics;

public class FillingArrowWidget extends AnimatedTextureWidget {
	public FillingArrowWidget(int x, int y, int time) {
		super(EmiRenderHelper.WIDGETS, x, y, 24, 17, 44, 17, time, true, false, false);
	}

	@Override
	public void render(GuiGraphics draw, int mouseX, int mouseY, float delta) {
		EmiDrawContext context = EmiDrawContext.wrap(draw);
		context.drawTexture(
			this.texture, this.x, this.y, this.width, this.height, (float)this.u, 0.0F, this.regionWidth, this.regionHeight, this.textureWidth, this.textureHeight
		);
		super.render(context.raw(), mouseX, mouseY, delta);
	}
}
