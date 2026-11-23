package com.onehumanawa.cfhcore.content.gui.quests.client.vanilla;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.quests.QuestsConfig;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.Backdrops;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.BookStats;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.CanvasDraw;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.GraphModel;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.Inlay;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.LinkView;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.NodeView;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestIcons;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestPainter;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.CFHGuiQuestPainter;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.SubtreePreview;
import com.onehumanawa.cfhcore.content.gui.quests.logic.Rules;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestState;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

final class VanillaQuestPainter implements QuestPainter {
	private static final int R = 13;
	private static final float DOT_BELOW = 3.0F;
	private static final float PICTURE_FROM = 5.5F;
	private static final float NAMES_FROM = 1.3F;
	private static final float CFH_GROUND = 0.3F;
	private static final int GOLD = -5603825;
	private static final int SILVER = -3750202;
	private static final int GOLD_LIGHT = -2383341;
	private final Map<String, ResourceLocation> textures = new HashMap<>();
	private int[] lines = new int[768];
	private int lineCount;
	private int[] flows = new int[384];
	private int flowCount;
	private final int[] px = new int[4];
	private final int[] py = new int[4];
	private boolean deferring;
	private NodeView[] putOff = new NodeView[64];
	private QuestPainter.View[] putOffView = new QuestPainter.View[64];
	private float[] putOffAt = new float[256];
	private int putOffCount;
	private boolean main;
	private int hot = -1;
	private float settled;
	private static final String[] BANG = new String[]{".X.", ".X.", ".X.", ".X.", "...", ".X."};
	private static final String[] LOCK = new String[]{".XXX.", "X...X", "XXXXX", "XX.XX", "XXXXX"};
	private static final String[] PIN = new String[]{".XXX.", ".XXX.", "XXXXX", "..X..", "..X.."};
	private static final String[] LOOP = new String[]{".XXX.X", "X...XX", "X..XXX", "X.....", ".XXXX."};
	private static final String[] PLUS = new String[]{"..X..", "..X..", "XXXXX", "..X..", "..X.."};
	private static final String[] MINUS = new String[]{".....", ".....", "XXXXX", ".....", "....."};
	private static final int BOX_MAX = 200;
	private static final int BOX_MIN = 116;
	private static final int TEXT_MAX = 186;
	private final List<VanillaQuestPainter.Row> boxRows = new ArrayList<>();
	@Nullable
	private NodeView boxNode;
	private int boxRevision = -1;
	private int boxWidth;
	private int boxBody;
	private boolean boxVanilla;
	private boolean boxUnfolded;
	private FormattedCharSequence boxTitle = FormattedCharSequence.EMPTY;
	@Nullable
	private FormattedCharSequence boxCount;
	private int clearRight = Integer.MAX_VALUE;
	private static final int[] SPLIT_OFFSETS = new int[]{0, 10, -10, 25, -25};

	@Override
	public float unit() {
		return 28.0F;
	}

	@Override
	public float radius() {
		return 13.0F;
	}

	private ResourceLocation texture(@Nullable Chapter chapter) {
		String backdrop = chapter != null && chapter.backdrop != null ? chapter.backdrop : "";
		return this.textures.computeIfAbsent(backdrop, Backdrops::texture);
	}

	private static int cfhGround() {
		return Colors.mix(Theme.current().palette().bg(), -16777216, 0.15F) | 0xFF000000;
	}

	private void ground(GuiGraphics g, @Nullable Chapter chapter, Rect a, float ox, float oy, float scale, float alpha) {
		if (!(alpha <= 0.004F) && a.w() > 0 && a.h() > 0) {
			Theme t = Theme.current();
			float tile = 16.0F * scale;
			float u = (((float)a.x() - ox) % tile + tile) % tile;
			float v = (((float)a.y() - oy) % tile + tile) % tile;
			float light = t.isVanilla() ? 1.0F : 0.3F;
			g.setColor(light, light, light, alpha);
			RenderSystem.enableBlend();
			g.pose().pushPose();
			g.pose().translate((float)Math.round((float)a.x() - u), (float)Math.round((float)a.y() - v), 0.0F);
			g.pose().scale(scale, scale, 1.0F);
			g.blit(
				this.texture(chapter),
				0,
				0,
				0.0F,
				0.0F,
				(int)Math.ceil((double)(((float)a.w() + tile + 1.0F) / scale)),
				(int)Math.ceil((double)(((float)a.h() + tile + 1.0F) / scale)),
				16,
				16
			);
			g.pose().popPose();
			g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.disableBlend();
			if (!t.isVanilla()) {
				g.fill(a.x(), a.y(), a.right(), a.bottom(), Colors.scaleAlpha(Colors.withAlpha(t.palette().bg(), 88), alpha));
			}
		}
	}

	private void ground(GuiGraphics g, QuestPainter.View view, float alpha) {
		Rect a = view.area();
		float unit = this.unit() * view.zoom();
		this.ground(
			g,
			view.chapter(),
			a,
			(float)a.x() + (float)a.w() / 2.0F - view.camX() * unit,
			(float)a.y() + (float)a.h() / 2.0F - view.camY() * unit,
			Mth.clamp(view.zoom(), 0.5F, 2.0F),
			alpha
		);
	}

	@Override
	public void backdrop(GuiGraphics g, QuestPainter.View view) {
		this.lineCount = 0;
		this.flowCount = 0;
		this.main = true;
		this.ground(g, view, 1.0F);
	}

	@Override
	public void veil(GuiGraphics g, QuestPainter.View view, float alpha) {
		this.ground(g, view, alpha);
	}

	@Override
	public void decoration(GuiGraphics g, QuestPainter.View view, Chapter.Decoration d, float sx, float sy, float scale) {
		Theme t = Theme.current();
		boolean van = t.isVanilla();
		if ("image".equals(d.type)) {
			ResourceLocation id = ResourceLocation.tryParse(d.image);
			if (id != null) {
				float w = d.w * this.unit() * scale;
				float h = d.h * this.unit() * scale;
				g.pose().pushPose();
				g.pose().translate(sx - w / 2.0F, sy - h / 2.0F, 0.0F);
				g.pose().scale(w / 16.0F, h / 16.0F, 1.0F);
				g.flush();
				CFHGuiDraw.blit(g, id, 0, 0, 16, 16, 0.0F, 0.0F, 16, 16, 16, 16, Mth.clamp(d.alpha, 0.0F, 1.0F));
				g.pose().popPose();
			}
		} else if (!d.text.isBlank()) {
			if (!(scale < (d.heading ? 0.22F : 0.5F))) {
				float s = Math.max(d.heading ? 0.75F : 0.6F, Math.min(1.5F, scale)) * d.scale;
				Font font = CFHGuiDraw.font();
				Component text = d.heading && !van ? Fonts.heading(QuestText.text(d.text)) : QuestText.text(d.text);
				int color = d.color.isEmpty() ? (van ? -1 : t.palette().textMuted()) : Colors.fromHex(d.color, t.palette().textMuted());
				g.pose().pushPose();
				g.pose().translate((float)Math.round(sx), (float)Math.round(sy), 0.0F);
				g.pose().scale(s, s, 1.0F);
				g.drawString(font, text, -font.width(text) / 2, -4, Colors.scaleAlpha(color | 0xFF000000, Mth.clamp(d.alpha, 0.0F, 1.0F)), van);
				g.pose().popPose();
			}
		}
	}

	@Override
	public void link(GuiGraphics g, QuestPainter.View view, LinkView link, float x1, float y1, float x2, float y2, float scale, float alpha, float grown) {
		if (!(alpha <= 0.01F) && !(grown <= 0.01F)) {
			NodeView from = link.from();
			NodeView to = link.to();
			float ra = 13.0F * scale * from.size;
			float rb = 13.0F * scale * to.size;
			float dx = x2 - x1;
			float dy = y2 - y1;
			float cut = Mth.sqrt(dx * dx + dy * dy);
			if (!(cut < 0.5F)) {
				float len = between(cut, ra, rb);
				float ux = dx / cut;
				float uy = dy / cut;
				float backA = Math.min(0.45F * len, ra);
				float backB = Math.min(0.45F * len, rb);
				int ax = Math.round(x1 - ux * backA);
				int ay = Math.round(y1 - uy * backA);
				int bx = Math.round(x2 + ux * backB);
				int by = Math.round(y2 + uy * backB);
				int ha = Math.max(1, Math.round(ra));
				int hb = Math.max(1, Math.round(rb));
				if (bx - hb > ax + ha) {
					int first = ax + ha;
					int last = bx - hb - 1;
					int mid = (first + last + 1) / 2;
					this.path(first, ay, mid, ay, mid, by, last, by);
				} else if (bx + hb < ax - ha) {
					int first = ax - ha - 1;
					int last = bx + hb;
					int mid = (first + last) / 2;
					this.path(first, ay, mid, ay, mid, by, last, by);
				} else if (by - hb > ay + ha) {
					int first = ay + ha;
					int last = by - hb - 1;
					int mid = (first + last + 1) / 2;
					this.path(ax, first, ax, mid, bx, mid, bx, last);
				} else {
					if (by + hb >= ay - ha) {
						return;
					}

					int first = ay - ha - 1;
					int last = by + hb;
					int mid = (first + last) / 2;
					this.path(ax, first, ax, mid, bx, mid, bx, last);
				}

				Theme t = Theme.current();
				boolean van = t.isVanilla();
				float lit = link.lit();
				float there = Mth.clamp(alpha / (0.18F + 0.82F * lit), 0.0F, 1.0F);
				float hover = link.hover();
				int width = scale >= 1.75F ? 2 : 1;
				int ink;
				if (van) {
					ink = !to.quest.looseDeps && !link.satisfied() ? Colors.lerp(-6645094, -1, hover) : -1;
					ink = Colors.lerp(Colors.mix(ink, -16777216, 0.6F), ink, lit);
				} else {
					Palette p = t.palette();
					int accent = view.accent();
					int bg = cfhGround();
					ink = link.complete()
						? Colors.mix(p.success(), bg, 0.3F)
						: (
							link.leading()
								? Colors.mix(accent, bg, 0.15F)
								: (!link.satisfied() && !to.quest.looseDeps ? Colors.mix(p.borderStrong(), bg, 0.1F) : Colors.mix(accent, bg, 0.45F))
						);
					if (hover > 0.01F) {
						ink = Colors.lerp(ink, link.complete() ? p.success() : (link.satisfied() ? accent : p.textMuted()), hover * 0.85F);
					}

					ink = Colors.lerp(Colors.mix(ink, bg, 0.7F), ink, lit);
				}

				int total = Math.abs(this.px[1] - this.px[0])
					+ Math.abs(this.py[1] - this.py[0])
					+ Math.abs(this.px[2] - this.px[1])
					+ Math.abs(this.py[2] - this.py[1])
					+ Math.abs(this.px[3] - this.px[2])
					+ Math.abs(this.py[3] - this.py[2])
					+ 1;
				int left = grown >= 0.999F ? total : Math.max(1, Math.round((float)total * grown));
				int black = Colors.scaleAlpha(van ? -16777216 : Colors.withAlpha(cfhGround(), 192), there);
				int white = Colors.scaleAlpha(ink, there);

				for (int i = 0; i < 3 && left > 0; i++) {
					int sx = this.px[i];
					int sy = this.py[i];
					int ex = this.px[i + 1];
					int ey = this.py[i + 1];
					int run = Math.abs(ex - sx) + Math.abs(ey - sy) + (i == 2 ? 1 : 0);
					if (run > left) {
						ex = sx + Integer.signum(ex - sx) * Math.max(0, left - 1);
						ey = sy + Integer.signum(ey - sy) * Math.max(0, left - 1);
					}

					left -= run;
					cells(g, sx, sy, ex, ey, width, 1, black);
					this.lines = queue(this.lines, this.lineCount++, sx, sy, ex, ey, width, white);
				}

				if (left >= 0 && grown >= 0.999F && link.leading() && !to.quest.looseDeps && scale >= 0.5F && lit > 0.5F && QuestsConfig.get().flowingLines) {
					this.flow(view.seconds() * 22.0F + (float)link.seed() * 5.0F, width, Colors.scaleAlpha(van ? -171 : Colors.brighten(view.accent(), 0.45F), there), total);
				}
			}
		}
	}

	private static float between(float cut, float ra, float rb) {
		float len = cut + ra + rb;
		if (ra <= 0.45F * len && rb <= 0.45F * len) {
			return len;
		} else {
			len = (cut + rb) / 0.55F;
			if (ra >= 0.45F * len && rb <= 0.45F * len) {
				return len;
			} else {
				len = (cut + ra) / 0.55F;
				return rb >= 0.45F * len && ra <= 0.45F * len ? len : cut * 10.0F;
			}
		}
	}

	private void path(int x0, int y0, int x1, int y1, int x2, int y2, int x3, int y3) {
		this.px[0] = x0;
		this.py[0] = y0;
		this.px[1] = x1;
		this.py[1] = y1;
		this.px[2] = x2;
		this.py[2] = y2;
		this.px[3] = x3;
		this.py[3] = y3;
	}

	private static void cells(GuiGraphics g, int xa, int ya, int xb, int yb, int width, int grow, int color) {
		if (color >>> 24 != 0) {
			int lo = (width - 1) / 2 + grow;
			int hi = width / 2 + 1 + grow;
			g.fill(Math.min(xa, xb) - lo, Math.min(ya, yb) - lo, Math.max(xa, xb) + hi, Math.max(ya, yb) + hi, color);
		}
	}

	private static int[] queue(int[] into, int at, int xa, int ya, int xb, int yb, int width, int color) {
		if (at * 6 + 6 > into.length) {
			into = Arrays.copyOf(into, into.length * 2);
		}

		int i = at * 6;
		into[i] = xa;
		into[i + 1] = ya;
		into[i + 2] = xb;
		into[i + 3] = yb;
		into[i + 4] = width;
		into[i + 5] = color;
		return into;
	}

	private void flow(float phase, int width, int color, int total) {
		int dash = 4;
		int every = 16;

		for (int head = Math.floorMod((int)phase, 16) - 4; head < total; head += 16) {
			int from = Math.max(0, head);
			int to = Math.min(total - 1, head + 4 - 1);
			if (to >= from) {
				int walked = 0;

				for (int i = 0; i < 3; i++) {
					int sx = this.px[i];
					int sy = this.py[i];
					int ex = this.px[i + 1];
					int ey = this.py[i + 1];
					int run = Math.abs(ex - sx) + Math.abs(ey - sy) + (i == 2 ? 1 : 0);
					int a = Math.max(from, walked);
					int b = Math.min(to, walked + run - 1);
					if (b >= a) {
						int stepX = Integer.signum(ex - sx);
						int stepY = Integer.signum(ey - sy);
						this.flows = queue(
							this.flows, this.flowCount++, sx + stepX * (a - walked), sy + stepY * (a - walked), sx + stepX * (b - walked), sy + stepY * (b - walked), width, color
						);
					}

					walked += run;
				}
			}
		}
	}

	private void flushLines(GuiGraphics g) {
		for (int n = 0; n < this.lineCount; n++) {
			int i = n * 6;
			cells(g, this.lines[i], this.lines[i + 1], this.lines[i + 2], this.lines[i + 3], this.lines[i + 4], 0, this.lines[i + 5]);
		}

		for (int n = 0; n < this.flowCount; n++) {
			int i = n * 6;
			cells(g, this.flows[i], this.flows[i + 1], this.flows[i + 2], this.flows[i + 3], this.flows[i + 4], 0, this.flows[i + 5]);
		}

		this.lineCount = 0;
		this.flowCount = 0;
	}

	@Override
	public void beginNodes(GuiGraphics g, QuestPainter.View view) {
		this.flushLines(g);
		this.deferring = true;
		this.putOffCount = 0;
		this.hot = -1;
		this.settled = 0.0F;
	}

	@Override
	public void node(GuiGraphics g, QuestPainter.View view, NodeView n, float sx, float sy, float scale0, float alpha) {
		if (!(alpha <= 0.01F) && !(scale0 <= 0.01F)) {
			float half = 13.0F * scale0;
			if (half < 5.5F) {
				this.small(g, view, n, sx, sy, half, alpha);
			} else if (!this.deferring) {
				this.flushLines(g);
				this.under(g, view, n, sx, sy, scale0, alpha);
				this.body(g, view, n, sx, sy, scale0, alpha);
				this.picture(g, view, n, sx, sy, scale0, alpha);
				this.over(g, view, n, sx, sy, scale0, alpha);
			} else {
				if (this.putOffCount == this.putOff.length) {
					this.putOff = Arrays.copyOf(this.putOff, this.putOffCount * 2);
					this.putOffView = Arrays.copyOf(this.putOffView, this.putOffCount * 2);
					this.putOffAt = Arrays.copyOf(this.putOffAt, this.putOffCount * 8);
				}

				if (this.main) {
					float h = n.hover.get();
					if (h > this.settled) {
						this.settled = h;
					}

					if (n.hover.target() >= 1.0F) {
						this.hot = this.putOffCount;
					}
				}

				this.putOff[this.putOffCount] = n;
				this.putOffView[this.putOffCount] = view;
				this.putOffAt[this.putOffCount * 4] = sx;
				this.putOffAt[this.putOffCount * 4 + 1] = sy;
				this.putOffAt[this.putOffCount * 4 + 2] = scale0;
				this.putOffAt[this.putOffCount * 4 + 3] = alpha;
				this.putOffCount++;
			}
		}
	}

	@Override
	public void endNodes(GuiGraphics g, QuestPainter.View view) {
		if (this.deferring) {
			this.deferring = false;
			boolean own = this.main;
			this.main = false;
			int count = this.putOffCount;
			float dark = own ? this.settled * (Theme.current().isVanilla() ? 0.3F : 0.34F) : 0.0F;
			int lifted = dark > 0.01F ? this.hot : -1;

			for (int i = 0; i < count; i++) {
				if (i != lifted) {
					this.under(g, this.putOffView[i], this.putOff[i], this.putOffAt[i * 4], this.putOffAt[i * 4 + 1], this.putOffAt[i * 4 + 2], this.putOffAt[i * 4 + 3]);
				}
			}

			for (int ix = 0; ix < count; ix++) {
				if (ix != lifted) {
					this.body(g, this.putOffView[ix], this.putOff[ix], this.putOffAt[ix * 4], this.putOffAt[ix * 4 + 1], this.putOffAt[ix * 4 + 2], this.putOffAt[ix * 4 + 3]);
				}
			}

			for (int ixx = 0; ixx < count; ixx++) {
				if (ixx != lifted) {
					this.picture(
						g, this.putOffView[ixx], this.putOff[ixx], this.putOffAt[ixx * 4], this.putOffAt[ixx * 4 + 1], this.putOffAt[ixx * 4 + 2], this.putOffAt[ixx * 4 + 3]
					);
				}
			}

			for (int ixxx = 0; ixxx < count; ixxx++) {
				if (ixxx != lifted) {
					this.over(
						g,
						this.putOffView[ixxx],
						this.putOff[ixxx],
						this.putOffAt[ixxx * 4],
						this.putOffAt[ixxx * 4 + 1],
						this.putOffAt[ixxx * 4 + 2],
						this.putOffAt[ixxx * 4 + 3]
					);
				}
			}

			if (dark > 0.01F) {
				CanvasDraw.layer(g);
				Rect a = view.area();
				g.fill(a.x(), a.y(), a.right(), a.bottom(), Colors.withAlpha(Theme.current().isVanilla() ? -16777216 : cfhGround(), Math.round(255.0F * dark)));
				if (lifted >= 0) {
					NodeView n = this.putOff[lifted];
					QuestPainter.View with = this.putOffView[lifted];
					float x = this.putOffAt[lifted * 4];
					float y = this.putOffAt[lifted * 4 + 1];
					float s = this.putOffAt[lifted * 4 + 2];
					float al = this.putOffAt[lifted * 4 + 3];
					this.under(g, with, n, x, y, s, al);
					this.body(g, with, n, x, y, s, al);
					this.picture(g, with, n, x, y, s, al);
					this.over(g, with, n, x, y, s, al);
				}
			}

			Arrays.fill(this.putOff, 0, count, null);
			Arrays.fill(this.putOffView, 0, count, null);
			this.putOffCount = 0;
			this.hot = -1;
		}
	}

	private static float presence(NodeView n, float alpha) {
		return Mth.clamp(alpha / (0.22F + 0.78F * n.lit.get()), 0.0F, 1.0F);
	}

	private static float light(NodeView n) {
		return (n.locked() ? 0.55F : 1.0F) * Mth.lerp(n.lit.get(), 0.42F, 1.0F);
	}

	private static boolean plain(NodeView n) {
		return n.chapter.readOnly && n.advancement();
	}

	private static int half(float scale0) {
		return Math.max(1, Math.round(13.0F * scale0));
	}

	private void small(GuiGraphics g, QuestPainter.View view, NodeView n, float sx, float sy, float half, float alpha) {
		boolean van = Theme.current().isVanilla();
		int cx = Math.round(sx);
		int cy = Math.round(sy);
		float there = presence(n, alpha);
		int tone = van ? (n.status == Rules.Status.CLAIMABLE ? -2383341 : (n.done() ? -5603825 : -3750202)) : WindowArt.cfhFrameTone(n.status, view.accent(), 0.0F);
		tone = Colors.scale(tone, light(n));
		if (half < 3.0F) {
			int r = half < 1.5F ? 0 : 1;
			g.fill(cx - r, cy - r, cx + r + 1, cy + r + 1, Colors.scaleAlpha(tone, there));
		} else {
			int r = Math.max(2, Math.round(half));
			if (n.selected.get() > 0.01F) {
				g.fill(cx - r - 1, cy - r - 1, cx + r + 1, cy + r + 1, Colors.scaleAlpha(van ? -1 : view.accent(), there * n.selected.get()));
			}

			g.fill(cx - r, cy - r, cx + r, cy + r, Colors.scaleAlpha(van ? -16777216 : Colors.mix(tone, -16777216, 0.6F), there));
			g.fill(cx - r + 1, cy - r + 1, cx + r - 1, cy + r - 1, Colors.scaleAlpha(tone, there));
		}
	}

	private void under(GuiGraphics g, QuestPainter.View view, NodeView n, float sx, float sy, float scale0, float alpha) {
		float sel = n.selected.get();
		boolean gate = n.gateway();
		if (gate || !(sel <= 0.01F)) {
			Theme t = Theme.current();
			boolean van = t.isVanilla();
			float there = presence(n, alpha) * Mth.lerp(n.lit.get(), 0.5F, 1.0F);
			int h = half(scale0);
			g.pose().pushPose();
			g.pose().translate((float)Math.round(sx), (float)Math.round(sy), 0.0F);
			g.pose().scale((float)h / 13.0F, (float)h / 13.0F, 1.0F);
			if (gate) {
				float unfolded = n.folds ? n.unfolded.get() : 0.0F;
				float phase = n.locked() ? 0.0F : view.seconds() * 7.0F * (1.0F - unfolded);
				int dash = unfolded > 0.5F ? 6 : 4;
				int ring = van ? (n.locked() ? -6250336 : -1) : (n.locked() ? t.palette().borderStrong() : Colors.brighten(view.accent(), 0.2F));
				if (van) {
					g.pose().pushPose();
					g.pose().translate(1.0F, 1.0F, 0.0F);
					CanvasDraw.rimDashes(g, Quest.Shape.SQUARE, 15, 6, dash, phase, Colors.scaleAlpha(-16777216, there));
					g.pose().popPose();
				}

				CanvasDraw.rimDashes(g, Quest.Shape.SQUARE, 15, 6, dash, phase, Colors.scaleAlpha(ring, there));
			}

			if (sel > 0.01F) {
				int d = 17 + Math.round(1.2F * Mth.sin(view.seconds() * 3.0F));
				if (van) {
					brackets(g, d, 1, Colors.scaleAlpha(-16777216, there * sel));
				}

				brackets(g, d, 0, Colors.scaleAlpha(van ? -1 : view.accent(), there * sel));
			}

			g.pose().popPose();
		}
	}

	private static void brackets(GuiGraphics g, int d, int shift, int color) {
		int arm = 4;

		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sy = -1; sy <= 1; sy += 2) {
				int x = sx * d + shift;
				int y = sy * d + shift;
				g.fill(Math.min(x, x - sx * 4), y, Math.max(x, x - sx * 4) + 1, y + 1, color);
				g.fill(x, Math.min(y, y - sy * 4), x + 1, Math.max(y, y - sy * 4) + 1, color);
			}
		}
	}

	private void body(GuiGraphics g, QuestPainter.View view, NodeView n, float sx, float sy, float scale0, float alpha) {
		if (n.shape() != Quest.Shape.NONE || n.advancement()) {
			WindowArt.frame(g, n.quest.frame, n.status, view.accent(), Math.round(sx), Math.round(sy), half(scale0), light(n), n.hover.get(), presence(n, alpha));
		}
	}

	private void picture(GuiGraphics g, QuestPainter.View view, NodeView n, float sx, float sy, float scale0, float alpha) {
		if (!(presence(n, alpha) < 0.45F)) {
			boolean van = Theme.current().isVanilla();
			int cx = Math.round(sx);
			int cy = Math.round(sy);
			float size = 16.0F * (float)half(scale0) / 13.0F;
			if (n.secret()) {
				Font font = CFHGuiDraw.font();
				float s = size / 16.0F * 1.5F;
				g.pose().pushPose();
				g.pose().translate((float)cx, (float)cy, 0.0F);
				g.pose().scale(s, s, 1.0F);
				g.drawString(font, "?", -font.width("?") / 2 + 1, -4, van ? -12566464 : Theme.current().palette().textDim(), false);
				g.pose().popPose();
			} else {
				n.icon()
					.draw(
						g,
						(float)cx,
						(float)cy,
						size * n.quest.iconScale,
						van ? -13949406 : (n.locked() ? Theme.current().palette().textDim() : Theme.current().palette().text())
					);
			}
		}
	}

	private void over(GuiGraphics g, QuestPainter.View view, NodeView n, float sx, float sy, float scale0, float alpha) {
		Theme t = Theme.current();
		Palette p = t.palette();
		boolean van = t.isVanilla();
		int cx = Math.round(sx);
		int cy = Math.round(sy);
		int h = half(scale0);
		float there = presence(n, alpha);
		float lit = n.lit.get();
		boolean plain = plain(n);
		float shade = Math.max(n.locked() && !n.secret() ? 0.5F : 0.0F, (1.0F - lit) * 0.55F);
		boolean marks = !plain && there > 0.5F;
		if (shade > 0.01F || marks || n.pinned) {
			g.pose().pushPose();
			g.pose().translate((float)cx, (float)cy, 200.0F);
			g.pose().scale((float)h / 13.0F, (float)h / 13.0F, 1.0F);
			if (shade > 0.01F) {
				g.fill(-8, -8, 8, 8, Colors.withAlpha(van ? -16777216 : cfhGround(), Math.round(255.0F * shade * there)));
			}

			if (marks) {
				float progress = n.shownProgress.get();
				if (n.status == Rules.Status.IN_PROGRESS && progress > 0.01F) {
					g.fill(-9, 7, 9, 10, Colors.scaleAlpha(-16777216, there));
					g.fill(-8, 8, -8 + Math.max(1, Math.round(16.0F * progress)), 9, Colors.scaleAlpha(van ? -8323296 : Colors.brighten(view.accent(), 0.2F), there));
				}

				if (n.status == Rules.Status.CLAIMABLE) {
					int bob = Math.round(Math.abs(Mth.sin(view.seconds() * 3.2F)) * 2.0F);
					mark(g, BANG, 8, -13 - bob, van ? -171 : p.warning(), there);
				} else if (n.locked() && !n.secret()) {
					mark(g, LOCK, 6, 5, van ? -2039584 : p.textMuted(), there);
				}

				if (n.quest.repeatable && !n.locked()) {
					mark(g, LOOP, -12, 5, van ? -1 : p.textMuted(), there);
				}

				if (n.folds) {
					mark(
						g,
						n.unfolded.target() >= 1.0F ? MINUS : PLUS,
						-2,
						10,
						van ? (n.locked() ? -6250336 : -1) : (n.locked() ? p.textDim() : Colors.brighten(view.accent(), 0.2F)),
						there
					);
				}
			}

			if (n.pinned && there > 0.5F) {
				mark(g, PIN, -12, -13, van ? -1 : view.accent(), there);
			}

			g.pose().popPose();
		}

		float near = scale0 / n.size;
		float named = n.folds ? 1.0F - n.unfolded.get() : 1.0F;
		if (near >= 1.3F && !n.secret() && alpha > 0.5F && named > 0.02F) {
			float shown = Mth.clamp((near - 1.3F) / 0.2F, 0.0F, 1.0F) * there * named;
			int room = Math.round(n.room * this.unit() * near) - 8;
			if (room >= 24) {
				FormattedCharSequence name = n.caption(Math.min(room, 110));
				int w = n.captionWidth();
				int tx = cx - w / 2;
				int ty = cy + h + 5 + (n.folds ? Math.round(4.0F * (float)h / 13.0F) : 0);
				int color = n.locked() ? (van ? -6250336 : p.textDim()) : (van ? -1 : p.text());
				g.pose().pushPose();
				g.pose().translate(0.0F, 0.0F, 200.0F);
				g.fill(tx - 2, ty - 1, tx + w + 2, ty + 9, Colors.withAlpha(van ? -16777216 : cfhGround(), Math.round((float)(van ? 144 : 160) * shown)));
				g.drawString(CFHGuiDraw.font(), name, tx, ty, Colors.scaleAlpha(color, Math.max(0.03F, shown)), van);
				g.pose().popPose();
			}
		}
	}

	private static void mark(GuiGraphics g, String[] rows, int x, int y, int color, float alpha) {
		g.fill(x - 1, y - 1, x + rows[0].length() + 1, y + rows.length + 1, Colors.scaleAlpha(-16777216, alpha));
		int ink = Colors.scaleAlpha(color, alpha);

		for (int row = 0; row < rows.length; row++) {
			String s = rows[row];
			int from = -1;

			for (int i = 0; i <= s.length(); i++) {
				boolean on = i < s.length() && s.charAt(i) == 'X';
				if (on && from < 0) {
					from = i;
				} else if (!on && from >= 0) {
					g.fill(x + from, y + row, x + i, y + row + 1, ink);
					from = -1;
				}
			}
		}
	}

	@Override
	public void opened(GuiGraphics g, QuestPainter.View view, NodeView n, float sx, float sy, float scale, float t) {
		float r = 13.0F * scale;
		if (!(r < 5.5F)) {
			boolean van = Theme.current().isVanilla();
			int color = van ? -1 : view.accent();
			int cx = Math.round(sx);
			int cy = Math.round(sy);

			for (int k = 0; k < 2; k++) {
				float tk = (t - (float)k * 0.22F) / (1.0F - (float)k * 0.22F);
				if (!(tk <= 0.0F) && !(tk >= 1.0F)) {
					float e = 1.0F - (1.0F - tk) * (1.0F - tk);
					int d = Math.round(r + 2.0F + e * r * 2.2F);
					float a = (1.0F - tk) * (1.0F - tk);
					if (van) {
						CFHGuiDraw.outline(g, cx - d, cy - d, d * 2 + 2, d * 2 + 2, Colors.withAlpha(-16777216, Math.round(144.0F * a)), 0);
					}

					CFHGuiDraw.outline(g, cx - d, cy - d, d * 2, d * 2, Colors.withAlpha(color, Math.round(224.0F * a)), 0);
				}
			}
		}
	}

	@Override
	public void celebrated(GuiGraphics g, QuestPainter.View view, NodeView n, float sx, float sy, float scale, float t) {
		float r = 13.0F * scale;
		if (!(r < 5.5F)) {
			Theme th = Theme.current();
			Palette p = th.palette();
			boolean van = th.isVanilla();
			Quest.Frame frame = n.quest.frame;
			int cx = Math.round(sx);
			int cy = Math.round(sy);
			int sparks = frame == Quest.Frame.CHALLENGE ? 44 : (frame == Quest.Frame.GOAL ? 26 : 14);
			float reach = r * (frame == Quest.Frame.CHALLENGE ? 4.6F : (frame == Quest.Frame.GOAL ? 3.4F : 2.5F));
			int[] colors = van ? new int[]{-171, -1, -2383341, -8323296} : new int[]{p.success(), p.warning(), -1, view.accent()};
			if (t < 0.25F) {
				int h = Math.round(r) - 1;
				g.fill(cx - h, cy - h, cx + h, cy + h, Colors.withAlpha(-1, Math.round(176.0F * (1.0F - t / 0.25F))));
			}

			int rings = frame == Quest.Frame.TASK ? 1 : 2;

			for (int k = 0; k < rings; k++) {
				float tk = Mth.clamp((t - (float)k * 0.12F) / 0.55F, 0.0F, 1.0F);
				if (!(tk <= 0.0F) && !(tk >= 1.0F)) {
					float e = 1.0F - (1.0F - tk) * (1.0F - tk) * (1.0F - tk);
					int d = Math.round(r + e * reach * 0.7F);
					CFHGuiDraw.outline(g, cx - d, cy - d, d * 2, d * 2, Colors.withAlpha(colors[k % colors.length], Math.round(224.0F * (1.0F - tk))), 0);
				}
			}

			float e = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);

			for (int i = 0; i < sparks; i++) {
				float a = spark(n.index, i, 1) * (float) (Math.PI * 2);
				float far = reach * (0.45F + 0.75F * spark(n.index, i, 2));
				float life = 0.6F + 0.4F * spark(n.index, i, 3);
				float tk = t / life;
				if (!(tk >= 1.0F)) {
					int x = Math.round(sx + Mth.cos(a) * (r * 0.6F + far * e));
					int y = Math.round(sy + Mth.sin(a) * (r * 0.6F + far * e) + r * 1.6F * t * t);
					int size = tk < 0.5F && scale > 0.8F ? 2 : 1;
					if (van) {
						g.fill(x + 1, y + 1, x + size + 1, y + size + 1, Colors.withAlpha(-16777216, Math.round(160.0F * (1.0F - tk * tk))));
					}

					g.fill(x, y, x + size, y + size, Colors.withAlpha(colors[i % colors.length], Math.round(255.0F * (1.0F - tk * tk))));
				}
			}
		}
	}

	private static float spark(int node, int i, int salt) {
		int h = (node * 31 + i) * -1640531535 + salt * -2048144789;
		h ^= h >>> 15;
		h *= 739982445;
		h ^= h >>> 12;
		return (float)(h & 65535) / 65536.0F;
	}

	void clearRight(int x) {
		this.clearRight = x;
	}

	private void words(QuestPainter.View view, NodeView n) {
		Theme t = Theme.current();
		Palette p = t.palette();
		boolean van = t.isVanilla();
		Font font = CFHGuiDraw.font();
		Quest q = n.quest;
		this.boxNode = n;
		this.boxRevision = BookStats.revision();
		this.boxVanilla = van;
		this.boxUnfolded = unfolded(n);
		this.boxRows.clear();
		int said = van ? (q.frame == Quest.Frame.CHALLENGE ? -5635926 : -11141291) : p.textMuted();
		int grey = van ? -5592406 : p.textMuted();
		int dim = van ? -8355712 : p.textDim();
		int good = van ? -11141291 : p.success();
		int gold = van ? -22016 : p.warning();
		boolean secret = n.secret();
		Component title = (Component)(secret ? QuestText.secret() : (van ? QuestText.title(n.entry) : Fonts.heading(QuestText.title(n.entry))));
		QuestState state = QuestsState.state(n.key);
		String count = "";
		if (!secret && q.tasks.size() == 1 && q.tasks.get(0).count > 1L) {
			Task task = q.tasks.get(0);
			count = QuestText.count(task, n.done() ? task.count : state.task(0)).getString().replace(" ", "");
		} else if (!secret && q.tasks.size() > 1 && !n.locked()) {
			int done = 0;

			for (int i = 0; i < q.tasks.size(); i++) {
				if (n.done() || Rules.taskDone(q.tasks.get(i), state.task(i))) {
					done++;
				}
			}

			count = done + "/" + q.tasks.size();
		}

		this.boxCount = count.isEmpty() ? null : Component.literal(count).getVisualOrderText();
		int countW = this.boxCount == null ? 0 : font.width(this.boxCount) + 8;
		this.boxTitle = CFHGuiDraw.truncate(title, 163 - countW);
		int bar = 29 + font.width(this.boxTitle) + countW;
		int width = bar;
		boolean plain = plain(n);
		if (secret) {
			width = this.lines(Component.translatable("cfhgui_quests.card.secret"), grey, 3, Math.max(bar, 150), false, bar);
		} else {
			String told = plain ? AdvancementChapters.description(n.key) : "";
			Component about = !told.isEmpty() ? Component.literal(told) : (q.subtitle.isBlank() ? null : QuestText.text(q.subtitle));
			if (about != null) {
				width = this.lines(about, said, plain ? 12 : 3, plain ? bar : Math.max(bar, 150), plain, bar);
			}

			if (!plain) {
				this.boxRows
					.add(
						new VanillaQuestPainter.Row(
							0,
							CFHGuiDraw.truncate(Component.empty().append(QuestText.status(n.status)).append("  ·  ").append(QuestText.sync(n.entry)), 186),
							n.status == Rules.Status.COMPLETE ? grey : (n.status == Rules.Status.CLAIMABLE ? gold : (n.locked() ? dim : (van ? -1 : view.accent()))),
							null,
							0,
							null,
							10
						)
					);

				for (int ix = 0; ix < q.tasks.size() && ix < 4; ix++) {
					Task task = q.tasks.get(ix);
					long counted = n.done() ? task.count : state.task(ix);
					boolean done = Rules.taskDone(task, counted);
					String c = QuestText.count(task, counted).getString();
					FormattedCharSequence right = c.isEmpty() ? null : Component.literal(c).getVisualOrderText();
					int rightW = right == null ? 0 : font.width(right) + 8;
					FormattedCharSequence label = CFHGuiDraw.truncate(QuestText.label(task), 178 - rightW);
					this.boxRows.add(new VanillaQuestPainter.Row(1, label, done ? good : (n.locked() ? dim : grey), right, done ? good : dim, null, 9));
					width = Math.max(width, font.width(label) + 8 + rightW);
				}

				if (q.tasks.size() > 4) {
					this.boxRows
						.add(
							new VanillaQuestPainter.Row(
								0, Component.translatable("cfhgui_quests.card.more", new Object[]{q.tasks.size() - 4}).getVisualOrderText(), dim, null, 0, null, 9
							)
						);
				}

				if (!q.rewards.isEmpty()) {
					List<QuestIcons.Drawable> icons = new ArrayList<>();

					for (int ix = 0; ix < q.rewards.size() && ix < 7; ix++) {
						icons.add(QuestIcons.of(q.rewards.get(ix)));
					}

					FormattedCharSequence label = Component.translatable("cfhgui_quests.rewards").getVisualOrderText();
					this.boxRows.add(new VanillaQuestPainter.Row(2, label, n.status == Rules.Status.CLAIMABLE ? gold : grey, null, 0, icons, 14));
					width = Math.max(width, font.width(label) + 6 + icons.size() * 13);
				}

				if (n.gateway()) {
					Chapter sub = QuestsState.book() == null ? null : QuestsState.book().chapter(q.subtree);
					BookStats.Stat stat = BookStats.of(sub);
					width = this.lines(
						Component.translatable("cfhgui_quests.card.subtree", new Object[]{stat.done(), stat.total()}), van ? -1 : view.accent(), 2, 186, false, width
					);
				}

				if (n.locked()) {
					Component needs = QuestText.needs(n.entry);
					if (needs != null) {
						width = this.lines(needs, dim, 3, 186, false, width);
					}
				}

				FormattedCharSequence hint = CFHGuiDraw.truncate(Component.translatable(CFHGuiQuestPainter.hintKey(n)), 186);
				this.boxRows.add(new VanillaQuestPainter.Row(0, hint, dim, null, 0, null, 10));
				width = Math.max(width, font.width(hint));
			}
		}

		this.boxWidth = Mth.clamp(width + 8, plain ? 40 : 116, 200);
		int h = 0;

		for (VanillaQuestPainter.Row r : this.boxRows) {
			h += r.height();
		}

		this.boxBody = h;
	}

	private int lines(Component text, int color, int max, int at, boolean balanced, int width) {
		Font font = CFHGuiDraw.font();
		int limit = Mth.clamp(at, 60, 186);
		List<FormattedCharSequence> split = null;
		if (balanced) {
			float best = Float.MAX_VALUE;

			for (int offset : SPLIT_OFFSETS) {
				List<FormattedCharSequence> tried = font.split(text, Mth.clamp(limit - offset, 40, 186));
				int widest = 0;

				for (FormattedCharSequence line : tried) {
					widest = Math.max(widest, font.width(line));
				}

				float off = (float)Math.abs(widest - limit);
				if (off <= 10.0F) {
					split = tried;
					break;
				}

				if (off < best) {
					best = off;
					split = tried;
				}
			}
		}

		if (split == null) {
			split = font.split(text, limit);
		}

		for (int i = 0; i < split.size() && i < max; i++) {
			this.boxRows.add(new VanillaQuestPainter.Row(0, split.get(i), color, null, 0, null, 9));
			width = Math.max(width, font.width(split.get(i)));
		}

		return width;
	}

	@Override
	public void card(GuiGraphics g, QuestPainter.View view, NodeView n, float nx, float ny, float nr, int side, float alpha, Rect bounds) {
		if (!(alpha <= 0.01F)) {
			Theme t = Theme.current();
			boolean van = t.isVanilla();
			if (this.boxNode != n || this.boxRevision != BookStats.revision() || this.boxVanilla != van || this.boxUnfolded != unfolded(n)) {
				this.words(view, n);
			}

			Font font = CFHGuiDraw.font();
			int w = this.boxWidth;
			int body = this.boxRows.isEmpty() ? 0 : this.boxBody + 6;
			if (w + 6 <= bounds.w() && 26 + body + 6 <= bounds.h()) {
				int cx = Math.round(nx);
				int cy = Math.round(ny);
				int half = Math.max(1, Math.round(nr));
				int lead = half > 13 ? 3 - half : 16;
				int edge = cx < this.clearRight && this.clearRight - bounds.x() >= w + 6 ? Math.min(bounds.right(), this.clearRight) : bounds.right();
				boolean fitsRight = cx - lead + w <= edge - 2;
				boolean fitsLeft = cx + lead - w >= bounds.x() + 2;
				boolean right = side > 0 ? fitsRight || !fitsLeft : (side < 0 ? !fitsLeft && fitsRight : fitsRight || !fitsLeft);
				int x = right ? cx - lead : cx + lead - w;
				x = Mth.clamp(x, bounds.x() + 2, Math.max(bounds.x() + 2, edge - 2 - w));
				int y = Mth.clamp(cy - 13, bounds.y() + 2, Math.max(bounds.y() + 2, bounds.bottom() - 28));
				boolean below = y + 26 + body <= bounds.bottom() - 2 || y - body < bounds.y() + 2;
				int boxY = below ? y : y + 26 - (26 + body);
				if (body > 0) {
					WindowArt.box(g, x, boxY, w, 26 + body, alpha);
				}

				WindowArt.titleBar(g, x, y, w, n.done() ? 1.0F : (n.locked() ? 0.0F : n.progress), n.status, view.accent(), n.locked() ? 0.6F : 1.0F, alpha);
				WindowArt.frame(g, n.quest.frame, n.status, view.accent(), cx, cy, half, n.locked() ? 0.7F : 1.0F, 1.0F, alpha);
				if (alpha > 0.5F) {
					if (n.secret()) {
						g.drawString(font, "?", cx - font.width("?") / 2 + 1, cy - 4, van ? -12566464 : t.palette().textDim(), false);
					} else {
						n.icon().draw(g, (float)cx, (float)cy, 16.0F * (float)half / 13.0F, van ? -13949406 : t.palette().text());
					}
				}

				int ink = Colors.scaleAlpha(van ? -1 : t.palette().text(), alpha);
				int textLeft = right && x == cx - lead ? cx + half + 3 : x + 5;
				int textRight = !right && x == cx + lead - w ? cx - half - 3 : x + w - 5;
				FormattedCharSequence title = font.width(this.boxTitle) + (this.boxCount == null ? 0 : font.width(this.boxCount) + 8) <= textRight - textLeft
					? this.boxTitle
					: CFHGuiDraw.truncate(
						n.secret() ? QuestText.secret() : QuestText.title(n.entry),
						Math.max(8, textRight - textLeft - (this.boxCount == null ? 0 : font.width(this.boxCount) + 8))
					);
				g.drawString(font, title, textLeft, y + 9, ink, true);
				if (this.boxCount != null) {
					g.drawString(font, this.boxCount, textRight - font.width(this.boxCount), y + 9, ink, true);
				}

				if (body > 0) {
					int ty = below ? y + 26 : boxY + 7;
					int tx = x + 5;
					int inner = w - 10;

					for (VanillaQuestPainter.Row r : this.boxRows) {
						switch (r.kind()) {
							case 1:
								g.fill(tx + 1, ty + 3, tx + 3, ty + 5, Colors.scaleAlpha(r.color(), alpha));
								g.drawString(font, r.text(), tx + 7, ty, Colors.scaleAlpha(r.color(), alpha), false);
								if (r.right() != null) {
									g.drawString(font, r.right(), tx + inner - font.width(r.right()), ty, Colors.scaleAlpha(r.rightColor(), alpha), false);
								}
								break;
							case 2:
								g.drawString(font, r.text(), tx, ty + 3, Colors.scaleAlpha(r.color(), alpha), false);
								int ix = tx + font.width(r.text()) + 6;
								if (alpha > 0.6F && r.icons() != null) {
									for (QuestIcons.Drawable d : r.icons()) {
										d.draw(g, (float)(ix + 6), (float)(ty + 7), 12.0F, van ? -1 : t.palette().textMuted());
										ix += 13;
									}
								}
								break;
							default:
								g.drawString(font, r.text(), tx, ty + (r.height() > 9 ? 1 : 0), Colors.scaleAlpha(r.color(), alpha), false);
						}

						ty += r.height();
					}
				}
			}
		}
	}

	@Override
	public void previewPlate(GuiGraphics g, QuestPainter.View view, SubtreePreview preview, int x, int y, int w, int h, float open) {
		Theme t = Theme.current();
		boolean van = t.isVanilla();
		Font font = CFHGuiDraw.font();
		float fade = Math.min(1.0F, open * 1.5F);
		this.flushLines(g);
		int side = preview.side();
		int ex = Math.round(preview.ownerX() + (float)side * (preview.ownerRadius() + 1.0F));
		int plateX = side > 0 ? x : x + w - 1;
		int oy = Math.round(preview.ownerY());
		int py2 = Mth.clamp(oy, y + 6, y + h - 6);
		if (Math.abs(plateX - ex) > 1) {
			int mid = (ex + plateX) / 2;
			int black = Colors.scaleAlpha(van ? -16777216 : Colors.withAlpha(cfhGround(), 192), fade);
			int white = Colors.scaleAlpha(van ? -1 : Colors.brighten(view.accent(), 0.2F), fade);
			cells(g, ex, oy, mid, oy, 1, 1, black);
			cells(g, mid, oy, mid, py2, 1, 1, black);
			cells(g, mid, py2, plateX, py2, 1, 1, black);
			cells(g, ex, oy, mid, oy, 1, 0, white);
			cells(g, mid, oy, mid, py2, 1, 0, white);
			cells(g, mid, py2, plateX, py2, 1, 0, white);
		}

		Rect box = preview.box();
		if (w >= 34 && h >= 44) {
			g.enableScissor(x + 3, y + 3, x + w - 3, y + h - 3);
			this.ground(g, view.chapter(), new Rect(x, y, w, h), (float)box.x(), (float)box.y(), 1.0F, fade);
			g.disableScissor();
			WindowArt.window(g, x, y, w, h, fade);
			float words = preview.words();
			if (!(words <= 0.02F) && w >= 60) {
				g.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
				String count = preview.done + " / " + preview.total;
				int countW = font.width(count);
				Component title = (Component)(van ? preview.title : Fonts.heading(preview.title));
				g.drawString(font, CFHGuiDraw.truncate(title, box.w() - 16 - countW - 6), box.x() + 8, box.y() + 6, Colors.scaleAlpha(WindowArt.titleInk(), words), false);
				g.drawString(font, count, box.x() + box.w() - 8 - countW, box.y() + 6, Colors.scaleAlpha(WindowArt.titleInkQuiet(), words), false);
				FormattedCharSequence hint = CFHGuiDraw.truncate(Component.translatable("cfhgui_quests.preview.hint"), box.w() - 24);
				int hw = font.width(hint);
				int hx = box.x() + (box.w() - hw) / 2;
				int hy = box.y() + box.h() - 9 - 11;
				g.pose().pushPose();
				g.pose().translate(0.0F, 0.0F, 250.0F);
				g.fill(hx - 3, hy - 1, hx + hw + 3, hy + 9, Colors.withAlpha(van ? -16777216 : cfhGround(), Math.round(144.0F * words)));
				g.drawString(font, hint, hx, hy, Colors.scaleAlpha(van ? -1 : t.palette().textMuted(), words), van);
				g.pose().popPose();
				g.disableScissor();
			}
		} else {
			g.fill(x, y, x + w, y + h, Colors.scaleAlpha(van ? -16777216 : t.palette().borderStrong(), fade));
			g.fill(x + 1, y + 1, x + w - 1, y + h - 1, Colors.scaleAlpha(van ? -3750202 : t.palette().bg2(), fade));
		}
	}

	private static boolean unfolded(NodeView n) {
		return n.folds && n.unfolded.target() >= 1.0F;
	}

	@Override
	public void inlayPlate(
		GuiGraphics g, QuestPainter.View view, Inlay inlay, int x, int y, int w, int h, float hangX, float hangY, float scale, float open, float words, float alpha
	) {
		if (!(alpha <= 0.01F) && w >= 2 && h >= 2) {
			Theme t = Theme.current();
			boolean van = t.isVanilla();
			Font font = CFHGuiDraw.font();
			this.flushLines(g);
			float unit = this.unit() * scale;
			Rect a = view.area();
			float ox = (float)a.x() + (float)a.w() / 2.0F + (inlay.x - view.camX()) * unit;
			float oy = (float)a.y() + (float)a.h() / 2.0F + (inlay.y - view.camY()) * unit;
			int head = Math.round(0.75F * unit);
			int hx = Math.round(hangX);
			int from = Math.round(hangY);
			int black = Colors.scaleAlpha(van ? -16777216 : Colors.withAlpha(cfhGround(), 192), alpha);
			int white = Colors.scaleAlpha(van ? -1 : Colors.brighten(view.accent(), 0.2F), alpha);
			if (y - 1 > from) {
				cells(g, hx, from, hx, y - 1, 1, 1, black);
				cells(g, hx, from, hx, y - 1, 1, 0, white);
			}

			boolean window = scale >= 0.86F && w >= 34 && h >= 44;
			if (!window) {
				g.fill(x, y, x + w, y + h, Colors.scaleAlpha(van ? -16777216 : t.palette().borderStrong(), alpha));
				g.fill(x + 1, y + 1, x + w - 1, y + h - 1, Colors.scaleAlpha(van ? -3750202 : t.palette().bg2(), alpha));
				if (w > 6 && h > 6) {
					g.fill(x + 2, y + 2, x + w - 2, y + h - 2, Colors.scaleAlpha(-16777216, alpha));
					g.enableScissor(x + 2, y + 2, x + w - 2, y + h - 2);
					this.ground(g, view.chapter(), new Rect(x, y, w, h), ox, oy, Mth.clamp(scale, 0.5F, 2.0F), alpha);
					g.disableScissor();
				}

				if (head > 3 && h > head + 2) {
					cells(g, hx, y + 2, hx, y + head, 1, 1, black);
					cells(g, hx, y + 2, hx, y + head, 1, 0, white);
				}

				if (words > 0.02F && w >= 60 && head >= 11 && h > head) {
					BookStats.Stat stat = BookStats.of(inlay.chapter);
					String count = stat.total() > 0 ? stat.done() + " / " + stat.total() : "";
					int countW = count.isEmpty() ? 0 : font.width(count) + 6;
					int ty = y + 2 + Math.max(1, (head - 2 - 8) / 2);
					int room = x + w - 5 - countW - (hx + 4);
					int tx = room >= 40 ? hx + 4 : x + 5;
					int tw = room >= 40 ? room : hx - 3 - tx;
					Component title = (Component)(van ? inlay.title : Fonts.heading(inlay.title));
					if (tw >= 24) {
						g.drawString(font, CFHGuiDraw.truncate(title, tw), tx, ty, Colors.scaleAlpha(van ? -1 : t.palette().text(), words * alpha), true);
					}

					if (!count.isEmpty() && w >= 110) {
						g.drawString(
							font,
							count,
							x + w - 5 - font.width(count),
							ty,
							Colors.scaleAlpha(stat.complete() ? (van ? -11141291 : t.palette().success()) : (van ? -5592406 : t.palette().textDim()), words * alpha),
							true
						);
					}
				}
			} else {
				g.fill(x + 3, y + 3, x + w - 3, y + h - 3, Colors.scaleAlpha(-16777216, alpha));
				g.enableScissor(x + 3, y + 3, x + w - 3, y + h - 3);
				this.ground(g, view.chapter(), new Rect(x, y, w, h), ox, oy, Mth.clamp(scale, 0.5F, 2.0F), alpha);
				g.disableScissor();
				WindowArt.window(g, x, y, w, h, alpha);
				if (y + head > y + 18 && h > head) {
					cells(g, hx, y + 18, hx, y + head, 1, 1, black);
					cells(g, hx, y + 18, hx, y + head, 1, 0, white);
				}

				if (!(words <= 0.02F) && w >= 60) {
					BookStats.Stat statx = BookStats.of(inlay.chapter);
					String countx = statx.total() > 0 ? statx.done() + " / " + statx.total() : "";
					int countWx = countx.isEmpty() ? 0 : font.width(countx) + 6;
					Component titlex = (Component)(van ? inlay.title : Fonts.heading(inlay.title));
					g.drawString(font, CFHGuiDraw.truncate(titlex, w - 16 - countWx), x + 8, y + 6, Colors.scaleAlpha(WindowArt.titleInk(), words * alpha), false);
					if (!countx.isEmpty() && w - 16 - countWx >= 40) {
						g.drawString(
							font,
							countx,
							x + w - 8 - font.width(countx),
							y + 6,
							Colors.scaleAlpha(statx.complete() ? (van ? -13735122 : t.palette().success()) : WindowArt.titleInkQuiet(), words * alpha),
							false
						);
					}
				}
			}
		}
	}

	@Override
	public void stem(GuiGraphics g, QuestPainter.View view, LinkView stem, float[] xs, float[] ys, int points, float scale, float alpha, float grown) {
		if (!(alpha <= 0.01F) && !(grown <= 0.01F) && points >= 2) {
			Theme t = Theme.current();
			boolean van = t.isVanilla();
			float lit = stem.lit();
			int width = scale >= 1.75F ? 2 : 1;
			int ink = van ? -1 : Colors.lerp(Colors.mix(view.accent(), cfhGround(), 0.15F), view.accent(), stem.hover() * 0.85F);
			ink = Colors.lerp(Colors.mix(ink, van ? -16777216 : cfhGround(), van ? 0.6F : 0.7F), ink, lit);
			int black = Colors.scaleAlpha(van ? -16777216 : Colors.withAlpha(cfhGround(), 192), alpha);
			int white = Colors.scaleAlpha(ink, alpha);
			int total = 0;

			for (int i = 1; i < points; i++) {
				total += Math.abs(Math.round(xs[i]) - Math.round(xs[i - 1])) + Math.abs(Math.round(ys[i]) - Math.round(ys[i - 1]));
			}

			int left = grown >= 0.999F ? total + 1 : Math.max(1, Math.round((float)total * grown));

			for (int i = 1; i < points && left > 0; i++) {
				int sx = Math.round(xs[i - 1]);
				int sy = Math.round(ys[i - 1]);
				int ex = Math.round(xs[i]);
				int ey = Math.round(ys[i]);
				int run = Math.abs(ex - sx) + Math.abs(ey - sy);
				if (run > left) {
					ex = sx + Integer.signum(ex - sx) * left;
					ey = sy + Integer.signum(ey - sy) * left;
				}

				left -= run;
				cells(g, sx, sy, ex, ey, width, 1, black);
				this.lines = queue(this.lines, this.lineCount++, sx, sy, ex, ey, width, white);
			}
		}
	}

	@Override
	public void control(GuiGraphics g, QuestPainter.View view, QuestPainter.Control control, int x, int y, int size, float hover, boolean enabled) {
		if (!Theme.current().isVanilla()) {
			QuestPainter.super.control(g, view, control, x, y, size, hover, enabled);
		} else {
			g.flush();
			CFHGuiDraw.vanillaButton(g, x, y, size, size, hover, enabled, 1.0F);
			int cx = x + size / 2;
			int cy = y + size / 2;

			for (int pass = 0; pass < 2; pass++) {
				int o = pass == 0 ? 1 : 0;
				int c = pass == 0 ? -12632257 : (enabled ? (hover > 0.5F ? -96 : -1) : -6250336);
				int sx;
				switch (control) {
					case ZOOM_IN:
						g.fill(cx - 3 + o, cy - 1 + o, cx + 3 + o, cy + 1 + o, c);
						g.fill(cx - 1 + o, cy - 3 + o, cx + 1 + o, cy + 3 + o, c);
						continue;
					case ZOOM_OUT:
						g.fill(cx - 3 + o, cy - 1 + o, cx + 3 + o, cy + 1 + o, c);
						continue;
					default:
						sx = -1;
				}

				while (sx <= 1) {
					for (int sy = -1; sy <= 1; sy += 2) {
						int px0 = cx + (sx < 0 ? -3 : 2) + o;
						int py0 = cy + (sy < 0 ? -3 : 2) + o;
						g.fill(Math.min(px0, px0 - sx), py0, Math.max(px0, px0 - sx) + 1, py0 + 1, c);
						g.fill(px0, Math.min(py0, py0 - sy), px0 + 1, Math.max(py0, py0 - sy) + 1, c);
					}

					sx += 2;
				}
			}
		}
	}

	@Override
	public void overview(GuiGraphics g, QuestPainter.View view, GraphModel graph, QuestPainter.Overview o, float alpha) {
		if (!Theme.current().isVanilla()) {
			QuestPainter.super.overview(g, view, graph, o, alpha);
		} else if (!(alpha <= 0.02F) && !graph.isEmpty()) {
			int x = o.x();
			int y = o.y();
			int w = o.w();
			int h = o.h();
			g.fill(x, y, x + w, y + h, Colors.scaleAlpha(-16777216, alpha));
			g.fill(x + 1, y + 1, x + w - 1, y + h - 1, Colors.scaleAlpha(-3750202, alpha));
			g.fill(x + 1, y + 1, x + w - 2, y + 2, Colors.scaleAlpha(-1, alpha));
			g.fill(x + 1, y + 1, x + 2, y + h - 2, Colors.scaleAlpha(-1, alpha));
			g.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, Colors.scaleAlpha(-11184811, alpha));
			g.fill(x + w - 2, y + 2, x + w - 1, y + h - 1, Colors.scaleAlpha(-11184811, alpha));
			g.fill(x + 3, y + 3, x + w - 3, y + h - 3, Colors.scaleAlpha(-14606047, alpha));
			int dot = o.scale() >= 3.0F ? 2 : 1;

			for (NodeView n : graph.nodes) {
				int color = switch (n.status) {
					case COMPLETE -> -2383341;
					case CLAIMABLE -> -171;
					case AVAILABLE, IN_PROGRESS -> -1;
					default -> -9474193;
				};
				int dx = Math.round(o.sx(n.x));
				int dy = Math.round(o.sy(n.y));
				if (dx >= x + 3 && dy >= y + 3 && dx + dot <= x + w - 3 && dy + dot <= y + h - 3) {
					g.fill(dx, dy, dx + dot, dy + dot, Colors.scaleAlpha(color, alpha));
				}
			}

			int rx0 = Math.max(x + 3, Math.round(o.sx(o.vx0())));
			int ry0 = Math.max(y + 3, Math.round(o.sy(o.vy0())));
			int rx1 = Math.min(x + w - 3, Math.round(o.sx(o.vx1())));
			int ry1 = Math.min(y + h - 3, Math.round(o.sy(o.vy1())));
			if (rx1 - rx0 >= 2 && ry1 - ry0 >= 2) {
				g.fill(rx0, ry0, rx1, ry1, Colors.scaleAlpha(587202559, alpha));
				CFHGuiDraw.outline(g, rx0, ry0, rx1 - rx0, ry1 - ry0, Colors.scaleAlpha(-1, alpha), 0);
			}
		}
	}

	@Override
	public void empty(GuiGraphics g, QuestPainter.View view, Component text) {
		Theme t = Theme.current();
		Rect a = view.area();
		if (!t.isVanilla()) {
			QuestPainter.super.empty(g, view, text);
		} else {
			g.fill(a.x(), a.y(), a.right(), a.bottom(), Integer.MIN_VALUE);
			CFHGuiDraw.textCentered(g, text, a.centerX(), a.centerY() - 4, -1, true);
			if (a.h() >= 60) {
				CFHGuiDraw.textCentered(g, Component.translatable("advancements.sad_label"), a.centerX(), a.bottom() - 14, -1, true);
			}
		}
	}

	private static record Row(
		int kind, FormattedCharSequence text, int color, @Nullable FormattedCharSequence right, int rightColor, @Nullable List<QuestIcons.Drawable> icons, int height
	) {
		static final int TEXT = 0;
		static final int TASK = 1;
		static final int ICONS = 2;
	}
}
