package com.onehumanawa.cfhcore.content.gui.quests.client;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.core.net.CFHGuiNetwork;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiModal;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.quests.model.BookJson;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Ids;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestState;
import com.onehumanawa.cfhcore.content.gui.quests.model.Reward;
import com.onehumanawa.cfhcore.content.gui.quests.net.QuestsNet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class QuestActions {
	private static int nextRequest = 1;
	private static final Map<Integer, Consumer<QuestsNet.Result>> WAITING = new HashMap<>();

	public static void claim(String questKey, int reward, int choice, @Nullable Consumer<QuestsNet.Result> then) {
		CompoundTag a = new CompoundTag();
		a.putString("quest", questKey);
		a.putInt("reward", reward);
		a.putInt("choice", choice);
		send("claim", a, then);
	}

	public static void claimAll(boolean pick, @Nullable Consumer<QuestsNet.Result> then) {
		CompoundTag a = new CompoundTag();
		if (pick) {
			a.putBoolean("pick", true);
		}

		send("claim_all", a, then);
	}

	public static QuestActions.Waiting waiting() {
		QuestBook book = QuestsState.book();
		if (book == null) {
			return new QuestActions.Waiting(0, 0, 0);
		} else {
			int quests = 0;
			int plain = 0;
			int choices = 0;

			for (Chapter c : book.chapters.values()) {
				if (!c.readOnly) {
					for (Quest q : c.quests) {
						String key = Ids.key(c, q);
						if (QuestsState.progress().claimable(key)) {
							QuestState state = QuestsState.state(key);
							boolean plainHere = false;
							boolean choiceHere = false;

							for (int i = 0; i < q.rewards.size(); i++) {
								Reward r = q.rewards.get(i);
								if (!r.excluded && !state.claimed(i)) {
									if (r.is("choice") && !r.choices.isEmpty()) {
										choices++;
										choiceHere = true;
									} else {
										plainHere = true;
									}
								}
							}

							if (plainHere || choiceHere) {
								quests++;
							}

							if (plainHere) {
								plain++;
							}
						}
					}
				}
			}

			return new QuestActions.Waiting(quests, plain, choices);
		}
	}

	public static void claimAllAsking(BiConsumer<Integer, QuestsNet.Result> then) {
		QuestActions.Waiting w = waiting();
		if (w.choices() == 0) {
			claimAll(false, r -> then.accept(w.quests(), r));
		} else {
			CFHGuiModal ask = new CFHGuiModal(
					Component.translatable("cfhgui_quests.claim_all"),
					Component.translatable(w.choices() == 1 ? "cfhgui_quests.claim_all.pick.one" : "cfhgui_quests.claim_all.pick.many", new Object[]{w.choices()}),
					Icon.QUESTION
				)
				.button(Component.translatable("gui.cancel"), Variant.SECONDARY, null);
			if (w.plain() > 0) {
				ask.width(300).button(Component.translatable("cfhgui_quests.claim_all.pick.skip"), Variant.SECONDARY, () -> claimAll(false, r -> then.accept(w.plain(), r)));
			}

			ask.button(Component.translatable("cfhgui_quests.claim_all.pick.ok"), Variant.PRIMARY, () -> claimAll(true, r -> then.accept(w.quests(), r))).show();
		}
	}

	public static void submit(String questKey, int task, @Nullable Consumer<QuestsNet.Result> then) {
		CompoundTag a = new CompoundTag();
		a.putString("quest", questKey);
		a.putInt("task", task);
		send("submit", a, then);
	}

	public static void sync() {
		send("sync", new CompoundTag(), r -> {
		});
	}

	public static void editChapter(Chapter chapter, @Nullable Consumer<QuestsNet.Result> then) {
		CompoundTag a = new CompoundTag();
		a.putString("id", chapter.id);
		a.putByteArray("chapter", JsonWire.encode(BookJson.GSON, chapter));
		send("edit_chapter", a, then);
	}

	public static void deleteChapter(String chapterId, @Nullable Consumer<QuestsNet.Result> then) {
		CompoundTag a = new CompoundTag();
		a.putString("id", chapterId);
		send("delete_chapter", a, then);
	}

	public static void editHead(QuestBook head, @Nullable Consumer<QuestsNet.Result> then) {
		CompoundTag a = new CompoundTag();
		a.putByteArray("book", JsonWire.encode(BookJson.GSON, head));
		send("edit_head", a, then);
	}

	public static void renameChapter(String chapterId, String to, @Nullable Consumer<QuestsNet.Result> then) {
		CompoundTag a = new CompoundTag();
		a.putString("id", chapterId);
		a.putString("to", to);
		send("rename_chapter", a, then);
	}

	public static void renameQuest(String questKey, String to, @Nullable Consumer<QuestsNet.Result> then) {
		CompoundTag a = new CompoundTag();
		a.putString("quest", questKey);
		a.putString("to", to);
		send("rename_quest", a, then);
	}

	public static void reload(@Nullable Consumer<QuestsNet.Result> then) {
		send("reload", new CompoundTag(), then);
	}

	public static void reset(String target, @Nullable String player, @Nullable Consumer<QuestsNet.Result> then) {
		CompoundTag a = new CompoundTag();
		a.putString("target", target);
		if (player != null) {
			a.putString("player", player);
		}

		send("reset", a, then);
	}

	public static void complete(String questKey, @Nullable String player, @Nullable Consumer<QuestsNet.Result> then) {
		CompoundTag a = new CompoundTag();
		a.putString("quest", questKey);
		if (player != null) {
			a.putString("player", player);
		}

		send("complete", a, then);
	}

	public static void send(String op, CompoundTag args, @Nullable Consumer<QuestsNet.Result> then) {
		int request = nextRequest++;
		if (QuestsState.preview()) {
			deliver(PreviewQuests.play(request, op, args), then);
		} else {
			CFHGuiNetwork net = CFHGuiNetwork.get();
			if (QuestsState.connected() && net.serverHasChannel(QuestsNet.Action.TYPE)) {
				if (then != null) {
					WAITING.put(request, then);
				}

				net.sendToServer(new QuestsNet.Action(request, op, args));
			} else {
				deliver(QuestsNet.Result.fail(request, "disabled"), then);
			}
		}
	}

	public static void onResult(QuestsNet.Result result) {
		deliver(result, WAITING.remove(result.request()));
	}

	private static void deliver(QuestsNet.Result result, @Nullable Consumer<QuestsNet.Result> then) {
		if (then != null) {
			then.accept(result);
		} else {
			if (!result.ok()) {
				CFHGuiToasts.show(Component.translatable("cfhgui_quests.result.failed"), describe(result), Icon.WARNING);
			}
		}
	}

	public static Component describe(QuestsNet.Result result) {
		return Component.translatable("cfhgui_quests.result." + result.code(), result.args().toArray());
	}

	static void reset() {
		WAITING.clear();
	}

	private QuestActions() {
	}

	public static record Waiting(int quests, int plain, int choices) {
	}
}
