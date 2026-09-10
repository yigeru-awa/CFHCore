package com.onehumanawa.cfhcore.foundation.recipe.action;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Interface for actions performed on ingredients during crafting.
 * Equivalent to KubeJS's IngredientAction.
 */
public interface IngredientAction {

    /**
     * Performs the action on the given item stack.
     *
     * @param stack   The item stack to act upon
     * @param player  The player performing the crafting
     * @param recipe  The recipe being crafted
     * @return The resulting item stack (may be the same or modified)
     */
    ItemStack apply(ItemStack stack, Player player, RecipeHolder<?> recipe);

    /**
     * Gets the type ID of this action.
     *
     * @return The action type ID
     */
    String type();

    /**
     * Serializes this action to JSON.
     *
     * @return The JSON representation
     */
    com.google.gson.JsonObject toJson();
}