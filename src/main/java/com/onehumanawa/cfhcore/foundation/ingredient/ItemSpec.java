package com.onehumanawa.cfhcore.foundation.ingredient;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class ItemSpec {

    private static final char SEPARATOR = '@';
    public record Decoded(String id, JsonObject components) {
    }
    private ItemSpec() {}
    public static String of(String id, String dataComponents) {
        if (dataComponents == null || dataComponents.isBlank())
            return id;
        return id + SEPARATOR + dataComponents.trim();
    }

    public static boolean isEncoded(String spec) {
        return spec.indexOf(SEPARATOR) > 0;
    }

    public static Decoded decode(String spec) {
        int separator = spec.indexOf(SEPARATOR);
        if (separator < 0)
            return new Decoded(spec, null);
        String id = spec.substring(0, separator);
        JsonObject components = JsonParser.parseString(spec.substring(separator + 1)).getAsJsonObject();
        return new Decoded(id, components);
    }

    public static JsonObject ingredientJson(String spec) {
        Decoded decoded = decode(spec);
        JsonObject json = new JsonObject();
        if (decoded.components() == null) {
            json.addProperty("item", decoded.id());
            return json;
        }
        json.addProperty("type", "neoforge:components");
        JsonArray items = new JsonArray();
        items.add(decoded.id());
        json.add("items", items);
        json.add("components", decoded.components());
        return json;
    }

    public static JsonObject stackJson(String spec, int count) {
        Decoded decoded = decode(spec);
        JsonObject json = new JsonObject();
        json.addProperty("id", decoded.id());
        if (count != 1)
            json.addProperty("count", count);
        if (decoded.components() != null)
            json.add("components", decoded.components());
        return json;
    }
}
