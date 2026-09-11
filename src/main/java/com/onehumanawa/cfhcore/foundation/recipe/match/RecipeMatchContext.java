package com.onehumanawa.cfhcore.foundation.recipe.match;

import com.onehumanawa.cfhcore.foundation.recipe.RecipeInfo;

import java.util.HashMap;
import java.util.Map;

public class RecipeMatchContext {
    private final Map<String, Object> attributes = new HashMap<>();
    private RecipeInfo currentRecipe;

    public RecipeMatchContext() {
    }

    public RecipeMatchContext(RecipeInfo recipe) {
        this.currentRecipe = recipe;
    }

    public RecipeInfo currentRecipe() {
        return currentRecipe;
    }

    public void setCurrentRecipe(RecipeInfo recipe) {
        this.currentRecipe = recipe;
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

    public void clear() {
        attributes.clear();
    }

    public static RecipeMatchContext of(RecipeInfo recipe) {
        return new RecipeMatchContext(recipe);
    }

    @Override
    public String toString() {
        return "RecipeMatchContext{" +
                "currentRecipe=" + currentRecipe +
                ", attributes=" + attributes +
                '}';
    }
}