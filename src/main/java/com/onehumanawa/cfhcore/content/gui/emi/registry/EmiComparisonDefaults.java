package com.onehumanawa.cfhcore.content.gui.emi.registry;

import com.onehumanawa.cfhcore.content.gui.emi.api.stack.Comparison;
import java.util.Map;

public class EmiComparisonDefaults {
	public static Map<Object, Comparison> comparisons = Map.of();

	public static Comparison get(Object obj) {
		return comparisons.containsKey(obj) ? comparisons.get(obj) : Comparison.DEFAULT_COMPARISON;
	}
}
