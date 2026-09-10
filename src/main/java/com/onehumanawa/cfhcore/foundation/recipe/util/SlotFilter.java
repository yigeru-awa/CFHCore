package com.onehumanawa.cfhcore.foundation.recipe.util;

import java.util.HashSet;
import java.util.Set;

/**
 * Filter for matching specific slots in a recipe.
 */
public class SlotFilter {

    private final Set<Integer> slots = new HashSet<>();
    private boolean all = false;

    private SlotFilter() {}

    public static SlotFilter all() {
        SlotFilter filter = new SlotFilter();
        filter.all = true;
        return filter;
    }

    public static SlotFilter of(int... slots) {
        SlotFilter filter = new SlotFilter();
        for (int slot : slots) {
            filter.slots.add(slot);
        }
        return filter;
    }

    public static SlotFilter range(int start, int end) {
        SlotFilter filter = new SlotFilter();
        for (int i = start; i <= end; i++) {
            filter.slots.add(i);
        }
        return filter;
    }

    public SlotFilter and(int slot) {
        if (!all) {
            slots.add(slot);
        }
        return this;
    }

    public SlotFilter and(int... moreSlots) {
        if (!all) {
            for (int slot : moreSlots) {
                slots.add(slot);
            }
        }
        return this;
    }

    public boolean matches(int slot) {
        return all || slots.contains(slot);
    }

    @Override
    public String toString() {
        if (all) return "*";
        return slots.toString();
    }
}