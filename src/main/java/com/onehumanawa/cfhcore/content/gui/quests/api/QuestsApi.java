package com.onehumanawa.cfhcore.content.gui.quests.api;

import com.onehumanawa.cfhcore.content.gui.core.event.Event;
import com.onehumanawa.cfhcore.content.gui.quests.logic.Rules;
import com.onehumanawa.cfhcore.content.gui.quests.server.QuestsServer;
import net.minecraft.server.level.ServerPlayer;

public final class QuestsApi {
	public static final Event<QuestsApi.QuestListener> QUEST_DONE = new Event("QUESTS_QUEST_DONE");
	public static final Event<QuestsApi.ChapterListener> CHAPTER_DONE = new Event("QUESTS_CHAPTER_DONE");
	public static final Event<QuestsApi.RewardListener> REWARD = new Event("QUESTS_REWARD");

	public static long count(ServerPlayer player, String name, long amount) {
		return QuestsServer.apiCount(player, name, amount);
	}

	public static boolean running() {
		return QuestsServer.book() != null;
	}

	public static int progress(ServerPlayer player, String task, long amount) {
		return QuestsServer.apiProgress(player, task, amount);
	}

	public static boolean progress(ServerPlayer player, String quest, String task, long amount) {
		return QuestsServer.apiProgress(player, quest, task, amount);
	}

	public static Rules.Status status(ServerPlayer player, String quest) {
		return QuestsServer.apiStatus(player, quest);
	}

	public static boolean done(ServerPlayer player, String quest) {
		return status(player, quest).done();
	}

	private QuestsApi() {
	}

	@FunctionalInterface
	public interface ChapterListener {
		void done(ServerPlayer var1, String var2);
	}

	@FunctionalInterface
	public interface QuestListener {
		void done(ServerPlayer var1, String var2);
	}

	@FunctionalInterface
	public interface RewardListener {
		void reward(ServerPlayer var1, String var2, String var3);
	}
}
