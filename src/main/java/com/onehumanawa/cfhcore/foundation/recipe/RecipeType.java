package com.onehumanawa.cfhcore.foundation.recipe;

import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchema;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Defines a recipe type with its schema and serializer information.
 */
public class RecipeType {
    private final ResourceLocation id;
    private final RecipeSchema schema;
    private final ResourceLocation serializerKey;
    private final String serializerType;
    private RecipeSerializer<?> cachedSerializer;

    private RecipeType(Builder builder) {
        this.id = builder.id;
        this.schema = builder.schema;
        this.serializerKey = builder.serializerKey;
        this.serializerType = builder.serializerType;
    }

    public ResourceLocation id() { return id; }
    public RecipeSchema schema() { return schema; }
    public ResourceLocation serializerKey() { return serializerKey; }
    public String serializerType() { return serializerType; }

    @SuppressWarnings("unchecked")
    public <T extends RecipeSerializer<?>> T kjsGetSerializer() {
        if (cachedSerializer == null) {
            // Would need to look up from registry
            // This is a placeholder - actual implementation would use BuiltInRegistries.RECIPE_SERIALIZER
        }
        return (T) cachedSerializer;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ResourceLocation id;
        private RecipeSchema schema;
        private ResourceLocation serializerKey;
        private String serializerType;

        public Builder id(ResourceLocation id) {
            this.id = id;
            return this;
        }

        public Builder id(String id) {
            this.id = ResourceLocation.parse(id);
            return this;
        }

        public Builder schema(RecipeSchema schema) {
            this.schema = schema;
            return this;
        }

        public Builder serializerKey(ResourceLocation key) {
            this.serializerKey = key;
            return this;
        }

        public Builder serializerKey(String key) {
            this.serializerKey = ResourceLocation.parse(key);
            return this;
        }

        public Builder serializerType(String type) {
            this.serializerType = type;
            return this;
        }

        public RecipeType build() {
            if (id == null) throw new IllegalStateException("RecipeType id is required");
            if (schema == null) throw new IllegalStateException("RecipeType schema is required");
            if (serializerKey == null) throw new IllegalStateException("RecipeType serializerKey is required");
            if (serializerType == null) throw new IllegalStateException("RecipeType serializerType is required");
            return new RecipeType(this);
        }
    }

    @Override
    public String toString() {
        return id.toString();
    }
}