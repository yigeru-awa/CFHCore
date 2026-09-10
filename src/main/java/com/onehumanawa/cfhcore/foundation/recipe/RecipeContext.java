package com.onehumanawa.cfhcore.foundation.recipe;

import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchema;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Context object passed to components during read/write operations.
 */
public class RecipeContext {
    private final KubeJavaRecipe recipe;
    private final Map<String, Object> attributes = new HashMap<>();

    public RecipeContext(KubeJavaRecipe recipe) {
        this.recipe = recipe;
    }

    public KubeJavaRecipe recipe() {
        return recipe;
    }

    public ResourceLocation recipeId() {
        return recipe.id;
    }

    public RecipeSchema schema() {
        return recipe.type.schema();
    }

    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }

    @Nullable
    public HolderLookup.Provider registries() {
        // This would need access to the registry access
        return null;
    }

    public Optional<RecipeManager> recipeManager() {
        // This would need access to the recipe manager
        return Optional.empty();
    }

    public static RecipeContext empty() {
        return new RecipeContext(null);
    }
}