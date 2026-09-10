package com.onehumanawa.cfhcore.foundation.recipe.component;

import com.onehumanawa.cfhcore.foundation.recipe.RecipeContext;
import com.onehumanawa.cfhcore.foundation.recipe.KubeJavaRecipe;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeValidationContext;
import com.google.gson.JsonObject;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

/**
 * A map of component values for a recipe.
 */
public class RecipeComponentValueMap {
    public static final RecipeComponentValueMap EMPTY = new RecipeComponentValueMap(List.of());

    private final List<RecipeComponentValue<?>> holders;
    private final Map<String, RecipeComponentValue<?>> nameIndex;

    public RecipeComponentValueMap(List<RecipeKey<?>> keys) {
        this.holders = new ArrayList<>(keys.size());
        this.nameIndex = new LinkedHashMap<>();

        for (RecipeKey<?> key : keys) {
            RecipeComponentValue<?> holder = new RecipeComponentValue<>(key);
            this.holders.add(holder);
            for (String name : key.names()) {
                this.nameIndex.put(name, holder);
            }
        }
    }

    public List<RecipeComponentValue<?>> holders() {
        return Collections.unmodifiableList(holders);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> RecipeComponentValue<T> getHolder(RecipeKey<T> key) {
        for (RecipeComponentValue<?> holder : holders) {
            if (holder.key().equals(key)) {
                return (RecipeComponentValue<T>) holder;
            }
        }
        return null;
    }

    @Nullable
    public RecipeComponentValue<?> getHolder(String name) {
        return nameIndex.get(name);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getValue(RecipeKey<T> key) {
        RecipeComponentValue<T> holder = getHolder(key);
        return holder != null ? holder.value() : null;
    }

    public <T> void setValue(RecipeKey<T> key, T value) {
        RecipeComponentValue<T> holder = getHolder(key);
        if (holder != null) {
            holder.value(value);
        }
    }

    public Set<String> keySet() {
        return Collections.unmodifiableSet(nameIndex.keySet());
    }

    public void deserialize(JsonObject json, RecipeContext context) {
        for (RecipeComponentValue<?> holder : holders) {
            try {
                holder.readFromJson(json, context);
            } catch (Exception e) {
                if (!holder.key().optional()) {
                    throw new RuntimeException("Failed to read component: " + holder.key().primaryName(), e);
                }
            }
        }
    }

    public void serialize(JsonObject json, RecipeContext context) {
        for (RecipeComponentValue<?> holder : holders) {
            if (holder.shouldWrite()) {
                holder.writeToJson(json, context);
            }
        }
    }

    public void validate(RecipeValidationContext context, KubeJavaRecipe recipe) {
        for (RecipeComponentValue<?> holder : holders) {
            holder.validate(context, recipe);
        }
    }

    public void forEach(Consumer<RecipeComponentValue<?>> action) {
        holders.forEach(action);
    }

    public int size() {
        return holders.size();
    }

    public boolean isEmpty() {
        return holders.isEmpty();
    }
}