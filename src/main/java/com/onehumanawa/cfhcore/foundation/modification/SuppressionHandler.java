package com.onehumanawa.cfhcore.foundation.modification;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.onehumanawa.cfhcore.CFHCore;
import com.onehumanawa.cfhcore.foundation.recipe.RecipeModificationHandler;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.CompositeEntryBase;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.TagEntry;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;

@SuppressWarnings({"unused", "unchecked"})
final class SuppressionHandler {
    private static final int SCAN_DEPTH = 3;

    private SuppressionHandler() {}

    static void register() {
        NeoForge.EVENT_BUS.addListener(SuppressionHandler::onLootTableLoad);
        NeoForge.EVENT_BUS.addListener(SuppressionHandler::onLivingDrops);
        NeoForge.EVENT_BUS.addListener(SuppressionHandler::onVillagerTrades);
        NeoForge.EVENT_BUS.addListener(SuppressionHandler::onWandererTrades);
    }

    static void stripRecipes(HolderLookup.Provider registries) {
        if (!ModificationHandler.hasSuppressed()) {
            return;
        }
        RecipeManager manager = RecipeModificationHandler.recipeManager();
        if (manager == null) {
            return;
        }
        Collection<RecipeHolder<?>> recipes = manager.getRecipes();
        List<RecipeHolder<?>> kept = new ArrayList<>(recipes.size());
        int removed = 0;
        for (RecipeHolder<?> holder : recipes) {
            if (mentionsSuppressed(holder.value(), registries)) {
                removed++;
                continue;
            }
            kept.add(holder);
        }
        if (removed != 0) {
            manager.replaceRecipes(kept);
            CFHCore.LOGGER.info("[Modification] {} recipes referencing disabled entries removed", removed);
        }
    }

    private static boolean mentionsSuppressed(Recipe<?> recipe, HolderLookup.Provider registries) {
        try {
            ItemStack result = recipe.getResultItem(registries);
            if (ModificationHandler.isSuppressed(result.getItem())) {
                return true;
            }
            for (Ingredient ingredient : recipe.getIngredients()) {
                for (ItemStack stack : ingredient.getItems()) {
                    if (ModificationHandler.isSuppressed(stack.getItem())) {
                        return true;
                    }
                }
            }
        } catch (RuntimeException | LinkageError e) {
            CFHCore.LOGGER.debug("[Modification] Cannot inspect recipe {}: {}", recipe.getClass().getName(), e.toString());
        }
        return false;
    }

    private static void onLootTableLoad(LootTableLoadEvent event) {
        if (!ModificationHandler.hasSuppressed()) {
            return;
        }
        LootTable table = event.getTable();
        if (table == null || !(Reflections.get(table, "pools") instanceof List<?> pools)) {
            return;
        }
        List<LootPool> kept = new ArrayList<>(pools.size());
        boolean changed = false;
        for (Object value : pools) {
            if (value instanceof LootPool pool && stripPool(pool)) {
                changed = true;
                continue;
            }
            kept.add((LootPool) value);
        }
        if (changed) {
            Reflections.set(table, "pools", kept);
            CFHCore.LOGGER.debug("[Modification] Stripped disabled entries from loot table {}", event.getName());
        }
    }

    private static boolean stripPool(LootPool pool) {
        if (!(Reflections.get(pool, "entries") instanceof List<?> entries)) {
            return false;
        }
        List<LootPoolEntryContainer> kept = new ArrayList<>(entries.size());
        boolean changed = false;
        for (Object value : entries) {
            if (value instanceof LootPoolEntryContainer entry && stripEntry(entry)) {
                changed = true;
                continue;
            }
            kept.add((LootPoolEntryContainer) value);
        }
        if (!changed) {
            return false;
        }
        if (kept.isEmpty()) {
            return true;
        }
        Reflections.set(pool, "entries", kept);
        return false;
    }

    private static boolean stripEntry(LootPoolEntryContainer entry) {
        if (entry instanceof LootItem) {
            return ModificationHandler.isSuppressed(holderItem(Reflections.get(entry, "item")));
        }
        if (entry instanceof TagEntry) {
            return fullySuppressed(Reflections.get(entry, "tag"));
        }
        if (entry instanceof CompositeEntryBase) {
            return stripComposite(entry);
        }
        return false;
    }

    private static boolean stripComposite(LootPoolEntryContainer entry) {
        if (!(Reflections.get(entry, "children") instanceof List<?> children)) {
            return false;
        }
        List<LootPoolEntryContainer> kept = new ArrayList<>(children.size());
        boolean changed = false;
        for (Object value : children) {
            if (value instanceof LootPoolEntryContainer child && stripEntry(child)) {
                changed = true;
                continue;
            }
            kept.add((LootPoolEntryContainer) value);
        }
        if (!changed) {
            return false;
        }
        if (kept.isEmpty()) {
            return true;
        }
        Object composed = Reflections.invoke(entry, "compose", List.class, kept);
        if (composed == null) {
            CFHCore.LOGGER.error("[Modification] Cannot recompose loot entry {}", entry.getClass().getName());
            return false;
        }
        Reflections.set(entry, "composedChildren", composed);
        Reflections.set(entry, "children", kept);
        return false;
    }

    @Nullable
    private static Item holderItem(@Nullable Object value) {
        return value instanceof Holder<?> holder && holder.value() instanceof Item item ? item : null;
    }

    private static boolean fullySuppressed(@Nullable Object value) {
        if (!(value instanceof TagKey<?> tag)) {
            return false;
        }
        HolderSet.Named<Item> members = BuiltInRegistries.ITEM.getTag((TagKey<Item>) tag).orElse(null);
        if (members == null) {
            return false;
        }
        boolean any = false;
        for (Holder<Item> holder : members) {
            any = true;
            if (!ModificationHandler.isSuppressed(holder.value())) {
                return false;
            }
        }
        return any;
    }

    private static void onLivingDrops(LivingDropsEvent event) {
        if (!ModificationHandler.hasSuppressed()) {
            return;
        }
        event.getDrops().removeIf(entity -> ModificationHandler.isSuppressed(entity.getItem().getItem()));
    }

    private static void onVillagerTrades(VillagerTradesEvent event) {
        if (!ModificationHandler.hasSuppressed()) {
            return;
        }
        for (List<ItemListing> listings : event.getTrades().values()) {
            removeSuppressed(listings);
        }
    }

    private static void onWandererTrades(WandererTradesEvent event) {
        if (!ModificationHandler.hasSuppressed()) {
            return;
        }
        removeSuppressed(event.getGenericTrades());
        removeSuppressed(event.getRareTrades());
    }

    private static void removeSuppressed(@Nullable List<ItemListing> listings) {
        if (listings == null) {
            return;
        }
        int before = listings.size();
        listings.removeIf(SuppressionHandler::mentionsSuppressed);
        if (listings.size() != before) {
            CFHCore.LOGGER.debug("[Modification] {} villager trades offering disabled entries removed", before - listings.size());
        }
    }

    private static boolean mentionsSuppressed(@Nullable ItemListing listing) {
        if (listing == null) {
            return false;
        }
        for (Field field : Reflections.fields(listing.getClass())) {
            if (refersToSuppressed(Reflections.read(field, listing), SCAN_DEPTH)) {
                return true;
            }
        }
        return false;
    }

    private static boolean refersToSuppressed(@Nullable Object value, int depth) {
        if (value == null) {
            return false;
        }
        if (value instanceof ItemStack stack) {
            return ModificationHandler.isSuppressed(stack.getItem());
        }
        if (value instanceof ItemCost cost) {
            return ModificationHandler.isSuppressed(cost.item().value())
                    || refersToSuppressed(cost.itemStack(), depth - 1);
        }
        if (value instanceof ItemLike like) {
            return ModificationHandler.isSuppressed(like.asItem());
        }
        if (depth <= 0) {
            return false;
        }
        if (value instanceof Holder<?> holder) {
            return refersToSuppressed(holder.value(), depth - 1);
        }
        if (value instanceof Map<?, ?> map) {
            for (Object element : map.values()) {
                if (refersToSuppressed(element, depth - 1)) {
                    return true;
                }
            }
            return false;
        }
        if (value instanceof Collection<?> collection) {
            for (Object element : collection) {
                if (refersToSuppressed(element, depth - 1)) {
                    return true;
                }
            }
            return false;
        }
        if (value instanceof Object[] array) {
            for (Object element : array) {
                if (refersToSuppressed(element, depth - 1)) {
                    return true;
                }
            }
        }
        return false;
    }
}
