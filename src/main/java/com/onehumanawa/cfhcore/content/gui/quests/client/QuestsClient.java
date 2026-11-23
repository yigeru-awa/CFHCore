package com.onehumanawa.cfhcore.content.gui.quests.client;

import com.mojang.blaze3d.platform.InputConstants.Type;
import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiKeys;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.action.ActionType;
import com.onehumanawa.cfhcore.content.gui.core.layout.action.ActionType.Arg;
import com.onehumanawa.cfhcore.content.gui.core.module.CFHGuiModule.HubEntry;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.quests.QuestsConfig;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import com.onehumanawa.cfhcore.content.gui.quests.client.dev.DevTools;
import com.onehumanawa.cfhcore.content.gui.quests.client.editor.QuestEditor;
import com.onehumanawa.cfhcore.content.gui.quests.client.hud.QuestsHud;
import com.onehumanawa.cfhcore.content.gui.quests.client.overhaul.OverhaulQuests;
import com.onehumanawa.cfhcore.content.gui.quests.client.ui.QuestsUi;
import com.onehumanawa.cfhcore.content.gui.quests.client.vanilla.VanillaQuests;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestIcons;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Ids;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class QuestsClient {
	public static final KeyMapping BOOK_KEY = new KeyMapping("key.cfhgui_quests.book", Type.KEYSYM, 74, "key.categories.cfh");
	private static boolean initialised;
	@Nullable
	private static QuestsClient.Target pending;

	public static synchronized void init() {
		if (!initialised) {
			initialised = true;
			QuestsConfig.file();
			CFHGuiKeys.register(BOOK_KEY);
			CoreActions.SCREEN_FACTORIES.put("cfhgui_quests:book", p -> closed(true) ? p : MenuSlots.create("cfhgui_quests:book", p));
			CFHGuiEvents.CLIENT_TICK_END.register(QuestsClient::tick);
			CFHGuiEvents.CLIENT_JOINED_SERVER.register(QuestIcons::clear);
			CFHGuiEvents.CLIENT_LEFT_SERVER.register((Runnable)() -> {
				QuestActions.reset();
				QuestIcons.clear();
				QuestsState.disconnected();
			});
			QuestsUi.init();
			VanillaQuests.init();
			OverhaulQuests.init();
			DevTools.init();
			QuestEditor.init();
			QuestsHud.init();
			QuestsState.ensurePreview();
			harnessScreens();
			CFHGuiQuests.LOGGER.info("[CFHGui Quests] client ready");
		}
	}

	private static void harnessScreens() {
		QuestBook book = QuestsState.book();
		if (book != null && QuestsState.preview()) {
			for (Chapter c : book.chapters.values()) {
				CoreActions.SCREEN_FACTORIES.put("cfhgui_quests:book/" + c.id, p -> {
					target(c.id, null);
					return MenuSlots.create("cfhgui_quests:book", p);
				});

				for (Quest q : c.quests) {
					String key = Ids.key(c, q);
					CoreActions.SCREEN_FACTORIES.put("cfhgui_quests:quest/" + key, p -> {
						target(c.id, key);
						return MenuSlots.create("cfhgui_quests:book", p);
					});
				}
			}
		}
	}

	private static void tick() {
		while (BOOK_KEY.consumeClick()) {
			if (Minecraft.getInstance().screen == null) {
				open(null, null);
			}
		}
	}

	public static void open(@Nullable String chapter, @Nullable String quest) {
		if (!closed(true)) {
			pending = new QuestsClient.Target(chapter != null ? chapter : (quest != null ? Ids.chapterOf(quest) : null), quest);
			MenuSlots.open("cfhgui_quests:book", Minecraft.getInstance().screen);
		}
	}

	public static boolean closed(boolean say) {
		QuestBook book = QuestsState.book();
		if (book != null && book.closed && !QuestsState.canEdit()) {
			if (say) {
				CFHGuiToasts.show(Component.translatable("cfhgui_quests.closed"), Component.translatable("cfhgui_quests.closed.body"), Icon.LOCK);
			}

			return true;
		} else {
			return false;
		}
	}

	@Nullable
	public static QuestsClient.Target takeTarget() {
		QuestsClient.Target t = pending;
		pending = null;
		return t;
	}

	public static void target(@Nullable String chapter, @Nullable String quest) {
		pending = new QuestsClient.Target(chapter, quest);
	}

	public static List<HubEntry> hubEntries() {
		return List.of(new HubEntry(Component.translatable("cfhgui_quests.hub.book"), Icon.QUEST, () -> open(null, null)));
	}

	public static List<ActionType> actions() {
		return List.of(
			new ActionType(
				"cfhgui_quests:open",
				Component.translatable("cfhgui_quests.action.open"),
				List.of(Arg.text("chapter", Component.translatable("cfhgui_quests.action.arg.chapter"), "")),
				a -> {
					String c = a.getOrDefault("chapter", "");
					open(c.isBlank() ? null : c, null);
				}
			)
		);
	}

	private QuestsClient() {
	}

	public static record Target(@Nullable String chapter, @Nullable String quest) {
	}
}
