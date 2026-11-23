package com.onehumanawa.cfhcore.content.gui.emi.api;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.onehumanawa.cfhcore.content.gui.emi.VanillaPlugin;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipeCategory;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipeManager;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStackInteraction;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.ListEmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.TagEmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.bom.BoM;
import com.onehumanawa.cfhcore.content.gui.emi.config.EmiConfig;
import com.onehumanawa.cfhcore.content.gui.emi.recipe.EmiSyntheticIngredientRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.recipe.EmiTagRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.registry.EmiRecipes;
import com.onehumanawa.cfhcore.content.gui.emi.registry.EmiStackList;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiFavorite;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiHistory;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiSidebars;
import com.onehumanawa.cfhcore.content.gui.emi.screen.BoMScreen;
import com.onehumanawa.cfhcore.content.gui.emi.screen.EmiScreenManager;
import com.onehumanawa.cfhcore.content.gui.emi.screen.RecipeScreen;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.jetbrains.annotations.Nullable;

public class EmiApi {
	private static final Minecraft client = Minecraft.getInstance();

	public static List<EmiStack> getIndexStacks() {
		return EmiStackList.stacks;
	}

	public static EmiRecipeManager getRecipeManager() {
		return EmiRecipes.manager;
	}

	public static boolean isCheatMode() {
		return switch (EmiConfig.cheatMode) {
			case TRUE -> true;
			case CREATIVE -> client.player == null || client.player.hasInfiniteMaterials();
			case FALSE -> false;
		};
	}

	public static String getSearchText() {
		return EmiScreenManager.search.getValue();
	}

	public static void setSearchText(String text) {
		EmiScreenManager.search.setValue(text);
	}

	public static boolean isSearchFocused() {
		return EmiScreenManager.search.isFocused();
	}

	public static EmiStackInteraction getHoveredStack(int mouseX, int mouseY, boolean includeStandard) {
		return EmiScreenManager.getHoveredStack(mouseX, mouseY, includeStandard);
	}

	public static EmiStackInteraction getHoveredStack(boolean includeStandard) {
		return EmiScreenManager.getHoveredStack(EmiScreenManager.lastMouseX, EmiScreenManager.lastMouseY, includeStandard);
	}

	@Nullable
	public static EmiRecipe getRecipeContext(EmiIngredient stack) {
		return stack instanceof EmiFavorite fav ? fav.getRecipe() : null;
	}

	public static AbstractContainerScreen<?> getHandledScreen() {
		Screen s = client.screen;
		if (s instanceof AbstractContainerScreen) {
			return (AbstractContainerScreen<?>)s;
		} else if (s instanceof RecipeScreen rs) {
			return rs.old;
		} else {
			return s instanceof BoMScreen bs ? bs.old : null;
		}
	}

	public static void displayAllRecipes() {
		EmiRecipeManager manager = getRecipeManager();
		setPages(manager.getCategories().stream().collect(Collectors.toMap(c -> (EmiRecipeCategory)c, c -> manager.getRecipes(c))), EmiStack.EMPTY);
	}

	public static void displayRecipeCategory(EmiRecipeCategory category) {
		setPages(Map.of(category, getRecipeManager().getRecipes(category)), EmiStack.EMPTY);
	}

	public static void displayRecipesForWorkstation(EmiIngredient workstation) {
		EmiRecipeManager manager = getRecipeManager();
		setPages(
			manager.getCategories()
				.stream()
				.filter(c -> manager.getWorkstations(c).contains(workstation))
				.collect(Collectors.toMap(c -> (EmiRecipeCategory)c, manager::getRecipes)),
			workstation
		);
	}

	public static void displayRecipe(EmiRecipe recipe) {
		setPages(Map.of(recipe.getCategory(), List.of(recipe)), EmiStack.EMPTY);
	}

	public static void displayRecipes(EmiIngredient stack) {
		if (stack instanceof EmiFavorite fav) {
			stack = fav.getStack();
		}

		if (stack instanceof TagEmiIngredient tag) {
			for (EmiRecipe recipe : getRecipeManager().getRecipes(VanillaPlugin.TAG)) {
				if (recipe instanceof EmiTagRecipe tr && tr.key.equals(tag.key)) {
					setPages(Map.of(VanillaPlugin.TAG, List.of(recipe)), stack);
					break;
				}
			}
		} else if (stack instanceof ListEmiIngredient list) {
			setPages(Map.of(VanillaPlugin.INGREDIENT, List.of(new EmiSyntheticIngredientRecipe(stack))), stack);
		} else if (stack.getEmiStacks().size() == 1) {
			EmiStack es = stack.getEmiStacks().get(0);
			setPages(mapRecipes(pruneSources(getRecipeManager().getRecipesByOutput(es), es)), stack);
			focusRecipe(BoM.getRecipe(es));
		}
	}

	public static void displayUses(EmiIngredient stack) {
		if (!stack.isEmpty()) {
			EmiStack zero = stack.getEmiStacks().get(0);
			Map<EmiRecipeCategory, List<EmiRecipe>> map = mapRecipes(
				Stream.concat(pruneUses(getRecipeManager().getRecipesByInput(zero), stack).stream(), EmiRecipes.byWorkstation.getOrDefault(zero, List.of()).stream())
					.distinct()
					.toList()
			);
			setPages(map, stack);
		}
	}

	public static void viewRecipeTree() {
		if (client.screen == null) {
			client.setScreen(new InventoryScreen(client.player));
		}

		Screen s = client.screen;
		if (s instanceof AbstractContainerScreen<?> hs) {
			push();
			client.setScreen(new BoMScreen(hs));
		} else if (s instanceof RecipeScreen rs) {
			push();
			client.setScreen(new BoMScreen(rs.old));
		}
	}

	public static void focusRecipe(EmiRecipe recipe) {
		if (client.screen instanceof RecipeScreen rs) {
			rs.focusRecipe(recipe);
		}
	}

	private static void push() {
		if (client.screen instanceof RecipeScreen rs) {
			EmiHistory.push(rs);
		} else if (client.screen instanceof BoMScreen bs) {
			EmiHistory.push(bs);
		} else {
			EmiHistory.clear();
			EmiHistory.push(client.screen);
		}
	}

	private static List<EmiRecipe> pruneSources(List<EmiRecipe> list, EmiStack context) {
		return list.stream().filter(r -> r.getOutputs().stream().anyMatch(i -> i.isEqual(context))).toList();
	}

	private static List<EmiRecipe> pruneUses(List<EmiRecipe> list, EmiIngredient context) {
		return list.stream()
			.filter(r -> r.getInputs().stream().anyMatch(i -> containsAll(i, context)) || r.getCatalysts().stream().anyMatch(i -> containsAll(i, context)))
			.sorted((a, b) -> getSmallestPresence(a, context) - getSmallestPresence(b, context))
			.toList();
	}

	private static int getSmallestPresence(EmiRecipe recipe, EmiIngredient context) {
		int ideal = context.getEmiStacks().size();
		int smallestPresence = Integer.MAX_VALUE;

		for (EmiIngredient i : recipe.getInputs()) {
			if (containsAll(i, context)) {
				smallestPresence = Math.min(smallestPresence, i.getEmiStacks().size());
				if (smallestPresence <= ideal) {
					break;
				}
			}
		}

		return smallestPresence;
	}

	private static Map<EmiRecipeCategory, List<EmiRecipe>> mapRecipes(List<EmiRecipe> list) {
		Map<EmiRecipeCategory, List<EmiRecipe>> map = Maps.newHashMap();

		for (EmiRecipe recipe : list) {
			map.computeIfAbsent(recipe.getCategory(), k -> Lists.newArrayList()).add(recipe);
		}

		return map;
	}

	private static boolean containsAll(EmiIngredient collection, EmiIngredient ingredient) {
		label21:
		for (EmiStack ing : ingredient.getEmiStacks()) {
			for (EmiStack col : collection.getEmiStacks()) {
				if (col.isEqual(ing)) {
					continue label21;
				}
			}

			return false;
		}

		return true;
	}

	private static void setPages(Map<EmiRecipeCategory, List<EmiRecipe>> recipes, EmiIngredient stack) {
		recipes = recipes.entrySet().stream().filter(e -> !e.getValue().isEmpty()).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
		if (!recipes.isEmpty()) {
			EmiSidebars.lookup(stack);
			if (getHandledScreen() == null) {
				client.setScreen(new InventoryScreen(client.player));
			}

			if (client.screen instanceof AbstractContainerScreen<?> hs) {
				push();
				client.setScreen(new RecipeScreen(hs, recipes));
			} else if (client.screen instanceof BoMScreen bs) {
				push();
				client.setScreen(new RecipeScreen(bs.old, recipes));
			} else if (client.screen instanceof RecipeScreen rs) {
				push();
				RecipeScreen n = new RecipeScreen(rs.old, recipes);
				client.setScreen(n);
				n.focusCategory(rs.getFocusedCategory());
			}
		}
	}
}
