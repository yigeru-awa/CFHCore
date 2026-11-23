package com.onehumanawa.cfhcore.content.gui.teams.client.vanilla;

import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsDraw;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.ObjectSelectionList.Entry;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class RowList extends ObjectSelectionList<RowList.Row> {
	@Nullable
	private Runnable onSelect;
	@Nullable
	private Consumer<RowList.Row> onActivate;
	private int rowWidth = 220;
	@Nullable
	private Component emptyText;
	static final int ROW_MAX = 400;

	RowList(Minecraft mc, int itemHeight) {
		super(mc, 100, 100, 0, itemHeight);
	}

	RowList.Row row(String key, Object value, Component narration, RowList.Painter painter) {
		return new RowList.Row(this, key, false, value, narration, painter);
	}

	RowList.Row plain(String key, Component narration, RowList.Painter painter) {
		return new RowList.Row(this, key, true, null, narration, painter);
	}

	RowList.Row caption(String key, Component text) {
		return new RowList.Row(this, key, true, null, text, (g, x, y, w, h, hovered, selected, mouseX, mouseY) -> {
			TeamsDraw.text(g, text, x + 2, y + (h - 8) / 2, Theme.current().palette().textMuted(), 1.0F);
			int tw = this.minecraft.font.width(text);
			g.fill(x + tw + 8, y + h / 2, x + w - 4, y + h / 2 + 1, Theme.current().isVanilla() ? 1627389951 : Theme.current().palette().border());
		});
	}

	RowList onSelect(Runnable r) {
		this.onSelect = r;
		return this;
	}

	RowList onActivate(Consumer<RowList.Row> c) {
		this.onActivate = c;
		return this;
	}

	RowList emptyText(Component text) {
		this.emptyText = text;
		return this;
	}

	void place(int x, int y, int width, int height) {
		this.setX(x);
		this.setY(y);
		this.setWidth(width);
		this.setHeight(height);
		this.rowWidth = Math.max(60, Math.min(400, width - 20));
		this.clampScrollAmount();
	}

	void rows(List<RowList.Row> rows) {
		RowList.Row was = (RowList.Row)this.getSelected();
		double at = this.getScrollAmount();
		this.replaceEntries(rows);
		RowList.Row keep = null;
		if (was != null) {
			for (RowList.Row r : rows) {
				if (!r.caption && r.key.equals(was.key)) {
					keep = r;
					break;
				}
			}
		}

		super.setSelected(keep);
		this.setScrollAmount(at);
		this.clampScrollAmount();
		if (this.onSelect != null) {
			this.onSelect.run();
		}
	}

	@Nullable
	<T> T chosen(Class<T> type) {
		RowList.Row r = (RowList.Row)this.getSelected();
		return r != null && type.isInstance(r.value) ? type.cast(r.value) : null;
	}

	public void setSelected(@Nullable RowList.Row row) {
		if (row == null || !row.caption) {
			super.setSelected(row);
			if (this.onSelect != null) {
				this.onSelect.run();
			}
		}
	}

	public int getRowWidth() {
		return this.rowWidth;
	}

	protected int getScrollbarPosition() {
		return Math.min(this.getDefaultScrollbarPosition(), this.getX() + this.getWidth() - 6);
	}

	protected void renderListBackground(GuiGraphics g) {
	}

	protected void renderListSeparators(GuiGraphics g) {
	}

	public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.renderWidget(g, mouseX, mouseY, partialTick);
		if (this.children().isEmpty() && this.emptyText != null) {
			TeamsDraw.textCentered(
				g, this.emptyText, this.getX() + this.getWidth() / 2, this.getY() + this.getHeight() / 2 - 4, Theme.current().palette().textMuted(), 1.0F
			);
		}
	}

	@FunctionalInterface
	interface Painter {
		void paint(GuiGraphics var1, int var2, int var3, int var4, int var5, boolean var6, boolean var7, int var8, int var9);
	}

	static final class Row extends Entry<RowList.Row> {
		private final RowList owner;
		final String key;
		final boolean caption;
		@Nullable
		final Object value;
		private final Component narration;
		private final RowList.Painter painter;
		private long lastClick;

		private Row(RowList owner, String key, boolean caption, @Nullable Object value, Component narration, RowList.Painter painter) {
			this.owner = owner;
			this.key = key;
			this.caption = caption;
			this.value = value;
			this.narration = narration;
			this.painter = painter;
		}

		public Component getNarration() {
			return this.narration;
		}

		public void render(GuiGraphics g, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
			this.painter.paint(g, left, top, width, height, hovering && !this.caption, this.owner.getSelected() == this, mouseX, mouseY);
		}

		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (this.caption) {
				return false;
			} else {
				this.owner.setSelected(this);
				long now = Util.getMillis();
				if (now - this.lastClick < 250L && this.owner.onActivate != null) {
					this.owner.onActivate.accept(this);
				}

				this.lastClick = now;
				return true;
			}
		}
	}
}
