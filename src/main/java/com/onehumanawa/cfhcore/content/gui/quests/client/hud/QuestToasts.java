package com.onehumanawa.cfhcore.content.gui.quests.client.hud;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.quests.QuestsConfig;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.client.ui.QuestsUi;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestIcons;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class QuestToasts {
	private static final int SEPARATE = 3;
	private static final List<QuestsState.Happening> DONE = new ArrayList<>();

	static void clear() {
		synchronized (DONE) {
			DONE.clear();
		}
	}

	static void questDone(QuestsState.Happening h) {
		BookIndex.Entry e = QuestsState.entry(h.subject());
		if (e != null) {
			Quest q = e.quest();
			if (QuestsConfig.get().toasts && !q.silent) {
				if (!q.isAdvancement() || !mine(h.who()) || !advancement(e)) {
					synchronized (DONE) {
						DONE.add(h);
					}
				}
			}
		}
	}

	static void flush() {
		List<QuestsState.Happening> done;
		synchronized (DONE) {
			if (DONE.isEmpty()) {
				return;
			}

			done = new ArrayList<>(DONE);
			DONE.clear();
		}

		if (done.size() <= 3) {
			for (QuestsState.Happening h : done) {
				say(h);
			}
		} else {
			String who = done.get(0).who();
			boolean mine = true;
			boolean one = true;

			for (QuestsState.Happening h : done) {
				mine &= mine(h.who());
				one &= Objects.equals(who, h.who());
			}

			Component title = !mine && one
				? Component.translatable("cfhgui_quests.toast.many.by", new Object[]{who, done.size()})
				: Component.translatable("cfhgui_quests.toast.many", new Object[]{done.size()});
			BookIndex.Entry a = QuestsState.entry(done.get(0).subject());
			BookIndex.Entry b = QuestsState.entry(done.get(1).subject());
			Component body = a != null && b != null
				? Component.translatable("cfhgui_quests.toast.many.body", new Object[]{QuestText.title(a), QuestText.title(b), done.size() - 2})
				: null;
			String chapter = a == null ? null : a.chapter().id;
			CFHGuiToasts.show(title, body, Icon.QUEST, () -> QuestsUi.show(chapter, null), 6500);
			if (mine) {
				chime(false);
			}
		}
	}

	private static void say(QuestsState.Happening h) {
		BookIndex.Entry e = QuestsState.entry(h.subject());
		if (e != null) {
			Quest q = e.quest();
			boolean mine = mine(h.who());
			Component title = mine
				? Component.translatable(
					q.frame == Quest.Frame.CHALLENGE ? "cfhgui_quests.toast.challenge" : (q.frame == Quest.Frame.GOAL ? "cfhgui_quests.toast.goal" : "cfhgui_quests.toast.quest")
				)
				: Component.translatable("cfhgui_quests.toast.quest.by", new Object[]{h.who()});
			boolean waiting = !q.rewards.isEmpty() && !q.autoClaim && !q.isAdvancement();
			Component body = (Component)(waiting ? Component.translatable("cfhgui_quests.toast.quest.rewards", new Object[]{QuestText.title(e)}) : QuestText.title(e));
			String key = e.key();
			CFHGuiToasts.show(title, body, q.frame == Quest.Frame.TASK ? Icon.QUEST : Icon.TROPHY, () -> QuestsUi.show(null, key), waiting ? 6500 : 5000);
			if (mine && q.frame != Quest.Frame.TASK) {
				chime(q.frame == Quest.Frame.CHALLENGE);
			}
		}
	}

	static void chapterDone(QuestsState.Happening h) {
		QuestBook book = QuestsState.book();
		Chapter c = book == null ? null : book.chapter(h.subject());
		if (c != null) {
			QuestsConfig config = QuestsConfig.get();
			boolean mine = mine(h.who());
			if (config.chapterBanner) {
				ChapterBanner.show(c, mine ? "" : h.who());
				if (config.sounds) {
					chime(true);
				}
			} else if (config.toasts) {
				CFHGuiToasts.show(Component.translatable("cfhgui_quests.toast.chapter"), QuestText.title(c), Icon.TROPHY, () -> QuestsUi.show(c.id, null));
			}
		}
	}

	static void unlocked(QuestsState.Happening h) {
		if (QuestsConfig.get().toasts && h.subject().indexOf(47) < 0) {
			QuestBook book = QuestsState.book();
			Chapter c = book == null ? null : book.chapter(h.subject());
			if (c != null && !c.isSubtree()) {
				CFHGuiToasts.show(Component.translatable("cfhgui_quests.toast.unlocked"), QuestText.title(c), Icon.UNLOCK, () -> QuestsUi.show(c.id, null));
			}
		}
	}

	static void notice(QuestsState.Note note) {
		if (!note.title().isBlank() || !note.text().isBlank()) {
			Icon icon = Icon.QUEST;
			if (note.icon().startsWith("icon:")) {
				try {
					icon = Icon.valueOf(note.icon().substring(5).trim().toUpperCase(Locale.ROOT));
				} catch (IllegalArgumentException var4) {
					icon = Icon.QUEST;
				}
			}

			boolean both = !note.title().isBlank() && !note.text().isBlank();
			Icon shown = icon;
			Minecraft.getInstance()
				.execute(
					() -> CFHGuiToasts.show(
							QuestText.text(both ? note.title() : (note.title().isBlank() ? note.text() : note.title())),
							both ? QuestText.text(note.text()) : null,
							shown,
							() -> QuestsUi.show(null, null),
							6500
						)
				);
		}
	}

	private static boolean mine(String who) {
		return who == null || who.isBlank() || who.equals(Minecraft.getInstance().getUser().getName());
	}

	private static boolean advancement(BookIndex.Entry e) {
		try {
			Quest q = e.quest();
			ItemStack icon = QuestIcons.of(e).stack();
			if (icon.isEmpty()) {
				icon = new ItemStack(Items.WRITABLE_BOOK);
			}
			AdvancementType type = switch (q.frame) {
				case GOAL -> AdvancementType.GOAL;
				case CHALLENGE -> AdvancementType.CHALLENGE;
				default -> AdvancementType.TASK;
			};
			DisplayInfo display = new DisplayInfo(icon, QuestText.title(e), QuestText.text(q.subtitle), Optional.empty(), type, true, false, false);
			AdvancementHolder holder = new AdvancementHolder(
				CFHGuiQuests.id("quest/" + e.key()),
				new Advancement(Optional.empty(), Optional.of(display), AdvancementRewards.EMPTY, Map.of(), AdvancementRequirements.EMPTY, false)
			);
			Minecraft mc = Minecraft.getInstance();
			mc.execute(() -> mc.getToasts().addToast(new AdvancementToast(holder)));
			return true;
		} catch (Exception var7) {
			CFHGuiQuests.LOGGER.debug("[CFHGui Quests] no advancement toast for {}: {}", e.key(), var7.toString());
			return false;
		}
	}

	private static void chime(boolean grand) {
		if (QuestsConfig.get().sounds && Theme.current().uiSounds()) {
			Minecraft mc = Minecraft.getInstance();
			mc.getSoundManager()
				.play(
					grand
						? SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 0.6F)
						: SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.3F, 0.35F)
				);
		}
	}

	private QuestToasts() {
	}
}
