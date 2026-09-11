package com.onehumanawa.cfhcore.foundation.recipe.match;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Objects;

public class TagMatch implements ReplacementMatch {
    private final String tagId;
    private final ResourceLocation location;
    private TagKey<Item> cachedTag;

    public TagMatch(String tagId) {
        String normalized = tagId != null && tagId.startsWith("#") ? tagId.substring(1) : tagId;
        this.tagId = Objects.requireNonNull(normalized, "tagId cannot be null");
        this.location = ResourceLocation.parse(this.tagId);
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
        if (id == null || id.isEmpty()) return false;

        ResourceLocation parsed;
        if (id.startsWith("#")) {
            try {
                parsed = ResourceLocation.parse(id.substring(1));
            } catch (Exception e) {
                return false;
            }
            return location.equals(parsed);
        }

        try {
            parsed = ResourceLocation.parse(id);
        } catch (Exception e) {
            return false;
        }
        Item item = BuiltInRegistries.ITEM.get(parsed);
        if (item == null || item == Items.AIR) return false;
        return BuiltInRegistries.ITEM.wrapAsHolder(item).is(tag());
    }

    @Override
    public boolean matches(Ingredient ingredient) {
        if (ingredient == null || ingredient == Ingredient.EMPTY) return false;

        ItemStack[] stacks = ingredient.getItems();
        if (stacks.length == 0) return false;

        TagKey<Item> tag = tag();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty() || !stack.is(tag)) {
                return false;
            }
        }
        return true;
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
