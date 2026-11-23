package com.onehumanawa.cfhcore.content.gui.quests.client.view;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class SubtreePreview {
	public static final int PAD = 8;
	public static final int HEAD = 24;
	public static final int FOOT = 13;
	public static final int GAP = 10;
	private static final int MIN_W = 132;
	private static final int PLATE_MS = 230;
	private static final int FIRST_MS = 130;
	private static final int STEP_MS = 75;
	private static final int RISE_MS = 240;
	private static final int FOLD_MS = 170;
	public final NodeView owner;
	public final Chapter chapter;
	public final GraphModel graph;
	public final Component title;
	public final int done;
	public final int total;
	private final Anim time = new Anim(0.0F, 600, Ease.LINEAR);
	private final int totalMs;
	private boolean closing;
	private int x;
	private int y;
	private int w;
	private int h;
	private float ownerX;
	private float ownerY;
	private float ownerR;
	private int side;
	private float scale = 0.5F;

	public static int preferredSide(float ownerX, int roomLeft, int roomRight) {
		return (float)roomRight - ownerX >= ownerX - (float)roomLeft ? 1 : -1;
	}

	public SubtreePreview(NodeView owner, Chapter subtree) {
		this.owner = owner;
		this.chapter = subtree;
		this.graph = GraphModel.of(subtree);
		this.title = QuestText.title(subtree);
		int d = 0;
		int t = 0;

		for (NodeView n : this.graph.all) {
			if (!n.quest.optional) {
				t++;
				if (n.done()) {
					d++;
				}
			}
		}

		this.done = d;
		this.total = t;
		this.totalMs = 360 + Math.min(this.graph.deepest, 9) * 75 + 240;
		this.time.duration(this.totalMs);
		this.time.set(1.0F);
	}

	public void close() {
		if (!this.closing) {
			this.closing = true;
			this.time.set(0.0F, 170);
		}
	}

	public boolean gone() {
		return this.closing && this.time.get() <= 0.005F;
	}

	public boolean closing() {
		return this.closing;
	}

	public float open() {
		return this.time.get();
	}

	public Rect box() {
		return new Rect(this.x, this.y, this.w, this.h);
	}

	public int side() {
		return this.side;
	}

	public float ownerX() {
		return this.ownerX;
	}

	public float ownerY() {
		return this.ownerY;
	}

	public float ownerRadius() {
		return this.ownerR;
	}

	private float ms() {
		return this.time.get() * (float)this.totalMs;
	}

	public float plate() {
		return Ease.OUT_CUBIC.apply(Mth.clamp(this.ms() / 230.0F, 0.0F, 1.0F));
	}

	private float turn(int depth) {
		return 291.0F + (float)(Math.min(depth, 9) * 75);
	}

	public float risen(int depth) {
		return Mth.clamp((this.ms() - this.turn(depth)) / 240.0F, 0.0F, 1.0F);
	}

	public float drawn(int depth) {
		return Mth.clamp((this.ms() - this.turn(depth) + 75.0F) / 75.0F, 0.0F, 1.0F);
	}

	public float words() {
		return Mth.clamp((this.ms() - 115.0F) / 160.0F, 0.0F, 1.0F);
	}

	public void render(GuiGraphics g, QuestPainter painter, QuestPainter.View view, float ownerX, float ownerY, float ownerRadius, int roomLeft, int roomRight) {
		if (!this.graph.isEmpty()) {
			Rect area = view.area();
			this.ownerX = ownerX;
			this.ownerY = ownerY;
			this.ownerR = ownerRadius;
			if (this.side == 0) {
				this.side = preferredSide(ownerX, roomLeft, roomRight);
			}

			float room = this.side > 0 ? (float)(roomRight - 4) - (ownerX + ownerRadius + 10.0F) : ownerX - ownerRadius - 10.0F - (float)(roomLeft + 4);
			float gw = this.graph.questWidth() + 1.2F;
			float gh = this.graph.questHeight() + 1.2F;
			float maxW = Mth.clamp(room, 132.0F, Math.max(132.0F, (float)(roomRight - roomLeft) * 0.55F));
			float maxH = (float)area.h() * 0.7F;
			this.scale = Mth.clamp(Math.min((maxW - 16.0F) / (gw * painter.unit()), (maxH - 16.0F - 24.0F - 13.0F) / (gh * painter.unit())), 0.2F, 0.8F);
			float unit = painter.unit() * this.scale;
			this.w = Math.max(Math.round(gw * unit) + 16, 132);
			this.h = Math.round(gh * unit) + 16 + 24 + 13;
			this.x = this.side > 0 ? Math.round(ownerX + ownerRadius + 10.0F) : Math.round(ownerX - ownerRadius - 10.0F - (float)this.w);
			this.x = Mth.clamp(this.x, roomLeft + 4, Math.max(roomLeft + 4, roomRight - 4 - this.w));
			this.y = Mth.clamp(Math.round(ownerY - (float)this.h * 0.42F), area.y() + 4, Math.max(area.y() + 4, area.bottom() - 4 - this.h));
			float p = this.plate();
			if (!(p <= 0.01F)) {
				float ax = this.side > 0 ? (float)this.x : (float)(this.x + this.w);
				float ay = Mth.clamp(ownerY, (float)this.y + 6.0F, (float)(this.y + this.h) - 6.0F);
				int pw = Math.max(4, Math.round((float)this.w * p));
				int ph = Math.max(4, Math.round((float)this.h * p));
				int px = this.side > 0 ? Math.round(ax) : Math.round(ax - (float)pw);
				int py = Math.round(Mth.lerp(p, ay - 2.0F, (float)this.y));
				QuestPainter.View inner = view.of(this.chapter);
				painter.previewPlate(g, inner, this, px, py, pw, ph, p);
				if (!(p < 0.55F)) {
					g.enableScissor(px + 1, py + 1, px + pw - 1, py + ph - 1);

					try {
						float cx = (this.graph.questMinX + this.graph.questMaxX) / 2.0F;
						float cy = (this.graph.questMinY + this.graph.questMaxY) / 2.0F;
						float ox = (float)this.x + (float)this.w / 2.0F;
						float oy = (float)(this.y + 24) + (float)(this.h - 24 - 13) / 2.0F;
						float r = painter.radius();

						for (LinkView l : this.graph.links) {
							float grown = this.drawn(l.to().depth);
							if (!(grown <= 0.01F)) {
								float x1 = ox + (l.from().x - cx) * unit;
								float y1 = oy + (l.from().y - cy) * unit;
								float x2 = ox + (l.to().x - cx) * unit;
								float y2 = oy + (l.to().y - cy) * unit;
								float len = Mth.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
								if (!(len < 1.0F)) {
									float a = Math.min(0.45F, r * this.scale * l.from().size / len);
									float b = Math.min(0.45F, r * this.scale * l.to().size / len);
									painter.link(
										g,
										inner,
										l,
										Mth.lerp(a, x1, x2),
										Mth.lerp(a, y1, y2),
										Mth.lerp(1.0F - b, x1, x2),
										Mth.lerp(1.0F - b, y1, y2),
										this.scale,
										Math.min(1.0F, grown * 2.0F),
										grown
									);
								}
							}
						}

						painter.beginNodes(g, inner);

						for (NodeView n : this.graph.nodes) {
							float in = this.risen(n.depth);
							if (!(in <= 0.01F)) {
								float pop = this.closing ? in : Ease.OUT_BACK.apply(in);
								painter.node(g, inner, n, ox + (n.x - cx) * unit, oy + (n.y - cy) * unit, this.scale * n.size * pop, Math.min(1.0F, in * 1.8F));
							}
						}

						painter.endNodes(g, inner);
					} finally {
						g.disableScissor();
					}
				}
			}
		}
	}
}
