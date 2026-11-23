package com.onehumanawa.cfhcore.content.gui.quests.client.editor;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTextField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

class EditField extends CFHGuiTextField {
	static final int HEIGHT = 18;
	private String committed = "";
	@Nullable
	private Consumer<String> onCommit;
	@Nullable
	private Consumer<String> onTyping;
	@Nullable
	private Predicate<String> valid;
	@Nullable
	private List<Component> tip;
	private long hoverSinceMs;
	private boolean number;
	private boolean decimal;
	private double min = -Double.MAX_VALUE;
	private double max = Double.MAX_VALUE;

	EditField(int width, Component narration, @Nullable String value) {
		super(0, 0, width, 18, narration);
		this.setMaxLength(256);
		this.setResponder(text -> {
			if (this.valid != null) {
				this.setInvalid(!this.valid.test(text));
			}

			if (this.onTyping != null) {
				this.onTyping.accept(text);
			}
		});
		this.set(value);
	}

	final void set(@Nullable String value) {
		this.committed = value == null ? "" : value;
		this.setValue(this.committed);
		this.moveCursorToStart(false);
	}

	EditField max(int length) {
		this.setMaxLength(length);
		if (!this.getValue().equals(this.committed)) {
			this.setValue(this.committed);
			this.moveCursorToStart(false);
		}

		return this;
	}

	EditField onCommit(Consumer<String> c) {
		this.onCommit = c;
		return this;
	}

	EditField typing(Consumer<String> c) {
		this.onTyping = c;
		return this;
	}

	EditField valid(Predicate<String> ok) {
		this.valid = ok;
		this.setInvalid(!ok.test(this.getValue()));
		return this;
	}

	EditField hint(Component placeholder) {
		this.placeholder(placeholder);
		return this;
	}

	EditField tooltip(@Nullable Component text) {
		this.tip = text != null && !text.getString().isEmpty() ? List.of(text) : null;
		return this;
	}

	EditField whole(long min, long max) {
		this.number = true;
		this.min = (double)min;
		this.max = (double)max;
		this.setFilter(s -> s.isEmpty() || min < 0L && s.equals("-") || s.matches(min < 0L ? "-?\\d{1,12}" : "\\d{1,12}"));
		return this;
	}

	EditField decimal(double min, double max) {
		this.number = this.decimal = true;
		this.min = min;
		this.max = max;
		this.setFilter(s -> s.isEmpty() || s.matches("-?\\d{0,7}([.,]\\d{0,2})?"));
		return this;
	}

	double number(double fallback) {
		try {
			return Math.max(this.min, Math.min(this.max, Double.parseDouble(this.getValue().trim().replace(',', '.'))));
		} catch (NumberFormatException var4) {
			return fallback;
		}
	}

	boolean dirty() {
		return !this.getValue().equals(this.committed);
	}

	void commit() {
		if (this.dirty()) {
			if (this.number) {
				double v = this.number(Double.NaN);
				if (Double.isNaN(v)) {
					this.revert();
					return;
				}

				String tidy = this.decimal ? EditorTool.fmt((float)v) : Long.toString(Math.round(v));
				if (!tidy.equals(this.getValue())) {
					this.setValue(tidy);
				}

				if (tidy.equals(this.committed)) {
					return;
				}
			}

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

	public void setFocused(boolean focused) {
		boolean was = this.isFocused();
		super.setFocused(focused);
		if (was && !focused) {
			this.commit();
			this.setHighlightPos(this.getCursorPosition());
		}
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (!this.isFocused() || !this.active) {
			return false;
		} else if (keyCode != 257 && keyCode != 335) {
			if (this.number && (keyCode == 264 || keyCode == 265)) {
				double by = (double)((keyCode == 265 ? 1 : -1) * ((modifiers & 1) != 0 ? 10 : 1)) * (this.decimal ? 0.1 : 1.0);
				double v = Math.max(this.min, Math.min(this.max, this.number(0.0) + by));
				this.setValue(this.decimal ? EditorTool.fmt((float)v) : Long.toString(Math.round(v)));
				this.commit();
				return true;
			} else {
				return super.keyPressed(keyCode, scanCode, modifiers);
			}
		} else {
			this.commit();
			return true;
		}
	}

	public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.renderWidget(g, mouseX, mouseY, partialTick);
		if (this.tip != null && this.visible) {
			if (this.isHovered() && g.containsPointInScissor(mouseX, mouseY)) {
				if (this.hoverSinceMs == 0L) {
					this.hoverSinceMs = Clock.nowMs();
				}

				if (Clock.nowMs() - this.hoverSinceMs > 350L && !this.isFocused()) {
					CFHGuiTooltips.request(this.tip, this);
				}
			} else {
				this.hoverSinceMs = 0L;
			}
		}
	}
}
