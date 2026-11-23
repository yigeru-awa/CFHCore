package com.onehumanawa.cfhcore.content.gui.crafting;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.module.CFHGuiModule;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.crafting.client.CraftingShots;
import com.onehumanawa.cfhcore.content.gui.crafting.look.Look;
import com.onehumanawa.cfhcore.content.gui.crafting.team.TeamPins;
import com.onehumanawa.cfhcore.content.gui.crafting.tree.Tree;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CFHGuiCrafting implements CFHGuiModule {
	public static final String MOD_ID = "cfhgui_crafting";
	public static final Logger LOGGER = LoggerFactory.getLogger("CFHGui Crafting");
	public static final CFHGuiCrafting MODULE = new CFHGuiCrafting();
	public static final String EMI_ID = "emi";

	private CFHGuiCrafting() {
	}

	public String id() {
		return "cfhgui_crafting";
	}

	public Component displayName() {
		return Component.translatable("cfhgui_crafting.name");
	}

	public Icon icon() {
		return Icon.GRID;
	}

	public void init() {
		CFHGui.init();
		if (CFHGuiPlatform.get().isModLoaded("emi")) {
			LOGGER.info("[CFHGui Crafting] init (the loader knows it as {} too)", "emi");
		} else {
			LOGGER.error("[CFHGui Crafting] init: the loader does not know this module as {}; mods that plug into EMI will not find it", "emi");
		}

		TeamPins.init();
	}

	public void initClient() {
		CFHGuiCrafting.ClientSide.init();
	}

	private static final class ClientSide {
		static void init() {
			CraftingShots.init();
			Look.init();
			Tree.init();
		}
	}
}
