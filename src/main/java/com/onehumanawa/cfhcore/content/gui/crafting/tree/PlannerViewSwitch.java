package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;

final class PlannerViewSwitch extends CFHGuiWidget {
	private static final String[] OUTLINE = new String[]{
		"XX.XXXXXXXX", "...........", ".XX.XXXXXX.", "...........", "..XX.XXXXX.", "...........", "..XX.XXXXX.", "...........", ".XX.XXXXXX.", "..........."
	};
	private static final String[] GRAPH = new String[]{
		"....XXX....", "....XXX....", ".....X.....", ".XXXXXXXXX.", ".X...X...X.", ".X...X...X.", "XXX.XXX.XXX", "XXX.XXX.XXX", "...........", "..........."
	};
	private static final int PICTURE_W = 11;
	private static final int PICTURE_H = 10;
	private static final int PAD = 5;
	private static final int GAP = 3;
	private static final int BARE = 20;
	private final boolean words;
	private final Runnable onPress;
	private final int split;
	private boolean graph;
	private final Anim slide = new Anim(0.0F, 180, Ease.OUT_CUBIC);

	private static Component name(boolean graph) {
		return Component.translatable(graph ? "cfhgui_crafting.planner.view.graph.name" : "cfhgui_crafting.planner.view.outline.name");
	}

	private static int half(boolean graph, boolean words) {
		return words ? 19 + CFHGuiDraw.width(name(graph)) + 5 : 20;
	}

	static int width(boolean words) {
		return half(false, words) + half(true, words);
	}

	PlannerViewSwitch(boolean graph, boolean words, Runnable onPress) {
		super(0, 0, width(words), 20, Component.translatable("cfhgui_crafting.planner.view"));
		this.graph = graph;
		this.words = words;
		this.onPress = onPress;
		this.split = half(false, words);
		this.slide.snap(graph ? 1.0F : 0.0F);
	}

	void show(boolean toGraph) {
		if (this.graph != toGraph) {
			this.graph = toGraph;
			this.slide.set(toGraph ? 1.0F : 0.0F);
		}
	}

	private boolean overGraph(double mouseX) {
		return mouseX >= (double)(this.getX() + this.split);
	}

	public void onClick(double mouseX, double mouseY) {
		super.onClick(mouseX, mouseY);
		if (this.overGraph(mouseX) != this.graph) {
			this.onPress.run();
		}
	}

	public void playDownSound(SoundManager handler) {
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.active && this.visible) {
			if ((keyCode != 263 || !this.graph) && (keyCode != 262 || this.graph) && keyCode != 257 && keyCode != 335 && keyCode != 32) {
				return false;
			} else {
				this.onPress.run();
				return true;
			}
		} else {
			return false;
		}
	}

	private void say(int mouseX) {
		this.tip(Component.translatable(this.overGraph((double)mouseX) ? "cfhgui_crafting.planner.view.graph" : "cfhgui_crafting.planner.view.outline"));
	}

	protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		Theme t = Theme.current();
		Palette p = t.palette();
		float a = this.effectiveAlpha();
		if (!(a <= 0.004F)) {
			int x = this.getX();
			int y = this.getY() + this.enterOffset();
			int w = this.getWidth();
			int h = this.getHeight();
			boolean hovered = this.isHovered();
			this.say(mouseX);
			CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(p.bg2(), a), t.radius());
			CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(p.border(), a), t.radius());
			int inner = Math.max(0, t.radius() - 1);
			boolean otherHovered = hovered && this.overGraph((double)mouseX) != this.graph;
			if (otherHovered) {
				int ox = this.graph ? x : x + this.split;
				int ow = this.graph ? this.split : w - this.split;
				CFHGuiDraw.pixelRound(g, ox + 2, y + 2, ow - 4, h - 4, Colors.scaleAlpha(p.surfaceHover(), a * 0.6F), inner);
			}

			float s = Math.max(0.0F, Math.min(1.0F, this.slide.get()));
			int mx = x + Math.round((float)this.split * s);
			int mw = Math.round((float)this.split + (float)(w - this.split - this.split) * s);
			CFHGuiDraw.pixelRound(g, mx + 2, y + 2, mw - 4, h - 4, Colors.scaleAlpha(p.surfaceActive(), a), inner);
			CFHGuiDraw.outline(g, mx + 2, y + 2, mw - 4, h - 4, Colors.scaleAlpha(p.borderStrong(), a), inner);
			CFHGuiDraw.rect(g, mx + 2 + inner, y + h - 3, mw - 4 - inner * 2, 1, Colors.scaleAlpha(p.accent(), a));

			for (int i = 0; i < 2; i++) {
				boolean isGraph = i == 1;
				int fg = isGraph == this.graph ? p.text() : (otherHovered ? p.text() : p.textMuted());
				this.half(g, isGraph ? x + this.split : x, y, isGraph ? w - this.split : this.split, h, isGraph, Colors.scaleAlpha(fg, a), false);
			}

			CFHGuiDraw.focusRing(g, x, y, w, h, this.focus() * a);
		}
	}

	protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		float a = this.effectiveAlpha();
		if (!(a <= 0.004F)) {
			int x = this.getX();
			int y = this.getY() + this.enterOffset();
			int w = this.getWidth();
			int h = this.getHeight();
			boolean hovered = this.isHovered();
			this.say(mouseX);

			for (int i = 0; i < 2; i++) {
				boolean isGraph = i == 1;
				int sx = isGraph ? x + this.split : x;
				int sw = isGraph ? w - this.split : this.split;
				boolean marked = isGraph == this.graph;
				float lift = marked ? 1.0F : (hovered && this.overGraph((double)mouseX) == isGraph ? 0.6F : 0.0F);
				CFHGuiDraw.vanillaButton(g, sx, y, sw, h, lift, true, a);
				if (marked) {
					g.fill(sx + 1, y + 1, sx + sw - 1, y + h - 1, Colors.scaleAlpha(1073741824, a));
				}

				this.half(g, sx, y, sw, h, isGraph, Colors.scaleAlpha(marked ? -96 : -2039584, a), true);
			}

			if (this.focus() > 0.5F) {
				CFHGuiDraw.outline(g, this.graph ? x + this.split : x, y, this.graph ? w - this.split : this.split, h, Colors.scaleAlpha(-1, a), 0);
			}
		}
	}

	private void half(GuiGraphics g, int x, int y, int w, int h, boolean isGraph, int fg, boolean shadow) {
		Component name = name(isGraph);
		int content = this.words ? 14 + CFHGuiDraw.width(name) : 11;
		int left = x + (w - content) / 2;
		TreePaint.pixels(g, isGraph ? GRAPH : OUTLINE, left, y + (h - 10) / 2, fg);
		if (this.words) {
			g.drawString(CFHGuiDraw.font(), name, left + 11 + 3, CFHGuiDraw.textY(y, h), fg, shadow);
		}
	}

	protected void updateWidgetNarration(NarrationElementOutput out) {
		out.add(
			NarratedElementType.TITLE,
			Component.translatable("gui.narrate.button", new Object[]{Component.empty().append(this.getMessage()).append(": ").append(name(this.graph))})
		);
		out.add(NarratedElementType.USAGE, Component.translatable(this.graph ? "cfhgui_crafting.planner.view.outline" : "cfhgui_crafting.planner.view.graph"));
	}
}
