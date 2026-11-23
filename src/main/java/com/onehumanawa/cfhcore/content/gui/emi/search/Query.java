package com.onehumanawa.cfhcore.content.gui.emi.search;

import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;

public abstract class Query {
	public boolean negated;

	public abstract boolean matches(EmiStack var1);

	public boolean matchesUnbaked(EmiStack stack) {
		return this.matches(stack);
	}
}
