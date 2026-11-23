package com.onehumanawa.cfhcore.content.gui.crafting.compat;

import com.onehumanawa.cfhcore.content.gui.emi.api.widget.Bounds;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

public final class BetterInventoryCompat {
	private static final String MOD = "betterinventory";
	private static final String SWAP = "dev.fallingcloud.betterinventory.client.InventorySwap";
	private static final int HALF_W = 175;
	private static final int BELOW = 115;
	private static final int SIZE = 18;
	private static final int FRAME = 4;
	private static Boolean present;
	private static boolean resolved;
	private static Method buttonX;
	private static Method buttonY;

	private BetterInventoryCompat() {
	}

	public static void exclusion(Screen screen, Consumer<Bounds> out) {
		if (screen instanceof InventoryScreen && present()) {
			int x = screen.width / 2 - 175;
			int y = screen.height / 2 + 115;
			resolve();
			if (buttonX != null && buttonY != null) {
				try {
					x = (Integer)buttonX.invoke(null, screen);
					y = (Integer)buttonY.invoke(null, screen);
				} catch (RuntimeException | ReflectiveOperationException var5) {
					buttonX = null;
				}
			}

			out.accept(new Bounds(x - 4, y - 4, 26, 26));
		}
	}

	private static boolean present() {
		if (present == null) {
			present = CFHGuiPlatform.get().isModLoaded("betterinventory");
		}

		return present;
	}

	private static void resolve() {
		if (!resolved) {
			resolved = true;

			try {
				Class<?> swap = Class.forName("dev.fallingcloud.betterinventory.client.InventorySwap");
				buttonX = swap.getMethod("buttonX", Screen.class);
				buttonY = swap.getMethod("buttonY", Screen.class);
			} catch (LinkageError | ReflectiveOperationException var1) {
				buttonX = null;
				buttonY = null;
			}
		}
	}
}
