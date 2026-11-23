package com.onehumanawa.cfhcore.content.gui.quests.client.ui;

import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import org.jetbrains.annotations.Nullable;

public final class BookExtensions {
	private static final List<BookExtensions.Extension> EXTENSIONS = new CopyOnWriteArrayList<>();

	public static void register(BookExtensions.Extension extension) {
		if (!EXTENSIONS.contains(extension)) {
			EXTENSIONS.add(extension);
		}
	}

	static void each(Consumer<BookExtensions.Extension> call) {
		for (BookExtensions.Extension e : EXTENSIONS) {
			try {
				call.accept(e);
			} catch (Exception var4) {
				CFHGuiQuests.LOGGER.error("[CFHGui Quests] an extension of the quest book threw", var4);
			}
		}
	}

	static boolean key(QuestBookScreen screen, int keyCode, int scanCode, int modifiers) {
		for (BookExtensions.Extension e : EXTENSIONS) {
			try {
				if (e.keyPressed(screen, keyCode, scanCode, modifiers)) {
					return true;
				}
			} catch (Exception var7) {
				CFHGuiQuests.LOGGER.error("[CFHGui Quests] an extension of the quest book threw", var7);
			}
		}

		return false;
	}

	private BookExtensions() {
	}

	public interface Extension {
		default void header(QuestBookScreen screen, Consumer<AbstractWidget> add) {
		}

		default void chapterShown(QuestBookScreen screen, @Nullable Chapter chapter) {
		}

		default boolean keyPressed(QuestBookScreen screen, int keyCode, int scanCode, int modifiers) {
			return false;
		}

		default void closed(QuestBookScreen screen) {
		}
	}

	public interface SidePanel {
		default int width(int available) {
			return QuestSheet.widthFor(available);
		}

		void build(QuestBookScreen var1, Rect var2, Consumer<AbstractWidget> var3);

		default void render(GuiGraphics g, Rect area, int mouseX, int mouseY, float partialTick) {
		}

		default boolean keyPressed(QuestBookScreen screen, int keyCode, int scanCode, int modifiers) {
			return false;
		}

		default void closed() {
		}
	}
}
