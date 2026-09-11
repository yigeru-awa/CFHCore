package com.onehumanawa.cfhcore.foundation.recipe.match;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Objects;

public class ItemMatch implements ReplacementMatch {
    private final String itemId;
    private final ResourceLocation location;
    private Item cachedItem;

    public ItemMatch(String itemId) {
        this.itemId = Objects.requireNonNull(itemId, "itemId cannot be null");
        this.location = ResourceLocation.parse(itemId);
    }

    public String itemId() {
        return itemId;
    }

    public ResourceLocation location() {
        return location;
    }

    public Item item() {
        if (cachedItem == null) {
            cachedItem = BuiltInRegistries.ITEM.get(location);
        }
        return cachedItem;
    }

    @Override
    public boolean matches(String id) {
        return itemId.equals(id);
    }

    @Override
    public boolean matches(Ingredient ingredient) {
        if (ingredient == Ingredient.EMPTY) return false;
        Item item = item();
        if (item == null) return false;
        return ingredient.test(new ItemStack(item));
    }

    @Override
    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = item();
        if (item == null) return false;
        return stack.getItem() == item;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemMatch that)) return false;
        return Objects.equals(itemId, that.itemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId);
    }

    @Override
    public String toString() {
        return "ItemMatch[" + itemId + "]";
    }
}