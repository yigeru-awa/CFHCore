package com.onehumanawa.cfhcore.content.gui.quests.client.view;

import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import org.jetbrains.annotations.Nullable;

public interface TreeView {
	AbstractWidget widget();

	void place(int var1, int var2, int var3, int var4);

	default boolean fillsScreen() {
		return false;
	}

	void cover(int var1, int var2, int var3, int var4);

	void show(Chapter var1, TreeView.Travel var2, @Nullable String var3);

	@Nullable
	Chapter chapter();

	void rebuild(Chapter var1);

	void refresh();

	boolean travelling();

	boolean focus(@Nullable String var1);

	void reveal(@Nullable String var1);

	void select(@Nullable String var1);

	@Nullable
	String selected();

	@Nullable
	String hovered();

	void fit();

	boolean step(int var1);

	void filter(TreeFilter var1);

	TreeFilter filter();

	void showHidden(boolean var1);

	void onOpen(@Nullable Consumer<String> var1);

	void onEnter(@Nullable Consumer<String> var1);

	void onContext(@Nullable TreeView.ContextHandler var1);

	void renderOver(GuiGraphics var1, int var2, int var3);

	default boolean dragging() {
		return false;
	}

	default boolean opensInline() {
		return false;
	}

	default boolean unfolded(@Nullable String questKey) {
		return false;
	}

	default void unfold(String questKey, boolean open) {
	}

	void remember();

	boolean key(int var1, int var2, int var3);

	@FunctionalInterface
	public interface ContextHandler {
		boolean open(@Nullable String var1, double var2, double var4, float var6, float var7);
	}

	public static enum Travel {
		NONE,
		ENTRANCE,
		INTO,
		OUT;
	}
}
