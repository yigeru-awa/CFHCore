package com.onehumanawa.cfhcore.content.gui.quests.client.hud;

import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents.ScreenRender;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.BookStats;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class QuestsHud {
	private static boolean initialised;
	private static boolean quiet;

	public static synchronized void init() {
		if (!initialised) {
			initialised = true;
			BookStats.revision();
			QuestsState.HAPPENED.register(QuestsHud::happened);
			QuestsState.NOTICE.register((Consumer<QuestsState.Note>)note -> {
				if (!quiet) {
					QuestToasts.notice(note);
				}
			});
			QuestsState.PROGRESS_CHANGED.register(QuestTracker::progress);
			CFHGuiEvents.CLIENT_TICK_END.register(QuestsHud::tick);
			CFHGuiEvents.HUD_RENDER.register(QuestsHud::hud);
			CFHGuiEvents.SCREEN_RENDER_POST.register((ScreenRender)(screen, g, mouseX, mouseY, partial) -> ChapterBanner.render(g, screen.width));
			InventoryButton.register();
			CFHGuiEvents.CLIENT_LEFT_SERVER.register((Runnable)() -> {
				ChapterBanner.clear();
				QuestTracker.clear();
				QuestToasts.clear();
			});
		}
	}

	public static void quiet(boolean on) {
		quiet = on;
	}

	private static void happened(QuestsState.Happening h) {
		if (!quiet) {
			try {
				String e = h.kind();
				switch (e) {
					case "quest":
						QuestToasts.questDone(h);
						break;
					case "chapter":
						QuestToasts.chapterDone(h);
						break;
					case "unlocked":
						QuestToasts.unlocked(h);
				}
			} catch (Exception var3) {
				CFHGuiQuests.LOGGER.warn("[CFHGui Quests] could not show what happened ({} {}): {}", new Object[]{h.kind(), h.subject(), var3.toString()});
			}
		}
	}

	private static void tick() {
		try {
			QuestToasts.flush();
		} catch (Exception var1) {
			QuestToasts.clear();
			CFHGuiQuests.LOGGER.warn("[CFHGui Quests] could not say what was done: {}", var1.toString());
		}
	}

	private static void hud(GuiGraphics g, float partialTick) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && !mc.options.hideGui) {
			QuestTracker.render(g);
			if (mc.screen == null) {
				ChapterBanner.render(g, g.guiWidth());
			}
		}
	}

	private QuestsHud() {
	}
}
