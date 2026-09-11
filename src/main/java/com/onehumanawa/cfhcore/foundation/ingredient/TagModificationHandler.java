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
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

import com.onehumanawa.cfhcore.CFHCore;

@EventBusSubscriber(modid = CFHCore.ID)
public class TagModificationHandler {
    private static final RegistryOps<Item> ITEM_OPS = new RegistryOps<>(Registries.ITEM, "Item");
    private static final RegistryOps<Block> BLOCK_OPS = new RegistryOps<>(Registries.BLOCK, "Block");

    public static void addItemAdditions(TagKey<Item> tag, Collection<ResourceLocation> itemIds) {
        ITEM_OPS.addAdditions(tag, itemIds);
    }

    public static void addItemRemovals(TagKey<Item> tag, Collection<ResourceLocation> itemIds) {
        ITEM_OPS.addRemovals(tag, itemIds);
    }

    public static void addBlockAdditions(TagKey<Block> tag, Collection<ResourceLocation> blockIds) {
        BLOCK_OPS.addAdditions(tag, blockIds);
    }

    public static void addBlockRemovals(TagKey<Block> tag, Collection<ResourceLocation> blockIds) {
        BLOCK_OPS.addRemovals(tag, blockIds);
    }

    static void addAdditions(TagKey<Item> tag, Collection<ResourceLocation> itemIds) {
        addItemAdditions(tag, itemIds);
    }

    static void addRemovals(TagKey<Item> tag, Collection<ResourceLocation> itemIds) {
        addItemRemovals(tag, itemIds);
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD)
            return;
        RegistryAccess access = event.getRegistryAccess();
        ITEM_OPS.apply(access);
        BLOCK_OPS.apply(access);
    }

    private static final class RegistryOps<T> {
        private final ResourceKey<? extends Registry<T>> registryKey;
        private final String displayName;
        private final Map<TagKey<T>, Set<ResourceLocation>> additions = new HashMap<>();
        private final Map<TagKey<T>, Set<ResourceLocation>> removals = new HashMap<>();

        RegistryOps(ResourceKey<? extends Registry<T>> registryKey, String displayName) {
            this.registryKey = registryKey;
            this.displayName = displayName;
        }

        void addAdditions(TagKey<T> tag, Collection<ResourceLocation> ids) {
            additions.computeIfAbsent(tag, key -> new LinkedHashSet<>()).addAll(ids);
        }

        void addRemovals(TagKey<T> tag, Collection<ResourceLocation> ids) {
            removals.computeIfAbsent(tag, key -> new LinkedHashSet<>()).addAll(ids);
        }

        void apply(RegistryAccess access) {
            if (additions.isEmpty() && removals.isEmpty())
                return;
            Registry<T> registry = access.registryOrThrow(this.registryKey);

            Map<TagKey<T>, List<Holder<T>>> tags = new HashMap<>();
            registry.getTags().forEach(pair ->
                    tags.put(pair.getFirst(), new ArrayList<>(pair.getSecond().stream().toList())));

            int addedCount = 0;
            for (Map.Entry<TagKey<T>, Set<ResourceLocation>> entry : additions.entrySet()) {
                List<Holder<T>> contents = tags.computeIfAbsent(entry.getKey(), key -> new ArrayList<>());
                Set<Holder<T>> existing = new HashSet<>(contents);
                for (ResourceLocation id : entry.getValue()) {
                    Optional<Holder.Reference<T>> holder = registry.getHolder(id);
                    if (holder.isEmpty()) {
                        CFHCore.LOGGER.warn("[Tag] Cannot add unknown {} {} to {}", displayName, id, entry.getKey().location());
                    } else if (existing.add(holder.get())) {
                        contents.add(holder.get());
                        addedCount++;
                    }
                }
            }

            int removedCount = 0;
            for (Map.Entry<TagKey<T>, Set<ResourceLocation>> entry : removals.entrySet()) {
                List<Holder<T>> contents = tags.get(entry.getKey());
                if (contents == null)
                    continue;
                for (ResourceLocation id : entry.getValue()) {
                    Optional<Holder.Reference<T>> holder = registry.getHolder(id);
                    if (holder.isPresent() && contents.remove(holder.get()))
                        removedCount++;
                }
            }

            if (addedCount == 0 && removedCount == 0)
                return;

            registry.bindTags(tags);
            CFHCore.LOGGER.info("[Tag] {} tags updated: {} added, {} removed", displayName, addedCount, removedCount);
        }
    }
}
