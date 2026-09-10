package com.onehumanawa.cfhcore.foundation.recipe.match;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Objects;

/**
 * Replacement match that matches an item tag.
 */
public class TagMatch implements ReplacementMatch {
    private final String tagId;
    private final ResourceLocation location;
    private TagKey<Item> cachedTag;

    public TagMatch(String tagId) {
        this.tagId = Objects.requireNonNull(tagId, "tagId cannot be null");
        this.location = ResourceLocation.parse(tagId);
    }

    public String tagId() {
        return tagId;
    }

    public ResourceLocation location() {
        return location;
    }

    public TagKey<Item> tag() {
        if (cachedTag == null) {
            cachedTag = TagKey.create(BuiltInRegistries.ITEM.key(), location);
        }
        return cachedTag;
    }

    @Override
    public boolean matches(String id) {
        // A tag match does not match a specific item ID
        return false;
    }

    @Override
    public boolean matches(Ingredient ingredient) {
        // Check if the ingredient is a tag ingredient
        // This is a simplified check - proper implementation would need to inspect the ingredient's internal structure
        return false;
    }

    @Override
    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(tag());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TagMatch that)) return false;
        return Objects.equals(tagId, that.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tagId);
    }

    @Override
    public String toString() {
        return "TagMatch[#" + tagId + "]";
    }
}