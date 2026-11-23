package com.onehumanawa.cfhcore.content.gui.quests.client.view;

import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public final class TreeThumb {
	public static void draw(GuiGraphics g, QuestPainter painter, GraphModel graph, QuestPainter.View view, int x, int y, int w, int h, float maxScale, float alpha) {
		if (!graph.isEmpty() && w > 8 && h > 8) {
			float gw = graph.questWidth() + 1.2F;
			float gh = graph.questHeight() + 1.2F;
			float scale = Mth.clamp(Math.min((float)w / (gw * painter.unit()), (float)h / (gh * painter.unit())), 0.08F, maxScale);
			float unit = painter.unit() * scale;
			float cx = (graph.questMinX + graph.questMaxX) / 2.0F;
			float cy = (graph.questMinY + graph.questMaxY) / 2.0F;
			float ox = (float)x + (float)w / 2.0F;
			float oy = (float)y + (float)h / 2.0F;
			float r = painter.radius();
			g.enableScissor(x, y, x + w, y + h);

			try {
				for (LinkView l : graph.links) {
					float x1 = ox + (l.from().x - cx) * unit;
					float y1 = oy + (l.from().y - cy) * unit;
					float x2 = ox + (l.to().x - cx) * unit;
					float y2 = oy + (l.to().y - cy) * unit;
					float len = Mth.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
					if (!(len < 1.0F)) {
						float a = Math.min(0.45F, r * scale * l.from().size / len);
						float b = Math.min(0.45F, r * scale * l.to().size / len);
						painter.link(g, view, l, Mth.lerp(a, x1, x2), Mth.lerp(a, y1, y2), Mth.lerp(1.0F - b, x1, x2), Mth.lerp(1.0F - b, y1, y2), scale, alpha, 1.0F);
					}
				}

				painter.beginNodes(g, view);

				for (NodeView n : graph.nodes) {
					painter.node(g, view, n, ox + (n.x - cx) * unit, oy + (n.y - cy) * unit, scale * n.size, alpha);
				}

				painter.endNodes(g, view);
			} finally {
				g.disableScissor();
			}
		}
	}

	public static QuestPainter.View view(Chapter chapter, Rect box, float seconds) {
		return new QuestPainter.View(box, 1.0F, 0.0F, 0.0F, seconds, -1, -1, chapter, QuestPainter.accentOf(chapter));
	}

	private TreeThumb() {
	}
}
