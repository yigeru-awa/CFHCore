package com.onehumanawa.cfhcore.foundation.recipe.component.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeContext;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeValidationContext;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeComponent;
import com.onehumanawa.cfhcore.foundation.recipe.match.ReplacementMatch;

import java.util.ArrayList;
import java.util.List;

public class ItemArrayComponent implements RecipeComponent<String[]> {

    public static final ItemArrayComponent INSTANCE = new ItemArrayComponent();

    @Override
    public String[] read(JsonObject json, RecipeContext context) {
        JsonElement element = locate(json, context);
        List<String> items = new ArrayList<>();
        if (element != null && element.isJsonArray()) {
            for (JsonElement entry : element.getAsJsonArray()) {
                String id = ItemComponent.readId(entry);
                if (id != null) {
                    items.add(id);
                }
            }
        } else {
            String id = ItemComponent.readId(element);
            if (id != null) {
                items.add(id);
            }
        }
        return items.toArray(new String[0]);
    }

    @Override
    public void write(JsonObject json, String[] value, RecipeContext context) {
        if (value == null || value.length == 0) return;

        boolean useId = useId(context);
        String field = targetField(json, context);
        if (field == null) {
            field = "ingredients";
        }

        JsonElement existing = json.get(field);
        if (existing != null && existing.isJsonArray()) {
            JsonArray array = existing.getAsJsonArray();
            if (array.size() == value.length) {
                for (int i = 0; i < value.length; i++) {
                    String item = value[i];
                    if (item == null || item.isEmpty()) continue;
                    JsonElement entry = array.get(i);
                    if (entry != null && entry.isJsonPrimitive()) {
                        array.set(i, new JsonPrimitive(item));
                        continue;
                    }
                    JsonObject target = ItemComponent.firstObject(entry);
                    if (target != null) {
                        ItemComponent.applyTo(target, item, useId);
                    } else {
                        array.set(i, ItemComponent.createEntry(item, useId));
                    }
                }
                return;
            }
        }

        JsonArray array = new JsonArray();
        for (String item : value) {
            if (item == null || item.isEmpty()) continue;
            array.add(ItemComponent.createEntry(item, useId));
        }
        if (array.isEmpty()) return;

        json.add(field, array);
    }

    @Override
    public boolean matches(String[] value, ReplacementMatch match) {
        if (value == null) return false;
        for (String item : value) {
            if (match.matches(item)) return true;
        }
        return false;
    }

    @Override
    public boolean replace(String[] value, ReplacementMatch match, Object replacement, RecipeContext context) {
        if (value == null) return false;

        boolean replaced = false;
        List<String> newItems = new ArrayList<>(value.length);

        for (String item : value) {
            String replacementId = match.matches(item) ? ItemComponent.toId(replacement) : null;
            if (replacementId != null) {
                newItems.add(replacementId);
                replaced = true;
            } else {
                newItems.add(item);
            }
        }

        if (replaced && context != null) {
            context.setValue(newItems.toArray(new String[0]));
        }

        return replaced;
    }

    @Override
    public String[] defaultValue() {
        return new String[0];
    }

    @Override
    public void validate(String[] value, RecipeValidationContext context) {
        if (value == null) {
            context.error("Item array cannot be null");
            return;
        }

        for (int i = 0; i < value.length; i++) {
            context.push(i);
            if (value[i] == null || value[i].isEmpty()) {
                context.error("Item cannot be null or empty");
            } else {
                ItemComponent.INSTANCE.validate(value[i], context);
            }
            context.pop();
        }
    }
}
