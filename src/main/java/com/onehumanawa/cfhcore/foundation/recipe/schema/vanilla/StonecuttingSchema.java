package com.onehumanawa.cfhcore.foundation.recipe.schema.vanilla;

import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.IntegerComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.ItemComponent;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchema;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchemaType;

@RecipeSchemaType("minecraft:stonecutting")
public class StonecuttingSchema {

    public static final RecipeKey<String> INGREDIENT_KEY =
            RecipeKey.builder(ItemComponent.INSTANCE).name("ingredient").input().alwaysWrite().build();
    public static final RecipeKey<String> RESULT_KEY =
            RecipeKey.builder(ItemComponent.INSTANCE).name("result").output().alwaysWrite().build();
    public static final RecipeKey<Integer> COUNT_KEY =
            RecipeKey.builder(new IntegerComponent("count", 1, 64, 1))
                    .name("count")
                    .optional()
                    .defaultValue(1)
                    .build();

    public static final RecipeSchema INSTANCE = create();

    private static RecipeSchema create() {
        return RecipeSchema.builder()
                .type("minecraft:stonecutting")
                .key(INGREDIENT_KEY)
                .key(RESULT_KEY)
                .key(COUNT_KEY)
                .idGenerator(recipe -> {
                    String input = recipe.getValue(INGREDIENT_KEY);
                    String result = recipe.getValue(RESULT_KEY);
                    if (input != null && result != null) {
                        return "stonecut_" + input.replace(':', '_').replace("#", "")
                                + "_to_" + result.replace(':', '_').replace("#", "");
                    }
                    return "";
                })
                .build();
    }
}
