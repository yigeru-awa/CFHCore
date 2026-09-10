package com.onehumanawa.cfhcore.foundation.recipe;

import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.foundation.RecipeHelper;
import com.onehumanawa.cfhcore.foundation.recipe.filter.RecipeFilterBuilder;
import com.onehumanawa.cfhcore.foundation.recipe.match.ReplacementMatch;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Enterprise-grade Recipe Filter with fluent API for recipe manipulation.
 * <p>
 * This class implements the Chain of Responsibility and Specification patterns
 * for composable recipe filtering and transformation. Instances are created
 * through factory methods in {@link RecipeHelper}.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class RecipeFilter {

    // ================================================================
    // Filter State
    // ================================================================

    private final Predicate<RecipeInfo> predicate;
    private final Map<String, String> inputReplacements = new LinkedHashMap<>();
    private final Map<String, String> outputReplacements = new LinkedHashMap<>();
    private final List<Consumer<JsonObject>> transformers = new ArrayList<>();
    private final List<Consumer<JsonObject>> preTransformers = new ArrayList<>();
    private final List<Consumer<JsonObject>> postTransformers = new ArrayList<>();
    private boolean shouldRemove = false;
    private boolean executed = false;

    // ================================================================
    // Package-Private Constructor
    // ================================================================

    /**
     * Package-private constructor. Use factory methods in RecipeHelper.
     *
     * @param predicate The filtering predicate
     */
    public RecipeFilter(Predicate<RecipeInfo> predicate) {
        this.predicate = Objects.requireNonNull(predicate, "Predicate cannot be null");
        RecipeModificationHandler.register(this);
    }

    // ================================================================
    // Factory Methods (Delegated from RecipeHelper)
    // ================================================================

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

    // ================================================================
    // Fluent API Methods
    // ================================================================

    public RecipeFilter replaceInput(String oldItem, String newItem) {
        ensureNotExecuted();
        inputReplacements.put(oldItem, newItem);
        return this;
    }

    public RecipeFilter replaceInput(ReplacementMatch match, String newItem) {
        // This is a more sophisticated version - would need to handle the match differently
        ensureNotExecuted();
        // Store the match info for later processing
        inputReplacements.put(match.toString(), newItem);
        return this;
    }

    public RecipeFilter replaceOutput(String oldItem, String newItem) {
        ensureNotExecuted();
        outputReplacements.put(oldItem, newItem);
        return this;
    }

    public RecipeFilter replaceOutput(ReplacementMatch match, String newItem) {
        ensureNotExecuted();
        outputReplacements.put(match.toString(), newItem);
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

    // ================================================================
    // Internal Methods (Package-Private)
    // ================================================================

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
        if (!shouldRemove) {
            inputs.putAll(inputReplacements);
            outputs.putAll(outputReplacements);
            transformerList.addAll(preTransformers);
            transformerList.addAll(transformers);
            transformerList.addAll(postTransformers);
        }
    }

    // ================================================================
    // Validation Methods
    // ================================================================

    private void ensureNotExecuted() {
        if (executed) {
            throw new IllegalStateException(
                    "This RecipeFilter has already been executed and cannot be modified. " +
                            "Create a new filter for additional changes."
            );
        }
    }

    // ================================================================
    // Getters
    // ================================================================

    public Map<String, String> getInputReplacements() {
        return Collections.unmodifiableMap(inputReplacements);
    }

    public Map<String, String> getOutputReplacements() {
        return Collections.unmodifiableMap(outputReplacements);
    }

    public int getTransformerCount() {
        return preTransformers.size() + transformers.size() + postTransformers.size();
    }

    // ================================================================
    // Object Overrides
    // ================================================================

    @Override
    public String toString() {
        return String.format("RecipeFilter{inputReplacements=%d, outputReplacements=%d, transformers=%d, remove=%s}",
                inputReplacements.size(), outputReplacements.size(), getTransformerCount(), shouldRemove);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RecipeFilter that)) return false;
        return shouldRemove == that.shouldRemove &&
                Objects.equals(inputReplacements, that.inputReplacements) &&
                Objects.equals(outputReplacements, that.outputReplacements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inputReplacements, outputReplacements, shouldRemove);
    }
}