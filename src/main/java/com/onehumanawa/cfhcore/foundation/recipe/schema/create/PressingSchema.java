package com.onehumanawa.cfhcore.foundation.recipe.schema.create;

import com.onehumanawa.cfhcore.foundation.recipe.component.RecipeKey;
import com.onehumanawa.cfhcore.foundation.recipe.component.impl.ItemComponent;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchema;
import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchemaType;

@RecipeSchemaType("create:pressing")
public class PressingSchema {

    public static final RecipeKey<String> INPUT_KEY =
            RecipeKey.builder(ItemComponent.INSTANCE).names("ingredients", "ingredient").input().alwaysWrite().build();
    public static final RecipeKey<String> RESULT_KEY =
            RecipeKey.builder(ItemComponent.INSTANCE).name("result").output().alwaysWrite().build();

    public static final RecipeSchema INSTANCE = create();

    private static RecipeSchema create() {
        return RecipeSchema.builder()
                .type("create:pressing")
                .key(INPUT_KEY)
                .key(RESULT_KEY)
                .idGenerator(recipe -> {
                    String input = recipe.getValue(INPUT_KEY);
                    String result = recipe.getValue(RESULT_KEY);
                    if (input != null && result != null) {
                        return "press_" + input.replace(':', '_') + "_to_" + result.replace(':', '_');
                    }
                    return "";
                })
                .build();
    }
}
