package com.onehumanawa.cfhcore.content.gui.emi.api.render;

import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.screen.tooltip.EmiTextTooltipWrapper;
import com.onehumanawa.cfhcore.content.gui.emi.screen.tooltip.RecipeTooltipComponent;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import org.jetbrains.annotations.ApiStatus.Experimental;

@Experimental
public class EmiTooltipMetadata {
	private static final EmiTooltipMetadata EMPTY = new EmiTooltipMetadata(EmiStack.EMPTY, null);
	private final EmiIngredient stack;
	private final EmiRecipe recipe;

	private EmiTooltipMetadata(EmiIngredient stack, EmiRecipe recipe) {
		this.stack = stack;
		this.recipe = recipe;
	}

	public EmiIngredient getStack() {
		return this.stack;
	}

	public EmiRecipe getRecipe() {
		return this.recipe;
	}

	public static EmiTooltipMetadata of(List<ClientTooltipComponent> tooltip) {
		if (!tooltip.isEmpty()) {
			ClientTooltipComponent title = tooltip.get(0);
			EmiRecipe recipe = null;

			for (ClientTooltipComponent comp : tooltip) {
				if (comp instanceof RecipeTooltipComponent rtc) {
					recipe = rtc.getRecipe();
					break;
				}
			}

			if (title instanceof EmiTextTooltipWrapper ettw && !ettw.stack.isEmpty()) {
				return new EmiTooltipMetadata(ettw.stack, recipe);
			}
		}

		return EMPTY;
	}
}
