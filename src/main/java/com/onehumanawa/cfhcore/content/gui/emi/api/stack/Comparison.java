package com.onehumanawa.cfhcore.content.gui.emi.api.stack;

import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiLog;
import java.util.Objects;
import java.util.function.Function;
import org.jetbrains.annotations.ApiStatus.Internal;

public class Comparison {
	private static final Comparison COMPARE_COMPONENTS = compareData(stack -> stack.getComponentChanges());
	public static final Comparison DEFAULT_COMPARISON = of((a, b) -> true);
	private Comparison.Predicate predicate;
	private Comparison.HashFunction hash;

	private Comparison(Comparison.Predicate comparator, Comparison.HashFunction hash) {
		this.predicate = comparator;
		this.hash = hash;
	}

	public static Comparison of(Comparison.Predicate comparator) {
		return of(comparator, i -> 0);
	}

	public static Comparison of(Comparison.Predicate comparator, Comparison.HashFunction hashFunction) {
		return new Comparison(comparator, hashFunction);
	}

	public static <T> Comparison compareData(Function<EmiStack, T> function) {
		return of((a, b) -> Objects.equals(function.apply(a), function.apply(b)), stack -> Objects.hashCode(function.apply(stack)));
	}

	public static Comparison compareComponents() {
		return COMPARE_COMPONENTS;
	}

	public boolean compare(EmiStack a, EmiStack b) {
		try {
			return this.predicate.test(a, b);
		} catch (Throwable var4) {
			this.predicate = (na, nb) -> true;
			EmiLog.error("Comparison threw an exception, disabling", var4);
			return true;
		}
	}

	@Internal
	public int getHash(EmiStack stack) {
		try {
			return this.hash.hash(stack);
		} catch (Throwable var3) {
			this.hash = s -> 0;
			EmiLog.error("Comparison threw an exception, disabling", var3);
			return 0;
		}
	}

	static class Builder {
	}

	public interface HashFunction {
		int hash(EmiStack var1);
	}

	public interface Predicate {
		boolean test(EmiStack var1, EmiStack var2);
	}
}
