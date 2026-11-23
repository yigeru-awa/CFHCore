package com.onehumanawa.cfhcore.content.gui.teams.client.overhaul;

import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.stage.Stage;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageWidget;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class SceneFrame {
	static void plate(GuiGraphics g, Rect view) {
		Theme t = Theme.current();
		boolean van = t.isVanilla();
		CFHGuiDraw.shadow(g, view.x(), view.y(), view.w(), view.h(), 0.55F);
		CFHGuiDraw.pixelRound(g, view.x(), view.y(), view.w(), view.h(), van ? -16777216 : -16053233, van ? 0 : t.radius());
	}

	static void footShade(GuiGraphics g, Rect view, int height, float alpha) {
		int h = Math.min(height, view.h() - 2);
		CFHGuiDraw.vgradient(g, view.x() + 1, view.bottom() - 1 - h, view.w() - 2, h, 0, Colors.withAlpha(0, Math.round(168.0F * alpha)));
	}

	static void label(GuiGraphics g, int x, int y, int w, int h, float alpha) {
		Theme t = Theme.current();
		if (t.isVanilla()) {
			CFHGuiDraw.rect(g, x, y, w, h, Colors.scaleAlpha(-1073741824, alpha));
		} else {
			CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(Colors.withAlpha(t.palette().bg(), 216), alpha), 2);
			CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(Colors.withAlpha(t.palette().border(), 224), alpha), 2);
		}
	}

	private SceneFrame() {
	}

	static final class Overlay extends CFHGuiWidget {
		private final Rect frame;
		private final SceneFrame.Painter painter;

		Overlay(Rect view, SceneFrame.Painter painter) {
			super(view.x(), view.y(), view.w(), view.h(), Component.empty());
			this.frame = view;
			this.painter = painter;
			this.active = false;
			this.silent();
		}

		public boolean isMouseOver(double mouseX, double mouseY) {
			return false;
		}

		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			return false;
		}

		public ComponentPath nextFocusPath(FocusNavigationEvent event) {
			return null;
		}

		protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			this.painter.paint(g, mouseX, mouseY);
			Theme t = Theme.current();
			CFHGuiDraw.outline(g, this.frame.x(), this.frame.y(), this.frame.w(), this.frame.h(), t.palette().borderStrong(), t.radius());
		}

		protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			this.painter.paint(g, mouseX, mouseY);
			CFHGuiDraw.outline(g, this.frame.x(), this.frame.y(), this.frame.w(), this.frame.h(), -16777216, 0);
		}
	}

	@FunctionalInterface
	interface Painter {
		void paint(GuiGraphics var1, int var2, int var3);
	}

	static final class View extends StageWidget {
		@Nullable
		private final IntConsumer onWheel;

		View(Rect view, Stage stage, Component label, @Nullable IntConsumer onWheel) {
			super(view.x() + 1, view.y() + 1, view.w() - 2, view.h() - 2, stage, label);
			this.onWheel = onWheel;
		}

		public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
			if (this.onWheel != null && scrollY != 0.0 && this.visible && this.isMouseOver(mouseX, mouseY)) {
				this.onWheel.accept(scrollY > 0.0 ? -1 : 1);
				return true;
			} else {
				return false;
			}
		}
	}
}
