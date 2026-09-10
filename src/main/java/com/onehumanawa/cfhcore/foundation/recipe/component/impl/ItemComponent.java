package com.onehumanawa.cfhcore.foundation.recipe.component.impl;

import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeContext;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeValidationContext;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;
import com.onehumanawa.cfhcore.foundation.recipe.match.ReplacementMatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Component for single item values in recipes.
 * Handles both regular item IDs and tag references (#prefix).
 */
public class ItemComponent implements RecipeComponent<String> {

    public static final ItemComponent INSTANCE = new ItemComponent();

    @Override
    public String read(JsonObject json, RecipeContext context) {
        if (json.has("item")) {
            return json.get("item").getAsString();
        }
        if (json.has("id")) {
            return json.get("id").getAsString();
        }
        if (json.has("tag")) {
            return "#" + json.get("tag").getAsString();
        }
        return null;
    }

    @Override
    public void write(JsonObject json, String value, RecipeContext context) {
        if (value == null) return;
        if (value.startsWith("#")) {
            json.addProperty("tag", value.substring(1));
        } else {
            json.addProperty("item", value);
        }
    }

    @Override
    public boolean matches(String value, ReplacementMatch match) {
        if (value == null) return false;
        return match.matches(value);
    }

    @Override
    public boolean replace(String value, ReplacementMatch match, Object replacement, RecipeContext context) {
        if (value == null || !matches(value, match)) return false;
        if (replacement instanceof String replacementStr) {
            context.recipe().setValue(RecipeKey.builder(INSTANCE).name("item").build(), replacementStr);
            return true;
        }
        return false;
    }

    @Override
    public String defaultValue() {
        return "minecraft:air";
    }

    @Override
    public void validate(String value, RecipeValidationContext context) {
        if (value == null || value.isEmpty()) {
            context.error("Item value cannot be null or empty");
            return;
        }

        if (value.startsWith("#")) {
            // Tag validation - skip for now
            return;
        }

        try {
            ResourceLocation location = ResourceLocation.parse(value);
            Item item = BuiltInRegistries.ITEM.get(location);
            if (item == null || item == Items.AIR) {
                context.warning("Item not found: {}", value);
            }
        } catch (Exception e) {
            context.error("Invalid item ID: {}", value);
        }
    }
}