package com.onehumanawa.cfhcore.foundation.modification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.onehumanawa.cfhcore.CFHCore;

import it.unimi.dsi.fastutil.objects.ObjectSortedSet;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;

public final class ModificationHandler {
    private static final List<ItemModifier> ITEM_MODIFIERS = new ArrayList<>();
    private static final List<ItemModifier> ITEM_TAG_MODIFIERS = new ArrayList<>();
    private static final List<BlockModifier> BLOCK_MODIFIERS = new ArrayList<>();
    private static final List<BlockModifier> BLOCK_TAG_MODIFIERS = new ArrayList<>();
    private static final Map<ResourceLocation, Integer> BURN_TIMES = new HashMap<>();
    private static final Map<ResourceLocation, Integer> BURN_TIME_TAGS = new HashMap<>();
    private static final Set<ResourceLocation> HIDDEN = new LinkedHashSet<>();
    private static final Set<ResourceLocation> HIDDEN_TAGS = new LinkedHashSet<>();
    private static final Set<ResourceLocation> DISABLED = new LinkedHashSet<>();
    private static final Set<ResourceLocation> DISABLED_TAGS = new LinkedHashSet<>();
    private static final Set<ResourceLocation> DISABLED_APPLIED = new HashSet<>();
    private static boolean registered;

    private ModificationHandler() {}

    public static void register(IEventBus modEventBus) {
        if (registered) {
            return;
        }
        registered = true;
        modEventBus.addListener(ModificationHandler::onCommonSetup);
        modEventBus.addListener(ModificationHandler::onModifyDefaultComponents);
        modEventBus.addListener(ModificationHandler::onBuildCreativeModeTabContents);
        NeoForge.EVENT_BUS.addListener(ModificationHandler::onFurnaceFuelBurnTime);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, TagsUpdatedEvent.class,
                ModificationHandler::onTagsUpdatedEarly);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, TagsUpdatedEvent.class,
                ModificationHandler::onTagsUpdatedLate);
        SuppressionHandler.register();
    }

    static void addItemModifier(ItemModifier modifier) {
        ITEM_MODIFIERS.add(modifier);
    }

    static void addItemTagModifier(ItemModifier modifier) {
        ITEM_TAG_MODIFIERS.add(modifier);
    }

    static void addBlockModifier(BlockModifier modifier) {
        BLOCK_MODIFIERS.add(modifier);
    }

    static void addBlockTagModifier(BlockModifier modifier) {
        BLOCK_TAG_MODIFIERS.add(modifier);
    }

    static void setBurnTime(ResourceLocation id, int burnTime) {
        BURN_TIMES.put(id, burnTime);
    }

    static void setBurnTimeTag(ResourceLocation tagId, int burnTime) {
        BURN_TIME_TAGS.put(tagId, burnTime);
    }

    public static void hide(ResourceLocation id) {
        HIDDEN.add(id);
    }

    public static void hideTag(ResourceLocation tagId) {
        HIDDEN_TAGS.add(tagId);
    }

    public static void disable(ResourceLocation id) {
        DISABLED.add(id);
        HIDDEN.add(id);
    }

    public static void disableTag(ResourceLocation tagId) {
        DISABLED_TAGS.add(tagId);
        HIDDEN_TAGS.add(tagId);
    }

    static String overrideKey(ResourceLocation id) {
        return CFHCore.ID + ".override." + id.getNamespace() + "." + id.getPath();
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        int blocks = 0;
        for (BlockModifier modifier : BLOCK_MODIFIERS) {
            Block block = resolveBlock(modifier.id());
            if (block == null) {
                CFHCore.LOGGER.warn("[Modification] Cannot modify unknown block {}", modifier.target());
                continue;
            }
            modifier.applyTo(block);
            blocks++;
        }

        int items = 0;
        for (ItemModifier modifier : ITEM_MODIFIERS) {
            Item item = resolveItem(modifier.id());
            if (item == null) {
                CFHCore.LOGGER.warn("[Modification] Cannot modify unknown item {}", modifier.target());
                continue;
            }
            modifier.applyFields(item);
            items++;
        }

        int disabled = applyDisabled();
        for (ResourceLocation id : HIDDEN) {
            if (resolveItem(id) == null && resolveBlock(id) == null) {
                CFHCore.LOGGER.warn("[Modification] Cannot hide unknown entry {}", id);
            }
        }

        if (blocks != 0 || items != 0 || disabled != 0 || !HIDDEN.isEmpty()) {
            CFHCore.LOGGER.info("[Modification] {} blocks and {} items modified, {} disabled, {} hidden",
                    blocks, items, disabled, HIDDEN.size());
        }
    }

    private static void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        for (ItemModifier modifier : ITEM_MODIFIERS) {
            Item item = resolveItem(modifier.id());
            if (item != null) {
                modifier.applyComponents(event, item);
            }
        }
    }

    private static void onTagsUpdatedEarly(TagsUpdatedEvent event) {
        if (!event.shouldUpdateStaticData()) {
            return;
        }
        if (hasTagTargets()) {
            expandTagSets(event.getRegistryAccess());
        }
        applyDisabled();
    }

    private static void onTagsUpdatedLate(TagsUpdatedEvent event) {
        if (!event.shouldUpdateStaticData()) {
            return;
        }

        RegistryAccess access = event.getRegistryAccess();
        int expanded = hasTagTargets() ? expandTagSets(access) : 0;
        int disabled = applyDisabled();
        int itemTargets = 0;
        int blockTargets = 0;

        if (!ITEM_TAG_MODIFIERS.isEmpty()) {
            Registry<Item> items = access.registryOrThrow(Registries.ITEM);
            for (ItemModifier modifier : ITEM_TAG_MODIFIERS) {
                List<Item> members = tagValues(items, modifier.tag().location());
                if (members.isEmpty()) {
                    CFHCore.LOGGER.warn("[Modification] Cannot modify unknown or empty item tag {}", modifier.target());
                    continue;
                }
                members.forEach(modifier::applyLate);
                itemTargets += members.size();
            }
        }
        if (!BLOCK_TAG_MODIFIERS.isEmpty()) {
            Registry<Block> blocks = access.registryOrThrow(Registries.BLOCK);
            for (BlockModifier modifier : BLOCK_TAG_MODIFIERS) {
                List<Block> members = tagValues(blocks, modifier.tag().location());
                if (members.isEmpty()) {
                    CFHCore.LOGGER.warn("[Modification] Cannot modify unknown or empty block tag {}", modifier.target());
                    continue;
                }
                members.forEach(modifier::applyTo);
                blockTargets += members.size();
            }
        }

        SuppressionHandler.stripRecipes(access);

        if (expanded != 0 || disabled != 0 || itemTargets != 0 || blockTargets != 0) {
            CFHCore.LOGGER.info("[Modification] Tag pass ({}): {} items and {} blocks modified, {} entries expanded, {} disabled",
                    event.getUpdateCause(), itemTargets, blockTargets, expanded, disabled);
        }
    }

    private static boolean hasTagTargets() {
        return !ITEM_TAG_MODIFIERS.isEmpty() || !BLOCK_TAG_MODIFIERS.isEmpty()
                || !HIDDEN_TAGS.isEmpty() || !DISABLED_TAGS.isEmpty() || !BURN_TIME_TAGS.isEmpty();
    }

    private static int expandTagSets(RegistryAccess access) {
        Registry<Item> items = access.registryOrThrow(Registries.ITEM);
        Registry<Block> blocks = access.registryOrThrow(Registries.BLOCK);
        int expanded = 0;
        for (ResourceLocation tagId : HIDDEN_TAGS) {
            expanded += expandTag(items, tagId, HIDDEN);
            expanded += expandTag(blocks, tagId, HIDDEN);
        }
        for (ResourceLocation tagId : DISABLED_TAGS) {
            expanded += expandTag(items, tagId, DISABLED);
            expanded += expandTag(blocks, tagId, DISABLED);
        }
        for (Map.Entry<ResourceLocation, Integer> entry : BURN_TIME_TAGS.entrySet()) {
            for (ResourceLocation id : tagIds(items, entry.getKey())) {
                BURN_TIMES.put(id, entry.getValue());
            }
        }
        return expanded;
    }

    private static void onBuildCreativeModeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (HIDDEN.isEmpty()) {
            return;
        }
        List<ItemStack> removals = new ArrayList<>();
        collectHidden(event.getParentEntries(), removals);
        collectHidden(event.getSearchEntries(), removals);
        for (ItemStack stack : removals) {
            event.remove(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }

    private static void onFurnaceFuelBurnTime(FurnaceFuelBurnTimeEvent event) {
        if (BURN_TIMES.isEmpty()) {
            return;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        Integer burnTime = id == null ? null : BURN_TIMES.get(id);
        if (burnTime != null) {
            event.setBurnTime(burnTime);
        }
    }

    private static void collectHidden(ObjectSortedSet<ItemStack> entries, List<ItemStack> removals) {
        for (ItemStack stack : entries) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id != null && HIDDEN.contains(id)) {
                removals.add(stack);
            }
        }
    }

    private static int applyDisabled() {
        int count = 0;
        for (ResourceLocation id : DISABLED) {
            if (!DISABLED_APPLIED.add(id)) {
                continue;
            }
            boolean found = false;
            Item item = resolveItem(id);
            if (item != null) {
                disable(item);
                found = true;
            }
            Block block = resolveBlock(id);
            if (block != null) {
                disable(block);
                found = true;
            }
            if (found) {
                count++;
            } else {
                CFHCore.LOGGER.warn("[Modification] Cannot disable unknown entry {}", id);
            }
        }
        return count;
    }

    static boolean hasSuppressed() {
        return !DISABLED.isEmpty();
    }

    static boolean isSuppressed(@Nullable Item item) {
        if (item == null) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id != null && DISABLED.contains(id);
    }

    private static void disable(FeatureElement element) {
        FeatureFlagSet required = element.requiredFeatures().join(FeatureFlagSet.of(FeatureFlags.BUNDLE));
        Reflections.set(element, "requiredFeatures", required);
        if (element instanceof Block block) {
            Reflections.set(block.properties(), "requiredFeatures", required);
        }
    }

    private static <T> int expandTag(Registry<T> registry, ResourceLocation tagId, Set<ResourceLocation> target) {
        int count = 0;
        for (ResourceLocation id : tagIds(registry, tagId)) {
            if (target.add(id)) {
                count++;
            }
        }
        return count;
    }

    private static <T> List<ResourceLocation> tagIds(Registry<T> registry, ResourceLocation tagId) {
        List<ResourceLocation> ids = new ArrayList<>();
        for (T value : tagValues(registry, tagId)) {
            ResourceLocation id = registry.getKey(value);
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    private static <T> List<T> tagValues(Registry<T> registry, ResourceLocation tagId) {
        Optional<HolderSet.Named<T>> holders = registry.getTag(TagKey.create(registry.key(), tagId));
        if (holders.isEmpty()) {
            return List.of();
        }
        List<T> values = new ArrayList<>();
        for (Holder<T> holder : holders.get()) {
            values.add(holder.value());
        }
        return values;
    }

    @Nullable
    private static Item resolveItem(@Nullable ResourceLocation id) {
        return id == null ? null : BuiltInRegistries.ITEM.getHolder(id).map(Holder.Reference::value).orElse(null);
    }

    @Nullable
    private static Block resolveBlock(@Nullable ResourceLocation id) {
        return id == null ? null : BuiltInRegistries.BLOCK.getHolder(id).map(Holder.Reference::value).orElse(null);
    }
}
