package com.onehumanawa.cfhcore.content.gui.emi.recipe;

import com.onehumanawa.cfhcore.content.gui.emi.EmiPort;
import com.onehumanawa.cfhcore.content.gui.emi.EmiUtil;
import com.onehumanawa.cfhcore.content.gui.emi.VanillaPlugin;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiIngredientRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipeCategory;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiResolutionRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.TagEmiIngredient;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

public class EmiTagRecipe extends EmiIngredientRecipe {
	private final List<EmiStack> stacks;
	private final EmiIngredient ingredient;
	public final TagKey<?> key;

	public EmiTagRecipe(TagKey<?> key) {
		this.key = key;
		this.ingredient = new TagEmiIngredient(key, 1L);
		this.stacks = this.ingredient.getEmiStacks();
	}

	@Override
	protected EmiIngredient getIngredient() {
		return this.ingredient;
	}

	@Override
	protected List<EmiStack> getStacks() {
		return this.stacks;
	}

	@Override
	protected EmiRecipe getRecipeContext(EmiStack stack, int offset) {
		return new EmiResolutionRecipe(this.ingredient, stack);
	}

	@Override
	public EmiRecipeCategory getCategory() {
		return VanillaPlugin.TAG;
	}

	@Override
	public ResourceLocation getId() {
		return EmiPort.id("emi", "/tag/" + this.key.registry().location().getPath() + "/" + EmiUtil.subId(this.key.location()));
	}
}
