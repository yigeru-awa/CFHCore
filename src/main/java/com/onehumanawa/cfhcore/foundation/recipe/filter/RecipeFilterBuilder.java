package com.onehumanawa.cfhcore.foundation.recipe.filter;

import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeFilter;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeInfo;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeModificationHandler;
import com.onehumanawa.cfhcore.foundation.recipe.match.ItemMatch;
import com.onehumanawa.cfhcore.foundation.recipe.match.ReplacementMatch;
import com.onehumanawa.cfhcore.foundation.recipe.match.TagMatch;
import com.onehumanawa.cfhcore.foundation.recipe.util.JsonUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

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
        return contains(RecipeFilter.INPUT_FIELDS, itemId);
    }

    public RecipeFilterBuilder hasOutput(String itemId) {
        return contains(RecipeFilter.OUTPUT_FIELDS, itemId);
    }

    private RecipeFilterBuilder contains(Collection<String> fields, String itemId) {
        ReplacementMatch match = itemId != null && itemId.startsWith("#")
                ? new TagMatch(itemId)
                : new ItemMatch(itemId);
        predicates.add(info -> {
            JsonObject json = RecipeModificationHandler.getRecipeJson(info.id()).orElse(null);
            if (json == null) {
                return false;
            }
            for (String id : JsonUtils.collectIds(json, fields)) {
                if (idMatches(id, match)) {
                    return true;
                }
            }
            return false;
        });
        return this;
    }

    private static boolean idMatches(String id, ReplacementMatch match) {
        if (match.matches(id)) {
            return true;
        }
        if (!id.startsWith("#") || !(match instanceof ItemMatch itemMatch)) {
            return false;
        }
        Item item = itemMatch.item();
        if (item == null || item == Items.AIR) {
            return false;
        }
        try {
            TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(id.substring(1)));
            return BuiltInRegistries.ITEM.wrapAsHolder(item).is(tag);
        } catch (Exception e) {
            return false;
        }
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