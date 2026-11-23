package com.onehumanawa.cfhcore.content.gui.emi.screen.widget;

import com.onehumanawa.cfhcore.content.gui.emi.EmiPort;
import com.onehumanawa.cfhcore.content.gui.emi.EmiRenderHelper;
import com.onehumanawa.cfhcore.content.gui.emi.api.render.EmiTexture;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.api.widget.SlotWidget;
import com.onehumanawa.cfhcore.content.gui.emi.api.widget.Widget;
import com.onehumanawa.cfhcore.content.gui.emi.bom.BoM;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiDrawContext;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiHistory;
import com.onehumanawa.cfhcore.content.gui.emi.widget.RecipeDefaultButtonWidget;
import com.onehumanawa.cfhcore.content.gui.crafting.look.RecipeLook;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.MutableComponent;

public class ResolutionButtonWidget extends Button {
	public Supplier<Widget> hoveredWidget;
	public EmiIngredient stack;

	public ResolutionButtonWidget(int x, int y, int width, int height, EmiIngredient stack, Supplier<Widget> hoveredWidget) {
		super(x, y, width, height, EmiPort.literal(""), button -> {
			BoM.tree.addResolution(stack, null);
			EmiHistory.pop();
		}, s -> (MutableComponent)s.get());
		this.stack = stack;
		this.hoveredWidget = hoveredWidget;
	}

	public void renderWidget(GuiGraphics raw, int mouseX, int mouseY, float delta) {
		EmiDrawContext context = EmiDrawContext.wrap(raw);
		int u = 0;
		if (this.isHovered()) {
			u = 18;
		} else {
			Widget widget = this.hoveredWidget.get();
			if (widget instanceof SlotWidget slot && slot.getRecipe() != null || widget instanceof RecipeDefaultButtonWidget) {
				u = 36;
			}
		}

		if (RecipeLook.resolution(this, raw, u)) {
			this.stack.render(raw, this.x + 1, this.y + 1, delta);
		} else {
			EmiTexture.SLOT.render(context.raw(), this.x, this.y, delta);
			context.drawTexture(EmiRenderHelper.WIDGETS, this.x, this.y, u, 128, this.width, this.height);
			if (this.isHovered()) {
				Minecraft client = Minecraft.getInstance();
				raw.renderComponentTooltip(
					client.font,
					List.of(
						EmiPort.translatable("tooltip.emi.resolution"),
						EmiPort.translatable("tooltip.emi.select_resolution"),
						EmiPort.translatable("tooltip.emi.default_resolution"),
						EmiPort.translatable("tooltip.emi.clear_resolution")
					),
					mouseX,
					mouseY
				);
			}

			this.stack.render(raw, this.x + 1, this.y + 1, delta);
		}
	}
}
