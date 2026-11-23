package com.onehumanawa.cfhcore.content.gui.server.client.hud;

import com.onehumanawa.cfhcore.content.gui.core.client.invrow.InventoryRow.Entry;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.ServerClientConfig;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerClient;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerState;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.server.model.PlayerRecord;
import com.onehumanawa.cfhcore.content.gui.server.model.TeleportRequest;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;

public final class InventoryRow {
	public static void register() {
		com.onehumanawa.cfhcore.content.gui.core.client.invrow.InventoryRow.register(
			new Entry(
				"cfhgui_server:menu",
				Icon.HOME,
				() -> Component.translatable("cfhgui_server.hub.menu"),
				() -> ServerClient.HUB_KEY,
				100,
				() -> ServerClientConfig.get().inventoryButtons
						&& ServerState.available()
						&& ServerClient.hubAvailable()
						&& !ServerState.newerServer()
						&& (ServerState.may(Nodes.HUB_USE) || ServerState.preview()),
				() -> ServerClient.open(null),
				InventoryRow::waiting
			)
		);
		com.onehumanawa.cfhcore.content.gui.core.client.invrow.InventoryRow.register(
			new Entry(
				"cfhgui_server:admin",
				Icon.SHIELD,
				() -> Component.translatable("cfhgui_server.hub.admin"),
				() -> ServerClient.ADMIN_KEY,
				900,
				() -> ServerClientConfig.get().inventoryAdmin && ServerState.available() && ServerClient.adminAvailable() && !ServerState.newerServer(),
				() -> ServerClient.openAdmin(null),
				null
			)
		);
	}

	private static List<Component> waiting() {
		int requests = 0;
		int letters = 0;

		for (TeleportRequest r : ServerState.requests()) {
			if (r.incoming) {
				requests++;
			}
		}

		for (PlayerRecord.Mail m : ServerState.mail()) {
			if (!m.read) {
				letters++;
			}
		}

		letters = Math.max(letters, ServerState.mail().isEmpty() ? ServerState.self().unreadMail : 0);
		List<Component> out = new ArrayList<>();
		if (requests > 0) {
			out.add(Txt.t(requests == 1 ? "row.requests.one" : "row.requests", requests));
		}

		if (letters > 0) {
			out.add(Txt.t(letters == 1 ? "row.mail.one" : "row.mail", letters));
		}

		return out;
	}

	private InventoryRow() {
	}
}
