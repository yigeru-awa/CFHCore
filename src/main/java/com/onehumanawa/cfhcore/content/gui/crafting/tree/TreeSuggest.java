package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import com.onehumanawa.cfhcore.content.gui.emi.api.EmiApi;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipeCategory;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipeManager;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiResolutionRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.VanillaEmiRecipeCategories;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.bom.BoM;
import com.onehumanawa.cfhcore.content.gui.crafting.CFHGuiCrafting;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class TreeSuggest {
	private static final Map<EmiStack, Optional<EmiRecipe>> FOUND = new HashMap<>();
	private static int foundAt = -1;

	@Nullable
	public static EmiRecipe of(EmiIngredient ingredient) {
		if (!TreeConfig.get().findRecipes) {
			return null;
		} else {
			List<EmiStack> stacks = ingredient.getEmiStacks();
			if (stacks.size() != 1) {
				return null;
			} else {
				EmiStack stack = stacks.get(0);
				if (stack.isEmpty()) {
					return null;
				} else {
					ResourceLocation id = stack.getId();
					if (id != null && !"minecraft".equals(id.getNamespace())) {
						if (foundAt != BoM.generation) {
							FOUND.clear();
							foundAt = BoM.generation;
						}

						Optional<EmiRecipe> known = FOUND.get(stack);
						if (known != null) {
							return known.orElse(null);
						} else {
							EmiRecipe found = null;

							try {
								found = find(stack);
							} catch (RuntimeException var7) {
								CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] looking for a recipe for {} failed: {}", id, var7.toString());
							}

							FOUND.put(stack, Optional.ofNullable(found));
							return found;
						}
					} else {
						return null;
					}
				}
			}
		}
	}

	@Nullable
	private static EmiRecipe find(EmiStack stack) {
		EmiRecipeManager recipes = EmiApi.getRecipeManager();
		EmiRecipe best = null;
		int bestScore = Integer.MIN_VALUE;

		for (EmiRecipe recipe : recipes.getRecipesByOutput(stack)) {
			if (!(recipe instanceof EmiResolutionRecipe) && recipe.supportsRecipeTree() && !BoM.disabledRecipes.contains(recipe)) {
				List<EmiStack> outputs = recipe.getOutputs();
				if (!outputs.isEmpty() && outputs.get(0).isEqual(stack) && !(outputs.get(0).getChance() < 0.5F)) {
					int score = score(recipe);
					if (score >= bestScore && score >= 0 && (score != bestScore || before(recipe, best)) && !uses(recipe, stack) && !turnsBack(recipes, recipe, stack)) {
						best = recipe;
						bestScore = score;
					}
				}
			}
		}

		return best;
	}

	private static boolean before(EmiRecipe recipe, @Nullable EmiRecipe than) {
		if (than == null) {
			return true;
		} else {
			String a = recipe.getId() == null ? null : recipe.getId().toString();
			String b = than.getId() == null ? null : than.getId().toString();
			return a == null ? false : b == null || a.compareTo(b) < 0;
		}
	}

	private static int score(EmiRecipe recipe) {
		EmiRecipeCategory category = recipe.getCategory();
		if (category == null) {
			return -1;
		} else {
			int score;
			if (category == VanillaEmiRecipeCategories.CRAFTING) {
				score = 100;
			} else if (category == VanillaEmiRecipeCategories.SMITHING) {
				score = 40;
			} else if (category == VanillaEmiRecipeCategories.STONECUTTING) {
				score = 30;
			} else {
				if (category == VanillaEmiRecipeCategories.SMELTING
					|| category == VanillaEmiRecipeCategories.BLASTING
					|| category == VanillaEmiRecipeCategories.SMOKING
					|| category == VanillaEmiRecipeCategories.CAMPFIRE_COOKING
					|| category == VanillaEmiRecipeCategories.ANVIL_REPAIRING
					|| category == VanillaEmiRecipeCategories.GRINDING
					|| category == VanillaEmiRecipeCategories.BREWING
					|| category == VanillaEmiRecipeCategories.WORLD_INTERACTION
					|| category == VanillaEmiRecipeCategories.FUEL
					|| category == VanillaEmiRecipeCategories.COMPOSTING
					|| category == VanillaEmiRecipeCategories.INFO) {
					return -1;
				}

				if ("emi".equals(category.getId().getNamespace())) {
					return -1;
				}

				score = 70;
			}

			if (recipe.getId() != null) {
				score += 5;
			}

			return score;
		}
	}

	private static boolean uses(EmiRecipe recipe, EmiStack stack) {
		for (EmiIngredient input : recipe.getInputs()) {
			for (EmiStack alternative : input.getEmiStacks()) {
				if (alternative.isEqual(stack)) {
					return true;
				}
			}
		}

		return false;
	}

	private static boolean turnsBack(EmiRecipeManager recipes, EmiRecipe recipe, EmiStack stack) {
		for (EmiIngredient input : recipe.getInputs()) {
			List<EmiStack> alternatives = input.getEmiStacks();

			for (int i = 0; i < alternatives.size() && i < 4; i++) {
				EmiStack alternative = alternatives.get(i);
				if (!alternative.isEmpty()) {
					List<EmiRecipe> makers = recipes.getRecipesByOutput(alternative);

					for (int k = 0; k < makers.size() && k < 48; k++) {
						EmiRecipe maker = makers.get(k);
						if (maker != recipe
							&& maker.supportsRecipeTree()
							&& !maker.getOutputs().isEmpty()
							&& maker.getOutputs().get(0).isEqual(alternative)
							&& uses(maker, stack)) {
							return true;
						}
					}
				}
			}
		}

		return false;
	}

	private TreeSuggest() {
	}
}
