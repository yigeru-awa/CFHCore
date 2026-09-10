package com.onehumanawa.cfhcore.foundation.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.onehumanawa.cfhcore.CFHCore;
import com.onehumanawa.cfhcore.foundation.data.DatagenSupport;
import com.onehumanawa.cfhcore.foundation.RecipeHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

/**
 * Engine for modpack-wide recipe control.
 */
@SuppressWarnings("unused")
@EventBusSubscriber(modid = CFHCore.ID)
public class RecipeModificationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("KubeJavaRecipe");

    private static final List<RecipeFilter> FILTERS = new ArrayList<>();
    private static final List<RecipeFilter> COMPLETED_FILTERS = new ArrayList<>();

    /** Create sequenced assembly recipes do not support input/output replacement. */
    private static final ResourceLocation SEQUENCED_ASSEMBLY_TYPE = ResourceLocation.parse("create:sequenced_assembly");

    private static RecipeManager recipeManager;
    private static ResourceManager resourceManager;

    static {
        RecipeHelper.init();
    }

    static void register(RecipeFilter filter) {
        FILTERS.add(filter);
        LOGGER.debug("Registered filter: {}", filter);
    }

    static void complete(RecipeFilter filter) {
        if (FILTERS.remove(filter)) {
            COMPLETED_FILTERS.add(filter);
            LOGGER.debug("Filter completed: {}", filter);
        } else {
            throw new IllegalArgumentException("Filter not registered: " + filter);
        }
    }

    // ================================================================
    // Public Query API
    // ================================================================

    public static Optional<RecipeHolder<?>> getRecipeById(String recipeId) {
        return recipeManager == null ? Optional.empty() : recipeManager.byKey(parseId(recipeId));
    }

    public static Optional<JsonObject> getRecipeJson(String recipeId) {
        if (resourceManager == null) return Optional.empty();
        return readRecipeJson(parseId(recipeId), resourceManager);
    }

    public static boolean isCompleted(RecipeFilter filter) {
        return COMPLETED_FILTERS.contains(filter);
    }

    public static int getActiveFilterCount() {
        return FILTERS.size();
    }

    public static int getCompletedFilterCount() {
        return COMPLETED_FILTERS.size();
    }

    // ================================================================
    // Reload Handling
    // ================================================================

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new SimplePreparableReloadListener<Void>() {
            @Override
            protected @org.jetbrains.annotations.NotNull Void prepare(
                    ResourceManager resourceManager, ProfilerFiller profiler) {
                return null;
            }

            @Override
            protected void apply(Void data, ResourceManager resourceManager, ProfilerFiller profiler) {
                profiler.push("cfhcore:modify_recipes");
                processRecipes(event.getServerResources().getRecipeManager(), resourceManager,
                        event.getRegistryAccess());
                profiler.pop();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static void processRecipes(RecipeManager manager, ResourceManager resources,
                                       HolderLookup.Provider registries) {
        recipeManager = manager;
        resourceManager = resources;

        Map<ResourceLocation, JsonObject> added = DatagenSupport.getAddedRecipes();

        // ===== Debug Logging =====
        CFHCore.LOGGER.info("=== Recipe Processing Debug ===");
        CFHCore.LOGGER.info("Active filters: {}", FILTERS.size());
        CFHCore.LOGGER.info("Completed filters: {}", COMPLETED_FILTERS.size());
        CFHCore.LOGGER.info("Added recipes: {}", added.size());
        for (Map.Entry<ResourceLocation, JsonObject> entry : added.entrySet()) {
            CFHCore.LOGGER.info("  - To add: {} -> {}", entry.getKey(), entry.getValue());
        }
        // ==========================

        if (FILTERS.isEmpty() && COMPLETED_FILTERS.isEmpty() && added.isEmpty()) {
            CFHCore.LOGGER.info("No recipe changes to apply");
            return;
        }

        RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);

        var typeLookup = registries.lookupOrThrow(Registries.RECIPE_TYPE);
        Map<RecipeType<?>, ResourceLocation> typeIds = new HashMap<>();
        typeLookup.listElements().forEach(holder ->
                typeIds.put(holder.value(), holder.key().location()));

        Map<ResourceLocation, RecipeHolder<?>> result = new LinkedHashMap<>();
        int removedCount = 0;
        int modifiedCount = 0;

        List<RecipeFilter> allFilters = new ArrayList<>();
        allFilters.addAll(FILTERS);
        allFilters.addAll(COMPLETED_FILTERS);

        CFHCore.LOGGER.info("Total recipes in manager: {}", manager.getRecipes().size());

        for (RecipeHolder<?> holder : manager.getRecipes()) {
            RecipeType<?> recipeType = holder.value().getType();
            ResourceLocation typeId = typeIds.get(recipeType);

            RecipeInfo info = new RecipeInfo(holder.id(), holder.id().getNamespace(), typeId);

            List<RecipeFilter> matched = new ArrayList<>();
            for (RecipeFilter filter : allFilters) {
                if (filter.matches(info)) {
                    matched.add(filter);
                }
            }

            if (!matched.isEmpty()) {
                CFHCore.LOGGER.debug("Recipe {} matched by {} filters", holder.id(), matched.size());
            }

            boolean removed = matched.stream().anyMatch(RecipeFilter::shouldRemove);

            if (removed) {
                removedCount++;
                CFHCore.LOGGER.info("Removed recipe: {}", holder.id());
                continue;
            }

            if (matched.isEmpty()) {
                result.put(holder.id(), holder);
                continue;
            }

            Map<String, String> inputs = new HashMap<>();
            Map<String, String> outputs = new HashMap<>();
            List<Consumer<JsonObject>> transformers = new ArrayList<>();

            for (RecipeFilter filter : matched) {
                filter.collect(inputs, outputs, transformers);
            }

            if (SEQUENCED_ASSEMBLY_TYPE.equals(typeId) && (!inputs.isEmpty() || !outputs.isEmpty())) {
                CFHCore.LOGGER.warn("Recipe {} is a Create sequenced assembly recipe; " +
                        "input/output replacement is not supported for this type and will be skipped", holder.id());
                inputs.clear();
                outputs.clear();
            }

            CFHCore.LOGGER.debug("Applying modifications to: {}", holder.id());
            CFHCore.LOGGER.debug("  Input replacements: {}", inputs);
            CFHCore.LOGGER.debug("  Output replacements: {}", outputs);

            RecipeHolder<?> modified = tryModify(holder, resources, ops, inputs, outputs, transformers);
            if (modified != holder) {
                modifiedCount++;
                CFHCore.LOGGER.info("Modified recipe: {}", holder.id());
            }
            result.put(modified.id(), modified);
        }

        // Add new recipes
        int addedCount = 0;
        for (Map.Entry<ResourceLocation, JsonObject> entry : added.entrySet()) {
            try {
                CFHCore.LOGGER.debug("Parsing added recipe: {} -> {}", entry.getKey(), entry.getValue());

                if (!entry.getValue().has("type")) {
                    CFHCore.LOGGER.warn("Added recipe {} has no 'type' field", entry.getKey());
                    continue;
                }

                var recipeResult = Recipe.CODEC.parse(ops, entry.getValue());
                if (recipeResult.result().isPresent()) {
                    Recipe<?> recipe = recipeResult.result().get();
                    result.put(entry.getKey(), new RecipeHolder<>(entry.getKey(), recipe));
                    addedCount++;
                    CFHCore.LOGGER.info("Successfully added recipe: {}", entry.getKey());
                } else {
                    CFHCore.LOGGER.warn("Failed to parse added recipe: {}", entry.getKey());
                    if (recipeResult.error().isPresent()) {
                        CFHCore.LOGGER.warn("Parse error: {}", recipeResult.error().get().message());
                    }
                }
            } catch (Exception e) {
                CFHCore.LOGGER.error("Failed to add recipe: {}", entry.getKey(), e);
            }
        }

        manager.replaceRecipes(result.values());
        CFHCore.LOGGER.info("Recipe processing done: {} removed, {} modified, {} added",
                removedCount, modifiedCount, addedCount);
    }

    // ================================================================
    // Recipe Modification
    // ================================================================

    private static RecipeHolder<?> tryModify(RecipeHolder<?> holder, ResourceManager resources,
                                             RegistryOps<JsonElement> ops,
                                             Map<String, String> inputReplacements,
                                             Map<String, String> outputReplacements,
                                             List<Consumer<JsonObject>> transformers) {
        if (inputReplacements.isEmpty() && outputReplacements.isEmpty() && transformers.isEmpty()) {
            return holder;
        }

        Optional<JsonObject> jsonOpt = readRecipeJson(holder.id(), resources);
        if (jsonOpt.isEmpty()) {
            CFHCore.LOGGER.warn("Could not read JSON of {}, leaving unchanged", holder.id());
            return holder;
        }

        JsonObject json = jsonOpt.get();
        boolean changed = false;

        if (!inputReplacements.isEmpty()) {
            changed |= applyReplacements(json, "ingredient", inputReplacements, true);
            changed |= applyReplacements(json, "ingredients", inputReplacements, true);
            changed |= applyReplacements(json, "key", inputReplacements, true);
        }

        if (!outputReplacements.isEmpty()) {
            changed |= applyReplacements(json, "result", outputReplacements, false);
            changed |= applyReplacements(json, "results", outputReplacements, false);
        }

        for (Consumer<JsonObject> transformer : transformers) {
            transformer.accept(json);
            changed = true;
        }

        if (!changed) {
            return holder;
        }

        try {
            var recipeResult = Recipe.CODEC.parse(ops, json);
            if (recipeResult.result().isPresent()) {
                Recipe<?> recipe = recipeResult.result().get();
                return new RecipeHolder<>(holder.id(), recipe);
            }
        } catch (Exception e) {
            CFHCore.LOGGER.warn("Failed to re-parse modified JSON of {}, keeping original", holder.id(), e);
        }

        return holder;
    }

    // ================================================================
    // JSON Helpers
    // ================================================================

    private static boolean applyReplacements(JsonObject root, String field,
                                             Map<String, String> replacements, boolean input) {
        boolean changed = false;

        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            if (field.equals(entry.getKey())) {
                changed |= replaceItemIds(entry.getValue(), replacements, input);
            } else {
                changed |= applyReplacementsDeep(entry.getValue(), field, replacements, input);
            }
        }

        return changed;
    }

    private static boolean applyReplacementsDeep(JsonElement element, String field,
                                                 Map<String, String> replacements, boolean input) {
        boolean changed = false;

        if (element.isJsonObject()) {
            changed |= applyReplacements(element.getAsJsonObject(), field, replacements, input);
        } else if (element.isJsonArray()) {
            for (JsonElement e : element.getAsJsonArray()) {
                changed |= applyReplacementsDeep(e, field, replacements, input);
            }
        }

        return changed;
    }

    private static boolean replaceItemIds(JsonElement element, Map<String, String> replacements,
                                          boolean inputContext) {
        boolean changed = false;

        if (element.isJsonArray()) {
            var array = element.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                JsonElement e = array.get(i);
                if (e.isJsonPrimitive()) {
                    String mapped = replacements.get(e.getAsString());
                    if (mapped != null) {
                        array.set(i, new com.google.gson.JsonPrimitive(mapped));
                        changed = true;
                    }
                } else {
                    changed |= replaceItemIds(e, replacements, inputContext);
                }
            }
        } else if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();

            if (obj.has("item")) {
                String current = obj.get("item").getAsString();
                String mapped = replacements.get(current);
                if (mapped != null) {
                    obj.addProperty("item", mapped);
                    changed = true;
                }
            }

            if (obj.has("tag")) {
                String current = "#" + obj.get("tag").getAsString();
                String mapped = replacements.get(current);
                if (mapped != null) {
                    if (mapped.startsWith("#")) {
                        obj.addProperty("tag", mapped.substring(1));
                    } else {
                        obj.remove("tag");
                        obj.addProperty("item", mapped);
                    }
                    changed = true;
                }
            }

            if (obj.has("id")) {
                String current = obj.get("id").getAsString();
                String mapped = replacements.get(current);
                if (mapped != null) {
                    obj.addProperty("id", mapped);
                    changed = true;
                }
            }

            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                changed |= replaceItemIds(entry.getValue(), replacements, inputContext);
            }
        }

        return changed;
    }

    private static Optional<JsonObject> readRecipeJson(ResourceLocation id, ResourceManager resources) {
        try {
            String[] possiblePaths = {
                    "recipes/" + id.getPath() + ".json",
                    "recipe/" + id.getPath() + ".json",
                    id.getPath() + ".json",
                    "data/" + id.getNamespace() + "/recipes/" + id.getPath() + ".json",
                    "data/" + id.getNamespace() + "/recipe/" + id.getPath() + ".json"
            };

            for (String path : possiblePaths) {
                ResourceLocation location = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), path);
                Optional<Resource> resource = resources.getResource(location);
                if (resource.isPresent()) {
                    try (var reader = resource.get().openAsReader()) {
                        JsonElement element = JsonParser.parseReader(reader);
                        if (element.isJsonObject()) {
                            CFHCore.LOGGER.debug("Found recipe JSON at: {}", location);
                            return Optional.of(element.getAsJsonObject());
                        }
                    }
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            CFHCore.LOGGER.warn("Error reading JSON of {}", id, e);
            return Optional.empty();
        }
    }

    public static ResourceLocation parseId(String s) {
        String[] parts = s.split(":", 2);
        return parts.length == 2
                ? ResourceLocation.fromNamespaceAndPath(parts[0], parts[1])
                : ResourceLocation.withDefaultNamespace(s);
    }
}