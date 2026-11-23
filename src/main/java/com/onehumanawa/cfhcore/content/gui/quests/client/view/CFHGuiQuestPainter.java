package com.onehumanawa.cfhcore.content.gui.quests.client.view;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.quests.QuestsConfig;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.logic.Rules;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestState;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public class CFHGuiQuestPainter implements QuestPainter {
	protected static final int R = 11;
	private static final QuestIcons.Drawable QUESTION = QuestIcons.glyph(Icon.QUESTION);
	protected int fill;
	protected int rim;
	protected int tint;
	protected float dim;
	protected int ground = -15724528;
	private static final float DOT_BELOW = 2.6F;
	private static final float PICTURE_FROM = 5.2F;
	private boolean deferring;
	private NodeView[] putOff = new NodeView[64];
	private QuestPainter.View[] putOffView = new QuestPainter.View[64];
	private float[] putOffAt = new float[256];
	private int putOffCount;
	protected static final float NAMES_FROM = 1.3F;
	private static final String[] CHECK = new String[]{"......X", ".....XX", "X...XX.", "XX.XX..", ".XXX...", "..X...."};
	private static final String[] STAR = new String[]{"...X...", "...X...", "XXXXXXX", ".XXXXX.", "..XXX..", ".XX.XX.", ".X...X."};
	private static final String[] LOCK = new String[]{".XXX.", "X...X", "XXXXX", "XX.XX", "XX.XX", "XXXXX"};
	private static final String[] PIN = new String[]{".XXX.", ".XXX.", "XXXXX", "..X..", "..X..", "..X.."};
	private static final String[] LOOP = new String[]{".XXX.X", "X...XX", "X..XXX", "X.....", "X...X.", ".XXX.."};
	private static final int CARD_PAD = 7;
	private static final int CARD_MAX = 190;
	private static final int CARD_MIN = 118;
	private final List<CFHGuiQuestPainter.Row> cardRows = new ArrayList<>();
	@Nullable
	private NodeView cardNode;
	private int cardRevision = -1;
	private int cardWidth;
	private int cardHeight;
	private int cardStatusColor;
	private boolean cardVanilla;
	private boolean cardUnfolded;
	private FormattedCharSequence cardTitle = FormattedCharSequence.EMPTY;
	private FormattedCharSequence cardStatus = FormattedCharSequence.EMPTY;

	public CFHGuiQuestPainter on(int groundColor) {
		this.ground = groundColor | 0xFF000000;
		return this;
	}

	protected int groundOf(NodeView n) {
		Inlay in = n.inlay;
		return in != null && in.ground != 0 ? in.ground : this.ground;
	}

	protected void tones(QuestPainter.View view, NodeView n) {
		Theme t = Theme.current();
		Palette p = t.palette();
		float hover = n.hover.get();
		this.dim = 0.0F;
		if (t.isVanilla()) {
			this.rim = Colors.lerp(-16777216, -1, Math.max(hover, n.selected.get()));
			this.tint = -1;
			switch (n.status) {
				case LOCKED:
				case HIDDEN:
					this.fill = -12829636;
					this.tint = -7631989;
					this.dim = 0.55F;
					break;
				case COMPLETE:
					this.fill = -11630002;
					break;
				case CLAIMABLE:
					this.fill = -5207506;
					break;
				default:
					this.fill = Colors.lerp(-9474193, -7695440, hover);
			}
		} else {
			int accent = view.accent();
			this.tint = p.text();
			switch (n.status) {
				case LOCKED:
				case HIDDEN:
					this.fill = p.bg2();
					this.rim = Colors.lerp(p.border(), p.borderStrong(), hover);
					this.tint = p.textDim();
					this.dim = 0.62F;
					break;
				case COMPLETE:
					this.fill = Colors.mix(p.surface(), p.success(), 0.18F);
					this.rim = Colors.lerp(p.success(), Colors.brighten(p.success(), 0.25F), hover);
					break;
				case CLAIMABLE:
					this.fill = Colors.mix(p.surface(), p.warning(), 0.22F);
					this.rim = Colors.lerp(p.warning(), Colors.brighten(p.warning(), 0.25F), hover);
					break;
				case IN_PROGRESS:
					this.fill = Colors.lerp(p.surface(), p.surfaceHover(), hover);
					this.rim = Colors.lerp(p.borderStrong(), Colors.mix(p.borderStrong(), accent, 0.5F), hover);
					break;
				default:
					this.fill = Colors.lerp(p.surface(), p.surfaceHover(), hover);
					this.rim = Colors.lerp(Colors.mix(p.borderStrong(), accent, 0.7F), accent, hover);
			}
		}
	}

	protected int statusColor(QuestPainter.View view, Rules.Status status) {
		Theme t = Theme.current();
		Palette p = t.palette();
		boolean van = t.isVanilla();

		return switch (status) {
			case LOCKED, HIDDEN -> van ? -6250336 : p.textDim();
			case COMPLETE -> van ? -11141291 : p.success();
			case CLAIMABLE -> van ? -22016 : p.warning();
			default -> van ? -1 : view.accent();
		};
	}

	@Override
	public void backdrop(GuiGraphics g, QuestPainter.View view) {
		this.ground = Theme.current().isVanilla() ? -14540254 : Backdrops.ground(view);
		Backdrops.draw(g, view, this.unit() * view.zoom());
	}

	@Override
	public void veil(GuiGraphics g, QuestPainter.View view, float alpha) {
		Backdrops.veil(g, view, this.unit() * view.zoom(), alpha);
	}

	@Override
	public void frame(GuiGraphics g, QuestPainter.View view) {
		if (!Theme.current().isVanilla()) {
			Rect a = view.area();
			int edge = 1342177280;
			int h = Math.max(8, a.h() / 7);
			g.fillGradient(a.x(), a.y(), a.right(), a.y() + h, 1342177280, 0);
			g.fillGradient(a.x(), a.bottom() - h, a.right(), a.bottom(), 0, 1342177280);
		}
	}

	@Override
	public void decoration(GuiGraphics g, QuestPainter.View view, Chapter.Decoration d, float sx, float sy, float scale) {
		Theme t = Theme.current();
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
			float s = Math.max(d.heading ? 0.75F : 0.6F, Math.min(1.5F, scale)) * d.scale;
			if (!(scale < (d.heading ? 0.22F : 0.5F))) {
				Font font = CFHGuiDraw.font();
				Component text = d.heading ? Fonts.heading(QuestText.text(d.text)) : QuestText.text(d.text);
				int color = d.color.isEmpty() ? (t.isVanilla() ? -2039584 : t.palette().textMuted()) : Colors.fromHex(d.color, t.palette().textMuted());
				g.pose().pushPose();
				g.pose().translate((float)Math.round(sx), (float)Math.round(sy), 0.0F);
				g.pose().scale(s, s, 1.0F);
				g.drawString(
					font, text, -font.width(text) / 2, -4, Colors.scaleAlpha(color | 0xFF000000, Mth.clamp(d.alpha, 0.0F, 1.0F) * (d.heading ? 1.0F : 0.9F)), t.isVanilla()
				);
				g.pose().popPose();
			}
		}
	}

	@Override
	public void link(GuiGraphics g, QuestPainter.View view, LinkView link, float x1, float y1, float x2, float y2, float scale, float alpha, float grown) {
		if (!(alpha <= 0.01F) && !(grown <= 0.01F)) {
			Theme t = Theme.current();
			Palette p = t.palette();
			float ex = Mth.lerp(grown, x1, x2);
			float ey = Mth.lerp(grown, y1, y2);
			float hover = link.hover();
			boolean thin = scale < 0.5F;
			float width = thin ? 1.0F : (scale < 0.85F ? 1.5F : 2.0F);
			boolean whole = grown >= 0.999F;
			if (t.isVanilla()) {
				int line = link.satisfied() ? -1 : Colors.lerp(-9474193, -4144960, hover);
				if (!thin) {
					CanvasDraw.line(g, x1, y1, ex, ey, width + 2.0F, Colors.scaleAlpha(-16777216, alpha));
				}

				CanvasDraw.line(g, x1, y1, ex, ey, width, Colors.scaleAlpha(line, alpha));
				if (whole && link.leading() && !thin && QuestsConfig.get().flowingLines) {
					CanvasDraw.dashes(g, x1, y1, x2, y2, width, 4.0F, 10.0F, view.seconds() * 18.0F + (float)link.seed(), Colors.scaleAlpha(-171, alpha));
				}
			} else {
				int accent = view.accent();
				int bg = this.groundOf(link.to());
				int color = link.complete()
					? Colors.mix(p.success(), bg, 0.42F)
					: (link.leading() ? Colors.mix(accent, bg, 0.3F) : (link.satisfied() ? Colors.mix(accent, bg, 0.58F) : Colors.mix(p.borderStrong(), bg, 0.25F)));
				if (hover > 0.01F) {
					color = Colors.lerp(color, link.complete() ? p.success() : (link.satisfied() ? accent : p.textMuted()), hover * 0.85F);
				}

				float w = !link.satisfied() && !(hover > 0.5F) ? Math.max(1.0F, width - 1.0F) : width;
				CanvasDraw.line(g, x1, y1, ex, ey, w, Colors.scaleAlpha(color, alpha));
				if (whole && !thin) {
					if (link.leading() && QuestsConfig.get().flowingLines) {
						CanvasDraw.current(
							g,
							x1,
							y1,
							x2,
							y2,
							w,
							9.0F * Math.min(1.4F, scale) + 3.0F,
							34.0F * Math.min(1.4F, scale) + 8.0F,
							view.seconds() * 30.0F + (float)link.seed() * 7.0F,
							Colors.scaleAlpha(Colors.brighten(accent, 0.45F), alpha)
						);
					}

					if (!link.complete() && scale >= 0.7F) {
						CanvasDraw.arrow(g, x2, y2, x2 - x1, y2 - y1, 3.0F * Math.min(1.3F, scale), 0.9F, Colors.scaleAlpha(color, alpha));
					}
				}
			}
		}
	}

	@Override
	public void beginNodes(GuiGraphics g, QuestPainter.View view) {
		this.deferring = true;
		this.putOffCount = 0;
	}

	@Override
	public void endNodes(GuiGraphics g, QuestPainter.View view) {
		if (this.deferring) {
			this.deferring = false;
			int count = this.putOffCount;

			for (int i = 0; i < count; i++) {
				this.picture(g, this.putOffView[i], this.putOff[i], this.putOffAt[i * 4], this.putOffAt[i * 4 + 1], this.putOffAt[i * 4 + 2], this.putOffAt[i * 4 + 3]);
			}

			for (int i = 0; i < count; i++) {
				this.over(g, this.putOffView[i], this.putOff[i], this.putOffAt[i * 4], this.putOffAt[i * 4 + 1], this.putOffAt[i * 4 + 2], this.putOffAt[i * 4 + 3]);
			}

			Arrays.fill(this.putOff, 0, count, null);
			Arrays.fill(this.putOffView, 0, count, null);
			this.putOffCount = 0;
		}
	}

	@Override
	public void node(GuiGraphics g, QuestPainter.View view, NodeView n, float sx, float sy, float scale0, float alpha) {
		if (!(alpha <= 0.01F) && !(scale0 <= 0.01F)) {
			if (this.radius() * scale0 < 5.2F) {
				this.small(g, view, n, sx, sy, scale0, alpha);
			} else {
				this.under(g, view, n, sx, sy, scale0, alpha);
				if (this.deferring) {
					if (this.putOffCount == this.putOff.length) {
						this.putOff = Arrays.copyOf(this.putOff, this.putOffCount * 2);
						this.putOffView = Arrays.copyOf(this.putOffView, this.putOffCount * 2);
						this.putOffAt = Arrays.copyOf(this.putOffAt, this.putOffCount * 8);
					}

					this.putOff[this.putOffCount] = n;
					this.putOffView[this.putOffCount] = view;
					this.putOffAt[this.putOffCount * 4] = sx;
					this.putOffAt[this.putOffCount * 4 + 1] = sy;
					this.putOffAt[this.putOffCount * 4 + 2] = scale0;
					this.putOffAt[this.putOffCount * 4 + 3] = alpha;
					this.putOffCount++;
				} else {
					this.picture(g, view, n, sx, sy, scale0, alpha);
					this.over(g, view, n, sx, sy, scale0, alpha);
				}
			}
		}
	}

	private void small(GuiGraphics g, QuestPainter.View view, NodeView n, float sx, float sy, float scale0, float alpha) {
		boolean van = Theme.current().isVanilla();
		this.tones(view, n);
		float px = this.radius() * scale0;
		int cx = Math.round(sx);
		int cy = Math.round(sy);
		if (px < 2.6F) {
			int half = px < 1.5F ? 0 : 1;
			g.fill(
				cx - half,
				cy - half,
				cx + half + 1,
				cy + half + 1,
				Colors.scaleAlpha(n.locked() ? Colors.mix(this.rim, this.groundOf(n), 0.3F) : this.statusColor(view, n.status), alpha)
			);
		} else {
			Quest.Shape shape = n.advancement() ? Quest.Shape.ROUNDED : n.shape();
			g.pose().pushPose();
			g.pose().translate((float)cx, (float)cy, 0.0F);
			int r = Math.max(2, Math.round(px));
			if (n.selected.get() > 0.01F) {
				CanvasDraw.shape(g, shape, r + 2, Colors.scaleAlpha(van ? -1 : view.accent(), alpha * n.selected.get()));
			}

			int body = n.locked() ? this.fill : Colors.mix(this.fill, this.statusColor(view, n.status), 0.45F);
			CanvasDraw.shape(g, shape, r, Colors.scaleAlpha(body, alpha), Colors.scaleAlpha(this.rim, alpha));
			g.pose().popPose();
		}
	}

	private void under(GuiGraphics g, QuestPainter.View view, NodeView n, float sx, float sy, float scale0, float alpha) {
		Theme t = Theme.current();
		Palette p = t.palette();
		boolean van = t.isVanilla();
		this.tones(view, n);
		int cx = Math.round(sx);
		int cy = Math.round(sy);
		Quest.Shape shape = n.advancement() ? Quest.Shape.ROUNDED : n.shape();
		float hover = n.hover.get();
		float sel = n.selected.get();
		float scale = scale0 * (1.0F + 0.08F * hover);
		int accent = van ? -1 : view.accent();
		float time = view.seconds();
		g.pose().pushPose();
		g.pose().translate((float)cx, (float)cy, 0.0F);
		g.pose().scale(scale, scale, 1.0F);
		if (!van) {
			if (n.status == Rules.Status.AVAILABLE && hover < 0.99F) {
				float breath = 0.5F + 0.5F * Mth.sin(time * 2.1F + (float)n.index * 0.9F);
				CanvasDraw.shape(g, shape, 14, Colors.scaleAlpha(Colors.withAlpha(accent, Math.round(18.0F + 30.0F * breath)), alpha * (1.0F - hover)));
			} else if (n.status == Rules.Status.CLAIMABLE) {
				float breath = 0.5F + 0.5F * Mth.sin(time * 3.2F + (float)n.index);
				CanvasDraw.shape(g, shape, 14, Colors.scaleAlpha(Colors.withAlpha(p.warning(), Math.round(28.0F + 48.0F * breath)), alpha));
			}

			if (hover > 0.01F) {
				int glow = n.locked() ? p.textDim() : (n.done() ? this.rim : accent);
				CanvasDraw.shape(g, shape, 16, Colors.scaleAlpha(Colors.withAlpha(glow, 28), alpha * hover));
				CanvasDraw.shape(g, shape, 14, Colors.scaleAlpha(Colors.withAlpha(glow, 56), alpha * hover));
			}
		}

		if (n.gateway()) {
			float unfolded = n.folds ? n.unfolded.get() : 0.0F;
			int ring = van ? -1 : (n.locked() ? p.borderStrong() : Colors.mix(this.rim, accent, 0.5F));
			if (unfolded > 0.01F && !van) {
				ring = Colors.lerp(ring, n.treeAccent(), unfolded);
			}

			boolean plain = shape == Quest.Shape.GEAR || shape == Quest.Shape.STAR;
			CanvasDraw.rimDashes(
				g,
				plain ? Quest.Shape.CIRCLE : shape,
				plain ? 15 : 14,
				6,
				unfolded > 0.5F ? 6 : 4,
				n.locked() ? 0.0F : time * 7.0F * (1.0F - unfolded),
				Colors.scaleAlpha(Colors.withAlpha(ring, van ? 192 : 208), alpha)
			);
		}

		if (sel > 0.01F) {
			brackets(g, 16 + Math.round(1.2F * Mth.sin(time * 3.0F)), Colors.scaleAlpha(accent, alpha * sel));
		}

		g.pose().pushPose();
		g.pose().translate(1.0F, 2.0F + hover, 0.0F);
		CanvasDraw.shape(g, shape, 11, Colors.scaleAlpha(van ? 1879048192 : p.shadow(), alpha));
		g.pose().popPose();
		if (n.advancement()) {
			advancementFrame(g, n, alpha);
		} else if (n.status == Rules.Status.HIDDEN) {
			CanvasDraw.shape(g, shape, 10, Colors.scaleAlpha(Colors.withAlpha(this.fill, 176), alpha));
			CanvasDraw.rimDashes(g, shape, 11, 4, 2, 0.0F, Colors.scaleAlpha(this.rim, alpha));
		} else {
			CanvasDraw.shape(g, shape, 11, Colors.scaleAlpha(this.fill, alpha), Colors.scaleAlpha(this.rim, alpha));
			if (!van) {
				CanvasDraw.shapeTop(g, shape, 10, 9, Colors.scaleAlpha(218103807, alpha));
			} else {
				CanvasDraw.shapeTop(g, shape, 10, 2, Colors.scaleAlpha(822083583, alpha));
			}
		}

		float progress = n.shownProgress.get();
		if (!n.done() && !n.locked() && progress > 0.01F) {
			int lit = van ? -8399233 : Colors.brighten(accent, 0.15F);
			CanvasDraw.rim(g, shape, 11, progress, Colors.scaleAlpha(lit, alpha));
			CanvasDraw.rim(g, shape, 12, progress, Colors.scaleAlpha(Colors.withAlpha(lit, 112), alpha));
		}

		g.pose().popPose();
	}

	private void picture(GuiGraphics g, QuestPainter.View view, NodeView n, float sx, float sy, float scale0, float alpha) {
		this.tones(view, n);
		float scale = scale0 * (1.0F + 0.08F * n.hover.get());
		int ink = n.advancement() ? -13949406 : this.tint;
		if (n.secret()) {
			QUESTION.draw(g, (float)Math.round(sx), (float)Math.round(sy), 12.0F * scale, Colors.scaleAlpha(ink, alpha));
		} else {
			n.icon().draw(g, (float)Math.round(sx), (float)Math.round(sy), 16.0F * scale * n.quest.iconScale, Colors.scaleAlpha(ink, alpha));
		}
	}

	private void over(GuiGraphics g, QuestPainter.View view, NodeView n, float sx, float sy, float scale0, float alpha) {
		Theme t = Theme.current();
		Palette p = t.palette();
		boolean van = t.isVanilla();
		this.tones(view, n);
		int cx = Math.round(sx);
		int cy = Math.round(sy);
		Quest.Shape shape = n.advancement() ? Quest.Shape.ROUNDED : n.shape();
		float scale = scale0 * (1.0F + 0.08F * n.hover.get());
		float time = view.seconds();
		int ground = this.groundOf(n);
		g.pose().pushPose();
		g.pose().translate((float)cx, (float)cy, 200.0F);
		g.pose().scale(scale, scale, 1.0F);
		float veil = Math.max(n.secret() ? 0.0F : this.dim, 1.0F - alpha);
		if (veil > 0.01F) {
			if (n.advancement()) {
				CanvasDraw.shape(g, Quest.Shape.ROUNDED, 8, Colors.withAlpha(alpha < 0.999F ? ground : -16777216, Math.round(255.0F * veil * 0.45F)));
			} else {
				CanvasDraw.shape(g, shape, 10, Colors.withAlpha(alpha < 0.999F ? Colors.lerp(this.fill, ground, 1.0F - alpha) : this.fill, Math.round(255.0F * veil)));
			}
		}

		if (n.status == Rules.Status.COMPLETE) {
			badge(g, 8, -8, van ? -11141291 : p.success(), CHECK, alpha);
		} else if (n.status == Rules.Status.CLAIMABLE) {
			badge(g, 8, -8 - Math.round(Math.abs(Mth.sin(time * 3.2F)) * 2.0F), van ? -22016 : p.warning(), STAR, alpha);
		} else if (n.locked() && !n.secret()) {
			pixels(g, LOCK, 4, 3, Colors.scaleAlpha(van ? -4144960 : p.textMuted(), alpha));
		}

		if (n.pinned) {
			badge(g, -8, -8, van ? -1 : view.accent(), PIN, alpha);
		}

		if (n.quest.repeatable && !n.locked()) {
			pixels(g, LOOP, -10, 4, Colors.scaleAlpha(van ? -1 : p.textMuted(), alpha));
		}

		if (n.folds) {
			foldMark(g, 0, 12, van ? (n.locked() ? -6250336 : -1) : (n.locked() ? p.textDim() : n.treeAccent()), n.unfolded.get(), alpha);
		}

		g.pose().popPose();
		float near = scale0 / n.size;
		float named = n.folds ? 1.0F - n.unfolded.get() : 1.0F;
		if (near >= 1.3F && !n.secret() && alpha > 0.5F && named > 0.02F) {
			float shown = Mth.clamp((near - 1.3F) / 0.2F, 0.0F, 1.0F) * alpha * named;
			int room = Math.round(n.room * this.unit() * near) - 8;
			if (room >= 24) {
				FormattedCharSequence name = n.caption(Math.min(room, 110));
				int w = n.captionWidth();
				int tx = cx - w / 2;
				int ty = cy + Math.round(this.radius() * scale) + 5 + (n.folds ? Math.round(6.0F * scale) - 2 : 0);
				int color = n.locked()
					? (van ? -6250336 : p.textDim())
					: (n.done() ? Colors.mix(this.statusColor(view, n.status), van ? -1 : p.text(), 0.45F) : (van ? -1 : p.text()));
				g.pose().pushPose();
				g.pose().translate(0.0F, 0.0F, 200.0F);
				g.fill(tx - 2, ty - 1, tx + w + 2, ty + 9, Colors.withAlpha(ground, Math.round((float)(van ? 176 : 144) * shown)));
				g.drawString(CFHGuiDraw.font(), name, tx, ty, Colors.scaleAlpha(color, Math.max(0.03F, shown)), van);
				g.pose().popPose();
			}
		}
	}

	private static void advancementFrame(GuiGraphics g, NodeView n, float alpha) {
		CanvasDraw.advancementFrame(g, n.quest.frame, n.done(), n.locked() ? 0.72F : 1.0F, alpha);
	}

	private static void brackets(GuiGraphics g, int d, int color) {
		int arm = 4;

		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sy = -1; sy <= 1; sy += 2) {
				int x = sx * d;
				int y = sy * d;
				g.fill(Math.min(x, x - sx * 4), y, Math.max(x, x - sx * 4) + 1, y + 1, color);
				g.fill(x, Math.min(y, y - sy * 4), x + 1, Math.max(y, y - sy * 4) + 1, color);
			}
		}
	}

	private static void pixels(GuiGraphics g, String[] rows, int x, int y, int color) {
		if (color >>> 24 != 0) {
			for (int row = 0; row < rows.length; row++) {
				String s = rows[row];
				int from = -1;

				for (int i = 0; i <= s.length(); i++) {
					boolean on = i < s.length() && s.charAt(i) == 'X';
					if (on && from < 0) {
						from = i;
					} else if (!on && from >= 0) {
						g.fill(x + from, y + row, x + i, y + row + 1, color);
						from = -1;
					}
				}
			}
		}
	}

	private static void badge(GuiGraphics g, int x, int y, int color, String[] mark, float alpha) {
		CFHGuiDraw.pixelCircle(g, x, y, 5, Colors.scaleAlpha(-15724528, alpha));
		CFHGuiDraw.pixelCircle(g, x, y, 4, Colors.scaleAlpha(color, alpha));
		pixels(g, mark, x - mark[0].length() / 2, y - mark.length / 2, Colors.scaleAlpha(-15724528, alpha));
	}

	private static void foldMark(GuiGraphics g, int x, int y, int color, float unfolded, float alpha) {
		int ink = Colors.scaleAlpha(-15724528, alpha);
		CFHGuiDraw.pixelCircle(g, x, y, 5, ink);
		CFHGuiDraw.pixelCircle(g, x, y, 4, Colors.scaleAlpha(color, alpha));
		g.fill(x - 2, y, x + 3, y + 1, ink);
		int half = Math.round(2.0F * (1.0F - Mth.clamp(unfolded, 0.0F, 1.0F)));
		if (half > 0) {
			g.fill(x, y - half, x + 1, y + half + 1, ink);
		}
	}

	@Override
	public void opened(GuiGraphics g, QuestPainter.View view, NodeView n, float sx, float sy, float scale, float t) {
		float r = this.radius() * scale;
		if (!(r < 4.0F)) {
			int color = Theme.current().isVanilla() ? -1 : view.accent();

			for (int k = 0; k < 2; k++) {
				float tk = (t - (float)k * 0.22F) / (1.0F - (float)k * 0.22F);
				if (!(tk <= 0.0F) && !(tk >= 1.0F)) {
					float e = 1.0F - (1.0F - tk) * (1.0F - tk);
					CanvasDraw.ring(g, sx, sy, r + 2.0F + e * r * 2.6F, Colors.withAlpha(color, Math.round(208.0F * (1.0F - tk) * (1.0F - tk))));
				}
			}
		}
	}

	@Override
	public void celebrated(GuiGraphics g, QuestPainter.View view, NodeView n, float sx, float sy, float scale, float t) {
		float r = this.radius() * scale;
		if (!(r < 4.0F)) {
			Theme th = Theme.current();
			Palette p = th.palette();
			boolean van = th.isVanilla();
			Quest.Frame frame = n.quest.frame;
			int sparks = frame == Quest.Frame.CHALLENGE ? 46 : (frame == Quest.Frame.GOAL ? 28 : 15);
			float reach = r * (frame == Quest.Frame.CHALLENGE ? 5.2F : (frame == Quest.Frame.GOAL ? 3.8F : 2.7F));
			int[] colors = van ? new int[]{-171, -1, -11141291} : new int[]{p.success(), p.warning(), -1, view.accent()};
			if (t < 0.25F) {
				g.pose().pushPose();
				g.pose().translate((float)Math.round(sx), (float)Math.round(sy), 0.0F);
				g.pose().scale(scale, scale, 1.0F);
				CanvasDraw.shape(g, n.advancement() ? Quest.Shape.ROUNDED : n.shape(), 12, Colors.withAlpha(-1, Math.round(176.0F * (1.0F - t / 0.25F))));
				g.pose().popPose();
			}

			int rings = frame == Quest.Frame.TASK ? 1 : 2;

			for (int k = 0; k < rings; k++) {
				float tk = Mth.clamp((t - (float)k * 0.12F) / 0.55F, 0.0F, 1.0F);
				if (!(tk <= 0.0F) && !(tk >= 1.0F)) {
					float e = 1.0F - (1.0F - tk) * (1.0F - tk) * (1.0F - tk);
					CanvasDraw.ring(g, sx, sy, r + e * reach * 0.8F, Colors.withAlpha(colors[k % colors.length], Math.round(224.0F * (1.0F - tk))));
				}
			}

			if (frame == Quest.Frame.CHALLENGE && t < 0.7F) {
				float tk = t / 0.7F;
				float len = reach * (0.5F + 0.5F * tk);

				for (int i = 0; i < 10; i++) {
					float a = (float)i * (float) (Math.PI * 2) / 10.0F + t * 1.4F;
					CanvasDraw.line(
						g,
						sx + Mth.cos(a) * (r + 3.0F),
						sy + Mth.sin(a) * (r + 3.0F),
						sx + Mth.cos(a) * len,
						sy + Mth.sin(a) * len,
						1.0F,
						Colors.withAlpha(van ? -171 : p.warning(), Math.round(144.0F * (1.0F - tk)))
					);
				}
			}

			float e = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);

			for (int i = 0; i < sparks; i++) {
				float a = spark(n.index, i, 1) * (float) (Math.PI * 2);
				float far = reach * (0.45F + 0.75F * spark(n.index, i, 2));
				float life = 0.6F + 0.4F * spark(n.index, i, 3);
				float tk = t / life;
				if (!(tk >= 1.0F)) {
					float x = sx + Mth.cos(a) * (r * 0.6F + far * e);
					float y = sy + Mth.sin(a) * (r * 0.6F + far * e) + r * 1.6F * t * t;
					int size = tk < 0.5F && scale > 0.8F ? 2 : 1;
					int ix = Math.round(x);
					int iy = Math.round(y);
					g.fill(ix, iy, ix + size, iy + size, Colors.withAlpha(colors[i % colors.length], Math.round(255.0F * (1.0F - tk * tk))));
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

	private void buildCard(QuestPainter.View view, NodeView n) {
		Theme t = Theme.current();
		Palette p = t.palette();
		boolean van = t.isVanilla();
		Font font = CFHGuiDraw.font();
		Quest q = n.quest;
		this.cardNode = n;
		this.cardRevision = BookStats.revision();
		this.cardVanilla = van;
		this.cardUnfolded = unfolded(n);
		this.cardRows.clear();
		int muted = van ? -6250336 : p.textMuted();
		int dimText = van ? -8355712 : p.textDim();
		int text = van ? -1 : p.text();
		int inner = 176;
		Component title = (Component)(n.secret() ? QuestText.secret() : Fonts.heading(QuestText.title(n.entry)));
		this.cardTitle = CFHGuiDraw.truncate(title, 176 - (n.pinned ? 10 : 0));
		Component status = QuestText.status(n.status);
		if (!n.secret()) {
			status = Component.empty().append(status).append(Component.literal("  ·  ").append(QuestText.sync(n.entry)).withStyle(s -> s.withColor(muted & 16777215)));
		}

		this.cardStatus = CFHGuiDraw.truncate(status, 168);
		this.cardStatusColor = this.statusColor(view, n.status);
		int width = Math.max(font.width(this.cardTitle) + (n.pinned ? 10 : 0), font.width(this.cardStatus) + 8);
		if (n.secret()) {
			for (FormattedCharSequence line : font.split(Component.translatable("cfhgui_quests.card.secret"), 176)) {
				this.cardRows.add(new CFHGuiQuestPainter.Row(0, line, muted, null, 0.0F, null, 10));
				width = Math.max(width, font.width(line));
			}
		} else {
			if (!q.subtitle.isBlank()) {
				List<FormattedCharSequence> lines = font.split(QuestText.text(q.subtitle), 176);

				for (int i = 0; i < lines.size() && i < 2; i++) {
					this.cardRows.add(new CFHGuiQuestPainter.Row(0, lines.get(i), muted, null, 0.0F, null, 10));
					width = Math.max(width, font.width(lines.get(i)));
				}
			}

			QuestState state = QuestsState.state(n.key);
			if (!q.tasks.isEmpty()) {
				this.cardRows.add(new CFHGuiQuestPainter.Row(3, null, 0, null, 0.0F, null, 7));
			}

			for (int i = 0; i < q.tasks.size() && i < 4; i++) {
				Task task = q.tasks.get(i);
				long counted = n.done() ? task.count : state.task(i);
				boolean done = Rules.taskDone(task, counted);
				String count = QuestText.count(task, counted).getString();
				FormattedCharSequence right = count.isEmpty() ? null : Component.literal(count).getVisualOrderText();
				int rightW = right == null ? 0 : font.width(right) + 8;
				FormattedCharSequence label = CFHGuiDraw.truncate(QuestText.label(task), 167 - rightW);
				float frac = Rules.taskFraction(task, counted);
				this.cardRows
					.add(new CFHGuiQuestPainter.Row(1, label, done ? (van ? -11141291 : p.success()) : text, right, done ? 1.0F : frac, null, frac > 0.0F && !done ? 13 : 10));
				width = Math.max(width, font.width(label) + 9 + rightW);
			}

			if (q.tasks.size() > 4) {
				this.cardRows
					.add(
						new CFHGuiQuestPainter.Row(
							0, Component.translatable("cfhgui_quests.card.more", new Object[]{q.tasks.size() - 4}).getVisualOrderText(), dimText, null, 0.0F, null, 10
						)
					);
			}

			if (!q.rewards.isEmpty()) {
				List<QuestIcons.Drawable> icons = new ArrayList<>();

				for (int i = 0; i < q.rewards.size() && i < 7; i++) {
					icons.add(QuestIcons.of(q.rewards.get(i)));
				}

				FormattedCharSequence label = Component.translatable("cfhgui_quests.rewards").getVisualOrderText();
				this.cardRows.add(new CFHGuiQuestPainter.Row(3, null, 0, null, 0.0F, null, 7));
				this.cardRows.add(new CFHGuiQuestPainter.Row(2, label, n.status == Rules.Status.CLAIMABLE ? (van ? -22016 : p.warning()) : muted, null, 0.0F, icons, 14));
				width = Math.max(width, font.width(label) + 6 + icons.size() * 14);
			}

			if (n.gateway()) {
				Chapter sub = QuestsState.book() == null ? null : QuestsState.book().chapter(q.subtree);
				BookStats.Stat stat = BookStats.of(sub);
				FormattedCharSequence line = CFHGuiDraw.truncate(Component.translatable("cfhgui_quests.card.subtree", new Object[]{stat.done(), stat.total()}), 176);
				this.cardRows.add(new CFHGuiQuestPainter.Row(0, line, van ? -1 : view.accent(), null, 0.0F, null, 11));
				width = Math.max(width, font.width(line));
			}

			if (n.locked()) {
				Component needs = QuestText.needs(n.entry);
				if (needs != null) {
					List<FormattedCharSequence> lines = font.split(needs, 176);

					for (int i = 0; i < lines.size() && i < 3; i++) {
						this.cardRows.add(new CFHGuiQuestPainter.Row(0, lines.get(i), dimText, null, 0.0F, null, 10));
						width = Math.max(width, font.width(lines.get(i)));
					}
				}
			}
		}

		FormattedCharSequence hint = CFHGuiDraw.truncate(Component.translatable(hintKey(n)), 176);
		this.cardRows.add(new CFHGuiQuestPainter.Row(0, hint, dimText, null, 0.0F, null, 12));
		width = Math.max(width, font.width(hint));
		this.cardWidth = Mth.clamp(width, 104, 176) + 14;
		int h = 29;

		for (CFHGuiQuestPainter.Row r : this.cardRows) {
			h += r.height();
		}

		this.cardHeight = h + 7 - 3;
	}

	private static boolean unfolded(NodeView n) {
		return n.folds && n.unfolded.target() >= 1.0F;
	}

	public static String hintKey(NodeView n) {
		if (n.folds) {
			return unfolded(n) ? "cfhgui_quests.card.hint.fold" : "cfhgui_quests.card.hint.unfold";
		} else {
			return n.gateway() && !n.locked() ? "cfhgui_quests.card.hint.tree" : "cfhgui_quests.card.hint";
		}
	}

	@Override
	public void card(GuiGraphics g, QuestPainter.View view, NodeView n, float nx, float ny, float nr, int side, float alpha, Rect bounds) {
		if (!(alpha <= 0.01F)) {
			Theme t = Theme.current();
			Palette p = t.palette();
			boolean van = t.isVanilla();
			if (this.cardNode != n || this.cardRevision != BookStats.revision() || this.cardVanilla != van || this.cardUnfolded != unfolded(n)) {
				this.buildCard(view, n);
			}

			Font font = CFHGuiDraw.font();
			int w = this.cardWidth;
			int h = this.cardHeight;
			if (w + 8 <= bounds.w() && h + 8 <= bounds.h()) {
				int gap = Math.round(nr * 1.1F) + 12;
				boolean fitsRight = nx + (float)gap + (float)w <= (float)(bounds.right() - 4);
				boolean fitsLeft = nx - (float)gap - (float)w >= (float)(bounds.x() + 4);
				boolean right = side > 0 ? fitsRight || !fitsLeft : (side < 0 ? !fitsLeft && fitsRight : fitsRight || !fitsLeft);
				int x = right ? Math.round(nx) + gap : Math.round(nx) - gap - w;
				x = Mth.clamp(x, bounds.x() + 4, Math.max(bounds.x() + 4, bounds.right() - 4 - w));
				int y = Mth.clamp(Math.round(ny - 14.0F), bounds.y() + 4, Math.max(bounds.y() + 4, bounds.bottom() - 4 - h));
				int slide = Math.round((1.0F - alpha) * 4.0F) * (right ? -1 : 1);
				int cx = x + slide;
				if (van) {
					g.fill(cx - 1, y - 1, cx + w + 1, y + h + 1, Colors.scaleAlpha(-16777216, alpha));
					g.fill(cx, y, cx + w, y + h, Colors.scaleAlpha(-267386864, alpha));
					CFHGuiDraw.outline(g, cx, y, w, h, Colors.scaleAlpha(-13825437, alpha), 0);
				} else {
					CFHGuiDraw.popupPlate(g, cx, y, w, h, p.surface(), p.borderStrong(), 0.6F, alpha);
					g.fill(cx, y + 3, cx + 2, y + h - 3, Colors.scaleAlpha(this.cardStatusColor, alpha));
				}

				int tx = cx + 7;
				int ty = y + 7;
				g.drawString(font, this.cardTitle, tx, ty, Colors.scaleAlpha(van ? -1 : p.text(), alpha), van);
				if (n.pinned) {
					pixels(g, PIN, cx + w - 7 - 5, ty, Colors.scaleAlpha(van ? -1 : view.accent(), alpha));
				}

				ty += 11;
				g.fill(tx, ty + 2, tx + 4, ty + 6, Colors.scaleAlpha(this.cardStatusColor, alpha));
				g.drawString(font, this.cardStatus, tx + 8, ty, Colors.scaleAlpha(this.cardStatusColor, alpha), van);
				ty += 11;
				int inner = w - 14;

				for (CFHGuiQuestPainter.Row r : this.cardRows) {
					switch (r.kind()) {
						case 1:
							boolean done = r.bar() >= 1.0F;
							if (done) {
								pixels(g, CHECK, tx - 1, ty + 1, Colors.scaleAlpha(r.color(), alpha));
							} else {
								g.fill(tx + 2, ty + 3, tx + 5, ty + 6, Colors.scaleAlpha(van ? -6250336 : p.textDim(), alpha));
							}

							g.drawString(font, r.text(), tx + 9, ty, Colors.scaleAlpha(r.color(), alpha), van);
							if (r.right() != null) {
								g.drawString(
									font, r.right(), tx + inner - font.width(r.right()), ty, Colors.scaleAlpha(done ? r.color() : (van ? -6250336 : p.textMuted()), alpha), van
								);
							}

							if (r.height() > 10) {
								g.fill(tx + 9, ty + 10, tx + inner, ty + 11, Colors.scaleAlpha(van ? -16777216 : p.surfaceActive(), alpha));
								g.fill(tx + 9, ty + 10, tx + 9 + Math.round((float)(inner - 9) * r.bar()), ty + 11, Colors.scaleAlpha(van ? -8399233 : view.accent(), alpha));
							}
							break;
						case 2:
							g.drawString(font, r.text(), tx, ty + 3, Colors.scaleAlpha(r.color(), alpha), van);
							int ix = tx + font.width(r.text()) + 6;
							if (alpha > 0.6F && r.icons() != null) {
								for (QuestIcons.Drawable d : r.icons()) {
									d.draw(g, (float)(ix + 6), (float)(ty + 7), 12.0F, van ? -1 : p.textMuted());
									ix += 14;
								}
							}
							break;
						case 3:
							g.fill(tx, ty + 3, tx + inner, ty + 4, Colors.scaleAlpha(van ? -12965286 : p.border(), alpha));
							break;
						default:
							g.drawString(font, r.text(), tx, ty + (r.height() > 10 ? r.height() - 10 : 0), Colors.scaleAlpha(r.color(), alpha), van);
					}

					ty += r.height();
				}
			}
		}
	}

	@Override
	public void previewPlate(GuiGraphics g, QuestPainter.View view, SubtreePreview preview, int x, int y, int w, int h, float open) {
		Theme t = Theme.current();
		Palette p = t.palette();
		boolean van = t.isVanilla();
		Font font = CFHGuiDraw.font();
		int accent = van ? -1 : view.accent();
		float fade = Math.min(1.0F, open * 1.5F);
		int side = preview.side();
		float ex = preview.ownerX() + (float)side * (preview.ownerRadius() + 2.0F);
		float px = side > 0 ? (float)x : (float)(x + w);
		float sy = Mth.clamp(preview.ownerY(), (float)y + 5.0F, (float)(y + h) - 5.0F);
		if (Math.abs(px - ex) > 1.0F) {
			CanvasDraw.line(g, ex, preview.ownerY(), px, sy, 2.0F, Colors.scaleAlpha(Colors.withAlpha(accent, 208), fade));
			if (!van) {
				CanvasDraw.current(g, ex, preview.ownerY(), px, sy, 2.0F, 5.0F, 12.0F, view.seconds() * 26.0F, Colors.scaleAlpha(Colors.brighten(accent, 0.4F), fade));
			}

			int lx = side > 0 ? x - 2 : x + w;
			int ly = Math.round(sy);
			g.fill(lx, ly - 3, lx + 2, ly + 3, Colors.scaleAlpha(accent, fade));
		}

		if (van) {
			this.ground = -15461356;
			g.fill(x - 1, y - 1, x + w + 1, y + h + 1, Colors.scaleAlpha(-16777216, fade));
			g.fill(x, y, x + w, y + h, Colors.scaleAlpha(-200010732, fade));
			CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(-7631989, fade), 0);
		} else {
			this.ground = Colors.mix(Backdrops.ground(view), -16777216, 0.18F) | 0xFF000000;
			if (t.panelAlpha() < 255) {
				CFHGuiDraw.underlay(g, x, y, w, h, t.radius(), fade);
			}

			CFHGuiDraw.shadow(g, x, y, w, h, 0.7F * fade);
			CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(Colors.withAlpha(this.ground, t.panelAlpha() < 255 ? 216 : 255), fade), t.radius());
			CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(Colors.mix(p.borderStrong(), accent, 0.55F), fade), t.radius());
		}

		float words = preview.words();
		if (!(words <= 0.02F) && w >= 40 && h >= 24) {
			g.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
			Rect box = preview.box();
			int bx = box.x();
			int by = box.y();
			int bw = box.w();
			int bh = box.h();
			if (open >= 0.6F) {
				float sky = Mth.clamp((open - 0.6F) / 0.3F, 0.0F, 1.0F);
				g.enableScissor(x + 2, y + 24 - 2, x + w - 2, y + h - 13);
				Backdrops.draw(
					g, new QuestPainter.View(new Rect(bx, by, bw, bh), 0.5F, 0.0F, 0.0F, view.seconds(), -1, -1, view.chapter(), accent), this.unit() * 0.5F, this.ground
				);
				g.fill(bx, by, bx + bw, by + bh, Colors.withAlpha(this.ground, Math.round(255.0F - 102.0F * sky)));
				g.disableScissor();
				g.fillGradient(bx + 1, by + 1, bx + bw - 1, by + 24 - 1, Colors.scaleAlpha(Colors.withAlpha(accent, 48), words), 0);
			}

			String count = preview.done + " / " + preview.total;
			int countW = font.width(count);
			QuestIcons.of(preview.chapter.icon).draw(g, (float)(bx + 8 + 5), (float)(by + 11), 10.0F, Colors.scaleAlpha(accent, words));
			g.drawString(
				font, CFHGuiDraw.truncate(Fonts.heading(preview.title), bw - 16 - countW - 20), bx + 8 + 13, by + 7, Colors.scaleAlpha(van ? -1 : p.text(), words), van
			);
			g.drawString(font, count, bx + bw - 8 - countW, by + 7, Colors.scaleAlpha(van ? -6250336 : accent, words), van);
			int barX = bx + 8;
			int barW = bw - 16;
			int barY = by + 19;
			g.fill(barX, barY, barX + barW, barY + 1, Colors.scaleAlpha(van ? -16777216 : p.surfaceActive(), words));
			float done = preview.total <= 0 ? 0.0F : (float)preview.done / (float)preview.total;
			g.fill(barX, barY, barX + Math.round((float)barW * done * words), barY + 1, Colors.scaleAlpha(van ? -8399233 : accent, words));
			Component hint = Component.translatable("cfhgui_quests.preview.hint");
			g.drawString(font, CFHGuiDraw.truncate(hint, bw - 16), bx + 8, by + bh - 13 + 1, Colors.scaleAlpha(van ? -8355712 : p.textDim(), words), van);
			Component stands = QuestText.status(preview.owner.status);
			int standsW = font.width(stands);
			if (font.width(hint) + standsW + 14 <= bw - 16) {
				g.drawString(font, stands, bx + bw - 8 - standsW, by + bh - 13 + 1, Colors.scaleAlpha(this.statusColor(view, preview.owner.status), words), van);
			}

			g.disableScissor();
		}
	}

	@Override
	public void inlayPlate(
		GuiGraphics g, QuestPainter.View view, Inlay inlay, int x, int y, int w, int h, float hangX, float hangY, float scale, float open, float words, float alpha
	) {
		if (!(alpha <= 0.01F) && w >= 2 && h >= 2) {
			Theme t = Theme.current();
			Palette p = t.palette();
			boolean van = t.isVanilla();
			Font font = CFHGuiDraw.font();
			int accent = van ? -1 : view.accent();
			float unit = this.unit() * scale;
			int fill;
			if (van) {
				fill = -15461356;
				g.fill(x - 1, y - 1, x + w + 1, y + h + 1, Colors.scaleAlpha(-16777216, alpha));
				g.fill(x, y, x + w, y + h, Colors.scaleAlpha(fill, alpha));
				CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(-7631989, alpha), 0);
			} else {
				fill = Colors.mix(Backdrops.ground(view), -16777216, 0.18F) | 0xFF000000;
				CFHGuiDraw.shadow(g, x, y, w, h, 0.55F * alpha);
				CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(fill, alpha), t.radius());
				CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(Colors.mix(p.borderStrong(), accent, 0.55F), alpha), t.radius());
			}

			inlay.ground = fill;
			int head = Math.round(0.75F * unit);
			int hx = Math.round(hangX);
			if (open >= 0.6F && w > 12 && h > 12 && scale >= 0.3F) {
				float sky = Mth.clamp((open - 0.6F) / 0.3F, 0.0F, 1.0F);
				Rect a = view.area();
				float ox = (float)a.x() + (float)a.w() / 2.0F + (inlay.x - view.camX()) * unit;
				float oy = (float)a.y() + (float)a.h() / 2.0F + (inlay.y - view.camY()) * unit;
				QuestPainter.View on = new QuestPainter.View(
					new Rect(x, y, w, h),
					scale,
					((float)x + (float)w / 2.0F - ox) / unit,
					((float)y + (float)h / 2.0F - oy) / unit,
					view.seconds(),
					-1,
					-1,
					view.chapter(),
					accent
				);
				boolean tiled = van || Backdrops.kind(view.chapter()) == Backdrops.Kind.TEXTURE;
				g.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
				if (tiled) {
					Backdrops.veil(g, on, unit, sky * alpha);
				}

				Backdrops.draw(g, on, unit, fill);
				g.fill(x + 1, y + 1, x + w - 1, y + h - 1, Colors.withAlpha(fill, Math.round((tiled ? 88.0F : 255.0F - 116.0F * sky) * alpha)));
				g.disableScissor();
			}

			if (h > head) {
				g.fillGradient(x + 1, y + 1, x + w - 1, y + head, Colors.scaleAlpha(Colors.withAlpha(accent, van ? 32 : 52), alpha), 0);
			}

			int line = Colors.scaleAlpha(Colors.withAlpha(accent, 224), alpha);
			int thick = scale < 0.85F ? 1 : 2;
			int lx = hx - thick / 2;
			g.fill(lx, Math.round(hangY), lx + thick, Math.min(y + h, y + head), line);
			if ((float)y - hangY > 2.0F) {
				g.fill(lx - 2, y - 1, lx + thick + 2, y + 1, line);
			}

			if (!(words <= 0.02F) && !(scale < 0.5F) && h >= head && w >= 40) {
				float ts = Mth.clamp(scale, 0.75F, 1.2F);
				float wa = words * alpha;
				BookStats.Stat stat = BookStats.of(inlay.chapter);
				String count = stat.done() + " / " + stat.total();
				int pad = Math.round(6.0F * ts);
				int countW = Math.round((float)font.width(count) * ts);
				int leftRoom = hx - 5 - (x + pad);
				int rightRoom = x + w - pad - (hx + 6);
				boolean left = leftRoom >= rightRoom;
				int from = left ? x + pad : hx + 6;
				int room = left ? leftRoom : rightRoom;
				boolean counted = stat.total() > 0 && (left ? rightRoom >= countW + 4 : room >= countW + 44);
				int nameRoom = room - 13 - (counted && !left ? countW + 8 : 0);
				if (nameRoom >= 20) {
					g.pose().pushPose();
					g.pose().translate((float)from, (float)y + ((float)head - 9.0F * ts) / 2.0F, 0.0F);
					g.pose().scale(ts, ts, 1.0F);
					QuestIcons.of(inlay.chapter.icon).draw(g, 5.0F, 4.0F, 10.0F, Colors.scaleAlpha(accent, wa));
					g.drawString(
						font, CFHGuiDraw.truncate(Fonts.heading(inlay.title), Math.round((float)nameRoom / ts)), 13, 1, Colors.scaleAlpha(van ? -1 : p.text(), wa), van
					);
					g.pose().popPose();
				}

				if (counted) {
					g.pose().pushPose();
					g.pose().translate((float)(x + w - pad - countW), (float)y + ((float)head - 9.0F * ts) / 2.0F, 0.0F);
					g.pose().scale(ts, ts, 1.0F);
					g.drawString(font, count, 0, 1, Colors.scaleAlpha(stat.complete() ? (van ? -11141291 : p.success()) : (van ? -6250336 : accent), wa), van);
					g.pose().popPose();
				}

				int barX = x + pad;
				int barW = w - pad * 2;
				int barY = y + head - 1;
				g.fill(barX, barY, barX + barW, barY + 1, Colors.scaleAlpha(van ? -16777216 : p.surfaceActive(), wa));
				g.fill(
					barX,
					barY,
					barX + Math.round((float)barW * stat.fraction() * words),
					barY + 1,
					Colors.scaleAlpha(stat.complete() ? (van ? -11141291 : p.success()) : (van ? -8399233 : accent), wa)
				);
			}
		}
	}

	@Override
	public void stem(GuiGraphics g, QuestPainter.View view, LinkView stem, float[] xs, float[] ys, int points, float scale, float alpha, float grown) {
		if (!(alpha <= 0.01F) && !(grown <= 0.01F) && points >= 2) {
			boolean van = Theme.current().isVanilla();
			NodeView to = stem.to();
			float hover = stem.hover();
			int width = scale < 0.85F ? 1 : 2;
			int accent = van ? -1 : view.accent();
			int color = Colors.mix(accent, this.groundOf(to), to.locked() ? 0.62F : 0.22F);
			if (hover > 0.01F) {
				color = Colors.lerp(color, accent, hover * 0.85F);
			}

			color = Colors.scaleAlpha(color, alpha * (0.3F + 0.7F * stem.lit()));
			float total = 0.0F;

			for (int i = 1; i < points; i++) {
				total += Math.abs(xs[i] - xs[i - 1]) + Math.abs(ys[i] - ys[i - 1]);
			}

			int half = width / 2;

			for (int pass = van ? 0 : 1; pass < 2; pass++) {
				int grow = pass == 0 ? 1 : 0;
				int ink = pass == 0 ? Colors.scaleAlpha(-16777216, alpha) : color;
				float left = total * Math.min(1.0F, grown);

				for (int i = 1; i < points && left > 0.0F; i++) {
					float len = Math.abs(xs[i] - xs[i - 1]) + Math.abs(ys[i] - ys[i - 1]);
					float k = len <= 0.0F ? 1.0F : Math.min(1.0F, left / len);
					int x0 = Math.round(xs[i - 1]);
					int y0 = Math.round(ys[i - 1]);
					int x1 = Math.round(xs[i - 1] + (xs[i] - xs[i - 1]) * k);
					int y1 = Math.round(ys[i - 1] + (ys[i] - ys[i - 1]) * k);
					g.fill(Math.min(x0, x1) - half - grow, Math.min(y0, y1) - half - grow, Math.max(x0, x1) - half + width + grow, Math.max(y0, y1) - half + width + grow, ink);
					left -= len;
				}
			}
		}
	}

	private static record Row(
		int kind,
		@Nullable FormattedCharSequence text,
		int color,
		@Nullable FormattedCharSequence right,
		float bar,
		@Nullable List<QuestIcons.Drawable> icons,
		int height
	) {
		static final int TEXT = 0;
		static final int TASK = 1;
		static final int ICONS = 2;
		static final int RULE = 3;
	}
}
