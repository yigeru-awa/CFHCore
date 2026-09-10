package com.onehumanawa.cfhcore.foundation.data;

import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.CFHCore;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DatagenSupport {
    private static final Map<ResourceLocation, JsonObject> RECIPES = new LinkedHashMap<>();

    public static void addRecipe(String id, JsonObject json) {
        ResourceLocation location = parse(id);
        RECIPES.put(location, json);
        CFHCore.LOGGER.info("Registered recipe for addition: {}", location);
    }

    public static Map<ResourceLocation, JsonObject> getAddedRecipes() {
        return RECIPES;
    }

    private static ResourceLocation parse(String id) {
        if (id.contains(":")) {
            return ResourceLocation.parse(id);
        }
        return ResourceLocation.fromNamespaceAndPath(CFHCore.ID, id);
    }

    public static void clear() {
        RECIPES.clear();
    }
}