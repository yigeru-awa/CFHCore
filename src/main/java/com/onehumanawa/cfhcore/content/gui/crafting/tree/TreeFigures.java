package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public final class TreeFigures {
	public TreeFigures.State state = TreeFigures.State.WAITING;
	public long wanted;
	public long missing;
	public long held;
	public float chance = 1.0F;
	public long crafts;
	public boolean readyForAll;
	public boolean here;
	@Nullable
	public EmiRecipe recipe;

	public long have() {
		return Math.max(0L, Math.min(this.wanted, this.held));
	}

	public long leftOver() {
		return Math.max(0L, this.wanted - this.missing - this.have());
	}

	public boolean unasked() {
		return this.wanted <= 0L;
	}

	public boolean estimated() {
		return this.chance != 1.0F;
	}

	public float fraction() {
		return this.state == TreeFigures.State.DONE ? 1.0F : (this.wanted <= 0L ? 0.0F : Math.max(0.0F, Math.min(1.0F, (float)this.have() / (float)this.wanted)));
	}

	public static final class Fold {
		public int steps;
		public int done;
		public boolean ready;
		@Nullable
		public TreeNode next;
		public final List<TreeFigures.Raw> raws = new ArrayList<>();
		public final List<TreeNode> first = new ArrayList<>();

		public float fraction() {
			return this.steps <= 0 ? (this.missingRaw() ? 0.0F : 1.0F) : (float)this.done / (float)this.steps;
		}

		public boolean complete() {
			return this.done >= this.steps && !this.missingRaw();
		}

		private boolean missingRaw() {
			for (TreeFigures.Raw raw : this.raws) {
				if (raw.missing > 0L) {
					return true;
				}
			}

			return false;
		}
	}

	public static final class Raw {
		public final EmiIngredient ingredient;
		public long wanted;
		public long missing;
		public boolean estimated;

		public Raw(EmiIngredient ingredient) {
			this.ingredient = ingredient;
		}

		public long have() {
			return Math.max(0L, this.wanted - this.missing);
		}
	}

	public static enum State {
		DONE,
		COVERED,
		READY,
		WAITING,
		MISSING,
		GIVEN;
	}
}
