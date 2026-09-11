package com.onehumanawa.cfhcore.foundation.recipe.action;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

public class KeepAction implements IngredientAction {

    public static final KeepAction INSTANCE = new KeepAction();

    private KeepAction() {}

    @Override
    public ItemStack apply(ItemStack stack, Player player, RecipeHolder<?> recipe) {
        return stack.copy();
    }

    @Override
    public String type() {
        return "keep";
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "keep");
        return json;
    }

    @Override
    public String toString() {
        return "KeepAction";
    }
}