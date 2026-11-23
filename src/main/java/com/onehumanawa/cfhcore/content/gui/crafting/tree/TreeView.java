package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.VanillaEmiRecipeCategories;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStackInteraction;
import com.onehumanawa.cfhcore.content.gui.emi.input.EmiInput;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import com.onehumanawa.cfhcore.content.gui.crafting.CFHGuiCrafting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.LongConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class TreeView {
	static final int ROW = 20;
	static final int PLATE = 18;
	static final int CARET = 8;
	static final int STEP = 12;
	static final int LANE = 6;
	static final int HANDLES = 10;
	private static final int RAW_LINE = 11;
	private static final int NAME_ROOM = 34;
	private static final int MOVE_MS = 150;
	private static final int LIT_MS = 900;
	private static final int TICK_MS = 240;
	private static final int SHAKE_MS = 320;
	private static final int BURST_MS = 1300;
	private final TreeView.Host host;
	private int x;
	private int y;
	private int w;
	private int h;
	@Nullable
	private TreeSession session;
	private int rowsVersion = -1;
	private List<TreeView.Line> lines = List.of();
	private final List<TreeView.Line> gone = new ArrayList<>();
	private Map<String, TreeView.Line> byId = new HashMap<>();
	private final Map<Island, TreeView.Span> spans = new IdentityHashMap<>();
	private long changedAt;
	private boolean settled;
	private int scrolled;
	private int contentHeight;
	private int step = 12;
	private int laneCount;
	private int deepest;
	private final Anim scroll = new Anim(0.0F, 150, Ease.OUT_CUBIC);
	private final Anim gutter = new Anim(0.0F, 150, Ease.OUT_CUBIC);
	private final List<TreeView.Line> visible = new ArrayList<>();
	@Nullable
	private TreeView.Line hovered;
	private TreeView.Zone zone = TreeView.Zone.NONE;
	@Nullable
	private TreeView.Span hoveredSpan;
	private boolean draggingBar;
	private double dragStartY;
	private double dragStartScroll;
	@Nullable
	private String lastClicked;
	private long lastClickAt;
	@Nullable
	private String refusedId;
	@Nullable
	private TreeRows.Refusal refusedWhy;
	private long refusedAt;
	@Nullable
	private Island preview;
	private boolean previewRefused;
	@Nullable
	private String previewFor;
	private static boolean complained;
	@Nullable
	static LongConsumer probe;

	public TreeView(TreeView.Host host) {
		this.host = host;
	}

	public void setBounds(int x, int y, int w, int h) {
		if (this.w != w) {
			for (TreeView.Line line : this.lines) {
				line.nameRoom = -1;
			}
		}

		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
	}

	public boolean contains(double mx, double my) {
		return mx >= (double)this.x && mx < (double)(this.x + this.w) && my >= (double)this.y && my < (double)(this.y + this.h);
	}

	private void sync(@Nullable TreeSession now) {
		boolean other = now != this.session;
		if (now == null) {
			this.session = null;
			this.lines = List.of();
			this.byId = new HashMap<>();
			this.gone.clear();
			this.spans.clear();
			this.rowsVersion = -1;
		} else if (other || this.rowsVersion != now.rowsVersion()) {
			boolean moving = Theme.current().motion() > 0.0F;
			List<TreeRow> rows = now.rows();
			Map<String, TreeView.Line> old = (Map<String, TreeView.Line>)(other ? new HashMap<>() : this.byId);
			Map<String, TreeView.Line> next = new HashMap<>(rows.size() * 2);
			List<TreeView.Line> made = new ArrayList<>(rows.size());
			int top = 0;
			this.deepest = 0;

			for (TreeRow row : rows) {
				TreeView.Line line = new TreeView.Line(row);
				line.height = row.isIsland() ? islandHeight(row) : 20;
				line.top = top;
				top += line.height;
				this.deepest = Math.max(this.deepest, row.depth);
				TreeView.Line was = old.remove(row.id);
				if (was != null) {
					line.fromTop = was.curTop;
					line.fromAlpha = was.alpha;
					line.hover = was.hover;
					line.countWas = was.countWas;
					line.count = was.count;
					line.countLooks = was.countLooks;
				} else if (moving) {
					line.entering = true;
					line.fromAlpha = 0.0F;
					line.fromTop = (float)(line.top + (other ? 6 : -9));
					line.delay = other ? Math.min(row.index * 14, 196) : 0;
				} else {
					line.fromTop = (float)line.top;
				}

				line.curTop = line.fromTop;
				line.alpha = line.fromAlpha;
				made.add(line);
				next.put(row.id, line);
			}

			this.gone.clear();
			if (moving && !other) {
				for (TreeView.Line was : old.values()) {
					if (!was.leaving && !(was.alpha < 0.05F)) {
						was.leaving = true;
						was.fromTop = was.curTop;
						was.fromAlpha = was.alpha;
						was.top = Math.round(was.curTop) - 6;
						this.gone.add(was);
					}
				}
			}

			this.lines = made;
			this.byId = next;
			this.contentHeight = top + 3;
			this.session = now;
			this.rowsVersion = now.rowsVersion();
			this.changedAt = Clock.nowMs();
			this.settled = false;
			this.spans.clear();
			this.laneCount = 0;

			for (TreeView.Line line : made) {
				List<Island> within = line.row.within;

				for (int lane = 0; lane < within.size(); lane++) {
					TreeView.Span span = this.spans.computeIfAbsent(within.get(lane), TreeView.Span::new);
					if (span.first < 0) {
						span.first = line.row.index;
					}

					span.last = line.row.index;
					span.lane = lane;
					this.laneCount = Math.max(this.laneCount, lane + 1);
				}
			}

			if (other) {
				this.scroll.snap(0.0F);
			}

			this.preview = null;
			this.previewFor = null;
		}
	}

	private static int islandHeight(TreeRow row) {
		for (TreeNode node : row.hidden) {
			if (!node.step()) {
				return 31;
			}
		}

		return 20;
	}

	private void measure() {
		int room = this.w - 4 - this.laneCount * 6 - 8 - 18 - 34;
		this.step = this.deepest <= 0 ? 12 : Mth.clamp(room / this.deepest, 4, 12);
	}

	private int maxScroll() {
		return Math.max(0, this.contentHeight - this.h);
	}

	private boolean scrollable() {
		return this.maxScroll() > 0;
	}

	private int gutterWidth() {
		return Math.round(this.gutter.get());
	}

	private int left() {
		return this.x + 2 + this.gutterWidth();
	}

	private int right() {
		return this.x + this.w - 2 - (this.scrollable() ? 5 : 0);
	}

	private int cx(int depth) {
		return this.left() + depth * this.step;
	}

	private boolean names() {
		return TreeConfig.get().names && this.w >= 120;
	}

	private void place() {
		this.scrolled = Math.round(this.scroll.get());
		if (!this.settled || !this.gone.isEmpty()) {
			long now = Clock.nowMs();
			float duration = (float)Theme.current().ms(150);
			boolean moving = false;

			for (TreeView.Line line : this.lines) {
				moving |= this.place(line, now, duration);
			}

			for (TreeView.Line line : this.gone) {
				this.place(line, now, duration);
			}

			this.gone.removeIf(linex -> linex.alpha <= 0.01F);
			this.settled = !moving && this.gone.isEmpty();
		}
	}

	private boolean place(TreeView.Line line, long now, float duration) {
		float t = duration <= 0.0F ? 1.0F : Mth.clamp((float)(now - this.changedAt - (long)line.delay) / duration, 0.0F, 1.0F);
		float e = Ease.OUT_CUBIC.apply(t);
		line.curTop = Mth.lerp(e, line.fromTop, (float)line.top);
		if (line.leaving) {
			line.alpha = line.fromAlpha * (1.0F - smooth(t / 0.55F));
		} else if (line.entering) {
			line.alpha = smooth((t - 0.25F) / 0.75F);
		} else {
			line.alpha = Mth.lerp(e, line.fromAlpha, 1.0F);
		}

		if (t >= 1.0F && !line.leaving) {
			line.entering = false;
			line.fromTop = (float)line.top;
			line.fromAlpha = 1.0F;
			line.curTop = (float)line.top;
			line.alpha = 1.0F;
		}

		line.sy = this.y + Math.round(line.curTop) - this.scrolled;
		return t < 1.0F || line.leaving;
	}

	private TreeView.Line line(int index) {
		return this.placed(this.lines.get(index));
	}

	private TreeView.Line placed(TreeView.Line line) {
		if (this.settled && !line.leaving) {
			line.sy = this.y + line.top - this.scrolled;
		}

		return line;
	}

	private void inSight() {
		this.visible.clear();

		for (TreeView.Line line : this.gone) {
			if (line.sy + line.height > this.y && line.sy < this.y + this.h) {
				this.visible.add(line);
			}
		}

		if (!this.settled) {
			for (TreeView.Line linex : this.lines) {
				if (linex.sy + linex.height > this.y && linex.sy < this.y + this.h) {
					this.visible.add(linex);
				}
			}
		} else {
			int lo = 0;
			int hi = this.lines.size();

			while (lo < hi) {
				int mid = lo + hi >>> 1;
				TreeView.Line at = this.lines.get(mid);
				if (at.top + at.height <= this.scrolled) {
					lo = mid + 1;
				} else {
					hi = mid;
				}
			}

			for (int i = lo; i < this.lines.size(); i++) {
				TreeView.Line linexx = this.line(i);
				if (linexx.sy >= this.y + this.h) {
					break;
				}

				this.visible.add(linexx);
			}
		}
	}

	private static float smooth(float t) {
		float k = Mth.clamp(t, 0.0F, 1.0F);
		return k * k * (3.0F - 2.0F * k);
	}

	public void render(GuiGraphics g, @Nullable TreeSession tree, int mouseX, int mouseY, float delta) {
		LongConsumer timing = probe;
		long began = timing != null ? System.nanoTime() : 0L;
		this.draw(g, tree, mouseX, mouseY, delta);
		if (timing != null) {
			timing.accept(System.nanoTime() - began);
		}
	}

	private void draw(GuiGraphics g, @Nullable TreeSession tree, int mouseX, int mouseY, float delta) {
		this.sync(tree);
		if (this.session != null && this.w > 0 && this.h > 0) {
			TreeSession s = this.session;
			this.measure();
			this.gutter.set((float)(this.laneCount * 6 + (s.foldMode ? 10 : 0)));
			this.scroll.set(Mth.clamp(this.scroll.target(), 0.0F, (float)this.maxScroll()));
			if (this.scroll.get() > (float)(this.maxScroll() + 20)) {
				this.scroll.snap((float)this.maxScroll());
			}

			this.place();
			this.inSight();
			this.hover(mouseX, mouseY);
			this.preview(s);
			g.enableScissor(this.x, this.y, this.x + this.w, this.y + this.h);
			g.drawManaged(() -> {
				this.rails(g, s);
				this.brackets(g, s);

				for (TreeView.Line linex : this.visible) {
					this.flat(g, s, linex, mouseX, mouseY);
				}
			});

			for (TreeView.Line line : this.visible) {
				this.items(g, s, line, delta);
			}

			g.pose().pushPose();
			g.pose().translate(0.0F, 0.0F, 260.0F);
			g.drawManaged(() -> {
				for (TreeView.Line linex : this.visible) {
					this.over(g, s, linex);
				}

				this.burst(g, s);
			});
			g.pose().popPose();
			g.disableScissor();
			this.scrollbar(g, mouseX, mouseY);
		}
	}

	private void hover(int mouseX, int mouseY) {
		TreeView.Line was = this.hovered;
		this.hovered = null;
		this.hoveredSpan = null;
		this.zone = TreeView.Zone.NONE;
		boolean inside = this.contains((double)mouseX, (double)mouseY) && !Popups.any() && !this.draggingBar && mouseX < this.right() + 2;
		if (inside) {
			for (TreeView.Span span : this.spans.values()) {
				TreeView.Line head = span.first >= 0 && span.first < this.lines.size() ? this.line(span.first) : null;
				if (head != null) {
					int hx = this.laneX(span.lane);
					int hy = head.sy + 10;
					if (Math.abs(mouseX - hx) <= 4 && Math.abs(mouseY - hy) <= 5) {
						this.hoveredSpan = span;
						break;
					}
				}
			}

			if (this.hoveredSpan == null) {
				for (TreeView.Line line : this.visible) {
					if (!line.leaving && mouseY >= line.sy && mouseY < line.sy + line.height) {
						this.hovered = line;
						this.zone = this.zoneOf(line, mouseX, mouseY);
						break;
					}
				}
			}
		}

		if (was != null && was != this.hovered) {
			was.hover.set(0.0F);
		}

		if (this.hovered != null) {
			this.hovered.hover.set(1.0F);
		}
	}

	private TreeView.Zone zoneOf(TreeView.Line line, int mouseX, int mouseY) {
		TreeSession s = this.session;
		if (s == null) {
			return TreeView.Zone.NONE;
		} else if (craftShown(s, line) && mouseX >= this.right() - 16 && mouseX < this.right() && mouseY >= line.sy + 2 && mouseY < line.sy + 18) {
			return TreeView.Zone.CRAFT;
		} else if (line.row.isIsland()) {
			return TreeView.Zone.BODY;
		} else {
			int cx = this.cx(line.row.depth);
			if (s.foldMode) {
				return TreeView.Zone.HANDLE;
			} else {
				return line.row.hasChildren && mouseX >= cx - 2 && mouseX < cx + 8 ? TreeView.Zone.CARET : TreeView.Zone.BODY;
			}
		}
	}

	private static boolean craftShown(TreeSession s, TreeView.Line line) {
		if (line.leaving) {
			return false;
		} else if (line.row.isIsland()) {
			return s.fold(line.row).next != null;
		} else {
			TreeFigures f = s.figures(line.row.node.path);
			return f.state == TreeFigures.State.READY && f.here && line.row.node.step();
		}
	}

	private void preview(TreeSession s) {
		String at = s.foldMode && s.firstPoint != null && this.hovered != null && this.hovered.row.node != null ? this.hovered.row.node.path : null;
		if (at == null || at.equals(s.firstPoint)) {
			this.preview = null;
			this.previewFor = null;
		} else if (!at.equals(this.previewFor)) {
			this.previewFor = at;
			TreeRows.Refusal refusal = TreeRows.refusal(s.outline(), s.marks.islands, s.firstPoint, at);
			this.preview = new Island(s.firstPoint, at);
			this.previewRefused = refusal != null && refusal != TreeRows.Refusal.ALREADY_THERE;
		}
	}

	private int laneX(int lane) {
		return this.x + 2 + lane * 6 + 3;
	}

	private static int railColor(TreeFigures f) {
		return switch (f.state) {
			case DONE -> f.unasked() ? TreePaint.rail() : Colors.mix(TreePaint.rail(), TreePaint.success(), TreePaint.vanilla() ? 0.7F : 0.5F);
			case READY -> TreePaint.vanilla() ? -1 : TreePaint.accent();
			default -> TreePaint.rail();
		};
	}

	private TreeFigures figuresOf(TreeSession s, TreeView.Line line) {
		return line.row.node != null ? s.figures(line.row.node.path) : null;
	}

	private int railEnd(TreeView.Line line) {
		int cx = this.cx(line.row.depth);
		return !line.row.isIsland() && !line.row.hasChildren ? cx + 8 - 1 : cx - 1;
	}

	private void rails(GuiGraphics g, TreeSession s) {
		if (!this.lines.isEmpty()) {
			TreeView.Line last = null;

			for (TreeView.Line line : this.visible) {
				if (!line.leaving) {
					this.rail(g, s, line);
					last = line;
				}
			}

			for (TreeView.Line up = last; up != null; up = up.row.parent >= 0 ? this.line(up.row.parent) : null) {
				if (up.row.nextSibling >= 0) {
					TreeView.Line next = this.line(up.row.nextSibling);
					if (next.sy >= this.y + this.h) {
						this.rail(g, s, next);
					}
				}
			}
		}
	}

	private void rail(GuiGraphics g, TreeSession s, TreeView.Line line) {
		if (line.row.parent >= 0) {
			TreeView.Line parent = this.line(line.row.parent);
			int trunk = this.cx(parent.row.depth) + 3;
			int mid = line.sy + 10;
			int from;
			if (line.row.prevSibling >= 0) {
				from = this.line(line.row.prevSibling).sy + 10 - 1;
			} else {
				from = parent.sy + (parent.row.isIsland() ? parent.height - 1 : 15);
			}

			int end = this.railEnd(line);
			TreeFigures f = line.row.isIsland() ? null : this.figuresOf(s, line);
			boolean ready = f != null ? f.state == TreeFigures.State.READY : s.fold(line.row).ready;
			int color = Colors.scaleAlpha(f != null ? railColor(f) : (ready ? (TreePaint.vanilla() ? -1 : TreePaint.accent()) : TreePaint.rail()), line.alpha);
			TreePaint.trunk(g, trunk, Math.max(from, this.y - 4), mid - 1, Colors.scaleAlpha(TreePaint.rail(), line.alpha));
			TreePaint.rail(g, trunk, mid - 1, mid, end, color);
			if (ready && Theme.current().motion() > 0.0F && line.alpha > 0.9F) {
				int top = Math.max(from, this.y - 4);
				int length = TreePaint.railLength(trunk, top, mid, end);
				float at = (float)((Clock.nowMs() + (long)line.row.index * 137L) % 2600L) / 2600.0F * (float)(length + 40);
				TreePaint.railLight(g, trunk, top, mid, end, at, 7, Colors.brighten(TreePaint.accent(), TreePaint.vanilla() ? 0.0F : 0.45F));
			}
		}
	}

	private void brackets(GuiGraphics g, TreeSession s) {
		if (this.laneCount > 0) {
			float shown = Mth.clamp(this.gutter.get() / (float)(this.laneCount * 6), 0.0F, 1.0F);
			if (!(shown <= 0.02F)) {
				for (TreeView.Span span : this.spans.values()) {
					if (span.first >= 0 && span.first < this.lines.size() && span.last < this.lines.size()) {
						TreeView.Line head = this.line(span.first);
						TreeView.Line tail = this.line(span.last);
						int lx = this.laneX(span.lane);
						boolean hot = this.hoveredSpan == span;
						int color = Colors.scaleAlpha(hot ? TreePaint.accent() : Colors.withAlpha(TreePaint.accent(), TreePaint.vanilla() ? 176 : 154), shown);
						int top = head.sy + 10 + 5;
						int bottom = tail.sy + tail.height - 5;

						for (TreeView.Line line : this.visible) {
							if (!line.leaving && line.row.index >= span.first && line.row.index <= span.last) {
								int a = Math.max(top, line.sy);
								int b = Math.min(bottom, line.sy + line.height);
								if (b > a) {
									if (line.row.within.contains(span.island)) {
										g.fill(lx, a, lx + 1, b, color);
									} else {
										TreePaint.dashesDown(g, lx, a, b, 1, 2, color);
									}
								}
							}
						}

						if (bottom > this.y && bottom < this.y + this.h + 4) {
							g.fill(lx + 1, bottom, lx + 2, bottom + 1, color);
							g.fill(lx + 2, bottom + 1, lx + 4, bottom + 2, color);
						}

						int hy = head.sy + 10;
						if (hy > this.y - 6 && hy < this.y + this.h + 6) {
							TreePaint.pixelsCentered(g, TreePaint.DIAMOND, lx, hy, Colors.scaleAlpha(TreePaint.vanilla() ? -16777216 : Theme.current().palette().bg(), shown));
							TreePaint.pixelsCentered(
								g,
								hot ? TreePaint.DIAMOND : TreePaint.DIAMOND_HOLLOW,
								lx,
								hy,
								Colors.scaleAlpha(hot ? Colors.brighten(TreePaint.accent(), 0.2F) : TreePaint.accent(), shown)
							);
						}
					}
				}
			}
		}
	}

	private void flat(GuiGraphics g, TreeSession s, TreeView.Line line, int mouseX, int mouseY) {
		float a = line.alpha;
		if (!(a <= 0.01F)) {
			Theme t = Theme.current();
			Palette p = t.palette();
			boolean van = t.isVanilla();
			TreeRow row = line.row;
			int sy = line.sy;
			int shake = this.shakeOf(line);
			int rowLeft = this.x + 1 + this.gutterWidth();
			int rowRight = this.right();
			float hov = line.hover.get();
			boolean selected = row.node != null && row.node.path.equals(s.selected);
			if (selected) {
				CFHGuiDraw.pixelRound(
					g, rowLeft, sy, rowRight - rowLeft, line.height, Colors.scaleAlpha(van ? 1358954495 : Colors.withAlpha(p.surfaceActive(), 208), a), van ? 0 : 2
				);
				g.fill(rowLeft, sy + 3, rowLeft + 2, sy + line.height - 3, Colors.scaleAlpha(TreePaint.accent(), a));
			} else if (hov > 0.01F) {
				CFHGuiDraw.pixelRound(
					g, rowLeft, sy, rowRight - rowLeft, line.height, Colors.scaleAlpha(van ? 822083583 : Colors.withAlpha(p.surfaceHover(), 176), a * hov), van ? 0 : 2
				);
			}

			if (this.preview != null && row.node != null) {
				int wash = this.previewRefused ? TreePaint.danger() : TreePaint.accent();
				if (this.preview.hides(row.node.path)) {
					g.fill(rowLeft, sy, rowRight, sy + line.height, Colors.scaleAlpha(Colors.withAlpha(wash, 38), a));
				} else if (row.node.path.equals(this.preview.a) || row.node.path.equals(this.preview.b)) {
					CFHGuiDraw.outline(g, rowLeft, sy, rowRight - rowLeft, line.height, Colors.scaleAlpha(Colors.withAlpha(wash, 192), a), van ? 0 : 2);
				}
			}

			if (row.isIsland()) {
				this.island(g, s, line, shake, mouseX, mouseY);
			} else {
				TreeNode node = row.node;
				TreeFigures f = s.figures(node.path);
				boolean done = f.state == TreeFigures.State.DONE;
				boolean quiet = done || f.state == TreeFigures.State.COVERED;
				int cx = this.cx(row.depth) + shake;
				int plateX = cx + 8;
				Long lit = s.lit.get(node.path);
				float glow = lit != null && !(t.motion() <= 0.0F) ? 1.0F - Mth.clamp((float)(Clock.nowMs() - lit) / (float)t.ms(900), 0.0F, 1.0F) : 0.0F;
				if (glow > 0.01F) {
					g.fill(rowLeft, sy, rowRight, sy + line.height, Colors.scaleAlpha(Colors.withAlpha(TreePaint.accent(), 64), a * glow * glow));
				}

				if (s.foldMode || this.gutter.get() > (float)(this.laneCount * 6) + 0.5F) {
					float shown = Mth.clamp((this.gutter.get() - (float)(this.laneCount * 6)) / 10.0F, 0.0F, 1.0F);
					int hx = this.x + 2 + this.laneCount * 6 + 5 - 1;
					int hy = sy + 10;
					boolean first = node.path.equals(s.firstPoint);
					if (first) {
						float beat = t.motion() <= 0.0F ? 1.0F : 0.6F + 0.4F * Mth.sin((float)Clock.nowMs() / 180.0F);
						CFHGuiDraw.outline(g, hx - 4, hy - 4, 9, 9, Colors.scaleAlpha(Colors.withAlpha(TreePaint.accent(), Math.round(192.0F * beat)), a * shown), van ? 0 : 2);
						TreePaint.pixelsCentered(g, TreePaint.DIAMOND, hx, hy, Colors.scaleAlpha(TreePaint.accent(), a * shown));
					} else {
						TreePaint.pixelsCentered(
							g,
							hov > 0.5F ? TreePaint.DIAMOND : TreePaint.DIAMOND_HOLLOW,
							hx,
							hy,
							Colors.scaleAlpha(Colors.lerp(TreePaint.dim(), TreePaint.accent(), hov), a * shown)
						);
					}
				}

				if (row.hasChildren) {
					int caret = Colors.scaleAlpha(Colors.lerp(TreePaint.muted(), TreePaint.text(), this.zone == TreeView.Zone.CARET && this.hovered == line ? 1.0F : 0.0F), a);
					if (row.folded) {
						TreePaint.pixels(g, TreePaint.CARET_RIGHT, cx + 2, sy + 8, caret);
					} else {
						TreePaint.pixels(g, TreePaint.CARET_DOWN, cx + 1, sy + 9, caret);
					}
				}

				int ring = f.state == TreeFigures.State.READY ? (van ? -1 : p.accent()) : (node.given ? TreePaint.muted() : 0);
				TreePaint.plate(g, plateX, sy + 1, hov, done && !f.unasked(), ring, node.given, a);
				if (f.state == TreeFigures.State.READY && !van && t.motion() > 0.0F) {
					float breath = 0.5F + 0.5F * Mth.sin((float)Clock.nowMs() / 420.0F + (float)row.index);
					CFHGuiDraw.outline(g, plateX - 1, sy, 20, 20, Colors.scaleAlpha(Colors.withAlpha(p.accent(), Math.round(24.0F + 52.0F * breath)), a), 3);
				}

				int rightEdge = this.stationColumn() ? rowRight - 19 : rowRight - 2;
				if (craftShown(s, line)) {
					craftButton(g, rowRight - 16, sy + 2, this.hovered == line && this.zone == TreeView.Zone.CRAFT ? 1.0F : 0.0F, a);
					rightEdge = rowRight - 19;
				}

				boolean names = this.names();
				int textY = sy + 6;

				int countColor = Colors.scaleAlpha(switch (f.state) {
					case DONE -> TreePaint.success();
					case READY -> TreePaint.text();
					case MISSING, GIVEN -> TreePaint.warning();
					case COVERED -> TreePaint.dim();
					default -> f.have() > 0L ? TreePaint.text() : TreePaint.muted();
				}, a);
				String count = this.count(line, f, node.ingredient(), s.looks());
				boolean showTick = done && !f.unasked();
				int countW = count == null ? (showTick ? 7 : 0) : CFHGuiDraw.width(count);
				int countX = names ? rightEdge - countW : plateX + 18 + 4;
				if (showTick) {
					TreePaint.pixels(g, TreePaint.TICK, names ? rightEdge - 7 : plateX + 18 + 4, sy + 7, Colors.scaleAlpha(TreePaint.success(), a));
				} else if (count != null && countX + countW <= rowRight) {
					Long ticked = s.ticked.get(node.path);
					float tick = ticked != null && !(t.motion() <= 0.0F) && line.countWas != null
						? Mth.clamp((float)(Clock.nowMs() - ticked) / (float)t.ms(240), 0.0F, 1.0F)
						: 1.0F;
					if (tick < 1.0F) {
						float e = Ease.OUT_CUBIC.apply(tick);
						int wasW = CFHGuiDraw.width(line.countWas);
						TreePaint.text(g, line.countWas, names ? rightEdge - wasW : countX, textY - Math.round(5.0F * e), Colors.scaleAlpha(countColor, 1.0F - e));
						TreePaint.text(g, count, countX, textY + Math.round(5.0F * (1.0F - e)), Colors.scaleAlpha(countColor, Math.max(0.05F, e)));
					} else {
						TreePaint.text(g, count, countX, textY, countColor);
					}
				}

				if (names) {
					int nameX = plateX + 18 + 4;
					int room = rightEdge - countW - 5 - nameX;
					if (room >= 34) {
						if (line.nameRoom != room || line.name == null) {
							line.name = CFHGuiDraw.truncate(TreeActions.name(node.ingredient()), room);
							line.nameRoom = room;
						}

						int nameColor = quiet ? TreePaint.dim() : (node.given ? TreePaint.muted() : Colors.lerp(van ? -1513240 : p.text(), -1, hov * 0.4F));
						TreePaint.text(g, line.name, nameX, textY, Colors.scaleAlpha(nameColor, a));
					}
				}

				float fraction = f.fraction();
				if (!quiet && fraction > 0.0F && fraction < 1.0F) {
					TreePaint.bar(
						g,
						plateX,
						sy + 20 - 1,
						Math.max(8, rightEdge - plateX),
						1,
						fraction,
						Colors.scaleAlpha(van ? 1610612736 : Colors.withAlpha(p.borderStrong(), 144), a),
						Colors.scaleAlpha(f.state != TreeFigures.State.MISSING && f.state != TreeFigures.State.GIVEN ? TreePaint.accent() : TreePaint.warning(), a)
					);
				}
			}
		}
	}

	private boolean stationColumn() {
		return TreeConfig.get().workstations && this.w >= 190 && this.names();
	}

	private boolean stationShown(TreeNode node, TreeFigures f) {
		return this.stationColumn()
			&& node.step()
			&& node.recipe() != null
			&& node.recipe().getCategory() != VanillaEmiRecipeCategories.CRAFTING
			&& f.state != TreeFigures.State.DONE
			&& f.state != TreeFigures.State.COVERED;
	}

	@Nullable
	private String count(TreeView.Line line, TreeFigures f, EmiIngredient ingredient, int looks) {
		if (line.countLooks == looks) {
			return line.count;
		} else {
			String now;
			if (f.state != TreeFigures.State.DONE && f.wanted > 0L) {
				if (f.have() <= 0L) {
					now = "x" + TreeTips.amount(ingredient, f.wanted, f.chance);
				} else {
					now = TreeTips.amount(ingredient, f.have(), f.chance) + "/" + TreeTips.amount(ingredient, f.wanted, f.chance).replace("~", "");
				}
			} else {
				now = null;
			}

			if (line.countLooks >= 0 && line.count != null && !line.count.equals(now)) {
				line.countWas = line.count;
			}

			line.count = now;
			line.countLooks = looks;
			return now;
		}
	}

	private int shakeOf(TreeView.Line line) {
		if (this.refusedId != null && this.refusedId.equals(line.row.id)) {
			float t = (float)(Clock.nowMs() - this.refusedAt) / 320.0F;
			return !(t >= 1.0F) && !(Theme.current().motion() <= 0.0F) ? Math.round(Mth.sin(t * (float) (Math.PI * 2) * 3.0F) * 3.0F * (1.0F - t)) : 0;
		} else {
			return 0;
		}
	}

	private static void craftButton(GuiGraphics g, int x, int y, float hover, float alpha) {
		Theme t = Theme.current();
		Palette p = t.palette();
		if (t.isVanilla()) {
			CFHGuiDraw.vanillaButton(g, x, y, 16, 16, hover, true, alpha);
			Icons.draw(g, Icon.HAMMER, x + 4, y + 4, 8, Colors.scaleAlpha(-1, alpha));
		} else {
			int fill = Colors.lerp(p.accent(), p.accentHover(), hover);
			CFHGuiDraw.shadow(g, x, y, 16, 16, 0.35F * alpha);
			CFHGuiDraw.pixelRound(g, x, y, 16, 16, Colors.scaleAlpha(fill, alpha), 2);
			CFHGuiDraw.outline(g, x, y, 16, 16, Colors.scaleAlpha(Colors.brighten(fill, 0.15F), alpha), 2);
			Icons.draw(g, Icon.HAMMER, x + 4, y + 4, 8, Colors.scaleAlpha(p.accentText(), alpha));
		}
	}

	private void island(GuiGraphics g, TreeSession s, TreeView.Line line, int shake, int mouseX, int mouseY) {
		Theme t = Theme.current();
		Palette p = t.palette();
		boolean van = t.isVanilla();
		float a = line.alpha;
		float hov = line.hover.get();
		TreeFigures.Fold fold = s.fold(line.row);
		int cx = this.cx(line.row.depth) + shake;
		int sy = line.sy;
		int right = this.right() - 1;
		int cw = right - cx;
		int ch = line.height - 2;
		if (cw >= 24) {
			boolean complete = fold.complete();
			int edge = complete ? TreePaint.success() : (fold.ready ? TreePaint.accent() : (van ? -3750202 : Colors.lerp(p.textDim(), p.textMuted(), hov)));
			int fill = van ? Colors.lerp(1476395008, 1881153568, hov) : Colors.withAlpha(Colors.lerp(p.surface(), p.surfaceHover(), hov), 200);
			CFHGuiDraw.pixelRound(g, cx, sy + 1, cw, ch, Colors.scaleAlpha(fill, a), van ? 0 : 3);
			float walk = fold.ready && t.motion() > 0.0F ? (float)Clock.nowMs() / 110.0F : 0.0F;
			TreePaint.dashes(g, cx, sy + 1, cw, ch, van ? 0 : 3, 3, 2, walk, Colors.scaleAlpha(edge, a));
			TreePaint.pixelsCentered(g, TreePaint.DIAMOND, cx + 7, sy + 10, Colors.scaleAlpha(edge, a));
			int rightEdge = right - 3;
			if (craftShown(s, line)) {
				craftButton(g, this.right() - 16, sy + 2, this.hovered == line && this.zone == TreeView.Zone.CRAFT ? 1.0F : 0.0F, a);
				rightEdge = this.right() - 19;
			}

			String progress = fold.done + "/" + fold.steps;
			int pw = CFHGuiDraw.width(progress);
			boolean roomForProgress = cw >= 60;
			if (roomForProgress) {
				TreePaint.text(g, progress, rightEdge - pw, sy + 6, Colors.scaleAlpha(complete ? TreePaint.success() : TreePaint.muted(), a));
				rightEdge -= pw + 4;
			}

			Component label = this.names() && cw >= 150
				? TreeTips.counted("cfhgui_crafting.tree.island.steps", (long)fold.steps, fold.steps)
				: Component.translatable("cfhgui_crafting.tree.island.steps.short", new Object[]{fold.steps});
			int lx = cx + 13;
			int labelRoom = rightEdge - lx;
			if (labelRoom >= CFHGuiDraw.width(label)) {
				TreePaint.text(g, label.getVisualOrderText(), lx, sy + 6, Colors.scaleAlpha(van ? -1 : Colors.lerp(p.textMuted(), p.text(), hov), a));
			}

			if (fold.steps > 0 && fold.done > 0 && !complete) {
				TreePaint.bar(
					g,
					cx + 4,
					sy + 20 - 2,
					cw - 8,
					1,
					fold.fraction(),
					Colors.scaleAlpha(van ? 1610612736 : Colors.withAlpha(p.borderStrong(), 144), a),
					Colors.scaleAlpha(TreePaint.accent(), a)
				);
			}

			if (line.height > 20) {
				int rx = cx + 6;
				int ry = sy + 20 + 1;

				for (TreeFigures.Raw raw : fold.raws) {
					String text = rawText(raw);
					int tw = CFHGuiDraw.width(text);
					if (rx + 9 + tw > right - 12) {
						TreePaint.pixels(g, TreePaint.DOTS, rx, ry + 3, Colors.scaleAlpha(TreePaint.dim(), a));
						break;
					}

					TreePaint.text(g, text, rx + 9, ry, Colors.scaleAlpha(raw.missing > 0L ? TreePaint.warning() : TreePaint.success(), a));
					rx += 9 + tw + 6;
				}
			}
		}
	}

	private static String rawText(TreeFigures.Raw raw) {
		String wanted = TreeTips.amount(raw.ingredient, raw.wanted, 1.0F);
		return (raw.estimated ? "~" : "") + (raw.have() > 0L ? TreeTips.amount(raw.ingredient, raw.have(), 1.0F) + "/" + wanted : wanted);
	}

	private void items(GuiGraphics g, TreeSession s, TreeView.Line line, float delta) {
		if (!(line.alpha <= 0.02F)) {
			TreeRow row = line.row;
			int shake = this.shakeOf(line);

			try {
				if (row.node != null) {
					EmiIngredient shown = row.node.ingredient();
					int cx = this.cx(row.depth) + shake;
					if (shown != null) {
						shown.render(g, cx + 8 + 1, line.sy + 2, delta, -3);
					}

					EmiRecipe recipe = row.node.recipe();
					if (this.stationShown(row.node, s.figures(row.node.path)) && !craftShown(s, line)) {
						RenderSystem.enableBlend();
						g.setColor(1.0F, 1.0F, 1.0F, 0.55F * line.alpha);
						recipe.getCategory().renderSimplified(g, this.right() - 17, line.sy + 2, delta);
						g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
					}

					return;
				}

				TreeFigures.Fold fold = s.fold(row);
				int cxx = this.cx(row.depth) + shake;
				int right = this.right() - 1;
				int cw = right - cxx;
				Component label = this.names() && cw >= 150
					? TreeTips.counted("cfhgui_crafting.tree.island.steps", (long)fold.steps, fold.steps)
					: Component.translatable("cfhgui_crafting.tree.island.steps.short", new Object[]{fold.steps});
				int mx = cxx + 13 + CFHGuiDraw.width(label) + 5;
				int limit = right - 3 - (craftShown(s, line) ? 18 : 0) - (cw >= 60 ? CFHGuiDraw.width(fold.done + "/" + fold.steps) + 6 : 0);

				for (TreeNode node : fold.first) {
					if (mx + 9 > limit || node.ingredient() == null) {
						break;
					}

					small(g, node.ingredient(), mx, line.sy + 6, delta);
					mx += 10;
				}

				if (line.height > 20) {
					int rx = cxx + 6;
					int ry = line.sy + 20 + 1;

					for (TreeFigures.Raw raw : fold.raws) {
						int tw = CFHGuiDraw.width(rawText(raw));
						if (rx + 9 + tw > right - 12) {
							break;
						}

						small(g, raw.ingredient, rx, ry, delta);
						rx += 9 + tw + 6;
					}
				}
			} catch (RuntimeException var19) {
				if (!complained) {
					complained = true;
					CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] a stack of the tree could not be drawn (said once)", var19);
				}
			}
		}
	}

	private static void small(GuiGraphics g, EmiIngredient ingredient, int x, int y, float delta) {
		g.pose().pushPose();
		g.pose().translate((float)x, (float)y, 0.0F);
		g.pose().scale(0.5F, 0.5F, 1.0F);
		ingredient.render(g, 0, 0, delta, 1);
		g.pose().popPose();
	}

	private void over(GuiGraphics g, TreeSession s, TreeView.Line line) {
		TreeRow row = line.row;
		int shake = this.shakeOf(line);
		if (line.alpha < 0.995F) {
			int veil = Colors.withAlpha(TreePaint.ground(), Math.round(255.0F * (1.0F - line.alpha)));
			if (row.node != null) {
				g.fill(this.cx(row.depth) + 8 + shake, line.sy + 1, this.cx(row.depth) + 8 + 18 + shake, line.sy + 1 + 18, veil);
			} else {
				g.fill(this.cx(row.depth), line.sy + 1, this.right(), line.sy + line.height - 1, veil);
			}
		}

		if (row.node != null && !(line.alpha <= 0.3F)) {
			TreeFigures f = s.figures(row.node.path);
			int plateX = this.cx(row.depth) + 8 + shake;
			if (f.state == TreeFigures.State.DONE || f.state == TreeFigures.State.COVERED) {
				boolean asked = f.state == TreeFigures.State.COVERED || !f.unasked();
				g.fill(
					plateX + 1,
					line.sy + 2,
					plateX + 18 - 1,
					line.sy + 18,
					Colors.withAlpha(TreePaint.ground(), Math.round((float)(TreePaint.vanilla() ? 88 : 106) * (asked ? 1.0F : 1.3F) * line.alpha))
				);
				if (asked) {
					TreePaint.badge(
						g, plateX + 18 - 3, line.sy + 18 - 2, f.state == TreeFigures.State.COVERED ? TreePaint.muted() : TreePaint.success(), TreePaint.TICK_SMALL, line.alpha
					);
				}
			}

			Long lit = s.lit.get(row.node.path);
			if (lit != null && Theme.current().motion() > 0.0F) {
				float t = (float)(Clock.nowMs() - lit) / (float)Theme.current().ms(900);
				if (t < 1.0F) {
					float e = 1.0F - (1.0F - t) * (1.0F - t);
					int grow = Math.round(e * 7.0F);
					CFHGuiDraw.outline(
						g,
						plateX - grow,
						line.sy + 1 - grow,
						18 + grow * 2,
						18 + grow * 2,
						Colors.withAlpha(TreePaint.accent(), Math.round(208.0F * (1.0F - t) * (1.0F - t) * line.alpha)),
						2 + grow / 2
					);
				}
			}
		}
	}

	private void burst(GuiGraphics g, TreeSession s) {
		if (s.celebrated != 0L && !(Theme.current().motion() <= 0.0F) && !this.lines.isEmpty()) {
			float t = (float)(Clock.nowMs() - s.celebrated) / (float)Theme.current().ms(1300);
			if (!(t < 0.0F) && !(t >= 1.0F)) {
				TreeView.Line goal = this.line(0);
				float cx = (float)(this.cx(0) + 8) + 9.0F;
				float cy = (float)(goal.sy + 1) + 9.0F;
				int[] colors = TreePaint.vanilla() ? new int[]{-171, -1, -11141291} : new int[]{TreePaint.success(), TreePaint.warning(), -1, TreePaint.accent()};
				float e = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);

				for (int k = 0; k < 2; k++) {
					float tk = Mth.clamp((t - (float)k * 0.12F) / 0.55F, 0.0F, 1.0F);
					if (!(tk <= 0.0F) && !(tk >= 1.0F)) {
						int grow = Math.round((1.0F - (1.0F - tk) * (1.0F - tk)) * 16.0F);
						CFHGuiDraw.outline(
							g,
							Math.round(cx) - 9 - grow,
							Math.round(cy) - 9 - grow,
							18 + grow * 2,
							18 + grow * 2,
							Colors.withAlpha(colors[k], Math.round(224.0F * (1.0F - tk))),
							2 + grow / 2
						);
					}
				}

				for (int i = 0; i < 22; i++) {
					float angle = spark(i, 1) * (float) (Math.PI * 2);
					float far = 14.0F + 34.0F * spark(i, 2);
					float life = 0.55F + 0.45F * spark(i, 3);
					float tk = t / life;
					if (!(tk >= 1.0F)) {
						int px = Math.round(cx + Mth.cos(angle) * (6.0F + far * e));
						int py = Math.round(cy + Mth.sin(angle) * (6.0F + far * e) + 22.0F * t * t);
						int size = tk < 0.5F ? 2 : 1;
						g.fill(px, py, px + size, py + size, Colors.withAlpha(colors[i % colors.length], Math.round(255.0F * (1.0F - tk * tk))));
					}
				}
			}
		}
	}

	private static float spark(int i, int salt) {
		int h = (i * 31 + 7) * -1640531535 + salt * -2048144789;
		h ^= h >>> 15;
		h *= 739982445;
		h ^= h >>> 12;
		return (float)(h & 65535) / 65536.0F;
	}

	private void scrollbar(GuiGraphics g, int mouseX, int mouseY) {
		if (this.scrollable()) {
			Theme t = Theme.current();
			Palette p = t.palette();
			int bx = this.x + this.w - 4;
			int barH = Math.max(12, (int)((long)this.h * (long)this.h / (long)Math.max(1, this.contentHeight)));
			int by = this.y + (int)((float)(this.h - barH) * (this.scroll.get() / (float)Math.max(1, this.maxScroll())));
			boolean hot = this.draggingBar || mouseX >= this.x + this.w - 7 && mouseX < this.x + this.w && mouseY >= this.y && mouseY < this.y + this.h;
			if (t.isVanilla()) {
				g.fill(bx, this.y, bx + 3, this.y + this.h, Integer.MIN_VALUE);
				g.fill(bx, by, bx + 3, by + barH, hot ? -2039584 : -6250336);
			} else {
				CFHGuiDraw.pixelRound(g, bx, this.y, 3, this.h, Colors.withAlpha(p.surface(), 128), 1);
				CFHGuiDraw.pixelRound(g, bx, by, 3, barH, this.draggingBar ? p.accent() : (hot ? p.textMuted() : p.borderStrong()), 1);
				int shade = Colors.withAlpha(p.bg(), 160);
				if (this.scroll.get() > 0.5F) {
					CFHGuiDraw.vgradient(g, this.x, this.y, this.w - 5, 8, shade, 0);
				}

				if (this.scroll.get() < (float)this.maxScroll() - 0.5F) {
					CFHGuiDraw.vgradient(g, this.x, this.y + this.h - 8, this.w - 5, 8, 0, shade);
				}
			}
		}
	}

	public EmiStackInteraction hoveredStack() {
		if (this.hovered != null && this.hovered.row.node != null && this.session != null && !this.session.foldMode) {
			EmiIngredient shown = this.hovered.row.node.ingredient();
			return shown == null ? EmiStackInteraction.EMPTY : new EmiStackInteraction(shown, this.hovered.row.node.recipe(), false);
		} else {
			return EmiStackInteraction.EMPTY;
		}
	}

	@Nullable
	public List<ClientTooltipComponent> tooltip() {
		TreeSession s = this.session;
		if (s == null) {
			return null;
		} else if (this.refusedWhy != null && Clock.nowMs() - this.refusedAt < 2600L) {
			return List.of(
				ClientTooltipComponent.create(
					Component.translatable("cfhgui_crafting.tree.refused." + this.refusedWhy.name().toLowerCase(Locale.ROOT))
						.withStyle(st -> st.withColor(TreePaint.danger() & 16777215))
						.getVisualOrderText()
				)
			);
		} else if (this.hoveredSpan != null) {
			List<ClientTooltipComponent> out = new ArrayList<>();
			long hiddenSteps = TreeRows.hidden(s.outline(), this.hoveredSpan.island).stream().filter(TreeNode::step).count();
			out.add(ClientTooltipComponent.create(TreeTips.counted("cfhgui_crafting.tree.island.fold", hiddenSteps, hiddenSteps).getVisualOrderText()));
			out.add(
				ClientTooltipComponent.create(
					Component.translatable("cfhgui_crafting.tree.island.fold.keys").withStyle(st -> st.withColor(TreePaint.dim() & 16777215)).getVisualOrderText()
				)
			);
			return out;
		} else if (this.hovered == null || this.hovered.leaving) {
			return null;
		} else if (this.zone == TreeView.Zone.CRAFT) {
			TreeNode node = this.hovered.row.isIsland() ? s.fold(this.hovered.row).next : this.hovered.row.node;
			if (node == null) {
				return null;
			} else {
				long crafts = s.figures(node.path).crafts;
				List<ClientTooltipComponent> out = new ArrayList<>();
				out.add(
					ClientTooltipComponent.create(Component.translatable("cfhgui_crafting.tree.craft", new Object[]{TreeActions.name(node.ingredient())}).getVisualOrderText())
				);
				if (crafts > 1L) {
					out.add(
						ClientTooltipComponent.create(
							Component.translatable("cfhgui_crafting.tree.craft.all", new Object[]{crafts})
								.withStyle(st -> st.withColor(TreePaint.dim() & 16777215))
								.getVisualOrderText()
						)
					);
				}

				return out;
			}
		} else {
			return this.hovered.row.isIsland() ? TreeTips.island(s, this.hovered.row) : TreeTips.node(s, this.hovered.row.node);
		}
	}

	public boolean mouseClicked(double mx, double my, int button) {
		TreeSession s = this.session;
		if (s == null || !this.contains(mx, my)) {
			return false;
		} else if (button != 0 && button != 1) {
			return false;
		} else if (button == 0 && this.scrollable() && mx >= (double)(this.x + this.w - 7)) {
			int barH = Math.max(12, (int)((long)this.h * (long)this.h / (long)Math.max(1, this.contentHeight)));
			int by = this.y + (int)((float)(this.h - barH) * (this.scroll.get() / (float)Math.max(1, this.maxScroll())));
			if (my < (double)by || my >= (double)(by + barH)) {
				this.scroll
					.snap(
						(float)Mth.clamp(
							(my - (double)this.y - (double)barH / 2.0) / (double)Math.max(1, this.h - barH) * (double)this.maxScroll(), 0.0, (double)this.maxScroll()
						)
					);
			}

			this.draggingBar = true;
			this.dragStartY = my;
			this.dragStartScroll = (double)this.scroll.target();
			return true;
		} else {
			this.hover((int)mx, (int)my);
			if (this.hoveredSpan != null) {
				if (button == 0) {
					s.open(this.hoveredSpan.island, false);
					CFHGuiSounds.tick();
				} else {
					TreeMenus.island(s, this.hoveredSpan.island, mx, my);
				}

				return true;
			} else {
				TreeView.Line line = this.hovered;
				if (line == null) {
					return true;
				} else {
					TreeRow row = line.row;
					if (row.isIsland()) {
						if (this.zone == TreeView.Zone.CRAFT && button == 0) {
							TreeNode next = s.fold(row).next;
							if (next != null) {
								TreeActions.craft(s, next, EmiInput.isShiftDown());
							}
						} else if (button == 0) {
							s.open(row.island, true);
							CFHGuiSounds.tick();
						} else {
							TreeMenus.island(s, row.island, mx, my);
						}

						return true;
					} else {
						TreeNode node = row.node;
						if (button == 1) {
							s.selected = node.path;
							TreeMenus.row(s, node, mx, my);
							return true;
						} else if (this.zone == TreeView.Zone.CRAFT) {
							TreeActions.craft(s, node, EmiInput.isShiftDown());
							return true;
						} else if (s.foldMode) {
							this.pick(s, line);
							return true;
						} else if (this.zone == TreeView.Zone.CARET) {
							s.fold(node.path, !row.folded);
							CFHGuiSounds.tick();
							return true;
						} else {
							long now = Clock.nowMs();
							boolean again = node.path.equals(this.lastClicked) && now - this.lastClickAt < 320L;
							this.lastClicked = node.path;
							this.lastClickAt = now;
							if (again && TreeActions.craft(s, node, EmiInput.isShiftDown())) {
								return true;
							} else {
								if (!node.path.equals(s.selected)) {
									CFHGuiSounds.tick();
								}

								s.selected = node.path;
								this.host.chosen(s, node);
								return true;
							}
						}
					}
				}
			}
		}
	}

	private void pick(TreeSession s, TreeView.Line line) {
		String path = line.row.node.path;
		if (s.firstPoint == null) {
			s.firstPoint = path;
			CFHGuiSounds.tick();
		} else if (s.firstPoint.equals(path)) {
			s.firstPoint = null;
			CFHGuiSounds.tick();
		} else {
			TreeRows.Refusal refusal = s.island(s.firstPoint, path);
			if (refusal == TreeRows.Refusal.ALREADY_THERE) {
				Island there = s.marks.island(s.firstPoint, path);
				if (there != null) {
					s.open(there, false);
				}

				s.firstPoint = null;
				CFHGuiSounds.tick();
			} else if (refusal != null) {
				this.refusedId = line.row.id;
				this.refusedWhy = refusal;
				this.refusedAt = Clock.nowMs();
			} else {
				s.firstPoint = null;
				CFHGuiSounds.click();
			}

			this.preview = null;
			this.previewFor = null;
		}
	}

	public boolean mouseReleased(double mx, double my, int button) {
		boolean was = this.draggingBar;
		this.draggingBar = false;
		return was;
	}

	public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
		if (!this.draggingBar) {
			return false;
		} else {
			int barH = Math.max(12, (int)((long)this.h * (long)this.h / (long)Math.max(1, this.contentHeight)));
			double ratio = (double)this.maxScroll() / (double)Math.max(1, this.h - barH);
			this.scroll.snap((float)Mth.clamp(this.dragStartScroll + (my - this.dragStartY) * ratio, 0.0, (double)this.maxScroll()));
			return true;
		}
	}

	public boolean mouseScrolled(double mx, double my, double amount) {
		if (this.session != null && this.contains(mx, my)) {
			if (this.scrollable()) {
				this.scroll.set((float)Mth.clamp((double)this.scroll.target() - amount * 20.0 * 2.0, 0.0, (double)this.maxScroll()));
			}

			return true;
		} else {
			return false;
		}
	}

	public boolean keyPressed(int key, int scan, int modifiers) {
		TreeSession s = this.session;
		if (s == null || this.lines.isEmpty()) {
			return false;
		} else if (key == 70 && modifiers == 0) {
			foldMode(s, !s.foldMode);
			return true;
		} else if (key != 256) {
			TreeView.Line at = s.selected == null ? null : this.byId.get(s.selected);
			switch (key) {
				case 257:
				case 335:
					if (at == null) {
						return false;
					}

					if (!TreeActions.craft(s, at.row.node, (modifiers & 1) != 0)) {
						this.host.chosen(s, at.row.node);
					}

					return true;
				case 262:
					if (at == null) {
						return false;
					}

					if (at.row.folded) {
						s.fold(at.row.node.path, false);
						CFHGuiSounds.tick();
					} else if (at.row.index + 1 < this.lines.size()) {
						TreeView.Line under = this.lines.get(at.row.index + 1);
						if (under.row.isIsland()) {
							s.open(under.row.island, true);
							CFHGuiSounds.tick();
						} else if (under.row.parent == at.row.index) {
							this.select(s, under);
						}
					}

					return true;
				case 263:
					if (at == null) {
						return false;
					}

					if (at.row.hasChildren && !at.row.folded) {
						s.fold(at.row.node.path, true);
						CFHGuiSounds.tick();
					} else {
						for (int i = at.row.parent; i >= 0; i = this.lines.get(i).row.parent) {
							if (this.lines.get(i).row.node != null) {
								this.select(s, this.lines.get(i));
								break;
							}
						}
					}

					return true;
				case 264:
				case 265:
					int i = at == null ? (key == 264 ? -1 : this.lines.size()) : at.row.index;
					i += key == 264 ? 1 : -1;

					while (i >= 0 && i < this.lines.size() && this.lines.get(i).row.node == null) {
						i += key == 264 ? 1 : -1;
					}

					if (i >= 0 && i < this.lines.size()) {
						this.select(s, this.lines.get(i));
					}

					return true;
				case 266:
					this.scroll.set(Mth.clamp(this.scroll.target() - (float)(this.h - 20), 0.0F, (float)this.maxScroll()));
					return true;
				case 267:
					this.scroll.set(Mth.clamp(this.scroll.target() + (float)(this.h - 20), 0.0F, (float)this.maxScroll()));
					return true;
				case 268:
					this.scroll.set(0.0F);
					return true;
				case 269:
					this.scroll.set((float)this.maxScroll());
					return true;
				default:
					return false;
			}
		} else if (s.firstPoint != null) {
			s.firstPoint = null;
			return true;
		} else if (s.foldMode) {
			foldMode(s, false);
			return true;
		} else {
			return false;
		}
	}

	public static void foldMode(TreeSession s, boolean on) {
		s.foldMode = on;
		if (!on) {
			s.firstPoint = null;
		}

		CFHGuiSounds.tick();
	}

	private void select(TreeSession s, TreeView.Line line) {
		s.selected = line.row.node.path;
		this.reveal(line);
		CFHGuiSounds.tick();
		this.host.chosen(s, line.row.node);
	}

	private void reveal(TreeView.Line line) {
		float at = this.scroll.target();
		if ((float)line.top < at + 2.0F) {
			this.scroll.set((float)Math.max(0, line.top - 2));
		} else if ((float)(line.top + line.height) > at + (float)this.h - 2.0F) {
			this.scroll.set((float)Math.min(this.maxScroll(), line.top + line.height - this.h + 2));
		}
	}

	public void reveal(String path) {
		TreeView.Line line = this.byId.get(path);
		if (line != null) {
			this.reveal(line);
		}
	}

	@Nullable
	public int[] placeOf(String id) {
		TreeView.Line line = this.byId.get(id);
		return line == null ? null : new int[]{this.cx(line.row.depth) + 8 + 18 + 6, this.placed(line).sy + 10};
	}

	public void scrollToEnd() {
		this.scroll.snap((float)this.maxScroll());
	}

	void scrollBy(int pixels) {
		float to = this.scroll.target() + (float)pixels;
		this.scroll.snap(to > (float)this.maxScroll() ? 0.0F : to);
	}

	public int rowCount() {
		return this.lines.size();
	}

	public interface Host {
		default void chosen(TreeSession session, TreeNode node) {
		}
	}

	private static final class Line {
		final TreeRow row;
		int top;
		int height;
		float fromTop;
		float fromAlpha = 1.0F;
		boolean entering;
		boolean leaving;
		int delay;
		Anim hover = new Anim(0.0F, 110, Ease.OUT_CUBIC);
		float curTop;
		float alpha = 1.0F;
		int sy;
		@Nullable
		FormattedCharSequence name;
		int nameRoom = -1;
		@Nullable
		String count;
		@Nullable
		String countWas;
		int countLooks = -1;

		Line(TreeRow row) {
			this.row = row;
		}
	}

	private static final class Span {
		final Island island;
		int first = -1;
		int last = -1;
		int lane;

		Span(Island island) {
			this.island = island;
		}
	}

	private static enum Zone {
		NONE,
		BODY,
		CARET,
		CRAFT,
		HANDLE;
	}
}
