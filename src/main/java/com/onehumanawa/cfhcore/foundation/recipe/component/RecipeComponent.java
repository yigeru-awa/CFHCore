package com.onehumanawa.cfhcore.foundation.recipe.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeContext;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeValidationContext;
import com.onehumanawa.cfhcore.foundation.recipe.match.ReplacementMatch;

import java.util.Set;

public interface RecipeComponent<T> {
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

    default Set<String> fieldNames(RecipeContext context) {
        return context != null ? context.fieldNames() : Set.of();
    }

    default boolean isOutput(RecipeContext context) {
        return context != null && context.isOutput();
    }

    default boolean useId(RecipeContext context) {
        return context != null && (context.isOutput() || context.stackFormat());
    }

    default JsonElement locate(JsonObject json, RecipeContext context) {
        for (String name : fieldNames(context)) {
            if (json.has(name)) {
                return json.get(name);
            }
        }
        return json;
    }

    default String targetField(JsonObject json, RecipeContext context) {
        for (String name : fieldNames(context)) {
            if (json.has(name)) {
                return name;
            }
        }
        return context != null ? context.fieldName() : null;
    }
}
