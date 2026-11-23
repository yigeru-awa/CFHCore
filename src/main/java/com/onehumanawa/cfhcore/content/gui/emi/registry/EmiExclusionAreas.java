package com.onehumanawa.cfhcore.content.gui.emi.registry;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.onehumanawa.cfhcore.content.gui.emi.api.EmiExclusionArea;
import com.onehumanawa.cfhcore.content.gui.emi.api.widget.Bounds;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiLog;
import com.onehumanawa.cfhcore.content.gui.emi.screen.EmiScreenBase;
import com.onehumanawa.cfhcore.content.gui.emi.screen.EmiScreenManager;
import com.onehumanawa.cfhcore.content.gui.core.client.invrow.InventoryRow;
import com.onehumanawa.cfhcore.content.gui.core.client.quick.QuickActions;
import com.onehumanawa.cfhcore.content.gui.crafting.look.LookButtons;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.gui.screens.Screen;

public class EmiExclusionAreas {
	public static Map<Class<?>, List<EmiExclusionArea<?>>> fromClass = Maps.newHashMap();
	public static List<EmiExclusionArea<?>> generic = Lists.newArrayList();

	public static void clear() {
		fromClass.clear();
		generic.clear();
	}

	public static List<Bounds> getExclusion(EmiScreenBase base) {
		Screen screen = base.screen();
		List<Bounds> list = Lists.newArrayList();
		list.add(base.bounds());
		list.add(LookButtons.footBounds(EmiScreenManager.tree, EmiScreenManager.emi, screen.height, base.bounds()));
		if (EmiScreenManager.search.isVisible()) {
			list.add(
				new Bounds(EmiScreenManager.search.x - 1, EmiScreenManager.search.y - 1, EmiScreenManager.search.getWidth() + 2, EmiScreenManager.search.getHeight() + 2)
			);
		}

		int[] row = InventoryRow.bounds(screen);
		if (row != null) {
			list.add(new Bounds(row[0] - 2, row[1] - 2, row[2] + 4, row[3] + 4));
		}

		int[] quick = QuickActions.bounds(screen);
		if (quick != null) {
			list.add(new Bounds(quick[0] - 2, quick[1] - 2, quick[2] + 4, quick[3] + 4));
		}

		try {
			if (fromClass.containsKey(screen.getClass())) {
				for (EmiExclusionArea exclusion : fromClass.get(screen.getClass())) {
					exclusion.addExclusionArea(screen, addBounds(list));
				}
			}

			for (EmiExclusionArea exclusion : generic) {
				exclusion.addExclusionArea(screen, addBounds(list));
			}
		} catch (Exception var7) {
			EmiLog.error("Exception thrown when adding exclusion areas", var7);
		}

		return list;
	}

	public static List<Bounds> getOthers(EmiScreenBase base) {
		Screen screen = base.screen();
		List<Bounds> list = Lists.newArrayList();

		try {
			if (fromClass.containsKey(screen.getClass())) {
				for (EmiExclusionArea exclusion : fromClass.get(screen.getClass())) {
					exclusion.addExclusionArea(screen, addBounds(list));
				}
			}

			for (EmiExclusionArea exclusion : generic) {
				exclusion.addExclusionArea(screen, addBounds(list));
			}
		} catch (Exception var5) {
			EmiLog.error("Exception thrown when adding exclusion areas", var5);
		}

		return list;
	}

	private static Consumer<Bounds> addBounds(List<Bounds> list) {
		return rect -> {
			if (!rect.empty() && rect.right() > rect.x() && rect.bottom() > rect.y()) {
				list.add(rect);
			}
		};
	}
}
