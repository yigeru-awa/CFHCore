package com.onehumanawa.cfhcore.content.gui.teams.client.ui.dialog;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public interface FormPart {
	Component title();

	@Nullable
	default Icon icon() {
		return null;
	}

	int preferredWidth();

	default int widthFor(int roomWidth, int roomHeight) {
		return Math.min(this.preferredWidth(), roomWidth);
	}

	int build(int var1, FormPart.Placer var2);

	default void render(GuiGraphics g, int x, int y, int width, float alpha, int mouseX, int mouseY) {
	}

	Component submitLabel();

	default Variant submitVariant() {
		return Variant.PRIMARY;
	}

	boolean ready();

	default boolean mayBeRefused() {
		return true;
	}

	void submit(FormPart.Host var1);

	@Nullable
	default AbstractWidget firstFocus() {
		return null;
	}

	default boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		return false;
	}

	public interface Host {
		void done();

		void fail(Component var1);

		void cancel();
	}

	@FunctionalInterface
	public interface Placer {
		void place(AbstractWidget var1, int var2, int var3);
	}
}
