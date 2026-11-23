package com.onehumanawa.cfhcore.content.gui.quests.client.dev;

import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class AdvTreesScreen extends CFHGuiScreen {
	private final AdvTreesPanel panel = new AdvTreesPanel();

	public AdvTreesScreen(@Nullable Screen parent) {
		super(Component.translatable("cfhgui_quests.adv.title"), parent);
		this.maxContentWidth = 520;
	}

	@Nullable
	Screen origin() {
		return this.parent;
	}

	protected void build() {
		Rect area = this.contentRect();
		this.panel.build(new Rect(area.x(), area.y() + 2, area.w(), area.h() - 2), this, x$0 -> {
			AbstractWidget var10000 = (AbstractWidget)this.add(x$0);
		});
	}

	public void tick() {
		super.tick();
		this.panel.tick();
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.panel.typing() || keyCode != 47 && (keyCode != 70 || (modifiers & (Minecraft.ON_OSX ? 8 : 2)) == 0)) {
			return super.keyPressed(keyCode, scanCode, modifiers);
		} else {
			this.panel.focusSearch(this);
			return true;
		}
	}

	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(g, mouseX, mouseY, partialTick);
		this.panel.render(g, mouseX, mouseY);

		for (Renderable r : this.renderableList()) {
			r.render(g, mouseX, mouseY, partialTick);
		}

		if (this.showHeader) {
			this.renderHeader(g);
		}
	}
}
