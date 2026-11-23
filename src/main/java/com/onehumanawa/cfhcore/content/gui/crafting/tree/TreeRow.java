package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import java.util.List;
import org.jetbrains.annotations.Nullable;

public final class TreeRow {
	public final String id;
	public final int depth;
	@Nullable
	public final TreeNode node;
	@Nullable
	public final Island island;
	public final List<TreeNode> hidden;
	public int index;
	public int parent = -1;
	public int prevSibling = -1;
	public int nextSibling = -1;
	public boolean folded;
	public boolean hasChildren;
	public List<Island> within = List.of();

	TreeRow(TreeNode node, int depth) {
		this.id = node.path;
		this.depth = depth;
		this.node = node;
		this.island = null;
		this.hidden = List.of();
	}

	TreeRow(Island island, List<TreeNode> hidden, int depth) {
		this.id = island.id();
		this.depth = depth;
		this.node = null;
		this.island = island;
		this.hidden = hidden;
	}

	public boolean isIsland() {
		return this.island != null;
	}

	public String path() {
		return this.node != null ? this.node.path : this.island.top();
	}
}
