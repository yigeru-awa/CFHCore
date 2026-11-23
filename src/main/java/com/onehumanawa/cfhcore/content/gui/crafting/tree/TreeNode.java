package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiResolutionRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.bom.MaterialNode;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public final class TreeNode {
	public final String path;
	public final int depth;
	@Nullable
	public final TreeNode parent;
	public final List<TreeNode> children = new ArrayList<>();
	@Nullable
	public final MaterialNode node;
	@Nullable
	public final MaterialNode tag;
	public int order;
	public boolean given;
	private final boolean madeUpStep;

	TreeNode(String path, int depth, @Nullable TreeNode parent, @Nullable MaterialNode node, @Nullable MaterialNode tag, boolean madeUpStep) {
		this.path = path;
		this.depth = depth;
		this.parent = parent;
		this.node = node;
		this.tag = tag;
		this.madeUpStep = madeUpStep;
	}

	@Nullable
	public EmiRecipe recipe() {
		return this.node != null && !(this.node.recipe instanceof EmiResolutionRecipe) ? this.node.recipe : null;
	}

	public boolean step() {
		return !this.given && (this.node == null ? this.madeUpStep : this.recipe() != null);
	}

	public boolean hasRecipe() {
		return this.node == null ? this.madeUpStep : this.recipe() != null;
	}

	@Nullable
	public EmiIngredient ingredient() {
		return this.node == null ? null : this.node.ingredient;
	}

	@Nullable
	public EmiIngredient asked() {
		return this.tag != null ? this.tag.ingredient : this.ingredient();
	}

	public boolean hasChildren() {
		return this.step() && !this.children.isEmpty();
	}
}
