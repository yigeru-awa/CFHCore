package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import com.onehumanawa.cfhcore.content.gui.config.CFHGuiConfigApi;
import com.onehumanawa.cfhcore.content.gui.config.option.Binding;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionType;
import com.onehumanawa.cfhcore.content.gui.config.ui.Section;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.network.chat.Component;

public final class TreeSettings {
	private static final TreeConfig DEFAULTS = new TreeConfig();

	static void register() {
		CFHGuiConfigApi.registerReloadHook("crafting", () -> TreeConfig.file().load());
	}

	public static Section section() {
		Section tree = new Section("crafting_tree", text("section"), text("section.about"));
		tree.add(bool("openDockWithNewTree", c -> c.openDockWithNewTree, (c, v) -> c.openDockWithNewTree = v));
		tree.add(bool("findRecipes", c -> c.findRecipes, (c, v) -> c.findRecipes = v));
		tree.add(bool("craftIntoInventory", c -> c.craftIntoInventory, (c, v) -> c.craftIntoInventory = v));
		tree.add(bool("names", c -> c.names, (c, v) -> c.names = v));
		tree.add(bool("workstations", c -> c.workstations, (c, v) -> c.workstations = v).enabledIf(() -> TreeConfig.get().names));
		tree.add(number("dockWidth", 0, 320, 4, c -> c.dockWidth, (c, v) -> {
			c.dockWidth = v.intValue() > 0 && v.intValue() < 80 ? 80 : v.intValue();
			TreeDockSeam.changed();
		}).format(v -> (Component)(((Number)v).intValue() <= 0 ? text("dockWidth.auto") : Component.literal(((Number)v).intValue() + " px"))));
		tree.add(number("chips", 1, 12, 1, c -> c.chips, (c, v) -> c.chips = v.intValue()));
		tree.add(bool("doneToast", c -> c.doneToast, (c, v) -> c.doneToast = v));
		return tree;
	}

	private static Component text(String key) {
		return Component.translatable("cfhgui_crafting.tree.settings." + key);
	}

	private static void set(Consumer<TreeConfig> change) {
		change.accept(TreeConfig.get());
		TreeConfig.save();
	}

	private static Binding row(String field, OptionType type) {
		return Binding.of("cfhgui_crafting:tree." + field, type, text(field))
			.tooltip(text(field + ".tip"))
			.searchWords("crafting tree cfhgui_crafting emi recipe " + field);
	}

	private static Binding bool(String field, Function<TreeConfig, Boolean> get, BiConsumer<TreeConfig, Boolean> put) {
		return row(field, OptionType.BOOLEAN)
			.getter(() -> get.apply(TreeConfig.get()))
			.setter(v -> set(c -> put.accept(c, Boolean.TRUE.equals(v))))
			.def(get.apply(DEFAULTS));
	}

	private static Binding number(String field, int min, int max, int step, Function<TreeConfig, Number> get, BiConsumer<TreeConfig, Number> put) {
		return row(field, OptionType.INT).range((double)min, (double)max, (double)step).getter(() -> get.apply(TreeConfig.get())).setter(v -> {
			if (v instanceof Number n) {
				set(c -> put.accept(c, n));
			}
		}).def(get.apply(DEFAULTS));
	}

	private TreeSettings() {
	}
}
