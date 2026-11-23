package com.onehumanawa.cfhcore.content.gui.quests.client.overhaul;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.stage.Stage;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageCamera;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageLighting;
import com.onehumanawa.cfhcore.content.gui.core.stage.anim.IdleMotion;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.FxNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PivotNode;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import com.onehumanawa.cfhcore.content.gui.quests.QuestsConfig;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.client.ui.QuestBookScreen;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.BookStats;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.CanvasDraw;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.GraphModel;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.NodeView;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestPainter;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.CFHGuiQuestPainter;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.SubtreePreview;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.TreeFilter;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.TreeView;
import com.onehumanawa.cfhcore.content.gui.quests.logic.Rules;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

final class IslandView extends CFHGuiWidget implements TreeView {
	private static final Map<String, float[]> LEFT_AT = new HashMap<>();
	@Nullable
	static IslandView last;
	private static final float FOV = 34.0F;
	private static final float TAN = (float)Math.tan(Math.toRadians(17.0));
	private static final float AIM = 1.0F;
	private static final float ZOOM_MIN = 0.14F;
	private static final float ZOOM_MAX = 3.6F;
	private static final float READABLE = 0.45F;
	private static final float CLOSE = 0.85F;
	private static final float FIT_MAX = 1.3F;
	private static final float NAMES_FROM = 1.25F;
	private static final int OWN = 0;
	private static final int FIRST_LOOK = 1;
	private static final int FITTED = 2;
	private static final int KEPT = 6;
	private static final int CONTROL = 14;
	private static final int CONTROL_GAP = 2;
	private static final int CORNER = 6;
	private static final int ARRIVE_MS = 620;
	private static final int LEAVE_MS = 340;
	private static final int ARRIVE_AFTER_MS = 200;
	private static final float RISE = 9.0F;
	private static final int SETTLE_MS = 220;
	private static final float SETTLE_RISE = 1.4F;
	private static final int FLY_MS = 720;
	private static final int GROW_MS = 1000;
	private static final int PART_MS = 340;
	private static final int BACK_MS = 880;
	private final QuestBookScreen screen;
	private final CFHGuiQuestPainter painter = new CFHGuiQuestPainter();
	private final Island.Lens lens = new Island.Lens();
	private final boolean inPlace;
	private boolean unseen = true;
	private int arriveMs = 620;
	private float arriveRise = 9.0F;
	@Nullable
	private Stage stage;
	@Nullable
	private Props props;
	@Nullable
	private Things things;
	@Nullable
	private Air air;
	@Nullable
	private FxNode puffs;
	private final LinkedHashMap<String, Island> islands = new LinkedHashMap<>(8, 0.75F, true);
	@Nullable
	private Chapter shown;
	@Nullable
	private Island main;
	@Nullable
	private Island leaving;
	@Nullable
	private IslandTheme emptyAir;
	private boolean arriving;
	private long arrivedAtMs;
	private long leftAtMs;
	private long shownAtMs;
	private int hoverLamp = -1;
	@Nullable
	private Island.Spot lampSpot;
	private int frame;
	private float camX;
	private float camZ;
	private float dist = 40.0F;
	private float toX;
	private float toZ;
	private float toDist = 40.0F;
	private int held = 0;
	private long lastFrameMs;
	private int coverL;
	private int coverT;
	private int coverR;
	private int coverB;
	private boolean footChanged;
	private boolean pressed;
	private boolean dragged;
	private int pressButton;
	private double pressX;
	private double pressY;
	private float grabX;
	private float grabZ;
	@Nullable
	private String pressedKey;
	@Nullable
	private NodeView hovered;
	private long hoveredSinceMs;
	@Nullable
	private String selected;
	private long lastClickMs;
	@Nullable
	private String lastClickKey;
	@Nullable
	private IslandView.Preview preview;
	@Nullable
	private IslandView.Journey journey;
	@Nullable
	private Consumer<String> onOpen;
	@Nullable
	private Consumer<String> onEnter;
	@Nullable
	private TreeView.ContextHandler onContext;
	private TreeFilter filter = TreeFilter.ALL;
	private boolean showHidden;
	private final Anim[] controlHover = new Anim[]{new Anim(0.0F, 120, Ease.OUT_CUBIC), new Anim(0.0F, 120, Ease.OUT_CUBIC), new Anim(0.0F, 120, Ease.OUT_CUBIC)};
	private int hoveredControl = -1;
	private long controlSinceMs;
	private long zoomedAtMs;
	private GuiGraphics frameG;
	private int frameMouseX;
	private int frameMouseY;
	private final Runnable paintOver = this::paintOver;
	private final Runnable paintCard = this::paintCard;
	private final Vector3f eye = new Vector3f();
	private final Vector3f look = new Vector3f();
	private final Vector3f ray = new Vector3f();
	private final Vector3f rayFrom = new Vector3f();
	private final Vector3f ground = new Vector3f();
	private final Vector3f spare = new Vector3f();
	private final Vector3f spare2 = new Vector3f();
	private final Matrix4f lensMatrix = new Matrix4f();
	private final Vector4f lensPoint = new Vector4f();
	private final float[] box = new float[4];
	private static final boolean TIMED = Boolean.getBoolean("cfh.quests.frameTimes");
	private int timedFrames;
	private final int[] taken = new int[384];
	private int takenCount;

	IslandView(QuestBookScreen screen) {
		super(0, 0, 10, 10, Component.translatable("cfhgui_quests.overhaul.island"));
		this.screen = screen;
		this.inPlace = Minecraft.getInstance().screen instanceof QuestBookScreen;
		this.silent();
		if (IslandShots.ON) {
			last = this;
		}
	}

	QuestBookScreen screen() {
		return this.screen;
	}

	@Override
	public AbstractWidget widget() {
		return this;
	}

	@Override
	public boolean fillsScreen() {
		return true;
	}

	@Override
	public void place(int x, int y, int width, int height) {
		boolean changed = width != this.getWidth() || height != this.getHeight() || x != this.getX() || y != this.getY();
		this.setX(x);
		this.setY(y);
		this.setWidth(width);
		this.setHeight(height);
		this.lens();
		if (changed && this.main != null && this.journey == null) {
			this.footChanged = true;
		}
	}

	@Override
	public void cover(int left, int top, int right, int bottom) {
		if (this.coverB != Math.max(0, bottom)) {
			this.footChanged = true;
		}

		this.coverL = Math.max(0, left);
		this.coverT = Math.max(0, top);
		this.coverR = Math.max(0, right);
		this.coverB = Math.max(0, bottom);
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

	@Nullable
	@Override
	public Chapter chapter() {
		return this.shown;
	}

	@Override
	public boolean travelling() {
		return this.journey != null;
	}

	@Override
	public TreeFilter filter() {
		return this.filter;
	}

	@Override
	public void filter(TreeFilter f) {
		this.filter = f;
		if (this.main != null) {
			this.main.graph().filter(f, QuestsConfig.get().dimCompleted);
		}
	}

	@Override
	public void showHidden(boolean on) {
		this.showHidden = on;
		if (this.main != null) {
			this.main.graph().showHidden(on);
			this.main.sync(false);
		}
	}

	@Nullable
	Island island() {
		return this.main;
	}

	private void lens() {
		if (this.screen.width > 0 && this.screen.height > 0) {
			this.lens.x = 0;
			this.lens.y = 0;
			this.lens.w = this.screen.width;
			this.lens.h = this.screen.height;
			this.lens.tan = TAN;
			this.lens.px = (float)Math.max(1.0, Minecraft.getInstance().getWindow().getGuiScale());
		}
	}

	private void ensureStage() {
		if (this.stage == null || this.stage.isClosed()) {
			this.islands.clear();
			this.main = this.leaving = null;
			this.preview = null;
			this.journey = null;
			if (this.hovered != null) {
				this.hovered.hover.set(false);
			}

			this.hovered = null;
			this.lampSpot = null;
			this.arriving = false;
			this.emptyAir = null;
			Stage s = new Stage().bind(this.screen);
			this.stage = s;
			s.soft(true);
			s.focusOutline(false);
			s.resolutionScale(Minecraft.getInstance().getWindow().getWidth() <= 2048 ? 2.0F : 1.5F);
			s.camera().clip(0.3F, 900.0F).fov(34.0F);
			s.camera().idle(IdleMotion.sway(27000.0F, 1.1F, 0.22F, 0.03F));
			this.air = new Air(s);
			FxNode ring = (FxNode)s.add(FxNode.cloudRing(9, 1.0F, 0.45F, 0.0F, 0.3F, 91L));
			ring.spin(9.0F).visible(false);
			this.puffs = ring;
			final Props made = new Props(s.level());
			final Things held = new Things(s.level());
			this.props = made;
			this.things = held;
			s.add(
				new PivotNode() {
					public void dispose() {
						int items = Things.held();
						int blocks = Props.held();
						made.close();
						held.close();
						if (IslandView.TIMED) {
							CFHGuiQuests.LOGGER
								.info(
									"[CFHGui Quests] island scene closed: {} item meshes and {} block meshes given back, {} and {} still held",
									new Object[]{items - Things.held(), blocks - Props.held(), Things.held(), Props.held()}
								);
						}
					}
				}
			);
			if (this.shown != null) {
				String chosen = this.selected;
				this.take(this.islandFor(this.shown, true), false);
				if (this.main != null && this.main.graph().shown(chosen) != null) {
					this.select(chosen);
				}
			}
		}
	}

	private GraphModel model(Chapter chapter, boolean own) {
		GraphModel m = GraphModel.of(chapter);
		if (own) {
			m.showHidden(this.showHidden);
			m.filter(this.filter, QuestsConfig.get().dimCompleted);
		}

		return m;
	}

	private Island islandFor(Chapter chapter, boolean own) {
		GraphModel model = this.model(chapter, own);
		Island had = this.islands.get(chapter.id);
		if (had != null && had.layout.signature == IslandBuilder.signature(chapter)) {
			had.bind(model);
			return had;
		} else {
			if (had != null) {
				this.discard(had);
			}

			long began = System.nanoTime();
			Island made = new Island(this.stage, this.props, this.things, this.lens, chapter, model);
			made.show(false, false);
			this.islands.put(chapter.id, made);
			if (TIMED) {
				CFHGuiQuests.LOGGER
					.info(
						"[CFHGui Quests] island of {}: {} quests, {} by {} cells, {} of land, planned and placed in {} ms",
						new Object[]{
							chapter.id,
							model.all.size(),
							made.layout.w,
							made.layout.d,
							made.layout.landCells,
							String.format(Locale.ROOT, "%.1f", (double)(System.nanoTime() - began) / 1000000.0)
						}
					);
			}

			Iterator<Island> it = this.islands.values().iterator();

			while (this.islands.size() > 6 && it.hasNext()) {
				Island old = it.next();
				if (old != made && !this.inUse(old)) {
					it.remove();
					old.dispose();
				}
			}

			return made;
		}
	}

	private boolean inUse(Island island) {
		return island == this.main
			|| island == this.leaving
			|| this.preview != null && this.preview.island == island
			|| this.journey != null && (this.journey.from == island || this.journey.to == island);
	}

	private void discard(Island island) {
		this.islands.remove(island.chapter.id);
		if (this.main == island) {
			this.main = null;
		}

		if (this.leaving == island) {
			this.leaving = null;
		}

		if (this.preview != null && this.preview.island == island) {
			this.preview = null;
		}

		island.dispose();
	}

	private void lamps(Island island) {
		StageLighting l = this.stage.lighting();
		l.clearLamps();
		int i = 0;

		for (IslandBuilder.Lamp lamp : island.layout.lamps) {
			l.lamp(i, lamp.x(), lamp.y(), lamp.z(), lamp.r(), lamp.g(), lamp.b(), lamp.reach());
			l.lampFlicker(i, 0.07F);
			i++;
		}

		this.hoverLamp = i;
		this.lampSpot = null;
	}

	@Override
	public void show(Chapter chapter, TreeView.Travel how, @Nullable String through) {
		this.lens();
		this.ensureStage();
		boolean motion = Theme.current().motion() > 0.0F;
		this.remember();
		this.drop();
		Island next = this.islandFor(chapter, true);
		Island from = this.main;
		this.shown = chapter;
		if (motion && from != null && from != next && !next.isEmpty()) {
			boolean into = how == TreeView.Travel.INTO && from.graph().shown(through) != null && from.spot(through) != null;
			boolean out = how == TreeView.Travel.OUT && next.graph().shown(through) != null && next.spot(through) != null;
			if (into || out) {
				from.place(0.0F, 0.0F, 0.0F, 1.0F, 1.0F);
				from.settle();
				this.arriving = false;
				this.journey = new IslandView.Journey(how, from, next, through);
				whoosh(into ? 1.2F : 0.92F);
				return;
			}
		}

		this.take(next, how != TreeView.Travel.NONE && motion);
	}

	private void take(Island next, boolean entrance) {
		Island old = this.main == next ? null : this.main;
		if (this.leaving != null) {
			if (this.leaving != next) {
				this.leaving.show(false, false);
			}

			this.leaving = null;
		}

		long now = Clock.nowMs();
		if (old != null) {
			if (entrance) {
				this.leaving = old;
				this.leftAtMs = now;
				old.show(true, false);
			} else {
				old.show(false, false);
			}
		}

		this.main = next;
		this.selected = null;
		next.show(true, true);
		next.place(0.0F, 0.0F, 0.0F, 1.0F, 1.0F);
		this.air.theme(next.theme, !entrance || old == null);
		this.lamps(next);
		this.restoreView();
		this.camX = this.toX;
		this.camZ = this.toZ;
		this.dist = this.toDist;
		this.arriving = entrance;
		this.shownAtMs = now;
		if (entrance) {
			boolean settle = this.inPlace && this.unseen;
			int wait = old == null ? 0 : 200;
			this.arrivedAtMs = now + (long)Math.round((float)wait * Theme.current().motion());
			this.arriveMs = settle ? 220 : 620;
			this.arriveRise = settle ? 1.4F : 9.0F;
			next.place(0.0F, -this.arriveRise, 0.0F, 1.0F, 0.0F);
			if (settle) {
				next.settle();
			} else {
				next.entrance(wait + 300);
			}

			this.dist = this.toDist * (settle ? 1.03F : 1.12F);
		}
	}

	@Override
	public void rebuild(Chapter chapter) {
		this.lens();
		this.ensureStage();
		String chosen = this.selected;
		this.drop();
		this.shown = chapter;
		Island next = this.islandFor(chapter, true);
		Island old = this.main;
		if (old != null && old != next) {
			old.show(false, false);
		}

		if (this.leaving != null) {
			this.leaving.show(false, false);
			this.leaving = null;
		}

		this.main = next;
		next.show(true, true);
		next.place(0.0F, 0.0F, 0.0F, 1.0F, 1.0F);
		this.arriving = false;
		this.air.theme(next.theme, true);
		this.lamps(next);
		this.clampTarget();
		this.selected = null;
		if (next.graph().shown(chosen) != null) {
			this.select(chosen);
		}
	}

	@Override
	public void refresh() {
		if (this.main != null) {
			this.main.graph().refresh(true);
			this.main.sync(true);
			if (this.preview != null) {
				this.preview.island.graph().refresh(false);
				this.preview.island.sync(false);
			}

			if (this.journey != null) {
				Island other = this.journey.stretch == 0 ? this.journey.to : this.journey.from;
				other.graph().refresh(false);
				other.sync(false);
			}

			if (this.selected != null && this.main.graph().shown(this.selected) == null) {
				this.selected = null;
			}
		}
	}

	private void drop() {
		if (this.hovered != null) {
			this.hovered.hover.set(false);
		}

		this.hovered = null;
		this.pressed = false;
		this.pressedKey = null;
		if (this.preview != null) {
			this.preview.island.show(false, false);
			this.preview = null;
		}

		if (this.journey != null) {
			Island other = this.journey.stretch == 0 ? this.journey.to : this.journey.from;
			if (other != this.main) {
				other.show(false, false);
			}

			this.journey.from.pass(null, 0.0F, 0);
			this.journey.to.pass(null, 0.0F, 0);
			this.journey = null;
		}
	}

	@Override
	public void remember() {
		if (this.journey == null && this.main != null && this.shown != null) {
			if (this.held == 1) {
				LEFT_AT.remove(this.shown.id);
			} else {
				LEFT_AT.put(this.shown.id, new float[]{this.toX, this.toZ, this.toDist});
			}
		}
	}

	private void restoreView() {
		this.footChanged = false;
		float[] left = this.shown == null ? null : LEFT_AT.get(this.shown.id);
		if (left != null) {
			this.toX = left[0];
			this.toZ = left[1];
			this.toDist = left[2];
			this.held = 0;
			this.clampTarget();
		} else {
			this.firstLook();
		}
	}

	private Rect free() {
		return new Rect(
			this.getX() + this.coverL,
			this.getY() + this.coverT,
			Math.max(1, this.getWidth() - this.coverL - this.coverR),
			Math.max(1, this.getHeight() - this.coverT - this.coverB)
		);
	}

	private float zoomOf(float d) {
		return 2.0F * (float)this.lens.h * 0.5F / (Math.max(0.01F, d) * TAN * 30.0F);
	}

	private float distOf(float zoom) {
		return 2.0F * (float)this.lens.h * 0.5F / (Math.max(0.01F, zoom) * TAN * 30.0F);
	}

	private float farthest() {
		float whole = this.main == null ? 0.0F : this.main.layout.radius() * 5.2F + 30.0F;
		return Math.min(620.0F, Math.max(this.distOf(0.28F), whole));
	}

	private float pitchOf(float d) {
		float t = Mth.clamp((float)(Math.log((double)(this.zoomOf(d) / 0.4F)) / Math.log(9.0)), 0.0F, 1.0F);
		return (float)Math.toRadians((double)Mth.lerp(t, 56.0F, 17.0F));
	}

	private void rayAt(double sx, double sy, float pitch, Vector3f out) {
		float nx = (float)((sx - (double)this.lens.x) / (double)this.lens.w * 2.0 - 1.0);
		float ny = (float)(1.0 - (sy - (double)this.lens.y) / (double)this.lens.h * 2.0);
		float tanH = TAN * (float)this.lens.w / (float)this.lens.h;
		float sin = Mth.sin(pitch);
		float cos = Mth.cos(pitch);
		out.set(nx * tanH, -sin + ny * TAN * cos, -cos - ny * TAN * sin).normalize();
	}

	private void pose(float vx, float vz, float d, Vector3f eyeOut, Vector3f lookOut) {
		float pitch = this.pitchOf(d);
		this.rayAt((double)this.getX() + (double)this.getWidth() / 2.0, (double)this.getY() + (double)this.getHeight() / 2.0, pitch, this.ray);
		eyeOut.set(vx - this.ray.x * d, 1.0F - this.ray.y * d, vz - this.ray.z * d);
		lookOut.set(eyeOut.x, eyeOut.y - Mth.sin(pitch) * d, eyeOut.z - Mth.cos(pitch) * d);
	}

	private Vector3f reach(double sx, double sy, float d, float y, Vector3f out) {
		this.pose(0.0F, 0.0F, d, this.rayFrom, this.spare2);
		this.rayAt(sx, sy, this.pitchOf(d), this.ray);
		float t = this.ray.y > -0.02F ? 600.0F : (y - this.rayFrom.y) / this.ray.y;
		return out.set(this.rayFrom.x + this.ray.x * t, y, this.rayFrom.z + this.ray.z * t);
	}

	private void aim(float x, float y, float z, float d) {
		Rect f = this.free();
		this.reach((double)f.x() + (double)f.w() / 2.0, (double)f.y() + (double)f.h() / 2.0, d, y, this.ground);
		this.toX = x - this.ground.x;
		this.toZ = z - this.ground.z;
		this.toDist = d;
	}

	private void lensFor(float vx, float vz, float d) {
		this.pose(vx, vz, d, this.spare, this.spare2);
		this.lensMatrix
			.setPerspective((float)Math.toRadians(34.0), (float)this.lens.w / (float)this.lens.h, 0.3F, 900.0F)
			.lookAt(this.spare.x, this.spare.y, this.spare.z, this.spare2.x, this.spare2.y, this.spare2.z, 0.0F, 1.0F, 0.0F);
	}

	private void through(float x, float y, float z) {
		this.lensPoint.set(x, y, z, 1.0F);
		this.lensMatrix.transform(this.lensPoint);
		float w = Math.max(0.01F, this.lensPoint.w);
		this.box[0] = (float)this.lens.x + (this.lensPoint.x / w * 0.5F + 0.5F) * (float)this.lens.w;
		this.box[1] = (float)this.lens.y + (0.5F - this.lensPoint.y / w * 0.5F) * (float)this.lens.h;
	}

	@Override
	public void fit() {
		this.held = 2;
		this.fitTarget();
	}

	private void fitTarget() {
		Island island = this.main;
		Rect f = this.free();
		if (island != null && this.lens.h > 1 && this.getWidth() > 1) {
			IslandBuilder.Layout l = island.layout;
			float x0 = Math.max((float)l.landMinX - 0.5F, (float)l.questMinX - 4.5F);
			float x1 = Math.min((float)l.landMaxX + 0.5F, (float)l.questMaxX + 4.5F);
			float z0 = Math.max((float)l.landMinZ - 0.5F, (float)l.questMinZ - 5.5F);
			float z1 = Math.min((float)l.landMaxZ + 0.5F, (float)l.questMaxZ + 4.0F);
			float high = 3.6F;
			float roomW = Math.max(40.0F, (float)f.w() - 20.0F);
			float roomH = Math.max(40.0F, (float)f.h() - 26.0F);
			float near = this.distOf(1.3F);
			float far = this.farthest();
			float bestX = (x0 + x1) / 2.0F;
			float bestZ = (z0 + z1) / 2.0F;

			for (int round = 0; round < 18; round++) {
				float d = round == 0 ? near : (near + far) / 2.0F;
				float vx = (x0 + x1) / 2.0F;
				float vz = (z0 + z1) / 2.0F;
				float w = 0.0F;
				float h = 0.0F;

				for (int pass = 0; pass < 4; pass++) {
					this.lensFor(vx, vz, d);
					float minX = Float.MAX_VALUE;
					float minY = Float.MAX_VALUE;
					float maxX = -Float.MAX_VALUE;
					float maxY = -Float.MAX_VALUE;

					for (int corner = 0; corner < 8; corner++) {
						this.through((corner & 1) == 0 ? x0 : x1, (corner & 2) == 0 ? 0.0F : 3.6F, (corner & 4) == 0 ? z0 : z1);
						minX = Math.min(minX, this.box[0]);
						maxX = Math.max(maxX, this.box[0]);
						minY = Math.min(minY, this.box[1]);
						maxY = Math.max(maxY, this.box[1]);
					}

					w = maxX - minX;
					h = maxY - minY;
					this.reach((double)(minX + maxX) / 2.0, (double)(minY + maxY) / 2.0, d, 1.0F, this.ground);
					float gx = this.ground.x;
					float gz = this.ground.z;
					this.reach((double)f.x() + (double)f.w() / 2.0, (double)f.y() + (double)f.h() / 2.0, d, 1.0F, this.ground);
					vx += gx - this.ground.x;
					vz += gz - this.ground.z;
				}

				if (w <= roomW && h <= roomH) {
					far = d;
					bestX = vx;
					bestZ = vz;
					if (round == 0) {
						break;
					}
				} else {
					near = d;
				}
			}

			this.toX = bestX;
			this.toZ = bestZ;
			this.toDist = far;
		}
	}

	private void firstLook() {
		this.held = 1;
		this.fitTarget();
		Island island = this.main;
		if (island != null && !island.isEmpty() && !(this.zoomOf(this.toDist) >= 0.45F)) {
			float x = 0.0F;
			float z = 0.0F;
			int n = 0;

			for (Island.Spot s : island.spots) {
				if (s.node.visible && (s.node.status.open() || s.node.status == Rules.Status.CLAIMABLE)) {
					x += s.x;
					z += s.z;
					n++;
				}
			}

			Island.Spot first = island.spots.get(0);
			this.aim(n > 0 ? x / (float)n : first.x, 1.0F, n > 0 ? z / (float)n : first.z, this.distOf(0.85F));
			this.clampTarget();
		}
	}

	private void fitNow() {
		this.fit();
		this.zoomedAtMs = Clock.nowMs();
	}

	private void footMoved() {
		if (this.journey == null && this.coverR == 0) {
			this.footChanged = false;
			if (this.main != null && !this.main.isEmpty()) {
				if (this.held == 1) {
					this.firstLook();
				} else if (this.held == 2) {
					this.fitTarget();
				}
			}
		}
	}

	private void clampTarget() {
		Island island = this.main;
		if (island != null) {
			IslandBuilder.Layout l = island.layout;
			this.toX = Mth.clamp(this.toX, (float)l.landMinX - 10.0F, (float)l.landMaxX + 10.0F);
			this.toZ = Mth.clamp(this.toZ, (float)l.landMinZ - 8.0F, (float)l.landMaxZ + 14.0F);
			this.toDist = Mth.clamp(this.toDist, this.distOf(3.6F), this.farthest());
		}
	}

	@Override
	public boolean focus(@Nullable String key) {
		Island.Spot s = this.shownSpot(key);
		if (s == null) {
			return false;
		} else {
			this.held = 0;
			this.aim(s.x, 1.0F, s.z, this.zoomOf(this.toDist) < 0.8F ? this.distOf(1.0F) : this.toDist);
			this.select(key);
			return true;
		}
	}

	@Override
	public void reveal(@Nullable String key) {
		Island.Spot s = this.shownSpot(key);
		if (s != null) {
			Rect f = this.free();
			this.lensFor(this.toX, this.toZ, this.toDist);
			this.through(s.x, s.heart(), s.z);
			float r = 22.0F + 18.0F * this.zoomOf(this.toDist) * s.scale;
			float x = Mth.clamp(this.box[0], (float)f.x() + r, Math.max((float)f.x() + r, (float)f.right() - r));
			float y = Mth.clamp(this.box[1], (float)f.y() + r, Math.max((float)f.y() + r, (float)f.bottom() - r));
			if (x != this.box[0] || y != this.box[1]) {
				this.held = 0;
				this.reach((double)x, (double)y, this.toDist, s.heart(), this.ground);
				this.toX = s.x - this.ground.x;
				this.toZ = s.z - this.ground.z;
			}
		}
	}

	@Nullable
	private Island.Spot shownSpot(@Nullable String key) {
		Island island = this.main;
		return island != null && island.graph().shown(key) != null ? island.spot(key) : null;
	}

	private void pan(float dx, float dz) {
		this.held = 0;
		this.toX += dx;
		this.toZ += dz;
		this.clampTarget();
	}

	private void zoomAbout(float factor, double sx, double sy) {
		float before = this.toDist;
		float d = Mth.clamp(this.toDist / factor, this.distOf(3.6F), this.farthest());
		if (d != before) {
			this.held = 0;
			this.zoomedAtMs = Clock.nowMs();
			this.reach(sx, sy, before, 0.0F, this.ground);
			float gx = this.ground.x;
			float gz = this.ground.z;
			this.reach(sx, sy, d, 0.0F, this.ground);
			this.toX = this.toX + (gx - this.ground.x);
			this.toZ = this.toZ + (gz - this.ground.z);
			this.toDist = d;
			this.clampTarget();
		}
	}

	@Override
	public void select(@Nullable String key) {
		if (!Objects.equals(key, this.selected)) {
			Island island = this.main;
			if (island != null) {
				NodeView was = island.graph().node(this.selected);
				NodeView now = island.graph().node(key);
				if (was != null) {
					was.selected.set(false);
				}

				if (now != null) {
					now.selected.set(true);
				}
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

	@Override
	public boolean step(int direction) {
		Island island = this.main;
		if (island == null) {
			return false;
		} else {
			List<NodeView> todo = new ArrayList<>();

			for (NodeView n : island.graph().nodes) {
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
	}

	private void glide() {
		long now = Clock.nowMs();
		float dt = this.lastFrameMs == 0L ? 0.016F : Math.min(0.1F, (float)(now - this.lastFrameMs) / 1000.0F);
		this.lastFrameMs = now;
		float motion = Theme.current().motion();
		if (motion <= 0.0F) {
			this.camX = this.toX;
			this.camZ = this.toZ;
			this.dist = this.toDist;
		} else {
			float k = 1.0F - (float)Math.exp((double)(-dt * 13.0F / motion));
			this.camX = this.camX + (this.toX - this.camX) * k;
			this.camZ = this.camZ + (this.toZ - this.camZ) * k;
			this.dist = (float)Math.exp(Math.log((double)this.dist) + (Math.log((double)this.toDist) - Math.log((double)this.dist)) * (double)k);
			if (Math.abs(this.toDist - this.dist) < 0.01F) {
				this.dist = this.toDist;
			}

			if (Math.abs(this.toX - this.camX) < 0.004F) {
				this.camX = this.toX;
			}

			if (Math.abs(this.toZ - this.camZ) < 0.004F) {
				this.camZ = this.toZ;
			}
		}
	}

	private static void whoosh(float pitch) {
		if (Theme.current().uiSounds() && QuestsConfig.get().sounds) {
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_IN, pitch, 0.35F));
		}
	}

	void lookAt(@Nullable String key, float zoom) {
		Island island = this.main;
		if (island != null && !island.isEmpty()) {
			Island.Spot spot = island.spot(key);
			if (spot == null) {
				for (Island.Spot s : island.spots) {
					if (s.node.visible && (spot == null || s.node.status.open() && !spot.node.status.open())) {
						spot = s;
					}
				}
			}

			if (spot != null) {
				this.held = 0;
				this.aim(spot.x, 1.0F, spot.z, this.distOf(zoom));
				this.clampTarget();
				this.camX = this.toX;
				this.camZ = this.toZ;
				this.dist = this.toDist;
			}
		}
	}

	private static float smooth(float t) {
		float u = Mth.clamp(t, 0.0F, 1.0F);
		return u * u * (3.0F - 2.0F * u);
	}

	private static void bezier(Vector3f a, Vector3f b, Vector3f c, float t, Vector3f out) {
		float u = 1.0F - t;
		float x = u * u * a.x + 2.0F * u * t * b.x + t * t * c.x;
		float y = u * u * a.y + 2.0F * u * t * b.y + t * t * c.y;
		float z = u * u * a.z + 2.0F * u * t * b.z + t * t * c.z;
		out.set(x, y, z);
	}

	private void unfold(boolean quiet) {
		QuestBook book = QuestsState.book();
		NodeView h = this.hovered;
		Island island = this.main;
		Chapter tree = h != null && book != null && h.gateway() ? book.chapter(h.quest.subtree) : null;
		boolean wanted = !quiet && island != null && tree != null && tree != this.shown && !h.locked() && QuestsConfig.get().subtreePreview;
		if (this.preview != null && (!wanted || this.preview.owner != h)) {
			this.preview.close();
		}

		if (this.preview != null && this.preview.gone()) {
			if (this.preview.island != this.main) {
				this.preview.island.show(false, false);
			}

			this.preview = null;
		}

		if (wanted && this.preview == null && Clock.nowMs() - this.hoveredSinceMs >= (long)QuestsConfig.get().subtreePreviewDelayMs) {
			Island.Spot gate = island.spot(h.key);
			if (gate != null) {
				this.preview = new IslandView.Preview(h, gate, tree);
			}
		}
	}

	private void buildAhead() {
		Island island = this.main;
		QuestBook book = QuestsState.book();
		if (island != null
			&& book != null
			&& this.journey == null
			&& this.preview == null
			&& !this.arriving
			&& ++this.frame % 14 == 0
			&& Clock.nowMs() - this.shownAtMs >= 900L) {
			int gates = 0;

			for (Island.Spot s : island.spots) {
				if (s.gate && s.node.visible && !s.node.locked()) {
					if (++gates > 4) {
						return;
					}

					Chapter tree = book.chapter(s.node.quest.subtree);
					if (tree != null && tree != this.shown && !this.islands.containsKey(tree.id)) {
						this.islandFor(tree, false);
						return;
					}
				}
			}
		}
	}

	protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.draw(g, mouseX, mouseY, partialTick);
	}

	protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.draw(g, mouseX, mouseY, partialTick);
	}

	private void draw(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.lens();
		if (this.lens.w > 1 && this.lens.h > 1) {
			this.ensureStage();
			Stage s = this.stage;
			if (s != null && this.air != null) {
				if (this.main != null && this.footChanged) {
					this.footMoved();
				}

				long now = Clock.nowMs();
				float motion = Theme.current().motion();
				if (this.unseen) {
					this.unseen = false;
					if (this.inPlace && this.journey == null) {
						this.camX = this.toX;
						this.camZ = this.toZ;
						this.dist = this.toDist * (this.arriving ? 1.03F : 1.0F);
						this.lastFrameMs = now;
					}

					if (TIMED) {
						CFHGuiQuests.LOGGER
							.info(
								"[CFHGui Quests] island scene shown ({}): {} stages live, {} item meshes and {} block meshes held from before",
								new Object[]{this.inPlace ? "in an open book's place" : "opened", Stage.liveCount(), Things.held(), Props.held()}
							);
					}
				}

				if (this.journey != null) {
					if (this.journey.advance()) {
						this.journey = null;
						this.lastFrameMs = now;
					}
				} else {
					this.glide();
				}

				IslandView.Journey way = this.journey;
				Island here = this.main;
				boolean busy = way != null || here == null;
				boolean inside = !busy && this.isHovered() && g.containsPointInScissor(mouseX, mouseY);
				int control = inside && !this.pressed ? this.controlAt((double)mouseX, (double)mouseY) : -1;
				if (control != this.hoveredControl) {
					this.hoveredControl = control;
					this.controlSinceMs = now;
				}

				for (int i = 0; i < this.controlHover.length; i++) {
					this.controlHover[i].set(i == control);
				}

				boolean over = inside && control < 0 && (!this.pressed || !this.dragged) && !this.arriving;
				Island.Spot under = over ? this.spotAt((double)mouseX, (double)mouseY) : null;
				NodeView pointed = under == null ? null : under.node;
				if (pointed != this.hovered) {
					if (this.hovered != null) {
						this.hovered.hover.set(false);
					}

					this.hovered = pointed;
					this.hoveredSinceMs = now;
					if (this.hovered != null) {
						this.hovered.hover.set(true);
						this.lampSpot = under;
					}
				}

				this.unfold(busy);
				this.buildAhead();
				if (control >= 0 && now - this.controlSinceMs > 350L) {
					CFHGuiTooltips.request(
						Component.translatable(control == 0 ? "cfhgui_quests.overhaul.closer" : (control == 1 ? "cfhgui_quests.overhaul.back" : "cfhgui_quests.overhaul.fit")), this
					);
				}

				if (here != null && way == null) {
					if (this.arriving) {
						float t = motion <= 0.0F ? 1.0F : Mth.clamp((float)(now - this.arrivedAtMs) / ((float)this.arriveMs * motion), 0.0F, 1.0F);
						here.place(0.0F, -this.arriveRise * (1.0F - Ease.OUT_CUBIC.apply(t)), 0.0F, 1.0F, Math.min(1.0F, t * 3.0F));
						if (t >= 1.0F) {
							this.arriving = false;
						}
					} else {
						here.place(0.0F, 0.0F, 0.0F, 1.0F, 1.0F);
					}
				}

				if (this.leaving != null) {
					float t = motion <= 0.0F ? 1.0F : Mth.clamp((float)(now - this.leftAtMs) / (340.0F * motion), 0.0F, 1.0F);
					if (t >= 1.0F) {
						this.leaving.show(false, false);
						this.leaving = null;
					} else {
						this.leaving.place(0.0F, -9.0F * t * t, 0.0F, 1.0F, 1.0F - t * t);
					}
				}

				if (this.preview != null && here != null) {
					this.preview.stand(s.timeMs() / 1000.0F, here);
				} else if (this.puffs != null) {
					this.puffs.visible(false);
				}

				StageCamera camera = s.camera();
				if (way != null && way.flown) {
					camera.at(way.eye.x, way.eye.y, way.eye.z).lookAt(way.look.x, way.look.y, way.look.z).fov(way.fov);
				} else {
					this.pose(this.camX, this.camZ, this.dist, this.eye, this.look);
					camera.at(this.eye.x, this.eye.y, this.eye.z).lookAt(this.look.x, this.look.y, this.look.z).fov(way == null ? 34.0F : way.fov);
				}

				if (here == null) {
					if (this.emptyAir == null) {
						this.emptyAir = IslandTheme.of(null);
					}

					this.air.theme(this.emptyAir, true);
					this.air.frame(18.0F, this.dist);
				} else {
					this.air.frame(here.layout.radius(), this.dist);
				}

				Island.Spot lit = this.lampSpot;
				if (lit != null && this.hoverLamp >= 0 && this.hoverLamp < 8) {
					float strength = lit.node.hover.get();
					if (strength <= 0.01F) {
						s.lighting().lampStrength(this.hoverLamp, 0.0F);
						this.lampSpot = null;
					} else {
						int c = lit.node.locked() ? -5196608 : (here == null ? -1 : here.accent);
						s.lighting()
							.lamp(
								this.hoverLamp,
								lit.x,
								lit.top + 1.7F,
								lit.z + 1.2F,
								(float)Colors.red(c) / 255.0F * 0.85F,
								(float)Colors.green(c) / 255.0F * 0.85F,
								(float)Colors.blue(c) / 255.0F * 0.85F,
								5.5F
							);
						s.lighting().lampStrength(this.hoverLamp, strength);
					}
				}

				s.render(g, 0, 0, this.lens.w, this.lens.h, (double)mouseX, (double)mouseY, partialTick);
				this.frameG = g;
				this.frameMouseX = mouseX;
				this.frameMouseY = mouseY;
				CanvasDraw.batch(g, this.paintOver);
				this.frameG = null;
				if (TIMED && here != null && ++this.timedFrames >= 100) {
					this.timedFrames = 0;
					CFHGuiQuests.LOGGER
						.info(
							"[CFHGui Quests] island of {} ({} quests, {} islands kept): {} ms a frame for the scene at {}x{}, {} fps",
							new Object[]{
								here.chapter.id,
								here.spots.size(),
								this.islands.size(),
								String.format(Locale.ROOT, "%.2f", s.frameMs()),
								s.targetWidth(),
								s.targetHeight(),
								Minecraft.getInstance().getFps()
							}
						);
				}
			}
		}
	}

	@Nullable
	private Island.Spot spotAt(double mouseX, double mouseY) {
		Stage s = this.stage;
		Island island = this.main;
		if (s != null && island != null && s.frames() != 0L && this.contains(mouseX, mouseY)) {
			s.camera()
				.ray(
					(float)((mouseX - (double)this.lens.x) / (double)this.lens.w * 2.0 - 1.0),
					(float)(1.0 - (mouseY - (double)this.lens.y) / (double)this.lens.h * 2.0),
					this.spare,
					this.spare2
				);
			Island.Spot hit = island.pick(this.spare, this.spare2);
			return hit != null ? hit : island.near(mouseX, mouseY);
		} else {
			return null;
		}
	}

	private void paintOver() {
		GuiGraphics g = this.frameG;
		Theme t = Theme.current();
		Palette p = t.palette();
		boolean van = t.isVanilla();
		Island island = this.main;
		IslandView.Journey way = this.journey;
		int sw = this.lens.w;
		int sh = this.lens.h;
		if (way != null && way.veiled > 0.004F) {
			g.fill(0, 0, sw, sh, Colors.scaleAlpha(way.veil, way.veiled));
		}

		if (van) {
			g.fill(0, 0, sw, 32, -1275068416);
			shade(g, this.screen.rail(), -1409286144);
			shade(g, this.screen.tabs(), -1409286144);
			if (this.coverR > 0) {
				g.fill(this.getX() + this.getWidth() - this.coverR, this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), -805306368);
			}
		} else {
			CFHGuiDraw.rect(g, 0, 0, sw, 32, t.chrome(p.bg()));
		}

		if (island != null) {
			float zoom = this.zoomOf(this.dist);
			this.takenCount = 0;
			g.enableScissor(this.getX(), this.getY(), this.getX() + this.getWidth() - this.coverR, this.getY() + this.getHeight());

			try {
				if (way == null && !this.arriving) {
					this.plate(g, van, p);
					this.marks(g, island, zoom, van);
					this.names(g, island, zoom, van, p);
				}

				this.corner(g);
				if (island.isEmpty() && way == null) {
					Rect f = this.free();
					CFHGuiDraw.textCentered(
						g,
						Component.translatable(QuestsState.available() ? "cfhgui_quests.canvas.empty" : "cfhgui_quests.canvas.no_book"),
						f.centerX(),
						f.centerY() - 4,
						van ? -1 : p.text(),
						true
					);
				}
			} finally {
				g.disableScissor();
			}
		}
	}

	private static void shade(GuiGraphics g, AbstractWidget panel, int argb) {
		if (panel.visible && panel.getWidth() > 0) {
			g.fill(panel.getX(), panel.getY(), panel.getX() + panel.getWidth(), panel.getY() + panel.getHeight(), argb);
		}
	}

	private void marks(GuiGraphics g, Island island, float zoom, boolean van) {
		Chapter c = island.chapter;
		if (!c.decorations.isEmpty() && this.stage != null) {
			Font font = CFHGuiDraw.font();
			StageCamera camera = this.stage.camera();
			Rect f = this.free();

			for (int i = 0; i < c.decorations.size(); i++) {
				Chapter.Decoration d = c.decorations.get(i);
				if ("label".equals(d.type)
					&& !d.text.isBlank()
					&& !(zoom < (d.heading ? 0.3F : 0.55F))
					&& !d.editorOnly
					&& (d.requires.isEmpty() || QuestsState.progress().everDone(d.requires))
					&& camera.project(island.layout.cellX(d.x), Math.max(0.1F, island.markLift(i)), island.layout.cellZ(d.y), this.spare)) {
					float x = (float)this.lens.x + (this.spare.x * 0.5F + 0.5F) * (float)this.lens.w;
					float y = (float)this.lens.y + (0.5F - this.spare.y * 0.5F) * (float)this.lens.h;
					if (!(x < (float)(f.x() - 80)) && !(x > (float)(f.right() + 80)) && !(y < (float)(f.y() - 10)) && !(y > (float)(f.bottom() + 10))) {
						Component text = d.heading ? Fonts.heading(QuestText.text(d.text)) : QuestText.text(d.text);
						float size = Mth.clamp(zoom, d.heading ? 0.8F : 0.7F, 1.4F) * d.scale;
						int color = d.color.isEmpty() ? (!van && !d.heading ? -1645348 : -1) : Colors.fromHex(d.color, -1) | 0xFF000000;
						float half = (float)font.width(text) * size / 2.0F;
						if (this.claim(Math.round(x - half) - 2, Math.round(y - 5.0F * size), Math.round(x + half) + 2, Math.round(y + 6.0F * size))) {
							g.pose().pushPose();
							g.pose().translate((float)Math.round(x), (float)Math.round(y), 0.0F);
							g.pose().scale(size, size, 1.0F);
							g.drawString(font, text, -font.width(text) / 2, -4, Colors.scaleAlpha(color, Mth.clamp(d.alpha, 0.0F, 1.0F) * (d.heading ? 0.95F : 0.8F)), true);
							g.pose().popPose();
						}
					}
				}
			}
		}
	}

	private void names(GuiGraphics g, Island island, float zoom, boolean van, Palette p) {
		float near = Mth.clamp((zoom - 1.25F) / 0.2F, 0.0F, 1.0F);
		if (!(near <= 0.02F)) {
			Font font = CFHGuiDraw.font();
			Rect f = this.free();

			for (int turn = 0; turn < 4; turn++) {
				for (Island.Spot s : island.spots) {
					if (s.inView && !(s.rise < 0.9F) && s.node.visible) {
						int rank = s.node != this.hovered && !s.node.key.equals(this.selected) ? (s.node.locked() ? 3 : (s.node.done() ? 2 : 1)) : 0;
						if (rank == turn
							&& !(s.sx < (float)(f.x() - 40))
							&& !(s.sx > (float)(f.right() + 40))
							&& !(s.sy < (float)(f.y() + 4))
							&& !(s.sy > (float)(f.bottom() + 30))) {
							float shown = near * Math.max(0.3F, s.node.lit.get());
							int room = Mth.clamp(Math.round(s.node.room * 2.0F * s.sr * 2.0F / Math.max(0.3F, s.scale)) - 8, 30, 130);
							FormattedCharSequence name = s.node.secret() ? QuestText.secret().getVisualOrderText() : s.node.caption(room);
							int lock = s.node.locked() && !s.node.secret() ? 10 : 0;
							int w = font.width(name) + lock;
							if (w > lock) {
								int x = Math.round(s.sx - (float)w / 2.0F);
								int y = Math.round(s.sy - s.sr * 1.3F - 15.0F);
								if (!this.claim(x - 5, y - 3, x + w + 5, y + 12)) {
									y -= 13;
									if (!this.claim(x - 5, y - 3, x + w + 5, y + 12)) {
										continue;
									}
								}

								int color = s.node.locked() ? (van ? -4671304 : p.textMuted()) : (van ? -1 : p.text());

								int ink = switch (s.status) {
									case COMPLETE -> van ? -11141291 : p.success();
									case CLAIMABLE -> van ? -22016 : p.warning();
									case LOCKED, HIDDEN -> van ? -6250336 : p.textDim();
									default -> van ? -1 : island.accent;
								};
								if (van) {
									g.fill(x - 3, y - 2, x + w + 3, y + 10, Colors.withAlpha(-16777216, Math.round(152.0F * shown)));
								} else {
									CFHGuiDraw.pixelRound(g, x - 4, y - 2, w + 8, 12, Colors.withAlpha(p.bg(), Math.round(184.0F * shown)), 2);
								}

								g.fill(x - 2, y + 10, x + w + 2, y + 11, Colors.scaleAlpha(ink, shown * 0.9F));
								if (lock > 0) {
									Icons.draw(g, Icon.LOCK, x - 1, y, 8, Colors.scaleAlpha(color, Math.max(0.04F, shown)));
								}

								g.drawString(font, name, x + lock, y, Colors.scaleAlpha(color, Math.max(0.04F, shown)), van);
							}
						}
					}
				}
			}
		}
	}

	private boolean claim(int x0, int y0, int x1, int y1) {
		for (int i = 0; i < this.takenCount; i++) {
			int o = i * 4;
			if (x0 < this.taken[o + 2] && x1 > this.taken[o] && y0 < this.taken[o + 3] && y1 > this.taken[o + 1]) {
				return false;
			}
		}

		this.take(x0, y0, x1, y1);
		return true;
	}

	private void take(int x0, int y0, int x1, int y1) {
		if (this.takenCount < this.taken.length / 4) {
			int o = this.takenCount++ * 4;
			this.taken[o] = x0;
			this.taken[o + 1] = y0;
			this.taken[o + 2] = x1;
			this.taken[o + 3] = y1;
		}
	}

	private void plate(GuiGraphics g, boolean van, Palette p) {
		IslandView.Preview pv = this.preview;
		if (pv != null && this.stage != null) {
			float e = Mth.clamp((pv.open.get() - 0.45F) / 0.4F, 0.0F, 1.0F);
			Island sub = pv.island;
			IslandBuilder.Layout l = sub.layout;
			StageCamera camera = this.stage.camera();
			float minX = Float.MAX_VALUE;
			float minY = Float.MAX_VALUE;
			float maxX = -Float.MAX_VALUE;
			float maxY = -Float.MAX_VALUE;

			for (int corner = 0; corner < 8; corner++) {
				sub.world(
					(corner & 1) == 0 ? (float)l.landMinX - 0.5F : (float)l.landMaxX + 0.5F,
					(corner & 2) == 0 ? -3.0F : 3.5F,
					(corner & 4) == 0 ? (float)l.landMinZ - 0.5F : (float)l.landMaxZ + 0.5F,
					this.spare
				);
				if (camera.project(this.spare.x, this.spare.y, this.spare.z, this.spare2)) {
					float x = (float)this.lens.x + (this.spare2.x * 0.5F + 0.5F) * (float)this.lens.w;
					float y = (float)this.lens.y + (0.5F - this.spare2.y * 0.5F) * (float)this.lens.h;
					minX = Math.min(minX, x);
					maxX = Math.max(maxX, x);
					minY = Math.min(minY, y);
					maxY = Math.max(maxY, y);
				}
			}

			if (!(minX > maxX)) {
				pv.shownAt = new Rect(Math.round(minX), Math.round(minY), Math.max(1, Math.round(maxX - minX)), Math.max(1, Math.round(maxY - minY)));
				if (pv.open.get() > 0.3F) {
					this.take(pv.shownAt.x(), pv.shownAt.y(), pv.shownAt.right(), pv.shownAt.bottom());
				}

				if (!(e <= 0.02F)) {
					Font font = CFHGuiDraw.font();
					int accent = van ? -1 : sub.accent;
					String count = pv.done + " / " + pv.total;
					Component hint = Component.translatable("cfhgui_quests.preview.hint");
					FormattedCharSequence title = CFHGuiDraw.truncate(Fonts.heading(pv.title), 150);
					int w = Math.max(font.width(title) + 10 + font.width(count), font.width(hint)) + 14;
					int h = 27;
					Rect f = this.free();
					int x = Mth.clamp(Math.round((minX + maxX) / 2.0F - (float)w / 2.0F), f.x() + 4, Math.max(f.x() + 4, f.right() - 4 - w));
					int y = Mth.clamp(Math.round(minY) - 27 - 2 + Math.round((1.0F - e) * 5.0F), f.y() + 4, Math.max(f.y() + 4, f.bottom() - 4 - 27));
					this.take(x - 2, y - 2, x + w + 2, y + 27 + 2);
					if (van) {
						g.fill(x - 1, y - 1, x + w + 1, y + 27 + 1, Colors.scaleAlpha(-16777216, e));
						g.fill(x, y, x + w, y + 27, Colors.scaleAlpha(-267382768, e));
						CFHGuiDraw.outline(g, x, y, w, 27, Colors.scaleAlpha(-7631989, e), 0);
					} else {
						CFHGuiDraw.popupPlate(g, x, y, w, 27, p.surface(), Colors.mix(p.borderStrong(), accent, 0.5F), 0.5F, e);
						g.fill(x + 3, y, x + w - 3, y + 2, Colors.scaleAlpha(accent, e));
					}

					g.drawString(font, title, x + 7, y + 5, Colors.scaleAlpha(van ? -1 : p.text(), e), van);
					g.drawString(font, count, x + w - 7 - font.width(count), y + 5, Colors.scaleAlpha(van ? -6250336 : accent, e), van);
					g.drawString(font, hint, x + 7, y + 16, Colors.scaleAlpha(van ? -8355712 : p.textDim(), e), van);
				}
			}
		}
	}

	private int controlX() {
		return this.getX() + this.getWidth() - this.coverR - 6 - 14;
	}

	private int controlY(int i) {
		return this.getY() + this.getHeight() - this.coverB - 6 - (3 - i) * 14 - (2 - i) * 2;
	}

	private boolean controlsShown() {
		return this.getWidth() - this.coverL - this.coverR >= 120 && this.getHeight() - this.coverT - this.coverB >= 80 && this.main != null && !this.main.isEmpty();
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

	private void corner(GuiGraphics g) {
		if (this.controlsShown() && this.journey == null) {
			QuestPainter.View view = this.view(this.frameMouseX, this.frameMouseY);
			int x = this.controlX();
			this.painter.control(g, view, QuestPainter.Control.ZOOM_IN, x, this.controlY(0), 14, this.controlHover[0].get(), this.toDist > this.distOf(3.6F) + 0.01F);
			this.painter.control(g, view, QuestPainter.Control.ZOOM_OUT, x, this.controlY(1), 14, this.controlHover[1].get(), this.toDist < this.farthest() - 0.01F);
			this.painter.control(g, view, QuestPainter.Control.FIT, x, this.controlY(2), 14, this.controlHover[2].get(), true);
			long since = Clock.nowMs() - this.zoomedAtMs;
			if (this.zoomedAtMs != 0L && since < 1400L) {
				float a = since < 1000L ? 1.0F : 1.0F - (float)(since - 1000L) / 400.0F;
				this.painter.readout(g, view, Component.literal(Math.round(this.zoomOf(this.toDist) * 100.0F) + "%"), x - 4, this.controlY(2) + 1, a);
			}
		}
	}

	private QuestPainter.View view(int mouseX, int mouseY) {
		Chapter c = this.shown;
		float seconds = Theme.current().motion() > 0.0F ? (float)(Clock.nowMs() % 3600000L) / 1000.0F : 0.0F;
		return new QuestPainter.View(
			new Rect(this.getX(), this.getY(), this.getWidth(), this.getHeight()),
			this.zoomOf(this.dist),
			this.camX,
			this.camZ,
			seconds,
			mouseX,
			mouseY,
			c,
			QuestPainter.accentOf(c)
		);
	}

	@Override
	public void renderOver(GuiGraphics g, int mouseX, int mouseY) {
		if (this.hovered != null && this.journey == null && !this.arriving && (!this.pressed || !this.dragged)) {
			if (Clock.nowMs() - this.hoveredSinceMs >= 140L) {
				this.frameG = g;
				this.frameMouseX = mouseX;
				this.frameMouseY = mouseY;
				CanvasDraw.layer(g);
				CanvasDraw.batch(g, this.paintCard);
				this.frameG = null;
			}
		}
	}

	private void paintCard() {
		NodeView n = this.hovered;
		Island island = this.main;
		Island.Spot s = n != null && island != null ? island.spot(n.key) : null;
		if (s != null && s.inView) {
			float motion = Theme.current().motion();
			float alpha = motion <= 0.0F ? 1.0F : Mth.clamp((float)(Clock.nowMs() - this.hoveredSinceMs - 140L) / (110.0F * motion), 0.0F, 1.0F);
			int limit = this.coverR > 0 ? this.getX() + this.getWidth() - this.coverR : this.lens.w;
			Rect bounds = new Rect(0, 0, limit, this.lens.h);
			int side = 0;
			IslandView.Preview pv = this.preview;
			if (pv != null && pv.owner == n && !pv.closing) {
				side = -pv.side;
				Rect raised = pv.shownAt;
				if (raised.w() > 1) {
					bounds = side > 0
						? new Rect(raised.right() + 2, 0, Math.max(1, limit - raised.right() - 2), this.lens.h)
						: new Rect(0, 0, Math.max(1, Math.min(limit, raised.x() - 2)), this.lens.h);
				}
			} else if (n.gateway() && !n.locked() && QuestsConfig.get().subtreePreview) {
				Rect f = this.free();
				side = -SubtreePreview.preferredSide(s.sx, f.x(), f.right());
			}

			this.painter
				.card(this.frameG, this.view(this.frameMouseX, this.frameMouseY), n, s.sx, s.sy, Math.max(9.0F, s.sr * (s.gate ? 2.4F : 1.5F)), side, alpha, bounds);
		}
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.active && this.visible && this.contains(mouseX, mouseY)) {
			Island island = this.main;
			if (this.journey == null && island != null) {
				if (button == 0) {
					int control = this.controlAt(mouseX, mouseY);
					if (control >= 0) {
						this.press(control);
						return true;
					}
				}

				Island.Spot spot = this.arriving ? null : this.spotAt(mouseX, mouseY);
				this.reach(mouseX, mouseY, this.dist, 0.0F, this.ground);
				float gx = this.camX + this.ground.x;
				float gz = this.camZ + this.ground.z;
				if (button == 1) {
					IslandBuilder.Layout l = island.layout;
					return this.onContext != null
						&& this.onContext.open(spot == null ? null : spot.node.key, mouseX, mouseY, (gx + (float)l.ox) / 2.0F, (gz + (float)l.oz) / 2.0F);
				} else if (button != 0 && button != 2) {
					return false;
				} else {
					this.pressed = true;
					this.dragged = false;
					this.pressButton = button;
					this.pressX = mouseX;
					this.pressY = mouseY;
					this.grabX = gx;
					this.grabZ = gz;
					this.pressedKey = button == 0 && spot != null ? spot.node.key : null;
					return true;
				}
			} else {
				return true;
			}
		} else {
			return false;
		}
	}

	private void press(int control) {
		Rect f = this.free();
		switch (control) {
			case 0:
				this.zoomAbout(1.25F, (double)f.x() + (double)f.w() / 2.0, (double)f.y() + (double)f.h() / 2.0);
				break;
			case 1:
				this.zoomAbout(0.8F, (double)f.x() + (double)f.w() / 2.0, (double)f.y() + (double)f.h() / 2.0);
				break;
			default:
				this.fitNow();
		}

		CFHGuiSounds.tick();
	}

	@Override
	public boolean dragging() {
		return this.pressed;
	}

	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (!this.pressed) {
			return false;
		} else if (!this.dragged && Math.abs(mouseX - this.pressX) + Math.abs(mouseY - this.pressY) < 3.0) {
			return true;
		} else {
			this.dragged = true;
			this.held = 0;
			this.reach(mouseX, mouseY, this.dist, 0.0F, this.ground);
			this.toX = this.grabX - this.ground.x;
			this.toZ = this.grabZ - this.ground.z;
			this.toDist = this.dist;
			this.clampTarget();
			this.camX = this.toX;
			this.camZ = this.toZ;
			return true;
		}
	}

	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (!this.pressed) {
			return false;
		} else {
			this.pressed = false;
			String key = this.pressedKey;
			this.pressedKey = null;
			if (!this.dragged && this.pressButton == 0) {
				Island.Spot now = this.spotAt(mouseX, mouseY);
				if (key != null && now != null && key.equals(now.node.key)) {
					this.choose(now.node);
				} else if (key == null) {
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
		this.select(node.key);
		CFHGuiSounds.click();
		if (twice && node.gateway() && !node.locked() && this.onEnter != null) {
			this.lastClickKey = null;
			this.onEnter.accept(node.key);
		} else {
			if (this.onOpen != null) {
				this.onOpen.accept(node.key);
			}
		}
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (this.contains(mouseX, mouseY) && scrollY != 0.0 && this.journey == null && this.main != null) {
			float factor = (float)Math.pow(1.18, scrollY);
			Rect f = this.free();
			if (QuestsConfig.get().zoomToPointer) {
				this.zoomAbout(factor, mouseX, mouseY);
			} else {
				this.zoomAbout(factor, (double)f.x() + (double)f.w() / 2.0, (double)f.y() + (double)f.h() / 2.0);
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
		Island island = this.main;
		if (this.journey == null && island != null) {
			float step = 3.0F / Math.max(0.5F, this.zoomOf(this.dist));
			Rect f = this.free();
			double cx = (double)f.x() + (double)f.w() / 2.0;
			double cy = (double)f.y() + (double)f.h() / 2.0;
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
					NodeView n = island.graph().shown(this.selected);
					if (n == null) {
						return false;
					}

					CFHGuiSounds.click();
					if (this.onOpen != null) {
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
		} else {
			return false;
		}
	}

	public void onClick(double mouseX, double mouseY) {
	}

	protected void updateWidgetNarration(NarrationElementOutput out) {
		this.defaultButtonNarrationText(out);
	}

	private final class Journey {
		final TreeView.Travel kind;
		final Island from;
		final Island to;
		final String through;
		int stretch;
		long startMs = Clock.nowMs();
		final Vector3f eye0 = new Vector3f();
		final Vector3f look0 = new Vector3f();
		float fromX = IslandView.this.camX;
		float fromZ = IslandView.this.camZ;
		float fromDist = IslandView.this.dist;
		final Vector3f gate = new Vector3f();
		float size = 1.0F;
		final int veil;
		final Vector3f eye = new Vector3f();
		final Vector3f look = new Vector3f();
		float fov = 34.0F;
		float veiled;
		boolean flown;

		Journey(final TreeView.Travel kind, final Island from, final Island to, final String through) {
			this.kind = kind;
			this.from = from;
			this.to = to;
			this.through = through;
			Island.Spot s = (kind == TreeView.Travel.INTO ? from : to).spot(through);
			if (s != null) {
				this.gate.set(s.x, 0.0F, s.z);
				this.size = s.scale;
			}

			this.veil = Colors.mix(to.accent, to.theme.below, 0.3F) | 0xFF000000;
			IslandView.this.pose(IslandView.this.camX, IslandView.this.camZ, IslandView.this.dist, this.eye0, this.look0);
		}

		float t(int ms) {
			return Mth.clamp((float)(Clock.nowMs() - this.startMs) / ((float)ms * Math.max(0.05F, Theme.current().motion())), 0.0F, 1.0F);
		}

		boolean advance() {
			if (this.kind == TreeView.Travel.INTO) {
				if (this.stretch == 0) {
					float t = this.t(720);
					float e = Ease.IN_OUT_CUBIC.apply(t);
					this.flown = true;
					IslandView.bezier(
						this.eye0,
						IslandView.this.spare.set(this.gate.x, 1.5F * this.size + 1.2F, this.gate.z + 5.2F * this.size + 2.4F),
						IslandView.this.spare2.set(this.gate.x, 1.05F * this.size, this.gate.z + 0.12F),
						e,
						this.eye
					);
					this.look.set(this.look0).lerp(IslandView.this.spare.set(this.gate.x, 1.02F * this.size, this.gate.z - 6.0F), IslandView.smooth(Math.min(1.0F, t * 1.5F)));
					this.fov = Mth.lerp(e * e, 34.0F, 64.0F);
					this.from.pass(this.through, IslandView.smooth((t - 0.1F) / 0.4F), this.veil);
					this.veiled = IslandView.smooth((t - 0.5F) / 0.34F);
					if (t >= 1.0F) {
						this.arrive();
					}

					return false;
				} else {
					float t = this.t(1000);
					float e = Ease.IN_OUT_CUBIC.apply(t);
					this.flown = false;
					IslandView.this.camX = Mth.lerp(e, this.fromX, IslandView.this.toX);
					IslandView.this.camZ = Mth.lerp(e, this.fromZ, IslandView.this.toZ);
					IslandView.this.dist = (float)Math.exp((double)Mth.lerp(e, (float)Math.log((double)this.fromDist), (float)Math.log((double)IslandView.this.toDist)));
					this.fov = Mth.lerp(IslandView.smooth(Math.min(1.0F, t * 1.6F)), 64.0F, 34.0F);
					this.veiled = 1.0F - IslandView.smooth(t / 0.3F);
					return t >= 1.0F;
				}
			} else if (this.stretch == 0) {
				float t = this.t(340);
				float e = t * t;
				this.flown = false;
				IslandView.this.dist = this.fromDist * (1.0F + 0.9F * e);
				this.fov = Mth.lerp(e, 34.0F, 52.0F);
				this.veiled = IslandView.smooth((t - 0.2F) / 0.7F);
				if (t >= 1.0F) {
					this.arrive();
				}

				return false;
			} else {
				float t = this.t(880);
				float e = Ease.IN_OUT_CUBIC.apply(t);
				this.flown = true;
				IslandView.this.pose(IslandView.this.toX, IslandView.this.toZ, IslandView.this.toDist, this.eye, this.look);
				IslandView.bezier(
					IslandView.this.spare.set(this.gate.x, 1.2F * this.size, this.gate.z + 1.7F * this.size + 0.4F),
					IslandView.this.spare2.set(this.gate.x, 2.6F * this.size + 1.0F, this.gate.z + 6.5F * this.size + 2.0F),
					this.eye,
					e,
					this.eye
				);
				this.look.lerp(IslandView.this.spare.set(this.gate.x, 1.05F * this.size, this.gate.z - 3.0F), 1.0F - IslandView.smooth(e));
				this.fov = Mth.lerp(e, 56.0F, 34.0F);
				this.to.pass(this.through, 1.0F - IslandView.smooth((t - 0.15F) / 0.45F), this.veil);
				this.veiled = 1.0F - IslandView.smooth(t / 0.4F);
				if (t < 1.0F) {
					return false;
				} else {
					this.to.pass(null, 0.0F, 0);
					IslandView.this.camX = IslandView.this.toX;
					IslandView.this.camZ = IslandView.this.toZ;
					IslandView.this.dist = IslandView.this.toDist;
					NodeView n = this.to.graph().shown(this.through);
					if (n != null) {
						n.openedAtMs = Clock.nowMs();
					}

					return true;
				}
			}
		}

		private void arrive() {
			this.from.pass(null, 0.0F, 0);
			this.from.show(false, false);
			IslandView.this.main = this.to;
			IslandView.this.selected = null;
			this.to.show(true, true);
			this.to.place(0.0F, 0.0F, 0.0F, 1.0F, 1.0F);
			IslandView.this.air.theme(this.to.theme, true);
			IslandView.this.lamps(this.to);
			IslandView.this.arriving = false;
			IslandView.this.shownAtMs = Clock.nowMs();
			this.stretch = 1;
			this.startMs = IslandView.this.shownAtMs;
			IslandView.this.restoreView();
			if (this.kind == TreeView.Travel.INTO) {
				Island.Spot start = null;

				for (Island.Spot s : this.to.spots) {
					if (s.node.visible && (start == null || s.node.depth < start.node.depth)) {
						start = s;
					}
				}

				float keepX = IslandView.this.toX;
				float keepZ = IslandView.this.toZ;
				float keepDist = IslandView.this.toDist;
				float close = Math.min(IslandView.this.toDist, IslandView.this.distOf(2.4F));
				if (start != null) {
					IslandView.this.aim(start.x, 1.0F, start.z, close);
				}

				this.fromX = IslandView.this.toX;
				this.fromZ = IslandView.this.toZ;
				this.fromDist = close;
				IslandView.this.toX = keepX;
				IslandView.this.toZ = keepZ;
				IslandView.this.toDist = keepDist;
				IslandView.this.camX = this.fromX;
				IslandView.this.camZ = this.fromZ;
				IslandView.this.dist = this.fromDist;
				this.to.entrance(0);
			} else {
				this.to.settle();
				IslandView.this.camX = IslandView.this.toX;
				IslandView.this.camZ = IslandView.this.toZ;
				IslandView.this.dist = IslandView.this.toDist;
			}
		}
	}

	private final class Preview {
		final NodeView owner;
		final Island.Spot gate;
		final Island island;
		final int side;
		final float headroom;
		final Component title;
		final int done;
		final int total;
		final Anim open = new Anim(0.0F, 460, Ease.OUT_CUBIC);
		boolean closing;
		Rect shownAt = new Rect(0, 0, 0, 0);

		Preview(final NodeView owner, final Island.Spot gate, final Chapter tree) {
			this.owner = owner;
			this.gate = gate;
			this.island = IslandView.this.islandFor(tree, false);
			Rect f = IslandView.this.free();
			this.side = SubtreePreview.preferredSide(gate.sx, f.x(), f.right());
			this.headroom = Mth.clamp((gate.sy - (float)f.y()) / (0.45F * (float)f.h()), 0.25F, 1.0F);
			this.title = QuestText.title(tree);
			BookStats.Stat stat = BookStats.of(tree);
			this.done = stat.done();
			this.total = stat.total();
			this.island.show(true, false);
			this.island.unfold(240, 80);
			this.open.set(1.0F);
		}

		void close() {
			if (!this.closing) {
				this.closing = true;
				this.open.set(0.0F, 200);
			}
		}

		boolean gone() {
			return this.closing && this.open.get() <= 0.01F;
		}

		void stand(float seconds, Island on) {
			float e = Mth.clamp(this.open.get(), 0.0F, 1.0F);
			float pop = this.closing ? e : Ease.OUT_BACK.apply(e);
			IslandBuilder.Layout l = this.island.layout;
			float landW = (float)(l.landMaxX - l.landMinX + 1);
			float landD = (float)(l.landMaxZ - l.landMinZ + 1);
			float scale = Mth.clamp(0.3F * IslandView.this.dist / Math.max(landW, landD * 1.3F), 0.04F, 0.7F);
			float midX = (float)(l.landMaxX + l.landMinX) * 0.5F * scale;
			float midZ = (float)(l.landMaxZ + l.landMinZ) * 0.5F * scale;
			float cx = this.gate.x + (float)this.side * (1.7F * this.gate.scale + 0.04F * IslandView.this.dist + landW * 0.5F * scale);
			float cy = (2.2F + 0.05F * IslandView.this.dist) * this.headroom + 8.0F * scale + 0.1F * Mth.sin(seconds * 1.3F);
			float cz = this.gate.z + 0.02F * IslandView.this.dist;
			float x = Mth.lerp(e, this.gate.x, cx);
			float y = Mth.lerp(e, 1.1F * this.gate.scale, cy);
			float z = Mth.lerp(e, this.gate.z, cz);
			this.island.place(x - midX * pop, y, z - midZ * pop, Math.max(0.002F, scale * pop), Math.min(1.0F, e * 1.8F));
			if (IslandView.this.puffs != null) {
				IslandView.this.puffs.color(Colors.mix(on.theme.clouds, -1, 0.25F) | 0xFF000000).opacity(0.6F);
				IslandView.this.puffs.visible(true);
				IslandView.this.puffs.at(x, y - 3.2F * scale * pop, z).scale(Math.max(0.01F, l.radius() * scale * 0.88F * pop)).alpha(Math.min(1.0F, e * 1.5F));
			}
		}
	}
}
