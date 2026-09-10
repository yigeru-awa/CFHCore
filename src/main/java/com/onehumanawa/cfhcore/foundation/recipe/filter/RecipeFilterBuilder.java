package com.onehumanawa.cfhcore.foundation.recipe.filter;

import com.onehumanawa.cfhcore.foundation.recipe.RecipeFilter;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeInfo;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeModificationHandler;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Builder for creating complex recipe filters with multiple conditions.
 */
public class RecipeFilterBuilder {

    private final List<Predicate<RecipeInfo>> predicates = new ArrayList<>();
    private final Set<ResourceLocation> includeIds = new HashSet<>();
    private final Set<ResourceLocation> excludeIds = new HashSet<>();
    private final Set<String> includeMods = new HashSet<>();
    private final Set<String> excludeMods = new HashSet<>();
    private final Set<ResourceLocation> includeTypes = new HashSet<>();
    private final Set<ResourceLocation> excludeTypes = new HashSet<>();

    public RecipeFilterBuilder includeId(String id) {
        includeIds.add(RecipeModificationHandler.parseId(id));
        return this;
    }

    public RecipeFilterBuilder includeId(ResourceLocation id) {
        includeIds.add(id);
        return this;
    }

    public RecipeFilterBuilder excludeId(String id) {
        excludeIds.add(RecipeModificationHandler.parseId(id));
        return this;
    }

    public RecipeFilterBuilder excludeId(ResourceLocation id) {
        excludeIds.add(id);
        return this;
    }

    public RecipeFilterBuilder includeMod(String modId) {
        includeMods.add(modId);
        return this;
    }

    public RecipeFilterBuilder excludeMod(String modId) {
        excludeMods.add(modId);
        return this;
    }

    public RecipeFilterBuilder includeType(String typeId) {
        includeTypes.add(RecipeModificationHandler.parseId(typeId));
        return this;
    }

    public RecipeFilterBuilder includeType(ResourceLocation typeId) {
        includeTypes.add(typeId);
        return this;
    }

    public RecipeFilterBuilder excludeType(String typeId) {
        excludeTypes.add(RecipeModificationHandler.parseId(typeId));
        return this;
    }

    public RecipeFilterBuilder excludeType(ResourceLocation typeId) {
        excludeTypes.add(typeId);
        return this;
    }

    public RecipeFilterBuilder custom(Predicate<RecipeInfo> predicate) {
        predicates.add(predicate);
        return this;
    }

    public RecipeFilterBuilder hasInput(String itemId) {
        predicates.add(info -> false); // Would check components
        return this;
    }

    public RecipeFilterBuilder hasOutput(String itemId) {
        predicates.add(info -> false); // Would check components
        return this;
    }

    public RecipeFilter build() {
        Predicate<RecipeInfo> finalPredicate = info -> true;

        if (!includeIds.isEmpty()) {
            finalPredicate = finalPredicate.and(info -> includeIds.contains(info.id()));
        }

        if (!excludeIds.isEmpty()) {
            finalPredicate = finalPredicate.and(info -> !excludeIds.contains(info.id()));
        }

        if (!includeMods.isEmpty()) {
            finalPredicate = finalPredicate.and(info -> includeMods.contains(info.modId()));
        }

        if (!excludeMods.isEmpty()) {
            finalPredicate = finalPredicate.and(info -> !excludeMods.contains(info.modId()));
        }

        if (!includeTypes.isEmpty()) {
            finalPredicate = finalPredicate.and(info -> info.typeId() != null && includeTypes.contains(info.typeId()));
        }

        if (!excludeTypes.isEmpty()) {
            finalPredicate = finalPredicate.and(info -> info.typeId() == null || !excludeTypes.contains(info.typeId()));
        }

        for (Predicate<RecipeInfo> predicate : predicates) {
            finalPredicate = finalPredicate.and(predicate);
        }

        return new RecipeFilter(finalPredicate);
    }

    public static RecipeFilterBuilder builder() {
        return new RecipeFilterBuilder();
    }
}