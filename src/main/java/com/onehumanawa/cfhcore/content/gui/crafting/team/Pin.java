package com.onehumanawa.cfhcore.content.gui.crafting.team;

import com.google.gson.JsonObject;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public final class Pin {
	public static final String RECIPE = "recipe";
	public static final String TREE = "tree";
	public String id = "";
	public String kind = "recipe";
	public String recipe = "";
	@Nullable
	public JsonObject tree;
	public int steps;
	public UUID by = new UUID(0L, 0L);
	public String byName = "";
	public long at;

	public boolean isTree() {
		return "tree".equals(this.kind);
	}

	public boolean sanitize() {
		if (this.id != null && !this.id.isBlank()) {
			if (!"tree".equals(this.kind)) {
				this.kind = "recipe";
			}

			if (this.recipe == null || this.recipe.isBlank()) {
				return false;
			} else if ("tree".equals(this.kind) && this.tree == null) {
				return false;
			} else {
				if (this.by == null) {
					this.by = new UUID(0L, 0L);
				}

				if (this.byName == null) {
					this.byName = "";
				}

				if (this.steps < 0) {
					this.steps = 0;
				}

				return true;
			}
		} else {
			return false;
		}
	}
}
