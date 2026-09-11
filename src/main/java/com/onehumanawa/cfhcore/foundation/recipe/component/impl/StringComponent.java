package com.onehumanawa.cfhcore.foundation.recipe.component.impl;

import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeContext;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeValidationContext;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeComponent;
import com.onehumanawa.cfhcore.foundation.recipe.match.ReplacementMatch;

import java.util.Objects;
import java.util.Set;

public class StringComponent implements RecipeComponent<String> {

    private final String fieldName;
    private final String defaultValue;
    private final Set<String> allowedValues;

    public StringComponent(String fieldName) {
        this(fieldName, "", Set.of());
    }

    public StringComponent(String fieldName, String defaultValue) {
        this(fieldName, defaultValue, Set.of());
    }

    public StringComponent(String fieldName, String defaultValue, Set<String> allowedValues) {
        this.fieldName = fieldName;
        this.defaultValue = defaultValue;
        this.allowedValues = allowedValues;
    }

    @Override
    public String read(JsonObject json, RecipeContext context) {
        String field = targetField(json, context);
        if (field == null || !json.has(field) || !json.get(field).isJsonPrimitive()) {
            return null;
        }
        return json.get(field).getAsString();
    }

    @Override
    public void write(JsonObject json, String value, RecipeContext context) {
        if (value == null) {
            return;
        }
        String field = targetField(json, context);
        json.addProperty(field != null ? field : fieldName, value);
    }

    @Override
    public boolean matches(String value, ReplacementMatch match) {
        return false;
    }

    @Override
    public boolean replace(String value, ReplacementMatch match, Object replacement, RecipeContext context) {
        return false;
    }

    @Override
    public String defaultValue() {
        return defaultValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StringComponent that)) return false;
        return fieldName.equals(that.fieldName)
                && Objects.equals(defaultValue, that.defaultValue)
                && allowedValues.equals(that.allowedValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldName, defaultValue, allowedValues);
    }

    @Override
    public void validate(String value, RecipeValidationContext context) {
        if (value == null) {
            context.error("String value cannot be null for field: {}", fieldName);
            return;
        }
        if (!allowedValues.isEmpty() && !allowedValues.contains(value)) {
            context.warning("Value '{}' is not in allowed values: {}, field: {}", value, allowedValues, fieldName);
        }
    }

    public static StringComponent of(String fieldName) {
        return new StringComponent(fieldName);
    }

    public static StringComponent of(String fieldName, String defaultValue) {
        return new StringComponent(fieldName, defaultValue);
    }

    public static StringComponent of(String fieldName, String defaultValue, String... allowed) {
        return new StringComponent(fieldName, defaultValue, Set.of(allowed));
    }
}