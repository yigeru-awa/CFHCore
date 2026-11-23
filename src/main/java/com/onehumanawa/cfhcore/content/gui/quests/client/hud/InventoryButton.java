package com.onehumanawa.cfhcore.content.gui.quests.client.hud;

import com.onehumanawa.cfhcore.content.gui.core.client.invrow.InventoryRow;
import com.onehumanawa.cfhcore.content.gui.core.client.invrow.InventoryRow.Entry;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.quests.QuestsConfig;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsClient;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.BookStats;
import java.util.List;
import net.minecraft.network.chat.Component;

final class InventoryButton {
	static void register() {
		InventoryRow.register(
			new Entry(
				"cfhgui_quests:book",
				Icon.QUEST,
				() -> Component.translatable("cfhgui_quests.hub.book"),
				() -> QuestsClient.BOOK_KEY,
				200,
				() -> QuestsConfig.get().inventoryButton && QuestsState.available(),
				() -> QuestsClient.open(null, null),
				InventoryButton::waiting
			)
		);
	}

	private static List<Component> waiting() {
		int waiting = BookStats.book().claimable();
		return waiting <= 0
			? List.of()
			: List.of(Component.translatable(waiting == 1 ? "cfhgui_quests.rail.waiting.one" : "cfhgui_quests.rail.waiting", new Object[]{waiting}));
	}

	private InventoryButton() {
	}
}
