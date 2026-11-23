package com.onehumanawa.cfhcore.content.gui.emi.screen.widget.config;

import com.google.common.collect.Lists;
import com.onehumanawa.cfhcore.content.gui.emi.EmiPort;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiDrawContext;
import com.onehumanawa.cfhcore.content.gui.crafting.look.ConfigLook;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

public class GroupNameWidget extends ListWidget.Entry {
	protected static final Minecraft CLIENT = Minecraft.getInstance();
	public final String id;
	public final Component text;
	public final List<ConfigEntryWidget> children = Lists.newArrayList();
	public boolean collapsed = false;

	public GroupNameWidget(String id, Component text) {
		this.id = id;
		this.text = text;
	}

	@Override
	public void render(GuiGraphics raw, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean hovered, float delta) {
		EmiDrawContext context = EmiDrawContext.wrap(raw);
		if (!ConfigLook.group(raw, this.text, x, y, width, this.collapsed, hovered, true)) {
			context.drawCenteredTextWithShadow(this.text, x + width / 2, y + 3, -1);
			if (hovered || this.collapsed) {
				String collapse = "[-]";
				int cx = x + width / 2 - CLIENT.font.width(this.text) / 2 - 20;
				if (this.collapsed) {
					collapse = "[+]";
				}

				context.drawTextWithShadow(EmiPort.literal(collapse), cx, y + 3, -1);
			}
		}
	}

	@Override
	public int getHeight() {
		for (ConfigEntryWidget w : this.children) {
			if (w.isVisible()) {
				return 20;
			}
		}

		return 0;
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.isMouseOver(mouseX, mouseY)) {
			this.collapsed = !this.collapsed;
			EmiPort.playClickSound();
			return true;
		} else {
			return false;
		}
	}

	public List<? extends GuiEventListener> children() {
		return List.of();
	}
}
