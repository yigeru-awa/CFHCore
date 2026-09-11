package com.onehumanawa.cfhcore.foundation.recipe.schema.create;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeContext;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeValidationContext;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.ItemComponent;
import com.onehumanawa.cfhcore.foundation.recipe.match.ReplacementMatch;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchema;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchemaType;

import java.util.LinkedHashMap;
import java.util.Map;

@RecipeSchemaType("create:mechanical_crafting")
public class MechanicalCraftingSchema {

    public static final PatternComponent PATTERN = new PatternComponent();
    public static final KeyComponent KEY = new KeyComponent();

    public static final RecipeKey<String[]> PATTERN_KEY =
            RecipeKey.builder(PATTERN).name("pattern").input().alwaysWrite().build();
    public static final RecipeKey<Map<String, String>> KEY_KEY =
            RecipeKey.builder(KEY).name("key").input().alwaysWrite().build();
    public static final RecipeKey<String> RESULT_KEY =
            RecipeKey.builder(ItemComponent.INSTANCE).name("result").output().alwaysWrite().build();

    public static final RecipeSchema INSTANCE = create();

    private static RecipeSchema create() {
        return RecipeSchema.builder()
                .type("create:mechanical_crafting")
                .key(PATTERN_KEY)
                .key(KEY_KEY)
                .key(RESULT_KEY)
                .postProcessor((ctx, recipe) -> {
                    String[] pattern = recipe.getValue(PATTERN_KEY);
                    Map<String, String> key = recipe.getValue(KEY_KEY);

                    if (pattern == null || pattern.length == 0) {
                        ctx.error("Mechanical crafting pattern is empty");
                        return;
                    }

                    if (key == null || key.isEmpty()) {
                        ctx.error("Mechanical crafting key is empty");
                        return;
                    }

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
                    String[] pattern = recipe.getValue(PATTERN_KEY);
                    String result = recipe.getValue(RESULT_KEY);
                    if (pattern != null && result != null) {
                        return "mech_" + String.join("_", pattern) + "_to_" + result.replace(':', '_');
                    }
                    return "";
                })
                .build();
    }

    public static class PatternComponent implements RecipeComponent<String[]> {

        @Override
        public String[] read(JsonObject json, RecipeContext context) {
            JsonElement element = locate(json, context);
            if (!element.isJsonArray()) {
                return new String[0];
            }
            JsonArray array = element.getAsJsonArray();
            String[] result = new String[array.size()];
            for (int i = 0; i < array.size(); i++) {
                result[i] = array.get(i).getAsString();
            }
            return result;
        }

        @Override
        public void write(JsonObject json, String[] value, RecipeContext context) {
            if (value == null || value.length == 0) return;
            JsonArray array = new JsonArray();
            for (String row : value) {
                array.add(row);
            }
            String field = targetField(json, context);
            json.add(field != null ? field : "pattern", array);
        }

        @Override
        public boolean matches(String[] value, ReplacementMatch match) {
            return false;
        }

        @Override
        public boolean replace(String[] value, ReplacementMatch match, Object replacement, RecipeContext context) {
            return false;
        }

        @Override
        public String[] defaultValue() {
            return new String[0];
        }

        @Override
        public void validate(String[] value, RecipeValidationContext context) {
            if (value == null) {
                context.error("Pattern cannot be null");
                return;
            }
            if (value.length == 0) {
                context.error("Pattern cannot be empty");
                return;
            }
            int width = -1;
            for (int i = 0; i < value.length; i++) {
                context.push(i);
                if (value[i] == null || value[i].isEmpty()) {
                    context.error("Pattern row cannot be null or empty");
                } else if (width < 0) {
                    width = value[i].length();
                } else if (value[i].length() != width) {
                    context.warning("Pattern row width {} differs from the first row width {}", value[i].length(), width);
                }
                context.pop();
            }
        }
    }

    public static class KeyComponent implements RecipeComponent<Map<String, String>> {

        @Override
        public Map<String, String> read(JsonObject json, RecipeContext context) {
            Map<String, String> result = new LinkedHashMap<>();
            JsonElement element = locate(json, context);
            if (!element.isJsonObject()) {
                return result;
            }
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                String id = ItemComponent.readId(entry.getValue());
                if (id != null) {
                    result.put(entry.getKey(), id);
                }
            }
            return result;
        }

        @Override
        public void write(JsonObject json, Map<String, String> value, RecipeContext context) {
            if (value == null || value.isEmpty()) return;
            JsonObject keyObject = new JsonObject();
            for (Map.Entry<String, String> entry : value.entrySet()) {
                String item = entry.getValue();
                JsonObject object = new JsonObject();
                if (item.startsWith("#")) {
                    object.addProperty("tag", item.substring(1));
                } else {
                    object.addProperty("item", item);
                }
                keyObject.add(entry.getKey(), object);
            }
            String field = targetField(json, context);
            json.add(field != null ? field : "key", keyObject);
        }

        @Override
        public boolean matches(Map<String, String> value, ReplacementMatch match) {
            if (value == null) return false;
            for (String item : value.values()) {
                if (match.matches(item)) return true;
            }
            return false;
        }

        @Override
        public boolean replace(Map<String, String> value, ReplacementMatch match, Object replacement, RecipeContext context) {
            if (value == null) return false;
            String replacementId = ItemComponent.toId(replacement);
            if (replacementId == null) return false;

            boolean replaced = false;
            Map<String, String> updated = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : value.entrySet()) {
                if (match.matches(entry.getValue())) {
                    updated.put(entry.getKey(), replacementId);
                    replaced = true;
                } else {
                    updated.put(entry.getKey(), entry.getValue());
                }
            }

            if (replaced && context != null) {
                context.setValue(updated);
            }
            return replaced;
        }

        @Override
        public Map<String, String> defaultValue() {
            return new LinkedHashMap<>();
        }

        @Override
        public void validate(Map<String, String> value, RecipeValidationContext context) {
            if (value == null) {
                context.error("Key cannot be null");
                return;
            }
            for (Map.Entry<String, String> entry : value.entrySet()) {
                context.push(entry.getKey());
                if (entry.getKey().length() != 1) {
                    context.warning("Key symbol '{}' should be a single character", entry.getKey());
                }
                ItemComponent.INSTANCE.validate(entry.getValue(), context);
                context.pop();
            }
        }
    }
}
