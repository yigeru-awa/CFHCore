package com.onehumanawa.cfhcore.foundation.recipe.util;

import net.minecraft.resources.ResourceLocation;

/**
 * Utility class for ResourceLocation operations.
 */
public final class ResourceLocationUtils {

    private static final String DEFAULT_NAMESPACE = "minecraft";

    private ResourceLocationUtils() {}

    public static ResourceLocation parse(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("ResourceLocation cannot be null or empty");
        }
        String[] parts = id.split(":", 2);
        if (parts.length == 2) {
            return ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
        }
        return ResourceLocation.fromNamespaceAndPath(DEFAULT_NAMESPACE, id);
    }

    public static String toModId(ResourceLocation location) {
        return location.getNamespace();
    }

    public static String toPath(ResourceLocation location) {
        return location.getPath();
    }

    public static boolean isDefaultNamespace(ResourceLocation location) {
        return DEFAULT_NAMESPACE.equals(location.getNamespace());
    }

    public static String fullName(ResourceLocation location) {
        return location.toString();
    }

    public static String shortName(ResourceLocation location) {
        return isDefaultNamespace(location) ? location.getPath() : location.toString();
    }

    public static ResourceLocation withDefaultNamespace(String path) {
        if (path.contains(":")) {
            return parse(path);
        }
        return ResourceLocation.fromNamespaceAndPath(DEFAULT_NAMESPACE, path);
    }

    public static String toTagId(String id) {
        if (id.startsWith("#")) {
            return id.substring(1);
        }
        return id;
    }

    public static boolean isTag(String id) {
        return id != null && id.startsWith("#");
    }
}