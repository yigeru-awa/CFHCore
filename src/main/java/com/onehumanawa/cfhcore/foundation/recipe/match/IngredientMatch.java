package com.onehumanawa.cfhcore.foundation.recipe.match;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Objects;
import java.util.Optional;

public class IngredientMatch implements ReplacementMatch {
    private final Ingredient ingredient;

    public IngredientMatch(Ingredient ingredient) {
        this.ingredient = Objects.requireNonNull(ingredient, "ingredient cannot be null");
    }

    public Ingredient ingredient() {
        return ingredient;
    }

    @Override
    public boolean matches(String itemId) {
        if (itemId == null || itemId.isEmpty()) return false;
        if (ingredient == Ingredient.EMPTY) return false;

        ResourceLocation location;
        try {
            location = ResourceLocation.parse(itemId.startsWith("#") ? itemId.substring(1) : itemId);
        } catch (Exception e) {
            return false;
        }

        if (itemId.startsWith("#")) {
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, location);
            Optional<HolderSet.Named<Item>> tag = BuiltInRegistries.ITEM.getTag(tagKey);
            if (tag.isEmpty()) return false;
            boolean tested = false;
            for (Holder<Item> holder : tag.get()) {
                if (!ingredient.test(new ItemStack(holder.value()))) {
                    return false;
                }
                tested = true;
            }
            return tested;
        }

        Item item = BuiltInRegistries.ITEM.get(location);
        if (item == null || item == Items.AIR) return false;
        return ingredient.test(new ItemStack(item));
    }

    @Override
    public boolean matches(Ingredient other) {
        if (other == null || other == Ingredient.EMPTY) return false;
        if (ingredient == other || ingredient.equals(other)) return true;
        if (ingredient == Ingredient.EMPTY) return false;

        ItemStack[] stacks = other.getItems();
        if (stacks.length == 0) return false;

        for (ItemStack stack : stacks) {
            if (stack.isEmpty() || !ingredient.test(stack)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean matches(ItemStack stack) {
        return ingredient.test(stack);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IngredientMatch that)) return false;
        return Objects.equals(ingredient, that.ingredient);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ingredient);
    }

    @Override
    public String toString() {
        return "IngredientMatch[" + ingredient + "]";
    }
}
