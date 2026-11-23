package com.onehumanawa.cfhcore.content.gui.quests.server;

import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.quests.QuestsServerConfig;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.model.BookJson;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class BookDoor {
	public static boolean running() {
		return QuestsServer.engine() != null && QuestsServer.server() != null;
	}

	@Nullable
	public static MinecraftServer server() {
		return QuestsServer.server();
	}

	public static boolean mayEdit(ServerPlayer player) {
		return QuestsServer.mayEdit(player);
	}

	public static boolean mayEdit(CommandSourceStack source) {
		if (source.hasPermission(QuestsServerConfig.get().editorPermissionLevel)) {
			return true;
		} else {
			if (source.getEntity() instanceof ServerPlayer player && QuestsServer.mayEdit(player)) {
				return true;
			}

			return false;
		}
	}

	@Nullable
	public static QuestBook book() {
		return QuestsServer.book();
	}

	public static Chapter cleaned(Chapter chapter) {
		return BookEditor.clean(BookJson.copy(chapter));
	}

	public static String put(Chapter chapter) {
		QuestEngine e = QuestsServer.engine();
		if (e == null) {
			return "disabled";
		} else {
			QuestEngine.Outcome o = BookEditor.editChapter(e, QuestsServer.bookDir(), chapter.id, JsonWire.encode(BookJson.GSON, chapter));
			e.flush();
			return o.code();
		}
	}

	public static String remove(String chapterId) {
		QuestEngine e = QuestsServer.engine();
		if (e == null) {
			return "disabled";
		} else {
			QuestEngine.Outcome o = BookEditor.deleteChapter(e, QuestsServer.bookDir(), chapterId);
			e.flush();
			return o.code();
		}
	}

	public static String groups(List<QuestBook.Group> groups) {
		QuestEngine e = QuestsServer.engine();
		if (e == null) {
			return "disabled";
		} else {
			QuestBook book = e.book();
			List<QuestBook.Group> before = book.groups;
			long revision = book.revision;
			book.groups = new ArrayList<>(groups);
			book.revision = Math.max(book.revision + 1L, System.currentTimeMillis());

			try {
				BookJson.saveHead(QuestsServer.bookDir(), book);
			} catch (IOException var8) {
				CFHGuiQuests.LOGGER.error("[CFHGui Quests] the head of the book could not be written to {}: {}", QuestsServer.bookDir(), var8.toString());
				book.groups = before;
				book.revision = revision;
				return "write_failed";
			}

			e.repack("");

			for (Seat s : e.seats()) {
				e.sendBook(s);
			}

			e.flush();
			return "ok";
		}
	}

	public static Set<String> unreadable(Chapter chapter) {
		Set<String> out = new LinkedHashSet<>();
		MinecraftServer server = QuestsServer.server();
		if (server == null) {
			return out;
		} else {
			QuestBook scratch = new QuestBook();
			scratch.chapters.put(chapter.id, chapter);
			TriggerIndex index = new TriggerIndex(new BookIndex(scratch), server.registryAccess());
			String prefix = chapter.id + "/";

			for (String line : index.unreadable) {
				int at = line.indexOf(" task ");
				if (at > prefix.length() && line.startsWith(prefix)) {
					out.add(line.substring(prefix.length(), at));
				}
			}

			return out;
		}
	}

	public static void look() {
		QuestEngine e = QuestsServer.engine();
		if (e != null) {
			e.lookFor();
			e.flush();
		}
	}

	private BookDoor() {
	}
}
