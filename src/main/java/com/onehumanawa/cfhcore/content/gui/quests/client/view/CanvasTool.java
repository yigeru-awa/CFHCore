package com.onehumanawa.cfhcore.content.gui.quests.client.view;

import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

public interface CanvasTool {
	default boolean mouseClicked(QuestCanvas canvas, @Nullable NodeView node, double mouseX, double mouseY, int button) {
		return false;
	}

	default boolean mouseDragged(QuestCanvas canvas, double mouseX, double mouseY, int button, double dragX, double dragY) {
		return false;
	}

	default boolean mouseReleased(QuestCanvas canvas, double mouseX, double mouseY, int button) {
		return false;
	}

	default boolean keyPressed(QuestCanvas canvas, int key, int scan, int modifiers) {
		return false;
	}

	default void renderUnder(GuiGraphics g, QuestCanvas canvas, int mouseX, int mouseY) {
	}

	default void renderOver(GuiGraphics g, QuestCanvas canvas, int mouseX, int mouseY) {
	}

	default boolean quiet() {
		return false;
	}
}
