package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import com.google.gson.JsonElement;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiResolutionRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.serializer.EmiIngredientSerializer;
import com.onehumanawa.cfhcore.content.gui.emi.bom.MaterialNode;
import com.onehumanawa.cfhcore.content.gui.emi.bom.MaterialTree;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

public final class TreeOutline {
	public final TreeNode goal;
	public final List<TreeNode> all = new ArrayList<>();
	private final Map<String, TreeNode> byPath = new HashMap<>();

	private TreeOutline(TreeNode goal) {
		this.goal = goal;
	}

	public static TreeOutline of(MaterialTree tree) {
		TreeOutline out = new TreeOutline(node(tree.goal, "", 0, null));
		out.add(out.goal);
		out.grow(out.goal);
		return out;
	}

	private static TreeNode node(MaterialNode m, String path, int depth, @Nullable TreeNode parent) {
		return m.recipe instanceof EmiResolutionRecipe && m.children != null && !m.children.isEmpty()
			? new TreeNode(path, depth, parent, m.children.get(0), m, false)
			: new TreeNode(path, depth, parent, m, null, false);
	}

	private void grow(TreeNode at) {
		MaterialNode m = at.node;
		if (m != null && at.recipe() != null && m.children != null) {
			Set<String> taken = new HashSet<>();

			for (MaterialNode child : m.children) {
				String key = key(child.ingredient);

				for (int n = 2; !taken.add(key); n++) {
					key = key(child.ingredient) + "~" + n;
				}

				TreeNode node = node(child, TreePaths.child(at.path, key), at.depth + 1, at);
				at.children.add(node);
				this.add(node);
				this.grow(node);
			}
		}
	}

	private void add(TreeNode node) {
		node.order = this.all.size();
		this.all.add(node);
		this.byPath.put(node.path, node);
	}

	@Nullable
	public TreeNode at(@Nullable String path) {
		return path == null ? null : this.byPath.get(path);
	}

	public boolean has(@Nullable String path) {
		return path != null && this.byPath.containsKey(path);
	}

	public static String key(EmiIngredient ingredient) {
		JsonElement written = null;

		try {
			written = EmiIngredientSerializer.getSerialized(ingredient);
		} catch (RuntimeException var5) {
		}

		if (written != null && !written.isJsonNull()) {
			return written.isJsonPrimitive() ? written.getAsString() : written.toString();
		} else {
			StringBuilder b = new StringBuilder("?");

			for (EmiStack stack : ingredient.getEmiStacks()) {
				if (b.length() > 1) {
					b.append('|');
				}

				b.append(stack.getId());
			}

			return b.toString();
		}
	}

	public static TreeOutline parse(String text) {
		int[] at = new int[]{0};
		TreeOutline[] out = new TreeOutline[1];
		read(text, at, null, out);
		return out[0];
	}

	private static void read(String text, int[] at, @Nullable TreeNode parent, TreeOutline[] out) {
		while (at[0] < text.length()) {
			while (at[0] < text.length() && text.charAt(at[0]) == ' ') {
				at[0]++;
			}

			if (at[0] < text.length() && text.charAt(at[0]) != ')') {
				int from = at[0];

				while (at[0] < text.length() && " ()".indexOf(text.charAt(at[0])) < 0) {
					at[0]++;
				}

				String name = text.substring(from, at[0]);
				boolean step = at[0] < text.length() && text.charAt(at[0]) == '(';
				TreeNode node = parent == null
					? new TreeNode("", 0, null, null, null, step)
					: new TreeNode(TreePaths.child(parent.path, name), parent.depth + 1, parent, null, null, step);
				if (parent == null) {
					out[0] = new TreeOutline(node);
				} else {
					parent.children.add(node);
				}

				out[0].add(node);
				if (step) {
					at[0]++;
					read(text, at, node, out);
					if (at[0] < text.length() && text.charAt(at[0]) == ')') {
						at[0]++;
					}
				}

				if (parent != null) {
					continue;
				}

				return;
			}

			return;
		}
	}
}
