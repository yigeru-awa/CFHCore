package com.onehumanawa.cfhcore.content.gui.emi.api.recipe;

import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public interface EmiRecipeManager {
	List<EmiRecipeCategory> getCategories();

	List<EmiIngredient> getWorkstations(EmiRecipeCategory var1);

	List<EmiRecipe> getRecipes();

	List<EmiRecipe> getRecipes(EmiRecipeCategory var1);

	@Nullable
	EmiRecipe getRecipe(ResourceLocation var1);

	List<EmiRecipe> getRecipesByInput(EmiStack var1);

	List<EmiRecipe> getRecipesByOutput(EmiStack var1);
}
