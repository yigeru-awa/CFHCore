package com.onehumanawa.cfhcore.foundation.recipe.schema.create;

import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.ItemArrayComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.ItemComponent;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchema;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchemaType;

@RecipeSchemaType("create:splashing")
public class SplashingSchema {

    public static final RecipeKey<String> INPUT_KEY =
            RecipeKey.builder(ItemComponent.INSTANCE).names("ingredients", "ingredient").input().alwaysWrite().build();
    public static final RecipeKey<String[]> RESULTS_KEY =
            RecipeKey.builder(ItemArrayComponent.INSTANCE).name("results").output().alwaysWrite().build();

    public static final RecipeSchema INSTANCE = create();

    private static RecipeSchema create() {
        return RecipeSchema.builder()
                .type("create:splashing")
                .key(INPUT_KEY)
                .key(RESULTS_KEY)
                .idGenerator(recipe -> {
                    String input = recipe.getValue(INPUT_KEY);
                    String[] results = recipe.getValue(RESULTS_KEY);
                    if (input != null && results != null && results.length > 0) {
                        return "splash_" + input.replace(':', '_') + "_to_" + results[0].replace(':', '_');
                    }
                    return "";
                })
                .build();
    }
}
