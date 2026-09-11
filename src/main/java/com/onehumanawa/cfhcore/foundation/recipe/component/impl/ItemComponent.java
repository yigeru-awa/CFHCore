package com.onehumanawa.cfhcore.foundation.recipe.component.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeContext;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeValidationContext;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeComponent;
import com.onehumanawa.cfhcore.foundation.recipe.match.ReplacementMatch;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Optional;

public class ItemComponent implements RecipeComponent<String> {

    public static final ItemComponent INSTANCE = new ItemComponent();

    @Override
    public String read(JsonObject json, RecipeContext context) {
        return readId(locate(json, context));
    }

    @Override
    public void write(JsonObject json, String value, RecipeContext context) {
        if (value == null) return;

        boolean useId = useId(context);
        String field = targetField(json, context);
        if (field == null) {
            applyTo(json, value, useId);
            return;
        }

        JsonElement existing = json.get(field);
        if (existing != null && existing.isJsonArray()) {
            JsonArray array = existing.getAsJsonArray();
            if (array.isEmpty()) {
                array.add(createEntry(value, useId));
            } else {
                JsonElement first = array.get(0);
                if (first != null && first.isJsonPrimitive()) {
                    array.set(0, new JsonPrimitive(value));
                } else {
                    JsonObject target = firstObject(first);
                    if (target != null) {
                        applyTo(target, value, useId);
                    } else {
                        array.set(0, createEntry(value, useId));
                    }
                }
            }
            json.add(field, array);
            return;
        }

        if (existing != null && existing.isJsonObject()) {
            applyTo(existing.getAsJsonObject(), value, useId);
            return;
        }

        if (existing != null && existing.isJsonPrimitive()) {
            json.addProperty(field, value);
            return;
        }

        json.add(field, createEntry(value, useId));
    }

    @Override
    public boolean matches(String value, ReplacementMatch match) {
        if (value == null) return false;
        return match.matches(value);
    }

    @Override
    public boolean replace(String value, ReplacementMatch match, Object replacement, RecipeContext context) {
        if (value == null || !matches(value, match)) return false;
        String replacementId = toId(replacement);
        if (replacementId == null) return false;
        if (context != null) {
            context.setValue(replacementId);
        }
        return true;
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
            validateTag(value.substring(1), context);
            return;
        }

        try {
            ResourceLocation location = ResourceLocation.parse(value);
            if (!BuiltInRegistries.ITEM.containsKey(location)) {
                context.warning("Item not found: {}", value);
            }
        } catch (Exception e) {
            context.error("Invalid item ID: {}", value);
        }
    }

    private static void validateTag(String tagId, RecipeValidationContext context) {
        try {
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, ResourceLocation.parse(tagId));
            Optional<HolderSet.Named<Item>> tag = BuiltInRegistries.ITEM.getTag(tagKey);
            if (tag.isEmpty()) {
                context.warning("Unknown item tag: {}", tagId);
            } else if (tag.get().size() == 0) {
                context.warning("Item tag is empty: {}", tagId);
            }
        } catch (Exception e) {
            context.error("Invalid item tag ID: {}", tagId);
        }
    }

    @Nullable
    public static String readId(@Nullable JsonElement element) {
        if (element == null || element.isJsonNull()) return null;
        if (element.isJsonPrimitive()) return element.getAsString();
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            return array.isEmpty() ? null : readId(array.get(0));
        }
        JsonObject object = element.getAsJsonObject();
        if (object.has("item")) return object.get("item").getAsString();
        if (object.has("id")) return object.get("id").getAsString();
        if (object.has("tag")) return "#" + object.get("tag").getAsString();
        return null;
    }

    @Nullable
    public static String toId(@Nullable Object replacement) {
        if (replacement instanceof String id) {
            return id.isEmpty() ? null : id;
        }
        if (replacement instanceof ItemStack stack) {
            return stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        }
        if (replacement instanceof Item item) {
            return BuiltInRegistries.ITEM.getKey(item).toString();
        }
        if (replacement instanceof ResourceLocation location) {
            return location.toString();
        }
        return null;
    }

    public static JsonObject createEntry(String value, boolean useId) {
        JsonObject object = new JsonObject();
        applyTo(object, value, useId);
        return object;
    }

    public static void applyTo(JsonObject object, String value, boolean useId) {
        if (value.startsWith("#")) {
            object.remove("item");
            object.remove("id");
            object.addProperty("tag", value.substring(1));
            return;
        }

        object.remove("tag");
        if (useId) {
            object.remove("item");
            object.addProperty("id", value);
            return;
        }
        if (!object.has("item") && object.has("id")) {
            object.addProperty("id", value);
            return;
        }
        object.addProperty("item", value);
    }

    @Nullable
    public static JsonObject firstObject(@Nullable JsonElement element) {
        if (element == null || element.isJsonNull()) return null;
        if (element.isJsonObject()) return element.getAsJsonObject();
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            return array.isEmpty() ? null : firstObject(array.get(0));
        }
        return null;
    }
}
