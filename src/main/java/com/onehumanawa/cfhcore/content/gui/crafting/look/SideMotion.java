package com.onehumanawa.cfhcore.content.gui.crafting.look;

import com.onehumanawa.cfhcore.content.gui.emi.api.widget.Bounds;
import com.onehumanawa.cfhcore.content.gui.emi.config.SidebarSide;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiDrawContext;
import com.onehumanawa.cfhcore.content.gui.emi.screen.EmiScreenManager;
import com.onehumanawa.cfhcore.content.gui.core.screen.ContainerMotion;
import com.onehumanawa.cfhcore.content.gui.crafting.tree.TreeDockSeam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class SideMotion {
	private static final int BEYOND = 4;

	private SideMotion() {
	}

	public static void begin(EmiScreenManager.SidebarPanel panel, EmiDrawContext context) {
		if (ContainerMotion.active()) {
			if (panel.side == SidebarSide.LEFT || panel.side == SidebarSide.RIGHT) {
				slide(context, panel.getBounds(), panel.side == SidebarSide.LEFT);
			}
		}
	}

	public static void beginDock(EmiDrawContext context) {
		if (ContainerMotion.active()) {
			slide(context, TreeDockSeam.bounds(), true);
		}
	}

	public static void end(EmiDrawContext context) {
		ContainerMotion.endSide(context.raw());
	}

	private static void slide(EmiDrawContext context, Bounds bounds, boolean fromLeft) {
		Screen screen = Minecraft.getInstance().screen;
		if (screen != null && !bounds.empty()) {
			int travel = fromLeft ? bounds.right() : screen.width - bounds.left();
			ContainerMotion.beginSide(context.raw(), fromLeft ? -1 : 1, 0, (float)(travel + 4));
		}
	}
}
