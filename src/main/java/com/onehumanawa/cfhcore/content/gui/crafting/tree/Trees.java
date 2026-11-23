package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import com.onehumanawa.cfhcore.content.gui.emi.api.EmiApi;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiPlayerInventory;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.bom.BoM;
import com.onehumanawa.cfhcore.content.gui.emi.bom.MaterialTree;
import com.onehumanawa.cfhcore.content.gui.emi.config.SidebarType;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiFavorites;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiReloadManager;
import com.onehumanawa.cfhcore.content.gui.emi.screen.EmiScreenManager;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class Trees {
	private static final int LOOK_EVERY = 250;
	private static final int WRITE_AFTER = 1500;
	private static TreeStore store;
	@Nullable
	private static TreeEntry shown;
	private static int generation = -1;
	@Nullable
	private static EmiPlayerInventory inventory;
	private static int inventoryVersion;
	private static long lookedAt;
	private static long lookSoonUntil;
	private static int seenInventory = -1;
	@Nullable
	private static TreeSession seenSession;
	@Nullable
	private static AbstractContainerScreen<?> seenContainer;
	private static long seenCelebrated;
	private static long dirtySince;

	public static TreeStore store() {
		if (store == null) {
			boolean harness = (System.getProperty("cfh.autoScreens") != null || Boolean.getBoolean("cfh.autoWorld"))
				&& !Boolean.getBoolean("cfh.crafting.keepTrees");
			store = new TreeStore(harness ? null : TreeStore.defaultFile());
			store.load();
		}

		return store;
	}

	@Nullable
	public static TreeSession active() {
		reloaded();
		MaterialTree tree = BoM.tree;
		if (tree == null) {
			if (shown != null) {
				shown = null;
				store().shown = null;
				store().touch();
			}

			return null;
		} else if (shown != null && shown.session != null && shown.session.tree == tree) {
			return shown.session;
		} else {
			for (TreeEntry e : store().all()) {
				if (e.session != null && e.session.tree == tree) {
					return take(e);
				}
			}

			TreeEntry ex = store().create();
			ex.adopt(new TreeSession(tree, new TreeMarks()).grown());
			ex.write();
			store().trimRecent(ex);
			return take(ex);
		}
	}

	private static TreeSession take(TreeEntry e) {
		shown = e;
		e.lastUsed = System.currentTimeMillis();
		store().shown = e.id;
		store().touch();
		seenSession = null;
		return e.session;
	}

	@Nullable
	public static TreeEntry shown() {
		return active() == null ? null : shown;
	}

	public static boolean show(TreeEntry entry) {
		active();
		if (shown != null) {
			shown.write();
		}

		TreeSession s = entry.session();
		if (s == null) {
			return false;
		} else {
			BoM.tree = s.tree;
			BoM.craftingMode = false;
			take(entry);
			synthetic();
			return true;
		}
	}

	public static TreeSession start(EmiRecipe recipe) {
		active();
		if (shown != null) {
			shown.write();
		}

		BoM.setGoal(recipe);
		synthetic();
		return active();
	}

	@Nullable
	static TreeEntry of(EmiRecipe recipe) {
		if (recipe.getId() == null) {
			return null;
		} else {
			String id = recipe.getId().toString();
			TreeEntry recent = null;

			for (TreeEntry e : store().all()) {
				String goal = e.session != null
					? (e.session.tree.goal.recipe != null && e.session.tree.goal.recipe.getId() != null ? e.session.tree.goal.recipe.getId().toString() : null)
					: TreeCodec.goalRecipe(e.json);
				if (id.equals(goal)) {
					if (e.saved) {
						return e;
					}

					if (recent == null || e.lastUsed > recent.lastUsed) {
						recent = e;
					}
				}
			}

			return recent;
		}
	}

	public static void clear() {
		TreeEntry was = shown();
		BoM.tree = null;
		BoM.craftingMode = false;
		shown = null;
		store().shown = null;
		if (was != null && !was.saved) {
			store().remove(was);
		}

		store().touch();
		synthetic();
	}

	public static void save(TreeEntry entry, boolean saved) {
		if (entry.saved != saved) {
			entry.saved = saved;
			entry.pinned = saved;
			entry.write();
			store().trimRecent(shown);
			store().touch();
			store().save();
		}
	}

	public static void delete(TreeEntry entry) {
		if (entry == shown) {
			clear();
		}

		store().remove(entry);
		store().save();
	}

	static void synthetic() {
		if (EmiReloadManager.isLoaded()) {
			try {
				if (EmiScreenManager.lastPlayerInventory != null) {
					EmiFavorites.updateSynthetic(EmiScreenManager.lastPlayerInventory);
				}

				EmiScreenManager.repopulatePanels(SidebarType.FAVORITES);
			} catch (RuntimeException var1) {
			}
		}
	}

	private static void reloaded() {
		if (generation != BoM.generation && EmiReloadManager.isLoaded()) {
			generation = BoM.generation;
			if (shown != null && shown.session != null && BoM.tree == shown.session.tree) {
				TreeSession s = shown.session();
				if (s != null) {
					BoM.tree = s.tree;
				} else {
					BoM.tree = null;
					shown = null;
				}
			} else if (BoM.tree == null) {
				TreeEntry last = store().byId(store().shown);
				TreeSession s = last == null ? null : last.session();
				if (s != null) {
					BoM.tree = s.tree;
					shown = last;
				}
			}

			seenSession = null;
		}
	}

	static void tick() {
		if (EmiReloadManager.isLoaded()) {
			TreeSession s = active();
			if (s != null && shown != null && shown.write()) {
				store().touch();
			}

			long now = Clock.nowMs();
			if (!store().dirty()) {
				dirtySince = 0L;
			} else {
				if (dirtySince == 0L) {
					dirtySince = now;
				}

				if (now - dirtySince >= 1500L) {
					store().save();
					dirtySince = 0L;
				}
			}
		}
	}

	static void flush() {
		if (shown != null && shown.session != null && shown.write()) {
			store().touch();
		}

		store().save();
	}

	public static EmiPlayerInventory inventory() {
		long now = Clock.nowMs();
		if (inventory == null || now - lookedAt >= (long)(now < lookSoonUntil ? 50 : 250)) {
			lookedAt = now;
			EmiPlayerInventory is = EmiPlayerInventory.of(Minecraft.getInstance().player);
			if (inventory == null || !is.isEqual(inventory)) {
				inventory = is;
				inventoryVersion++;
			}
		}

		return inventory;
	}

	public static int inventoryVersion() {
		return inventoryVersion;
	}

	static void lookSoon() {
		lookSoonUntil = Clock.nowMs() + 1200L;
	}

	@Nullable
	public static TreeSession sync() {
		TreeSession s = active();
		if (s == null) {
			return null;
		} else {
			EmiPlayerInventory inv = inventory();
			AbstractContainerScreen<?> container = EmiApi.getHandledScreen();
			boolean sameTree = seenSession == s && !s.stale();
			if (sameTree && seenInventory == inventoryVersion && seenContainer == container) {
				return s;
			} else {
				boolean moved = sameTree && seenContainer == container;
				s.look(inv, container, moved);
				seenSession = s;
				seenInventory = inventoryVersion;
				seenContainer = container;
				if (s.celebrated != seenCelebrated) {
					seenCelebrated = s.celebrated;
					if (moved && s.celebrated != 0L && TreeConfig.get().doneToast) {
						CFHGuiToasts.show(Component.translatable("cfhgui_crafting.tree.toast.done"), s.displayName(), Icon.CHECK);
					}
				}

				return s;
			}
		}
	}

	public static float fraction(TreeEntry entry) {
		TreeSession s = entry.session();
		if (s == null) {
			return -1.0F;
		} else if (entry == shown && s == seenSession) {
			return s.fraction();
		} else {
			inventory();
			if (entry.looked != inventoryVersion || s.stale()) {
				s.look(inventory, null, false);
				entry.looked = inventoryVersion;
			}

			return s.fraction();
		}
	}

	public static List<TreeEntry> quick() {
		List<TreeEntry> pinned = store().pinned();
		int max = Math.max(1, TreeConfig.get().chips);
		return pinned.size() > max ? pinned.subList(0, max) : pinned;
	}

	@Nullable
	public static TreeEntry recent() {
		TreeEntry now = shown();

		for (TreeEntry e : store().recent()) {
			if (e != now) {
				return e;
			}
		}

		return null;
	}

	private Trees() {
	}
}
