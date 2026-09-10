package com.onehumanawa.cfhcore.foundation.recipe.schema.create;

import com.onehumanawa.cfhcore.foundation.ingredient.FluidSpec;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeValidationContext;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.FluidComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.IntegerComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.StringComponent;
import com.onehumanawa.cfhcore.foundation.recipe.match.ReplacementMatch;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchema;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchemaType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Schema for Create mixing recipes.
 */
@RecipeSchemaType("create:mixing")
public class MixingSchema {

    public static final RecipeSchema INSTANCE = create();

    // ==================== Public Data Classes ====================

    public static class MixingIngredients {
        public List<String> items = new ArrayList<>();
        public List<FluidSpec> fluids = new ArrayList<>();

        public boolean isEmpty() {
            return items.isEmpty() && fluids.isEmpty();
        }
    }

    public static class MixingResults {
        public List<String> items = new ArrayList<>();
        public List<FluidSpec> fluids = new ArrayList<>();

        public boolean isEmpty() {
            return items.isEmpty() && fluids.isEmpty();
        }
    }

    // ==================== Component Implementations ====================

    /**
     * Component for mixing ingredients (items + fluids)
     */
    public static class MixingIngredientComponent implements RecipeComponent<MixingIngredients> {

        @Override
        public MixingIngredients read(com.google.gson.JsonObject json, com.onehumanawa.cfhcore.foundation.recipe.RecipeContext context) {
            MixingIngredients result = new MixingIngredients();

            if (!json.has("ingredients")) return result;

            var array = json.get("ingredients").getAsJsonArray();
            for (var element : array) {
                if (element.isJsonObject()) {
                    var obj = element.getAsJsonObject();

                    // Check if it's a fluid
                    if (obj.has("fluid")) {
                        result.fluids.add(FluidComponent.INSTANCE.read(obj, context));
                        continue;
                    }

                    // Check if it's a fluid tag
                    if (obj.has("fluidTag")) {
                        result.fluids.add(FluidSpec.of(
                                obj.get("fluidTag").getAsString(),
                                obj.has("amount") ? obj.get("amount").getAsInt() : 1000
                        ));
                        continue;
                    }

                    // It's an item
                    if (obj.has("item")) {
                        result.items.add(obj.get("item").getAsString());
                    } else if (obj.has("tag")) {
                        result.items.add("#" + obj.get("tag").getAsString());
                    }
                }
            }

            return result;
        }

        @Override
        public void write(com.google.gson.JsonObject json, MixingIngredients value, com.onehumanawa.cfhcore.foundation.recipe.RecipeContext context) {
            if (value == null || value.isEmpty()) return;

            var array = new com.google.gson.JsonArray();

            // Write items
            for (String item : value.items) {
                var obj = new com.google.gson.JsonObject();
                if (item.startsWith("#")) {
                    obj.addProperty("tag", item.substring(1));
                } else {
                    obj.addProperty("item", item);
                }
                array.add(obj);
            }

            // Write fluids
            for (FluidSpec fluid : value.fluids) {
                if (fluid == null || fluid.isEmpty()) continue;
                var obj = new com.google.gson.JsonObject();
                if (fluid.isTag()) {
                    obj.addProperty("fluidTag", fluid.id());
                    obj.addProperty("amount", fluid.amount());
                } else {
                    var fluidObj = new com.google.gson.JsonObject();
                    fluidObj.addProperty("id", fluid.id());
                    fluidObj.addProperty("amount", fluid.amount());
                    obj.add("fluid", fluidObj);
                }
                array.add(obj);
            }

            json.add("ingredients", array);
        }

        @Override
        public boolean matches(MixingIngredients value, ReplacementMatch match) {
            if (value == null) return false;
            for (String item : value.items) {
                if (match.matches(item)) return true;
            }
            return false;
        }

        @Override
        public boolean replace(MixingIngredients value, ReplacementMatch match, Object replacement,
                               com.onehumanawa.cfhcore.foundation.recipe.RecipeContext context) {
            if (value == null) return false;

            boolean replaced = false;
            List<String> newItems = new ArrayList<>();

            for (String item : value.items) {
                if (match.matches(item) && replacement instanceof String replacementStr) {
                    newItems.add(replacementStr);
                    replaced = true;
                } else {
                    newItems.add(item);
                }
            }

            if (replaced) {
                value.items = newItems;
                context.recipe().setValue(RecipeKey.builder(this).name("ingredients").build(), value);
            }

            return replaced;
        }

        @Override
        public MixingIngredients defaultValue() {
            return new MixingIngredients();
        }

        @Override
        public void validate(MixingIngredients value, RecipeValidationContext context) {
            if (value == null) {
                context.error("Mixing ingredients cannot be null");
                return;
            }
            if (value.isEmpty()) {
                context.warning("Mixing ingredients are empty");
            }
        }
    }

    /**
     * Component for mixing results (items + fluids)
     */
    public static class MixingResultComponent implements RecipeComponent<MixingResults> {

        @Override
        public MixingResults read(com.google.gson.JsonObject json, com.onehumanawa.cfhcore.foundation.recipe.RecipeContext context) {
            MixingResults result = new MixingResults();

            if (!json.has("results")) return result;

            var array = json.get("results").getAsJsonArray();
            for (var element : array) {
                if (element.isJsonObject()) {
                    var obj = element.getAsJsonObject();

                    // Check if it's a fluid
                    if (obj.has("fluid")) {
                        result.fluids.add(FluidComponent.INSTANCE.read(obj, context));
                        continue;
                    }

                    if (obj.has("fluidTag")) {
                        result.fluids.add(FluidSpec.of(
                                obj.get("fluidTag").getAsString(),
                                obj.has("amount") ? obj.get("amount").getAsInt() : 1000
                        ));
                        continue;
                    }

                    // It's an item
                    if (obj.has("item")) {
                        result.items.add(obj.get("item").getAsString());
                    }
                }
            }

            return result;
        }

        @Override
        public void write(com.google.gson.JsonObject json, MixingResults value, com.onehumanawa.cfhcore.foundation.recipe.RecipeContext context) {
            if (value == null || value.isEmpty()) return;

            var array = new com.google.gson.JsonArray();

            // Write items
            for (String item : value.items) {
                var obj = new com.google.gson.JsonObject();
                obj.addProperty("item", item);
                array.add(obj);
            }

            // Write fluids
            for (FluidSpec fluid : value.fluids) {
                if (fluid == null || fluid.isEmpty()) continue;
                var obj = new com.google.gson.JsonObject();
                if (fluid.isTag()) {
                    obj.addProperty("fluidTag", fluid.id());
                    obj.addProperty("amount", fluid.amount());
                } else {
                    var fluidObj = new com.google.gson.JsonObject();
                    fluidObj.addProperty("id", fluid.id());
                    fluidObj.addProperty("amount", fluid.amount());
                    obj.add("fluid", fluidObj);
                }
                array.add(obj);
            }

            json.add("results", array);
        }

        @Override
        public boolean matches(MixingResults value, ReplacementMatch match) {
            if (value == null) return false;
            for (String item : value.items) {
                if (match.matches(item)) return true;
            }
            return false;
        }

        @Override
        public boolean replace(MixingResults value, ReplacementMatch match, Object replacement,
                               com.onehumanawa.cfhcore.foundation.recipe.RecipeContext context) {
            if (value == null) return false;

            boolean replaced = false;
            List<String> newItems = new ArrayList<>();

            for (String item : value.items) {
                if (match.matches(item) && replacement instanceof String replacementStr) {
                    newItems.add(replacementStr);
                    replaced = true;
                } else {
                    newItems.add(item);
                }
            }

            if (replaced) {
                value.items = newItems;
                context.recipe().setValue(RecipeKey.builder(this).name("results").build(), value);
            }

            return replaced;
        }

        @Override
        public MixingResults defaultValue() {
            return new MixingResults();
        }

        @Override
        public void validate(MixingResults value, RecipeValidationContext context) {
            if (value == null) {
                context.error("Mixing results cannot be null");
                return;
            }
            if (value.isEmpty()) {
                context.warning("Mixing results are empty");
            }
        }
    }

    // ==================== Schema Creation ====================

    private static RecipeSchema create() {
        return RecipeSchema.builder()
                .type("create:mixing")
                .key(RecipeKey.builder(new MixingIngredientComponent())
                        .name("ingredients")
                        .input()
                        .alwaysWrite()
                        .build())
                .key(RecipeKey.builder(new MixingResultComponent())
                        .name("results")
                        .output()
                        .alwaysWrite()
                        .build())
                .key(RecipeKey.builder(new IntegerComponent("processing_time", 1, 36000, 100))
                        .name("processing_time")
                        .optional()
                        .defaultValue(100)
                        .build())
                .key(RecipeKey.builder(new StringComponent("heat_requirement", "none", Set.of("none", "heated", "superheated")))
                        .name("heat_requirement")
                        .optional()
                        .defaultValue("none")
                        .build())
                .postProcessor((ctx, recipe) -> {
                    MixingIngredients ingredients =
                            recipe.getValue(RecipeKey.builder(new MixingIngredientComponent()).name("ingredients").build());
                    MixingResults results =
                            recipe.getValue(RecipeKey.builder(new MixingResultComponent()).name("results").build());

                    if (ingredients == null || ingredients.isEmpty()) {
                        ctx.error("Mixing recipe has no ingredients");
                    }

                    if (results == null || results.isEmpty()) {
                        ctx.error("Mixing recipe has no results");
                    }
                })
                .idGenerator(recipe -> {
                    MixingIngredients ingredients =
                            recipe.getValue(RecipeKey.builder(new MixingIngredientComponent()).name("ingredients").build());
                    MixingResults results =
                            recipe.getValue(RecipeKey.builder(new MixingResultComponent()).name("results").build());

                    StringBuilder sb = new StringBuilder("mixing");
                    if (ingredients != null) {
                        for (String item : ingredients.items) {
                            sb.append("_").append(item.replace(':', '_'));
                        }
                    }
                    if (results != null) {
                        for (String item : results.items) {
                            sb.append("_to_").append(item.replace(':', '_'));
                        }
                    }
                    return sb.toString();
                })
                .build();
    }
}