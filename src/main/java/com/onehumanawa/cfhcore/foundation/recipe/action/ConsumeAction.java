package com.onehumanawa.cfhcore.foundation.recipe.action;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

public class ConsumeAction implements IngredientAction {

    public static final ConsumeAction INSTANCE = new ConsumeAction();

    private ConsumeAction() {}

    @Override
    public ItemStack apply(ItemStack stack, Player player, RecipeHolder<?> recipe) {
        ItemStack result = stack.copy();
        result.shrink(1);
        return result.isEmpty() ? ItemStack.EMPTY : result;
    }

    @Override
    public String type() {
        return "consume";
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "consume");
        return json;
    }

    @Override
    public String toString() {
        return "ConsumeAction";
    }
}