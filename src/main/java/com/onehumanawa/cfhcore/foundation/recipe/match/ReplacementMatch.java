package com.onehumanawa.cfhcore.foundation.recipe.match;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Interface for match criteria used in recipe replacement operations.
 */
public interface ReplacementMatch {
    /**
     * Checks if this match applies to the given item ID.
     *
     * @param itemId The item ID to check
     * @return true if the item matches
     */
    boolean matches(String itemId);

    /**
     * Checks if this match applies to the given ingredient.
     *
     * @param ingredient The ingredient to check
     * @return true if the ingredient matches
     */
    boolean matches(Ingredient ingredient);

    /**
     * Checks if this match applies to the given item stack.
     *
     * @param stack The item stack to check
     * @return true if the item stack matches
     */
    boolean matches(ItemStack stack);

    /**
     * Creates a replacement match for a specific item ID.
     *
     * @param itemId The item ID to match
     * @return A new ReplacementMatch
     */
    static ReplacementMatch item(String itemId) {
        return new ItemMatch(itemId);
    }

    /**
     * Creates a replacement match for a specific tag.
     *
     * @param tagId The tag ID to match (without # prefix)
     * @return A new ReplacementMatch
     */
    static ReplacementMatch tag(String tagId) {
        return new TagMatch(tagId);
    }

    /**
     * Creates a replacement match from an ingredient.
     *
     * @param ingredient The ingredient to match
     * @return A new ReplacementMatch
     */
    static ReplacementMatch of(Ingredient ingredient) {
        return new IngredientMatch(ingredient);
    }
}