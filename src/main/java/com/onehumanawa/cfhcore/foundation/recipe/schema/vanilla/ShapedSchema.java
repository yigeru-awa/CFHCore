package com.onehumanawa.cfhcore.foundation.recipe.schema.vanilla;

import com.onehumanawa.cfhcore.foundation.recipe.KubeJavaRecipe;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeValidationContext;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.ItemArrayComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.ItemComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.StringComponent;
import com.onehumanawa.cfhcore.foundation.recipe.match.ReplacementMatch;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchema;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchemaType;

import java.util.HashMap;
import java.util.Map;

/**
 * Schema for vanilla shaped crafting recipes.
 */
@RecipeSchemaType("minecraft:crafting_shaped")
public class ShapedSchema {

    public static final RecipeSchema INSTANCE = create();

    private static RecipeSchema create() {
        return RecipeSchema.builder()
                .type("minecraft:crafting_shaped")
                .key(RecipeKey.<String[]>builder(new PatternComponent())
                        .name("pattern")
                        .input()
                        .alwaysWrite()
                        .build())
                .key(RecipeKey.<Map<String, String>>builder(new KeyComponent())
                        .name("key")
                        .input()
                        .alwaysWrite()
                        .build())
                .key(RecipeKey.<String>builder(ItemComponent.INSTANCE)
                        .name("result")
                        .output()
                        .alwaysWrite()
                        .build())
                .key(RecipeKey.<String>builder(new StringComponent("group"))
                        .name("group")
                        .optional()
                        .build())
                .postProcessor((ctx, recipe) -> {
                    // Validate pattern and key match
                    String[] pattern = recipe.getValue(RecipeKey.builder(new PatternComponent()).name("pattern").build());
                    Map<String, String> key = recipe.getValue(RecipeKey.builder(new KeyComponent()).name("key").build());

                    if (pattern == null || pattern.length == 0) {
                        ctx.error("Shaped recipe pattern is empty");
                        return;
                    }

                    if (key == null || key.isEmpty()) {
                        ctx.error("Shaped recipe key is empty");
                        return;
                    }

                    // Check all symbols in pattern are present in key
                    for (String row : pattern) {
                        for (char c : row.toCharArray()) {
                            String symbol = String.valueOf(c);
                            if (!symbol.equals(" ") && !key.containsKey(symbol)) {
                                ctx.error("Symbol '{}' in pattern not defined in key", symbol);
                            }
                        }
                    }
                })
                .idGenerator(recipe -> {
                    // Generate a unique ID based on the recipe contents
                    String[] pattern = recipe.getValue(RecipeKey.builder(new PatternComponent()).name("pattern").build());
                    String result = recipe.getValue(RecipeKey.builder(ItemComponent.INSTANCE).name("result").build());
                    if (pattern != null && result != null) {
                        return String.join("_", pattern) + "_to_" + result.replace(':', '_');
                    }
                    return "";
                })
                .build();
    }

    // Custom component for pattern
    static class PatternComponent implements RecipeComponent<String[]> {
        @Override
        public String[] read(com.google.gson.JsonObject json, com.onehumanawa.cfhcore.foundation.recipe.RecipeContext context) {
            if (json.has("pattern")) {
                var array = json.get("pattern").getAsJsonArray();
                String[] result = new String[array.size()];
                for (int i = 0; i < array.size(); i++) {
                    result[i] = array.get(i).getAsString();
                }
                return result;
            }
            return new String[0];
        }

        @Override
        public void write(com.google.gson.JsonObject json, String[] value, com.onehumanawa.cfhcore.foundation.recipe.RecipeContext context) {
            if (value != null && value.length > 0) {
                var array = new com.google.gson.JsonArray();
                for (String row : value) {
                    array.add(row);
                }
                json.add("pattern", array);
            }
        }

        @Override
        public boolean matches(String[] value, ReplacementMatch match) { return false; }
        @Override
        public boolean replace(String[] value, ReplacementMatch match, Object replacement, com.onehumanawa.cfhcore.foundation.recipe.RecipeContext context) { return false; }
        @Override
        public String[] defaultValue() { return new String[0]; }
        @Override
        public void validate(String[] value, RecipeValidationContext context) {}
    }

    // Custom component for key mapping
    static class KeyComponent implements RecipeComponent<Map<String, String>> {
        @Override
        public Map<String, String> read(com.google.gson.JsonObject json, com.onehumanawa.cfhcore.foundation.recipe.RecipeContext context) {
            Map<String, String> result = new HashMap<>();
            if (json.has("key")) {
                var keyObj = json.get("key").getAsJsonObject();
                for (var entry : keyObj.entrySet()) {
                    var value = entry.getValue();
                    if (value.isJsonObject()) {
                        var obj = value.getAsJsonObject();
                        if (obj.has("item")) {
                            result.put(entry.getKey(), obj.get("item").getAsString());
                        } else if (obj.has("tag")) {
                            result.put(entry.getKey(), "#" + obj.get("tag").getAsString());
                        }
                    } else if (value.isJsonPrimitive()) {
                        result.put(entry.getKey(), value.getAsString());
                    }
                }
            }
            return result;
        }

        @Override
        public void write(com.google.gson.JsonObject json, Map<String, String> value, com.onehumanawa.cfhcore.foundation.recipe.RecipeContext context) {
            if (value != null && !value.isEmpty()) {
                var keyObj = new com.google.gson.JsonObject();
                for (var entry : value.entrySet()) {
                    String item = entry.getValue();
                    if (item.startsWith("#")) {
                        var obj = new com.google.gson.JsonObject();
                        obj.addProperty("tag", item.substring(1));
                        keyObj.add(entry.getKey(), obj);
                    } else {
                        var obj = new com.google.gson.JsonObject();
                        obj.addProperty("item", item);
                        keyObj.add(entry.getKey(), obj);
                    }
                }
                json.add("key", keyObj);
            }
        }

        @Override
        public boolean matches(Map<String, String> value, ReplacementMatch match) { return false; }
        @Override
        public boolean replace(Map<String, String> value, ReplacementMatch match, Object replacement, com.onehumanawa.cfhcore.foundation.recipe.RecipeContext context) { return false; }
        @Override
        public Map<String, String> defaultValue() { return new HashMap<>(); }
        @Override
        public void validate(Map<String, String> value, RecipeValidationContext context) {}
    }
}