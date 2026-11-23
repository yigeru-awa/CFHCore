package com.onehumanawa.cfhcore.content.gui.quests.client.ui;

import com.onehumanawa.cfhcore.content.gui.config.CFHGuiConfigApi;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.screen.ScreenIds;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Layout;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsClient;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.BookStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public final class QuestsUi {
	public static void init() {
		BookStats.revision();
		ScreenIds.register(QuestBookScreen.class, "cfhgui_quests:book", "Quest book");
		MenuSlots.provide("cfhgui_quests:book", Layout.CUSTOM, parent -> new QuestBookScreen(parent instanceof QuestBookScreen open ? open.parentScreen() : parent));
		if (CFHGuiPlatform.get().isModLoaded("cfhgui_config")) {
			QuestsUi.ConfigSide.init();
		}

		EmergencyItems.init();
		StressBook.init();
		BookShots.init();
	}

	public static void show(@Nullable String chapter, @Nullable String quest) {
		if (Minecraft.getInstance().screen instanceof QuestBookScreen open) {
			open.show(chapter, quest);
		} else {
			QuestsClient.open(chapter, quest);
		}
	}

	public static boolean hasSettings() {
		return CFHGuiPlatform.get().isModLoaded("cfhgui_config");
	}

	public static void openSettings(@Nullable Screen parent) {
		Screen settings = settingsScreen(parent);
		if (settings != null) {
			Minecraft.getInstance().setScreen(settings);
		}
	}

	@Nullable
	public static Screen settingsScreen(@Nullable Screen parent) {
		return hasSettings() ? QuestsUi.ConfigSide.screen(parent) : null;
	}

	private QuestsUi() {
	}

	private static final class ConfigSide {
		static void init() {
			QuestsSettingsTab.register();
		}

		static Screen screen(@Nullable Screen parent) {
			return CFHGuiConfigApi.hub(parent, "gameplay/quests");
		}
	}
}
