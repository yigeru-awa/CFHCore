package com.onehumanawa.cfhcore.foundation.recipe;

import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;

import javax.annotation.Nullable;

public class RecipeModifyMissingComponentException extends RecipeModifyRuntimeException {
    private final String componentName;
    private final RecipeKey<?> key;

    public RecipeModifyMissingComponentException(String componentName) {
        this(componentName, null);
    }

    public RecipeModifyMissingComponentException(String componentName, RecipeKey<?> key) {
        super("Missing required component: " + componentName);
        this.componentName = componentName;
        this.key = key;
    }

    public String getComponentName() {
        return componentName;
    }

    @Nullable
    public RecipeKey<?> getKey() {
        return key;
    }
}