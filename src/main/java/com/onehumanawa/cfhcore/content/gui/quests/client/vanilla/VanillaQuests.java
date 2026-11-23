package com.onehumanawa.cfhcore.content.gui.quests.client.vanilla;

import com.onehumanawa.cfhcore.content.gui.core.screen.ScreenIds;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Layout;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsClient;
import com.onehumanawa.cfhcore.content.gui.quests.client.ui.BookExtensions;
import com.onehumanawa.cfhcore.content.gui.quests.client.ui.QuestBookScreen;
import com.onehumanawa.cfhcore.content.gui.quests.mixin.client.AdvancementsScreenAccessor;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public final class VanillaQuests {
	public static void init() {
		AdvancementChapters.init();
		ScreenIds.register(VanillaBookScreen.class, "cfhgui_quests:book", "Quest book");
		MenuSlots.provide(
			"cfhgui_quests:book", Layout.VANILLA, parent -> new VanillaBookScreen(parent instanceof VanillaBookScreen open ? open.parentScreen() : parent)
		);
		MenuSlots.provide("minecraft:advancements", Layout.CUSTOM, VanillaQuests::bookForAdvancements);
		BookExtensions.register(new BookExtensions.Extension() {
			@Override
			public void chapterShown(QuestBookScreen screen, @Nullable Chapter chapter) {
				AdvancementChapters.looking(chapter);
			}
		});
		VanillaShots.init();
	}

	@Nullable
	private static Screen bookForAdvancements(Screen advancements) {
		String chapter = AdvancementChapters.startChapter();
		if (chapter != null && !QuestsClient.closed(false)) {
			Screen parent = advancements instanceof AdvancementsScreenAccessor a ? a.slateQuests$lastScreen() : null;
			QuestsClient.target(chapter, null);
			Screen book = MenuSlots.create("cfhgui_quests:book", parent);
			if (book == null) {
				QuestsClient.takeTarget();
			}

			return book;
		} else {
			return null;
		}
	}

	private VanillaQuests() {
	}
}
