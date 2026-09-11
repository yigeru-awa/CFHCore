package com.onehumanawa.cfhcore.foundation.recipe.schema;

import com.onehumanawa.cfhcore.foundation.recipe.RecipeModification;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeValidationContext;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeComponentValueMap;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class RecipeSchema {
    private final List<RecipeKey<?>> keys;
    private final List<BiConsumer<RecipeValidationContext, RecipeModification>> postProcessors;
    private final IdGenerator idGenerator;
    private final String recipeType;
    private final int inputCount;
    private final int outputCount;

    private RecipeSchema(Builder builder) {
        this.keys = List.copyOf(builder.keys);
        this.postProcessors = List.copyOf(builder.postProcessors);
        this.idGenerator = builder.idGenerator != null ? builder.idGenerator : r -> "";
        this.recipeType = builder.recipeType != null ? builder.recipeType : "unknown";
        this.inputCount = builder.inputCount;
        this.outputCount = builder.outputCount;
    }

    public List<RecipeKey<?>> keys() { return keys; }
    public List<BiConsumer<RecipeValidationContext, RecipeModification>> postProcessors() { return postProcessors; }
    public IdGenerator idGenerator() { return idGenerator; }
    public String recipeType() { return recipeType; }
    public int inputCount() { return inputCount; }
    public int outputCount() { return outputCount; }

    public RecipeComponentValueMap createValueMap() {
        return new RecipeComponentValueMap(keys);
    }

    public void postProcess(RecipeValidationContext context, RecipeModification recipe) {
        for (var processor : postProcessors) {
            processor.accept(context, recipe);
        }
    }

    public String buildUniqueId(RecipeModification recipe) {
        return idGenerator.apply(recipe);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<RecipeKey<?>> keys = new ArrayList<>();
        private final List<BiConsumer<RecipeValidationContext, RecipeModification>> postProcessors = new ArrayList<>();
        private IdGenerator idGenerator;
        private String recipeType;
        private int inputCount = 0;
        private int outputCount = 0;

        public Builder key(RecipeKey<?> key) {
            keys.add(key);
            if (key.role() == RecipeComponent.Role.INPUT) inputCount++;
            if (key.role() == RecipeComponent.Role.OUTPUT) outputCount++;
            return this;
        }

        public Builder keys(RecipeKey<?>... keys) {
            for (RecipeKey<?> key : keys) {
                this.keys.add(key);
                if (key.role() == RecipeComponent.Role.INPUT) inputCount++;
                if (key.role() == RecipeComponent.Role.OUTPUT) outputCount++;
            }
            return this;
        }

        public Builder postProcessor(BiConsumer<RecipeValidationContext, RecipeModification> processor) {
            this.postProcessors.add(processor);
            return this;
        }

        @SafeVarargs
        public final Builder postProcessors(BiConsumer<RecipeValidationContext, RecipeModification>... processors) {
            for (BiConsumer<RecipeValidationContext, RecipeModification> processor : processors) {
                this.postProcessors.add(processor);
            }
            return this;
        }

        public Builder idGenerator(IdGenerator generator) {
            this.idGenerator = generator;
            return this;
        }

        public Builder type(String type) {
            this.recipeType = type;
            return this;
        }

        public RecipeSchema build() {
            return new RecipeSchema(this);
        }
    }

    @FunctionalInterface
    public interface IdGenerator {
        String apply(RecipeModification recipe);
    }
}