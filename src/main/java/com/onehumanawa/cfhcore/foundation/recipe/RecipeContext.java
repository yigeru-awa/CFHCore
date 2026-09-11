package com.onehumanawa.cfhcore.foundation.recipe;

import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeComponentValue;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchema;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class RecipeContext {
    private final RecipeModification recipe;
    private final Map<String, Object> attributes = new HashMap<>();
    private RecipeComponentValue<?> bound;

    public RecipeContext(RecipeModification recipe) {
        this.recipe = recipe;
    }

    @Nullable
    public RecipeModification recipe() {
        return recipe;
    }

    @Nullable
    public ResourceLocation recipeId() {
        return recipe != null ? recipe.id : null;
    }

    @Nullable
    public RecipeSchema schema() {
        return recipe != null ? recipe.type.schema() : null;
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }

    public void bind(RecipeComponentValue<?> holder) {
        this.bound = holder;
    }

    @Nullable
    public RecipeComponentValue<?> bound() {
        return bound;
    }

    @Nullable
    public RecipeKey<?> key() {
        return bound != null ? bound.key() : null;
    }

    public Set<String> fieldNames() {
        RecipeKey<?> key = key();
        return key != null ? key.names() : Set.of();
    }

    @Nullable
    public String fieldName() {
        RecipeKey<?> key = key();
        return key != null ? key.primaryName() : null;
    }

    public boolean isOutput() {
        RecipeKey<?> key = key();
        return key != null && key.role() == RecipeComponent.Role.OUTPUT;
    }

    public boolean stackFormat() {
        RecipeKey<?> key = key();
        return key != null && key.stackFormat();
    }

    @SuppressWarnings("unchecked")
    public <V> void setValue(V value) {
        if (bound == null) {
            throw new IllegalStateException("No recipe component is bound to this context");
        }
        ((RecipeComponentValue<V>) bound).value(value);
    }

    @Nullable
    public HolderLookup.Provider registries() {
        return RecipeModificationHandler.registries();
    }

    public Optional<RecipeManager> recipeManager() {
        return Optional.ofNullable(RecipeModificationHandler.recipeManager());
    }

    public static RecipeContext empty() {
        return new RecipeContext(null);
    }
}
