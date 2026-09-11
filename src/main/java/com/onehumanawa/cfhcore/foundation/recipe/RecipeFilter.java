package com.onehumanawa.cfhcore.foundation.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.ItemComponent;
import com.onehumanawa.cfhcore.foundation.recipe.filter.RecipeFilterBuilder;
import com.onehumanawa.cfhcore.foundation.recipe.match.ReplacementMatch;
import com.onehumanawa.cfhcore.foundation.recipe.match.ReplacementMatchInfo;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class RecipeFilter {

    public static final Set<String> INPUT_FIELDS = Set.of("ingredient", "ingredients", "key");
    public static final Set<String> OUTPUT_FIELDS = Set.of("result", "results");

    private final Predicate<RecipeInfo> predicate;
    private final Map<String, String> inputReplacements = new LinkedHashMap<>();
    private final Map<String, String> outputReplacements = new LinkedHashMap<>();
    private final List<ReplacementMatchInfo> inputMatches = new ArrayList<>();
    private final List<ReplacementMatchInfo> outputMatches = new ArrayList<>();
    private final List<Consumer<JsonObject>> transformers = new ArrayList<>();
    private final List<Consumer<JsonObject>> preTransformers = new ArrayList<>();
    private final List<Consumer<JsonObject>> postTransformers = new ArrayList<>();
    private boolean shouldRemove = false;
    private boolean executed = false;

    public RecipeFilter(Predicate<RecipeInfo> predicate) {
        this.predicate = Objects.requireNonNull(predicate, "Predicate cannot be null");
        RecipeModificationHandler.register(this);
    }

    public static RecipeFilter all() {
        return new RecipeFilter(info -> true);
    }

    public static RecipeFilter byId(String id) {
        return byId(id, false);
    }

    public static RecipeFilter byId(String id, boolean blacklist) {
        ResourceLocation target = RecipeModificationHandler.parseId(id);
        Predicate<RecipeInfo> predicate = info -> info.id().equals(target);
        return new RecipeFilter(blacklist ? predicate.negate() : predicate);
    }

    public static RecipeFilter byIds(String... ids) {
        Set<ResourceLocation> targets = new HashSet<>();
        for (String id : ids) {
            targets.add(RecipeModificationHandler.parseId(id));
        }
        return new RecipeFilter(info -> targets.contains(info.id()));
    }

    public static RecipeFilter byMod(String modId) {
        return byMod(modId, false);
    }

    public static RecipeFilter byMod(String modId, boolean blacklist) {
        Predicate<RecipeInfo> predicate = info -> info.modId().equals(modId);
        return new RecipeFilter(blacklist ? predicate.negate() : predicate);
    }

    public static RecipeFilter byMods(String... modIds) {
        Set<String> targets = new HashSet<>(Arrays.asList(modIds));
        return new RecipeFilter(info -> targets.contains(info.modId()));
    }

    public static RecipeFilter byType(String typeId) {
        return byType(typeId, false);
    }

    public static RecipeFilter byType(String typeId, boolean blacklist) {
        ResourceLocation target = RecipeModificationHandler.parseId(typeId);
        Predicate<RecipeInfo> predicate = info -> target.equals(info.typeId());
        return new RecipeFilter(blacklist ? predicate.negate() : predicate);
    }

    public static RecipeFilter byTypes(String... typeIds) {
        Set<ResourceLocation> targets = new HashSet<>();
        for (String typeId : typeIds) {
            targets.add(RecipeModificationHandler.parseId(typeId));
        }
        return new RecipeFilter(info -> info.typeId() != null && targets.contains(info.typeId()));
    }

    public static RecipeFilter byCustom(Predicate<RecipeInfo> predicate) {
        return new RecipeFilter(predicate);
    }

    public static RecipeFilterBuilder builder() {
        return new RecipeFilterBuilder();
    }

    public RecipeFilter replaceInput(String oldItem, String newItem) {
        ensureNotExecuted();
        inputReplacements.put(oldItem, newItem);
        return this;
    }

    public RecipeFilter replaceInput(ReplacementMatch match, String newItem) {
        return replaceInput(new ReplacementMatchInfo(Objects.requireNonNull(match, "Match cannot be null"), newItem));
    }

    public RecipeFilter replaceInput(ReplacementMatchInfo match) {
        ensureNotExecuted();
        inputMatches.add(Objects.requireNonNull(match, "Match cannot be null"));
        return this;
    }

    public RecipeFilter replaceOutput(String oldItem, String newItem) {
        ensureNotExecuted();
        outputReplacements.put(oldItem, newItem);
        return this;
    }

    public RecipeFilter replaceOutput(ReplacementMatch match, String newItem) {
        return replaceOutput(new ReplacementMatchInfo(Objects.requireNonNull(match, "Match cannot be null"), newItem));
    }

    public RecipeFilter replaceOutput(ReplacementMatchInfo match) {
        ensureNotExecuted();
        outputMatches.add(Objects.requireNonNull(match, "Match cannot be null"));
        return this;
    }

    public RecipeFilter modify(Consumer<JsonObject> transformer) {
        ensureNotExecuted();
        transformers.add(Objects.requireNonNull(transformer, "Transformer cannot be null"));
        return this;
    }

    public RecipeFilter preModify(Consumer<JsonObject> transformer) {
        ensureNotExecuted();
        preTransformers.add(Objects.requireNonNull(transformer, "Transformer cannot be null"));
        return this;
    }

    public RecipeFilter postModify(Consumer<JsonObject> transformer) {
        ensureNotExecuted();
        postTransformers.add(Objects.requireNonNull(transformer, "Transformer cannot be null"));
        return this;
    }

    public void remove() {
        ensureNotExecuted();
        this.shouldRemove = true;
        this.executed = true;
        RecipeModificationHandler.complete(this);
    }

    public void execute() {
        ensureNotExecuted();
        this.executed = true;
        RecipeModificationHandler.complete(this);
    }

    boolean matches(RecipeInfo info) {
        return predicate.test(info);
    }

    boolean shouldRemove() {
        return shouldRemove;
    }

    boolean isExecuted() {
        return executed;
    }

    void collect(Map<String, String> inputs, Map<String, String> outputs,
                 List<Consumer<JsonObject>> transformerList) {
        if (shouldRemove) {
            return;
        }

        inputs.putAll(inputReplacements);
        outputs.putAll(outputReplacements);
        transformerList.addAll(preTransformers);
        if (!inputMatches.isEmpty()) {
            List<ReplacementMatchInfo> matches = List.copyOf(inputMatches);
            transformerList.add(json -> applyMatchReplacements(json, INPUT_FIELDS, matches, false));
        }
        if (!outputMatches.isEmpty()) {
            List<ReplacementMatchInfo> matches = List.copyOf(outputMatches);
            transformerList.add(json -> applyMatchReplacements(json, OUTPUT_FIELDS, matches, true));
        }
        transformerList.addAll(transformers);
        transformerList.addAll(postTransformers);
    }

    private static void applyMatchReplacements(JsonElement element, Set<String> fields,
                                               List<ReplacementMatchInfo> matches, boolean useId) {
        if (element == null || element.isJsonNull()) {
            return;
        }

        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : new ArrayList<>(object.entrySet())) {
                if (fields.contains(entry.getKey())) {
                    replaceIds(entry.getValue(), matches, useId);
                } else {
                    applyMatchReplacements(entry.getValue(), fields, matches, useId);
                }
            }
            return;
        }

        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                applyMatchReplacements(child, fields, matches, useId);
            }
        }
    }

    private static void replaceIds(JsonElement element, List<ReplacementMatchInfo> matches, boolean useId) {
        if (element == null || element.isJsonNull()) {
            return;
        }

        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            String current = currentId(object);
            if (current != null) {
                String replacement = replacementFor(current, matches);
                if (replacement != null) {
                    ItemComponent.applyTo(object, replacement, useId);
                }
                return;
            }
            for (Map.Entry<String, JsonElement> entry : new ArrayList<>(object.entrySet())) {
                replaceIds(entry.getValue(), matches, useId);
            }
            return;
        }

        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                JsonElement child = array.get(i);
                if (child != null && child.isJsonPrimitive()) {
                    String replacement = replacementFor(child.getAsString(), matches);
                    if (replacement != null) {
                        array.set(i, new JsonPrimitive(replacement));
                    }
                } else {
                    replaceIds(child, matches, useId);
                }
            }
        }
    }

    private static String currentId(JsonObject object) {
        if (object.has("item") && object.get("item").isJsonPrimitive()) {
            return object.get("item").getAsString();
        }
        if (object.has("id") && object.get("id").isJsonPrimitive()) {
            return object.get("id").getAsString();
        }
        if (object.has("tag") && object.get("tag").isJsonPrimitive()) {
            return "#" + object.get("tag").getAsString();
        }
        return null;
    }

    private static String replacementFor(String current, List<ReplacementMatchInfo> matches) {
        if (current == null) {
            return null;
        }
        for (ReplacementMatchInfo info : matches) {
            if (!info.matches(current)) {
                continue;
            }
            String replacement = ItemComponent.toId(info.replacement());
            if (replacement != null) {
                return replacement;
            }
        }
        return null;
    }

    private void ensureNotExecuted() {
        if (executed) {
            throw new IllegalStateException(
                    "This RecipeFilter has already been executed and cannot be modified. " +
                            "Create a new filter for additional changes."
            );
        }
    }

    public Map<String, String> getInputReplacements() {
        return Collections.unmodifiableMap(inputReplacements);
    }

    public Map<String, String> getOutputReplacements() {
        return Collections.unmodifiableMap(outputReplacements);
    }

    public List<ReplacementMatchInfo> getInputMatches() {
        return Collections.unmodifiableList(inputMatches);
    }

    public List<ReplacementMatchInfo> getOutputMatches() {
        return Collections.unmodifiableList(outputMatches);
    }

    public int getTransformerCount() {
        return preTransformers.size() + transformers.size() + postTransformers.size();
    }

    public int getReplacementCount() {
        return inputReplacements.size() + outputReplacements.size() + inputMatches.size() + outputMatches.size();
    }

    @Override
    public String toString() {
        return String.format(
                "RecipeFilter{inputReplacements=%d, outputReplacements=%d, inputMatches=%d, outputMatches=%d, " +
                        "transformers=%d, remove=%s}",
                inputReplacements.size(), outputReplacements.size(), inputMatches.size(), outputMatches.size(),
                getTransformerCount(), shouldRemove);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RecipeFilter that)) return false;
        return shouldRemove == that.shouldRemove &&
                Objects.equals(inputReplacements, that.inputReplacements) &&
                Objects.equals(outputReplacements, that.outputReplacements) &&
                Objects.equals(inputMatches, that.inputMatches) &&
                Objects.equals(outputMatches, that.outputMatches);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inputReplacements, outputReplacements, inputMatches, outputMatches, shouldRemove);
    }
}
