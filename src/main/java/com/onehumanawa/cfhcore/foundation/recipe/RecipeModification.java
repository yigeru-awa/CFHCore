package com.onehumanawa.cfhcore.foundation.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.onehumanawa.cfhcore.foundation.recipe.action.ConsumeAction;
import com.onehumanawa.cfhcore.foundation.recipe.action.DamageAction;
import com.onehumanawa.cfhcore.foundation.recipe.action.IngredientAction;
import com.onehumanawa.cfhcore.foundation.recipe.action.IngredientActionHolder;
import com.onehumanawa.cfhcore.foundation.recipe.action.KeepAction;
import com.onehumanawa.cfhcore.foundation.recipe.action.ReplaceAction;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeComponentValue;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeComponentValueMap;
import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;
import com.onehumanawa.cfhcore.foundation.recipe.match.ReplacementMatch;
import com.onehumanawa.cfhcore.foundation.recipe.match.ReplacementMatchInfo;
import com.onehumanawa.cfhcore.foundation.recipe.util.SlotFilter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
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

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class RecipeModification {
    private static final Logger LOGGER = LoggerFactory.getLogger("RecipeModification");

    public static final String CHANGED_MARKER = "_changed_marker";
    public static final String MODIFY_RESULT_KEY = "_modify_result";
    public static final String INGREDIENT_ACTIONS_KEY = "_ingredient_actions";

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

    public RecipeModification(ResourceLocation id, RecipeType type, JsonObject json) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.json = json != null ? json.deepCopy() : new JsonObject();
        this.originalJson = json != null ? json.deepCopy() : null;
        initValues(true);
    }

    public RecipeModification(String id, RecipeType type, JsonObject json) {
        this(ResourceLocation.parse(id), type, json);
    }

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

    public <T> RecipeModification setValue(RecipeKey<T> key, T value) {
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

    public RecipeModification set(String name, Object value) {
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

    public RecipeComponentValue<?>[] inputValues() {
        if (inputValues == null) {
            List<RecipeComponentValue<?>> list = new ArrayList<>();
            for (RecipeComponentValue<?> holder : valueMap.holders()) {
                if (holder.key().role() == RecipeComponent.Role.INPUT) {
                    list.add(holder);
                }
            }
            inputValues = list.toArray(new RecipeComponentValue<?>[0]);
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
            outputValues = list.toArray(new RecipeComponentValue<?>[0]);
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

    public RecipeModification ingredientAction(SlotFilter filter, IngredientAction action) {
        if (ingredientActions == null) {
            ingredientActions = new ArrayList<>();
        }
        ingredientActions.add(new IngredientActionHolder(action, filter));
        save();
        return this;
    }

    public RecipeModification damageIngredient(SlotFilter filter, int damage) {
        return ingredientAction(filter, new DamageAction(damage));
    }

    public RecipeModification damageIngredient(SlotFilter filter) {
        return damageIngredient(filter, 1);
    }

    public RecipeModification replaceIngredient(SlotFilter filter, ItemStack item) {
        return ingredientAction(filter, new ReplaceAction(item));
    }

    public RecipeModification keepIngredient(SlotFilter filter) {
        return ingredientAction(filter, KeepAction.INSTANCE);
    }

    public RecipeModification consumeIngredient(SlotFilter filter) {
        return ingredientAction(filter, ConsumeAction.INSTANCE);
    }

    public RecipeModification modifyResult(String id) {
        this.modifyResult = id;
        save();
        return this;
    }

    @Nullable
    public List<IngredientActionHolder> getIngredientActions() {
        return ingredientActions;
    }

    public void initValues(boolean save) {
        if (save) {
            save();
        }

        if (!type.schema().keys().isEmpty()) {
            valueMap = new RecipeComponentValueMap(type.schema().keys());

            if (save) {
                for (RecipeComponentValue<?> holder : valueMap.holders()) {
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

    public RecipeModification serializeChanges() {
        if (newRecipe || hasChanged()) {
            serialize();

            if (!modifyResult.isEmpty()) {
                json.addProperty(MODIFY_RESULT_KEY, modifyResult);
            }

            if (ingredientActions != null && !ingredientActions.isEmpty()) {
                JsonArray actionsArray = new JsonArray();
                for (IngredientActionHolder holder : ingredientActions) {
                    JsonObject actionJson = holder.action().toJson();
                    actionJson.addProperty("slot", holder.filter().toString());
                    actionsArray.add(actionJson);
                }
                json.add(INGREDIENT_ACTIONS_KEY, actionsArray);
            }

            if (!json.has("type")) {
                json.addProperty("type", type.serializerType());
            }

            json.add(CHANGED_MARKER, new JsonPrimitive(sourceLine));
        }

        return this;
    }

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

    @Nullable
    public Recipe<?> getOriginalRecipe() {
        return RecipeModificationHandler.getRecipeById(id).map(RecipeHolder::value).orElse(null);
    }

    public ItemStack getOriginalRecipeResult() {
        Recipe<?> recipe = getOriginalRecipe();
        if (recipe == null) {
            return ItemStack.EMPTY;
        }
        HolderLookup.Provider registries = RecipeModificationHandler.registries();
        return recipe.getResultItem(registries != null ? registries : RegistryAccess.EMPTY);
    }

    public List<Ingredient> getOriginalRecipeIngredients() {
        Recipe<?> recipe = getOriginalRecipe();
        if (recipe == null) {
            return List.of();
        }
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        return ingredients == null ? List.of() : List.copyOf(ingredients);
    }

    public void validate(RecipeValidationContext context) {
        context.push(id.toString());

        for (RecipeComponentValue<?> holder : valueMap.holders()) {
            context.push(holder.key().primaryName());
            holder.validate(context, this);
            context.pop();
        }

        type.schema().postProcess(context, this);
        context.pop();
    }

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

            JsonObject payload = json.deepCopy();
            payload.remove(CHANGED_MARKER);
            payload.remove(MODIFY_RESULT_KEY);
            payload.remove(INGREDIENT_ACTIONS_KEY);
            if (!payload.has("type")) {
                payload.addProperty("type", type.serializerType());
            }

            var parsed = Recipe.CODEC.parse(RecipeModificationHandler.registryOps(), payload);
            if (parsed.result().isPresent()) {
                return new RecipeHolder<>(id, parsed.result().get());
            }

            LOGGER.warn("Failed to parse recipe {}: {}", id,
                    parsed.error().map(err -> err.message()).orElse("unknown error"));
            return null;
        } catch (Exception e) {
            LOGGER.error("Failed to create RecipeHolder from JSON for: {}", id, e);
            return null;
        }
    }

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

        public RecipeModification build() {
            if (id == null) {
                id = ResourceLocation.fromNamespaceAndPath("kubejava", "auto_" + System.currentTimeMillis());
            }
            RecipeModification recipe = new RecipeModification(id, type, json);
            recipe.newRecipe = newRecipe;
            recipe.sourceLine = sourceLine;
            recipe.deserialize(false);
            return recipe;
        }
    }

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
        if (!(o instanceof RecipeModification that)) return false;
        return Objects.equals(id, that.id) &&
                Objects.equals(type, that.type) &&
                Objects.equals(json, that.json);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, json);
    }
}
