package com.onehumanawa.cfhcore.content.gui.emi.recipe;

import com.onehumanawa.cfhcore.content.gui.emi.EmiPort;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiCraftingRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.ProxyRecipeManager;
import net.minecraft.world.item.crafting.ShapelessRecipe;

public class EmiShapelessRecipe extends EmiCraftingRecipe {
	public EmiShapelessRecipe(ShapelessRecipe recipe) {
		super(recipe.getIngredients().stream().map(i -> EmiIngredient.of(i)).toList(), EmiStack.of(EmiPort.getOutput(recipe)), ProxyRecipeManager.getId(recipe));
		EmiShapedRecipe.setRemainders(this.input, recipe);
	}

	@Override
	public boolean canFit(int width, int height) {
		return this.input.size() <= width * height;
	}
}
