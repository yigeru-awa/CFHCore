package com.onehumanawa.cfhcore.foundation.recipe.schema.create;

import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.IntegerComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.StringComponent;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchema;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchemaType;

import java.util.Set;

/**
 * Schema for Create compacting recipes.
 * Reuses MixingSchema's components but with different type.
 */
@RecipeSchemaType("create:compacting")
public class CompactingSchema {

    public static final RecipeSchema INSTANCE = create();

    private static RecipeSchema create() {
        return RecipeSchema.builder()
                .type("create:compacting")
                .key(RecipeKey.builder(MixingSchema.MixingIngredientComponent.class.cast(new MixingSchema.MixingIngredientComponent()))
                        .name("ingredients")
                        .input()
                        .alwaysWrite()
                        .build())
                .key(RecipeKey.builder(MixingSchema.MixingResultComponent.class.cast(new MixingSchema.MixingResultComponent()))
                        .name("results")
                        .output()
                        .alwaysWrite()
                        .build())
                .key(RecipeKey.builder(new IntegerComponent("processing_time", 1, 36000, 100))
                        .name("processing_time")
                        .optional()
                        .defaultValue(100)
                        .build())
                .key(RecipeKey.builder(new StringComponent("heat_requirement", "", Set.of("none", "heated", "superheated")))
                        .name("heat_requirement")
                        .optional()
                        .defaultValue("none")
                        .build())
                .idGenerator(recipe -> {
                    var ingredients = recipe.getValue(RecipeKey.builder(MixingSchema.MixingIngredientComponent.class.cast(new MixingSchema.MixingIngredientComponent())).name("ingredients").build());
                    var results = recipe.getValue(RecipeKey.builder(MixingSchema.MixingResultComponent.class.cast(new MixingSchema.MixingResultComponent())).name("results").build());
                    StringBuilder sb = new StringBuilder("compact");
                    if (ingredients != null && !ingredients.items.isEmpty()) {
                        sb.append("_").append(ingredients.items.get(0).replace(':', '_'));
                    }
                    if (results != null && !results.items.isEmpty()) {
                        sb.append("_to_").append(results.items.get(0).replace(':', '_'));
                    }
                    return sb.toString();
                })
                .build();
    }
}