package com.onehumanawa.cfhcore.content.gui.quests.client.editor;

import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class TextArea extends MultiLineEditBox {
	private String committed = "";
	@Nullable
	private Consumer<String> onCommit;
	@Nullable
	private Consumer<String> onTyping;

	TextArea(int width, int height, Component hint, Component narration) {
		super(CFHGuiDraw.font(), 0, 0, width, height, hint, narration);
		this.setValueListener(text -> {
			if (this.onTyping != null) {
				this.onTyping.accept(text);
			}
		});
	}

	static int heightFor(int lines) {
		return 8 + lines * 9;
	}

	static int heightIn(int room) {
		return heightFor(Math.max(2, (room - 8) / 9));
	}

	static String join(List<String> lines) {
		return String.join("\n", lines);
	}

	static List<String> split(String text) {
		List<String> out = new ArrayList<>(List.of(text.replace("\r", "").split("\n", -1)));

		while (!out.isEmpty() && out.get(out.size() - 1).isBlank()) {
			out.remove(out.size() - 1);
		}

		return out;
	}

	void set(String text) {
		this.committed = text;
		this.setValue(text);
		this.setScrollAmount(0.0);
	}

	TextArea onCommit(Consumer<String> c) {
		this.onCommit = c;
		return this;
	}

	TextArea typing(Consumer<String> c) {
		this.onTyping = c;
		return this;
	}

	boolean dirty() {
		return !this.getValue().equals(this.committed);
	}

	void commit() {
		if (this.dirty()) {
			this.committed = this.getValue();
			if (this.onCommit != null) {
				this.onCommit.accept(this.committed);
			}
		}
	}

	void revert() {
		if (this.dirty()) {
			this.setValue(this.committed);
		}
	}

	void type(String words) {
		boolean had = this.isFocused();
		if (!had) {
			super.setFocused(true);
		}

		for (int i = 0; i < words.length(); i++) {
			char c = words.charAt(i);
			if (c == '\n') {
				this.keyPressed(257, 0, 0);
			} else {
				this.charTyped(c, 0);
			}
		}

		if (!had) {
			super.setFocused(false);
		}
	}

	public void setFocused(boolean focused) {
		boolean was = this.isFocused();
		super.setFocused(focused);
		if (was && !focused) {
			this.commit();
		}
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (!this.scrollbarVisible()) {
			return false;
		} else if (scrollY > 0.0 && this.scrollAmount() <= 0.0) {
			return false;
		} else {
			return scrollY < 0.0 && this.scrollAmount() >= (double)this.getMaxScrollAmount() ? false : super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		}
	}

	protected void renderContents(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		g.enableScissor(this.getX() + 1, this.getY() + this.innerPadding(), this.getX() + this.getWidth() - 1, this.getY() + this.getHeight() - this.innerPadding());
		super.renderContents(g, mouseX, mouseY, partialTick);
		g.disableScissor();
	}

	static int barRoom() {
		return Theme.current().isVanilla() ? 9 : 0;
	}

	protected void renderDecorations(GuiGraphics g) {
		Theme t = Theme.current();
		if (t.isVanilla()) {
			super.renderDecorations(g);
		} else if (this.scrollbarVisible()) {
			int h = this.getHeight() - 4;
			int content = this.getInnerHeight() + 8;
			int barH = Math.max(10, Math.min(h, h * h / Math.max(1, content)));
			int barY = this.getY() + 2 + (int)((double)(h - barH) * (this.scrollAmount() / (double)Math.max(1, this.getMaxScrollAmount())));
			CFHGuiDraw.pixelRound(g, this.getX() + this.getWidth() - 4, barY, 2, barH, t.palette().borderStrong(), 1);
		}
	}

	protected void renderBackground(GuiGraphics g) {
		Theme t = Theme.current();
		if (t.isVanilla()) {
			super.renderBackground(g);
		} else {
			Palette p = t.palette();
			CFHGuiDraw.pixelRound(g, this.getX(), this.getY(), this.getWidth(), this.getHeight(), p.bg2(), t.radius());
			CFHGuiDraw.outline(
				g,
				this.getX(),
				this.getY(),
				this.getWidth(),
				this.getHeight(),
				this.isFocused() ? p.accent() : (this.isHovered() ? p.borderStrong() : p.border()),
				t.radius()
			);
		}
	}
}
