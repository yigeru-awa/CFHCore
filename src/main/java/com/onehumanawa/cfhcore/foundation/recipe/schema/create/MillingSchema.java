package com.onehumanawa.cfhcore.foundation.recipe.schema.create;

import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.ItemComponent;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchema;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchemaType;

/**
 * Schema for Create milling recipes.
 */
@RecipeSchemaType("create:milling")
public class MillingSchema {

    public static final RecipeSchema INSTANCE = create();

    private static RecipeSchema create() {
        return RecipeSchema.builder()
                .type("create:milling")
                .key(RecipeKey.builder(ItemComponent.INSTANCE)
                        .name("ingredients")
                        .name("ingredient")
                        .input()
                        .alwaysWrite()
                        .build())
                .key(RecipeKey.builder(ItemComponent.INSTANCE)
                        .name("result")
                        .output()
                        .alwaysWrite()
                        .build())
                .idGenerator(recipe -> {
                    String input = recipe.getValue(RecipeKey.builder(ItemComponent.INSTANCE).name("ingredients").build());
                    String result = recipe.getValue(RecipeKey.builder(ItemComponent.INSTANCE).name("result").build());
                    if (input != null && result != null) {
                        return "mill_" + input.replace(':', '_') + "_to_" + result.replace(':', '_');
                    }
                    return "";
                })
                .build();
    }
}