package com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiCard;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class PaintedCard extends CFHGuiCard {
	private final PaintedCard.Painter painter;
	private boolean bare;
	private boolean seen;
	private boolean entering;
	private long growStartMs;
	@Nullable
	private Component narration;

	public PaintedCard(int width, int height, PaintedCard.Painter painter) {
		super(0, 0, width, height);
		this.painter = painter;
	}

	public PaintedCard bare() {
		this.bare = true;
		this.flat();
		return this;
	}

	public PaintedCard narrate(Component text) {
		this.narration = text;
		return this;
	}

	public PaintedCard flat() {
		super.flat();
		return this;
	}

	public PaintedCard onClick(Runnable r) {
		super.onClick(r);
		return this;
	}

	public PaintedCard onRightClick(Runnable r) {
		super.onRightClick(r);
		return this;
	}

	protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		if (!this.bare) {
			super.renderWidget(g, mouseX, mouseY, partialTick);
		} else {
			float keep = this.alpha;
			this.alpha = 0.0F;
			super.renderWidget(g, mouseX, mouseY, partialTick);
			this.alpha = keep;
			float a = keep * this.enterProgress();
			if (!(a <= 0.004F)) {
				int dy = Math.round((1.0F - this.enterProgress()) * 6.0F);
				this.paintInto(g, this.getX(), this.getY() + dy, this.getWidth(), this.getHeight(), a, mouseX, mouseY);

				for (AbstractWidget w : this.widgets()) {
					if (w.visible) {
						w.setAlpha(a);
						w.render(g, mouseX, mouseY, partialTick);
					}
				}
			}
		}
	}

	protected void renderContent(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, float partialTick) {
		if (!this.bare) {
			this.paintInto(g, x, y, w, h, this.alpha * this.enterProgress(), mouseX, mouseY);
		}
	}

	private void paintInto(GuiGraphics g, int x, int y, int w, int h, float a, int mouseX, int mouseY) {
		if (!this.seen) {
			this.seen = true;
			this.entering = this.enterProgress() < 0.999F;
		}

		float grow = 1.0F;
		if (this.entering) {
			if (this.growStartMs == 0L && a > 0.02F) {
				this.growStartMs = Clock.nowMs();
			}

			float motion = Theme.current().motion();
			if (motion <= 0.0F) {
				grow = 1.0F;
			} else if (this.growStartMs == 0L) {
				grow = 0.0F;
			} else {
				grow = Ease.OUT_CUBIC.apply(Math.min(1.0F, (float)(Clock.nowMs() - this.growStartMs) / (620.0F * motion)));
			}

			if (grow >= 1.0F) {
				this.entering = false;
			}
		}

		this.painter.paint(g, new PaintedCard.Frame(x, y, w, h, a, this.hover(), grow, mouseX, mouseY, this));
	}

	protected void updateWidgetNarration(NarrationElementOutput out) {
		if (this.narration != null) {
			out.add(NarratedElementType.TITLE, this.narration);
		}
	}

	public static record Frame(int x, int y, int w, int h, float alpha, float hover, float grow, int mouseX, int mouseY, PaintedCard card) {
		public boolean over(int rx, int ry, int rw, int rh) {
			return this.mouseX >= this.x + rx && this.mouseX < this.x + rx + rw && this.mouseY >= this.y + ry && this.mouseY < this.y + ry + rh;
		}

		public void tip(int rx, int ry, int rw, int rh, List<Component> lines) {
			if (this.alpha > 0.9F && this.over(rx, ry, rw, rh)) {
				CFHGuiTooltips.request(lines, this.card);
			}
		}

		public void tip(int rx, int ry, int rw, int rh, Component line) {
			this.tip(rx, ry, rw, rh, List.of(line));
		}
	}

	@FunctionalInterface
	public interface Painter {
		void paint(GuiGraphics var1, PaintedCard.Frame var2);
	}
}
