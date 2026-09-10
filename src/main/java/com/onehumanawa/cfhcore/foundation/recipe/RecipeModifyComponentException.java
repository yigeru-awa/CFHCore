package com.onehumanawa.cfhcore.foundation.recipe;

import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeComponentValue;

import javax.annotation.Nullable;

/**
 * Thrown when a recipe component fails to read or write.
 */
public class RecipeModifyComponentException extends RecipeModifyRuntimeException {
    private final RecipeComponentValue<?> componentValue;

    public RecipeModifyComponentException(String message) {
        this(message, null, null);
    }

    public RecipeModifyComponentException(String message, Throwable cause) {
        this(message, cause, null);
    }

    public RecipeModifyComponentException(String message, Throwable cause, RecipeComponentValue<?> componentValue) {
        super(message, cause);
        this.componentValue = componentValue;
    }

    @Nullable
    public RecipeComponentValue<?> getComponentValue() {
        return componentValue;
    }
}