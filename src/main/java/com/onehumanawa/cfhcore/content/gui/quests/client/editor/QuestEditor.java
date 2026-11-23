package com.onehumanawa.cfhcore.content.gui.quests.client.editor;

import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsClient;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.ui.BookExtensions;
import com.onehumanawa.cfhcore.content.gui.quests.client.ui.QuestBookScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public final class QuestEditor {
	private static boolean initialised;

	public static synchronized void init() {
		if (!initialised) {
			initialised = true;
			BookExtensions.register(new EditorExtension());
			CFHGuiEvents.CLIENT_TICK_END.register(EditSession::tickRetries);
			EditorShots.init();
		}
	}

	public static boolean available() {
		return QuestsState.canEdit();
	}

	public static void open(@Nullable Screen parent, @Nullable String chapterId) {
		if (available()) {
			EditSession.want(true);
			QuestsClient.target(chapterId, null);
			QuestBookScreen screen = new QuestBookScreen(parent);
			EditSession.of(screen).beforeFirstBuild();
			Minecraft.getInstance().setScreen(screen);
		}
	}

	private QuestEditor() {
	}
}
