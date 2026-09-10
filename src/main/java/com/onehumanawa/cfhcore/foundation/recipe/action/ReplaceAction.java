package com.onehumanawa.cfhcore.foundation.recipe.action;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Action that replaces an ingredient with a different item.
 */
public class ReplaceAction implements IngredientAction {

    private final ItemStack replacement;

    public ReplaceAction(ItemStack replacement) {
        this.replacement = replacement.copy();
    }

    @Override
    public ItemStack apply(ItemStack stack, Player player, RecipeHolder<?> recipe) {
        return replacement.copy();
    }

    @Override
    public String type() {
        return "replace";
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "replace");
        JsonObject itemJson = new JsonObject();
        itemJson.addProperty("id", replacement.getItem().getDescriptionId());
        itemJson.addProperty("count", replacement.getCount());
        json.add("item", itemJson);
        return json;
    }

    public ItemStack replacement() {
        return replacement.copy();
    }

    @Override
    public String toString() {
        return "ReplaceAction{" + replacement + "}";
    }
}