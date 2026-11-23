package com.onehumanawa.cfhcore.content.gui.emi.bom;

import com.google.common.collect.Maps;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiPlayerInventory;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import java.util.Map;
import java.util.function.Function;

public class MaterialTree {
	public MaterialNode goal;
	public TreeCost cost = new TreeCost();
	public Map<EmiIngredient, EmiRecipe> resolutions = Maps.newHashMap();
	public long batches = 1L;
	public int revision = 0;
	public Runnable recalculated = null;
	public Function<EmiIngredient, EmiRecipe> fallback = null;

	public MaterialTree(EmiRecipe recipe) {
		EmiStack output = recipe.getOutputs().get(0);
		this.goal = new MaterialNode(output);
		this.goal.defineRecipe(recipe);
		this.recalculate();
	}

	public EmiRecipe getRecipe(EmiIngredient stack) {
		EmiRecipe recipe = this.resolutions.get(stack);
		if (recipe == null && !this.resolutions.containsKey(stack)) {
			recipe = BoM.getRecipe(stack);
			if (recipe == null && this.fallback != null) {
				recipe = this.fallback.apply(stack);
			}
		}

		return recipe;
	}

	public void addResolution(EmiIngredient ingredient, EmiRecipe recipe) {
		this.resolutions.put(ingredient, recipe);
		if (ingredient.equals(this.goal.ingredient)) {
			this.goal.defineRecipe(recipe);
			this.goal.amount = ingredient.getAmount();
		}

		this.recalculate();
	}

	public void recalculate() {
		this.goal.recalculate(this);
		this.revision++;
		if (this.recalculated != null) {
			this.recalculated.run();
		}
	}

	public void calculateProgress(EmiPlayerInventory inventory) {
		this.cost.calculateProgress(this.goal, this.batches, inventory);
	}

	public void calculateCost() {
		this.cost.calculate(this.goal, this.batches);
	}
}
