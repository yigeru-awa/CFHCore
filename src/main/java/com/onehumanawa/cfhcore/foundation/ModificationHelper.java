package com.onehumanawa.cfhcore.foundation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.onehumanawa.cfhcore.CFHCore;
import com.onehumanawa.cfhcore.foundation.modification.BlockModifier;
import com.onehumanawa.cfhcore.foundation.modification.ItemModifier;
import com.onehumanawa.cfhcore.foundation.modification.ModificationHandler;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class ModificationHelper {
    private static boolean initialized = false;

    public ModificationHelper() {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        blockTag("minecraft:logs")
                .strength(6.0f, 6.0f)
                .requiresCorrectTool()
                .apply();
        disableItems();

        CFHCore.LOGGER.info("[Modification] Modification helper initialized");
    }

    private static void disableItems() {
        List<String> materials = List.of("minecraft:leather", "minecraft:chainmail", "minecraft:wooden", "minecraft:stone", "minecraft:iron", "minecraft:diamond", "minecraft:golden", "minecraft:netherite", "minecraft:turtle");
        List<String> toolTypes = List.of("pickaxe", "axe", "sword", "hoe", "shovel");
        List<String> armorTypes = List.of("helmet", "chestplate", "leggings", "boots", "horse_armor");
        List<String> disabledItems = new ArrayList<>();
        for (String material : materials) {
            for (String type : toolTypes) {
                disabledItems.add(material + "_" + type);
            }
            for (String type : armorTypes) {
                disabledItems.add(material + "_" + type);
            }
        }
        List<String> otherDisables = List.of(
                "minecraft:totem_of_undying", "minecraft:elytra", "minecraft:fire_charge",
                "minecraft:wind_charge", "minecraft:ender_pearl", "minecraft:ender_eye",
                "minecraft:trident", "minecraft:mace", "minecraft:end_crystal",
                "minecraft:breeze_rod", "minecraft:blaze_rod", "minecraft:heavy_core",
                "minecraft:sculk", "minecraft:sculk_vein", "minecraft:sculk_catalyst",
                "minecraft:sculk_shrieker", "minecraft:sculk_sensor", "minecraft:calibrated_sculk_sensor"
        );
        disabledItems.addAll(otherDisables);
        disabledItems.forEach(id -> item(id).disable().apply());
    }

    public static void register(IEventBus modEventBus) {
        ModificationHandler.register(modEventBus);
    }

    public static ItemModifier item(String id) {
        return isTag(id) ? itemTag(id) : item(parseId(id));
    }

    public static ItemModifier item(@Nullable ResourceLocation id) {
        return new ItemModifier(id);
    }

    public static ItemModifier item(ItemLike item) {
        return item(BuiltInRegistries.ITEM.getKey(item.asItem()));
    }

    public static BlockModifier block(String id) {
        return isTag(id) ? blockTag(id) : block(parseId(id));
    }

    public static BlockModifier block(@Nullable ResourceLocation id) {
        return new BlockModifier(id);
    }

    public static BlockModifier block(Block block) {
        return block(BuiltInRegistries.BLOCK.getKey(block));
    }

    public static ItemModifier itemTag(String tagId) {
        return itemTag(parseItemTag(tagId));
    }

    public static ItemModifier itemTag(@Nullable TagKey<Item> tag) {
        return new ItemModifier(tag);
    }

    public static BlockModifier blockTag(String tagId) {
        return blockTag(parseBlockTag(tagId));
    }

    public static BlockModifier blockTag(@Nullable TagKey<Block> tag) {
        return new BlockModifier(tag);
    }

    public static void hide(String id) {
        if (isTag(id)) {
            hideTag(id);
        } else {
            hide(parseId(id));
        }
    }

    public static void hide(@Nullable ResourceLocation id) {
        if (id != null) {
            ModificationHandler.hide(id);
        }
    }

    public static void hide(ItemLike item) {
        hide(BuiltInRegistries.ITEM.getKey(item.asItem()));
    }

    public static void hide(Block block) {
        hide(BuiltInRegistries.BLOCK.getKey(block));
    }

    public static void disable(String id) {
        if (isTag(id)) {
            disableTag(id);
        } else {
            disable(parseId(id));
        }
    }

    public static void disable(@Nullable ResourceLocation id) {
        if (id != null) {
            ModificationHandler.disable(id);
        }
    }

    public static void disable(ItemLike item) {
        disable(BuiltInRegistries.ITEM.getKey(item.asItem()));
    }

    public static void disable(Block block) {
        disable(BuiltInRegistries.BLOCK.getKey(block));
    }

    public static void hideTag(String tagId) {
        hideTag(parseTagId(tagId));
    }

    public static void hideTag(@Nullable ResourceLocation tagId) {
        if (tagId != null) {
            ModificationHandler.hideTag(tagId);
        }
    }

    public static void disableTag(String tagId) {
        disableTag(parseTagId(tagId));
    }

    public static void disableTag(@Nullable ResourceLocation tagId) {
        if (tagId != null) {
            ModificationHandler.disableTag(tagId);
        }
    }

    @Nullable
    public static ResourceLocation parseId(@Nullable String id) {
        ResourceLocation parsed = RegistryHelper.parseTagId(id);
        if (parsed == null) {
            CFHCore.LOGGER.error("[Modification] Invalid id '{}' - expected 'namespace:path' or 'path'", id);
        }
        return parsed;
    }

    @Nullable
    public static ResourceLocation parseTagId(@Nullable String tagId) {
        ResourceLocation parsed = RegistryHelper.parseTagId(tagId);
        if (parsed == null) {
            CFHCore.LOGGER.error("[Modification] Invalid tag id '{}' - expected '#namespace:path', 'namespace:path' or 'path'", tagId);
        }
        return parsed;
    }

    @Nullable
    private static TagKey<Item> parseItemTag(@Nullable String tagId) {
        ResourceLocation parsed = parseTagId(tagId);
        return parsed == null ? null : TagKey.create(Registries.ITEM, parsed);
    }

    @Nullable
    private static TagKey<Block> parseBlockTag(@Nullable String tagId) {
        ResourceLocation parsed = parseTagId(tagId);
        return parsed == null ? null : TagKey.create(Registries.BLOCK, parsed);
    }

    private static boolean isTag(@Nullable String id) {
        return id != null && id.trim().startsWith("#");
    }
}
