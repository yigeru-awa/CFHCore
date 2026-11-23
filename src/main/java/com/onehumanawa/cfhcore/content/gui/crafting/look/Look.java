package com.onehumanawa.cfhcore.content.gui.crafting.look;

import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.crafting.config.CraftingSettingsTab;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public final class Look {
	private static final String UPSTREAM_SCREENS = "com.onehumanawa.cfhcore.content.gui.emi.screen.";

	public static void init() {
		Reskin.own("com.onehumanawa.cfhcore.content.gui.emi.screen.");
		if (hasCFHGuiConfig()) {
			Look.ConfigSide.register();
		}

		LookShots.init();
	}

	private static boolean hasCFHGuiConfig() {
		return CFHGuiPlatform.get().isModLoaded("cfhgui_config");
	}

	public static boolean openSettings(@Nullable Screen parent) {
		return openSettings(parent, null);
	}

	public static boolean openSettings(@Nullable Screen parent, @Nullable String key) {
		if (!hasCFHGuiConfig()) {
			return false;
		} else {
			Look.ConfigSide.open(parent, key);
			return true;
		}
	}

	public static boolean cfh() {
		return !Theme.current().isVanilla();
	}

	public static boolean plate(GuiGraphics g, int x, int y, int w, int h) {
		if (!cfh()) {
			return false;
		} else {
			LookDraw.panelPlate(g, x, y, w, h, 0, g.guiWidth(), g.guiHeight(), 1.0F);
			return true;
		}
	}

	public static void beginTooltip() {
		Reskin.beginOwnTooltip();
	}

	public static void endTooltip() {
		Reskin.endOwnTooltip();
	}

	static Theme theme() {
		return Theme.current();
	}

	static Palette palette() {
		return Theme.current().palette();
	}

	public static int color(int upstream) {
		if (!cfh()) {
			return upstream;
		} else {
			Palette p = palette();

			return switch (upstream) {
				case -16711681 -> p.accent();
				case -65536 -> p.danger();
				case 855703296 -> Colors.withAlpha(p.success(), 64);
				case 872349696 -> Colors.withAlpha(p.danger(), 72);
				case 1151992063 -> Colors.withAlpha(p.accent(), 80);
				case 1157562368 -> Colors.withAlpha(p.danger(), 88);
				case 1996488704 -> Colors.withAlpha(p.bg(), 176);
				case 1996536831 -> Colors.withAlpha(p.accent(), 102);
				default -> upstream;
			};
		}
	}

	private Look() {
	}

	private static final class ConfigSide {
		static void register() {
			CraftingSettingsTab.register();
		}

		static void open(@Nullable Screen parent, @Nullable String key) {
			CraftingSettingsTab.open(parent, key);
		}
	}
}
