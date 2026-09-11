package com.onehumanawa.cfhcore.foundation.recipe.schema.vanilla;

import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.ItemArrayComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.ItemComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.StringComponent;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchema;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchemaType;

@RecipeSchemaType("minecraft:crafting_shapeless")
public class ShapelessSchema {

    public static final RecipeKey<String[]> INGREDIENTS_KEY =
            RecipeKey.builder(ItemArrayComponent.INSTANCE).name("ingredients").input().alwaysWrite().build();
    public static final RecipeKey<String> RESULT_KEY =
            RecipeKey.builder(ItemComponent.INSTANCE).name("result").output().alwaysWrite().build();
    public static final RecipeKey<String> GROUP_KEY =
            RecipeKey.builder(new StringComponent("group")).name("group").optional().build();

    public static final RecipeSchema INSTANCE = create();

    private static RecipeSchema create() {
        return RecipeSchema.builder()
                .type("minecraft:crafting_shapeless")
                .key(INGREDIENTS_KEY)
                .key(RESULT_KEY)
                .key(GROUP_KEY)
                .postProcessor((ctx, recipe) -> {
                    String[] ingredients = recipe.getValue(INGREDIENTS_KEY);
                    if (ingredients == null || ingredients.length == 0) {
                        ctx.error("Shapeless recipe has no ingredients");
                    }
                })
                .idGenerator(recipe -> {
                    String[] ingredients = recipe.getValue(INGREDIENTS_KEY);
                    String result = recipe.getValue(RESULT_KEY);
                    if (ingredients != null && result != null) {
                        return String.join("_", ingredients).replace(':', '_').replace("#", "")
                                + "_to_" + result.replace(':', '_').replace("#", "");
                    }
                    return "";
                })
                .build();
    }
}
