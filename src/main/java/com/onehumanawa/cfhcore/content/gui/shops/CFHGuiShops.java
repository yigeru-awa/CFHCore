package com.onehumanawa.cfhcore.content.gui.shops;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.module.CFHGuiModule;
import com.onehumanawa.cfhcore.content.gui.core.module.CFHGuiModule.HubEntry;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.shops.client.ShopsClient;
import com.onehumanawa.cfhcore.content.gui.shops.net.ShopsNet;
import com.onehumanawa.cfhcore.content.gui.shops.service.ShopsRuntime;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CFHGuiShops implements CFHGuiModule {
	public static final String MOD_ID = "cfhgui_shops";
	public static final Logger LOGGER = LoggerFactory.getLogger("CFHGui Shops");
	public static final CFHGuiShops MODULE = new CFHGuiShops();

	private CFHGuiShops() {
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath("cfhgui_shops", path);
	}

	public String id() {
		return "cfhgui_shops";
	}

	public Component displayName() {
		return Component.translatable("cfhgui_shops.name");
	}

	public Icon icon() {
		return Icon.TAG;
	}

	public void init() {
		CFHGui.init();
		ShopNodes.init();
		ShopsNet.register();
		ShopsRuntime.init();
		LOGGER.info("[CFHGui Shops] init");
	}

	public void initClient() {
		CFHGuiShops.ClientSide.init();
	}

	public List<HubEntry> hubEntries() {
		return CFHGuiPlatform.get().isClient() ? CFHGuiShops.ClientSide.hubEntries() : List.of();
	}

	private static final class ClientSide {
		static void init() {
			ShopsClient.init();
		}

		static List<HubEntry> hubEntries() {
			return ShopsClient.hubEntries();
		}
	}
}
