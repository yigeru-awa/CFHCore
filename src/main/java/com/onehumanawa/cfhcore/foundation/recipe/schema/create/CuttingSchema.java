package com.onehumanawa.cfhcore.foundation.recipe.schema.create;

import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.IntegerComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.ItemArrayComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.ItemComponent;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchema;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchemaType;

/**
 * Schema for Create cutting recipes (mechanical saw).
 * Single ingredient, up to 4 outputs, processing time can be specified.
 */
@RecipeSchemaType("create:cutting")
public class CuttingSchema {

    public static final RecipeSchema INSTANCE = create();

    private static RecipeSchema create() {
        return RecipeSchema.builder()
                .type("create:cutting")
                .key(RecipeKey.builder(ItemComponent.INSTANCE)
                        .name("ingredients")
                        .name("ingredient")
                        .input()
                        .alwaysWrite()
                        .build())
                .key(RecipeKey.builder(ItemArrayComponent.INSTANCE)
                        .name("results")
                        .output()
                        .alwaysWrite()
                        .build())
                .key(RecipeKey.builder(new IntegerComponent("processing_time", 1, 36000, 100))
                        .name("processing_time")
                        .optional()
                        .defaultValue(100)
                        .build())
                .idGenerator(recipe -> {
                    String input = recipe.getValue(RecipeKey.builder(ItemComponent.INSTANCE).name("ingredients").build());
                    String[] results = recipe.getValue(RecipeKey.builder(ItemArrayComponent.INSTANCE).name("results").build());
                    if (input != null && results != null && results.length > 0) {
                        return "cut_" + input.replace(':', '_') + "_to_" + results[0].replace(':', '_');
                    }
                    return "";
                })
                .build();
    }
}
