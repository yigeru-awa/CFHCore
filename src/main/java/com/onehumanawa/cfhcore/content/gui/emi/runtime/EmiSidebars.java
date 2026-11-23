package com.onehumanawa.cfhcore.content.gui.emi.runtime;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.content.gui.emi.api.EmiApi;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.serializer.EmiIngredientSerializer;
import com.onehumanawa.cfhcore.content.gui.emi.chess.EmiChess;
import com.onehumanawa.cfhcore.content.gui.emi.config.EmiConfig;
import com.onehumanawa.cfhcore.content.gui.emi.config.SidebarType;
import com.onehumanawa.cfhcore.content.gui.emi.registry.EmiStackList;
import com.onehumanawa.cfhcore.content.gui.emi.screen.EmiScreenManager;
import com.onehumanawa.cfhcore.content.gui.crafting.team.client.TeamPage;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public class EmiSidebars {
	public static List<EmiIngredient> craftables = List.of();
	public static List<EmiIngredient> lookupHistory = Lists.newArrayList();
	public static List<EmiIngredient> craftHistory = Lists.newArrayList();

	public static List<? extends EmiIngredient> getStacks(SidebarType type) {
		return switch (type) {
			case INDEX -> EmiConfig.editMode ? EmiStackList.stacks : EmiStackList.filteredStacks;
			case CRAFTABLES -> craftables;
			case FAVORITES -> EmiFavorites.favoriteSidebar;
			case LOOKUP_HISTORY -> lookupHistory;
			case CRAFT_HISTORY -> craftHistory;
			case EMPTY -> List.of();
			case CHESS -> EmiChess.SIDEBAR;
			case TEAM -> TeamPage.stacks();
			default -> List.of();
		};
	}

	public static void lookup(EmiIngredient stack) {
		if (!stack.isEmpty()) {
			if (lookupHistory.size() >= 1 && lookupHistory.get(0).equals(stack)) {
				return;
			}

			lookupHistory.remove(stack);
			lookupHistory.add(0, stack);
			EmiPersistentData.save();
			EmiScreenManager.repopulatePanels(SidebarType.LOOKUP_HISTORY);
		}
	}

	public static void craft(EmiRecipe recipe) {
		if (!recipe.getOutputs().isEmpty()) {
			if (craftHistory.size() >= 1 && EmiApi.getRecipeContext(craftHistory.get(0)).equals(recipe)) {
				return;
			}

			EmiIngredient stack = new EmiFavorite.Craftable(recipe);
			craftHistory.removeIf(i -> {
				if (i instanceof EmiFavorite.Craftable c && c.getRecipe().equals(recipe)) {
					return true;
				}

				return false;
			});
			craftHistory.add(0, stack);
			EmiPersistentData.save();
			EmiScreenManager.repopulatePanels(SidebarType.CRAFT_HISTORY);
		}
	}

	public static void save(JsonObject json) {
		JsonArray arr = new JsonArray();

		for (int i = 0; i < 1024 && i < lookupHistory.size(); i++) {
			EmiIngredient stack = lookupHistory.get(i);
			JsonElement el = EmiIngredientSerializer.getSerialized(stack);
			if (el != null && !el.isJsonNull()) {
				arr.add(el);
			}
		}

		json.add("lookup_history", arr);
		arr = new JsonArray();

		for (int ix = 0; ix < 1024 && ix < craftHistory.size(); ix++) {
			EmiIngredient stack = craftHistory.get(ix);
			EmiRecipe recipe = EmiApi.getRecipeContext(stack);
			if (recipe != null && recipe.getId() != null) {
				arr.add(recipe.getId().toString());
			}
		}

		json.add("craft_history", arr);
	}

	public static void load(JsonObject json) {
		lookupHistory.clear();
		if (GsonHelper.isArrayNode(json, "lookup_history")) {
			for (JsonElement el : GsonHelper.getAsJsonArray(json, "lookup_history")) {
				EmiIngredient stack = EmiIngredientSerializer.getDeserialized(el);
				if (!stack.isEmpty()) {
					lookupHistory.add(stack);
				}
			}
		}

		craftHistory.clear();
		if (GsonHelper.isArrayNode(json, "craft_history")) {
			for (JsonElement elx : GsonHelper.getAsJsonArray(json, "craft_history")) {
				if (GsonHelper.isStringValue(elx)) {
					String s = elx.getAsString();
					ResourceLocation recipe = ResourceLocation.tryParse(s);
					if (recipe instanceof ResourceLocation) {
						EmiRecipe recipex = EmiApi.getRecipeManager().getRecipe(recipe);
						if (recipex != null) {
							craftHistory.add(new EmiFavorite.Craftable(recipex));
						}
					}
				}
			}
		}
	}
}
