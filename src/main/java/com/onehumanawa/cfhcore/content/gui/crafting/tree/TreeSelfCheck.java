package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.content.gui.emi.EmiUtil;
import com.onehumanawa.cfhcore.content.gui.emi.api.EmiApi;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiPlayerInventory;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipeCategory;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipeManager;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiResolutionRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.bom.FlatMaterialCost;
import com.onehumanawa.cfhcore.content.gui.emi.bom.FoldState;
import com.onehumanawa.cfhcore.content.gui.emi.bom.MaterialTree;
import com.onehumanawa.cfhcore.content.gui.emi.config.EmiConfig;
import com.onehumanawa.cfhcore.content.gui.emi.registry.EmiRecipes;
import com.onehumanawa.cfhcore.content.gui.emi.registry.EmiStackList;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiReloadManager;
import com.onehumanawa.cfhcore.content.gui.emi.search.EmiSearch;
import com.onehumanawa.cfhcore.content.gui.emi.search.ModQuery;
import com.onehumanawa.cfhcore.content.gui.emi.search.SearchStack;
import com.onehumanawa.cfhcore.content.gui.emi.search.TooltipQuery;
import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import com.onehumanawa.cfhcore.content.gui.crafting.CFHGuiCrafting;
import com.onehumanawa.cfhcore.content.gui.crafting.search.SharedText;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.client.searchtree.SuffixArray;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

final class TreeSelfCheck {
	private static int passed;
	private static int failed;
	private static final String MACHINE = "machine(casing(alloy(andesite nugget(ingot(raw(ore)))) log) shaft(alloy2(andesite2 nugget2)) plate(ingot2(raw2)))";

	static int run() {
		failed = 0;
		passed = 0;

		try {
			paths();
			islands();
			relations();
			rows();
			refusals();
			graph();
			written();
			sharedText();
			if (EmiReloadManager.isLoaded()) {
				real();
				indexes();
			} else {
				CFHGuiCrafting.LOGGER.info("[CFHGui Crafting] tree self-check: the viewer has no recipes yet, real trees are not checked");
			}
		} catch (RuntimeException var1) {
			failed++;
			CFHGuiCrafting.LOGGER.error("[CFHGui Crafting] tree self-check threw", var1);
		}

		CFHGuiCrafting.LOGGER.info("[CFHGui Crafting] tree self-check: {} passed, {} failed", passed, failed);
		return failed;
	}

	private static void check(String what, BooleanSupplier holds) {
		boolean ok;
		try {
			ok = holds.getAsBoolean();
		} catch (RuntimeException var4) {
			ok = false;
			CFHGuiCrafting.LOGGER.error("[CFHGui Crafting] tree self-check: \"{}\" threw", what, var4);
		}

		if (ok) {
			passed++;
		} else {
			failed++;
			CFHGuiCrafting.LOGGER.error("[CFHGui Crafting] tree self-check FAILED: {}", what);
		}
	}

	private static String p(String... keys) {
		return TreePaths.of(List.of(keys));
	}

	private static GraphLayout lay(List<TreeRow> rows, int[] widths) {
		int n = rows.size();
		int[] parent = new int[n];
		int[] height = new int[n];
		int[] depth = new int[n];

		for (int i = 0; i < n; i++) {
			parent[i] = rows.get(i).parent;
			height[i] = 30;
			depth[i] = rows.get(i).depth;
		}

		return GraphLayout.of(parent, widths, height, depth, 8.0F, 18);
	}

	private static void graph() {
		TreeOutline o = TreeOutline.parse("machine(casing(alloy(andesite nugget(ingot(raw(ore)))) log) shaft(alloy2(andesite2 nugget2)) plate(ingot2(raw2)))");
		List<TreeRow> rows = TreeRows.flatten(o, Set.of(), List.of());
		int[] widths = new int[rows.size()];

		for (int i = 0; i < widths.length; i++) {
			widths[i] = 20 + i % 3 * 7;
		}

		GraphLayout g = lay(rows, widths);
		check("in the graph no two nodes of a depth overlap, and each keeps its gap", () -> {
			for (int i = 0; i < rows.size(); i++) {
				for (int j = i + 1; j < rows.size(); j++) {
					if (g.y[i] == g.y[j]) {
						float li = g.x[i] - (float)widths[i] / 2.0F;
						float ri = li + (float)widths[i];
						float lj = g.x[j] - (float)widths[j] / 2.0F;
						float rj = lj + (float)widths[j];
						if (li < rj + 7.99F && lj < ri + 7.99F) {
							return false;
						}
					}
				}
			}

			return true;
		});
		check("in the graph every parent stands over the middle of its first and last child", () -> {
			for (TreeRow row : rows) {
				int firstChild = -1;
				int lastChild = -1;

				for (TreeRow other : rows) {
					if (other.parent == row.index) {
						if (firstChild < 0) {
							firstChild = other.index;
						}

						lastChild = other.index;
					}
				}

				if (firstChild >= 0 && Math.abs(g.x[row.index] - (g.x[firstChild] + g.x[lastChild]) / 2.0F) > 0.01F) {
					return false;
				}
			}

			return true;
		});
		check("in the graph a chain of single children stands straight down, and a depth is one line", () -> {
			int raw = rows.stream().filter(r -> r.node != null && r.node.path.equals(p("casing", "alloy", "nugget", "ingot", "raw"))).findFirst().orElseThrow().index;
			int ore = raw + 1;
			return Math.abs(g.x[raw] - g.x[ore]) < 0.01F && g.y[ore] > g.y[raw] && g.y[0] == 0 && g.maxY == g.y[ore] + 30;
		});
		check("in the graph subtrees of siblings never interleave: each sibling's subtree lies right of the one before", () -> {
			for (TreeRow row : rows) {
				if (row.prevSibling >= 0) {
					float[] before = extent(rows, g, widths, row.prevSibling);
					float[] here = extent(rows, g, widths, row.index);
					if (here[0] < before[1]) {
						for (int d = 0; d < 12; d++) {
							float[] b = extentAt(rows, g, widths, row.prevSibling, d);
							float[] h = extentAt(rows, g, widths, row.index, d);
							if (b != null && h != null && h[0] < b[1] + 7.99F) {
								return false;
							}
						}
					}
				}
			}

			return true;
		});
		check("the graph of three leaves under one node: packed by the gap, the node in the middle", () -> {
			GraphLayout three = GraphLayout.of(new int[]{-1, 0, 0, 0}, new int[]{20, 20, 20, 20}, new int[]{30, 30, 30, 30}, new int[]{0, 1, 1, 1}, 8.0F, 18);
			return three.x[1] == -28.0F && three.x[2] == 0.0F && three.x[3] == 28.0F && three.x[0] == 0.0F && three.y[1] == 48;
		});
		check("a graph of 500 steps is laid out in a moment, and nothing of a depth overlaps there either", () -> {
			int n = 1001;
			int[] parent = new int[1001];
			int[] width = new int[1001];
			int[] height = new int[1001];
			int[] depth = new int[1001];

			for (int i = 0; i < 1001; i++) {
				parent[i] = i == 0 ? -1 : (i - 1) / 2;
				width[i] = 22 + i % 5 * 4;
				height[i] = 30;
				depth[i] = i == 0 ? 0 : depth[(i - 1) / 2] + 1;
			}

			int[] order = new int[1001];
			int[] at = new int[1001];
			int[] stack = new int[1001];
			int top = 0;
			int k = 0;
			stack[top++] = 0;

			while (top > 0) {
				int v = stack[--top];
				order[k] = v;
				at[v] = k++;
				if (2 * v + 2 < 1001) {
					stack[top++] = 2 * v + 2;
				}

				if (2 * v + 1 < 1001) {
					stack[top++] = 2 * v + 1;
				}
			}

			int[] p2 = new int[1001];
			int[] w2 = new int[1001];
			int[] h2 = new int[1001];
			int[] d2 = new int[1001];

			for (int i = 0; i < 1001; i++) {
				int vx = order[i];
				p2[i] = parent[vx] < 0 ? -1 : at[parent[vx]];
				w2[i] = width[vx];
				h2[i] = height[vx];
				d2[i] = depth[vx];
			}

			long began = System.nanoTime();
			GraphLayout big = GraphLayout.of(p2, w2, h2, d2, 8.0F, 18);
			long took = System.nanoTime() - began;
			Map<Integer, List<Integer>> byLine = new HashMap<>();

			for (int i = 0; i < 1001; i++) {
				byLine.computeIfAbsent(big.y[i], key -> new ArrayList<>()).add(i);
			}

			for (List<Integer> line : byLine.values()) {
				line.sort((ax, bx) -> Float.compare(big.x[ax], big.x[bx]));

				for (int j = 1; j < line.size(); j++) {
					int a = line.get(j - 1);
					int b = line.get(j);
					if (big.x[a] + (float)w2[a] - (float)w2[a] / 2.0F + 7.99F > big.x[b] - (float)w2[b] / 2.0F) {
						return false;
					}
				}
			}

			return took < 50000000L;
		});
	}

	private static float[] extent(List<TreeRow> rows, GraphLayout g, int[] widths, int index) {
		float l = Float.MAX_VALUE;
		float r = -Float.MAX_VALUE;

		for (TreeRow row : rows) {
			if (row.index == index || under(rows, row.index, index)) {
				l = Math.min(l, g.x[row.index] - (float)widths[row.index] / 2.0F);
				r = Math.max(r, g.x[row.index] + (float)widths[row.index] - (float)widths[row.index] / 2.0F);
			}
		}

		return new float[]{l, r};
	}

	private static float[] extentAt(List<TreeRow> rows, GraphLayout g, int[] widths, int index, int below) {
		float l = Float.MAX_VALUE;
		float r = -Float.MAX_VALUE;
		boolean any = false;

		for (TreeRow row : rows) {
			if (row.depth == rows.get(index).depth + below && (row.index == index || under(rows, row.index, index))) {
				any = true;
				l = Math.min(l, g.x[row.index] - (float)widths[row.index] / 2.0F);
				r = Math.max(r, g.x[row.index] + (float)widths[row.index] - (float)widths[row.index] / 2.0F);
			}
		}

		return any ? new float[]{l, r} : null;
	}

	private static boolean under(List<TreeRow> rows, int index, int above) {
		for (int p = rows.get(index).parent; p >= 0; p = rows.get(p).parent) {
			if (p == above) {
				return true;
			}
		}

		return false;
	}

	private static void paths() {
		String c = p("b", "c");
		check("the goal's path is the empty one and lies above everything", () -> "".isEmpty() && TreePaths.under("", c) && TreePaths.at("", ""));
		check(
			"a path's parent, depth and last key",
			() -> p("b").equals(TreePaths.parent(c)) && TreePaths.parent("") == null && TreePaths.depth(c) == 2 && "c".equals(TreePaths.last(c))
		);
		check(
			"a path is its keys, and its keys are the path",
			() -> List.of("b", "c").equals(TreePaths.keys(c)) && c.equals(TreePaths.of(TreePaths.keys(c))) && TreePaths.keys("").isEmpty()
		);
		check(
			"under means below, not the same and not a longer name",
			() -> TreePaths.under(p("b"), c) && !TreePaths.under(c, c) && !TreePaths.under(p("b"), p("bc")) && !TreePaths.under(c, p("b"))
		);
		check("the lowest common ancestor of two branches is where they part", () -> p("a").equals(TreePaths.common(p("a", "x", "y"), p("a", "z"))));
		check(
			"the lowest common ancestor of a step and one under it is the higher of the two",
			() -> p("a").equals(TreePaths.common(p("a"), p("a", "x", "y"))) && p("a").equals(TreePaths.common(p("a", "x", "y"), p("a")))
		);
		check(
			"the lowest common ancestor of two top branches is the goal",
			() -> "".equals(TreePaths.common(p("a"), p("b"))) && "".equals(TreePaths.common(p("ab"), p("a")))
		);
		check("a path is its own lowest common ancestor", () -> c.equals(TreePaths.common(c, c)));
		TreeOutline o = TreeOutline.parse("machine(casing(alloy(andesite nugget(ingot(raw(ore)))) log) shaft(alloy2(andesite2 nugget2)) plate(ingot2(raw2)))");
		check(
			"an outline written down has every place at its path",
			() -> o.all.size() == 16 && o.has(p("casing", "alloy", "nugget", "ingot", "raw", "ore")) && o.at(p("shaft")).children.size() == 1 && !o.has(p("ore"))
		);
		check(
			"what has brackets is a step, what has none a leaf",
			() -> o.goal.step() && o.at(p("casing")).step() && !o.at(p("casing", "log")).step() && !o.at(p("casing", "log")).hasChildren()
		);
	}

	private static void islands() {
		TreeOutline o = TreeOutline.parse("machine(casing(alloy(andesite nugget(ingot(raw(ore)))) log) shaft(alloy2(andesite2 nugget2)) plate(ingot2(raw2)))");
		String ore = p("casing", "alloy", "nugget", "ingot", "raw", "ore");
		Island chain = new Island(ore, "");
		check("an island is the same whichever point was picked first", () -> chain.sameEnds(new Island("", ore)) && chain.id().equals(new Island("", ore).id()));
		check(
			"with one point above the other, the upper end is that point and the other the one lower end",
			() -> chain.top().equals("") && chain.bottoms().equals(List.of(ore))
		);
		check(
			"it hides every step under the upper end except the lower end: the machine, the island, the ore",
			() -> {
				List<TreeNode> hidden = TreeRows.hidden(o, chain);
				return hidden.size() == 14
					&& hidden.stream().noneMatch(n -> n.path.equals(ore) || n.depth == 0)
					&& chain.hides(p("shaft"))
					&& !chain.hides(ore)
					&& !chain.hides("");
			}
		);
		Island fork = new Island(p("casing", "log"), p("casing", "alloy", "nugget"));
		check(
			"with two points on different branches, the upper end is their lowest common ancestor and both are lower ends",
			() -> fork.top().equals(p("casing")) && fork.bottoms().size() == 2
		);
		check(
			"... and it hides what is under that ancestor and neither at nor under the two",
			() -> {
				List<TreeNode> hidden = TreeRows.hidden(o, fork);
				return hidden.size() == 2
					&& fork.hides(p("casing", "alloy"))
					&& fork.hides(p("casing", "alloy", "andesite"))
					&& !fork.hides(p("casing", "alloy", "nugget", "ingot"))
					&& !fork.hides(p("shaft"));
			}
		);
		check("what an island hides is listed top down", () -> {
			List<TreeNode> hidden = TreeRows.hidden(o, chain);

			for (int i = 1; i < hidden.size(); i++) {
				if (hidden.get(i).order <= hidden.get(i - 1).order) {
					return false;
				}
			}

			return true;
		});
	}

	private static Set<String> hiddenSet(TreeOutline o, Island island) {
		Set<String> out = new HashSet<>();

		for (TreeNode n : o.all) {
			if (island.hides(n.path)) {
				out.add(n.path);
			}
		}

		return out;
	}

	private static boolean hidesAnEnd(Set<String> hidden, Island of) {
		if (hidden.contains(of.top())) {
			return true;
		} else {
			for (String bottom : of.bottoms()) {
				if (hidden.contains(bottom)) {
					return true;
				}
			}

			return false;
		}
	}

	private static void relations() {
		TreeOutline o = TreeOutline.parse("machine(casing(alloy(andesite nugget(ingot(raw(ore)))) log) shaft(alloy2(andesite2 nugget2)) plate(ingot2(raw2)))");
		String nugget = p("casing", "alloy", "nugget");
		String raw = p("casing", "alloy", "nugget", "ingot", "raw");
		Island outer = new Island("", raw);
		Island inner = new Island(p("casing"), nugget);
		check(
			"an island under a hidden step of another, hiding only what that one hides, lies inside it",
			() -> inner.to(outer) == Island.Relation.INSIDE && outer.to(inner) == Island.Relation.AROUND
		);
		check("islands on different branches lie apart", () -> new Island(p("shaft"), p("shaft", "alloy2", "nugget2")).to(inner) == Island.Relation.APART);
		check(
			"an island below another's lower end lies apart from it",
			() -> new Island(raw, p("casing", "alloy", "nugget", "ingot", "raw", "ore")).to(outer) == Island.Relation.APART
					|| TreeRows.hidden(o, new Island(raw, p("casing", "alloy", "nugget", "ingot", "raw", "ore"))).isEmpty()
		);
		check("two islands from one upper end, one reaching further down, lie inside one another", () -> {
			Island shorter = new Island("", nugget);
			return shorter.to(outer) == Island.Relation.INSIDE && outer.to(shorter) == Island.Relation.AROUND;
		});
		check(
			"an island that reaches out of another through its lower end crosses it",
			() -> new Island(p("casing", "alloy"), p("casing", "alloy", "nugget", "ingot", "raw", "ore")).to(new Island("", p("casing", "alloy", "nugget", "ingot")))
					== Island.Relation.CROSSING
		);
		check("the same two ends are the same island", () -> new Island(raw, "").to(outer) == Island.Relation.SAME);
		List<Island> all = new ArrayList<>();

		for (int i = 0; i < o.all.size(); i++) {
			for (int k = i + 1; k < o.all.size(); k++) {
				Island island = new Island(o.all.get(i).path, o.all.get(k).path);
				if (!hiddenSet(o, island).isEmpty()) {
					all.add(island);
				}
			}
		}

		int[] wrong = new int[]{0};
		String[] first = new String[]{null};

		for (Island x : all) {
			Set<String> hx = hiddenSet(o, x);

			for (Island y : all) {
				if (x != y) {
					Set<String> hy = hiddenSet(o, y);
					boolean xInY = hy.containsAll(hx);
					boolean yInX = hx.containsAll(hy);
					boolean meet = false;

					for (String path : hx) {
						if (hy.contains(path)) {
							meet = true;
							break;
						}
					}

					Island.Relation said = x.to(y);
					boolean right;
					if (xInY && yInX) {
						right = said == Island.Relation.SAME;
					} else if (xInY) {
						right = said == Island.Relation.INSIDE;
					} else if (yInX) {
						right = said == Island.Relation.AROUND;
					} else if (meet) {
						right = said == Island.Relation.CROSSING;
					} else {
						right = said == (!hidesAnEnd(hx, y) && !hidesAnEnd(hy, x) ? Island.Relation.APART : Island.Relation.CROSSING);
					}

					if (!right) {
						wrong[0]++;
						if (first[0] == null) {
							first[0] = x.a.replace('\n', '/') + " - " + x.b.replace('\n', '/') + " to " + y.a.replace('\n', '/') + " - " + y.b.replace('\n', '/') + ": " + said;
						}
					}
				}
			}
		}

		if (first[0] != null) {
			CFHGuiCrafting.LOGGER.error("[CFHGui Crafting] tree self-check: the overlap rule is wrong for {} pairs, the first: {}", wrong[0], first[0]);
		}

		check(
			"the overlap rule agrees with what the islands hide, for every pair of islands of a made-up tree (" + all.size() + " islands)",
			() -> wrong[0] == 0 && all.size() > 60
		);
		check("... and says the same from either side", () -> {
			for (Island xx : all) {
				for (Island yx : all) {
					Island.Relation there = xx.to(yx);
					Island.Relation back = yx.to(xx);

					boolean mirrored = switch (there) {
						case INSIDE -> back == Island.Relation.AROUND;
						case AROUND -> back == Island.Relation.INSIDE;
						default -> back == there;
					};
					if (!mirrored) {
						return false;
					}
				}
			}

			return true;
		});
	}

	private static List<String> ids(List<TreeRow> rows) {
		List<String> out = new ArrayList<>();

		for (TreeRow row : rows) {
			out.add(row.isIsland() ? "[" + row.hidden.size() + "]" : row.depth + ":" + (row.node.depth == 0 ? "goal" : TreePaths.last(row.node.path)));
		}

		return out;
	}

	private static void rows() {
		TreeOutline o = TreeOutline.parse("machine(casing(alloy(andesite nugget(ingot(raw(ore)))) log) shaft(alloy2(andesite2 nugget2)) plate(ingot2(raw2)))");
		TreeMarks marks = new TreeMarks();
		check("with nothing folded there is a row for every place, top down", () -> {
			List<TreeRow> rows = marks.rows(o);
			if (rows.size() != o.all.size()) {
				return false;
			} else {
				for (int i = 0; i < rows.size(); i++) {
					if (rows.get(i).node != o.all.get(i) || rows.get(i).index != i || rows.get(i).depth != o.all.get(i).depth) {
						return false;
					}
				}

				return true;
			}
		});
		check(
			"a row knows the row it hangs under and its neighbours under that row",
			() -> {
				List<TreeRow> rows = marks.rows(o);
				TreeRow casing = rows.get(1);
				TreeRow shaft = rows.stream().filter(r -> r.node.path.equals(p("shaft"))).findFirst().orElseThrow();
				return casing.parent == 0
					&& casing.prevSibling == -1
					&& casing.nextSibling == shaft.index
					&& shaft.prevSibling == casing.index
					&& rows.get(shaft.nextSibling).node.path.equals(p("plate"))
					&& rows.get(0).parent == -1
					&& rows.get(0).hasChildren;
			}
		);
		marks.folded.add(p("casing"));
		check("a folded step keeps its row and loses those under it", () -> {
			List<TreeRow> rows = marks.rows(o);
			return rows.size() == o.all.size() - 7 && rows.get(1).folded && rows.get(1).hasChildren && rows.get(2).node.path.equals(p("shaft"));
		});
		marks.folded.clear();
		String ore = p("casing", "alloy", "nugget", "ingot", "raw", "ore");
		check(
			"an island is made between two points and is folded there and then",
			() -> marks.addIsland(o, p("casing"), ore) == null && marks.islands.size() == 1 && !marks.islands.get(0).open
		);
		check(
			"folded, it is one row under its upper end, and the lower end hangs under that row: the casing, the island, the ore",
			() -> ids(marks.rows(o)).subList(0, 4).equals(List.of("0:goal", "1:casing", "[6]", "3:ore"))
					&& marks.rows(o).get(3).parent == 2
					&& marks.rows(o).get(2).parent == 1
		);
		check("... and the rest of the tree stands as it stood", () -> marks.rows(o).size() == o.all.size() - 6 + 1);
		Island island = marks.islands.get(0);
		island.open = true;
		check("unfolded, every row is back, and those it hides when folded know the bracket they lie in", () -> {
			List<TreeRow> rows = marks.rows(o);
			if (rows.size() != o.all.size()) {
				return false;
			} else {
				for (TreeRow row : rows) {
					boolean in = island.hides(row.node.path);
					if (in != (row.within.size() == 1 && row.within.get(0) == island)) {
						return false;
					}
				}

				return true;
			}
		});
		island.open = false;
		check(
			"an island inside a folded one does not show",
			() -> marks.addIsland(o, p("casing", "alloy"), p("casing", "alloy", "nugget", "ingot")) == null
					&& marks.rows(o).stream().filter(TreeRow::isIsland).count() == 1L
		);
		island.open = true;
		check(
			"... and shows as a row of its own when the outer one is unfolded, in the outer one's bracket",
			() -> {
				List<TreeRow> rows = marks.rows(o);
				List<TreeRow> folds = rows.stream().filter(TreeRow::isIsland).toList();
				return folds.size() == 1
					&& folds.get(0).hidden.size() == 2
					&& folds.get(0).within.equals(List.of(island))
					&& rows.get(folds.get(0).index + 1).node.path.equals(p("casing", "alloy", "nugget", "ingot"))
					&& rows.get(folds.get(0).index + 1).parent == folds.get(0).index;
			}
		);
		marks.islands.get(1).open = true;
		check("both unfolded: the inner rows lie in both brackets, the outer one first", () -> {
			TreeRow row = marks.rows(o).stream().filter(r -> r.node.path.equals(p("casing", "alloy", "nugget"))).findFirst().orElseThrow();
			return row.within.size() == 2 && row.within.get(0) == island && row.within.get(1) == marks.islands.get(1);
		});
		marks.islands.clear();
		check(
			"with two lower ends, both hang under the island's row in the tree's order",
			() -> marks.addIsland(o, p("plate", "ingot2", "raw2"), p("casing", "log")) != null
					? false
					: ids(marks.rows(o)).equals(List.of("0:goal", "[13]", "2:log", "2:raw2"))
		);
		marks.islands.clear();
		o.at(p("casing")).given = true;
		check("a step that is taken as given shows as a leaf", () -> {
			List<TreeRow> rows = marks.rows(o);
			return rows.size() == o.all.size() - 7 && !rows.get(1).hasChildren && !o.at(p("casing")).step() && o.at(p("casing")).hasRecipe();
		});
		check("an island with an end under a step that is taken as given has no place while it is", () -> {
			marks.islands.add(new Island("", ore));
			boolean passedOver = marks.rows(o).stream().noneMatch(TreeRow::isIsland) && !TreeRows.live(o, marks.islands.get(0));
			o.at(p("casing")).given = false;
			return passedOver && TreeRows.live(o, marks.islands.get(0)) && marks.rows(o).stream().anyMatch(TreeRow::isIsland);
		});
	}

	private static void refusals() {
		TreeOutline o = TreeOutline.parse("machine(casing(alloy(andesite nugget(ingot(raw(ore)))) log) shaft(alloy2(andesite2 nugget2)) plate(ingot2(raw2)))");
		TreeMarks marks = new TreeMarks();
		String ingot = p("casing", "alloy", "nugget", "ingot");
		String ore = p("casing", "alloy", "nugget", "ingot", "raw", "ore");
		check("the same point twice is no island", () -> marks.addIsland(o, ingot, ingot) == TreeRows.Refusal.SAME_POINT);
		check("a point that is no place of the tree is refused", () -> marks.addIsland(o, ingot, p("nowhere")) == TreeRows.Refusal.NOT_IN_TREE);
		check(
			"two points with nothing between them are refused",
			() -> marks.addIsland(o, p("shaft"), p("shaft", "alloy2")) == TreeRows.Refusal.NOTHING_BETWEEN && marks.islands.isEmpty()
		);
		check("the first island is made", () -> marks.addIsland(o, "", ingot) == null);
		check("the same island again is refused", () -> marks.addIsland(o, ingot, "") == TreeRows.Refusal.ALREADY_THERE);
		check(
			"an island that would overlap it in part is refused, and nothing is added",
			() -> marks.addIsland(o, p("casing", "alloy"), ore) == TreeRows.Refusal.CROSSES && marks.islands.size() == 1
		);
		check("an island inside it is made", () -> marks.addIsland(o, p("casing"), p("casing", "alloy", "nugget")) == null && marks.islands.size() == 2);
		check("an island below its lower end is made", () -> marks.addIsland(o, ingot, ore) == null && marks.islands.size() == 3);
		check("no island of the tree crosses another", () -> {
			for (Island x : marks.islands) {
				for (Island y : marks.islands) {
					if (x != y && x.to(y) == Island.Relation.CROSSING) {
						return false;
					}
				}
			}

			return true;
		});
	}

	private static boolean same(TreeMarks a, TreeMarks b) {
		if (a.name.equals(b.name) && a.folded.equals(b.folded) && a.given.equals(b.given) && a.islands.size() == b.islands.size()) {
			for (int i = 0; i < a.islands.size(); i++) {
				if (!a.islands.get(i).sameEnds(b.islands.get(i)) || a.islands.get(i).open != b.islands.get(i).open) {
					return false;
				}
			}

			return true;
		} else {
			return false;
		}
	}

	private static void written() {
		TreeOutline o = TreeOutline.parse("machine(casing(alloy(andesite nugget(ingot(raw(ore)))) log) shaft(alloy2(andesite2 nugget2)) plate(ingot2(raw2)))");
		TreeMarks marks = new TreeMarks();
		marks.name = "Press, for the second line";
		marks.folded.add(p("shaft"));
		marks.given.add(p("plate", "ingot2"));
		marks.addIsland(o, "", p("casing", "alloy", "nugget", "ingot"));
		marks.addIsland(o, p("casing"), p("casing", "alloy", "nugget"));
		marks.islands.get(1).open = true;
		JsonObject json = new JsonObject();
		marks.write(json);
		check("the marks of a tree read back are the marks that were written", () -> same(marks, TreeMarks.read(json)));
		check("... through text as well", () -> same(marks, TreeMarks.read((JsonObject)JsonConfig.GSON.fromJson(JsonConfig.GSON.toJson(json), JsonObject.class))));
		check("marks with nothing in them write nothing", () -> {
			JsonObject empty = new JsonObject();
			new TreeMarks().write(empty);
			return empty.size() == 0 && same(new TreeMarks(), TreeMarks.read(empty));
		});
		check("islands that cross are not read from a file edited by hand", () -> {
			TreeMarks edited = TreeMarks.read(json);
			edited.islands.add(new Island(p("casing", "alloy"), p("casing", "alloy", "nugget", "ingot", "raw", "ore")));
			JsonObject bad = new JsonObject();
			edited.write(bad);
			return TreeMarks.read(bad).islands.size() == 2;
		});
		check("a change of recipe drops the marks of what is gone and keeps the rest", () -> {
			TreeOutline changed = TreeOutline.parse("machine(casing(alloy(andesite brass) log) shaft(alloy2(andesite2 nugget2)) plate(sheet))");
			TreeMarks kept = TreeMarks.read(json);
			return kept.prune(changed) && kept.folded.contains(p("shaft")) && kept.given.isEmpty() && kept.islands.isEmpty() && !kept.prune(changed);
		});
		check(
			"an island survives a change of recipe elsewhere",
			() -> {
				TreeOutline changed = TreeOutline.parse(
					"machine(casing(alloy(andesite nugget(ingot(raw(ore)))) log) shaft(alloy2(andesite2 nugget2)) plate(ingot2(raw2)))"
						.replace("plate(ingot2(raw2))", "plate(sheet)")
				);
				TreeMarks kept = TreeMarks.read(json);
				kept.prune(changed);
				return kept.islands.size() == 2 && kept.given.isEmpty();
			}
		);
		TreeStore store = new TreeStore(null);
		TreeEntry one = store.create();
		TreeEntry two = store.create();
		TreeEntry three = store.create();
		one.json = json.deepCopy();
		one.saved = one.pinned = true;
		one.lastUsed = 30L;
		two.json = new JsonObject();
		two.json.addProperty("recipe", "x:two");
		two.lastUsed = 20L;
		three.json = null;
		store.shown = one.id;
		store.dockOpen = true;
		TreeStore back = new TreeStore(null);
		back.read((JsonObject)JsonConfig.GSON.fromJson(JsonConfig.GSON.toJson(store.toJson()), JsonObject.class));
		check(
			"the store read back holds the trees that could be written, in their order, and which was shown",
			() -> back.all().size() == 2
					&& back.all().get(0).id.equals(one.id)
					&& back.all().get(0).saved
					&& back.all().get(0).pinned
					&& !back.all().get(1).saved
					&& back.all().get(0).lastUsed == 30L
					&& one.id.equals(back.shown)
					&& back.dockOpen
					&& back.all().get(0).json.equals(one.json)
		);
		check(
			"saved trees and recent ones are told apart, the last used first", () -> back.saved().size() == 1 && back.recent().size() == 1 && back.pinned().size() == 1
		);
		check("recent trees are kept few, and the tree that is shown always", () -> {
			TreeStore many = new TreeStore(null);
			TreeEntry oldest = null;

			for (int i = 0; i < 13; i++) {
				TreeEntry e = many.create();
				e.json = new JsonObject();
				e.json.addProperty("recipe", "x:" + i);
				e.lastUsed = (long)i;
				if (i == 0) {
					oldest = e;
				}
			}

			many.trimRecent(oldest);
			return many.recent().size() == 8 && many.all().contains(oldest);
		});
		check("a tree started over takes the place of the recent tree of the same goal", () -> {
			TreeStore again = new TreeStore(null);
			TreeEntry before = again.create();
			TreeEntry after = again.create();
			TreeEntry other = again.create();

			for (TreeEntry e : List.of(before, after, other)) {
				e.json = new JsonObject();
			}

			before.json.addProperty("recipe", "x:press");
			after.json.addProperty("recipe", "x:press");
			other.json.addProperty("recipe", "x:mixer");
			after.lastUsed = before.lastUsed + 1L;
			again.trimRecent(after);
			return again.all().equals(List.of(after, other));
		});
		check("a pinned tree is put at another place of quick access", () -> {
			TreeStore order = new TreeStore(null);
			List<TreeEntry> made = new ArrayList<>();

			for (int i = 0; i < 4; i++) {
				TreeEntry e = order.create();
				e.saved = e.pinned = true;
				made.add(e);
			}

			order.move(made.get(3), 0);
			boolean front = order.pinned().equals(List.of(made.get(3), made.get(0), made.get(1), made.get(2)));
			order.move(made.get(3), 99);
			boolean last = order.pinned().equals(made);
			order.move(made.get(0), 2);
			boolean right = order.pinned().equals(List.of(made.get(1), made.get(2), made.get(0), made.get(3)));
			order.move(made.get(0), 0);
			return front && last && right && order.pinned().equals(made);
		});
		check("the store written to a file and read from it is the same store", () -> {
			try {
				Path file = JsonConfig.dir().resolve("crafting-trees-check.json");
				TreeStore out = new TreeStore(file);
				out.read(store.toJson());
				out.touch();
				out.save();
				TreeStore in = new TreeStore(file);
				in.load();
				boolean same = in.toJson().equals(out.toJson()) && in.all().size() == 2;
				Files.deleteIfExists(file);
				return same;
			} catch (IOException var5x) {
				return false;
			}
		});
	}

	private static EmiRecipe recipe(String id) {
		return EmiApi.getRecipeManager().getRecipe(ResourceLocation.parse(id));
	}

	private static List<String> places(TreeSession s) {
		List<String> out = new ArrayList<>();

		for (TreeNode n : s.outline().all) {
			out.add(n.path);
		}

		return out;
	}

	private static void real() {
		EmiRecipe pickaxe = recipe("minecraft:iron_pickaxe");
		EmiRecipe table = recipe("minecraft:crafting_table");
		check("the viewer knows the recipes this check builds its trees from", () -> pickaxe != null && table != null);
		if (pickaxe != null && table != null) {
			TreeSession s = new TreeSession(new MaterialTree(pickaxe), new TreeMarks()).grown();
			EmiRecipe sticks = recipe("minecraft:stick");
			if (sticks != null) {
				s.tree.addResolution(EmiStack.of(Items.STICK), sticks);
			}

			check(
				"a real tree is read off upstream's model: the goal, and a place for each of its ingredients",
				() -> s.outline().goal.step()
						&& (long)s.outline().goal.children.size() == pickaxe.getInputs().stream().filter(i -> !i.isEmpty()).map(TreeOutline::key).distinct().count()
			);
			check("every place of it has a path of its own", () -> new HashSet<>(places(s)).size() == s.outline().all.size());
			check("paths are the ingredients as upstream writes them", () -> s.outline().all.stream().anyMatch(n -> n.path.contains("item:minecraft:")));
			TreeNode tag = null;

			for (TreeNode n : s.outline().all) {
				if (n.ingredient() != null && n.ingredient().getEmiStacks().size() > 1) {
					tag = n;
					break;
				}
			}

			if (tag != null) {
				s.tree.addResolution(tag.ingredient(), new EmiResolutionRecipe(tag.ingredient(), tag.ingredient().getEmiStacks().get(0)));
			}

			TreeNode chosen = tag;
			check(
				"a stack chosen for a tag stands in the tag's place, as one row",
				() -> chosen == null
						|| s.outline().at(chosen.path) != null && s.outline().at(chosen.path).tag != null && s.outline().at(chosen.path).ingredient().getEmiStacks().size() == 1
			);
			TreeNode step = null;

			for (TreeNode nx : s.outline().all) {
				if (nx.depth > 0 && nx.step() && nx.hasChildren()) {
					step = nx;
					break;
				}
			}

			TreeNode made = step;
			check("the tree of an iron pickaxe has a step under its goal (its sticks, made of planks)", () -> made != null);
			if (made != null) {
				s.fold(made.path, true);
				s.batches(3L);
				s.rename("Pickaxes for everyone");
				TreeNode end = made.children.get(0);
				s.fold(made.path, false);
				s.island("", end.path);
				s.fold(made.path, true);
			}

			JsonObject json = TreeCodec.write(s.tree, s.marks);
			check(
				"a tree whose recipes have ids can be written",
				() -> json != null
						&& "minecraft:iron_pickaxe".equals(TreeCodec.goalRecipe(json))
						&& TreeCodec.goal(json) != null
						&& TreeCodec.goal(json).isEqual(EmiStack.of(Items.IRON_PICKAXE))
			);
			TreeSession back = json == null ? null : TreeCodec.read((JsonObject)JsonConfig.GSON.fromJson(JsonConfig.GSON.toJson(json), JsonObject.class));
			check(
				"a tree written down and read back is the same tree: its goal, its places, how many to make",
				() -> back != null && back.tree.goal.recipe == pickaxe && back.tree.batches == 3L && places(back).equals(places(s))
			);
			check("... its name, its folds and its islands", () -> back != null && same(s.marks, back.marks) && back.rows().size() == s.rows().size());
			check(
				"... and the recipes chosen in it",
				() -> back != null && back.tree.resolutions.size() == s.tree.resolutions.size() && TreeCodec.write(back.tree, back.marks).equals(json)
			);
			EmiPlayerInventory nothing = new EmiPlayerInventory(List.of());
			nothing.inventory.clear();
			s.marks.folded.clear();
			s.marks.islands.clear();
			s.look(nothing, null, false);
			check("with nothing in the inventory nothing is done, and every leaf is missing all that is asked of it", () -> {
				if (s.fraction() == 0.0F && !s.complete()) {
					for (TreeNode nxx : s.outline().all) {
						TreeFigures f = s.figures(nxx.path);
						if (f.state == TreeFigures.State.DONE || f.state == TreeFigures.State.READY || f.missing != f.wanted || f.wanted <= 0L) {
							return false;
						}
					}

					return true;
				} else {
					return false;
				}
			});
			check("the goal asks for as many as its recipe makes, times the batches", () -> s.figures("").wanted == 3L * s.tree.goal.amount);
			check(
				"the tree's cost lists every raw material, all of it missing",
				() -> !s.raws().isEmpty() && s.raws().stream().allMatch(r -> r.wanted > 0L && r.missing == r.wanted)
			);
			long steps = (long)s.steps();
			check("the steps of the tree are counted", () -> steps >= 2L && s.stepsDone() == 0 && s.stepsReady() == 0);
			if (made != null) {
				EmiIngredient stuff = made.ingredient();
				long before = cost(s.tree, stuff);
				s.give(made.path, true);
				s.look(nothing, null, false);
				check(
					"a step taken as given is counted as a raw material: it is a cost of the tree now, and nothing under it is",
					() -> before == 0L
							&& cost(s.tree, stuff) > 0L
							&& s.figures(made.path).state == TreeFigures.State.GIVEN
							&& (long)s.steps() == steps - 1L
							&& s.rows().stream().noneMatch(r -> r.node != null && TreePaths.under(made.path, r.node.path))
				);
				check("... and what is taken as given outlasts a recalculation of upstream's tree", () -> {
					s.tree.recalculate();
					return s.outline().at(made.path).given && s.outline().at(made.path).node.state == FoldState.COLLAPSED;
				});
				s.give(made.path, false);
				s.look(nothing, null, false);
				check(
					"... and made again, it is a step again",
					() -> cost(s.tree, stuff) == 0L && (long)s.steps() == steps && s.figures(made.path).state == TreeFigures.State.WAITING
				);
				List<EmiStack> needs = new ArrayList<>();

				for (TreeNode child : made.children) {
					needs.add(child.ingredient().getEmiStacks().get(0).copy().setAmount(64L));
				}

				s.look(new EmiPlayerInventory(needs), null, true);
				check(
					"a step whose ingredients are in the inventory is ready, and what it needs is done",
					() -> s.figures(made.path).state == TreeFigures.State.READY
							&& s.figures(made.children.get(0).path).state == TreeFigures.State.DONE
							&& s.stepsReady() >= 1
							&& s.fraction() > 0.0F
				);
				check(
					"... and the change is noted for what moves: the step lights up, the counts tick",
					() -> s.lit.containsKey(made.path) && s.ticked.containsKey(made.children.get(0).path)
				);
				s.look(new EmiPlayerInventory(List.of(made.ingredient().getEmiStacks().get(0).copy().setAmount(64L))), null, true);
				check(
					"with the step's own product in the inventory the step is done, and nothing is asked of what is under it",
					() -> s.figures(made.path).state == TreeFigures.State.DONE
							&& s.figures(made.children.get(0).path).wanted == 0L
							&& s.figures(made.children.get(0).path).state == TreeFigures.State.DONE
				);
			}

			s.look(new EmiPlayerInventory(List.of(EmiStack.of(Items.IRON_PICKAXE, 3L))), null, true);
			check("with the goal itself in the inventory the tree is complete, and that is noted once", () -> s.complete() && s.fraction() == 1.0F && s.celebrated != 0L);
			TreeSession t = new TreeSession(new MaterialTree(table), new TreeMarks()).grown();
			t.look(new EmiPlayerInventory(List.of(EmiStack.of(Items.BIRCH_PLANKS, 4L))), null, false);
			check(
				"an ingredient that is a tag is done by any stack of the tag",
				() -> t.outline().goal.children.size() == 1
						&& t.figures(t.outline().goal.children.get(0).path).state == TreeFigures.State.DONE
						&& t.figures("").state == TreeFigures.State.READY
			);
		}
	}

	private static long cost(MaterialTree tree, EmiIngredient ingredient) {
		tree.calculateCost();
		FlatMaterialCost cost = tree.cost.costs.get(ingredient);
		return cost == null ? 0L : cost.amount;
	}

	private static void sharedText() {
		String[] lines = new String[]{
			"when in main hand:",
			"when in off hand:",
			"hold shift for summary",
			"+7 attack damage",
			"1.6 attack speed",
			"durability: 250 / 250",
			"minecraft",
			"cfh crafting",
			"a",
			""
		};
		Random random = new Random(7L);
		SharedText<Integer> shared = new SharedText<>();
		SuffixArray<Integer> plain = new SuffixArray();

		for (int entry = 0; entry < 300; entry++) {
			for (int n = random.nextInt(4); n >= 0; n--) {
				String line = lines[random.nextInt(lines.length)];
				shared.add(entry, line);
				plain.add(entry, line);
			}
		}

		shared.generate();
		plain.generate();
		List<String> queries = new ArrayList<>(List.of("zzz", "when in", "hand", "shift", "minecraft", "a", ":"));

		for (String line : lines) {
			for (int i = 0; i < line.length(); i += 3) {
				queries.add(line.substring(i, Math.min(line.length(), i + 4)));
			}
		}

		int wrong = 0;

		for (String q : queries) {
			if (!new HashSet<>(shared.search(q)).equals(new HashSet(plain.search(q)))) {
				wrong++;
			}
		}

		int misses = wrong;
		check("shared texts find what a suffix array of a copy per entry finds, for " + queries.size() + " queries", () -> misses == 0);
	}

	private static void indexes() {
		EmiRecipeManager manager = EmiApi.getRecipeManager();
		Map<EmiStack, Set<EmiRecipe>> uses = new Object2ObjectOpenCustomHashMap(new EmiStackList.ComparisonHashStrategy());
		Map<EmiStack, Set<EmiRecipe>> makes = new Object2ObjectOpenCustomHashMap(new EmiStackList.ComparisonHashStrategy());

		for (EmiRecipe recipe : manager.getRecipes()) {
			List<EmiIngredient> used = new ArrayList<>(recipe.getInputs());
			used.addAll(recipe.getCatalysts());

			for (EmiIngredient ingredient : used) {
				for (EmiStack stack : ingredient.getEmiStacks()) {
					uses.computeIfAbsent(stack, k -> identitySet()).add(recipe);
				}
			}

			for (EmiStack stack : recipe.getOutputs()) {
				makes.computeIfAbsent(stack, k -> identitySet()).add(recipe);
			}
		}

		Map<EmiStack, Set<EmiRecipe>> at = new HashMap<>();

		for (EmiRecipeCategory category : manager.getCategories()) {
			for (EmiIngredient ingredient : manager.getWorkstations(category)) {
				for (EmiStack stack : ingredient.getEmiStacks()) {
					at.computeIfAbsent(stack, k -> identitySet()).addAll(manager.getRecipes(category));
				}
			}
		}

		check("each of " + uses.size() + " stacks finds the recipes that use it, each once", () -> indexed("the uses", uses, manager::getRecipesByInput));
		check("each of " + makes.size() + " stacks finds the recipes that make it, each once", () -> indexed("the makers", makes, manager::getRecipesByOutput));
		check(
			"each of " + at.size() + " workstations finds the recipes of the categories it serves",
			() -> indexed("the recipes at", at, s -> EmiRecipes.byWorkstation.getOrDefault(s, List.of()))
		);
		boolean appended = EmiConfig.appendItemModId;
		EmiConfig.appendItemModId = false;

		try {
			for (String text : List.of("when in", "hand", "damage")) {
				TooltipQuery query = new TooltipQuery(text);
				check(
					"a search of tooltips for \"" + text + "\" finds the stacks whose tooltip has it",
					() -> searched("tooltips: " + text, query::matches, query::matchesUnbaked, text.equals("when in"))
				);
			}

			SharedText<EmiStack> shared = new SharedText<>();
			SuffixArray<EmiStack> copies = new SuffixArray();

			for (EmiStack stack : EmiSearch.bakedStacks) {
				for (Component line : TooltipQuery.getText(stack)) {
					String text = line.getString().toLowerCase();
					shared.add(stack, text);
					copies.add(stack, text);
				}
			}

			shared.generate();
			copies.generate();

			for (String text : List.of("when in", "shift", "damage", "speed", ":", "e")) {
				check(
					"the game's tooltips, shared, find what a copy per stack finds for \"" + text + "\"",
					() -> new HashSet<>(shared.search(text)).equals(new HashSet(copies.search(text)))
				);
			}
		} finally {
			EmiConfig.appendItemModId = appended;
		}

		for (String text : List.of("minecraft", "cfh", "craft")) {
			ModQuery query = new ModQuery(text);
			Predicate<EmiStack> plain = s -> s.getItemStack().getItem() == Items.ENCHANTED_BOOK ? query.matches(s) : modHas(s, text);
			check(
				"a search of mods for \"" + text + "\" finds the stacks of mods whose name or id has it",
				() -> searched("mods: " + text, query::matches, plain, text.equals("minecraft"))
			);
		}

		Set<EmiStack> stacks = EmiSearch.bakedStacks;
		SuffixArray<SearchStack> names = EmiSearch.names;
		SuffixArray<SearchStack> tooltips = EmiSearch.tooltips;
		SuffixArray<SearchStack> mods = EmiSearch.mods;
		SuffixArray<EmiStack> aliases = EmiSearch.aliases;

		try {
			EmiSearch.bakedStacks = identitySet();
			EmiSearch.names = new SuffixArray();
			EmiSearch.tooltips = new SuffixArray();
			EmiSearch.mods = new SuffixArray();
			EmiSearch.aliases = new SuffixArray();

			for (String text : List.of("stone", "@minecraft", "$damage", "-stone")) {
				check("a search for \"" + text + "\" before the index is built does not fail, and finds stacks without it", () -> {
					EmiSearch.CompiledQuery query = new EmiSearch.CompiledQuery(text);
					int found = 0;

					for (EmiStack stack : stacks) {
						if (query.test(stack)) {
							found++;
						}
					}

					return found > 0 && (text.startsWith("@") || text.startsWith("$") || found < stacks.size());
				});
			}
		} finally {
			EmiSearch.bakedStacks = stacks;
			EmiSearch.names = names;
			EmiSearch.tooltips = tooltips;
			EmiSearch.mods = mods;
			EmiSearch.aliases = aliases;
		}
	}

	private static boolean indexed(String what, Map<EmiStack, Set<EmiRecipe>> expected, Function<EmiStack, List<EmiRecipe>> index) {
		int wrong = 0;

		for (Entry<EmiStack, Set<EmiRecipe>> e : expected.entrySet()) {
			List<EmiRecipe> found = index.apply(e.getKey());
			Set<EmiRecipe> distinct = identitySet();
			distinct.addAll(found);
			if ((distinct.size() != found.size() || !distinct.equals(e.getValue())) && wrong++ < 3) {
				CFHGuiCrafting.LOGGER
					.error(
						"[CFHGui Crafting] tree self-check: {} {}: {} recipes ({} distinct), {} expected",
						new Object[]{what, e.getKey(), found.size(), distinct.size(), e.getValue().size()}
					);
			}
		}

		return wrong == 0;
	}

	private static boolean searched(String what, Predicate<EmiStack> index, Predicate<EmiStack> plain, boolean mustFind) {
		int wrong = 0;
		int found = 0;

		for (EmiStack stack : EmiSearch.bakedStacks) {
			boolean byIndex = index.test(stack);
			if (byIndex) {
				found++;
			}

			if (byIndex != plain.test(stack) && wrong++ < 3) {
				CFHGuiCrafting.LOGGER.error("[CFHGui Crafting] tree self-check: the search of {} {} {}", new Object[]{what, byIndex ? "finds" : "misses", stack});
			}
		}

		return wrong == 0 && (found > 0 || !mustFind);
	}

	private static boolean modHas(EmiStack stack, String text) {
		ResourceLocation id = stack.getId();
		return id != null && (EmiUtil.getModName(id.getNamespace()).toLowerCase().contains(text) || id.getNamespace().toLowerCase().contains(text));
	}

	private static <T> Set<T> identitySet() {
		return Collections.newSetFromMap(new IdentityHashMap<>());
	}

	private TreeSelfCheck() {
	}
}
