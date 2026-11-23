package com.onehumanawa.cfhcore.content.gui.quests.client.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.CanvasDraw;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestIcons;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.logic.Rules;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import net.minecraft.client.gui.GuiGraphics;

public final class UiBits {
	public static int statusColor(Rules.Status status, int accent) {
		Theme t = Theme.current();
		Palette p = t.palette();
		boolean van = t.isVanilla();

		return switch (status) {
			case COMPLETE -> van ? -11141291 : p.success();
			case CLAIMABLE -> van ? -22016 : p.warning();
			case LOCKED, HIDDEN -> van ? -6250336 : p.textDim();
			default -> van ? -1 : accent;
		};
	}

	public static void ring(GuiGraphics g, int cx, int cy, int r, float fraction, int track, int lit, float alpha) {
		g.pose().pushPose();
		g.pose().translate((float)cx, (float)cy, 0.0F);

		for (int k = 0; k < 2; k++) {
			CanvasDraw.rim(g, Quest.Shape.CIRCLE, r - k, 1.0F, Colors.scaleAlpha(track, alpha));
			CanvasDraw.rim(g, Quest.Shape.CIRCLE, r - k, fraction, Colors.scaleAlpha(lit, alpha));
		}

		g.pose().popPose();
	}

	public static void bar(GuiGraphics g, int x, int y, int w, int h, float fraction, int track, int lit, float alpha) {
		if (w > 0) {
			g.fill(x, y, x + w, y + h, Colors.scaleAlpha(track, alpha));
			int fw = fraction <= 0.0F ? 0 : Math.max(1, Math.min(w, Math.round((float)w * fraction)));
			if (fw > 0) {
				g.fill(x, y, x + fw, y + h, Colors.scaleAlpha(lit, alpha));
			}
		}
	}

	public static void medallion(GuiGraphics g, BookIndex.Entry entry, Rules.Status status, int accent, int cx, int cy, float size, float alpha) {
		Theme t = Theme.current();
		Palette p = t.palette();
		boolean van = t.isVanilla();
		Quest q = entry.quest();
		boolean locked = status == Rules.Status.LOCKED || status == Rules.Status.HIDDEN;
		boolean secret = status == Rules.Status.LOCKED && q.visibility == Quest.Visibility.SECRET;
		Quest.Shape shape = q.isAdvancement() ? Quest.Shape.ROUNDED : entry.shape();
		int rim = statusColor(status, accent);
		int fill = van
			? (locked ? -12829636 : (status == Rules.Status.COMPLETE ? -11630002 : (status == Rules.Status.CLAIMABLE ? -5207506 : -9474193)))
			: (
				locked
					? p.bg2()
					: (
						status == Rules.Status.COMPLETE
							? Colors.mix(p.surface(), p.success(), 0.18F)
							: (status == Rules.Status.CLAIMABLE ? Colors.mix(p.surface(), p.warning(), 0.22F) : p.surfaceHover())
					)
			);
		g.pose().pushPose();
		g.pose().translate((float)cx, (float)cy, 0.0F);
		g.pose().scale(size, size, 1.0F);
		g.pose().pushPose();
		g.pose().translate(1.0F, 2.0F, 0.0F);
		CanvasDraw.shape(g, shape, 11, Colors.scaleAlpha(van ? 1879048192 : p.shadow(), alpha));
		g.pose().popPose();
		if (q.isAdvancement()) {
			CanvasDraw.advancementFrame(g, q.frame, status.done(), locked ? 0.72F : 1.0F, alpha);
		} else {
			CanvasDraw.shape(g, shape, 11, Colors.scaleAlpha(fill, alpha), Colors.scaleAlpha(van ? -16777216 : (locked ? p.borderStrong() : rim), alpha));
			if (!van) {
				CanvasDraw.shapeTop(g, shape, 10, 9, Colors.scaleAlpha(218103807, alpha));
			}
		}

		g.pose().popPose();
		if (alpha > 0.5F) {
			int ink = q.isAdvancement() ? -13949406 : (van ? -1 : (locked ? p.textDim() : p.text()));
			if (secret) {
				CFHGuiDraw.textCentered(g, "?", cx + 1, cy - 4, q.isAdvancement() ? ink : (van ? -6250336 : p.textDim()));
			} else {
				QuestIcons.of(entry).draw(g, (float)cx, (float)cy, 16.0F * size, ink);
			}
		}

		if (locked && !secret) {
			g.pose().pushPose();
			g.pose().translate((float)cx, (float)cy, 200.0F);
			g.pose().scale(size, size, 1.0F);
			if (q.isAdvancement()) {
				CanvasDraw.shape(g, Quest.Shape.ROUNDED, 8, Colors.scaleAlpha(1879048192, alpha));
			} else {
				CanvasDraw.shape(g, shape, 10, Colors.scaleAlpha(Colors.withAlpha(fill, 158), alpha));
			}

			g.pose().popPose();
		}
	}

	private UiBits() {
	}
}
