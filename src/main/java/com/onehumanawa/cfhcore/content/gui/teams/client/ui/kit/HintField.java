package com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTextField;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class HintField extends CFHGuiTextField {
	private final Component hint;
	private boolean hasIcon;

	public HintField(int x, int y, int width, Component hint) {
		super(x, y, width, 20, hint);
		this.hint = hint;
		this.placeholder(hint);
	}

	public HintField icon(@Nullable Icon icon) {
		super.icon(icon);
		this.hasIcon = icon != null;
		return this;
	}

	public HintField maxLength(int n) {
		super.maxLength(n);
		return this;
	}

	public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.renderWidget(g, mouseX, mouseY, partialTick);
		if (this.visible && this.isFocused() && this.getValue().isEmpty()) {
			int x = this.getX() + 6 + (this.hasIcon ? 16 : 0);
			int y = this.getY() + (this.getHeight() - 9) / 2 + 1;
			g.drawString(
				CFHGuiDraw.font(), CFHGuiDraw.truncate(this.hint, this.getInnerWidth()), x, y, Colors.scaleAlpha(Theme.current().palette().textDim(), this.alpha), false
			);
		}
	}
}
