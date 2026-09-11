package com.onehumanawa.cfhcore.foundation.recipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

public record RecipeInfo(ResourceLocation id, String modId, ResourceLocation typeId) {

    public RecipeInfo(ResourceLocation id) {
        this(id, id.getNamespace(), null);
    }

    public RecipeInfo(ResourceLocation id, ResourceLocation typeId) {
        this(id, id.getNamespace(), typeId);
    }

    public static RecipeInfo from(RecipeHolder<?> holder, ResourceLocation typeId) {
        return new RecipeInfo(holder.id(), typeId);
    }

    public static RecipeInfo from(RecipeHolder<?> holder,
                                  java.util.function.Function<net.minecraft.world.item.crafting.Recipe<?>, ResourceLocation> typeLookup) {
        ResourceLocation typeId = typeLookup.apply(holder.value());
        return new RecipeInfo(holder.id(), typeId);
    }

    @Override
    public String toString() {
        return typeId == null ? id.toString() : id + "[" + typeId + "]";
    }
}