package com.onehumanawa.cfhcore.content.gui.crafting.team.client;

import com.onehumanawa.cfhcore.content.gui.emi.api.EmiApi;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.config.SidebarType;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiFavorite;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiFavorites;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiPersistentData;
import com.onehumanawa.cfhcore.content.gui.emi.screen.EmiScreenManager;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiContextMenu;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup.Item;
import com.onehumanawa.cfhcore.content.gui.crafting.team.Pin;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class TeamMenus {
	private static Component text(String key, Object... args) {
		return Component.translatable("cfhgui_crafting.team.menu." + key, args);
	}

	static void pin(TeamPinStack stack, double x, double y) {
		Pin pin = stack.pin;
		List<Item> items = new ArrayList<>();
		if (pin.isTree()) {
			items.add(Item.of(text("open_tree"), Icon.LAN, () -> TeamTrees.open(pin, false)));
			items.add(Item.of(text("open_planner"), Icon.FULLSCREEN, () -> TeamTrees.open(pin, true)));
		} else {
			recipeItems(items, stack, stack.getRecipe());
			EmiRecipe recipe = stack.getRecipe();
			if (recipe != null) {
				EmiFavorite kept = favorite(recipe);
				items.add(Item.checked(text("favorite"), kept != null, () -> {
					if (kept != null) {
						EmiFavorites.favorites.remove(kept);
						EmiPersistentData.save();
					} else {
						EmiFavorites.addFavorite(recipe.getOutputs().get(0), recipe);
					}

					EmiScreenManager.repopulatePanels(SidebarType.FAVORITES);
				}));
			}
		}

		items.add(Item.sep());
		unpinItem(items, pin);
		CFHGuiContextMenu.open(x, y, items);
	}

	static void favorite(EmiFavorite fav, double x, double y) {
		List<Item> items = new ArrayList<>();
		EmiRecipe recipe = fav.getRecipe();
		recipeItems(items, fav, recipe);
		if (recipe != null && recipe.getId() != null) {
			items.add(Item.sep());
			Pin pin = TeamPinsClient.recipePin(recipe);
			if (pin == null) {
				items.add(Item.of(text("pin"), Icon.PIN, () -> TeamPinsClient.pinRecipe(recipe)));
			} else {
				unpinItem(items, pin);
			}
		}

		items.add(Item.sep());
		items.add(Item.danger(text("unfavorite"), Icon.TRASH, () -> {
			EmiFavorites.favorites.remove(fav);
			EmiPersistentData.save();
			EmiScreenManager.repopulatePanels(SidebarType.FAVORITES);
		}));
		CFHGuiContextMenu.open(x, y, items);
	}

	private static void recipeItems(List<Item> items, EmiFavorite stack, @Nullable EmiRecipe recipe) {
		items.add(Item.of(text(recipe != null ? "recipe" : "recipes"), Icon.SEARCH, () -> {
			EmiApi.displayRecipes(stack.getStack());
			if (recipe != null) {
				EmiApi.focusRecipe(recipe);
			}
		}));
		items.add(Item.of(text("uses"), Icon.LIST, () -> EmiApi.displayUses(stack.getStack())));
	}

	private static void unpinItem(List<Item> items, Pin pin) {
		if (TeamPinsClient.mayUnpin(pin)) {
			items.add(Item.danger(text("unpin"), Icon.PIN, () -> TeamPinsClient.unpin(pin)));
		} else {
			items.add(Item.disabled(text("pinned_by", pin.byName), Icon.LOCK));
		}
	}

	@Nullable
	private static EmiFavorite favorite(EmiRecipe recipe) {
		for (EmiFavorite fav : EmiFavorites.favorites) {
			if (fav.getRecipe() == recipe) {
				return fav;
			}
		}

		return null;
	}

	private TeamMenus() {
	}
}
