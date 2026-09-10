package com.onehumanawa.cfhcore.foundation.recipe.action;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Action that damages an ingredient item.
 */
public class DamageAction implements IngredientAction {

    private final int damage;

    public DamageAction(int damage) {
        this.damage = damage;
    }

    @Override
    public ItemStack apply(ItemStack stack, Player player, RecipeHolder<?> recipe) {
        if (stack.isEmpty()) return stack;

        ItemStack result = stack.copy();
        if (result.isDamageableItem()) {
            int newDamage = result.getDamageValue() + damage;
            if (newDamage >= result.getMaxDamage()) {
                return ItemStack.EMPTY;
            }
            result.setDamageValue(newDamage);
        }
        return result;
    }

    @Override
    public String type() {
        return "damage";
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "damage");
        json.addProperty("amount", damage);
        return json;
    }

    public int damage() {
        return damage;
    }

    @Override
    public String toString() {
        return "DamageAction{" + damage + "}";
    }
}