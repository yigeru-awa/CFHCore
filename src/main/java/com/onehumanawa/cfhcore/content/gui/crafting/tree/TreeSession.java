package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiPlayerInventory;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.bom.ChanceMaterialCost;
import com.onehumanawa.cfhcore.content.gui.emi.bom.FlatMaterialCost;
import com.onehumanawa.cfhcore.content.gui.emi.bom.FoldState;
import com.onehumanawa.cfhcore.content.gui.emi.bom.MaterialNode;
import com.onehumanawa.cfhcore.content.gui.emi.bom.MaterialTree;
import com.onehumanawa.cfhcore.content.gui.emi.registry.EmiStackList;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiReloadManager;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class TreeSession {
	public final MaterialTree tree;
	public final TreeMarks marks;
	@Nullable
	public String selected;
	public boolean foldMode;
	@Nullable
	public String firstPoint;
	private TreeOutline outline;
	private int outlineRevision = -1;
	private List<TreeRow> rows;
	private int rowsView = -1;
	private int view;
	private int changes;
	private Map<String, TreeFigures> figures = Map.of();
	private final Map<String, TreeFigures.Fold> folds = new HashMap<>();
	private int looks;
	private boolean stale = true;
	private long batchesSeen = -1L;
	private List<TreeFigures.Raw> raws = List.of();
	private List<TreeFigures.Raw> leftovers = List.of();
	private int places;
	private int placesDone;
	private int steps;
	private int stepsDone;
	private int stepsReady;
	public final Map<String, Long> ticked = new HashMap<>();
	public final Map<String, Long> lit = new HashMap<>();
	public long celebrated;
	public final Map<EmiIngredient, EmiRecipe> found = new HashMap<>();
	private int asked;
	private static final int ASK_AT_MOST = 4000;
	private static final Comparator<TreeFigures.Raw> BY_INDEX = Comparator.comparingInt(raw -> EmiStackList.getIndex(raw.ingredient));
	private static final TreeFigures BLANK = new TreeFigures();

	public TreeSession(MaterialTree tree, TreeMarks marks) {
		this.tree = tree;
		this.marks = marks;
		tree.fallback = this::find;
		tree.recalculated = this::recalculated;
	}

	public TreeSession grown() {
		this.tree.recalculate();
		return this;
	}

	@Nullable
	private EmiRecipe find(EmiIngredient ingredient) {
		if (++this.asked > 4000) {
			return null;
		} else {
			EmiRecipe kept = this.found.get(ingredient);
			return kept != null ? kept : TreeSuggest.of(ingredient);
		}
	}

	private void recalculated() {
		this.asked = 0;
		this.outline = null;
		this.stale = true;
		this.changes++;
		if (!this.marks.given.isEmpty()) {
			this.outline();
		}
	}

	public TreeOutline outline() {
		if (this.outline == null || this.outlineRevision != this.tree.revision) {
			this.outline = TreeOutline.of(this.tree);
			this.outlineRevision = this.tree.revision;
			if (EmiReloadManager.isLoaded() && this.marks.prune(this.outline)) {
				this.changes++;
			}

			this.marks.given.removeIf(pathx -> {
				TreeNode node = this.outline.at(pathx);
				return node != null && !node.hasRecipe();
			});

			for (String path : this.marks.given) {
				give(this.outline.at(path), true);
			}

			if (this.selected != null && !this.outline.has(this.selected)) {
				this.selected = null;
			}

			if (this.firstPoint != null && !this.outline.has(this.firstPoint)) {
				this.firstPoint = null;
			}

			this.view++;
			this.stale = true;
		}

		return this.outline;
	}

	private static void give(@Nullable TreeNode node, boolean given) {
		if (node != null && node.node != null) {
			node.given = given;
			node.node.state = given ? FoldState.COLLAPSED : FoldState.EXPANDED;
		}
	}

	public List<TreeRow> rows() {
		TreeOutline o = this.outline();
		if (this.rows == null || this.rowsView != this.view) {
			this.rows = this.marks.rows(o);
			this.rowsView = this.view;
			this.folds.clear();
		}

		return this.rows;
	}

	public int rowsVersion() {
		this.outline();
		return this.view;
	}

	public int changes() {
		return this.changes;
	}

	public int looks() {
		return this.looks;
	}

	public Component displayName() {
		return (Component)(!this.marks.name.isBlank() ? Component.literal(this.marks.name) : this.tree.goal.ingredient.getEmiStacks().get(0).getName());
	}

	public boolean stale() {
		return this.stale || this.batchesSeen != this.tree.batches || this.outlineRevision != this.tree.revision;
	}

	public void look(EmiPlayerInventory inventory, @Nullable AbstractContainerScreen<?> container, boolean moved) {
		TreeOutline o = this.outline();
		Map<String, TreeFigures> before = this.figures;
		Map<String, TreeFigures> now = new HashMap<>(o.all.size() * 2);
		this.tree.calculateProgress(inventory);
		TreeCraft.Look look = new TreeCraft.Look(inventory, container);
		this.places = this.placesDone = this.steps = this.stepsDone = this.stepsReady = 0;
		this.read(o.goal, look, now);
		Map<EmiIngredient, FlatMaterialCost> left = new HashMap<>(this.tree.cost.costs);
		Map<EmiIngredient, ChanceMaterialCost> leftByChance = new HashMap<>(this.tree.cost.chanceCosts);
		this.tree.calculateCost();
		this.raws = this.costs(left, leftByChance);
		this.leftovers = this.leftOver();
		this.figures = now;
		this.folds.clear();
		this.looks++;
		this.stale = false;
		this.batchesSeen = this.tree.batches;
		long at = Clock.nowMs();
		this.ticked.values().removeIf(t -> at - t > 4000L);
		this.lit.values().removeIf(t -> at - t > 4000L);
		if (moved && !before.isEmpty()) {
			this.note(before, now, at);
		}
	}

	private void read(TreeNode node, TreeCraft.Look look, Map<String, TreeFigures> into) {
		MaterialNode m = node.node;
		TreeFigures f = new TreeFigures();
		f.wanted = m.wanted;
		f.missing = m.missing;
		f.held = m.held;
		f.chance = m.wantedChance;
		EmiRecipe recipe = node.recipe();
		f.recipe = recipe;
		this.places++;
		if (node.step()) {
			this.steps++;
		}

		if (f.missing <= 0L && f.wanted > 0L && f.held < f.wanted) {
			f.state = TreeFigures.State.COVERED;
		} else if (f.missing <= 0L) {
			f.state = TreeFigures.State.DONE;
			this.placesDone++;
			if (node.step()) {
				this.stepsDone++;
			}
		} else if (node.step()) {
			f.crafts = Math.max(1L, m.neededBatches);
			if (look.ready(recipe)) {
				f.state = TreeFigures.State.READY;
				f.readyForAll = look.ready(recipe, f.crafts);
				f.here = look.here(recipe);
				this.stepsReady++;
			} else {
				f.state = TreeFigures.State.WAITING;
			}
		} else {
			f.state = node.given ? TreeFigures.State.GIVEN : TreeFigures.State.MISSING;
		}

		into.put(node.path, f);
		if (node.hasChildren()) {
			for (TreeNode child : node.children) {
				this.read(child, look, into);
			}
		}
	}

	private List<TreeFigures.Raw> costs(Map<EmiIngredient, FlatMaterialCost> left, Map<EmiIngredient, ChanceMaterialCost> leftByChance) {
		List<TreeFigures.Raw> out = new ArrayList<>();

		for (FlatMaterialCost cost : this.tree.cost.costs.values()) {
			TreeFigures.Raw raw = new TreeFigures.Raw(cost.ingredient);
			raw.wanted = cost.amount;
			FlatMaterialCost still = left.get(cost.ingredient);
			raw.missing = still == null ? 0L : Math.min(raw.wanted, still.amount);
			out.add(raw);
		}

		for (ChanceMaterialCost cost : this.tree.cost.chanceCosts.values()) {
			TreeFigures.Raw raw = new TreeFigures.Raw(cost.ingredient);
			raw.estimated = true;
			raw.wanted = cost.getEffectiveAmount();
			ChanceMaterialCost still = leftByChance.get(cost.ingredient);
			long done = still == null ? raw.wanted : (long)Math.ceil((double)((float)cost.amount * cost.chance - (float)still.amount * still.chance));
			raw.missing = Math.max(0L, Math.min(raw.wanted, raw.wanted - done));
			out.add(raw);
		}

		out.sort(BY_INDEX);
		return out;
	}

	private List<TreeFigures.Raw> leftOver() {
		List<TreeFigures.Raw> out = new ArrayList<>();

		for (FlatMaterialCost cost : this.tree.cost.remainders.values()) {
			if (cost.getEffectiveAmount() > 0L) {
				TreeFigures.Raw raw = new TreeFigures.Raw(cost.ingredient);
				raw.wanted = cost.getEffectiveAmount();
				out.add(raw);
			}
		}

		for (ChanceMaterialCost costx : this.tree.cost.chanceRemainders.values()) {
			if (costx.getEffectiveAmount() > 0L) {
				TreeFigures.Raw raw = new TreeFigures.Raw(costx.ingredient);
				raw.estimated = true;
				raw.wanted = costx.getEffectiveAmount();
				out.add(raw);
			}
		}

		out.sort(BY_INDEX);
		return out;
	}

	private void note(Map<String, TreeFigures> before, Map<String, TreeFigures> now, long at) {
		for (Entry<String, TreeFigures> e : now.entrySet()) {
			TreeFigures was = before.get(e.getKey());
			TreeFigures is = e.getValue();
			if (was != null) {
				if (was.wanted != is.wanted || was.missing != is.missing || was.held != is.held) {
					this.ticked.put(e.getKey(), at);
				}

				if (is.state == TreeFigures.State.READY && was.state != TreeFigures.State.READY) {
					this.lit.put(e.getKey(), at);
				}
			}
		}

		TreeFigures was = before.get("");
		TreeFigures is = now.get("");
		if (was != null && is != null && is.state == TreeFigures.State.DONE && was.state != TreeFigures.State.DONE) {
			this.celebrated = at;
		}
	}

	public TreeFigures figures(String path) {
		TreeFigures f = this.figures.get(path);
		return f != null ? f : BLANK;
	}

	public TreeFigures.Fold fold(TreeRow row) {
		return this.folds.computeIfAbsent(row.id, id -> {
			TreeFigures.Fold fold = new TreeFigures.Fold();
			Map<EmiIngredient, TreeFigures.Raw> raws = new LinkedHashMap<>();
			int deepest = -1;

			for (TreeNode node : row.hidden) {
				TreeFigures f = this.figures(node.path);
				if (node.step()) {
					fold.steps++;
					if (fold.first.size() < 8) {
						fold.first.add(node);
					}

					if (f.state == TreeFigures.State.DONE) {
						fold.done++;
					}

					if (f.state == TreeFigures.State.READY) {
						fold.ready = true;
						if (f.here && node.depth > deepest) {
							deepest = node.depth;
							fold.next = node;
						}
					}
				} else if (node.ingredient() != null) {
					TreeFigures.Raw raw = raws.computeIfAbsent(node.ingredient(), TreeFigures.Raw::new);
					raw.wanted = raw.wanted + f.wanted;
					raw.missing = raw.missing + f.missing;
					raw.estimated = raw.estimated | f.estimated();
				}
			}

			for (TreeFigures.Raw raw : raws.values()) {
				if (raw.wanted > 0L) {
					fold.raws.add(raw);
				}
			}

			return fold;
		});
	}

	public List<TreeFigures.Raw> raws() {
		return this.raws;
	}

	public List<TreeFigures.Raw> leftovers() {
		return this.leftovers;
	}

	public float fraction() {
		return this.places <= 0 ? 0.0F : (float)this.placesDone / (float)this.places;
	}

	public int steps() {
		return this.steps;
	}

	public int stepsDone() {
		return this.stepsDone;
	}

	public int stepsReady() {
		return this.stepsReady;
	}

	public boolean complete() {
		return this.figures("").state == TreeFigures.State.DONE && !this.figures.isEmpty();
	}

	public void fold(String path, boolean folded) {
		if (folded ? this.marks.folded.add(path) : this.marks.folded.remove(path)) {
			this.changed();
		}
	}

	public boolean folded(String path) {
		return this.marks.folded.contains(path);
	}

	public void foldAll() {
		for (TreeNode node : this.outline().all) {
			if (node.depth > 0 && node.hasChildren()) {
				this.marks.folded.add(node.path);
			}
		}

		for (Island island : this.marks.islands) {
			island.open = false;
		}

		this.changed();
	}

	public void unfoldAll() {
		this.marks.folded.clear();

		for (Island island : this.marks.islands) {
			island.open = true;
		}

		this.changed();
	}

	public void give(String path, boolean given) {
		TreeNode node = this.outline().at(path);
		if (node != null && node.depth != 0 && node.hasRecipe()) {
			if (given ? this.marks.given.add(path) : this.marks.given.remove(path)) {
				give(node, given);
				this.stale = true;
				this.changed();
			}
		}
	}

	@Nullable
	public TreeRows.Refusal island(String first, String second) {
		TreeRows.Refusal refusal = this.marks.addIsland(this.outline(), first, second);
		if (refusal == null) {
			this.changed();
		}

		return refusal;
	}

	public void open(Island island, boolean open) {
		if (island.open != open) {
			island.open = open;
			this.changed();
		}
	}

	public void remove(Island island) {
		if (this.marks.islands.remove(island)) {
			this.changed();
		}
	}

	public void batches(long batches) {
		long to = Math.max(1L, batches);
		if (to != this.tree.batches) {
			this.tree.batches = to;
			this.changes++;
		}
	}

	public void choose(TreeNode node, @Nullable EmiRecipe recipe) {
		EmiIngredient asked = node.asked();
		if (asked != null) {
			this.tree.addResolution(asked, recipe);
		}
	}

	public void rename(String name) {
		String to = name == null ? "" : name.trim();
		if (to.equals(this.tree.goal.ingredient.getEmiStacks().get(0).getName().getString())) {
			to = "";
		}

		if (!to.equals(this.marks.name)) {
			this.marks.name = to;
			this.changes++;
		}
	}

	private void changed() {
		this.view++;
		this.changes++;
	}
}
