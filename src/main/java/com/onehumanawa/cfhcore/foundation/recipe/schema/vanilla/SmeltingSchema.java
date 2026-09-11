package com.onehumanawa.cfhcore.foundation.recipe.schema.vanilla;

import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.FloatComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.IntegerComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.ItemComponent;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchema;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchemaType;

public class SmeltingSchema {

    public static final RecipeKey<String> INGREDIENT_KEY =
            RecipeKey.builder(ItemComponent.INSTANCE).name("ingredient").input().alwaysWrite().build();
    public static final RecipeKey<String> RESULT_KEY =
            RecipeKey.builder(ItemComponent.INSTANCE).name("result").output().alwaysWrite().build();

    @RecipeSchemaType("minecraft:smelting")
    public static final RecipeSchema INSTANCE = create("minecraft:smelting", "smelt", 200, 0.35f);
    @RecipeSchemaType("minecraft:blasting")
    public static final RecipeSchema BLASTING = create("minecraft:blasting", "blast", 100, 1.0f);
    @RecipeSchemaType("minecraft:smoking")
    public static final RecipeSchema SMOKING = create("minecraft:smoking", "smoke", 100, 0.35f);
    @RecipeSchemaType("minecraft:campfire_cooking")
    public static final RecipeSchema CAMPFIRE = create("minecraft:campfire_cooking", "campfire", 600, 0.35f);

    private static RecipeSchema create(String type, String prefix, int defaultTime, float defaultExp) {
        return RecipeSchema.builder()
                .type(type)
                .key(INGREDIENT_KEY)
                .key(RESULT_KEY)
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
                    String input = recipe.getValue(INGREDIENT_KEY);
                    String result = recipe.getValue(RESULT_KEY);
                    if (input != null && result != null) {
                        return prefix + "_" + input.replace(':', '_').replace("#", "")
                                + "_to_" + result.replace(':', '_').replace("#", "");
                    }
                    return "";
                })
                .build();
    }
}
