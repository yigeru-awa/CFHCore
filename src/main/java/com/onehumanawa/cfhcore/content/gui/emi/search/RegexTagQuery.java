package com.onehumanawa.cfhcore.content.gui.emi.search;

import com.onehumanawa.cfhcore.content.gui.emi.EmiPort;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.registry.EmiTags;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiTagKey;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.world.item.BlockItem;

public class RegexTagQuery extends Query {
	private final Set<Object> valid;

	public RegexTagQuery(String name) {
		Pattern p = null;

		try {
			p = Pattern.compile(name, 2);
		} catch (Exception var4) {
		}

		if (p == null) {
			this.valid = Set.of();
		} else {
			Pattern pat = p;
			this.valid = Stream.concat(EmiTags.TAGS.stream(), EmiTagKey.fromRegistry(EmiPort.getBlockRegistry()))
				.filter(t -> t.hasTranslation() && pat.matcher(t.getTagName().getString().toLowerCase()).find() ? true : pat.matcher(t.id().toString()).find())
				.flatMap(v -> v.stream())
				.collect(Collectors.toSet());
		}
	}

	@Override
	public boolean matches(EmiStack stack) {
		if (stack.getKey() instanceof BlockItem bi && this.valid.contains(bi.getBlock())) {
			return true;
		}

		return this.valid.contains(stack.getKey());
	}
}
