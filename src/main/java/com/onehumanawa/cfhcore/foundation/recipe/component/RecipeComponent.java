package com.onehumanawa.cfhcore.foundation.recipe.component;

import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeContext;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeValidationContext;
import com.onehumanawa.cfhcore.foundation.recipe.match.ReplacementMatch;

/**
 * Represents a typed component of a recipe (input, output, time, etc.)
 *
 * @param <T> The Java type of the component value
 */
public interface RecipeComponent<T> {

    /** Role of the component in a recipe */
    enum Role {
        INPUT, OUTPUT, OTHER
    }

    T read(JsonObject json, RecipeContext context);

    void write(JsonObject json, T value, RecipeContext context);

    boolean matches(T value, ReplacementMatch match);

    boolean replace(T value, ReplacementMatch match, Object replacement, RecipeContext context);

    T defaultValue();

    void validate(T value, RecipeValidationContext context);

    default RecipeKey<T> key(String name, Role role) {
        return RecipeKey.<T>builder(this)
                .name(name)
                .role(role)
                .build();
    }

    default RecipeKey<T> key(Role role, String... names) {
        return RecipeKey.<T>builder(this)
                .names(names)
                .role(role)
                .build();
    }
}