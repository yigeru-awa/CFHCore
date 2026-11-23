package com.onehumanawa.cfhcore.content.gui.emi.recipe;

import com.onehumanawa.cfhcore.content.gui.emi.EmiPort;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipeCategory;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.VanillaEmiRecipeCategories;
import com.onehumanawa.cfhcore.content.gui.emi.api.render.EmiTexture;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.api.widget.WidgetHolder;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.ProxyRecipeManager;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.StonecutterRecipe;

public class EmiStonecuttingRecipe implements EmiRecipe {
	private final ResourceLocation id;
	private final EmiIngredient input;
	private final EmiStack output;

	public EmiStonecuttingRecipe(StonecutterRecipe recipe) {
		this.id = ProxyRecipeManager.getId(recipe);
		this.input = EmiIngredient.of((Ingredient)recipe.getIngredients().get(0));
		this.output = EmiStack.of(EmiPort.getOutput(recipe));
	}

	@Override
	public EmiRecipeCategory getCategory() {
		return VanillaEmiRecipeCategories.STONECUTTING;
	}

	@Override
	public ResourceLocation getId() {
		return this.id;
	}

	@Override
	public List<EmiIngredient> getInputs() {
		return List.of(this.input);
	}

	@Override
	public List<EmiStack> getOutputs() {
		return List.of(this.output);
	}

	@Override
	public int getDisplayWidth() {
		return 76;
	}

	@Override
	public int getDisplayHeight() {
		return 18;
	}

	@Override
	public void addWidgets(WidgetHolder widgets) {
		widgets.addTexture(EmiTexture.EMPTY_ARROW, 26, 1);
		widgets.addSlot(this.input, 0, 0);
		widgets.addSlot(this.output, 58, 0).recipeContext(this);
	}
}
