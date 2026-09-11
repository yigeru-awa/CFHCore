package com.onehumanawa.cfhcore.foundation.recipe.component.impl;

import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeContext;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeValidationContext;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeComponent;
import com.onehumanawa.cfhcore.foundation.recipe.match.ReplacementMatch;

import java.util.Objects;

public class IntegerComponent implements RecipeComponent<Integer> {

    private final String fieldName;
    private final int minValue;
    private final int maxValue;
    private final int defaultValue;

    public IntegerComponent(String fieldName) {
        this(fieldName, 0, Integer.MAX_VALUE, 0);
    }

    public IntegerComponent(String fieldName, int minValue, int maxValue) {
        this(fieldName, minValue, maxValue, minValue);
    }

    public IntegerComponent(String fieldName, int minValue, int maxValue, int defaultValue) {
        this.fieldName = fieldName;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.defaultValue = defaultValue;
    }

    @Override
    public Integer read(JsonObject json, RecipeContext context) {
        String field = targetField(json, context);
        if (field == null || !json.has(field) || !json.get(field).isJsonPrimitive()) {
            return null;
        }
        return json.get(field).getAsInt();
    }

    @Override
    public void write(JsonObject json, Integer value, RecipeContext context) {
        if (value == null) {
            return;
        }
        String field = targetField(json, context);
        json.addProperty(field != null ? field : fieldName, value);
    }

    @Override
    public boolean matches(Integer value, ReplacementMatch match) {
        return false;
    }

    @Override
    public boolean replace(Integer value, ReplacementMatch match, Object replacement, RecipeContext context) {
        return false;
    }

    @Override
    public Integer defaultValue() {
        return defaultValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IntegerComponent that)) return false;
        return minValue == that.minValue
                && maxValue == that.maxValue
                && defaultValue == that.defaultValue
                && fieldName.equals(that.fieldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldName, minValue, maxValue, defaultValue);
    }

    @Override
    public void validate(Integer value, RecipeValidationContext context) {
        if (value == null) {
            context.error("Integer value cannot be null for field: {}", fieldName);
            return;
        }
        if (value < minValue) {
            context.warning("Value {} is below minimum {}, field: {}", value, minValue, fieldName);
        }
        if (value > maxValue) {
            context.warning("Value {} is above maximum {}, field: {}", value, maxValue, fieldName);
        }
    }

    public String fieldName() {
        return fieldName;
    }

    public int minValue() {
        return minValue;
    }

    public int maxValue() {
        return maxValue;
    }

    public static IntegerComponent of(String fieldName) {
        return new IntegerComponent(fieldName);
    }

    public static IntegerComponent of(String fieldName, int min, int max) {
        return new IntegerComponent(fieldName, min, max);
    }
}