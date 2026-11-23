package com.onehumanawa.cfhcore.content.gui.shops.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.client.invrow.InventoryRow;
import com.onehumanawa.cfhcore.content.gui.core.client.invrow.InventoryRow.Entry;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiKeys;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.module.CFHGuiModule.HubEntry;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.screen.ScreenIds;
import com.onehumanawa.cfhcore.content.gui.shops.CFHGuiShops;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.ShopsScreen;
import com.onehumanawa.cfhcore.content.gui.shops.net.ShopsNet;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class ShopsClient {
	public static final String SCREEN = "cfhgui_shops:shops";
	public static final KeyMapping OPEN_KEY = new KeyMapping("key.cfhgui_shops.open", Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.cfh");
	private static boolean initialised;

	public static synchronized void init() {
		if (!initialised) {
			initialised = true;
			ShopsClientConfig.file();
			CFHGuiKeys.register(OPEN_KEY);
			InventoryRow.register(
				new Entry(
					"cfhgui_shops:shops",
					Icon.TAG,
					() -> Component.translatable("cfhgui_shops.ui.title"),
					() -> OPEN_KEY,
					150,
					() -> ShopsClientConfig.get().inventoryButton && ShopsState.available() && !ShopsState.newerServer() && ShopsState.may("use"),
					() -> open(null, null),
					null
				)
			);
			CFHGuiEvents.CLIENT_TICK_END.register((Runnable)() -> {
				while (OPEN_KEY.consumeClick()) {
					if (Minecraft.getInstance().screen == null) {
						open(null, null);
					}
				}
			});
			CFHGuiEvents.CLIENT_LEFT_SERVER.register((Runnable)() -> {
				ShopsActions.clear();
				ShopsState.clear();
			});
			ScreenIds.register(ShopsScreen.class, "cfhgui_shops:shops", "Shops");
			CoreActions.SCREEN_FACTORIES.put("cfhgui_shops:shops", p -> new ShopsScreen(p, null, null));

			for (String page : ShopsScreen.PAGES) {
				CoreActions.SCREEN_FACTORIES.put("cfhgui_shops:shops/" + page, p -> new ShopsScreen(p, page, null));
			}

			if (CFHGuiPlatform.get().isModLoaded("cfhgui_server")) {
				ServerMenu.init();
			}

			CFHGuiShops.LOGGER.info("[CFHGui Shops] client ready");
		}
	}

	public static List<HubEntry> hubEntries() {
		return List.of(new HubEntry(Component.translatable("cfhgui_shops.name"), Icon.TAG, () -> open(null, null)));
	}

	public static void open(@Nullable String page, @Nullable String shop) {
		Minecraft mc = Minecraft.getInstance();
		Screen parent = mc.screen instanceof ShopsScreen s ? s.parentScreen() : mc.screen;
		mc.setScreen(new ShopsScreen(parent, page, shop));
	}

	public static void onOpen(ShopsNet.Open p) {
		String var1 = p.what();
		switch (var1) {
			case "shop":
				open("browse", p.arg());
				break;
			case "admin":
				if (ShopsState.may("admin") && CFHGuiPlatform.get().isModLoaded("cfhgui_server") && ServerMenu.openAdmin()) {
					return;
				}

				open("admin", null);
				break;
			case "mine":
				open("mine", null);
				break;
			case "wallet":
				open("wallet", null);
				break;
			default:
				open("browse", null);
		}
	}

	private ShopsClient() {
	}
}
