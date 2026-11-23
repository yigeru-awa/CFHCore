package com.onehumanawa.cfhcore.content.gui.emi.search;

import com.onehumanawa.cfhcore.content.gui.emi.EmiPort;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.registry.EmiTags;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiTagKey;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.world.item.BlockItem;

public class TagQuery extends Query {
	private final Set<Object> valid;

	public TagQuery(String name) {
		String lowerName = name.toLowerCase();
		this.valid = Stream.concat(EmiTags.TAGS.stream(), EmiTagKey.fromRegistry(EmiPort.getBlockRegistry()))
			.filter(t -> t.hasTranslation() && t.getTagName().getString().toLowerCase().contains(lowerName) ? true : t.id().toString().contains(lowerName))
			.flatMap(v -> v.stream())
			.collect(Collectors.toSet());
	}

	@Override
	public boolean matches(EmiStack stack) {
		if (stack.getKey() instanceof BlockItem bi && this.valid.contains(bi.getBlock())) {
			return true;
		}

		return this.valid.contains(stack.getKey());
	}
}
