package com.onehumanawa.cfhcore.foundation.ingredient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

import com.onehumanawa.cfhcore.CFHCore;

@EventBusSubscriber(modid = CFHCore.ID)
public class TagModificationHandler {
    private static final Map<TagKey<Item>, Set<ResourceLocation>> ADDITIONS = new HashMap<>();
    private static final Map<TagKey<Item>, Set<ResourceLocation>> REMOVALS = new HashMap<>();

    static void addAdditions(TagKey<Item> tag, Collection<ResourceLocation> itemIds) {
        ADDITIONS.computeIfAbsent(tag, key -> new LinkedHashSet<>()).addAll(itemIds);
    }

    static void addRemovals(TagKey<Item> tag, Collection<ResourceLocation> itemIds) {
        REMOVALS.computeIfAbsent(tag, key -> new LinkedHashSet<>()).addAll(itemIds);
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD)
            return;
        if (ADDITIONS.isEmpty() && REMOVALS.isEmpty())
            return;
        apply(event.getRegistryAccess());
    }

    private static void apply(RegistryAccess access) {
        Registry<Item> registry = access.registryOrThrow(Registries.ITEM);

        Map<TagKey<Item>, List<Holder<Item>>> tags = new HashMap<>();
        registry.getTags().forEach(pair ->
                tags.put(pair.getFirst(), new ArrayList<>(pair.getSecond().stream().toList())));

        int addedCount = 0;
        for (Map.Entry<TagKey<Item>, Set<ResourceLocation>> entry : ADDITIONS.entrySet()) {
            List<Holder<Item>> contents = tags.computeIfAbsent(entry.getKey(), key -> new ArrayList<>());
            Set<Holder<Item>> existing = new HashSet<>(contents);
            for (ResourceLocation id : entry.getValue()) {
                Optional<Holder.Reference<Item>> holder = registry.getHolder(id);
                if (holder.isEmpty()) {
                    CFHCore.LOGGER.warn("[Tag] Cannot add unknown item {} to {}", id, entry.getKey().location());
                } else if (existing.add(holder.get())) {
                    contents.add(holder.get());
                    addedCount++;
                }
            }
        }

        int removedCount = 0;
        for (Map.Entry<TagKey<Item>, Set<ResourceLocation>> entry : REMOVALS.entrySet()) {
            List<Holder<Item>> contents = tags.get(entry.getKey());
            if (contents == null)
                continue;
            for (ResourceLocation id : entry.getValue()) {
                Optional<Holder.Reference<Item>> holder = registry.getHolder(id);
                if (holder.isPresent() && contents.remove(holder.get()))
                    removedCount++;
            }
        }

        if (addedCount == 0 && removedCount == 0)
            return;

        registry.bindTags(tags);
        CFHCore.LOGGER.info("[Tag] Item tags updated: {} added, {} removed", addedCount, removedCount);
    }
}
