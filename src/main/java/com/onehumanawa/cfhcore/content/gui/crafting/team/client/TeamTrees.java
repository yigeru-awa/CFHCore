package com.onehumanawa.cfhcore.content.gui.crafting.team.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.content.gui.emi.api.EmiApi;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiContextMenu;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup.Item;
import com.onehumanawa.cfhcore.content.gui.crafting.team.Pin;
import com.onehumanawa.cfhcore.content.gui.crafting.team.PinRules;
import com.onehumanawa.cfhcore.content.gui.crafting.tree.TreeCodec;
import com.onehumanawa.cfhcore.content.gui.crafting.tree.TreeDockSeam;
import com.onehumanawa.cfhcore.content.gui.crafting.tree.TreeEntry;
import com.onehumanawa.cfhcore.content.gui.crafting.tree.TreeNode;
import com.onehumanawa.cfhcore.content.gui.crafting.tree.TreeSession;
import com.onehumanawa.cfhcore.content.gui.crafting.tree.TreeStore;
import com.onehumanawa.cfhcore.content.gui.crafting.tree.Trees;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class TeamTrees {
	private static final TreeStore STANDS = new TreeStore(null);
	private static final Map<TreeEntry, Pin> BY_ENTRY = new IdentityHashMap<>();
	private static List<TreeEntry> quick = List.of();
	private static int builtAt = -1;
	private static final Map<String, String> LINKS = new HashMap<>();
	private static final Map<String, String> COPIES = new HashMap<>();
	@Nullable
	private static JsonObject sameAsJson;
	@Nullable
	private static Pin sameAsPin;
	private static int sameAsVersion = -1;
	private static final Map<CFHGuiIconButton, String> SHOWS = new WeakHashMap<>();

	public static List<TreeEntry> quick() {
		if (!TeamPinsClient.active()) {
			return List.of();
		} else {
			if (builtAt != TeamPinsClient.version()) {
				builtAt = TeamPinsClient.version();
				Map<String, TreeEntry> had = new HashMap<>();

				for (Entry<TreeEntry, Pin> e : BY_ENTRY.entrySet()) {
					had.put(e.getValue().id, e.getKey());
				}

				BY_ENTRY.clear();
				List<TreeEntry> out = new ArrayList<>();

				for (Pin pin : TeamPinsClient.pins()) {
					if (pin.isTree() && pin.tree != null) {
						TreeEntry e = had.remove(pin.id);
						if (e == null) {
							e = STANDS.create();
							e.json = pin.tree.deepCopy();
						}

						BY_ENTRY.put(e, pin);
						out.add(e);
					}
				}

				for (TreeEntry gone : had.values()) {
					STANDS.remove(gone);
				}

				quick = List.copyOf(out);
			}

			return quick;
		}
	}

	public static boolean isTeam(@Nullable TreeEntry entry) {
		return entry != null && BY_ENTRY.containsKey(entry);
	}

	public static void mark(GuiGraphics g, int cx, int cy, int radius) {
		int x = cx + radius - 3;
		int y = cy - radius + 3;
		int ground = Theme.current().isVanilla() ? -15724528 : Theme.current().palette().bg();
		g.pose().pushPose();
		g.pose().translate(0.0F, 0.0F, 260.0F);
		CFHGuiDraw.pixelCircle(g, x, y, 4, ground);
		CFHGuiDraw.pixelCircle(g, x, y, 3, TeamPinsClient.look().color());
		g.pose().popPose();
	}

	public static void tip(TreeEntry entry, List<Component> lines) {
		Pin pin = BY_ENTRY.get(entry);
		if (pin != null) {
			if (!lines.isEmpty()) {
				lines.remove(lines.size() - 1);
			}

			lines.add(TeamText.pinnedBy(pin));
			lines.add(TeamText.hint("cfhgui_crafting.team.tip.chip_keys"));
		}
	}

	public static boolean open(TreeEntry entry) {
		Pin pin = BY_ENTRY.get(entry);
		return pin != null && show(pin);
	}

	public static void menu(TreeEntry entry, double x, double y) {
		Pin pin = BY_ENTRY.get(entry);
		if (pin != null) {
			List<Item> items = new ArrayList<>();
			items.add(Item.of(Component.translatable("cfhgui_crafting.team.menu.open_tree"), Icon.LAN, () -> show(pin)));
			items.add(Item.of(Component.translatable("cfhgui_crafting.team.menu.open_planner"), Icon.FULLSCREEN, () -> {
				if (show(pin)) {
					EmiApi.viewRecipeTree();
				}
			}));
			items.add(Item.sep());
			if (TeamPinsClient.mayUnpin(pin)) {
				items.add(Item.danger(Component.translatable("cfhgui_crafting.team.menu.unpin"), Icon.PIN, () -> TeamPinsClient.unpin(pin)));
			} else {
				items.add(Item.disabled(Component.translatable("cfhgui_crafting.team.menu.pinned_by", new Object[]{pin.byName}), Icon.LOCK));
			}

			CFHGuiContextMenu.open(x, y, items);
		}
	}

	static boolean open(Pin pin, boolean planner) {
		if (!show(pin)) {
			return false;
		} else {
			if (planner) {
				EmiApi.viewRecipeTree();
			} else if (!TreeDockSeam.isOpen()) {
				TreeDockSeam.buttonPressed();
			}

			return true;
		}
	}

	private static boolean show(Pin pin) {
		if (pin.tree == null) {
			return false;
		} else {
			String had = COPIES.get(pin.id);
			TreeEntry copy = had == null ? null : Trees.store().byId(had);
			boolean made = copy == null;
			if (made) {
				copy = Trees.store().create();
				copy.json = pin.tree.deepCopy();
				COPIES.put(pin.id, copy.id);
				LINKS.put(copy.id, pin.id);
			}

			if (copy == Trees.shown()) {
				return true;
			} else if (!Trees.show(copy)) {
				if (made) {
					Trees.store().remove(copy);
					COPIES.remove(pin.id);
				}

				CFHGuiToasts.show(Component.translatable("cfhgui_crafting.team.refused"), Component.translatable("cfhgui_crafting.team.unknown_tree"), Icon.WARNING);
				return false;
			} else {
				if (made) {
					Trees.store().trimRecent(copy);
				}

				return true;
			}
		}
	}

	static Component name(Pin pin) {
		JsonElement name = pin.tree == null ? null : pin.tree.get("name");
		if (name != null && name.isJsonPrimitive() && !name.getAsString().isBlank()) {
			return Component.literal(name.getAsString());
		} else {
			EmiStack goal = pin.tree == null ? null : TreeCodec.goal(pin.tree);
			return (Component)(goal != null ? goal.getName() : Component.literal(pin.recipe));
		}
	}

	@Nullable
	static Pin linked(TreeEntry entry) {
		String id = LINKS.get(entry.id);
		Pin pin = id == null ? null : TeamPinsClient.pin(id);
		if (pin != null) {
			return pin;
		} else {
			JsonObject json = entry.json;
			if (json == null) {
				return null;
			} else {
				if (json != sameAsJson || sameAsVersion != TeamPinsClient.version()) {
					sameAsJson = json;
					sameAsVersion = TeamPinsClient.version();
					sameAsPin = null;

					for (Pin p : TeamPinsClient.pins()) {
						if (p.isTree() && json.equals(p.tree)) {
							sameAsPin = p;
							break;
						}
					}
				}

				return sameAsPin;
			}
		}
	}

	public static CFHGuiIconButton button(int size) {
		CFHGuiIconButton b = new CFHGuiIconButton(0, 0, size, Icon.PIN, Component.empty(), TeamTrees::press);
		b.visible = false;
		return b;
	}

	public static boolean sync(CFHGuiIconButton b) {
		TreeEntry entry = TeamPinsClient.active() ? Trees.shown() : null;
		if (entry == null) {
			b.visible = false;
			return false;
		} else {
			Pin pin = linked(entry);
			String state = pin == null ? (entry.writable() ? "pin" : "cannot") : (TeamPinsClient.mayUnpin(pin) ? "unpin:" : "theirs:") + pin.id;
			b.visible = true;
			b.active = pin != null ? TeamPinsClient.mayUnpin(pin) : entry.writable();
			if (!state.equals(SHOWS.put(b, state))) {
				b.toggled(pin != null);
				List<Component> tip = new ArrayList<>();
				if (pin == null) {
					tip.add(Component.translatable(entry.writable() ? "cfhgui_crafting.team.tree.pin" : "cfhgui_crafting.team.tree.cannot"));
					tip.add(TeamText.hint(entry.writable() ? "cfhgui_crafting.team.tree.pin.tip" : "cfhgui_crafting.team.tree.cannot.tip"));
				} else {
					tip.add(Component.translatable("cfhgui_crafting.team.tree.pinned"));
					tip.add(TeamText.pinnedBy(pin));
					tip.add(TeamText.hint(TeamPinsClient.mayUnpin(pin) ? "cfhgui_crafting.team.tree.unpin.tip" : "cfhgui_crafting.team.tree.theirs.tip"));
				}

				b.tip(tip);
			}

			return true;
		}
	}

	public static void press() {
		TreeEntry entry = Trees.shown();
		if (entry != null && TeamPinsClient.active()) {
			Pin pin = linked(entry);
			if (pin != null) {
				TeamPinsClient.unpin(pin);
			} else {
				pin(entry);
			}
		}
	}

	public static void menuItems(List<Item> items) {
		TreeEntry entry = TeamPinsClient.active() ? Trees.shown() : null;
		if (entry != null) {
			Pin pin = linked(entry);
			items.add(Item.sep());
			if (pin == null) {
				items.add(Item.of(Component.translatable("cfhgui_crafting.team.tree.pin"), Icon.PIN, TeamTrees::press));
			} else if (TeamPinsClient.mayUnpin(pin)) {
				items.add(Item.of(Component.translatable("cfhgui_crafting.team.menu.unpin"), Icon.PIN, TeamTrees::press));
			} else {
				items.add(Item.disabled(Component.translatable("cfhgui_crafting.team.menu.pinned_by", new Object[]{pin.byName}), Icon.LOCK));
			}
		}
	}

	static void pin(TreeEntry entry) {
		TreeSession s = entry.session();
		if (s != null) {
			JsonObject json = TreeCodec.write(s.tree, s.marks);
			String recipe = PinRules.goalRecipe(json);
			if (json != null && recipe != null) {
				int steps = (int)Math.max(1L, s.outline().all.stream().filter(TreeNode::step).count());
				byte[] packed = JsonWire.encode(json);
				if (steps <= 2000 && packed.length <= 30000) {
					TeamPinsClient.ask("pin_tree", recipe, packed, steps, ok -> {
						if (ok) {
							Pin made = null;

							for (Pin p : TeamPinsClient.pins()) {
								if (p.isTree() && p.by.equals(TeamPinsClient.self()) && json.equals(p.tree)) {
									made = p;
								}
							}

							if (made != null) {
								LINKS.put(entry.id, made.id);
							}
						}
					});
				} else {
					refused("too_big");
				}
			} else {
				refused("cannot");
			}
		}
	}

	private static void refused(String code) {
		CFHGuiToasts.show(Component.translatable("cfhgui_crafting.team.refused"), Component.translatable("cfhgui_crafting.team.refused." + code), Icon.WARNING);
	}

	static void forget() {
		LINKS.clear();
		COPIES.clear();

		for (TreeEntry e : new ArrayList<>(STANDS.all())) {
			STANDS.remove(e);
		}

		BY_ENTRY.clear();
		quick = List.of();
		builtAt = -1;
	}

	private TeamTrees() {
	}
}
