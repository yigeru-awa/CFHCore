package com.onehumanawa.cfhcore.foundation.recipe.component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Uniquely identifies a component within a recipe schema.
 *
 * @param <T> The Java type of the component value
 */
public class RecipeKey<T> {
    private final RecipeComponent<T> component;
    private final Set<String> names;
    private final RecipeComponent.Role role;
    private final boolean optional;
    private final T defaultValue;
    private final boolean alwaysWrite;

    private RecipeKey(Builder<T> builder) {
        this.component = Objects.requireNonNull(builder.component, "component cannot be null");
        this.names = Set.copyOf(builder.names);
        this.role = builder.role;
        this.optional = builder.optional;
        this.defaultValue = builder.defaultValue;
        this.alwaysWrite = builder.alwaysWrite;
    }

    public RecipeComponent<T> component() { return component; }
    public Set<String> names() { return names; }
    public RecipeComponent.Role role() { return role; }
    public boolean optional() { return optional; }
    public T defaultValue() { return defaultValue; }
    public boolean alwaysWrite() { return alwaysWrite; }

    public boolean hasName(String name) {
        return names.contains(name);
    }

    public String primaryName() {
        return names.isEmpty() ? "unnamed" : names.iterator().next();
    }

    public static <T> Builder<T> builder(RecipeComponent<T> component) {
        return new Builder<>(component);
    }

    public static class Builder<T> {
        private final RecipeComponent<T> component;
        private final Set<String> names = new HashSet<>();
        private RecipeComponent.Role role = RecipeComponent.Role.OTHER;
        private boolean optional = false;
        private T defaultValue = null;
        private boolean alwaysWrite = false;

        private Builder(RecipeComponent<T> component) {
            this.component = component;
        }

        public Builder<T> name(String name) {
            names.add(name);
            return this;
        }

        public Builder<T> names(String... names) {
            this.names.addAll(Arrays.asList(names));
            return this;
        }

        public Builder<T> role(RecipeComponent.Role role) {
            this.role = role;
            return this;
        }

        public Builder<T> input() {
            this.role = RecipeComponent.Role.INPUT;
            return this;
        }

        public Builder<T> output() {
            this.role = RecipeComponent.Role.OUTPUT;
            return this;
        }

        public Builder<T> optional() {
            this.optional = true;
            return this;
        }

        public Builder<T> defaultValue(T value) {
            this.defaultValue = value;
            return this;
        }

        public Builder<T> alwaysWrite() {
            this.alwaysWrite = true;
            return this;
        }

        public RecipeKey<T> build() {
            if (names.isEmpty()) {
                throw new IllegalStateException("RecipeKey must have at least one name");
            }
            return new RecipeKey<>(this);
        }
    }

    @Override
    public String toString() {
        return primaryName() + (optional ? "?" : "") + ":" + component.getClass().getSimpleName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecipeKey<?> that)) return false;
        return Objects.equals(component, that.component) && Objects.equals(names, that.names);
    }

    @Override
    public int hashCode() {
        return Objects.hash(component, names);
    }
}