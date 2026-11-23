package com.onehumanawa.cfhcore.content.gui.quests.client.editor;

import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.NodeView;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestPainter;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.CFHGuiQuestPainter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class EditorPainter extends CFHGuiQuestPainter {
	@Override
	public void empty(GuiGraphics g, QuestPainter.View view, Component text) {
	}

	@Override
	protected void tones(QuestPainter.View view, NodeView n) {
		super.tones(view, n);
		if (n.locked()) {
			Theme t = Theme.current();
			this.dim = 0.0F;
			if (t.isVanilla()) {
				this.fill = -10987432;
				this.tint = -1;
			} else {
				Palette p = t.palette();
				float hover = n.hover.get();
				this.fill = Colors.lerp(p.surface(), p.surfaceHover(), hover);
				this.rim = Colors.lerp(p.borderStrong(), Colors.mix(p.borderStrong(), view.accent(), 0.5F), hover);
				this.tint = p.text();
			}
		}
	}
}
