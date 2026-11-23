package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import com.onehumanawa.cfhcore.content.gui.emi.api.EmiApi;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStackInteraction;
import com.onehumanawa.cfhcore.content.gui.emi.api.widget.Bounds;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiDrawContext;
import com.onehumanawa.cfhcore.content.gui.emi.screen.EmiScreenManager;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;

public final class TreeDockSeam {
	private static final TreeDockSeam.Dock NONE = new TreeDockSeam.Dock() {
		@Override
		public Bounds layout(Screen screen, Bounds room, List<Bounds> exclusion) {
			return Bounds.EMPTY;
		}

		@Override
		public void render(EmiDrawContext context, int mouseX, int mouseY, float delta) {
		}

		@Override
		public EmiStackInteraction hovered(int mouseX, int mouseY) {
			return EmiStackInteraction.EMPTY;
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			return false;
		}

		@Override
		public boolean mouseReleased(double mouseX, double mouseY, int button) {
			return false;
		}

		@Override
		public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
			return false;
		}

		@Override
		public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
			return false;
		}

		@Override
		public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
			return false;
		}

		@Override
		public boolean charTyped(char chr, int modifiers) {
			return false;
		}

		@Override
		public int reserve(Screen screen) {
			return 0;
		}

		@Override
		public void buttonPressed() {
			EmiApi.viewRecipeTree();
		}

		@Override
		public boolean isOpen() {
			return false;
		}
	};
	private static TreeDockSeam.Dock dock = NONE;
	private static Bounds taken = Bounds.EMPTY;

	public static void install(TreeDockSeam.Dock installed) {
		dock = installed == null ? NONE : installed;
		taken = Bounds.EMPTY;
	}

	public static void changed() {
		EmiScreenManager.dockChanged();
	}

	public static Bounds layout(Screen screen, Bounds room, List<Bounds> exclusion) {
		Bounds b = dock.layout(screen, room, exclusion);
		taken = b == null ? Bounds.EMPTY : b;
		return taken;
	}

	public static Bounds bounds() {
		return taken;
	}

	public static boolean isOpen() {
		return dock.isOpen();
	}

	public static void render(EmiDrawContext context, int mouseX, int mouseY, float delta) {
		if (!taken.empty()) {
			dock.render(context, mouseX, mouseY, delta);
		}
	}

	public static EmiStackInteraction hovered(int mouseX, int mouseY) {
		if (taken.empty()) {
			return EmiStackInteraction.EMPTY;
		} else {
			EmiStackInteraction hovered = dock.hovered(mouseX, mouseY);
			return hovered == null ? EmiStackInteraction.EMPTY : hovered;
		}
	}

	public static boolean mouseClicked(double mouseX, double mouseY, int button) {
		return !taken.empty() && dock.mouseClicked(mouseX, mouseY, button);
	}

	public static boolean mouseReleased(double mouseX, double mouseY, int button) {
		return !taken.empty() && dock.mouseReleased(mouseX, mouseY, button);
	}

	public static boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		return !taken.empty() && dock.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	public static boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		return !taken.empty() && dock.mouseScrolled(mouseX, mouseY, amount);
	}

	public static boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		return dock.keyPressed(keyCode, scanCode, modifiers);
	}

	public static boolean charTyped(char chr, int modifiers) {
		return !taken.empty() && dock.charTyped(chr, modifiers);
	}

	public static void buttonPressed() {
		dock.buttonPressed();
	}

	public static int reserve(Screen screen) {
		return Math.max(0, dock.reserve(screen));
	}

	private TreeDockSeam() {
	}

	public interface Dock {
		Bounds layout(Screen var1, Bounds var2, List<Bounds> var3);

		void render(EmiDrawContext var1, int var2, int var3, float var4);

		EmiStackInteraction hovered(int var1, int var2);

		boolean mouseClicked(double var1, double var3, int var5);

		boolean mouseReleased(double var1, double var3, int var5);

		boolean mouseDragged(double var1, double var3, int var5, double var6, double var8);

		boolean mouseScrolled(double var1, double var3, double var5);

		boolean keyPressed(int var1, int var2, int var3);

		boolean charTyped(char var1, int var2);

		int reserve(Screen var1);

		void buttonPressed();

		boolean isOpen();
	}
}
