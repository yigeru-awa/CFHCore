package com.onehumanawa.cfhcore.content.gui.crafting.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.searchtree.SuffixArray;

public final class SharedText<T> extends SuffixArray<T> {
	private Map<String, List<T>> byText = new HashMap<>();
	private final SuffixArray<List<T>> array = new SuffixArray();

	public void add(T entry, String text) {
		this.byText.computeIfAbsent(text, t -> new ArrayList<>(1)).add(entry);
	}

	public void generate() {
		for (Entry<String, List<T>> e : this.byText.entrySet()) {
			List<T> entries = e.getValue();
			this.array.add(entries.size() == 1 ? List.of(entries.get(0)) : List.copyOf(entries), e.getKey());
		}

		this.byText = null;
		this.array.generate();
	}

	public List<T> search(String text) {
		List<List<T>> found = this.array.search(text);
		if (found.isEmpty()) {
			return List.of();
		} else if (found.size() == 1) {
			return found.get(0);
		} else {
			List<T> out = new ArrayList<>();

			for (List<T> entries : found) {
				out.addAll(entries);
			}

			return out;
		}
	}
}
