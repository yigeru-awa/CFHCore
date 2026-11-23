package com.onehumanawa.cfhcore.content.gui.emi.screen.widget.config;

import com.onehumanawa.cfhcore.content.gui.emi.EmiPort;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiDrawContext;
import com.onehumanawa.cfhcore.content.gui.crafting.look.ConfigLook;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class SubGroupNameWidget extends GroupNameWidget {
	public GroupNameWidget parent;

	public SubGroupNameWidget(String id, Component text) {
		super(id, text);
	}

	@Override
	public void render(GuiGraphics raw, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean hovered, float delta) {
		EmiDrawContext context = EmiDrawContext.wrap(raw);
		if (!ConfigLook.group(raw, this.text, x, y, width, this.collapsed, hovered, false)) {
			context.drawTextWithShadow(this.text, x + 20, y + 3);
			if (hovered || this.collapsed) {
				String collapse = "[-]";
				if (this.collapsed) {
					collapse = "[+]";
				}

				context.drawTextWithShadow(EmiPort.literal(collapse), x, y + 3);
			}
		}
	}

	@Override
	public int getHeight() {
		return this.parent.collapsed ? 0 : super.getHeight();
	}
}
