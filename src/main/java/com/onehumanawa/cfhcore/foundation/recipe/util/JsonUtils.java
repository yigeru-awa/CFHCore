package com.onehumanawa.cfhcore.foundation.recipe.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public final class JsonUtils {

    private static final List<String> ID_FIELDS = List.of("item", "id", "tag", "fluid", "fluidTag");
    private static final Set<String> TAG_FIELDS = Set.of("tag", "fluidTag");

    private JsonUtils() {}

    public static boolean isItemObject(JsonObject obj) {
        return obj.has("item") || obj.has("id");
    }

    public static boolean isTagObject(JsonObject obj) {
        return obj.has("tag");
    }

    public static boolean isFluidObject(JsonObject obj) {
        return obj.has("fluid") || obj.has("fluidTag");
    }

    @Nullable
    public static String getItemId(JsonObject obj) {
        if (obj.has("item")) {
            return obj.get("item").getAsString();
        }
        if (obj.has("id")) {
            return obj.get("id").getAsString();
        }
        if (obj.has("tag")) {
            return "#" + obj.get("tag").getAsString();
        }
        return null;
    }

    public static void setItemId(JsonObject obj, String id) {
        if (id.startsWith("#")) {
            obj.remove("item");
            obj.remove("id");
            obj.addProperty("tag", id.substring(1));
        } else {
            obj.remove("tag");
            obj.addProperty("item", id);
        }
    }

    public static Set<String> collectIds(@Nullable JsonElement element, Collection<String> fields) {
        Set<String> ids = new LinkedHashSet<>();
        collect(element, fields, ids);
        return ids;
    }

    private static void collect(@Nullable JsonElement element, Collection<String> fields, Set<String> ids) {
        if (element == null || element.isJsonNull()) {
            return;
        }

        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                if (fields.contains(entry.getKey())) {
                    extract(entry.getValue(), ids);
                } else {
                    collect(entry.getValue(), fields, ids);
                }
            }
            return;
        }

        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collect(child, fields, ids);
            }
        }
    }

    private static void extract(@Nullable JsonElement element, Set<String> ids) {
        if (element == null || element.isJsonNull()) {
            return;
        }

        if (element.isJsonPrimitive()) {
            ids.add(element.getAsString());
            return;
        }

        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                extract(child, ids);
            }
            return;
        }

        JsonObject object = element.getAsJsonObject();
        boolean found = false;
        for (String field : ID_FIELDS) {
            JsonElement child = object.get(field);
            if (child == null || !child.isJsonPrimitive()) {
                continue;
            }
            String value = child.getAsString();
            ids.add(TAG_FIELDS.contains(field) ? "#" + value : value);
            found = true;
        }

        if (found) {
            return;
        }

        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            extract(entry.getValue(), ids);
        }
    }

    public static boolean findAndReplace(JsonElement element, Predicate<JsonObject> matcher, String replacement) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            String currentId = getItemId(obj);
            if (currentId != null && matcher.test(obj)) {
                setItemId(obj, replacement);
                return true;
            }
            boolean changed = false;
            for (var entry : obj.entrySet()) {
                changed = findAndReplace(entry.getValue(), matcher, replacement) || changed;
            }
            return changed;
        }

        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            boolean changed = false;
            for (int i = 0; i < array.size(); i++) {
                changed = findAndReplace(array.get(i), matcher, replacement) || changed;
            }
            return changed;
        }

        return false;
    }

    public static JsonObject deepCopy(JsonObject obj) {
        return obj.deepCopy();
    }

    public static boolean hasField(JsonObject obj, String field) {
        return obj.has(field) && !obj.get(field).isJsonNull();
    }

    public static String getString(JsonObject obj, String field, String defaultValue) {
        if (!hasField(obj, field)) return defaultValue;
        JsonElement element = obj.get(field);
        return element.isJsonPrimitive() ? element.getAsString() : defaultValue;
    }

    public static int getInt(JsonObject obj, String field, int defaultValue) {
        if (!hasField(obj, field)) return defaultValue;
        JsonElement element = obj.get(field);
        return element.isJsonPrimitive() ? element.getAsInt() : defaultValue;
    }

    public static float getFloat(JsonObject obj, String field, float defaultValue) {
        if (!hasField(obj, field)) return defaultValue;
        JsonElement element = obj.get(field);
        return element.isJsonPrimitive() ? element.getAsFloat() : defaultValue;
    }

    public static boolean getBoolean(JsonObject obj, String field, boolean defaultValue) {
        if (!hasField(obj, field)) return defaultValue;
        JsonElement element = obj.get(field);
        return element.isJsonPrimitive() ? element.getAsBoolean() : defaultValue;
    }
}
