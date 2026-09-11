package com.onehumanawa.cfhcore.foundation.recipe.schema.create;

import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.IntegerComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.ItemArrayComponent;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.ItemComponent;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchema;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchemaType;

@RecipeSchemaType("create:sequenced_assembly")
public class SequencedAssemblySchema {

    public static final RecipeKey<String> INGREDIENT_KEY =
            RecipeKey.builder(ItemComponent.INSTANCE).name("ingredient").input().alwaysWrite().build();
    public static final RecipeKey<String> TRANSITIONAL_ITEM_KEY =
            RecipeKey.builder(ItemComponent.INSTANCE).name("transitional_item").stackFormat().alwaysWrite().build();
    public static final RecipeKey<String[]> RESULTS_KEY =
            RecipeKey.builder(ItemArrayComponent.INSTANCE).name("results").output().alwaysWrite().build();
    public static final RecipeKey<Integer> LOOPS_KEY =
            RecipeKey.builder(new IntegerComponent("loops", 1, 1000, 1))
                    .name("loops")
                    .optional()
                    .defaultValue(1)
                    .build();

    public static final RecipeSchema INSTANCE = create();

    private static RecipeSchema create() {
        return RecipeSchema.builder()
                .type("create:sequenced_assembly")
                .key(INGREDIENT_KEY)
                .key(TRANSITIONAL_ITEM_KEY)
                .key(RESULTS_KEY)
                .key(LOOPS_KEY)
                .postProcessor((ctx, recipe) -> {
                    String ingredient = recipe.getValue(INGREDIENT_KEY);
                    String transitional = recipe.getValue(TRANSITIONAL_ITEM_KEY);
                    String[] results = recipe.getValue(RESULTS_KEY);
                    if (ingredient == null || ingredient.isEmpty()) {
                        ctx.error("Sequenced assembly recipe has no ingredient");
                    }
                    if (transitional == null || transitional.isEmpty()) {
                        ctx.error("Sequenced assembly recipe has no transitional_item");
                    }
                    if (results == null || results.length == 0) {
                        ctx.error("Sequenced assembly recipe has no results");
                    }
                })
                .idGenerator(recipe -> {
                    String ingredient = recipe.getValue(INGREDIENT_KEY);
                    if (ingredient != null) {
                        return "assembly_" + ingredient.replace(':', '_').replace('#', '_');
                    }
                    return "";
                })
                .build();
    }
}
