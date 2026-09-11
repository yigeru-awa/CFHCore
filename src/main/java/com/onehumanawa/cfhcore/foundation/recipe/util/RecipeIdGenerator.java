package com.onehumanawa.cfhcore.foundation.recipe.util;

import com.onehumanawa.cfhcore.foundation.recipe.RecipeModification;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeComponentValue;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class RecipeIdGenerator {

    private RecipeIdGenerator() {}

    public static Function<RecipeModification, String> fromInputAndOutput() {
        return recipe -> {
            String inputs = joinRole(recipe, RecipeComponent.Role.INPUT);
            String outputs = joinRole(recipe, RecipeComponent.Role.OUTPUT);
            if (!inputs.isEmpty() && !outputs.isEmpty()) {
                return sanitize(inputs + "_to_" + outputs);
            }
            return fallback(recipe, inputs.isEmpty() ? outputs : inputs);
        };
    }

    public static Function<RecipeModification, String> fromString(String prefix) {
        String head = sanitize(prefix);
        return recipe -> {
            String body = recipe != null ? sanitize(recipe.getId()) : "";
            if (body.isEmpty()) {
                return generateUniqueId(head.isEmpty() ? "recipe" : head);
            }
            return head.isEmpty() ? body : head + "_" + body;
        };
    }

    public static Function<RecipeModification, String> fromPattern() {
        return recipe -> {
            String pattern = recipe != null ? stringify(recipe.get("pattern")) : "";
            if (pattern.isEmpty()) {
                return fromInputAndOutput().apply(recipe);
            }
            String outputs = joinRole(recipe, RecipeComponent.Role.OUTPUT);
            return outputs.isEmpty() ? sanitize(pattern) : sanitize(pattern + "_to_" + outputs);
        };
    }

    public static String generateUniqueId(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "_" + System.nanoTime();
    }

    public static String sanitize(String input) {
        if (input == null) return "";
        return input.toLowerCase()
                .replaceAll("[^a-z0-9_/]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    private static String fallback(@Nullable RecipeModification recipe, String candidate) {
        if (!candidate.isEmpty()) {
            return sanitize(candidate);
        }
        if (recipe != null) {
            String body = sanitize(recipe.getId());
            if (!body.isEmpty()) {
                return body;
            }
        }
        return "";
    }

    private static String joinRole(@Nullable RecipeModification recipe, RecipeComponent.Role role) {
        if (recipe == null || recipe.valueMap() == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (RecipeComponentValue<?> holder : recipe.valueMap().holders()) {
            if (holder.key().role() != role) {
                continue;
            }
            String text = stringify(holder.value());
            if (!text.isEmpty()) {
                parts.add(text);
            }
        }
        return String.join("+", parts);
    }

    private static String stringify(@Nullable Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String text) {
            return text;
        }
        if (value instanceof ItemStack stack) {
            return stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        }
        if (value instanceof Item item) {
            return BuiltInRegistries.ITEM.getKey(item).toString();
        }
        if (value instanceof Object[] array) {
            return join(array);
        }
        if (value instanceof Map<?, ?> map) {
            return join(map.values().toArray());
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> items = new ArrayList<>();
            iterable.forEach(items::add);
            return join(items.toArray());
        }
        return String.valueOf(value);
    }

    private static String join(Object[] values) {
        List<String> parts = new ArrayList<>();
        for (Object value : values) {
            String text = stringify(value);
            if (!text.isEmpty()) {
                parts.add(text);
            }
        }
        return String.join("+", parts);
    }
}
