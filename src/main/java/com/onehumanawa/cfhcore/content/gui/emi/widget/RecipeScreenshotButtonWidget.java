package com.onehumanawa.cfhcore.content.gui.emi.widget;

import com.onehumanawa.cfhcore.content.gui.emi.EmiPort;
import com.onehumanawa.cfhcore.content.gui.emi.EmiRenderHelper;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiDrawContext;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiScreenshotRecorder;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;

public class RecipeScreenshotButtonWidget extends RecipeButtonWidget {
	public RecipeScreenshotButtonWidget(int x, int y, EmiRecipe recipe) {
		super(x, y, 60, 0, recipe);
	}

	@Override
	public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
		return List.of(ClientTooltipComponent.create(EmiPort.ordered(EmiPort.translatable("tooltip.emi.recipe_screenshot"))));
	}

	@Override
	public boolean mouseClicked(int mouseX, int mouseY, int button) {
		this.playButtonSound();
		ResourceLocation id = this.recipe.getId();
		String path;
		if (id == null) {
			path = "unknown-recipe";
		} else {
			path = id.getNamespace() + "/" + id.getPath().replace("/", "_");
		}

		int width = this.recipe.getDisplayWidth() + 8;
		int height = this.recipe.getDisplayHeight() + 8;
		Minecraft client = Minecraft.getInstance();
		GuiGraphics context = new GuiGraphics(client, client.renderBuffers().bufferSource());
		EmiScreenshotRecorder.saveScreenshot(
			"emi/recipes/" + path, width, height, () -> EmiRenderHelper.renderRecipe(this.recipe, EmiDrawContext.wrap(context), 0, 0, false, -1)
		);
		return true;
	}
}
