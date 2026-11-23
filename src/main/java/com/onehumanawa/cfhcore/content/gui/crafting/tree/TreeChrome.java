package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.network.chat.Component;

final class TreeChrome {
	private static final Map<CFHGuiIconButton, Integer> SHOWS = new WeakHashMap<>();

	private static boolean changed(CFHGuiIconButton button, int state) {
		Integer was = SHOWS.put(button, state);
		return was == null || was != state;
	}

	static void foldMode(CFHGuiIconButton button, TreeSession session) {
		if (changed(button, session.foldMode ? 1 : 0)) {
			button.toggled(session.foldMode);
			button.tip(
				List.of(
					Component.translatable("cfhgui_crafting.tree.fold_mode"),
					Component.translatable(session.foldMode ? "cfhgui_crafting.tree.fold_mode.on" : "cfhgui_crafting.tree.fold_mode.tip")
						.withStyle(s -> s.withColor(TreePaint.dim() & 16777215))
				)
			);
		}
	}

	static void keep(CFHGuiIconButton button, TreeEntry entry) {
		int state = !entry.writable() ? 2 : (entry.saved ? 1 : 0);
		if (changed(button, state)) {
			button.toggled(entry.saved);
			button.tip(
				List.of(
					Component.translatable(state == 2 ? "cfhgui_crafting.tree.keep.cannot" : (state == 1 ? "cfhgui_crafting.tree.keep.kept" : "cfhgui_crafting.tree.keep")),
					Component.translatable(
							state == 2 ? "cfhgui_crafting.tree.keep.cannot.tip" : (state == 1 ? "cfhgui_crafting.tree.keep.kept.tip" : "cfhgui_crafting.tree.keep.tip")
						)
						.withStyle(s -> s.withColor(TreePaint.dim() & 16777215))
				)
			);
		}
	}

	static void panel(CFHGuiIconButton button, boolean open) {
		if (changed(button, open ? 1 : 0)) {
			button.toggled(open);
			button.tip(
				List.of(
					Component.translatable(open ? "cfhgui_crafting.planner.panel.hide" : "cfhgui_crafting.planner.panel.show"),
					Component.translatable("cfhgui_crafting.planner.panel.tip").withStyle(s -> s.withColor(TreePaint.dim() & 16777215))
				)
			);
		}
	}

	private TreeChrome() {
	}
}
