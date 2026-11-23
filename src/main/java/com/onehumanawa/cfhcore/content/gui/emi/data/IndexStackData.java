package com.onehumanawa.cfhcore.content.gui.emi.data;

import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import java.util.List;
import java.util.function.Predicate;

public record IndexStackData(boolean disable, List<IndexStackData.Added> added, List<EmiIngredient> removed, List<IndexStackData.Filter> filters) {
	public static record Added(EmiIngredient added, EmiIngredient after) {
	}

	public static record Filter(Predicate<String> filter) {
	}
}
