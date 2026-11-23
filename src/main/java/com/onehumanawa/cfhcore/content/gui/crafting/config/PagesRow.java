package com.onehumanawa.cfhcore.content.gui.crafting.config;

import com.onehumanawa.cfhcore.content.gui.emi.config.SidebarPages;
import com.onehumanawa.cfhcore.content.gui.emi.config.SidebarType;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.crafting.look.LookButtons;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class PagesRow extends SettingLine {
	private static final SidebarType[] KINDS = new SidebarType[]{
		SidebarType.INDEX, SidebarType.CRAFTABLES, SidebarType.FAVORITES, SidebarType.LOOKUP_HISTORY, SidebarType.CRAFT_HISTORY, SidebarType.EMPTY, SidebarType.CHESS
	};
	private final SidebarPages pages;
	private final List<SidebarType> defaults;
	private final Runnable changed;
	private int tipCell = -1;
	private long tipSince;

	PagesRow(int width, Component label, @Nullable Component tooltip, SidebarPages pages, List<SidebarType> defaults, Runnable changed) {
		super(width, label, tooltip);
		this.pages = pages;
		this.defaults = defaults;
		this.changed = changed;
	}

	private List<SidebarType> shown() {
		List<SidebarType> out = new ArrayList<>();

		for (SidebarPages.SidebarPage page : this.pages.pages) {
			out.add(page.type);
		}

		return out;
	}

	private SidebarType kind(int index) {
		List<SidebarType> shown = this.shown();
		if (index < shown.size()) {
			return shown.get(index);
		} else {
			int i = index - shown.size();

			for (SidebarType type : KINDS) {
				if (!shown.contains(type) && i-- == 0) {
					return type;
				}
			}

			return SidebarType.NONE;
		}
	}

	@Override
	protected int cellCount() {
		return KINDS.length;
	}

	@Override
	protected int cellWidth(int index) {
		return 16;
	}

	@Override
	protected void drawCell(GuiGraphics g, SettingLine.Cell cell, int y, boolean hovered, int mouseX, int mouseY) {
		SidebarType type = this.kind(cell.index());
		boolean on = cell.index() < this.pages.pages.size();
		int fg = face(g, cell.x(), y, cell.width(), hovered, on, !on);
		Icons.draw(g, LookButtons.kindIcon(type), cell.x() + (cell.width() - 8) / 2, y + 4, 8, fg);
		if (!hovered) {
			if (this.tipCell == cell.index()) {
				this.tipCell = -1;
			}
		} else {
			if (this.tipCell != cell.index()) {
				this.tipCell = cell.index();
				this.tipSince = Clock.nowMs();
			}

			if (Clock.nowMs() - this.tipSince > 350L) {
				CFHGuiTooltips.request(
					List.of(type.getText(), type.getDescription(), LookButtons.hint(on ? "cfhgui_crafting.settings.pages.remove" : "cfhgui_crafting.settings.pages.add")), this
				);
			}
		}
	}

	@Override
	protected void pressCell(int index, int button) {
		if (button == 0) {
			SidebarType type = this.kind(index);
			if (type != SidebarType.NONE) {
				if (index < this.pages.pages.size()) {
					this.pages.pages.remove(index);
				} else {
					this.pages.pages.add(new SidebarPages.SidebarPage(type));
				}

				this.pages.unique();
				this.changed.run();
			}
		}
	}

	@Override
	protected boolean isDefault() {
		return this.shown().equals(this.defaults);
	}

	@Override
	protected void reset() {
		this.pages.pages.clear();

		for (SidebarType type : this.defaults) {
			this.pages.pages.add(new SidebarPages.SidebarPage(type));
		}

		this.pages.unique();
		this.changed.run();
	}
}
