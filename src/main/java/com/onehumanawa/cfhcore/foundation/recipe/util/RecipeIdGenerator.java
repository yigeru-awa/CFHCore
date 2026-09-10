package com.onehumanawa.cfhcore.foundation.recipe.util;

import com.onehumanawa.cfhcore.foundation.recipe.KubeJavaRecipe;

import java.util.function.Function;

/**
 * Utility for generating recipe IDs.
 */
public final class RecipeIdGenerator {

    private RecipeIdGenerator() {}

    public static Function<KubeJavaRecipe, String> fromInputAndOutput() {
        return recipe -> {
            // Simplified - would need proper component access
            return "recipe_" + System.currentTimeMillis();
        };
    }

    public static Function<KubeJavaRecipe, String> fromString(String prefix) {
        return recipe -> prefix + "_" + System.currentTimeMillis();
    }

    public static Function<KubeJavaRecipe, String> fromPattern() {
        return recipe -> {
            // Would extract pattern information
            return "pattern_recipe";
        };
    }

    public static String generateUniqueId(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "_" + System.nanoTime();
    }

    public static String sanitize(String input) {
        if (input == null) return "";
        return input.toLowerCase()
                .replaceAll("[^a-z0-9_/]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }
}