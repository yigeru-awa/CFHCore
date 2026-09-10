package com.onehumanawa.cfhcore.foundation.recipe.component.impl;

import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeContext;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeValidationContext;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeComponent;
import com.onehumanawa.cfhcore.foundation.recipe.match.ReplacementMatch;

/**
 * Component for float values in recipes (experience, chance, etc.).
 */
public class FloatComponent implements RecipeComponent<Float> {

    private final String fieldName;
    private final float minValue;
    private final float maxValue;
    private final float defaultValue;

    public FloatComponent(String fieldName) {
        this(fieldName, 0.0f, Float.MAX_VALUE, 0.0f);
    }

    public FloatComponent(String fieldName, float minValue, float maxValue) {
        this(fieldName, minValue, maxValue, minValue);
    }

    public FloatComponent(String fieldName, float minValue, float maxValue, float defaultValue) {
        this.fieldName = fieldName;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.defaultValue = defaultValue;
    }

    @Override
    public Float read(JsonObject json, RecipeContext context) {
        if (json.has(fieldName)) {
            return json.get(fieldName).getAsFloat();
        }
        return defaultValue;
    }

    @Override
    public void write(JsonObject json, Float value, RecipeContext context) {
        if (value != null && !value.equals(defaultValue)) {
            json.addProperty(fieldName, value);
        }
    }

    @Override
    public boolean matches(Float value, ReplacementMatch match) {
        return false;
    }

    @Override
    public boolean replace(Float value, ReplacementMatch match, Object replacement, RecipeContext context) {
        return false;
    }

    @Override
    public Float defaultValue() {
        return defaultValue;
    }

    @Override
    public void validate(Float value, RecipeValidationContext context) {
        if (value == null) {
            context.error("Float value cannot be null for field: {}", fieldName);
            return;
        }
        if (value < minValue) {
            context.warning("Value {} is below minimum {}, field: {}", value, minValue, fieldName);
        }
        if (value > maxValue) {
            context.warning("Value {} is above maximum {}, field: {}", value, maxValue, fieldName);
        }
    }

    public static FloatComponent of(String fieldName) {
        return new FloatComponent(fieldName);
    }

    public static FloatComponent of(String fieldName, float min, float max) {
        return new FloatComponent(fieldName, min, max);
    }
}