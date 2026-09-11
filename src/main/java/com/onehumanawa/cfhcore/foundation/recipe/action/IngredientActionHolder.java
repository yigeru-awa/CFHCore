package com.onehumanawa.cfhcore.foundation.recipe.action;

import com.onehumanawa.cfhcore.foundation.recipe.util.SlotFilter;

public class IngredientActionHolder {

    private final IngredientAction action;
    private final SlotFilter filter;

    public IngredientActionHolder(IngredientAction action, SlotFilter filter) {
        this.action = action;
        this.filter = filter;
    }

    public IngredientAction action() {
        return action;
    }

    public SlotFilter filter() {
        return filter;
    }

    public boolean matches(int slot) {
        return filter.matches(slot);
    }

    @Override
    public String toString() {
        return "IngredientActionHolder{" + filter + " -> " + action + "}";
    }
}