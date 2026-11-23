package com.onehumanawa.cfhcore.content.gui.quests.client.view;

import com.mojang.math.Axis;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import com.onehumanawa.cfhcore.content.gui.quests.QuestsConfig;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsClient;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.logic.Rules;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Ids;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public class QuestCanvas extends CFHGuiWidget implements TreeView {
	public static final float MIN_ZOOM = 0.12F;
	public static final float MAX_ZOOM = 2.5F;
	private static final Map<String, float[]> LEFT_AT = new HashMap<>();
	public static final int OPENED_MS = 1100;
	public static final int DONE_MS = 1200;
	public static final int DONE_GOAL_MS = 1700;
	public static final int DONE_CHALLENGE_MS = 2300;
	private static final int FLY_MS = 330;
	private static final int GROW_STEP_MS = 60;
	private static final int GROW_RISE_MS = 300;
	private static final int FOLD_STEP_MS = 28;
	private static final int FOLD_FALL_MS = 190;
	private static final int GATE_MS = 200;
	private static final int CONTROL = 14;
	private static final int CONTROL_GAP = 2;
	private static final int CORNER = 6;
	private static final int OVERVIEW_W = 88;
	private static final int OVERVIEW_H = 58;
	private static final int OWN = 0;
	private static final int FIRST_LOOK = 1;
	private static final int FITTED = 2;
	private static final int AT_PLATE = 3;
	private GraphModel graph;
	private QuestPainter painter;
	private float camX;
	private float camY;
	private float zoom = 1.0F;
	private float toX;
	private float toY;
	private float toZoom = 1.0F;
	private long lastFrameMs;
	private int coverL;
	private int coverT;
	private int coverR;
	private int coverB;
	private int held = 0;
	private int heldBefore = 1;
	private boolean coverChanged;
	@Nullable
	private float[] before;
	private int beforeHeld;
	private boolean movedUnder;
	private boolean pressed;
	private boolean dragged;
	private boolean toolPressed;
	private int pressButton;
	private double pressX;
	private double pressY;
	@Nullable
	private NodeView pressedNode;
	@Nullable
	private NodeView hovered;
	private long hoveredSinceMs;
	@Nullable
	private String selected;
	@Nullable
	private SubtreePreview preview;
	private long lastClickMs;
	@Nullable
	private String lastClickKey;
	@Nullable
	private Consumer<String> onOpen;
	@Nullable
	private Consumer<String> onEnter;
	@Nullable
	private TreeView.ContextHandler onContext;
	@Nullable
	private CanvasTool tool;
	private TreeFilter filter = TreeFilter.ALL;
	private boolean showHidden;
	@Nullable
	private QuestCanvas.Journey journey;
	private final Anim[] controlHover = new Anim[]{new Anim(0.0F, 120, Ease.OUT_CUBIC), new Anim(0.0F, 120, Ease.OUT_CUBIC), new Anim(0.0F, 120, Ease.OUT_CUBIC)};
	private int hoveredControl = -1;
	private long controlSinceMs;
	private final Anim overviewShown = new Anim(0.0F, 200, Ease.OUT_CUBIC);
	private boolean overviewDrag;
	@Nullable
	private QuestPainter.Overview overview;
	private long zoomedAtMs;
	private GuiGraphics frameG;
	private int frameMouseX;
	private int frameMouseY;
	private QuestPainter.View frameView;
	private final Runnable paintFrame = this::paint;
	private final Runnable paintCard = this::paintCard;
	private static final float READABLE = 0.6F;
	private static final float CLOSE = 0.8F;
	private static final float TALL = 1.3F;
	private static final float TURN_GAIN = 1.25F;
	private static final float GROUND = 1.2F;
	@Nullable
	private String pointedAt;
	private static final boolean TIMED = Boolean.getBoolean("cfh.quests.frameTimes");
	private long timedNs;
	private long timedWorstNs;
	private int timedFrames;
	private float frameLeft;
	private float frameRight;
	private float frameTop;
	private float frameBottom;
	private float frameOriginX;
	private float frameOriginY;
	private final float[] stemX = new float[5];
	private final float[] stemY = new float[5];
	private final Chapter.Decoration fading = new Chapter.Decoration();
	@Nullable
	private Chapter.Decoration pointedDecoration;

	public QuestCanvas(int x, int y, int width, int height, QuestPainter painter) {
		super(x, y, width, height, Component.translatable("cfhgui_quests.canvas"));
		this.painter = painter;
		this.graph = GraphModel.of(new Chapter());
		this.silent();
	}

	@Override
	public void onOpen(@Nullable Consumer<String> listener) {
		this.onOpen = listener;
	}

	@Override
	public void onEnter(@Nullable Consumer<String> listener) {
		this.onEnter = listener;
	}

	@Override
	public void onContext(@Nullable TreeView.ContextHandler handler) {
		this.onContext = handler;
	}

	public QuestCanvas painter(QuestPainter p) {
		this.painter = p;
		return this;
	}

	public QuestPainter painter() {
		return this.painter;
	}

	public QuestCanvas tool(@Nullable CanvasTool t) {
		boolean inline = this.opensInline();
		boolean wasTurned = this.graph.turned();
		if (t != null && wasTurned) {
			this.graph.turn(false);
		}

		this.tool = t;
		if (t == null) {
			this.turnIfTall(this.graph);
		}

		if (wasTurned != this.graph.turned()) {
			this.gone();
			this.fitTarget();
			this.camX = this.toX;
			this.camY = this.toY;
			this.zoom = this.toZoom;
		}

		if (inline != this.opensInline() && !this.graph.chapter.id.isEmpty()) {
			this.graph.folding(this.opensInline());
			if (this.opensInline()) {
				this.graph.unfoldAll(QuestsConfig.get().openTrees);
			}

			this.gone();
			this.clampTarget();
			return this;
		} else {
			return this;
		}
	}

	private void gone() {
		if (this.selected != null && this.graph.shown(this.selected) == null) {
			this.selected = null;
		}

		if (this.hovered != null && this.graph.shown(this.hovered.key) != this.hovered) {
			this.hovered.hover.set(false);
			this.hovered = null;
		}

		if (this.pressedNode != null && this.graph.shown(this.pressedNode.key) != this.pressedNode) {
			this.pressedNode = null;
		}

		if (this.preview != null && this.graph.shown(this.preview.owner.key) != this.preview.owner) {
			this.preview = null;
		}
	}

	@Nullable
	public CanvasTool tool() {
		return this.tool;
	}

	public GraphModel graph() {
		return this.graph;
	}

	@Override
	public AbstractWidget widget() {
		return this;
	}

	@Override
	public void place(int x, int y, int width, int height) {
		if (width != this.getWidth() || height != this.getHeight()) {
			this.coverChanged = true;
		}

		this.setX(x);
		this.setY(y);
		this.setWidth(width);
		this.setHeight(height);
	}

	@Override
	public void cover(int left, int top, int right, int bottom) {
		int l = Math.max(0, left);
		int t = Math.max(0, top);
		int r = Math.max(0, right);
		int b = Math.max(0, bottom);
		if (l != this.coverL || t != this.coverT || r != this.coverR || b != this.coverB) {
			if ((l > 0 || r > 0) && this.coverL == 0 && this.coverR == 0) {
				this.before = new float[]{this.toX, this.toY, this.toZoom};
				this.beforeHeld = this.held;
				this.movedUnder = false;
			}

			this.coverL = l;
			this.coverT = t;
			this.coverR = r;
			this.coverB = b;
			this.coverChanged = true;
		}
	}

	private void covered() {
		if (this.journey == null) {
			this.coverChanged = false;
			if (!this.graph.isEmpty()) {
				boolean side = this.coverL > 0 || this.coverR > 0;
				if (!side && this.before != null) {
					if (!this.movedUnder) {
						this.toX = this.before[0];
						this.toY = this.before[1];
						this.toZoom = this.before[2];
						this.held = this.beforeHeld;
					}

					this.before = null;
				}

				boolean chosen = this.before == null;
				if (chosen && this.held == 3 && this.wholeReadable()) {
					this.held = this.heldBefore;
				}

				if (chosen && this.held == 1) {
					this.firstLook();
				} else if (chosen && this.held == 2) {
					this.fitTarget();
				} else {
					this.clampTarget();
				}
			}
		}
	}

	private void own() {
		this.held = 0;
		this.movedUnder = true;
	}

	@Nullable
	@Override
	public Chapter chapter() {
		return this.graph.chapter.id.isEmpty() ? null : this.graph.chapter;
	}

	@Override
	public void show(Chapter chapter, TreeView.Travel how, @Nullable String through) {
		boolean motion = Theme.current().motion() > 0.0F;
		GraphModel next = this.model(chapter);
		if (motion && how == TreeView.Travel.INTO && this.graph.shown(through) != null && !next.isEmpty()) {
			this.remember();
			this.drop();
			this.journey = new QuestCanvas.Journey(TreeView.Travel.INTO, next, through);
			whoosh(1.25F);
		} else if (motion && how == TreeView.Travel.OUT && next.shown(through) != null && !this.graph.isEmpty()) {
			this.remember();
			this.drop();
			this.graph.settle();
			this.journey = new QuestCanvas.Journey(TreeView.Travel.OUT, next, through);
			whoosh(0.95F);
		} else {
			this.remember();
			this.take(next);
			this.restoreView();
			if (how != TreeView.Travel.NONE && motion) {
				this.entrance();
			}
		}
	}

	private void entrance() {
		float margin = 1.5F;
		this.graph
			.playEntrance(
				this.tx((double)this.getX()) - 1.5F,
				this.ty((double)this.getY()) - 1.5F,
				this.tx((double)(this.getX() + this.getWidth())) + 1.5F,
				this.ty((double)(this.getY() + this.getHeight())) + 1.5F
			);
	}

	public void show(GraphModel model, boolean entrance) {
		this.remember();
		model.showHidden(this.showHidden);
		model.filter(this.filter, QuestsConfig.get().dimCompleted);
		this.take(model);
		this.restoreView();
		if (entrance && Theme.current().motion() > 0.0F) {
			this.entrance();
		}
	}

	@Override
	public void rebuild(Chapter chapter) {
		String chosen = this.selected;
		this.take(this.model(chapter));
		if (this.journey != null) {
			this.journey = null;
		}

		this.clampTarget();
		if (this.graph.shown(chosen) != null) {
			this.select(chosen);
		}
	}

	private GraphModel model(Chapter chapter) {
		GraphModel m = GraphModel.of(chapter);
		m.showHidden(this.showHidden);
		m.filter(this.filter, QuestsConfig.get().dimCompleted);
		if (this.opensInline()) {
			m.folding(true);
			m.unfoldAll(QuestsConfig.get().openTrees);
		}

		this.turnIfTall(m);
		return m;
	}

	private void turnIfTall(GraphModel m) {
		if (this.tool == null && QuestsConfig.get().turnTallTrees && !m.isEmpty() && m.canTurn()) {
			if (!(m.height() < m.width() * 1.3F) && this.getWidth() > 0 && this.getHeight() > 0) {
				float upright = this.fitZoom(m);
				if (!(upright >= 0.6F)) {
					m.turn(true);
					if (this.fitZoom(m) < upright * 1.25F) {
						m.turn(false);
					}
				}
			}
		}
	}

	private static String viewKey(GraphModel m) {
		return m.turned() ? m.chapter.id + "|turned" : m.chapter.id;
	}

	private void take(GraphModel model) {
		this.drop();
		this.graph = model;
		this.selected = null;
	}

	private void drop() {
		if (this.hovered != null) {
			this.hovered.hover.set(false);
		}

		this.hovered = null;
		this.preview = null;
		this.pressed = false;
		this.toolPressed = false;
		this.pressedNode = null;
		this.overviewDrag = false;
		this.journey = null;
	}

	private void restoreView() {
		this.coverChanged = false;
		this.before = null;
		if (!this.toLeft()) {
			this.firstLook();
		}

		this.camX = this.toX;
		this.camY = this.toY;
		this.zoom = this.toZoom;
	}

	private boolean toLeft() {
		float[] left = LEFT_AT.get(viewKey(this.graph));
		if (left == null) {
			return false;
		} else {
			this.toX = left[0];
			this.toY = left[1];
			this.toZoom = left[2];
			this.held = 0;
			if (left.length > 3 && left[3] > 0.0F) {
				this.heldBefore = left[3] == 2.0F ? 2 : 1;
				this.held = 3;
				if (this.wholeReadable()) {
					this.held = this.heldBefore;
					if (this.held == 1) {
						this.firstLook();
					} else {
						this.fitTarget();
					}

					return true;
				}
			}

			this.clampTarget();
			return true;
		}
	}

	public void lookAfresh() {
		LEFT_AT.remove(viewKey(this.graph));
		this.before = null;
		this.firstLook();
		this.camX = this.toX;
		this.camY = this.toY;
		this.zoom = this.toZoom;
	}

	private void firstLook() {
		this.held = 1;
		this.fitTarget();
		if (!(this.toZoom >= 0.6F) && !this.graph.isEmpty()) {
			if (this.coverB > 0) {
				int foot = this.coverB;
				this.coverB = 0;
				this.fitTarget();
				this.coverB = foot;
				if (this.toZoom >= 0.6F) {
					return;
				}
			}

			float x = 0.0F;
			float y = 0.0F;
			int n = 0;

			for (NodeView v : this.graph.nodes) {
				if (v.status.open() || v.status == Rules.Status.CLAIMABLE) {
					x += this.graph.restX(v);
					y += this.graph.restY(v);
					n++;
				}
			}

			NodeView first = this.graph.nodes.get(0);
			this.toZoom = 0.8F;
			String focus = this.graph.chapter.focus;
			NodeView asked = focus.isEmpty() ? null : this.graph.node(Ids.key(this.graph.chapter.id, focus));
			if (asked != null && asked.visible) {
				this.aim(this.graph.restX(asked), this.graph.restY(asked), this.toZoom);
				this.clampTarget();
			} else {
				this.aim(n > 0 ? x / (float)n : this.graph.restX(first), n > 0 ? y / (float)n : this.graph.restY(first), this.toZoom);
				this.clampTarget();
			}
		}
	}

	private boolean wholeReadable() {
		int l = this.coverL;
		int r = this.coverR;
		int foot = this.coverB;
		if (this.before != null) {
			this.coverL = 0;
			this.coverR = 0;
		}

		boolean readable = this.fitZoom(this.graph) >= 0.6F;
		if (!readable && foot > 0) {
			this.coverB = 0;
			readable = this.fitZoom(this.graph) >= 0.6F;
		}

		this.coverL = l;
		this.coverR = r;
		this.coverB = foot;
		return readable;
	}

	private float firstLookZoom(GraphModel m) {
		float fit = this.fitZoom(m);
		if (!(fit >= 0.6F) && !m.isEmpty()) {
			if (this.coverB > 0) {
				int foot = this.coverB;
				this.coverB = 0;
				fit = this.fitZoom(m);
				this.coverB = foot;
				if (fit >= 0.6F) {
					return fit;
				}
			}

			return 0.8F;
		} else {
			return fit;
		}
	}

	@Override
	public void remember() {
		if (this.journey == null) {
			if (this.graph != null && !this.graph.chapter.id.isEmpty()) {
				float[] was = this.before != null && !this.movedUnder ? this.before : null;
				int whose = was != null ? this.beforeHeld : this.held;
				if (whose == 1) {
					LEFT_AT.remove(viewKey(this.graph));
				} else {
					float plate = whose == 3 ? (float)this.heldBefore : 0.0F;
					LEFT_AT.put(viewKey(this.graph), was != null ? new float[]{was[0], was[1], was[2], plate} : new float[]{this.toX, this.toY, this.toZoom, plate});
				}
			}
		}
	}

	@Override
	public void refresh() {
		this.graph.refresh(true);
		if (this.journey != null) {
			this.journey.next.refresh(false);
		}

		if (this.preview != null) {
			this.preview.graph.refresh(true);
		}

		this.gone();
	}

	@Override
	public boolean opensInline() {
		return this.tool == null;
	}

	@Override
	public boolean unfolded(@Nullable String questKey) {
		Inlay in = this.graph.inlay(questKey);
		return in != null && !in.closing();
	}

	@Override
	public void unfold(String questKey, boolean open) {
		if (this.journey == null) {
			if (open && this.graph.shown(questKey) == null) {
				this.expandTo(questKey);
			}

			this.setUnfolded(questKey, open);
		}
	}

	private boolean setUnfolded(String key, boolean open) {
		if (this.opensInline() && this.unfolded(key) != open) {
			List<String> kept = QuestsConfig.get().openTrees;
			if (open) {
				Inlay in = this.graph.unfold(key, false);
				if (in == null) {
					return false;
				}

				if (!kept.contains(key)) {
					kept.add(key);
				}

				this.graph.unfoldAll(kept);
				this.roomChanged(this.graph.inlay(key));
			} else {
				if (!this.graph.fold(key, false)) {
					return false;
				}

				kept.remove(key);
				this.roomChanged(null);
			}

			return true;
		} else {
			return false;
		}
	}

	private boolean expandTo(@Nullable String key) {
		BookIndex index = QuestsState.index();
		BookIndex.Entry e = QuestsState.entry(key);
		if (this.opensInline() && index != null && e != null && this.journey == null) {
			List<String> ways = new ArrayList<>();
			Chapter c = e.chapter();

			for (int guard = 0; !c.id.equals(this.graph.chapter.id) && guard < 8; guard++) {
				BookIndex.Entry way = null;
				Iterator var8 = index.gateways(c.id).iterator();

				while (true) {
					if (var8.hasNext()) {
						String g = (String)var8.next();
						BookIndex.Entry ge = index.get(g);
						if (ge == null || !ge.quest().isInline()) {
							continue;
						}

						way = ge;
					}

					if (way == null) {
						return false;
					}

					ways.add(0, way.key());
					c = way.chapter();
					break;
				}
			}

			if (!c.id.equals(this.graph.chapter.id)) {
				return false;
			} else {
				for (String way : ways) {
					this.setUnfolded(way, true);
				}

				return this.graph.shown(key) != null;
			}
		} else {
			return false;
		}
	}

	private void roomChanged(@Nullable Inlay opened) {
		int whose = this.held == 3 ? this.heldBefore : this.held;
		if ((whose == 2 || whose == 1) && !this.wholeReadable()) {
			this.heldBefore = whose;
			whose = 3;
		}

		this.held = whose;
		if (this.before != null) {
			if (!this.movedUnder) {
				this.beforeHeld = whose;
			}

			if (opened != null && whose == 3) {
				this.movedUnder = true;
			}
		} else {
			if (whose == 2) {
				this.fitTarget();
				return;
			}

			if (whose == 1) {
				this.firstLook();
				return;
			}
		}

		this.clampTarget();
		if (opened != null) {
			this.revealPlate(opened);
		}
	}

	private void revealPlate(Inlay in) {
		if (!(this.painter.unit() * this.toZoom <= 0.0F)) {
			Rect f = this.free();
			float cx = (float)this.getX() + (float)this.getWidth() / 2.0F;
			float cy = (float)this.getY() + (float)this.getHeight() / 2.0F;
			float margin = 10.0F;
			float edge = this.controlsShown() ? 24.0F : 10.0F;
			NodeView o = in.owner;
			float rx = this.graph.restX(o);
			float ry = this.graph.restY(o);
			float reach = 0.5F * o.size + 0.3F;
			float wide = Math.max(in.restX + in.restW, rx + reach) - Math.min(in.restX, rx - reach);
			float high = in.restY + in.restH - (ry - reach);
			float fits = Math.min(((float)f.w() - 10.0F - edge) / (wide * this.painter.unit()), ((float)f.h() - 20.0F) / (high * this.painter.unit()));
			float back = Math.max(fits, 0.6F);
			if (back < this.toZoom * 0.95F) {
				float was = this.painter.unit() * this.toZoom;
				float px = cx + (rx - this.toX) * was;
				float py = cy + (ry - this.toY) * was;
				this.toZoom = back;
				this.zoomedAtMs = Clock.nowMs();
				float now = this.painter.unit() * this.toZoom;
				this.toX = rx - (px - cx) / now;
				this.toY = ry - (py - cy) / now;
				this.clampTarget();
			}

			float u = this.painter.unit() * this.toZoom;
			float ox = cx + (rx - this.toX) * u;
			float oy = cy + (ry - this.toY) * u;
			float or = this.painter.radius() * this.toZoom * o.size + 8.0F;
			float left = cx + (in.restX - this.toX) * u;
			float top = cy + (in.restY - this.toY) * u;
			float right = left + in.restW * u;
			float bottom = top + in.restH * u;
			float dx = 0.0F;
			float dy = 0.0F;
			if (bottom > (float)f.bottom() - 10.0F) {
				dy = Math.min(bottom - ((float)f.bottom() - 10.0F), Math.max(0.0F, oy - or - ((float)f.y() + 10.0F)));
			}

			if (right > (float)f.right() - edge) {
				dx = Math.min(right - ((float)f.right() - edge), Math.max(0.0F, ox - or - ((float)f.x() + 10.0F)));
			} else if (left < (float)f.x() + 10.0F) {
				dx = -Math.min((float)f.x() + 10.0F - left, Math.max(0.0F, (float)f.right() - edge - (ox + or)));
			}

			if (dx != 0.0F || dy != 0.0F) {
				this.toX += dx / u;
				this.toY += dy / u;
				this.clampTarget();
			}
		}
	}

	@Override
	public boolean travelling() {
		return this.journey != null;
	}

	@Override
	public void filter(TreeFilter f) {
		this.filter = f;
		this.graph.filter(f, QuestsConfig.get().dimCompleted);
	}

	@Override
	public TreeFilter filter() {
		return this.filter;
	}

	@Override
	public void showHidden(boolean on) {
		this.showHidden = on;
		this.graph.showHidden(on);
	}

	public float zoom() {
		return this.zoom;
	}

	public float unit() {
		return this.painter.unit() * this.zoom;
	}

	public float sx(float treeX) {
		return (float)this.getX() + (float)this.getWidth() / 2.0F + (treeX - this.camX) * this.unit();
	}

	public float sy(float treeY) {
		return (float)this.getY() + (float)this.getHeight() / 2.0F + (treeY - this.camY) * this.unit();
	}

	public float tx(double screenX) {
		return this.camX + (float)(screenX - (double)this.getX() - (double)this.getWidth() / 2.0) / this.unit();
	}

	public float ty(double screenY) {
		return this.camY + (float)(screenY - (double)this.getY() - (double)this.getHeight() / 2.0) / this.unit();
	}

	public float radiusOf(NodeView node) {
		return this.painter.radius() * this.zoom * node.size;
	}

	public Rect free() {
		return new Rect(
			this.getX() + this.coverL,
			this.getY() + this.coverT,
			Math.max(1, this.getWidth() - this.coverL - this.coverR),
			Math.max(1, this.getHeight() - this.coverT - this.coverB)
		);
	}

	private float freeDx() {
		return (float)(this.coverL - this.coverR) / 2.0F;
	}

	private float freeDy() {
		return (float)(this.coverT - this.coverB) / 2.0F;
	}

	private void aim(float treeX, float treeY, float atZoom) {
		float u = this.painter.unit() * atZoom;
		this.toX = treeX - this.freeDx() / u;
		this.toY = treeY - this.freeDy() / u;
		this.toZoom = atZoom;
	}

	@Override
	public void fit() {
		this.held = 2;
		this.movedUnder = true;
		this.fitTarget();
	}

	private void fitTarget() {
		if (!this.graph.isEmpty() && this.getWidth() - this.coverL - this.coverR > 0 && this.getHeight() - this.coverT - this.coverB > 0) {
			this.aim((this.graph.minX + this.graph.maxX) / 2.0F, (this.graph.minY + this.graph.maxY) / 2.0F, this.fitZoom(this.graph));
		} else {
			this.toX = 0.0F;
			this.toY = 0.0F;
			this.toZoom = 1.0F;
		}
	}

	private float fitZoom(GraphModel m) {
		int w = this.getWidth() - this.coverL - this.coverR;
		int h = this.getHeight() - this.coverT - this.coverB;
		if (!m.isEmpty() && w > 0 && h > 0) {
			float tw = (m.width() + 1.8F) * this.painter.unit();
			float th = (m.height() + 1.8F) * this.painter.unit();
			return Mth.clamp(Math.min((float)w / tw, (float)h / th), 0.12F, 1.0F);
		} else {
			return 1.0F;
		}
	}

	private void fitNow() {
		this.fit();
		this.zoomedAtMs = Clock.nowMs();
	}

	@Override
	public boolean focus(@Nullable String key) {
		NodeView n = this.graph.shown(key);
		if (n == null && this.expandTo(key)) {
			n = this.graph.shown(key);
		}

		if (n == null) {
			return false;
		} else {
			this.own();
			this.aim(this.graph.restX(n), this.graph.restY(n), this.toZoom < 0.8F ? 1.0F : this.toZoom);
			this.clampTarget();
			this.select(key);
			return true;
		}
	}

	@Override
	public void reveal(@Nullable String key) {
		NodeView n = this.graph.shown(key);
		if (n != null) {
			Rect f = this.free();
			float u = this.painter.unit() * this.toZoom;
			float r = this.painter.radius() * this.toZoom * n.size + 14.0F;
			float x = (float)this.getX() + (float)this.getWidth() / 2.0F + (this.graph.restX(n) - this.toX) * u;
			float y = (float)this.getY() + (float)this.getHeight() / 2.0F + (this.graph.restY(n) - this.toY) * u;
			float dx = 0.0F;
			float dy = 0.0F;
			if (x - r < (float)f.x()) {
				dx = x - r - (float)f.x();
			} else if (x + r > (float)f.right()) {
				dx = x + r - (float)f.right();
			}

			if (y - r < (float)f.y()) {
				dy = y - r - (float)f.y();
			} else if (y + r > (float)f.bottom()) {
				dy = y + r - (float)f.bottom();
			}

			this.toX += dx / u;
			this.toY += dy / u;
		}
	}

	public void pan(float unitsX, float unitsY) {
		this.own();
		this.toX += unitsX;
		this.toY += unitsY;
		this.clampTarget();
	}

	public void zoomAbout(float factor, double screenX, double screenY) {
		float before = this.toZoom;
		this.toZoom = Mth.clamp(this.toZoom * factor, 0.12F, 2.5F);
		if (before < 1.0F && this.toZoom > 1.0F || before > 1.0F && this.toZoom < 1.0F) {
			this.toZoom = 1.0F;
		}

		if (this.toZoom != before) {
			this.own();
			this.zoomedAtMs = Clock.nowMs();
			float px = (float)(screenX - (double)this.getX() - (double)this.getWidth() / 2.0);
			float py = (float)(screenY - (double)this.getY() - (double)this.getHeight() / 2.0);
			float ax = this.toX + px / (this.painter.unit() * before);
			float ay = this.toY + py / (this.painter.unit() * before);
			this.toX = ax - px / (this.painter.unit() * this.toZoom);
			this.toY = ay - py / (this.painter.unit() * this.toZoom);
			this.clampTarget();
		}
	}

	private void clampTarget() {
		float u = this.painter.unit() * this.toZoom;
		if (!(u <= 0.0F)) {
			Rect f = this.free();
			float offX = this.freeDx() / u;
			float offY = this.freeDy() / u;
			this.toX = within(this.toX + offX, this.graph.minX - 1.2F, this.graph.maxX + 1.2F, (float)f.w() / 2.0F / u) - offX;
			this.toY = within(this.toY + offY, this.graph.minY - 1.2F, this.graph.maxY + 1.2F, (float)f.h() / 2.0F / u) - offY;
		}
	}

	private static float within(float middle, float min, float max, float half) {
		return max - min <= half * 2.0F ? Mth.clamp(middle, max - half, min + half) : Mth.clamp(middle, min + half, max - half);
	}

	@Override
	public void select(@Nullable String key) {
		if (!Objects.equals(key, this.selected)) {
			NodeView was = this.graph.node(this.selected);
			NodeView now = this.graph.node(key);
			if (was != null) {
				was.selected.set(false);
			}

			if (now != null) {
				now.selected.set(true);
			}

			this.selected = key;
		}
	}

	@Nullable
	@Override
	public String selected() {
		return this.selected;
	}

	@Nullable
	@Override
	public String hovered() {
		return this.hovered == null ? null : this.hovered.key;
	}

	@Nullable
	public NodeView hoveredNode() {
		return this.hovered;
	}

	public void pointAt(@Nullable String key) {
		this.pointedAt = key;
	}

	@Nullable
	public SubtreePreview preview() {
		return this.preview;
	}

	@Nullable
	public NodeView nodeAt(double mouseX, double mouseY) {
		if (!this.contains(mouseX, mouseY)) {
			return null;
		} else {
			float u = this.unit();
			float cx = (float)this.getX() + (float)this.getWidth() / 2.0F;
			float cy = (float)this.getY() + (float)this.getHeight() / 2.0F;
			float r0 = this.painter.radius() * this.zoom;
			float mx = this.camX + ((float)mouseX - cx) / u;
			float my = this.camY + ((float)mouseY - cy) / u;
			float slack = 2.0F / u;

			for (int i = this.graph.nodes.size() - 1; i >= 0; i--) {
				NodeView n = this.graph.nodes.get(i);
				float dx = mx - n.x;
				float dy = my - n.y;
				float r = r0 * n.size / u + slack;
				if (!(dx * dx + dy * dy > r * r) && (n.inlay == null || !(n.inlay.presence(n) < 0.5F))) {
					return n;
				}
			}

			return null;
		}
	}

	@Override
	public boolean step(int direction) {
		List<NodeView> todo = new ArrayList<>();

		for (NodeView n : this.graph.nodes) {
			if (n.status.open() || n.status == Rules.Status.CLAIMABLE) {
				todo.add(n);
			}
		}

		if (todo.isEmpty()) {
			return false;
		} else {
			todo.sort(Comparator.<NodeView>comparingInt(nx -> nx.depth).thenComparingDouble(nx -> (double)nx.y).thenComparingDouble(nx -> (double)nx.x));
			int at = -1;

			for (int i = 0; i < todo.size(); i++) {
				if (todo.get(i).key.equals(this.selected)) {
					at = i;
					break;
				}
			}

			int next = at < 0 ? (direction >= 0 ? 0 : todo.size() - 1) : Math.floorMod(at + (direction >= 0 ? 1 : -1), todo.size());
			CFHGuiSounds.tick();
			return this.focus(todo.get(next).key);
		}
	}

	private QuestPainter.View view(int mouseX, int mouseY) {
		Chapter c = this.graph.chapter;
		float seconds = Theme.current().motion() > 0.0F ? (float)(Clock.nowMs() % 3600000L) / 1000.0F : 0.0F;
		return new QuestPainter.View(
			new Rect(this.getX(), this.getY(), this.getWidth(), this.getHeight()), this.zoom, this.camX, this.camY, seconds, mouseX, mouseY, c, QuestPainter.accentOf(c)
		);
	}

	private static void whoosh(float pitch) {
		if (Theme.current().uiSounds()) {
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_IN, pitch, 0.35F));
		}
	}

	protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.draw(g, mouseX, mouseY);
	}

	protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.draw(g, mouseX, mouseY);
	}

	private void draw(GuiGraphics g, int mouseX, int mouseY) {
		if (this.coverChanged) {
			this.covered();
		}

		if (this.graph.tick()) {
			this.gone();
		}

		if (this.journey != null) {
			if (this.journey.advance()) {
				this.journey = null;
				this.lastFrameMs = Clock.nowMs();
			}
		} else {
			this.glide();
		}

		boolean quiet = this.tool != null && this.tool.quiet();
		boolean busy = this.journey != null;
		boolean inside = !busy && this.isHovered() && g.containsPointInScissor(mouseX, mouseY);
		int control = inside && !this.pressed ? this.controlAt((double)mouseX, (double)mouseY) : -1;
		if (control != this.hoveredControl) {
			this.hoveredControl = control;
			this.controlSinceMs = Clock.nowMs();
		}

		for (int i = 0; i < this.controlHover.length; i++) {
			this.controlHover[i].set(i == control);
		}

		boolean over = inside && control < 0 && (!this.pressed || !this.dragged) && !this.overviewDrag && !this.overOverview((double)mouseX, (double)mouseY);
		NodeView now = this.pointedAt != null ? this.graph.shown(this.pointedAt) : (over ? this.nodeAt((double)mouseX, (double)mouseY) : null);
		if (now != this.hovered) {
			if (this.hovered != null) {
				this.hovered.hover.set(false);
			}

			this.hovered = now;
			this.hoveredSinceMs = Clock.nowMs();
			if (this.hovered != null) {
				this.hovered.hover.set(true);
			}
		}

		this.unfold(quiet || busy);
		if (control >= 0 && Clock.nowMs() - this.controlSinceMs > 350L) {
			CFHGuiTooltips.request(
				Component.translatable(control == 0 ? "cfhgui_quests.canvas.zoom_in" : (control == 1 ? "cfhgui_quests.canvas.zoom_out" : "cfhgui_quests.canvas.fit")), this
			);
		}

		this.frameG = g;
		this.frameMouseX = mouseX;
		this.frameMouseY = mouseY;
		this.frameView = this.view(mouseX, mouseY);
		this.pointedDecoration = null;
		long began = TIMED ? System.nanoTime() : 0L;
		CanvasDraw.batch(g, this.paintFrame);
		if (TIMED) {
			this.timed(System.nanoTime() - began);
		}

		this.frameG = null;
		if (inside && control < 0 && !this.pressed) {
			this.decorationTip();
		} else {
			this.pointedDecoration = null;
		}
	}

	private void timed(long ns) {
		this.timedNs += ns;
		this.timedWorstNs = Math.max(this.timedWorstNs, ns);
		if (++this.timedFrames >= 100) {
			CFHGuiQuests.LOGGER
				.info(
					"[CFHGui Quests] tree of {} quests and {} lines at zoom {}: {} ms a frame on average, {} ms at worst, over {} frames ({} fps)",
					new Object[]{
						this.graph.nodes.size(),
						this.graph.links.size(),
						String.format(Locale.ROOT, "%.2f", this.zoom),
						String.format(Locale.ROOT, "%.3f", (double)this.timedNs / 1000000.0 / (double)this.timedFrames),
						String.format(Locale.ROOT, "%.3f", (double)this.timedWorstNs / 1000000.0),
						this.timedFrames,
						Minecraft.getInstance().getFps()
					}
				);
			this.timedNs = this.timedWorstNs = 0L;
			this.timedFrames = 0;
		}
	}

	private void paint() {
		GuiGraphics g = this.frameG;
		QuestPainter.View view = this.frameView;
		int mouseX = this.frameMouseX;
		int mouseY = this.frameMouseY;
		float alpha = this.effectiveAlpha();
		QuestCanvas.Journey way = this.journey;
		Rect free = this.free();
		float originX = (float)free.x() + (float)free.w() / 2.0F;
		float originY = (float)free.y() + (float)free.h() / 2.0F;
		g.enableScissor(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight());

		try {
			this.painter.backdrop(g, view);
			if (this.tool != null) {
				this.tool.renderUnder(g, this, mouseX, mouseY);
			}

			float margin = 44.0F * this.zoom + 10.0F;
			float left = (float)this.getX() - margin;
			float right = (float)(this.getX() + this.getWidth()) + margin;
			float top = (float)this.getY() - margin;
			float bottom = (float)(this.getY() + this.getHeight()) + margin;
			this.frameLeft = left;
			this.frameRight = right;
			this.frameTop = top;
			this.frameBottom = bottom;
			this.frameOriginX = originX;
			this.frameOriginY = originY;
			if (way == null || !way.moving()) {
				this.decorations(g, view, this.graph, 1.0F);
			}

			for (LinkView l : this.graph.links) {
				this.link(g, view, l, alpha);
			}

			if (!this.graph.inlays.isEmpty()) {
				this.inlays(g, view, this.graph.inlays, alpha * (way == null ? 1.0F : way.plates()));
			}

			long now = Clock.nowMs();
			boolean party = QuestsConfig.get().celebrations && Theme.current().motion() > 0.0F;
			NodeView top1 = null;
			boolean celebrating = false;
			this.painter.beginNodes(g, view);

			for (NodeView n : this.graph.nodes) {
				if (n == this.hovered) {
					top1 = n;
				} else {
					celebrating |= this.drawNode(g, view, n, way, originX, originY, left, right, top, bottom, alpha, now, party);
				}
			}

			if (top1 != null) {
				celebrating |= this.drawNode(g, view, top1, way, originX, originY, left, right, top, bottom, alpha, now, party);
			}

			this.painter.endNodes(g, view);
			if (celebrating || this.preview != null) {
				CanvasDraw.layer(g);
			}

			if (celebrating) {
				for (NodeView nx : this.graph.nodes) {
					if (nx.doneAtMs != 0L) {
						float t = (float)(now - nx.doneAtMs) / (float)doneMs(nx);
						if (!(t >= 1.0F) && party) {
							float x = this.sx(nx.x);
							float y = this.sy(nx.y);
							if (!(x < left - 120.0F) && !(x > right + 120.0F) && !(y < top - 120.0F) && !(y > bottom + 120.0F)) {
								this.painter.celebrated(g, viewOf(nx, view), nx, x, y, this.zoom * nx.size, Math.max(0.0F, t));
							}
						} else {
							nx.doneAtMs = 0L;
						}
					}
				}
			}

			if (this.preview != null) {
				NodeView o = this.preview.owner;
				float back = this.preview.plate() * 0.42F;
				if (back > 0.01F) {
					this.painter.veil(g, view, back * alpha);
					this.painter.node(g, viewOf(o, view), o, this.sx(o.x), this.sy(o.y), this.zoom * o.size, alpha * (0.22F + 0.78F * o.lit.get()));
				}

				this.preview.render(g, this.painter, view, this.sx(o.x), this.sy(o.y), this.radiusOf(o), free.x(), free.right());
			}

			CanvasDraw.layer(g);
			if (this.tool != null) {
				this.tool.renderOver(g, this, mouseX, mouseY);
			}

			if (way != null) {
				this.painter.veil(g, view.of(way.beyond()), way.veil() * alpha);
				way.gate(g, view, originX, originY, alpha);
				CanvasDraw.layer(g);
			}

			this.painter.frame(g, view);
			if (this.graph.isEmpty() && way == null) {
				this.painter.empty(g, view, Component.translatable(QuestsState.available() ? "cfhgui_quests.canvas.empty" : "cfhgui_quests.canvas.no_book"));
			}

			this.corner(g, view, free, alpha);
		} finally {
			g.disableScissor();
		}
	}

	private static int doneMs(NodeView n) {
		return n.quest.frame == Quest.Frame.CHALLENGE ? 2300 : (n.quest.frame == Quest.Frame.GOAL ? 1700 : 1200);
	}

	private static QuestPainter.View viewOf(NodeView n, QuestPainter.View view) {
		Inlay in = n.inlay;
		return in != null && in.view != null ? in.view : view;
	}

	private void decorations(GuiGraphics g, QuestPainter.View view, GraphModel tree, float shown) {
		List<Chapter.Decoration> all = tree.chapter.decorations;
		boolean ordered = false;

		for (int i = 0; i < all.size() && !ordered; i++) {
			ordered = all.get(i).order != 0;
		}

		Integer[] turn = null;
		if (ordered) {
			turn = new Integer[all.size()];

			for (int i = 0; i < turn.length; i++) {
				turn[i] = i;
			}

			Arrays.sort(turn, Comparator.comparingInt(ix -> all.get(ix).order));
		}

		boolean editing = this.tool != null;

		for (int at = 0; at < all.size(); at++) {
			int i = turn == null ? at : turn[at];
			Chapter.Decoration d = all.get(i);
			if (editing || !d.editorOnly && (d.requires.isEmpty() || QuestsState.progress().everDone(d.requires))) {
				float dx = this.sx(tree.decorationX(i));
				float dy = this.sy(tree.decorationY(i));
				if (!(dx < this.frameLeft - 200.0F) && !(dx > this.frameRight + 200.0F) && !(dy < this.frameTop - 100.0F) && !(dy > this.frameBottom + 100.0F)) {
					if (shown >= 0.999F && !editing && this.hovered == null && (!d.hover.isEmpty() || !d.link.isEmpty())) {
						float hw = tree.decorationHalfW(i) * this.painter.unit() * this.zoom;
						float hh = tree.decorationHalfH(i) * this.painter.unit() * this.zoom;
						if (hw > 0.0F && Math.abs((float)this.frameMouseX - dx) <= hw && Math.abs((float)this.frameMouseY - dy) <= hh && this.isHovered()) {
							this.pointedDecoration = d;
						}
					}

					if (d.rotation != 0.0F) {
						g.pose().pushPose();
						g.pose().rotateAround(Axis.ZP.rotationDegrees(d.rotation), dx, dy, 0.0F);
					}

					boolean turned = d.rotation != 0.0F;
					if (shown < 0.999F) {
						this.fading.type = d.type;
						this.fading.text = d.text;
						this.fading.image = d.image;
						this.fading.w = d.w;
						this.fading.h = d.h;
						this.fading.scale = d.scale;
						this.fading.color = d.color;
						this.fading.heading = d.heading;
						this.fading.alpha = d.alpha * shown;
						d = this.fading;
					}

					this.painter.decoration(g, view, d, dx, dy, this.zoom);
					if (turned) {
						g.pose().popPose();
					}
				}
			}
		}
	}

	private void decorationTip() {
		Chapter.Decoration d = this.pointedDecoration;
		if (d != null) {
			List<Component> lines = new ArrayList<>();

			for (String line : d.hover) {
				lines.add(QuestText.text(line));
			}

			if (!d.link.isEmpty()) {
				lines.add(
					Component.translatable(d.link.startsWith("http") ? "cfhgui_quests.decoration.link.web" : "cfhgui_quests.decoration.link")
						.withStyle(s -> s.withColor(10592151))
				);
			}

			if (!lines.isEmpty()) {
				CFHGuiTooltips.request(lines, this);
			}
		}
	}

	private boolean follow(Chapter.Decoration d) {
		String link = d.link.trim();
		if (link.isEmpty()) {
			return false;
		} else if (link.startsWith("http://") || link.startsWith("https://")) {
			try {
				ConfirmLinkScreen.confirmLinkNow(Minecraft.getInstance().screen, new URI(link), false);
				return true;
			} catch (Exception var4) {
				return false;
			}
		} else if (link.startsWith("chapter:")) {
			QuestsClient.open(link.substring("chapter:".length()), null);
			return true;
		} else if (QuestsState.entry(link) != null && this.onOpen != null) {
			this.onOpen.accept(link);
			return true;
		} else {
			return false;
		}
	}

	private void link(GuiGraphics g, QuestPainter.View view, LinkView l, float alpha) {
		QuestCanvas.Journey way = this.journey;
		NodeView a = l.from();
		NodeView b = l.to();
		float x1 = this.sx(a.x);
		float y1 = this.sy(a.y);
		float x2 = this.sx(b.x);
		float y2 = this.sy(b.y);
		float in = Math.min(a.appear(), b.appear());
		float there = 1.0F;
		float drawn = 1.0F;
		if (a.inlay != null) {
			there = a.inlay.presence(a);
		}

		if (b.inlay != null) {
			there = Math.min(there, b.inlay.outer());
			drawn = b.inlay.drawn(b.inlay.depthOf(b));
		}

		if (way != null) {
			float ka = way.spread(a);
			float kb = way.spread(b);
			x1 = Mth.lerp(ka, this.frameOriginX, x1);
			y1 = Mth.lerp(ka, this.frameOriginY, y1);
			x2 = Mth.lerp(kb, this.frameOriginX, x2);
			y2 = Mth.lerp(kb, this.frameOriginY, y2);
			there = Math.min(there, Math.min(way.presence(a), way.presence(b)));
			in = Math.min(in, Math.min(ka, kb));
		}

		if (!(Math.max(x1, x2) < this.frameLeft)
			&& !(Math.min(x1, x2) > this.frameRight)
			&& !(Math.max(y1, y2) < this.frameTop)
			&& !(Math.min(y1, y2) > this.frameBottom)) {
			if (!(in <= 0.01F) && !(there <= 0.01F) && !(drawn <= 0.01F)) {
				float len = Mth.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
				if (!(len < 1.0F)) {
					float r0 = this.painter.radius() * this.zoom;
					float ta = Math.min(0.45F, r0 * a.size / len);
					float tb = Math.min(0.45F, r0 * b.size / len);
					float grown = Math.min(drawn, Ease.OUT_CUBIC.apply(Math.min(1.0F, in * 1.3F)));
					this.painter
						.link(
							g,
							view,
							l,
							Mth.lerp(ta, x1, x2),
							Mth.lerp(ta, y1, y2),
							Mth.lerp(1.0F - tb, x1, x2),
							Mth.lerp(1.0F - tb, y1, y2),
							this.zoom,
							alpha * there * (0.18F + 0.82F * l.lit()) * Math.min(1.0F, in * 2.0F),
							grown
						);
				}
			}
		}
	}

	private void inlays(GuiGraphics g, QuestPainter.View view, List<Inlay> inlays, float alpha) {
		for (Inlay in : inlays) {
			QuestPainter.View own = view.of(in.chapter);
			in.view = own;
			NodeView o = in.owner;
			float appear = o.appear();
			float shown = in.shown() * alpha * (appear >= 1.0F ? 1.0F : Ease.OUT_CUBIC.apply(appear));
			if (!(shown <= 0.01F)) {
				float p = in.plate();
				float x0 = this.sx(Mth.lerp(p, in.anchorX, in.x));
				float x1 = this.sx(Mth.lerp(p, in.anchorX, in.x + in.w));
				float y0 = this.sy(in.y);
				float y1 = this.sy(in.y + in.h * p);
				boolean inView = x1 >= this.frameLeft && x0 <= this.frameRight && y1 >= this.frameTop && this.sy(o.y) <= this.frameBottom;
				int px = Math.round(x0);
				int py = Math.round(y0);
				float words = in.words();
				if (inView) {
					this.painter
						.inlayPlate(
							g, own, in, px, py, Math.round(x1) - px, Math.round(y1) - py, this.sx(in.anchorX), this.sy(o.y) + this.radiusOf(o) + 2.0F, this.zoom, p, words, shown
						);
				}

				boolean still = inView && (this.journey == null || !this.journey.moving());
				if (still && words > 0.02F) {
					this.decorations(g, own, in.graph, words * shown);
				}

				float grown = in.drawn(0);
				if (still && grown > 0.01F) {
					float tx = this.sx(in.anchorX);
					float ty = this.sy(in.y + 0.75F);
					float over = this.sy(in.y + 0.75F + 0.25F);

					for (int i = 0; i < in.stems.size(); i++) {
						LinkView stem = in.stems.get(i);
						NodeView first = stem.to();
						float fx = this.sx(first.x);
						float fy = this.sy(first.y);
						float fr = this.radiusOf(first) + 2.0F;
						int points = 0;
						this.stemX[points] = tx;
						this.stemY[points++] = ty;
						if (i < in.fromAbove.length && in.fromAbove[i]) {
							if (Math.abs(fx - tx) >= 1.5F) {
								this.stemX[points] = tx;
								this.stemY[points++] = over;
								this.stemX[points] = fx;
								this.stemY[points++] = over;
							}

							this.stemX[points] = Math.abs(fx - tx) >= 1.5F ? fx : tx;
							this.stemY[points++] = fy - fr;
						} else {
							float side = this.sx(in.x + 0.25F);
							this.stemX[points] = tx;
							this.stemY[points++] = over;
							this.stemX[points] = side;
							this.stemY[points++] = over;
							this.stemX[points] = side;
							this.stemY[points++] = fy;
							this.stemX[points] = fx - fr;
							this.stemY[points++] = fy;
						}

						this.painter.stem(g, own, stem, this.stemX, this.stemY, points, this.zoom, shown * Math.min(1.0F, grown * 2.0F), grown);
					}
				}

				for (LinkView l : in.graph.links) {
					this.link(g, own, l, alpha);
				}

				for (LinkView l : in.cross) {
					this.link(g, own, l, alpha);
				}

				if (!in.graph.inlays.isEmpty()) {
					this.inlays(g, view, in.graph.inlays, alpha);
				}
			}
		}
	}

	private boolean drawNode(
		GuiGraphics g,
		QuestPainter.View view,
		NodeView n,
		@Nullable QuestCanvas.Journey way,
		float originX,
		float originY,
		float left,
		float right,
		float top,
		float bottom,
		float alpha,
		long now,
		boolean party
	) {
		float x = this.sx(n.x);
		float y = this.sy(n.y);
		float in = n.appear();
		float there = 1.0F;
		float pop;
		if (way != null) {
			float k = way.spread(n);
			x = Mth.lerp(k, originX, x);
			y = Mth.lerp(k, originY, y);
			there = way.presence(n);
			in = Math.min(in, k);
			pop = 0.35F + 0.65F * k;
		} else {
			pop = in >= 1.0F ? 1.0F : Ease.OUT_BACK.apply(in);
		}

		Inlay inlay = n.inlay;
		if (inlay != null) {
			float risen = inlay.presence(n);
			if (risen <= 0.01F) {
				return n.doneAtMs != 0L;
			}

			if (risen < 1.0F) {
				pop *= !inlay.closing() && !(inlay.outer() < 1.0F) ? Ease.OUT_BACK.apply(risen) : risen;
				there *= Math.min(1.0F, risen * 1.8F);
			}
		}

		float r = this.painter.radius() * this.zoom * n.size;
		if (x + r < left || x - r > right || y + r < top || y - r > bottom) {
			return n.doneAtMs != 0L;
		} else if (!(in <= 0.01F) && !(there <= 0.01F)) {
			float lit = n.lit.get();
			QuestPainter.View own = viewOf(n, view);
			if (n.openedAtMs != 0L) {
				float t = (float)(now - n.openedAtMs) / 1100.0F;
				if (t >= 1.0F || !party) {
					n.openedAtMs = 0L;
				} else if (way == null) {
					this.painter.opened(g, own, n, x, y, this.zoom * n.size, Math.max(0.0F, t));
				}
			}

			this.painter.node(g, own, n, x, y, this.zoom * n.size * pop, alpha * there * Math.min(1.0F, in * 1.6F) * (0.22F + 0.78F * lit));
			return n.doneAtMs != 0L;
		} else {
			return n.doneAtMs != 0L;
		}
	}

	private int controlX() {
		return this.getX() + this.getWidth() - this.coverR - 6 - 14;
	}

	private int controlY(int i) {
		return this.getY() + this.getHeight() - this.coverB - 6 - (3 - i) * 14 - (2 - i) * 2;
	}

	private boolean controlsShown() {
		return this.getWidth() - this.coverL - this.coverR >= 120 && this.getHeight() - this.coverT - this.coverB >= 80 && !this.graph.isEmpty();
	}

	private int controlAt(double mx, double my) {
		if (!this.controlsShown()) {
			return -1;
		} else {
			int x = this.controlX();
			if (!(mx < (double)x) && !(mx >= (double)(x + 14))) {
				for (int i = 0; i < 3; i++) {
					int y = this.controlY(i);
					if (my >= (double)y && my < (double)(y + 14)) {
						return i;
					}
				}

				return -1;
			} else {
				return -1;
			}
		}
	}

	private boolean overOverview(double mx, double my) {
		QuestPainter.Overview o = this.overview;
		return o != null
			&& this.overviewShown.get() > 0.5F
			&& mx >= (double)o.x()
			&& mx < (double)(o.x() + o.w())
			&& my >= (double)o.y()
			&& my < (double)(o.y() + o.h());
	}

	private static boolean meets(Rect r, int x, int y, int w, int h) {
		return r.x() < x + w && r.right() > x && r.y() < y + h && r.bottom() > y;
	}

	private boolean beyondView() {
		Rect f = this.free();
		return this.sx(this.graph.minX) < (float)(f.x() - 4)
			|| this.sx(this.graph.maxX) > (float)(f.right() + 4)
			|| this.sy(this.graph.minY) < (float)(f.y() - 4)
			|| this.sy(this.graph.maxY) > (float)(f.bottom() + 4);
	}

	private void corner(GuiGraphics g, QuestPainter.View view, Rect free, float alpha) {
		if (this.controlsShown() && this.journey == null) {
			SubtreePreview pv = this.preview;
			Rect plate = pv != null && pv.side() != 0 && pv.plate() > 0.05F ? pv.box() : null;
			int x = this.controlX();
			if (plate == null || !meets(plate, x, this.controlY(0), 14, this.controlY(2) + 14 - this.controlY(0))) {
				this.painter.control(g, view, QuestPainter.Control.ZOOM_IN, x, this.controlY(0), 14, this.controlHover[0].get(), this.toZoom < 2.499F);
				this.painter.control(g, view, QuestPainter.Control.ZOOM_OUT, x, this.controlY(1), 14, this.controlHover[1].get(), this.toZoom > 0.121F);
				this.painter.control(g, view, QuestPainter.Control.FIT, x, this.controlY(2), 14, this.controlHover[2].get(), true);
				long since = Clock.nowMs() - this.zoomedAtMs;
				if (this.zoomedAtMs != 0L && since < 1400L) {
					float a = since < 1000L ? 1.0F : 1.0F - (float)(since - 1000L) / 400.0F;
					this.painter.readout(g, view, Component.literal(Math.round(this.toZoom * 100.0F) + "%"), x - 4, this.controlY(2) + 1, a * alpha);
				}
			}

			int ox = free.x() + 6;
			int oy = free.bottom() - 6 - 58;
			this.overviewShown
				.set(QuestsConfig.get().overview && this.beyondView() && free.w() >= 260 && free.h() >= 150 && (plate == null || !meets(plate, ox, oy, 88, 58)));
			float shown = this.overviewShown.get();
			if (shown <= 0.02F) {
				this.overview = null;
			} else {
				float bw = this.graph.width() + 2.0F;
				float bh = this.graph.height() + 2.0F;
				float s = Math.min(82.0F / bw, 52.0F / bh);
				this.overview = new QuestPainter.Overview(
					ox,
					oy,
					88,
					58,
					s,
					(float)ox + 44.0F - (this.graph.minX + this.graph.maxX) / 2.0F * s,
					(float)oy + 29.0F - (this.graph.minY + this.graph.maxY) / 2.0F * s,
					this.tx((double)free.x()),
					this.ty((double)free.y()),
					this.tx((double)free.right()),
					this.ty((double)free.bottom())
				);
				this.painter.overview(g, view, this.graph, this.overview, shown * alpha);
			}
		} else {
			this.overview = null;
		}
	}

	private void overviewTo(double mx, double my) {
		QuestPainter.Overview o = this.overview;
		if (o != null && !(o.scale() <= 0.0F)) {
			this.own();
			this.aim((float)(mx - (double)o.ox()) / o.scale(), (float)(my - (double)o.oy()) / o.scale(), this.toZoom);
			this.clampTarget();
			this.camX = this.toX;
			this.camY = this.toY;
		}
	}

	@Override
	public void renderOver(GuiGraphics g, int mouseX, int mouseY) {
		if (this.hovered != null && this.journey == null && (this.tool == null || !this.tool.quiet()) && (!this.pressed || !this.dragged)) {
			if (this.coverL <= 0 && this.coverR <= 0 || !this.hovered.key.equals(this.selected)) {
				if (Clock.nowMs() - this.hoveredSinceMs >= 140L) {
					this.frameG = g;
					this.frameMouseX = mouseX;
					this.frameMouseY = mouseY;
					this.frameView = this.view(mouseX, mouseY);
					CanvasDraw.layer(g);
					CanvasDraw.batch(g, this.paintCard);
					this.frameG = null;
				}
			}
		}
	}

	private void paintCard() {
		NodeView n = this.hovered;
		if (n != null) {
			float motion = Theme.current().motion();
			float alpha = motion <= 0.0F ? 1.0F : Mth.clamp((float)(Clock.nowMs() - this.hoveredSinceMs - 140L) / (110.0F * motion), 0.0F, 1.0F);
			Minecraft mc = Minecraft.getInstance();
			int screenW = mc.getWindow().getGuiScaledWidth();
			int screenH = mc.getWindow().getGuiScaledHeight();
			int from = this.coverL > 0 ? this.getX() + this.coverL : 0;
			int to = this.coverR > 0 ? this.getX() + this.getWidth() - this.coverR : screenW;
			Rect bounds = new Rect(from, 0, Math.max(1, to - from), screenH);
			int side = 0;
			SubtreePreview pv = this.preview;
			if (pv != null && pv.owner == n && !pv.closing() && pv.side() != 0) {
				side = -pv.side();
			} else if (n.gateway() && !n.folds && !n.locked() && this.tool == null && QuestsConfig.get().subtreePreview) {
				Rect free = this.free();
				side = -SubtreePreview.preferredSide(this.sx(n.x), free.x(), free.right());
			}

			if (side != 0) {
				Rect free = this.free();
				int edge = Math.round(this.sx(n.x) + (float)side * this.radiusOf(n));
				bounds = side > 0 ? new Rect(edge, 0, Math.max(1, free.right() - edge), screenH) : new Rect(free.x(), 0, Math.max(1, edge - free.x()), screenH);
			}

			this.painter
				.card(this.frameG, n.inlay == null ? this.frameView : this.frameView.of(n.chapter), n, this.sx(n.x), this.sy(n.y), this.radiusOf(n), side, alpha, bounds);
		}
	}

	private void glide() {
		long now = Clock.nowMs();
		float dt = this.lastFrameMs == 0L ? 0.016F : Math.min(0.1F, (float)(now - this.lastFrameMs) / 1000.0F);
		this.lastFrameMs = now;
		float motion = Theme.current().motion();
		if (motion <= 0.0F) {
			this.camX = this.toX;
			this.camY = this.toY;
			this.zoom = this.toZoom;
		} else {
			float k = 1.0F - (float)Math.exp((double)(-dt * 16.0F / motion));
			this.camX = this.camX + (this.toX - this.camX) * k;
			this.camY = this.camY + (this.toY - this.camY) * k;
			this.zoom = this.zoom + (this.toZoom - this.zoom) * k;
			if (Math.abs(this.toZoom - this.zoom) < 5.0E-4F) {
				this.zoom = this.toZoom;
			}

			if (Math.abs(this.toX - this.camX) * this.unit() < 0.05F) {
				this.camX = this.toX;
			}

			if (Math.abs(this.toY - this.camY) * this.unit() < 0.05F) {
				this.camY = this.toY;
			}
		}
	}

	private void unfold(boolean quiet) {
		QuestBook book = QuestsState.book();
		boolean wanted = !quiet
			&& this.hovered != null
			&& this.hovered.gateway()
			&& !this.hovered.folds
			&& !this.hovered.locked()
			&& book != null
			&& QuestsConfig.get().subtreePreview
			&& book.chapter(this.hovered.quest.subtree) != null;
		if (this.preview != null && (!wanted || this.preview.owner != this.hovered)) {
			this.preview.close();
		}

		if (this.preview != null && this.preview.gone()) {
			this.preview = null;
		}

		if (wanted
			&& (this.preview == null || this.preview.closing() && this.preview.owner != this.hovered)
			&& Clock.nowMs() - this.hoveredSinceMs >= (long)QuestsConfig.get().subtreePreviewDelayMs) {
			this.preview = new SubtreePreview(this.hovered, book.chapter(this.hovered.quest.subtree));
		}
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!this.active || !this.visible || !this.contains(mouseX, mouseY)) {
			return false;
		} else if (this.journey != null) {
			return true;
		} else {
			if (button == 0) {
				int control = this.controlAt(mouseX, mouseY);
				if (control >= 0) {
					this.press(control);
					return true;
				}

				if (this.overOverview(mouseX, mouseY)) {
					this.overviewDrag = true;
					this.overviewTo(mouseX, mouseY);
					return true;
				}
			}

			NodeView node = this.nodeAt(mouseX, mouseY);
			if (this.tool != null && this.tool.mouseClicked(this, node, mouseX, mouseY, button)) {
				this.toolPressed = true;
				return true;
			} else if (button != 1) {
				if (button != 0 && button != 2) {
					return false;
				} else {
					this.pressed = true;
					this.dragged = false;
					this.pressButton = button;
					this.pressX = mouseX;
					this.pressY = mouseY;
					this.pressedNode = button == 0 ? node : null;
					return true;
				}
			} else {
				return this.onContext != null && this.onContext.open(node == null ? null : node.key, mouseX, mouseY, this.tx(mouseX), this.ty(mouseY));
			}
		}
	}

	private void press(int control) {
		double cx = (double)(this.getX() + this.coverL) + (double)(this.getWidth() - this.coverL - this.coverR) / 2.0;
		double cy = (double)(this.getY() + this.coverT) + (double)(this.getHeight() - this.coverT - this.coverB) / 2.0;
		switch (control) {
			case 0:
				this.zoomAbout(1.25F, cx, cy);
				break;
			case 1:
				this.zoomAbout(0.8F, cx, cy);
				break;
			default:
				this.fitNow();
		}

		CFHGuiSounds.tick();
	}

	@Override
	public boolean dragging() {
		return this.pressed || this.overviewDrag || this.toolPressed;
	}

	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (this.overviewDrag) {
			this.overviewTo(mouseX, mouseY);
			return true;
		} else if (this.tool != null && this.tool.mouseDragged(this, mouseX, mouseY, button, dragX, dragY)) {
			return true;
		} else if (!this.pressed) {
			return false;
		} else if (!this.dragged && Math.abs(mouseX - this.pressX) + Math.abs(mouseY - this.pressY) < 3.0) {
			return true;
		} else {
			this.dragged = true;
			this.own();
			float dx = (float)(dragX / (double)this.unit());
			float dy = (float)(dragY / (double)this.unit());
			this.toX -= dx;
			this.toY -= dy;
			this.clampTarget();
			this.camX = this.toX;
			this.camY = this.toY;
			return true;
		}
	}

	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (this.overviewDrag) {
			this.overviewDrag = false;
			return true;
		} else if (this.toolPressed) {
			this.toolPressed = false;
			this.pressed = false;
			if (this.tool != null) {
				this.tool.mouseReleased(this, mouseX, mouseY, button);
			}

			return true;
		} else if (!this.pressed) {
			return false;
		} else {
			this.pressed = false;
			NodeView node = this.pressedNode;
			this.pressedNode = null;
			if (!this.dragged && this.pressButton == 0) {
				if (node != null && node == this.nodeAt(mouseX, mouseY)) {
					this.choose(node);
				} else if (node == null && this.pointedDecoration != null && this.follow(this.pointedDecoration)) {
					this.pointedDecoration = null;
				} else if (node == null) {
					this.select(null);
					if (this.onOpen != null) {
						this.onOpen.accept(null);
					}
				}
			}

			this.dragged = false;
			return true;
		}
	}

	private void choose(NodeView node) {
		long now = Clock.nowMs();
		boolean twice = node.key.equals(this.lastClickKey) && now - this.lastClickMs < 320L;
		this.lastClickKey = node.key;
		this.lastClickMs = now;
		CFHGuiSounds.click();
		if (!node.folds || !this.opensInline()) {
			this.select(node.key);
			if (twice && node.gateway() && !node.locked() && this.onEnter != null) {
				this.lastClickKey = null;
				this.onEnter.accept(node.key);
			} else {
				if (this.onOpen != null) {
					this.onOpen.accept(node.key);
				}
			}
		} else if (twice) {
			this.lastClickKey = null;
			this.setUnfolded(node.key, true);
			this.select(node.key);
			if (this.onOpen != null) {
				this.onOpen.accept(node.key);
			}
		} else {
			boolean open = !this.unfolded(node.key);
			if (!node.key.equals(this.selected)) {
				if (this.selected != null && this.onOpen != null) {
					this.onOpen.accept(null);
				}

				if (this.coverChanged) {
					this.covered();
				}

				this.select(node.key);
			}

			this.setUnfolded(node.key, open);
		}
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (this.contains(mouseX, mouseY) && scrollY != 0.0 && this.journey == null) {
			float factor = (float)Math.pow(1.18, scrollY);
			if (QuestsConfig.get().zoomToPointer) {
				this.zoomAbout(factor, mouseX, mouseY);
			} else {
				this.zoomAbout(
					factor,
					(double)(this.getX() + this.coverL) + (double)(this.getWidth() - this.coverL - this.coverR) / 2.0,
					(double)(this.getY() + this.coverT) + (double)(this.getHeight() - this.coverT - this.coverB) / 2.0
				);
			}

			return true;
		} else {
			return false;
		}
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		return this.isFocused() && this.key(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean key(int keyCode, int scanCode, int modifiers) {
		if (this.journey != null) {
			return false;
		} else if (this.tool != null && this.tool.keyPressed(this, keyCode, scanCode, modifiers)) {
			return true;
		} else {
			float step = 1.5F / Math.max(0.5F, this.zoom);
			double cx = (double)(this.getX() + this.coverL) + (double)(this.getWidth() - this.coverL - this.coverR) / 2.0;
			double cy = (double)(this.getY() + this.coverT) + (double)(this.getHeight() - this.coverT - this.coverB) / 2.0;
			switch (keyCode) {
				case 32:
				case 268:
					this.fitNow();
					return true;
				case 45:
				case 333:
					this.zoomAbout(0.8F, cx, cy);
					return true;
				case 61:
				case 334:
					this.zoomAbout(1.25F, cx, cy);
					return true;
				case 257:
				case 335:
					NodeView n = this.graph.shown(this.selected);
					if (n == null) {
						return false;
					}

					CFHGuiSounds.click();
					if ((modifiers & 1) != 0 && n.folds && this.opensInline()) {
						this.setUnfolded(n.key, !this.unfolded(n.key));
					} else if (this.onOpen != null) {
						this.onOpen.accept(n.key);
					}

					return true;
				case 258:
					return this.step((modifiers & 1) != 0 ? -1 : 1);
				case 262:
					this.pan(step, 0.0F);
					return true;
				case 263:
					this.pan(-step, 0.0F);
					return true;
				case 264:
					this.pan(0.0F, step);
					return true;
				case 265:
					this.pan(0.0F, -step);
					return true;
				default:
					return false;
			}
		}
	}

	public void onClick(double mouseX, double mouseY) {
	}

	protected void updateWidgetNarration(NarrationElementOutput out) {
		this.defaultButtonNarrationText(out);
	}

	private final class Journey {
		final TreeView.Travel kind;
		final GraphModel next;
		final String through;
		int stretch;
		long startMs = Clock.nowMs();
		float fromX = QuestCanvas.this.camX;
		float fromY = QuestCanvas.this.camY;
		float fromZoom = QuestCanvas.this.zoom;
		float aimX;
		float aimY;
		float aimZoom;
		@Nullable
		final NodeView gate;
		final float gateZoom;

		Journey(final TreeView.Travel kind, final GraphModel next, final String through) {
			this.kind = kind;
			this.next = next;
			this.through = through;
			this.gate = kind == TreeView.Travel.INTO ? QuestCanvas.this.graph.shown(through) : next.shown(through);
			if (kind == TreeView.Travel.INTO) {
				this.flyTo(QuestCanvas.this.graph.shown(through), QuestCanvas.this.zoom);
			}

			float[] left = QuestCanvas.LEFT_AT.get(QuestCanvas.viewKey(next));
			float back = left != null ? left[2] : QuestCanvas.this.firstLookZoom(next);
			this.gateZoom = kind == TreeView.Travel.INTO ? this.aimZoom : Mth.clamp(Math.max(back * 2.4F, 2.0F), back, 3.4F);
		}

		void gate(GuiGraphics g, QuestPainter.View view, float originX, float originY, float alpha) {
			NodeView n = this.gate;
			if (n != null) {
				QuestPainter.View own = view.of(n.chapter);
				if (this.kind == TreeView.Travel.INTO) {
					if (this.stretch == 0) {
						if (this.veil() > 0.01F) {
							QuestCanvas.this.painter.node(g, own, n, QuestCanvas.this.sx(n.x), QuestCanvas.this.sy(n.y), QuestCanvas.this.zoom * n.size, alpha);
						}
					} else {
						float t = Mth.clamp(this.t(this.growMs()) * (float)this.growMs() / 200.0F, 0.0F, 1.0F);
						if (t < 1.0F) {
							QuestCanvas.this.painter.node(g, own, n, originX, originY, this.aimZoom * n.size * (1.0F - Ease.IN_OUT_CUBIC.apply(t)), alpha);
						}
					}
				} else if (this.stretch == 0) {
					float t = Mth.clamp((this.t(this.foldMs()) * (float)this.foldMs() - (float)(this.foldMs() - 200)) / 200.0F, 0.0F, 1.0F);
					if (t > 0.0F) {
						QuestCanvas.this.painter.node(g, own, n, originX, originY, this.gateZoom * n.size * Ease.OUT_CUBIC.apply(t), alpha);
					}
				} else if (this.veil() > 0.01F) {
					QuestCanvas.this.painter.node(g, own, n, QuestCanvas.this.sx(n.x), QuestCanvas.this.sy(n.y), QuestCanvas.this.zoom * n.size, alpha);
				}
			}
		}

		private void flyTo(@Nullable NodeView n, float fromZoom) {
			this.aimZoom = Mth.clamp(Math.max(fromZoom * 2.4F, 2.0F), fromZoom, 3.4F);
			float u = QuestCanvas.this.painter.unit() * this.aimZoom;
			this.aimX = (n == null ? QuestCanvas.this.camX : n.x) - QuestCanvas.this.freeDx() / u;
			this.aimY = (n == null ? QuestCanvas.this.camY : n.y) - QuestCanvas.this.freeDy() / u;
		}

		float t(int ms) {
			return Mth.clamp((float)(Clock.nowMs() - this.startMs) / ((float)ms * Math.max(0.05F, Theme.current().motion())), 0.0F, 1.0F);
		}

		int growMs() {
			return 300 + Math.min(this.next.deepest, 10) * 60;
		}

		int foldMs() {
			return 190 + Math.min(QuestCanvas.this.graph.deepest, 10) * 28;
		}

		boolean advance() {
			if (this.kind == TreeView.Travel.INTO) {
				if (this.stretch == 0) {
					float t = this.t(330);
					float e = Ease.IN_OUT_CUBIC.apply(t);
					QuestCanvas.this.camX = Mth.lerp(e, this.fromX, this.aimX);
					QuestCanvas.this.camY = Mth.lerp(e, this.fromY, this.aimY);
					QuestCanvas.this.zoom = Mth.lerp(e, this.fromZoom, this.aimZoom);
					if (t >= 1.0F) {
						this.arrive();
					}

					return false;
				} else {
					return this.t(this.growMs()) >= 1.0F;
				}
			} else if (this.stretch == 0) {
				if (this.t(this.foldMs()) >= 1.0F) {
					this.arrive();
				}

				return false;
			} else {
				float t = this.t(330);
				float e = Ease.OUT_CUBIC.apply(t);
				QuestCanvas.this.camX = Mth.lerp(e, this.aimX, QuestCanvas.this.toX);
				QuestCanvas.this.camY = Mth.lerp(e, this.aimY, QuestCanvas.this.toY);
				QuestCanvas.this.zoom = Mth.lerp(e, this.aimZoom, QuestCanvas.this.toZoom);
				if (t < 1.0F) {
					return false;
				} else {
					NodeView n = QuestCanvas.this.graph.shown(this.through);
					if (n != null) {
						n.openedAtMs = Clock.nowMs();
					}

					return true;
				}
			}
		}

		private void arrive() {
			QuestCanvas.this.graph = this.next;
			QuestCanvas.this.selected = null;
			this.stretch = 1;
			this.startMs = Clock.nowMs();
			this.restoreTarget();
			if (this.kind == TreeView.Travel.INTO) {
				QuestCanvas.this.camX = QuestCanvas.this.toX;
				QuestCanvas.this.camY = QuestCanvas.this.toY;
				QuestCanvas.this.zoom = QuestCanvas.this.toZoom;
			} else {
				this.flyTo(QuestCanvas.this.graph.shown(this.through), QuestCanvas.this.toZoom);
				QuestCanvas.this.camX = this.aimX;
				QuestCanvas.this.camY = this.aimY;
				QuestCanvas.this.zoom = this.aimZoom;
			}
		}

		private void restoreTarget() {
			QuestCanvas.this.coverChanged = false;
			QuestCanvas.this.before = null;
			if (!QuestCanvas.this.toLeft()) {
				QuestCanvas.this.firstLook();
			}
		}

		float veil() {
			if (this.kind == TreeView.Travel.INTO) {
				return this.stretch == 0
					? Mth.clamp((this.t(330) - 0.45F) / 0.5F, 0.0F, 1.0F)
					: 1.0F - Mth.clamp(this.t(this.growMs()) * (float)this.growMs() / 200.0F, 0.0F, 1.0F);
			} else {
				return this.stretch == 0 ? Mth.clamp((this.t(this.foldMs()) - 0.55F) / 0.45F, 0.0F, 1.0F) : 1.0F - Mth.clamp(this.t(330) / 0.55F, 0.0F, 1.0F);
			}
		}

		Chapter beyond() {
			return this.stretch == 0 ? this.next.chapter : QuestCanvas.this.graph.chapter;
		}

		boolean moving() {
			return this.kind == TreeView.Travel.INTO ? this.stretch == 1 : this.stretch == 0;
		}

		float spread(NodeView n) {
			if (this.kind == TreeView.Travel.INTO && this.stretch == 1) {
				float ms = this.t(this.growMs()) * (float)this.growMs();
				return Ease.OUT_CUBIC.apply(Mth.clamp((ms - (float)(Math.min(n.depth, 10) * 60)) / 300.0F, 0.0F, 1.0F));
			} else if (this.kind == TreeView.Travel.OUT && this.stretch == 0) {
				float ms = this.t(this.foldMs()) * (float)this.foldMs();
				int turn = Math.min(QuestCanvas.this.graph.deepest, 10) - Math.min(n.depth, 10);
				return 1.0F - Ease.IN_OUT_CUBIC.apply(Mth.clamp((ms - (float)(turn * 28)) / 190.0F, 0.0F, 1.0F));
			} else {
				return 1.0F;
			}
		}

		float presence(NodeView n) {
			if (this.kind == TreeView.Travel.INTO && this.stretch == 0) {
				return n.key.equals(this.through) ? 1.0F : 1.0F - Mth.clamp(this.t(330) / 0.7F, 0.0F, 1.0F);
			} else if (this.kind == TreeView.Travel.OUT && this.stretch == 1) {
				return n.key.equals(this.through) ? 1.0F : Mth.clamp((this.t(330) - 0.15F) / 0.6F, 0.0F, 1.0F);
			} else {
				return Mth.clamp(this.spread(n) * 1.6F, 0.0F, 1.0F);
			}
		}

		float plates() {
			if (this.kind == TreeView.Travel.INTO) {
				return this.stretch == 0 ? 1.0F - Mth.clamp(this.t(330) / 0.7F, 0.0F, 1.0F) : Mth.clamp((this.t(this.growMs()) - 0.8F) / 0.2F, 0.0F, 1.0F);
			} else {
				return this.stretch == 0 ? 1.0F - Mth.clamp(this.t(this.foldMs()) / 0.2F, 0.0F, 1.0F) : Mth.clamp((this.t(330) - 0.15F) / 0.6F, 0.0F, 1.0F);
			}
		}
	}
}
