package com.onehumanawa.cfhcore.content.gui.server.model;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public final class KitBook {
	public List<Kit> kits = new ArrayList<>();

	@Nullable
	public Kit get(String id) {
		for (Kit k : this.kits) {
			if (k.id.equalsIgnoreCase(id)) {
				return k;
			}
		}

		return null;
	}

	public KitBook sanitize() {
		if (this.kits == null) {
			this.kits = new ArrayList<>();
		}

		this.kits.removeIf(kx -> kx == null || kx.id == null || kx.id.isBlank());

		for (Kit k : this.kits) {
			k.sanitize();
		}

		return this;
	}
}
