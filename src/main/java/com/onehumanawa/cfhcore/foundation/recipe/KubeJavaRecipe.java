package com.onehumanawa.cfhcore.foundation.recipe;

import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.foundation.recipe.action.IngredientAction;
import com.onehumanawa.cfhcore.foundation.recipe.action.IngredientActionHolder;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeComponentValue;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeComponentValueMap;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;
import com.onehumanawa.cfhcore.foundation.recipe.match.ReplacementMatch;
import com.onehumanawa.cfhcore.foundation.recipe.match.ReplacementMatchInfo;
import com.onehumanawa.cfhcore.foundation.recipe.util.SlotFilter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Core recipe representation - equivalent to KubeJS's KubeRecipe.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class KubeJavaRecipe {
    private static final Logger LOGGER = LoggerFactory.getLogger("KubeJavaRecipe");

    public static final String CHANGED_MARKER = "_kubejava_changed_marker";

    public ResourceLocation id;
    public RecipeType type;
    public boolean newRecipe = false;
    public boolean removed = false;
    public String sourceLine = "unknown";
    public String modifyResult = "";
    public JsonObject originalJson = null;
    public JsonObject json = null;
    public boolean changed = false;
    public boolean creationError = false;

    private RecipeComponentValueMap valueMap = RecipeComponentValueMap.EMPTY;
    private RecipeComponentValue<?>[] inputValues;
    private RecipeComponentValue<?>[] outputValues;
    private List<IngredientActionHolder> ingredientActions;
    private Recipe<?> originalRecipe;

    public KubeJavaRecipe(ResourceLocation id, RecipeType type, JsonObject json) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.json = json != null ? json.deepCopy() : new JsonObject();
        this.originalJson = json != null ? json.deepCopy() : null;
        initValues(true);
    }

    public KubeJavaRecipe(String id, RecipeType type, JsonObject json) {
        this(ResourceLocation.parse(id), type, json);
    }

    // ================================================================
    // Component Access
    // ================================================================

    @Nullable
    public <T> T getValue(RecipeKey<T> key) {
        RecipeComponentValue<T> holder = valueMap.getHolder(key);
        if (holder == null) {
            if (!key.optional()) {
                throw new RecipeModifyMissingComponentException(key.primaryName(), key);
            }
            return null;
        }
        return holder.value();
    }

    public <T> KubeJavaRecipe setValue(RecipeKey<T> key, T value) {
        RecipeComponentValue<T> holder = valueMap.getHolder(key);
        if (holder == null) {
            throw new RecipeModifyMissingComponentException(key.primaryName(), key);
        }
        holder.value(value);
        holder.written(true);
        save();
        return this;
    }

    @Nullable
    public Object get(String name) {
        for (RecipeComponentValue<?> holder : valueMap.holders()) {
            if (holder.key().hasName(name)) {
                return holder.value();
            }
        }
        return null;
    }

    public KubeJavaRecipe set(String name, Object value) {
        for (RecipeComponentValue<?> holder : valueMap.holders()) {
            if (holder.key().hasName(name)) {
                @SuppressWarnings("unchecked")
                RecipeKey<Object> key = (RecipeKey<Object>) holder.key();
                setValue(key, value);
                return this;
            }
        }
        throw new RecipeModifyMissingComponentException(name);
    }

    // ================================================================
    // Input/Output Access
    // ================================================================

    public RecipeComponentValue<?>[] inputValues() {
        if (inputValues == null) {
            List<RecipeComponentValue<?>> list = new ArrayList<>();
            for (RecipeComponentValue<?> holder : valueMap.holders()) {
                if (holder.key().role() == RecipeComponent.Role.INPUT) {
                    list.add(holder);
                }
            }
            inputValues = list.toArray(new RecipeComponentValue[0]);
        }
        return inputValues;
    }

    public RecipeComponentValue<?>[] outputValues() {
        if (outputValues == null) {
            List<RecipeComponentValue<?>> list = new ArrayList<>();
            for (RecipeComponentValue<?> holder : valueMap.holders()) {
                if (holder.key().role() == RecipeComponent.Role.OUTPUT) {
                    list.add(holder);
                }
            }
            outputValues = list.toArray(new RecipeComponentValue[0]);
        }
        return outputValues;
    }

    public boolean hasInput(ReplacementMatch match) {
        for (RecipeComponentValue<?> holder : inputValues()) {
            if (holder.matches(match)) {
                return true;
            }
        }
        return false;
    }

    public boolean replaceInput(ReplacementMatch match, Object with) {
        boolean replaced = false;
        RecipeContext context = new RecipeContext(this);

        for (RecipeComponentValue<?> holder : inputValues()) {
            if (holder.matches(match)) {
                if (holder.replace(match, with, context)) {
                    replaced = true;
                }
            }
        }

        if (replaced) {
            save();
        }
        return replaced;
    }

    public boolean replaceInput(ReplacementMatchInfo match) {
        return replaceInput(match.match(), match.replacement());
    }

    public boolean hasOutput(ReplacementMatch match) {
        for (RecipeComponentValue<?> holder : outputValues()) {
            if (holder.matches(match)) {
                return true;
            }
        }
        return false;
    }

    public boolean replaceOutput(ReplacementMatch match, Object with) {
        boolean replaced = false;
        RecipeContext context = new RecipeContext(this);

        for (RecipeComponentValue<?> holder : outputValues()) {
            if (holder.matches(match)) {
                if (holder.replace(match, with, context)) {
                    replaced = true;
                }
            }
        }

        if (replaced) {
            save();
        }
        return replaced;
    }

    public boolean replaceOutput(ReplacementMatchInfo match) {
        return replaceOutput(match.match(), match.replacement());
    }

    // ================================================================
    // Ingredient Actions
    // ================================================================

    public KubeJavaRecipe ingredientAction(SlotFilter filter, IngredientAction action) {
        if (ingredientActions == null) {
            ingredientActions = new ArrayList<>();
        }
        ingredientActions.add(new IngredientActionHolder(action, filter));
        save();
        return this;
    }

    public KubeJavaRecipe damageIngredient(SlotFilter filter, int damage) {
        return ingredientAction(filter, new com.onehumanawa.cfhcore.foundation.recipe.action.DamageAction(damage));
    }

    public KubeJavaRecipe damageIngredient(SlotFilter filter) {
        return damageIngredient(filter, 1);
    }

    public KubeJavaRecipe replaceIngredient(SlotFilter filter, ItemStack item) {
        return ingredientAction(filter, new com.onehumanawa.cfhcore.foundation.recipe.action.ReplaceAction(item));
    }

    public KubeJavaRecipe keepIngredient(SlotFilter filter) {
        return ingredientAction(filter, com.onehumanawa.cfhcore.foundation.recipe.action.KeepAction.INSTANCE);
    }

    public KubeJavaRecipe consumeIngredient(SlotFilter filter) {
        return ingredientAction(filter, com.onehumanawa.cfhcore.foundation.recipe.action.ConsumeAction.INSTANCE);
    }

    public KubeJavaRecipe modifyResult(String id) {
        this.modifyResult = id;
        save();
        return this;
    }

    @Nullable
    public List<IngredientActionHolder> getIngredientActions() {
        return ingredientActions;
    }

    // ================================================================
    // Serialization / Deserialization
    // ================================================================

    public void initValues(boolean save) {
        if (save) {
            save();
        }

        if (!type.schema().keys().isEmpty()) {
            valueMap = new RecipeComponentValueMap(type.schema().keys());

            if (save) {
                for (RecipeComponentValue<?> holder : valueMap.holders()) {
                    if (holder.key().optional()) {
                        @SuppressWarnings("unchecked")
                        Object defaultValue = holder.key().defaultValue();
                        ((RecipeComponentValue<Object>) holder).value(defaultValue);
                    }
                    if (holder.key().alwaysWrite()) {
                        holder.written(true);
                    }
                }
            }
        }
    }

    public void deserialize(boolean merge) {
        RecipeContext context = new RecipeContext(this);

        for (RecipeComponentValue<?> holder : valueMap.holders()) {
            try {
                holder.readFromJson(json, context);
            } catch (Exception ex) {
                if (holder.key().optional()) {
                    LOGGER.warn("Failed to read component {} from recipe {}, using default",
                            holder.key().primaryName(), id, ex);
                } else {
                    throw new RecipeModifyComponentException(
                            "Failed to read required component: " + holder.key().primaryName(),
                            ex, holder
                    ).sourceLine(sourceLine);
                }
            }

            if (holder.value() != null && merge) {
                holder.written(true);
            }
        }
    }

    public void serialize() {
        RecipeContext context = new RecipeContext(this);

        for (RecipeComponentValue<?> holder : valueMap.holders()) {
            if (holder.shouldWrite()) {
                if (holder.value() == null && !holder.key().optional()) {
                    throw new RecipeModifyRuntimeException(
                            "Value not set for required component: " + holder.key().primaryName() + " in recipe " + id
                    ).sourceLine(sourceLine);
                }
                if (holder.value() != null) {
                    holder.writeToJson(json, context);
                }
            }
        }
    }

    public KubeJavaRecipe serializeChanges() {
        if (newRecipe || hasChanged()) {
            serialize();

            if (!modifyResult.isEmpty()) {
                json.addProperty("_kubejava_modify_result", modifyResult);
            }

            if (ingredientActions != null && !ingredientActions.isEmpty()) {
                var actionsArray = new com.google.gson.JsonArray();
                for (IngredientActionHolder holder : ingredientActions) {
                    var actionJson = holder.action().toJson();
                    actionJson.addProperty("slot", holder.filter().toString());
                    actionsArray.add(actionJson);
                }
                json.add("_kubejava_ingredient_actions", actionsArray);
            }

            if (newRecipe) {
                json.addProperty("type", type.serializerType());
            }

            json.add(CHANGED_MARKER, new com.google.gson.JsonPrimitive(sourceLine));
        }

        return this;
    }

    // ================================================================
    // State Management
    // ================================================================

    public void save() {
        this.changed = true;
    }

    public boolean hasChanged() {
        if (changed) {
            return true;
        }

        for (RecipeComponentValue<?> holder : valueMap.holders()) {
            if (holder.shouldWrite()) {
                return true;
            }
        }

        return false;
    }

    public void remove() {
        if (!removed) {
            removed = true;
            LOGGER.info("Removed recipe: {}", id);
        }
    }

    public RecipeComponentValueMap valueMap() {
        return valueMap;
    }

    // ================================================================
    // Original Recipe Access (simplified)
    // ================================================================

    @Nullable
    public Recipe<?> getOriginalRecipe() {
        // Simplified - returns null to avoid codec issues
        return null;
    }

    public ItemStack getOriginalRecipeResult() {
        return ItemStack.EMPTY;
    }

    public List<Ingredient> getOriginalRecipeIngredients() {
        return List.of();
    }

    // ================================================================
    // Validation
    // ================================================================

    public void validate(RecipeValidationContext context) {
        context.push(id.toString());

        for (RecipeComponentValue<?> holder : valueMap.holders()) {
            context.push(holder.key().primaryName());
            if (holder.value() != null) {
                @SuppressWarnings("unchecked")
                Object value = holder.value();
                ((RecipeComponent<Object>) holder.key().component()).validate(value, context);
            } else if (!holder.key().optional()) {
                context.error("Missing required component");
            }
            context.pop();
        }

        type.schema().postProcess(context, this);
        context.pop();
    }

    // ================================================================
    // Utility Methods
    // ================================================================

    public String getId() {
        return id.toString();
    }

    public String getPath() {
        return id.getPath();
    }

    public String getNamespace() {
        return id.getNamespace();
    }

    public boolean isFromMod(String modId) {
        return id.getNamespace().equals(modId);
    }

    @Nullable
    public RecipeHolder<?> toRecipeHolder() {
        try {
            serializeChanges();
            // Simplified - returns null to avoid codec issues
            return null;
        } catch (Exception e) {
            LOGGER.error("Failed to create RecipeHolder from JSON for: {}", id, e);
            return null;
        }
    }

    // ================================================================
    // Builder
    // ================================================================

    public static Builder builder(RecipeType type) {
        return new Builder(type);
    }

    public static class Builder {
        private final RecipeType type;
        private ResourceLocation id;
        private JsonObject json = new JsonObject();
        private boolean newRecipe = false;
        private String sourceLine = "unknown";

        private Builder(RecipeType type) {
            this.type = type;
        }

        public Builder id(String id) {
            this.id = ResourceLocation.parse(id);
            return this;
        }

        public Builder id(ResourceLocation id) {
            this.id = id;
            return this;
        }

        public Builder json(JsonObject json) {
            this.json = json;
            return this;
        }

        public Builder newRecipe(boolean newRecipe) {
            this.newRecipe = newRecipe;
            return this;
        }

        public Builder sourceLine(String sourceLine) {
            this.sourceLine = sourceLine;
            return this;
        }

        public KubeJavaRecipe build() {
            if (id == null) {
                id = ResourceLocation.fromNamespaceAndPath("kubejava", "auto_" + System.currentTimeMillis());
            }
            KubeJavaRecipe recipe = new KubeJavaRecipe(id, type, json);
            recipe.newRecipe = newRecipe;
            recipe.sourceLine = sourceLine;
            recipe.deserialize(false);
            return recipe;
        }
    }

    // ================================================================
    // Object Overrides
    // ================================================================

    @Override
    public String toString() {
        if (id == null && json == null) {
            return "<no id> [" + type + "]";
        }
        return id + "[" + type.id() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KubeJavaRecipe that)) return false;
        return Objects.equals(id, that.id) &&
                Objects.equals(type, that.type) &&
                Objects.equals(json, that.json);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, json);
    }
}