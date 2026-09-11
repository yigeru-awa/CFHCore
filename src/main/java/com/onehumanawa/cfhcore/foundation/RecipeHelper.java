package com.onehumanawa.cfhcore.foundation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onehumanawa.cfhcore.CFHCore;
import com.onehumanawa.cfhcore.foundation.data.DatagenSupport;
import com.onehumanawa.cfhcore.foundation.ingredient.FluidSpec;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeFilter;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeInfo;
import com.onehumanawa.cfhcore.foundation.recipe.filter.RecipeFilterBuilder;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchemaRegistry;

import java.util.Map;
import java.util.function.Predicate;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class RecipeHelper {

    private static boolean initialized = false;

    private RecipeHelper() {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        registerSchemas();

        CFHCore.LOGGER.info("RecipeHelper initialized with {} registered schemas",
                RecipeSchemaRegistry.registeredTypes().size());
    }

    private static void registerSchemas() {
        int discovered = RecipeSchemaRegistry.autoDiscover();
        CFHCore.LOGGER.info("Auto-discovered {} recipe schemas via @RecipeSchemaType", discovered);
    }

    public static FluidSpec fluidOf(String id, int amount) {
        return FluidSpec.of(id, amount);
    }

    public static FluidSpec fluidTagOf(String tagId, int amount) {
        return FluidSpec.tag(tagId, amount);
    }

    public static String itemOf(String id) {
        return id;
    }

    public static String itemOf(String id, String dataComponents) {
        return id + "{" + dataComponents + "}";
    }

    public static String itemTagOf(String tagId) {
        return "#" + (tagId.contains(":") ? tagId : "minecraft:" + tagId);
    }

    public static void addCreateMixing(String id, String[] itemInputs, FluidSpec[] fluidInputs,
                                       String[] itemOutputs, FluidSpec[] fluidOutputs) {
        addCreateMixing(id, itemInputs, fluidInputs, itemOutputs, fluidOutputs, -1, null);
    }

    public static void addCreateMixing(String id, String[] itemInputs, FluidSpec[] fluidInputs,
                                       String[] itemOutputs, FluidSpec[] fluidOutputs,
                                       int processingTime, String heatRequirement) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "create:mixing");

        JsonArray ingredients = new JsonArray();
        for (String spec : itemInputs) {
            if (spec != null && !spec.isEmpty()) {
                ingredients.add(ingredient(spec));
            }
        }
        for (FluidSpec fluid : fluidInputs) {
            if (fluid != null && !fluid.isEmpty()) {
                ingredients.add(fluid.toJson());
            }
        }
        json.add("ingredients", ingredients);

        JsonArray results = new JsonArray();
        for (String spec : itemOutputs) {
            if (spec != null && !spec.isEmpty()) {
                results.add(itemStack(spec, 1));
            }
        }
        for (FluidSpec fluid : fluidOutputs) {
            if (fluid != null && !fluid.isEmpty()) {
                results.add(fluid.toResultJson());
            }
        }
        json.add("results", results);

        if (processingTime >= 0) json.addProperty("processing_time", processingTime);
        if (heatRequirement != null && !heatRequirement.isEmpty()) {
            json.addProperty("heat_requirement", heatRequirement);
        }

        addRecipe(id, json);
    }

    public static void addCreateMixing(String id, String[] itemInputs, String[] itemOutputs) {
        addCreateMixing(id, itemInputs, new FluidSpec[0], itemOutputs, new FluidSpec[0]);
    }

    public static void addCreateMixing(String id, String[] itemInputs, String[] itemOutputs,
                                       int processingTime, String heatRequirement) {
        addCreateMixing(id, itemInputs, new FluidSpec[0], itemOutputs, new FluidSpec[0],
                processingTime, heatRequirement);
    }
    public static void addCreateProcessing(String type, String id, String[] itemInputs, FluidSpec[] fluidInputs,
                                           String[] itemOutputs, FluidSpec[] fluidOutputs) {
        addCreateProcessing(type, id, itemInputs, fluidInputs, itemOutputs, fluidOutputs, -1, null);
    }

    public static void addCreateProcessing(String type, String id, String[] itemInputs, FluidSpec[] fluidInputs,
                                           String[] itemOutputs, FluidSpec[] fluidOutputs,
                                           int processingTime, String heatRequirement) {
        JsonObject json = new JsonObject();
        json.addProperty("type", type);

        JsonArray ingredients = new JsonArray();
        for (String spec : itemInputs) {
            if (spec != null && !spec.isEmpty()) {
                ingredients.add(ingredient(spec));
            }
        }
        for (FluidSpec fluid : fluidInputs) {
            if (fluid != null && !fluid.isEmpty()) {
                ingredients.add(fluid.toJson());
            }
        }
        json.add("ingredients", ingredients);

        JsonArray results = new JsonArray();
        for (String spec : itemOutputs) {
            if (spec != null && !spec.isEmpty()) {
                JsonObject result = new JsonObject();
                result.addProperty("item", spec);
                results.add(result);
            }
        }
        for (FluidSpec fluid : fluidOutputs) {
            if (fluid != null && !fluid.isEmpty()) {
                results.add(fluid.toResultJson());
            }
        }
        json.add("results", results);

        if (processingTime >= 0) json.addProperty("processing_time", processingTime);
        if (heatRequirement != null && !heatRequirement.isEmpty()) {
            json.addProperty("heat_requirement", heatRequirement);
        }

        addRecipe(id, json);
    }

    public static void addCreateProcessing(String type, String id, String[] itemInputs, String[] itemOutputs) {
        addCreateProcessing(type, id, itemInputs, new FluidSpec[0], itemOutputs, new FluidSpec[0]);
    }

    public static void addCreateFilling(String id, String itemInput, FluidSpec fluidInput, String itemOutput) {
        addCreateProcessing("create:filling", id, new String[]{itemInput}, new FluidSpec[]{fluidInput},
                new String[]{itemOutput}, new FluidSpec[0]);
    }

    public static void addCreateEmptying(String id, String itemInput, String itemOutput, FluidSpec fluidOutput) {
        addCreateProcessing("create:emptying", id, new String[]{itemInput}, new FluidSpec[0],
                new String[]{itemOutput}, new FluidSpec[]{fluidOutput});
    }

    public static void addCreateCompacting(String id, String[] itemInputs, FluidSpec[] fluidInputs,
                                           String[] itemOutputs, FluidSpec[] fluidOutputs) {
        addCreateProcessing("create:compacting", id, itemInputs, fluidInputs, itemOutputs, fluidOutputs);
    }

    public static void addCreateCompacting(String id, String[] itemInputs, String[] itemOutputs) {
        addCreateProcessing("create:compacting", id, itemInputs, new FluidSpec[0], itemOutputs, new FluidSpec[0]);
    }

    public static void addCreatePressing(String id, String itemInput, String itemOutput) {
        addCreateProcessing("create:pressing", id, new String[]{itemInput}, new FluidSpec[0],
                new String[]{itemOutput}, new FluidSpec[0]);
    }

    public static void addCreateDeploying(String id, String itemInput, String toolInput, String itemOutput) {
        addCreateDeploying(id, itemInput, toolInput, itemOutput, 1);
    }

    public static void addCreateDeploying(String id, String itemInput, String toolInput, String itemOutput, int count) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "create:deploying");

        JsonArray ingredients = new JsonArray();
        ingredients.add(ingredient(itemInput));
        ingredients.add(ingredient(toolInput));
        json.add("ingredients", ingredients);

        JsonArray results = new JsonArray();
        results.add(itemStack(itemOutput, count));
        json.add("results", results);

        addRecipe(id, json);
    }

    public static void addCreateDeploying(String id, String[] itemInputs, String[] itemOutputs) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "create:deploying");

        JsonArray ingredients = new JsonArray();
        for (String spec : itemInputs) {
            if (spec != null && !spec.isEmpty()) {
                ingredients.add(ingredient(spec));
            }
        }
        json.add("ingredients", ingredients);

        JsonArray results = new JsonArray();
        for (String spec : itemOutputs) {
            if (spec != null && !spec.isEmpty()) {
                results.add(itemStack(spec, 1));
            }
        }
        json.add("results", results);

        addRecipe(id, json);
    }

    public static void addCreateMilling(String id, String itemInput, String itemOutput) {
        addCreateProcessing("create:milling", id, new String[]{itemInput}, new FluidSpec[0],
                new String[]{itemOutput}, new FluidSpec[0]);
    }

    public static void addCreateCrushing(String id, String itemInput, String[] itemOutputs) {
        addCreateProcessing("create:crushing", id, new String[]{itemInput}, new FluidSpec[0],
                itemOutputs, new FluidSpec[0]);
    }

    public static void addCreateSplashing(String id, String itemInput, String[] itemOutputs) {
        addCreateProcessing("create:splashing", id, new String[]{itemInput}, new FluidSpec[0],
                itemOutputs, new FluidSpec[0]);
    }

    public static void addCreateHaunting(String id, String itemInput, String itemOutput) {
        addCreateProcessing("create:haunting", id, new String[]{itemInput}, new FluidSpec[0],
                new String[]{itemOutput}, new FluidSpec[0]);
    }

    public static void addCreateCutting(String id, String itemInput, String itemOutput) {
        addCreateCutting(id, itemInput, new String[]{itemOutput}, -1);
    }

    public static void addCreateCutting(String id, String itemInput, String[] itemOutputs) {
        addCreateCutting(id, itemInput, itemOutputs, -1);
    }

    public static void addCreateCutting(String id, String itemInput, String[] itemOutputs, int processingTime) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "create:cutting");

        JsonArray ingredients = new JsonArray();
        ingredients.add(ingredient(itemInput));
        json.add("ingredients", ingredients);

        JsonArray results = new JsonArray();
        for (String spec : itemOutputs) {
            if (spec != null && !spec.isEmpty()) {
                results.add(itemStack(spec, 1));
            }
        }
        json.add("results", results);

        if (processingTime >= 0) json.addProperty("processing_time", processingTime);

        addRecipe(id, json);
    }

    public static void addCreateSandpaperPolishing(String id, String itemInput, String itemOutput) {
        addCreateSandpaperPolishing(id, itemInput, itemOutput, 1);
    }

    public static void addCreateSandpaperPolishing(String id, String itemInput, String itemOutput, int count) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "create:sandpaper_polishing");

        JsonArray ingredients = new JsonArray();
        ingredients.add(ingredient(itemInput));
        json.add("ingredients", ingredients);

        JsonArray results = new JsonArray();
        results.add(itemStack(itemOutput, count));
        json.add("results", results);

        addRecipe(id, json);
    }

    public static void addShaped(String id, String[] pattern, Map<String, String> key, String result) {
        addShaped(id, pattern, key, result, 1);
    }

    public static void addShaped(String id, String[] pattern, Map<String, String> key, String result, int count) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:crafting_shaped");

        JsonArray patternArray = new JsonArray();
        for (String row : pattern) patternArray.add(row);
        json.add("pattern", patternArray);

        JsonObject keyObject = new JsonObject();
        key.forEach((symbol, spec) -> keyObject.add(symbol, ingredient(spec)));
        json.add("key", keyObject);

        json.add("result", itemStack(result, count));
        addRecipe(id, json);
    }

    public static void addShapeless(String id, String[] ingredients, String result) {
        addShapeless(id, ingredients, result, 1);
    }

    public static void addShapeless(String id, String[] ingredients, String result, int count) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:crafting_shapeless");

        JsonArray array = new JsonArray();
        for (String spec : ingredients) {
            if (spec != null && !spec.isEmpty()) {
                array.add(ingredient(spec));
            }
        }
        json.add("ingredients", array);
        json.add("result", itemStack(result, count));

        addRecipe(id, json);
    }

    public static void addSmelting(String id, String input, String result) {
        addSmelting(id, input, result, 0.35F, 200);
    }

    public static void addSmelting(String id, String input, String result, float experience, int cookingTime) {
        addCooking("minecraft:smelting", id, input, result, experience, cookingTime);
    }

    public static void addBlasting(String id, String input, String result) {
        addBlasting(id, input, result, 1.0F, 100);
    }

    public static void addBlasting(String id, String input, String result, float experience, int cookingTime) {
        addCooking("minecraft:blasting", id, input, result, experience, cookingTime);
    }

    public static void addSmoking(String id, String input, String result) {
        addSmoking(id, input, result, 0.35F, 100);
    }

    public static void addSmoking(String id, String input, String result, float experience, int cookingTime) {
        addCooking("minecraft:smoking", id, input, result, experience, cookingTime);
    }

    public static void addCampfireCooking(String id, String input, String result) {
        addCampfireCooking(id, input, result, 0.35F, 600);
    }

    public static void addCampfireCooking(String id, String input, String result, float experience, int cookingTime) {
        addCooking("minecraft:campfire_cooking", id, input, result, experience, cookingTime);
    }

    private static void addCooking(String type, String id, String input, String result, float experience, int cookingTime) {
        JsonObject json = new JsonObject();
        json.addProperty("type", type);
        json.add("ingredient", ingredient(input));
        json.add("result", itemStack(result, 1));
        json.addProperty("experience", experience);
        json.addProperty("cookingtime", cookingTime);
        addRecipe(id, json);
    }

    public static void addStonecutting(String id, String input, String result) {
        addStonecutting(id, input, result, 1);
    }

    public static void addStonecutting(String id, String input, String result, int count) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:stonecutting");
        json.add("ingredient", ingredient(input));
        json.add("result", itemStack(result, count));
        addRecipe(id, json);
    }

    public static void addMechanicalCrafting(String id, String[] pattern, Map<String, String> key, String result) {
        addMechanicalCrafting(id, pattern, key, result, 1);
    }

    public static void addMechanicalCrafting(String id, String[] pattern, Map<String, String> key, String result, int count) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "create:mechanical_crafting");

        JsonArray patternArray = new JsonArray();
        for (String row : pattern) patternArray.add(row);
        json.add("pattern", patternArray);

        JsonObject keyObject = new JsonObject();
        key.forEach((symbol, spec) -> keyObject.add(symbol, ingredient(spec)));
        json.add("key", keyObject);

        json.add("result", itemStack(result, count));
        addRecipe(id, json);
    }

    public static void addCreateItemApplication(String id, String input, String appliedItem, String result) {
        addCreateItemApplication(id, input, appliedItem, result, 1);
    }

    public static void addCreateItemApplication(String id, String input, String appliedItem, String result, int count) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "create:item_application");

        JsonArray ingredients = new JsonArray();
        ingredients.add(ingredient(input));
        ingredients.add(ingredient(appliedItem));
        json.add("ingredients", ingredients);

        JsonArray results = new JsonArray();
        results.add(itemStack(result, count));
        json.add("results", results);
        addRecipe(id, json);
    }

    public static void addCreateItemApplication(String id, String input, String appliedItem, String[] results) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "create:item_application");

        JsonArray ingredients = new JsonArray();
        ingredients.add(ingredient(input));
        ingredients.add(ingredient(appliedItem));
        json.add("ingredients", ingredients);

        JsonArray resultArray = new JsonArray();
        for (String spec : results) {
            if (spec != null && !spec.isEmpty()) {
                resultArray.add(itemStack(spec, 1));
            }
        }
        json.add("results", resultArray);
        addRecipe(id, json);
    }

    public static void addCreateSequencedAssembly(String id, String ingredient, String transitionalItem,
                                                  int loops, String[] results, JsonObject... sequence) {
        JsonObject[] weighted = new JsonObject[results.length];
        for (int i = 0; i < results.length; i++) {
            weighted[i] = assemblyResult(results[i], 1F);
        }
        addCreateSequencedAssembly(id, ingredient, transitionalItem, loops, weighted, sequence);
    }

    public static void addCreateSequencedAssembly(String id, String ingredient, String transitionalItem,
                                                  String[] results, JsonObject... sequence) {
        addCreateSequencedAssembly(id, ingredient, transitionalItem, 1, results, sequence);
    }

    public static void addCreateSequencedAssembly(String id, String ingredient, String transitionalItem,
                                                  int loops, JsonObject[] weightedResults, JsonObject[] sequence) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "create:sequenced_assembly");
        json.add("ingredient", ingredient(ingredient));
        json.add("transitional_item", itemStack(transitionalItem, 1));

        JsonArray sequenceArray = new JsonArray();
        for (JsonObject step : sequence) {
            if (step != null) {
                sequenceArray.add(step);
            }
        }
        json.add("sequence", sequenceArray);

        JsonArray resultsArray = new JsonArray();
        for (JsonObject result : weightedResults) {
            if (result != null) {
                resultsArray.add(result);
            }
        }
        json.add("results", resultsArray);

        json.addProperty("loops", loops);
        addRecipe(id, json);
    }

    public static JsonObject assemblyStep(String type, String transitionalItem, String[] extraIngredients) {
        return assemblyStep(type, transitionalItem, extraIngredients, new String[]{transitionalItem});
    }

    public static JsonObject assemblyStep(String type, String transitionalItem, String[] extraIngredients,
                                          String[] stepResults) {
        JsonObject json = new JsonObject();
        json.addProperty("type", type);

        JsonArray ingredients = new JsonArray();
        ingredients.add(ingredient(transitionalItem));
        for (String spec : extraIngredients) {
            if (spec != null && !spec.isEmpty()) {
                ingredients.add(ingredient(spec));
            }
        }
        json.add("ingredients", ingredients);

        JsonArray results = new JsonArray();
        for (String spec : stepResults) {
            if (spec != null && !spec.isEmpty()) {
                results.add(itemStack(spec, 1));
            }
        }
        json.add("results", results);
        return json;
    }

    public static JsonObject assemblyResult(String item, float weight) {
        JsonObject json = new JsonObject();
        json.addProperty("id", item);
        if (weight != 1F) {
            json.addProperty("chance", weight);
        }
        return json;
    }
    public static void addRecipe(String id, JsonObject json) {
        DatagenSupport.addRecipe(id, json);
    }

    public static void addRecipe(String id, String jsonText) {
        addRecipe(id, JsonParser.parseString(jsonText).getAsJsonObject());
    }

    private static JsonObject ingredient(String spec) {
        JsonObject json = new JsonObject();
        if (spec.startsWith("#")) {
            json.addProperty("tag", spec.substring(1));
        } else {
            json.addProperty("item", spec);
        }
        return json;
    }

    private static JsonObject itemStack(String item, int count) {
        JsonObject json = new JsonObject();
        json.addProperty("id", item);
        if (count > 1) json.addProperty("count", count);
        return json;
    }

    public static RecipeFilter all() {
        return RecipeFilter.all();
    }
    public static RecipeFilter byId(String id) {
        return RecipeFilter.byId(id);
    }
    public static RecipeFilter byId(String id, boolean blacklist) {
        return RecipeFilter.byId(id, blacklist);
    }
    public static RecipeFilter byIds(String... ids) {
        return RecipeFilter.byIds(ids);
    }
    public static RecipeFilter byMod(String modId) {
        return RecipeFilter.byMod(modId);
    }
    public static RecipeFilter byMod(String modId, boolean blacklist) {
        return RecipeFilter.byMod(modId, blacklist);
    }
    public static RecipeFilter byMods(String... modIds) {
        return RecipeFilter.byMods(modIds);
    }
    public static RecipeFilter byType(String typeId) {
        return RecipeFilter.byType(typeId);
    }
    public static RecipeFilter byType(String typeId, boolean blacklist) {
        return RecipeFilter.byType(typeId, blacklist);
    }
    public static RecipeFilter byTypes(String... typeIds) {
        return RecipeFilter.byTypes(typeIds);
    }
    public static RecipeFilter byCustom(Predicate<RecipeInfo> predicate) {
        return RecipeFilter.byCustom(predicate);
    }
    public static RecipeFilterBuilder builder() {
        return RecipeFilterBuilder.builder();
    }

    public static String[] arr(String... input) {
        return input;
    }

    public static Map<String, String> map(String... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("Map pairs must be even length");
        }
        java.util.Map<String, String> result = new java.util.LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            result.put(pairs[i], pairs[i + 1]);
        }
        return result;
    }
}