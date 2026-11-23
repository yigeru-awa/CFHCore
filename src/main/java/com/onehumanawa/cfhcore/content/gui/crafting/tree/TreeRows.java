package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

public final class TreeRows {
	@Nullable
	public static TreeRows.Refusal refusal(TreeOutline outline, List<Island> islands, String first, String second) {
		if (first.equals(second)) {
			return TreeRows.Refusal.SAME_POINT;
		} else if (outline.has(first) && outline.has(second)) {
			Island island = new Island(first, second);
			if (hidden(outline, island).isEmpty()) {
				return TreeRows.Refusal.NOTHING_BETWEEN;
			} else {
				for (Island other : islands) {
					Island.Relation relation = island.to(other);
					if (relation == Island.Relation.SAME) {
						return TreeRows.Refusal.ALREADY_THERE;
					}

					if (relation == Island.Relation.CROSSING) {
						return TreeRows.Refusal.CROSSES;
					}
				}

				return null;
			}
		} else {
			return TreeRows.Refusal.NOT_IN_TREE;
		}
	}

	public static boolean live(TreeOutline outline, Island island) {
		return shown(outline.at(island.a)) && shown(outline.at(island.b));
	}

	private static boolean shown(@Nullable TreeNode node) {
		if (node == null) {
			return false;
		} else {
			for (TreeNode up = node.parent; up != null; up = up.parent) {
				if (up.given) {
					return false;
				}
			}

			return true;
		}
	}

	public static List<TreeNode> hidden(TreeOutline outline, Island island) {
		List<TreeNode> out = new ArrayList<>();
		TreeNode top = outline.at(island.top());
		if (top != null) {
			collect(top, island.bottoms(), out);
		}

		return out;
	}

	private static void collect(TreeNode at, List<String> bottoms, List<TreeNode> out) {
		if (at.hasChildren()) {
			for (TreeNode child : at.children) {
				if (!bottoms.contains(child.path)) {
					out.add(child);
					collect(child, bottoms, out);
				}
			}
		}
	}

	public static List<TreeRow> flatten(TreeOutline outline, Set<String> folded, List<Island> islands) {
		List<Island> live = new ArrayList<>();

		for (Island island : islands) {
			if (live(outline, island)) {
				live.add(island);
			}
		}

		List<TreeRow> rows = new ArrayList<>();
		visit(outline, outline.goal, 0, -1, folded, live, rows);
		return rows;
	}

	private static void visit(TreeOutline outline, TreeNode node, int depth, int parent, Set<String> folded, List<Island> islands, List<TreeRow> rows) {
		TreeRow row = add(rows, new TreeRow(node, depth), parent);
		row.within = within(islands, node.path, null);
		row.hasChildren = node.hasChildren();
		if (row.hasChildren) {
			if (folded.contains(node.path)) {
				row.folded = true;
			} else {
				Island island = null;

				for (Island candidate : islands) {
					if (!candidate.open && candidate.top().equals(node.path) && (island == null || candidate.to(island) == Island.Relation.AROUND)) {
						island = candidate;
					}
				}

				if (island == null) {
					for (TreeNode child : node.children) {
						visit(outline, child, depth + 1, row.index, folded, islands, rows);
					}
				} else {
					TreeRow fold = add(rows, new TreeRow(island, hidden(outline, island), depth + 1), row.index);
					fold.within = within(islands, null, island);
					List<TreeNode> ends = new ArrayList<>();

					for (String bottom : island.bottoms()) {
						TreeNode end = outline.at(bottom);
						if (end != null) {
							ends.add(end);
						}
					}

					ends.sort(Comparator.comparingInt(n -> n.order));

					for (TreeNode end : ends) {
						visit(outline, end, depth + 2, fold.index, folded, islands, rows);
					}
				}
			}
		}
	}

	private static TreeRow add(List<TreeRow> rows, TreeRow row, int parent) {
		row.index = rows.size();
		row.parent = parent;

		for (int i = rows.size() - 1; i > parent; i--) {
			TreeRow before = rows.get(i);
			if (before.parent == parent) {
				row.prevSibling = i;
				before.nextSibling = row.index;
				break;
			}
		}

		rows.add(row);
		return row;
	}

	private static List<Island> within(List<Island> islands, @Nullable String path, @Nullable Island folded) {
		List<Island> out = null;

		for (Island island : islands) {
			if (island.open) {
				boolean in = folded != null ? folded != island && folded.to(island) == Island.Relation.INSIDE : island.hides(path);
				if (in) {
					if (out == null) {
						out = new ArrayList<>(2);
					}

					out.add(island);
				}
			}
		}

		if (out == null) {
			return List.of();
		} else {
			out.sort((x, y) -> x == y ? 0 : (x.to(y) == Island.Relation.AROUND ? -1 : 1));
			return out;
		}
	}

	private TreeRows() {
	}

	public static enum Refusal {
		SAME_POINT,
		NOT_IN_TREE,
		NOTHING_BETWEEN,
		ALREADY_THERE,
		CROSSES;
	}
}
