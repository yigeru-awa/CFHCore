package com.onehumanawa.cfhcore.content.gui.emi.search;

import com.google.common.collect.Sets;
import com.onehumanawa.cfhcore.content.gui.emi.EmiUtil;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import java.util.Set;

public class ModQuery extends Query {
	private final Set<EmiStack> valid = Sets.newIdentityHashSet();
	private final String name;

	public ModQuery(String name) {
		this.name = name.toLowerCase();
		EmiSearch.mods.search(this.name).forEach(s -> this.valid.add(s.stack));
	}

	@Override
	public boolean matches(EmiStack stack) {
		return this.valid.contains(stack);
	}

	@Override
	public boolean matchesUnbaked(EmiStack stack) {
		return EmiUtil.getModName(stack.getId().getNamespace()).toLowerCase().contains(this.name);
	}
}
