package com.onehumanawa.cfhcore.content.gui.emi.api.recipe;

import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.api.widget.WidgetHolder;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.ProxyRecipeManager;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

public interface EmiRecipe {
	EmiRecipeCategory getCategory();

	@Nullable
	ResourceLocation getId();

	List<EmiIngredient> getInputs();

	default List<EmiIngredient> getCatalysts() {
		return List.of();
	}

	List<EmiStack> getOutputs();

	int getDisplayWidth();

	int getDisplayHeight();

	void addWidgets(WidgetHolder var1);

	default boolean supportsRecipeTree() {
		return !this.getInputs().isEmpty() && !this.getOutputs().isEmpty();
	}

	default boolean hideCraftable() {
		return false;
	}

	@Nullable
	default RecipeHolder<?> getBackingRecipe() {
		return ProxyRecipeManager.getRecipeEntry(this.getId());
	}
}
