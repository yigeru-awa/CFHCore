package com.onehumanawa.cfhcore.foundation.recipe.action;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

public interface IngredientAction {

    ItemStack apply(ItemStack stack, Player player, RecipeHolder<?> recipe);
    String type();
    com.google.gson.JsonObject toJson();
}