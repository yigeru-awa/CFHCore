package com.onehumanawa.cfhcore.foundation.recipe.component;

import com.onehumanawa.cfhcore.foundation.recipe.RecipeContext;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeValidationContext;
import com.onehumanawa.cfhcore.foundation.recipe.match.ReplacementMatch;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeModification;
import com.google.gson.JsonObject;

import javax.annotation.Nullable;
import java.util.Objects;

public class RecipeComponentValue<T> {
    private final RecipeKey<T> key;
    private T value;
    private boolean written = false;
    private boolean dirty = false;

    public RecipeComponentValue(RecipeKey<T> key) {
        this.key = Objects.requireNonNull(key, "key cannot be null");
        this.value = key.defaultValue();
    }

    public RecipeKey<T> key() { return key; }

    @Nullable
    public T value() { return value; }

    public RecipeComponentValue<T> value(T value) {
        this.value = value;
        this.dirty = true;
        return this;
    }

    public boolean written() { return written; }

    public RecipeComponentValue<T> written(boolean written) {
        this.written = written;
        return this;
    }

    public boolean dirty() { return dirty; }

    public RecipeComponentValue<T> clean() {
        this.dirty = false;
        return this;
    }

    public boolean shouldWrite() {
        return written || dirty || key.alwaysWrite();
    }

    public void readFromJson(JsonObject json, RecipeContext context) {
        if (context != null) {
            context.bind(this);
        }
        T readValue = key.component().read(json, context);
        if (readValue != null) {
            this.value = readValue;
            this.written = true;
        }
    }

    public void writeToJson(JsonObject json, RecipeContext context) {
        if (value == null && !key.optional()) {
            throw new IllegalStateException("Cannot write null value for required component: " + key.primaryName());
        }
        if (value != null) {
            if (context != null) {
                context.bind(this);
            }
            key.component().write(json, value, context);
            this.written = true;
            this.dirty = false;
        }
    }

    public boolean matches(ReplacementMatch match) {
        return value != null && key.component().matches(value, match);
    }

    public boolean replace(ReplacementMatch match, Object replacement, RecipeContext context) {
        if (value == null) return false;
        if (context != null) {
            context.bind(this);
        }
        boolean result = key.component().replace(value, match, replacement, context);
        if (result) {
            this.dirty = true;
        }
        return result;
    }

    public void validate(RecipeValidationContext context, RecipeModification recipe) {
        if (value != null) {
            key.component().validate(value, context);
        } else if (!key.optional()) {
            context.error("Missing required component: " + key.primaryName());
        }
    }

    public void write() {
        this.written = true;
        this.dirty = false;
    }

    @Override
    public String toString() {
        return key.primaryName() + "=" + value + (dirty ? "*" : "");
    }
}