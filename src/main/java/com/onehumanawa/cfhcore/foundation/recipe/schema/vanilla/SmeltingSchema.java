package com.onehumanawa.cfhcore.foundation.recipe.schema.vanilla;

import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.FloatComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.IntegerComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.ItemComponent;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchema;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchemaType;

/**
 * Schema for vanilla smelting recipes.
 */
public class SmeltingSchema {

    @RecipeSchemaType("minecraft:smelting")
    public static final RecipeSchema INSTANCE = create("minecraft:smelting", 200, 0.35f);
    @RecipeSchemaType("minecraft:blasting")
    public static final RecipeSchema BLASTING = create("minecraft:blasting", 100, 1.0f);
    @RecipeSchemaType("minecraft:smoking")
    public static final RecipeSchema SMOKING = create("minecraft:smoking", 100, 0.35f);
    @RecipeSchemaType("minecraft:campfire_cooking")
    public static final RecipeSchema CAMPFIRE = create("minecraft:campfire_cooking", 600, 0.35f);

    private static RecipeSchema create(String type, int defaultTime, float defaultExp) {
        return RecipeSchema.builder()
                .type(type)
                .key(RecipeKey.builder(ItemComponent.INSTANCE)
                        .name("ingredient")
                        .input()
                        .alwaysWrite()
                        .build())
                .key(RecipeKey.builder(ItemComponent.INSTANCE)
                        .name("result")
                        .output()
                        .alwaysWrite()
                        .build())
                .key(RecipeKey.builder(new FloatComponent("experience", 0, Float.MAX_VALUE, defaultExp))
                        .name("experience")
                        .optional()
                        .defaultValue(defaultExp)
                        .build())
                .key(RecipeKey.builder(new IntegerComponent("cookingtime", 1, Integer.MAX_VALUE, defaultTime))
                        .name("cookingtime")
                        .name("cookingTime")
                        .optional()
                        .defaultValue(defaultTime)
                        .build())
                .idGenerator(recipe -> {
                    String input = recipe.getValue(RecipeKey.builder(ItemComponent.INSTANCE).name("ingredient").build());
                    String result = recipe.getValue(RecipeKey.builder(ItemComponent.INSTANCE).name("result").build());
                    if (input != null && result != null) {
                        return "smelt_" + input.replace(':', '_') + "_to_" + result.replace(':', '_');
                    }
                    return "";
                })
                .build();
    }
}