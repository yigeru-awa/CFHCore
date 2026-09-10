package com.onehumanawa.cfhcore.foundation.recipe.match;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Objects;

/**
 * Replacement match that uses an Ingredient for matching.
 */
public class IngredientMatch implements ReplacementMatch {
    private final Ingredient ingredient;

    public IngredientMatch(Ingredient ingredient) {
        this.ingredient = Objects.requireNonNull(ingredient, "ingredient cannot be null");
    }

    public Ingredient ingredient() {
        return ingredient;
    }

    @Override
    public boolean matches(String itemId) {
        // This is a simplified check - proper implementation would need to get the item from the ID
        return false;
    }

    @Override
    public boolean matches(Ingredient other) {
        // Check if ingredients are functionally equivalent
        // This is a simplified check
        return ingredient == other;
    }

    @Override
    public boolean matches(ItemStack stack) {
        return ingredient.test(stack);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IngredientMatch that)) return false;
        return Objects.equals(ingredient, that.ingredient);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ingredient);
    }

    @Override
    public String toString() {
        return "IngredientMatch[" + ingredient + "]";
    }
}