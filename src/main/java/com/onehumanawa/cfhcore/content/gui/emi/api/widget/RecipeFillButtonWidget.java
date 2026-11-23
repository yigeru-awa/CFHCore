package com.onehumanawa.cfhcore.content.gui.emi.api.widget;

import com.google.common.collect.Lists;
import com.onehumanawa.cfhcore.content.gui.emi.EmiPort;
import com.onehumanawa.cfhcore.content.gui.emi.api.EmiApi;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiPlayerInventory;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.handler.EmiCraftContext;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.handler.EmiRecipeHandler;
import com.onehumanawa.cfhcore.content.gui.emi.input.EmiInput;
import com.onehumanawa.cfhcore.content.gui.emi.registry.EmiRecipeFiller;
import com.onehumanawa.cfhcore.content.gui.emi.widget.RecipeButtonWidget;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RecipeFillButtonWidget extends RecipeButtonWidget {
	private boolean canFill;
	private List<ClientTooltipComponent> tooltip;

	@Internal
	public RecipeFillButtonWidget(int x, int y, EmiRecipe recipe) {
		super(x, y, 24, 0, recipe);
		AbstractContainerScreen hs = EmiApi.getHandledScreen();
		EmiRecipeHandler handler = EmiRecipeFiller.getFirstValidHandler(recipe, hs);
		this.tooltip = List.of(ClientTooltipComponent.create(EmiPort.ordered(EmiPort.translatable("emi.inapplicable"))));
		if (handler == null) {
			this.canFill = false;
		} else {
			try {
				EmiPlayerInventory inv = handler.getInventory(hs);
				boolean applicable = handler.supportsRecipe(recipe);
				EmiCraftContext context = new EmiCraftContext(hs, inv, EmiCraftContext.Type.FILL_BUTTON);
				this.canFill = applicable && handler.canCraft(recipe, context);
				if (applicable) {
					this.tooltip = handler.getTooltip(recipe, context);
				}
			} catch (Exception var9) {
				this.canFill = false;
				this.tooltip = List.of(ClientTooltipComponent.create(EmiPort.ordered(EmiPort.translatable("emi.error.recipe.initialize"))));
			}
		}
	}

	@Override
	public int getTextureOffset(int mouseX, int mouseY) {
		return !this.canFill ? 24 : super.getTextureOffset(mouseX, mouseY);
	}

	@Override
	public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
		List<ClientTooltipComponent> list = Lists.newArrayList();
		if (this.canFill) {
			list.add(ClientTooltipComponent.create(EmiPort.ordered(EmiPort.translatable("tooltip.emi.fill_recipe"))));
		}

		list.addAll(this.tooltip);
		return list;
	}

	@Override
	public boolean mouseClicked(int mouseX, int mouseY, int button) {
		if (this.canFill) {
			AbstractContainerScreen<?> hs = EmiApi.getHandledScreen();
			if (hs != null
				&& EmiRecipeFiller.performFill(
					this.recipe, hs, EmiCraftContext.Type.FILL_BUTTON, EmiCraftContext.Destination.NONE, EmiInput.isShiftDown() ? Integer.MAX_VALUE : 1
				)) {
				this.playButtonSound();
				return true;
			}
		}

		return false;
	}
}
