package com.onehumanawa.cfhcore.foundation.recipe.match;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public interface ReplacementMatch {
    boolean matches(String itemId);

    boolean matches(Ingredient ingredient);

    boolean matches(ItemStack stack);

    static ReplacementMatch item(String itemId) {
        return new ItemMatch(itemId);
    }

    static ReplacementMatch tag(String tagId) {
        return new TagMatch(tagId);
    }

    static ReplacementMatch of(Ingredient ingredient) {
        return new IngredientMatch(ingredient);
    }
}