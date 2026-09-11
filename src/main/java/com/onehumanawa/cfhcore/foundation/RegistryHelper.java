package com.onehumanawa.cfhcore.foundation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import com.onehumanawa.cfhcore.foundation.registry.BlockRegistrar;
import com.onehumanawa.cfhcore.foundation.registry.ItemRegistrar;
import net.minecraft.world.level.block.SoundType;
import org.jetbrains.annotations.Nullable;

import com.onehumanawa.cfhcore.CFHCore;
import com.tterrag.registrate.providers.RegistrateLangProvider;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Rarity;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class RegistryHelper {
    private static final Pattern PATH_PATTERN = Pattern.compile("[a-z0-9/._-]+");
    private static boolean initialized = false;

    public RegistryHelper() {}

    public static void init() {
        block("fire_bricks")
                .strength(2.0F, 6.0F)
                .sound(SoundType.CALCITE)
                .translation("Fire Bricks", "耐火砖")
                .requiresCorrectTool()
                .texturePath(RegistryHelper.id("block/tfc/fire_bricks"))
                .tag("minecraft:mineable/pickaxe")
                .defaultCreativeTab()
                .register();
        ensureInitialized();
    }

    public static ItemRegistrar item(String path) {
        return new ItemRegistrar(path);
    }

    public static BlockRegistrar block(String path) {
        return new BlockRegistrar(path);
    }

    public static boolean checkPath(String kind, @Nullable String path) {
        if (path == null || !PATH_PATTERN.matcher(path).matches()) {
            CFHCore.LOGGER.error("[Registry] Invalid {} path: '{}' - expected a lowercase path without namespace, such as \"polished_brass\"; entry skipped", kind, path);
            return false;
        }
        return true;
    }

    public static ResourceLocation id(String path) {
        return CFHCore.asResource(path);
    }

    public static String itemLangKey(String path) {
        return "item." + CFHCore.ID + "." + path;
    }

    public static String blockLangKey(String path) {
        return "block." + CFHCore.ID + "." + path;
    }

    public static String defaultName(String path) {
        int slash = path.lastIndexOf('/');
        return RegistrateLangProvider.toEnglishName(slash >= 0 ? path.substring(slash + 1) : path);
    }

    public static void collectLang(String key, @Nullable String en, @Nullable String zh) {
        CFHCore.langCollector().add(key, en == null ? "" : en, zh == null ? "" : zh);
    }

    public static Rarity rarity(@Nullable String name) {
        Rarity resolved = findRarity(name);
        if (resolved != null) {
            return resolved;
        }
        CFHCore.LOGGER.error("[Registry] Unknown rarity '{}' - expected one of [{}]; falling back to COMMON", name, rarityNames());
        return Rarity.COMMON;
    }

    @Nullable
    static Rarity findRarity(@Nullable String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        for (Rarity rarity : Rarity.values()) {
            if (rarity.getSerializedName().equals(normalized) || rarity.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return rarity;
            }
        }
        return null;
    }

    private static String rarityNames() {
        List<String> names = new ArrayList<>();
        for (Rarity rarity : Rarity.values()) {
            names.add(rarity.getSerializedName());
        }
        return String.join(", ", names);
    }

    @Nullable
    public static <E extends Enum<E>> E byName(Class<E> type, @Nullable String name, String option) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String normalized = name.trim().toUpperCase(Locale.ROOT);
        for (E constant : type.getEnumConstants()) {
            if (constant.name().equals(normalized)) {
                return constant;
            }
        }
        CFHCore.LOGGER.error("[Registry] Unknown {} '{}' - expected one of {}", option, name, enumNames(type));
        return null;
    }

    private static <E extends Enum<E>> String enumNames(Class<E> type) {
        List<String> names = new ArrayList<>();
        for (E constant : type.getEnumConstants()) {
            names.add(constant.name().toLowerCase(Locale.ROOT));
        }
        return "[" + String.join(", ", names) + "]";
    }

    public static void ensureInitialized() {
        if (initialized) return;
        initialized = true;

        RecipeHelper.init();

        CFHCore.LOGGER.info("[Registry] Registry initialized");
    }

    @Nullable
    public static ResourceLocation parseTagId(@Nullable String tagId) {
        if (tagId == null || tagId.isBlank()) {
            return null;
        }
        String trimmed = tagId.trim();
        if (trimmed.startsWith("#")) {
            trimmed = trimmed.substring(1);
        }
        try {
            int separator = trimmed.indexOf(':');
            return separator >= 0
                    ? ResourceLocation.parse(trimmed)
                    : ResourceLocation.withDefaultNamespace(trimmed);
        } catch (Exception e) {
            return null;
        }
    }
}
