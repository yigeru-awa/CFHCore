package com.onehumanawa.cfhcore.foundation.recipe;

import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchema;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Context for recipe validation.
 */
public class RecipeValidationContext {
    private final KubeJavaRecipe recipe;
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private String currentPath = "";

    public RecipeValidationContext(KubeJavaRecipe recipe) {
        this.recipe = recipe;
    }

    public KubeJavaRecipe recipe() {
        return recipe;
    }

    public RecipeInfo info() {
        return new RecipeInfo(recipe.id, recipe.type.id());
    }

    public RecipeSchema schema() {
        return recipe.type.schema();
    }

    public void push(String path) {
        this.currentPath = currentPath.isEmpty() ? path : currentPath + "." + path;
    }

    public void push(int index) {
        this.currentPath = currentPath.isEmpty() ? "[" + index + "]" : currentPath + "[" + index + "]";
    }

    public void pop() {
        int lastDot = currentPath.lastIndexOf('.');
        int lastBracket = currentPath.lastIndexOf('[');
        int lastSeparator = Math.max(lastDot, lastBracket);
        this.currentPath = lastSeparator >= 0 ? currentPath.substring(0, lastSeparator) : "";
    }

    public String currentPath() {
        return currentPath;
    }

    public void error(String message) {
        errors.add(currentPath + ": " + message);
    }

    public void error(String message, Object... args) {
        errors.add(currentPath + ": " + String.format(message, args));
    }

    public void warning(String message) {
        warnings.add(currentPath + ": " + message);
    }

    public void warning(String message, Object... args) {
        warnings.add(currentPath + ": " + String.format(message, args));
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public List<String> errors() {
        return List.copyOf(errors);
    }

    public List<String> warnings() {
        return List.copyOf(warnings);
    }

    public void clear() {
        errors.clear();
        warnings.clear();
    }

    public void setKey(int index) {
        push(index);
    }

    public void setKey(String name) {
        push(name);
    }
}