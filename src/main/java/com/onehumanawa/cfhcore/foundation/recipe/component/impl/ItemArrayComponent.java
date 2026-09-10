package com.onehumanawa.cfhcore.foundation.recipe.component.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeContext;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeValidationContext;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;
import com.onehumanawa.cfhcore.foundation.recipe.match.ReplacementMatch;

import java.util.ArrayList;
import java.util.List;

/**
 * Component for arrays of item values in recipes.
 */
public class ItemArrayComponent implements RecipeComponent<String[]> {

    public static final ItemArrayComponent INSTANCE = new ItemArrayComponent();

    @Override
    public String[] read(JsonObject json, RecipeContext context) {
        if (!json.has("ingredients") && !json.has("items")) {
            return new String[0];
        }

        JsonArray array = json.has("ingredients") ? json.get("ingredients").getAsJsonArray() : json.get("items").getAsJsonArray();
        List<String> items = new ArrayList<>();

        for (int i = 0; i < array.size(); i++) {
            var element = array.get(i);
            if (element.isJsonPrimitive()) {
                items.add(element.getAsString());
            } else if (element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();
                if (obj.has("item")) {
                    items.add(obj.get("item").getAsString());
                } else if (obj.has("tag")) {
                    items.add("#" + obj.get("tag").getAsString());
                } else if (obj.has("id")) {
                    items.add(obj.get("id").getAsString());
                }
            }
        }

        return items.toArray(new String[0]);
    }

    @Override
    public void write(JsonObject json, String[] value, RecipeContext context) {
        if (value == null || value.length == 0) return;

        JsonArray array = new JsonArray();
        for (String item : value) {
            if (item.startsWith("#")) {
                JsonObject obj = new JsonObject();
                obj.addProperty("tag", item.substring(1));
                array.add(obj);
            } else {
                JsonObject obj = new JsonObject();
                obj.addProperty("item", item);
                array.add(obj);
            }
        }

        json.add("ingredients", array);
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
        List<String> newItems = new ArrayList<>();

        for (String item : value) {
            if (match.matches(item) && replacement instanceof String replacementStr) {
                newItems.add(replacementStr);
                replaced = true;
            } else {
                newItems.add(item);
            }
        }

        if (replaced) {
            context.recipe().setValue(RecipeKey.builder(INSTANCE).name("ingredients").build(), newItems.toArray(new String[0]));
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
            context.push("[" + i + "]");
            if (value[i] == null || value[i].isEmpty()) {
                context.error("Item cannot be null or empty");
            }
            context.pop();
        }
    }
}