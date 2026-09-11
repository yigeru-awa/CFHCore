package com.onehumanawa.cfhcore.foundation.recipe.action;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.function.BiFunction;

public class CustomAction implements IngredientAction {

    private final String id;
    private final BiFunction<ItemStack, Player, ItemStack> function;

    public CustomAction(String id, BiFunction<ItemStack, Player, ItemStack> function) {
        this.id = id;
        this.function = function;
    }

    @Override
    public ItemStack apply(ItemStack stack, Player player, RecipeHolder<?> recipe) {
        return function.apply(stack, player);
    }

    @Override
    public String type() {
        return "custom";
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "custom");
        json.addProperty("id", id);
        return json;
    }

    public String id() {
        return id;
    }

    @Override
    public String toString() {
        return "CustomAction{" + id + "}";
    }
}