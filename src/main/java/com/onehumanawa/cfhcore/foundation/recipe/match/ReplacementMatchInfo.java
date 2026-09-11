package com.onehumanawa.cfhcore.foundation.recipe.match;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public class ReplacementMatchInfo {
    private final ReplacementMatch match;
    private final Object replacement;

    public ReplacementMatchInfo(ReplacementMatch match, Object replacement) {
        this.match = match;
        this.replacement = replacement;
    }

    public ReplacementMatchInfo(Ingredient ingredient, Object replacement) {
        this(new IngredientMatch(ingredient), replacement);
    }

    public ReplacementMatchInfo(String itemId, Object replacement) {
        this(new ItemMatch(itemId), replacement);
    }

    public ReplacementMatch match() {
        return match;
    }

    public Object replacement() {
        return replacement;
    }

    public boolean matches(String itemId) {
        return match.matches(itemId);
    }

    public boolean matches(Ingredient ingredient) {
        return match.matches(ingredient);
    }

    public boolean matches(ItemStack stack) {
        return match.matches(stack);
    }

    public boolean matches(ItemStack stack, String itemId) {
        return match.matches(stack) || match.matches(itemId);
    }

    public static ReplacementMatchInfo of(String itemId, String replacement) {
        return new ReplacementMatchInfo(new ItemMatch(itemId), replacement);
    }

    public static ReplacementMatchInfo of(String itemId, Object replacement) {
        return new ReplacementMatchInfo(new ItemMatch(itemId), replacement);
    }

    public static ReplacementMatchInfo tag(String tagId, String replacement) {
        return new ReplacementMatchInfo(new TagMatch(tagId), replacement);
    }

    @Override
    public String toString() {
        return match + " -> " + replacement;
    }
}