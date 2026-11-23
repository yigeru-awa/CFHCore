package com.onehumanawa.cfhcore.content.gui.quests.client.overhaul;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.stage.Stage;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageBlend;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageSoft;
import com.onehumanawa.cfhcore.content.gui.core.stage.mesh.BlockMesh;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.BlocksNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PivotNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.StageNode;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.quests.QuestsConfig;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.GraphModel;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.NodeView;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestIcons;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestPainter;
import com.onehumanawa.cfhcore.content.gui.quests.logic.Rules;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

final class Island {
	static final int TASK = 0;
	static final int GOAL = 1;
	static final int CHALLENGE = 2;
	static final int GATE = 3;
	private static final float FAR_PX = 5.2F;
	private static final float HOLD_PX = 5.8F;
	private static final float MARK_PX = 6.8F;
	private static final float LOCK_PX = 9.0F;
	private static final byte NONE = 0;
	private static final byte PLAIN = 1;
	private static final byte GAP = 2;
	private static final byte MARKER = 3;
	private static final byte LOOSE = 4;
	private static final byte LAID = 5;
	private static final float DROP = 2.6F;
	private static final int DROP_MS = 260;
	private static final int LAY_STEP_MS = 85;
	private static final int LAY_WAIT_MS = 320;
	private static final int GOLD = -14003;
	Chapter chapter;
	final IslandTheme theme;
	final IslandBuilder.Layout layout;
	int accent;
	private final int stoneLit;
	private final Stage stage;
	private final Props props;
	private final Things things;
	private final Island.Lens lens;
	private final PivotNode root = new PivotNode();
	private final BlocksNode land;
	private final BlocksNode roads;
	private final StageNode solid = new Island.Solid();
	private final StageNode lights = new Island.Lights();
	private GraphModel graph;
	final List<Island.Spot> spots = new ArrayList<>();
	private final Map<String, Island.Spot> byKey = new HashMap<>();
	private final List<Island.Path> paths = new ArrayList<>();
	private final byte[] road;
	private final byte[] want;
	private final boolean[] coming;
	private final List<Island.Falling> falling = new ArrayList<>();
	private final List<Island.Puff> puffs = new ArrayList<>();
	private float[] markLift = new float[0];
	@Nullable
	private Island.Spot passing;
	private float passingAmount;
	private int passingColor;
	private boolean shown;
	private boolean live;
	private boolean gone;
	private final Vector3f rayFrom = new Vector3f();
	private final Vector3f rayAlong = new Vector3f();
	private final Matrix4f toScreen = new Matrix4f();
	private final Vector4f point = new Vector4f();
	private final String[] meshKeys = new String[4];
	private final BlockMesh[] meshes = new BlockMesh[4];
	private String laidKey = "";

	Island(Stage stage, Props props, Things things, Island.Lens lens, Chapter chapter, GraphModel graph) {
		this.stage = stage;
		this.props = props;
		this.things = things;
		this.lens = lens;
		this.chapter = chapter;
		this.theme = IslandTheme.of(chapter);
		this.accent = QuestPainter.accentOf(chapter) | 0xFF000000;
		int lit = 255;

		for (int c = 0; c < 3; c++) {
			float light = this.theme.ambient[c] + 0.62F * this.theme.key[c];
			lit = lit << 8 | Math.round(Mth.clamp((float)(this.theme.stone >> 16 - 8 * c & 0xFF) * light, 0.0F, 255.0F));
		}

		this.stoneLit = lit;
		this.layout = IslandBuilder.plan(chapter, graph, this.theme);
		int cells = this.layout.w * this.layout.d;
		this.road = new byte[cells];
		this.want = new byte[cells];
		this.coming = new boolean[cells];

		for (int design = 0; design < this.meshKeys.length; design++) {
			this.meshKeys[design] = this.theme.family + ":" + design;
		}

		this.laidKey = "block:" + this.theme.laid;
		stage.add(this.root);
		this.land = new BlocksNode(stage.level(), this.layout.w, 23, this.layout.d);
		this.roads = new BlocksNode(stage.level(), this.layout.w, 1, this.layout.d);
		if (this.theme.grassTint != 0 || this.theme.foliageTint != 0 || this.theme.waterTint != 0) {
			stage.level().tintRegion(this.land.origin(), this.theme.grassTint, this.theme.foliageTint, this.theme.waterTint);
			stage.level().tintRegion(this.roads.origin(), this.theme.grassTint, this.theme.foliageTint, this.theme.waterTint);
		}

		try {
			IslandBuilder.raise(this.layout, this.theme, this.land);
		} catch (RuntimeException var10) {
			CFHGuiQuests.LOGGER.error("[CFHGui Quests] the island of the chapter {} could not be built", chapter.id, var10);
		}

		((BlocksNode)stage.add(this.land.done().softLit(true))).at((float)this.layout.minX - 0.5F, -12.0F, (float)this.layout.minZ - 0.5F).attachTo(this.root);
		((BlocksNode)stage.add(this.roads.done().softLit(true))).at((float)this.layout.minX - 0.5F, -1.0F, (float)this.layout.minZ - 0.5F).attachTo(this.root);
		stage.add(this.solid).attachTo(this.root);
		stage.add(this.lights).attachTo(this.root);
		this.bind(graph);
	}

	GraphModel graph() {
		return this.graph;
	}

	boolean isEmpty() {
		return this.spots.isEmpty();
	}

	@Nullable
	Island.Spot spot(@Nullable String key) {
		return key == null ? null : this.byKey.get(key);
	}

	void bind(GraphModel model) {
		if (!this.gone) {
			this.graph = model;
			this.chapter = model.chapter;
			this.accent = QuestPainter.accentOf(this.chapter) | 0xFF000000;
			this.passing = null;
			this.spots.clear();
			this.byKey.clear();
			this.paths.clear();
			this.falling.clear();
			Arrays.fill(this.coming, false);
			float crowd = 1.0F;

			for (NodeView n : model.all) {
				IslandBuilder.Site site = this.layout.byKey.get(n.key);
				if (site != null) {
					Island.Spot s = new Island.Spot(n, site, 1.0F);
					this.spots.add(s);
					this.byKey.put(n.key, s);
				}
			}

			for (NodeView to : model.all) {
				if (!to.quest.hideLines) {
					for (String dep : to.entry.deps()) {
						NodeView from = model.node(dep);
						IslandBuilder.Route route = from != null && !from.quest.hideOutLines ? this.layout.route(from.index, to.index) : null;
						if (route != null) {
							this.paths.add(new Island.Path(route, from, to));
						}
					}
				}
			}

			this.markLift = new float[this.chapter.decorations.size()];

			for (int i = 0; i < this.markLift.length; i++) {
				Chapter.Decoration d = this.chapter.decorations.get(i);
				float x = this.layout.cellX(d.x);
				float z = this.layout.cellZ(d.y);

				for (Island.Spot s : this.spots) {
					if (Math.abs(s.x - x) <= 2.8F && s.z - z > -0.5F && s.z - z <= 4.4F) {
						this.markLift[i] = Math.max(this.markLift[i], s.top + 2.1F * s.scale);
					}
				}
			}

			this.sync(false);
		}
	}

	float markLift(int i) {
		return i >= 0 && i < this.markLift.length ? this.markLift[i] : 0.0F;
	}

	void pass(@Nullable String key, float amount, int color) {
		this.passing = key == null ? null : this.byKey.get(key);
		this.passingAmount = Mth.clamp(amount, 0.0F, 1.0F);
		this.passingColor = color;
	}

	void sync(boolean glide) {
		if (!this.gone) {
			boolean moving = glide && Theme.current().motion() > 0.0F;

			for (Island.Spot s : this.spots) {
				s.status = s.node.status;
				float dull = s.node.locked() ? 1.0F : 0.0F;

				float light = switch (s.status) {
					case AVAILABLE, IN_PROGRESS, CLAIMABLE, COMPLETE -> 1.0F;
					default -> 0.0F;
				};
				float flag = s.status == Rules.Status.COMPLETE && !s.gate ? 1.0F : 0.0F;
				if (moving) {
					s.dull.set(dull);
					s.light.set(light);
					s.flag.set(flag);
				} else {
					s.dull.snap(dull);
					s.light.snap(light);
					s.flag.snap(flag);
				}
			}

			Arrays.fill(this.want, (byte)0);

			for (Island.Path p : this.paths) {
				byte state = stateOf(p);
				int[] cells = p.route.cells;

				for (int k = 0; k < cells.length; k++) {
					int i = this.index(cells[k]);
					byte v = state == 2 && k % 2 == 0 ? 3 : state;
					if (v > this.want[i]) {
						this.want[i] = v;
					}
				}
			}

			long now = Clock.nowMs();

			for (Island.Path p : this.paths) {
				byte state = stateOf(p);
				boolean lay = moving && this.live && state == 5 && p.state != 0 && p.state != 5;
				p.state = state;
				if (lay) {
					int[] cells = p.route.cells;
					int step = cells.length == 0 ? 85 : Math.min(85, 900 / cells.length);
					int n = 0;

					for (int kx = 0; kx < cells.length; kx++) {
						int i = this.index(cells[kx]);
						if (this.road[i] != 5 && !this.coming[i]) {
							this.coming[i] = true;
							this.falling.add(new Island.Falling(i, now + 320L + (long)(n++) * (long)step, 0.8F + 0.5F * (float)(kx + 1) / (float)cells.length));
						}
					}
				}
			}

			boolean changed = false;

			for (Island.Path px : this.paths) {
				for (int c : px.route.cells) {
					int i = this.index(c);
					if (this.want[i] != this.road[i] && !this.coming[i]) {
						this.put(i, this.want[i]);
						changed = true;
					}
				}
			}

			if (changed) {
				this.roads.invalidateMesh();
			}
		}
	}

	private static byte stateOf(Island.Path p) {
		if (!p.from.visible || !p.to.visible) {
			return 1;
		} else if (p.from.done()) {
			return 5;
		} else {
			return (byte)(p.to.quest.looseDeps ? 4 : 2);
		}
	}

	private int index(int packed) {
		return ((short)(packed & 65535) - this.layout.minZ) * this.layout.w + ((packed >> 16) - this.layout.minX);
	}

	private void put(int cell, byte state) {
		this.road[cell] = state;
		BlockState ground = this.layout.routeGround[cell] == null ? this.theme.top : this.layout.routeGround[cell];

		BlockState block = switch (state) {
			case 3 -> this.theme.marker;
			case 4 -> this.theme.loose;
			case 5 -> this.theme.laid;
			default -> ground;
		};
		this.roads.put(cell % this.layout.w, 0, cell / this.layout.w, block);
	}

	void show(boolean on, boolean live) {
		if (!this.gone) {
			this.shown = on;
			this.live = on && live;
			this.root.visible(on);
			if (!on) {
				this.settle();
			}
		}
	}

	boolean shown() {
		return this.shown;
	}

	void place(float x, float y, float z, float scale, float alpha) {
		this.root.at(x, y, z);
		this.root.scale(Math.max(1.0E-4F, scale));
		this.root.alpha(alpha);
	}

	float scale() {
		return this.root.scale().x;
	}

	Vector3f world(float x, float y, float z, Vector3f out) {
		return this.root.model.transformPosition(out.set(x, y, z));
	}

	void entrance(int afterMs) {
		int deepest = Math.max(1, this.graph.deepest);
		int step = Math.min(55, 700 / deepest);
		float span = Math.max(1.0F, this.graph.height());

		for (NodeView n : this.graph.nodes) {
			n.appearIn(afterMs + n.depth * step + Math.round(40.0F * (n.y - this.graph.minY) / span));
		}
	}

	void unfold(int afterMs, int stepMs) {
		for (NodeView n : this.graph.nodes) {
			n.appearIn(afterMs + Math.min(n.depth, 9) * stepMs);
		}
	}

	void settle() {
		this.graph.settle();
		if (!this.gone && !this.falling.isEmpty()) {
			for (Island.Falling f : this.falling) {
				this.land(f.cell());
			}

			this.falling.clear();
			this.roads.invalidateMesh();
		}
	}

	private void land(int cell) {
		this.coming[cell] = false;
		this.put(cell, this.want[cell] == 0 ? 5 : this.want[cell]);
	}

	void dispose() {
		if (!this.gone) {
			this.gone = true;
			this.shown = this.live = false;
			this.falling.clear();
			this.stage.remove(this.lights);
			this.stage.remove(this.solid);
			this.stage.remove(this.roads);
			this.stage.remove(this.land);
			this.stage.remove(this.root);
		}
	}

	@Nullable
	Island.Spot pick(Vector3f origin, Vector3f direction) {
		this.rayFrom.set(origin);
		this.root.modelInverse.transformPosition(this.rayFrom);
		this.rayAlong.set(direction);
		this.root.modelInverse.transformDirection(this.rayAlong);
		Island.Spot best = null;
		float nearest = Float.MAX_VALUE;

		for (Island.Spot s : this.spots) {
			if (s.node.visible && !(s.rise < 0.5F)) {
				float half = (s.gate ? 1.55F : 0.6F) * s.scale;
				float deep = 0.6F * s.scale;
				float t = hit(this.rayFrom, this.rayAlong, s.x - half, 0.0F, s.z - deep, s.x + half, s.heart() + 0.6F * s.scale, s.z + deep);
				if (t < nearest) {
					nearest = t;
					best = s;
				}
			}
		}

		return best;
	}

	@Nullable
	Island.Spot near(double mouseX, double mouseY) {
		Island.Spot best = null;
		float nearest = Float.MAX_VALUE;

		for (Island.Spot s : this.spots) {
			if (s.node.visible && s.inView && !(s.rise < 0.5F)) {
				float dx = (float)mouseX - s.sx;
				float dy = (float)mouseY - s.sy;
				float reach = Math.max(4.0F, s.sr) + 2.0F;
				float far = dx * dx + dy * dy;
				if (far <= reach * reach && far < nearest) {
					nearest = far;
					best = s;
				}
			}
		}

		return best;
	}

	private static float hit(Vector3f o, Vector3f d, float x0, float y0, float z0, float x1, float y1, float z1) {
		float near = -Float.MAX_VALUE;
		float far = Float.MAX_VALUE;

		for (int axis = 0; axis < 3; axis++) {
			float from = axis == 0 ? o.x : (axis == 1 ? o.y : o.z);
			float along = axis == 0 ? d.x : (axis == 1 ? d.y : d.z);
			float lo = axis == 0 ? x0 : (axis == 1 ? y0 : z0);
			float hi = axis == 0 ? x1 : (axis == 1 ? y1 : z1);
			if (Math.abs(along) < 1.0E-7F) {
				if (from < lo || from > hi) {
					return Float.MAX_VALUE;
				}
			} else {
				float a = (lo - from) / along;
				float b = (hi - from) / along;
				if (a > b) {
					float swap = a;
					a = b;
					b = swap;
				}

				near = Math.max(near, a);
				far = Math.min(far, b);
				if (near > far) {
					return Float.MAX_VALUE;
				}
			}
		}

		return far < 0.0F ? Float.MAX_VALUE : Math.max(0.0F, near);
	}

	private static String iconSpec(NodeView n) {
		Quest q = n.quest;
		if (!q.icon.isBlank()) {
			return q.icon.trim();
		} else {
			Iterator var2 = q.tasks.iterator();
			if (var2.hasNext()) {
				Task t = (Task)var2.next();
				if (!t.icon.isBlank()) {
					return t.icon.trim();
				} else if (!t.item.isBlank() || !t.block.isBlank()) {
					return "item";
				} else if (!t.entity.isBlank()) {
					return n.icon().stack().isEmpty() ? "icon:sword" : "item";
				} else if (!t.fluid.isBlank()) {
					return n.icon().stack().isEmpty() ? "icon:dot" : "item";
				} else {
					String var4 = t.type;

					return switch (var4) {
						case "energy" -> "icon:bolt";
						case "stage" -> "icon:tag";
						case "counter" -> "icon:hash";
						case "kill" -> "icon:sword";
						case "advancement" -> "icon:trophy";
						case "stat" -> "icon:signal";
						case "xp" -> "item";
						case "location" -> "icon:compass";
						case "observe" -> "icon:eye";
						case "checkmark" -> "icon:check";
						case "quest", "chapter" -> "icon:quest";
						case "team" -> "icon:team";
						default -> "icon:dot";
					};
				}
			} else {
				return q.isGateway() ? "icon:compass" : "icon:quest";
			}
		}
	}

	private static record Falling(int cell, long atMs, float pitch) {
	}

	static final class Lens {
		int x;
		int y;
		int w = 1;
		int h = 1;
		float tan = 0.3F;
		float px = 1.0F;
	}

	private final class Lights extends StageNode {
		private static final int SEGMENTS = 14;
		private final Vector3f right = new Vector3f();
		private final Vector3f up = new Vector3f();
		private Matrix4f m;
		private BufferBuilder bb;
		private float fade;
		private boolean soft;
		private static final int INK = -14473682;
		private static final int EDGE = -1185054;
		private float u0;
		private float v0;
		private float u1;
		private float v1;
		private static final float[][] AROUND = new float[][]{
			{-1.0F, 0.0F}, {1.0F, 0.0F}, {0.0F, -1.0F}, {0.0F, 1.0F}, {-0.7F, -0.7F}, {0.7F, -0.7F}, {-0.7F, 0.7F}, {0.7F, 0.7F}
		};

		Lights() {
			this.pickable(false);
			this.hoverFeel(0.0F, 1.0F);
			this.named("lights");
		}

		protected void draw(StageRenderContext ctx) {
		}

		public boolean hasTranslucentPass() {
			return true;
		}

		protected void drawTranslucent(StageRenderContext ctx) {
			this.fade = this.alpha();
			if (!(this.fade <= 0.01F)) {
				this.soft = ctx.soft;
				this.m = ctx.pose.last().pose();
				Matrix4f view = ctx.camera.view();
				this.right.set(view.m00(), view.m10(), view.m20());
				this.up.set(view.m01(), view.m11(), view.m21());
				this.modelInverse.transformDirection(this.right).normalize();
				this.modelInverse.transformDirection(this.up).normalize();
				Theme style = Theme.current();
				boolean moving = style.motion() > 0.0F;
				float time = moving ? ctx.seconds() : 0.0F;
				long now = Clock.nowMs();
				int gold = style.isVanilla() ? -22016 : -14003;
				int green = style.isVanilla() ? -11141291 : style.palette().success();
				boolean party = QuestsConfig.get().celebrations && moving && Island.this.live;
				RenderSystem.setShader(GameRenderer::getPositionColorShader);
				RenderSystem.enableBlend();
				RenderSystem.enableDepthTest();
				RenderSystem.depthMask(false);
				RenderSystem.disableCull();
				StageBlend.over();
				this.bb = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

				for (Island.Path p : Island.this.paths) {
					float hover = Math.max(p.from.hover.get(), p.to.hover.get());
					if (!(hover <= 0.02F) && p.state != 0 && p.state != 1) {
						int color = p.state == 5 && p.to.done() ? green : Island.this.accent;

						for (int c : p.route.cells) {
							this.tile((float)(c >> 16), (float)((short)(c & 65535)), 0.03F, 0.46F, color, 0.42F * hover);
						}
					}
				}

				for (Island.Spot s : Island.this.spots) {
					if (s.drawn && !(s.rise <= 0.3F)) {
						float lit = s.node.lit.get() * Mth.clamp((s.rise - 0.3F) / 0.5F, 0.0F, 1.0F);
						float light = s.light.get() * lit;
						float hover = s.node.hover.get();
						float chosen = s.node.selected.get();
						float r = (s.gate ? 2.1F : 1.25F) * s.scale;
						float far = Mth.clamp((6.8F - s.pr) / 1.6000004F, 0.0F, 1.0F);
						if (far > 0.0F) {
							int color = switch (s.status) {
								case CLAIMABLE -> gold;
								case COMPLETE -> green;
								case LOCKED, HIDDEN -> Island.this.stoneLit;
								default -> Island.this.accent;
							};
							this.mark(s, color, far * Math.max(0.35F, lit));
						}

						if (s.far()) {
							if (hover > 0.01F) {
								this.arc(s.x, 0.06F, s.z, r * 1.5F, r * 1.9F, 0.0F, 1.0F, -1, 0.7F * hover);
							}

							if (chosen > 0.01F) {
								this.corners(s.x, 0.07F, s.z, r * 2.0F, -1, chosen);
							}
						} else {
							this.pool(s.x, 0.02F, s.z + 0.12F * s.scale, r * 0.92F, -16777216, 0.3F * s.ghost() * Mth.clamp((s.rise - 0.3F) / 0.5F, 0.0F, 1.0F));
							if (light > 0.01F) {
								switch (s.status) {
									case CLAIMABLE:
										this.pool(s.x, 0.04F, s.z, r * 1.25F, gold, (0.5F + 0.2F * Mth.sin(time * 3.2F + s.phase)) * light);
										break;
									case COMPLETE:
										this.pool(s.x, 0.04F, s.z, r, green, 0.2F * light);
										break;
									default:
										this.pool(s.x, 0.04F, s.z, r * 1.1F, Island.this.accent, (0.42F + 0.1F * Mth.sin(time * 1.7F + s.phase)) * light);
								}

								float progress = s.node.shownProgress.get();
								if (s.status == Rules.Status.IN_PROGRESS || progress > 0.01F && s.status == Rules.Status.AVAILABLE) {
									this.arc(s.x, 0.05F, s.z, r * 0.78F, r * 0.9F, 0.0F, 1.0F, Island.this.accent, 0.2F * light);
									this.arc(s.x, 0.055F, s.z, r * 0.76F, r * 0.92F, 0.0F, Mth.clamp(progress, 0.0F, 1.0F), -1, 0.85F * light);
								}
							}

							if (hover > 0.01F) {
								int color = s.node.locked() ? -4670268 : (s.status == Rules.Status.CLAIMABLE ? gold : (s.node.done() ? green : Island.this.accent));
								this.arc(s.x, 0.06F, s.z, r * 0.98F, r * 1.06F, 0.0F, 1.0F, color, 0.6F * hover * Math.max(0.3F, lit));
							}

							if (chosen > 0.01F) {
								this.corners(s.x, 0.07F, s.z, r * (1.16F + 0.05F * Mth.sin(time * 3.0F)), -1, chosen);
							}

							if (s.node.openedAtMs != 0L) {
								float t = (float)(now - s.node.openedAtMs) / 1100.0F;
								if (t >= 1.0F || !party) {
									s.node.openedAtMs = 0L;
								} else if (t > 0.0F) {
									for (int k = 0; k < 2; k++) {
										float tk = (t - (float)k * 0.2F) / (1.0F - (float)k * 0.2F);
										if (!(tk <= 0.0F) && !(tk >= 1.0F)) {
											float out = r * (0.9F + 2.8F * (1.0F - (1.0F - tk) * (1.0F - tk)));
											this.arc(s.x, 0.065F, s.z, out, out + 0.16F * s.scale, 0.0F, 1.0F, Island.this.accent, 0.85F * (1.0F - tk) * (1.0F - tk));
										}
									}
								}
							}

							if (s.flag.get() > 0.02F) {
								this.pennant(s, green, time, Mth.clamp(s.flag.get(), 0.0F, 1.15F) * lit);
							}

							if (s.gate) {
								this.film(s, time, false);
							}
						}
					}
				}

				for (int kx = Island.this.puffs.size() - 1; kx >= 0; kx--) {
					Island.Puff px = Island.this.puffs.get(kx);
					float t = (float)(now - px.atMs()) / 380.0F;
					if (t >= 1.0F) {
						Island.this.puffs.remove(kx);
					} else {
						this.arc(px.x(), 0.05F, px.z(), 0.3F + 0.6F * t, 0.42F + 0.75F * t, 0.0F, 1.0F, -1, 0.4F * (1.0F - t));
					}
				}

				this.flush();
				StageBlend.add();
				this.bb = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
				boolean flowing = QuestsConfig.get().flowingLines && moving;

				for (Island.Path px : Island.this.paths) {
					if (flowing && px.state == 5 && px.to.status.open() && px.route.cells.length != 0) {
						int[] cells = px.route.cells;
						float at = (time * 2.4F + (float)px.from.index * 0.37F) % ((float)cells.length + 2.0F) - 1.0F;
						int i = Mth.clamp(Mth.floor(at), 0, cells.length - 1);
						int j = Math.min(cells.length - 1, i + 1);
						float f = Mth.clamp(at - (float)i, 0.0F, 1.0F);
						float x = Mth.lerp(f, (float)(cells[i] >> 16), (float)(cells[j] >> 16));
						float z = Mth.lerp(f, (float)((short)(cells[i] & 65535)), (float)((short)(cells[j] & 65535)));
						float edge = Math.min(1.0F, Math.min(at + 1.0F, (float)cells.length - at));
						this.spark(x, 0.3F, z, 0.34F, Island.this.accent, 0.8F * Math.max(0.0F, edge) * Math.min(px.from.lit.get(), px.to.lit.get()));
					}
				}

				for (Island.Spot sx : Island.this.spots) {
					if (sx.drawn && !(sx.rise <= 0.3F)) {
						if (sx.far()) {
							if (sx.node.doneAtMs != 0L && now - sx.node.doneAtMs > 2300L) {
								sx.node.doneAtMs = 0L;
							}

							if (sx.node.openedAtMs != 0L && now - sx.node.openedAtMs > 1100L) {
								sx.node.openedAtMs = 0L;
							}
						} else {
							float litx = sx.node.lit.get() * Mth.clamp((sx.rise - 0.3F) / 0.5F, 0.0F, 1.0F);
							float lightx = sx.light.get() * litx;
							if (lightx > 0.01F && !sx.gate) {
								if (sx.status == Rules.Status.CLAIMABLE) {
									this.beam(
										sx.x, sx.top + sx.lift, sx.z, 0.36F * sx.scale, 0.2F * sx.scale, 3.4F * sx.scale, gold, (0.3F + 0.12F * Mth.sin(time * 3.2F + sx.phase)) * lightx
									);

									for (int kxx = 0; kxx < 5; kxx++) {
										float life = (time * 0.42F + (float)kxx * 0.2F + sx.phase) % 1.0F;
										float a = (float)kxx * 2.4F + sx.phase;
										this.spark(
											sx.x + Mth.cos(a) * 0.42F * sx.scale,
											sx.top + sx.lift + 0.2F + life * 2.6F * sx.scale,
											sx.z + Mth.sin(a) * 0.42F * sx.scale,
											0.16F * sx.scale,
											gold,
											Mth.sin(life * (float) Math.PI) * lightx
										);
									}
								} else if (sx.status.open()) {
									this.beam(sx.x, sx.top + sx.lift, sx.z, 0.32F * sx.scale, 0.16F * sx.scale, 2.9F * sx.scale, Island.this.accent, 0.16F * lightx);
								} else if (sx.status == Rules.Status.COMPLETE) {
									this.spark(sx.x, sx.heart() + sx.lift, sx.z, 1.5F * sx.scale, -2856, 0.1F * lightx);
								}
							}

							if (sx.gate) {
								this.film(sx, time, true);
							}

							if (sx.node.doneAtMs != 0L) {
								float span = sx.node.quest.frame == Quest.Frame.CHALLENGE ? 2300.0F : (sx.node.quest.frame == Quest.Frame.GOAL ? 1700.0F : 1200.0F);
								float t = (float)(now - sx.node.doneAtMs) / span;
								if (t >= 1.0F || !party) {
									sx.node.doneAtMs = 0L;
								} else if (t > 0.0F) {
									this.burst(sx, t, gold, green);
								}
							}
						}
					}
				}

				this.flush();
				StageBlend.over();
				this.glyphs(time);
				RenderSystem.enableCull();
				RenderSystem.depthMask(true);
			}
		}

		private void flush() {
			MeshData mesh = this.bb.build();
			if (mesh != null) {
				BufferUploader.drawWithShader(mesh);
			}
		}

		private void mark(Island.Spot s, int rgb, float alpha) {
			float pixel = 1.25F * s.scale / Math.max(0.05F, s.pr);
			float w = Math.max(0.4F * s.scale, pixel);
			float h = Math.max(1.7F * s.scale, 3.2F * pixel);
			this.vertex(s.x - this.right.x * w, 0.0F, s.z - this.right.z * w, rgb, alpha);
			this.vertex(s.x + this.right.x * w, 0.0F, s.z + this.right.z * w, rgb, alpha);
			this.vertex(s.x + this.right.x * w, h, s.z + this.right.z * w, rgb, alpha * 0.85F);
			this.vertex(s.x - this.right.x * w, h, s.z - this.right.z * w, rgb, alpha * 0.85F);
		}

		private void vertex(float x, float y, float z, int rgb, float alpha) {
			this.bb.addVertex(this.m, x, y, z).setColor(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, Math.round(255.0F * Mth.clamp(alpha * this.fade, 0.0F, 1.0F)));
		}

		private void pool(float x, float y, float z, float r, int rgb, float alpha) {
			if (!(alpha <= 0.01F)) {
				float mid = r * 0.45F;

				for (int i = 0; i < 14; i++) {
					float a0 = (float)i * (float) (Math.PI * 2) / 14.0F;
					float a1 = (float)(i + 1) * (float) (Math.PI * 2) / 14.0F;
					float c0 = Mth.cos(a0);
					float s0 = Mth.sin(a0);
					float c1 = Mth.cos(a1);
					float s1 = Mth.sin(a1);
					this.vertex(x, y, z, rgb, alpha);
					this.vertex(x + c0 * mid, y, z + s0 * mid, rgb, alpha * 0.62F);
					this.vertex(x + c1 * mid, y, z + s1 * mid, rgb, alpha * 0.62F);
					this.vertex(x, y, z, rgb, alpha);
					this.vertex(x + c0 * mid, y, z + s0 * mid, rgb, alpha * 0.62F);
					this.vertex(x + c0 * r, y, z + s0 * r, rgb, 0.0F);
					this.vertex(x + c1 * r, y, z + s1 * r, rgb, 0.0F);
					this.vertex(x + c1 * mid, y, z + s1 * mid, rgb, alpha * 0.62F);
				}
			}
		}

		private void arc(float x, float y, float z, float r0, float r1, float from, float to, int rgb, float alpha) {
			if (!(alpha <= 0.01F) && !(to <= from)) {
				int n = Math.max(1, Math.round(24.0F * (to - from)));

				for (int i = 0; i < n; i++) {
					float t0 = Mth.lerp((float)i / (float)n, from, to);
					float t1 = Mth.lerp((float)(i + 1) / (float)n, from, to);
					float a0 = t0 * (float) (Math.PI * 2);
					float a1 = t1 * (float) (Math.PI * 2);
					float c0 = Mth.sin(a0);
					float s0 = -Mth.cos(a0);
					float c1 = Mth.sin(a1);
					float s1 = -Mth.cos(a1);
					this.vertex(x + c0 * r0, y, z + s0 * r0, rgb, alpha);
					this.vertex(x + c0 * r1, y, z + s0 * r1, rgb, alpha);
					this.vertex(x + c1 * r1, y, z + s1 * r1, rgb, alpha);
					this.vertex(x + c1 * r0, y, z + s1 * r0, rgb, alpha);
				}
			}
		}

		private void corners(float x, float y, float z, float r, int rgb, float alpha) {
			float arm = r * 0.42F;
			float wide = 0.09F;

			for (int sx = -1; sx <= 1; sx += 2) {
				for (int sz = -1; sz <= 1; sz += 2) {
					float cx = x + (float)sx * r;
					float cz = z + (float)sz * r;
					this.quad(cx, cz, cx - (float)sx * arm, cz - (float)sz * 0.09F, y, rgb, alpha);
					this.quad(cx, cz, cx - (float)sx * 0.09F, cz - (float)sz * arm, y, rgb, alpha);
				}
			}
		}

		private void quad(float x0, float z0, float x1, float z1, float y, int rgb, float alpha) {
			this.vertex(x0, y, z0, rgb, alpha);
			this.vertex(x0, y, z1, rgb, alpha);
			this.vertex(x1, y, z1, rgb, alpha);
			this.vertex(x1, y, z0, rgb, alpha);
		}

		private void tile(float x, float z, float y, float half, int rgb, float alpha) {
			this.quad(x - half, z - half, x + half, z + half, y, rgb, alpha);
		}

		private void beam(float x, float y, float z, float r0, float r1, float height, int rgb, float alpha) {
			if (!(alpha <= 0.01F)) {
				int sides = 8;

				for (int i = 0; i < 8; i++) {
					float a0 = (float)i * (float) (Math.PI * 2) / 8.0F;
					float a1 = (float)(i + 1) * (float) (Math.PI * 2) / 8.0F;
					this.vertex(x + Mth.cos(a0) * r0, y, z + Mth.sin(a0) * r0, rgb, alpha);
					this.vertex(x + Mth.cos(a1) * r0, y, z + Mth.sin(a1) * r0, rgb, alpha);
					this.vertex(x + Mth.cos(a1) * r1, y + height, z + Mth.sin(a1) * r1, rgb, 0.0F);
					this.vertex(x + Mth.cos(a0) * r1, y + height, z + Mth.sin(a0) * r1, rgb, 0.0F);
				}
			}
		}

		private void spark(float x, float y, float z, float size, int rgb, float alpha) {
			if (!(alpha <= 0.01F)) {
				float h = size * 0.5F;
				this.vertex(x - this.right.x * h, y - this.right.y * h, z - this.right.z * h, rgb, alpha);
				this.vertex(x - this.up.x * h, y - this.up.y * h, z - this.up.z * h, rgb, alpha);
				this.vertex(x + this.right.x * h, y + this.right.y * h, z + this.right.z * h, rgb, alpha);
				this.vertex(x + this.up.x * h, y + this.up.y * h, z + this.up.z * h, rgb, alpha);
			}
		}

		private void pennant(Island.Spot s, int rgb, float time, float up) {
			float x = s.x + 0.36F * s.scale;
			float z = s.z - 0.36F * s.scale;
			float foot = s.top + s.lift;
			float height = 1.25F * s.scale * Math.max(0.0F, up);
			float w = 0.035F * s.scale;
			if (!(height <= 0.02F)) {
				int pole = -12960702;
				this.vertex(x - w, foot, z, -12960702, 1.0F);
				this.vertex(x + w, foot, z, -12960702, 1.0F);
				this.vertex(x + w, foot + height, z, -12960702, 1.0F);
				this.vertex(x - w, foot + height, z, -12960702, 1.0F);
				this.vertex(x, foot, z - w, -12960702, 1.0F);
				this.vertex(x, foot, z + w, -12960702, 1.0F);
				this.vertex(x, foot + height, z + w, -12960702, 1.0F);
				this.vertex(x, foot + height, z - w, -12960702, 1.0F);
				float len = 0.62F * s.scale;
				float tall = 0.34F * s.scale;
				float y1 = foot + height;
				int shade = Colors.mix(rgb, -16777216, 0.22F);
				float px = x;
				float pz = z;
				float ptop = y1;
				float pbot = y1 - tall;

				for (int k = 1; k <= 3; k++) {
					float f = (float)k / 3.0F;
					float nx = x + len * f;
					float nz = z + Mth.sin(time * 3.1F + s.phase - f * 2.2F) * 0.1F * f * s.scale;
					float ntop = y1 - tall * 0.14F * f;
					float nbot = y1 - tall + tall * 0.36F * f;
					int c = k == 2 ? shade : rgb;
					this.vertex(px, pbot, pz, c, 1.0F);
					this.vertex(nx, nbot, nz, c, 1.0F);
					this.vertex(nx, ntop, nz, c, 1.0F);
					this.vertex(px, ptop, pz, c, 1.0F);
					px = nx;
					pz = nz;
					ptop = ntop;
					pbot = nbot;
				}
			}
		}

		private void film(Island.Spot s, float time, boolean glow) {
			float solid = s == Island.this.passing ? Island.this.passingAmount : 0.0F;
			float open = Math.max(solid, (1.0F - s.dull.get()) * s.node.lit.get() * Mth.clamp((s.rise - 0.6F) / 0.4F, 0.0F, 1.0F));
			if (!(open <= 0.01F)) {
				float hover = s.node.hover.get();
				float half = 0.5F * s.scale;
				float y0 = s.lift;
				float y1 = s.lift + 2.0F * s.scale;
				float mid = (y0 + y1) / 2.0F;
				int rgb = solid > 0.0F ? Colors.lerp(s.beyond | 0xFF000000, Island.this.passingColor | 0xFF000000, solid) : s.beyond;
				if (!glow) {
					float edge = Mth.lerp(solid, (0.62F + 0.14F * hover) * open, 1.0F);
					float heart = Mth.lerp(solid, (0.9F + 0.1F * hover) * open, 1.0F);
					float[][] rim = new float[][]{{s.x - half, y0}, {s.x + half, y0}, {s.x + half, y1}, {s.x - half, y1}};

					for (int k = 0; k < 4; k++) {
						float[] a = rim[k];
						float[] b = rim[(k + 1) % 4];
						this.vertex(a[0], a[1], s.z, rgb, edge);
						this.vertex(b[0], b[1], s.z, rgb, edge);
						this.vertex(s.x, mid, s.z, rgb, heart);
						this.vertex(s.x, mid, s.z, rgb, heart);
					}
				} else {
					float breath = 0.5F + 0.5F * Mth.sin(time * 1.6F + s.phase);
					int heart = Colors.mix(rgb, -1, 0.35F);
					float[][] rim = new float[][]{{s.x - half, y0}, {s.x + half, y0}, {s.x + half, y1}, {s.x - half, y1}};

					for (int k = 0; k < 4; k++) {
						float[] a = rim[k];
						float[] b = rim[(k + 1) % 4];
						this.vertex(a[0], a[1], s.z, rgb, 0.06F * open);
						this.vertex(b[0], b[1], s.z, rgb, 0.06F * open);
						this.vertex(s.x, mid, s.z, heart, (0.16F + 0.1F * breath + 0.2F * hover) * open);
						this.vertex(s.x, mid, s.z, heart, (0.16F + 0.1F * breath + 0.2F * hover) * open);
					}

					for (int k = 0; k < 9; k++) {
						float life = (time * 0.3F + (float)k / 9.0F + s.phase) % 1.0F;
						float a = (float)k * 2.1F + life * 5.5F;
						float r = (1.0F - life) * 0.92F;
						this.spark(s.x + Mth.cos(a) * r * half, mid + Mth.sin(a) * r * s.scale, s.z + 0.04F, 0.13F * s.scale, -1, Mth.sin(life * (float) Math.PI) * 0.8F * open);
					}

					this.spark(s.x, mid, s.z + 0.3F, 3.4F * s.scale, rgb, (0.08F + 0.05F * breath + 0.1F * hover) * open);
				}
			}
		}

		private void burst(Island.Spot s, float t, int gold, int green) {
			Quest.Frame frame = s.node.quest.frame;
			int sparks = frame == Quest.Frame.CHALLENGE ? 60 : (frame == Quest.Frame.GOAL ? 36 : 20);
			float reach = (frame == Quest.Frame.CHALLENGE ? 4.6F : (frame == Quest.Frame.GOAL ? 3.4F : 2.4F)) * s.scale;
			int[] colors = new int[]{green, gold, -1, Island.this.accent};
			float e = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
			if (t < 0.3F) {
				this.spark(s.x, s.heart() + s.lift, s.z, (2.0F + 2.6F * t) * s.scale, -1, 0.5F * (1.0F - t / 0.3F));
			}

			int salt = s.node.index * 31 + 5;

			for (int i = 0; i < sparks; i++) {
				float life = 0.55F + 0.45F * IslandBuilder.hash(i, 3, salt);
				float tk = t / life;
				if (!(tk >= 1.0F)) {
					float a = IslandBuilder.hash(i, 1, salt) * (float) (Math.PI * 2);
					float far = reach * (0.3F + 0.7F * IslandBuilder.hash(i, 2, salt));
					float high = reach * (0.7F + 0.9F * IslandBuilder.hash(i, 4, salt));
					float x = s.x + Mth.cos(a) * far * e;
					float z = s.z + Mth.sin(a) * far * e;
					float y = s.top + s.lift + 0.3F + high * e - reach * 1.5F * t * t;
					this.spark(x, y, z, (0.2F + 0.14F * IslandBuilder.hash(i, 5, salt)) * s.scale * (1.0F - 0.5F * tk), colors[i % colors.length], 1.0F - tk * tk);
				}
			}

			if (frame != Quest.Frame.TASK) {
				this.beam(s.x, s.top + s.lift, s.z, 0.5F * s.scale, 0.9F * s.scale, 5.0F * s.scale, gold, 0.5F * (1.0F - t) * (1.0F - t));
			}
		}

		private void glyphs(float time) {
			boolean any = false;
			boolean pictures = false;

			for (Island.Spot s : Island.this.spots) {
				if (s.inView && !(s.rise <= 0.3F) && !(s.pr < 5.8F)) {
					boolean secret = s.node.secret();
					boolean locked = s.node.locked();
					float shown = Math.max(0.35F, s.node.lit.get());
					float lock = locked && !secret ? Mth.clamp((s.pr - 9.0F) / 4.0F, 0.0F, 1.0F) * Mth.clamp((s.rise - 0.6F) / 0.4F, 0.0F, 1.0F) : 0.0F;
					boolean picture = !secret && s.picture != null;
					Icon icon = secret ? Icon.QUESTION : (picture ? null : (s.glyph != null ? s.glyph : (s.blank ? Icon.QUESTION : null)));
					if (picture) {
						pictures = true;
					}

					if (icon != null || !(lock <= 0.01F)) {
						if (!any) {
							any = true;
							RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
							RenderSystem.setShaderTexture(0, Icons.ATLAS);
							this.bb = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
						}

						if (lock > 0.01F) {
							this.glyph(Icon.LOCK);
							boolean wide = s.design != 0;
							float h = (s.gate ? 0.4F : (wide ? 0.24F : 0.2F)) * s.scale;
							float y = s.lift + (s.gate ? 1.0F : 0.52F) * s.scale;
							float z = s.z + (s.gate ? 0.0F : (wide ? 0.5F : 0.25F)) * s.scale + 0.03F;

							for (int k = 0; k < 8; k++) {
								this.upright(s.x + AROUND[k][0] * h * 0.13F, y + AROUND[k][1] * h * 0.13F, z, h, -1185054, 0.8F * lock * shown);
							}

							this.upright(s.x, y, z + 0.004F, h, -14473682, lock * shown);
						}

						if (icon != null) {
							this.glyph(icon);
							float h = this.size(s, secret ? 0.5F : 0.42F);
							float y = this.height(s, time);
							if (secret) {
								this.edged(s.x, y, s.z, h, -14473682, -1185054, shown, 0.95F * shown);
							} else if (locked) {
								this.edged(s.x, y, s.z, h, Island.this.stoneLit, -14473682, shown, 0.5F * shown);
							} else {
								this.facing(s.x, y, s.z, h, s.status == Rules.Status.CLAIMABLE ? -14003 : -1, shown);
							}
						}
					}
				}
			}

			if (any) {
				this.flush();
			}

			if (pictures) {
				this.u0 = this.v0 = 0.0F;
				this.u1 = this.v1 = 1.0F;

				for (Island.Spot sx : Island.this.spots) {
					if (sx.inView && !(sx.rise <= 0.3F) && !(sx.pr < 5.8F) && sx.picture != null && !sx.node.secret()) {
						float shownx = Math.max(0.35F, sx.node.lit.get());
						float dull = sx.dull.get();
						if (this.soft && dull > 0.01F) {
							this.bb = StageSoft.begin(StageSoft.ENTITY);
							float x = sx.x;
							float y = this.height(sx, time);
							float z = sx.z;
							float h = this.size(sx, 0.42F);
							this.corner(x - this.right.x * h - this.up.x * h, y - this.right.y * h - this.up.y * h, z - this.right.z * h - this.up.z * h, 0.0F, 1.0F);
							this.corner(x + this.right.x * h - this.up.x * h, y + this.right.y * h - this.up.y * h, z + this.right.z * h - this.up.z * h, 1.0F, 1.0F);
							this.corner(x + this.right.x * h + this.up.x * h, y + this.right.y * h + this.up.y * h, z + this.right.z * h + this.up.z * h, 1.0F, 0.0F);
							this.corner(x - this.right.x * h + this.up.x * h, y - this.right.y * h + this.up.y * h, z - this.right.z * h + this.up.z * h, 0.0F, 0.0F);
							StageSoft.cast(Island.this.theme.stone, dull);

							try {
								StageSoft.end(this.bb, StageSoft.ENTITY, sx.picture, Math.min(0.999F, this.fade * shownx), false);
							} finally {
								StageSoft.noCast();
							}

							RenderSystem.depthMask(false);
							RenderSystem.disableCull();
						} else {
							RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
							RenderSystem.setShaderTexture(0, sx.picture);
							this.bb = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
							this.facing(sx.x, this.height(sx, time), sx.z, this.size(sx, 0.42F), -1, sx.node.locked() ? 0.6F * shownx : shownx);
							this.flush();
						}
					}
				}
			}
		}

		private void corner(float x, float y, float z, float u, float v) {
			this.bb.addVertex(this.m, x, y, z).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0).setNormal(0.0F, 0.0F, 1.0F);
		}

		private void glyph(Icon icon) {
			float tw = 256.0F;
			float th = (float)(Icon.rows() * 16);
			this.u0 = (float)icon.u() / 256.0F;
			this.v0 = (float)icon.v() / th;
			this.u1 = (float)(icon.u() + 16) / 256.0F;
			this.v1 = (float)(icon.v() + 16) / th;
		}

		private float size(Island.Spot s, float half) {
			float in = Mth.clamp((s.rise - 0.3F) / 0.7F, 0.0F, 1.0F);
			return half * s.scale * (1.0F + 0.14F * s.node.hover.get()) * Ease.OUT_BACK.apply(in);
		}

		private float height(Island.Spot s, float time) {
			boolean still = s.node.locked() || s.node.done();
			return s.heart() + s.lift + (still ? 0.0F : Mth.sin(time * 2.1F + s.phase) * 0.07F * s.scale);
		}

		private void edged(float x, float y, float z, float h, int rgb, int edge, float alpha, float edgeAlpha) {
			float e = h * 0.13F;

			for (int k = 0; k < 8; k++) {
				float dx = AROUND[k][0] * e;
				float dy = AROUND[k][1] * e;
				this.facing(x + this.right.x * dx + this.up.x * dy, y + this.right.y * dx + this.up.y * dy, z + this.right.z * dx + this.up.z * dy, h, edge, edgeAlpha);
			}

			this.facing(x, y, z, h, rgb, alpha);
		}

		private void facing(float x, float y, float z, float h, int rgb, float alpha) {
			int r = rgb >> 16 & 0xFF;
			int g = rgb >> 8 & 0xFF;
			int b = rgb & 0xFF;
			int a = Math.round(255.0F * Mth.clamp(alpha * this.fade, 0.0F, 1.0F));
			if (a > 2) {
				this.bb
					.addVertex(this.m, x - this.right.x * h - this.up.x * h, y - this.right.y * h - this.up.y * h, z - this.right.z * h - this.up.z * h)
					.setUv(this.u0, this.v1)
					.setColor(r, g, b, a);
				this.bb
					.addVertex(this.m, x + this.right.x * h - this.up.x * h, y + this.right.y * h - this.up.y * h, z + this.right.z * h - this.up.z * h)
					.setUv(this.u1, this.v1)
					.setColor(r, g, b, a);
				this.bb
					.addVertex(this.m, x + this.right.x * h + this.up.x * h, y + this.right.y * h + this.up.y * h, z + this.right.z * h + this.up.z * h)
					.setUv(this.u1, this.v0)
					.setColor(r, g, b, a);
				this.bb
					.addVertex(this.m, x - this.right.x * h + this.up.x * h, y - this.right.y * h + this.up.y * h, z - this.right.z * h + this.up.z * h)
					.setUv(this.u0, this.v0)
					.setColor(r, g, b, a);
			}
		}

		private void upright(float x, float y, float z, float h, int rgb, float alpha) {
			int r = rgb >> 16 & 0xFF;
			int g = rgb >> 8 & 0xFF;
			int b = rgb & 0xFF;
			int a = Math.round(255.0F * Mth.clamp(alpha * this.fade, 0.0F, 1.0F));
			if (a > 2) {
				this.bb.addVertex(this.m, x - h, y - h, z).setUv(this.u0, this.v1).setColor(r, g, b, a);
				this.bb.addVertex(this.m, x + h, y - h, z).setUv(this.u1, this.v1).setColor(r, g, b, a);
				this.bb.addVertex(this.m, x + h, y + h, z).setUv(this.u1, this.v0).setColor(r, g, b, a);
				this.bb.addVertex(this.m, x - h, y + h, z).setUv(this.u0, this.v0).setColor(r, g, b, a);
			}
		}
	}

	private static final class Path {
		final IslandBuilder.Route route;
		final NodeView from;
		final NodeView to;
		byte state = 0;

		Path(IslandBuilder.Route route, NodeView from, NodeView to) {
			this.route = route;
			this.from = from;
			this.to = to;
		}
	}

	private static record Puff(float x, float z, long atMs) {
	}

	private final class Solid extends StageNode {
		Solid() {
			this.pickable(false);
			this.hoverFeel(0.0F, 1.0F);
			this.named("pedestals");
		}

		public void update(StageRenderContext ctx) {
			if (!Island.this.falling.isEmpty()) {
				long now = Clock.nowMs();
				boolean landed = false;

				for (int k = Island.this.falling.size() - 1; k >= 0; k--) {
					Island.Falling f = Island.this.falling.get(k);
					if (now >= f.atMs() + 260L) {
						Island.this.falling.remove(k);
						Island.this.land(f.cell());
						landed = true;
						int x = f.cell() % Island.this.layout.w + Island.this.layout.minX;
						int z = f.cell() / Island.this.layout.w + Island.this.layout.minZ;
						if (Island.this.puffs.size() < 24) {
							Island.this.puffs.add(new Island.Puff((float)x, (float)z, now));
						}

						if (QuestsConfig.get().sounds && Theme.current().uiSounds()) {
							Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(Island.this.theme.laid.getSoundType().getPlaceSound(), f.pitch(), 0.32F));
						}
					}
				}

				if (landed) {
					Island.this.roads.invalidateMesh();
				}
			}
		}

		protected void draw(StageRenderContext ctx) {
			try {
				this.stand(ctx);
			} finally {
				StageSoft.noCast();
			}
		}

		private void stand(StageRenderContext ctx) {
			float alpha = this.alpha();
			boolean moving = Theme.current().motion() > 0.0F;
			float time = moving ? ctx.seconds() : 0.0F;
			this.locate(ctx);

			for (int design = 0; design < Island.this.meshes.length; design++) {
				Island.this.meshes[design] = this.pedestal(ctx, design);
			}

			for (Island.Spot s : Island.this.spots) {
				if (s.drawn && !(s.rise <= 0.01F) && !s.far()) {
					ctx.pose.pushPose();
					ctx.pose.translate(s.x, s.lift, s.z);
					ctx.pose.scale(s.scale, s.scale, s.scale);
					StageSoft.cast(Island.this.theme.stone, s.dull.get());
					Props.draw(Island.this.meshes[s.design], ctx, alpha * s.ghost());
					ctx.pose.popPose();
				}
			}

			StageSoft.noCast();
			if (!Island.this.falling.isEmpty()) {
				BlockMesh block = Island.this.props.get(Island.this.laidKey, ctx.soft, p -> p.put(0, 0, 0, Island.this.theme.laid));
				long now = Clock.nowMs();

				for (Island.Falling f : Island.this.falling) {
					float t = (float)(now - f.atMs()) / 260.0F;
					if (!(t <= 0.0F)) {
						float u = Math.min(1.0F, t);
						int x = f.cell() % Island.this.layout.w + Island.this.layout.minX;
						int z = f.cell() / Island.this.layout.w + Island.this.layout.minZ;
						ctx.pose.pushPose();
						ctx.pose.translate((float)x, -1.0F + 2.6F * (1.0F - u * u), (float)z);
						float size = 0.55F + 0.45F * u;
						ctx.pose.translate(0.0F, 0.5F, 0.0F);
						ctx.pose.scale(size, size, size);
						ctx.pose.translate(0.0F, -0.5F, 0.0F);
						Props.draw(block, ctx, alpha);
						ctx.pose.popPose();
					}
				}
			}

			boolean byGame = false;

			for (Island.Spot sx : Island.this.spots) {
				sx.blank = false;
				sx.live = null;
				if (this.holds(sx)) {
					ItemStack stack = sx.stack();
					if (stack.isEmpty()) {
						sx.blank = sx.glyph == null && sx.picture == null;
					} else {
						Things.Thing thing = Island.this.things.of(stack);
						float dull = sx.dull.get();
						if (ctx.soft && thing.parts.length != 0 && (!thing.live || !(dull < 0.5F))) {
							float there = alpha * sx.ghost();
							this.hold(ctx, sx, thing.block, time, alpha);
							StageSoft.cast(Island.this.theme.stone, thing.live ? Math.min(1.0F, (dull - 0.5F) * 2.0F) : dull);
							Things.draw(thing, ctx, there);
							if (thing.shimmer != null && dull < 0.98F) {
								Things.shimmer(thing, ctx, (1.0F - dull) * there);
							}

							ctx.pose.popPose();
						} else {
							sx.live = stack;
							sx.liveBlock = thing.block;
							byGame = true;
						}
					}
				}
			}

			StageSoft.noCast();
			if (byGame && !(alpha < 0.35F)) {
				float[] was = (float[])RenderSystem.getShaderColor().clone();
				ItemRenderer items = Minecraft.getInstance().getItemRenderer();
				float r = Math.min(1.0F, Island.this.theme.ambient[0] + 0.6F * Island.this.theme.key[0]);
				float g = Math.min(1.0F, Island.this.theme.ambient[1] + 0.6F * Island.this.theme.key[1]);
				float b = Math.min(1.0F, Island.this.theme.ambient[2] + 0.6F * Island.this.theme.key[2]);

				for (int pass = 0; pass < 2; pass++) {
					float tone = pass == 0 ? 1.0F : 0.78F;
					boolean begun = false;

					for (Island.Spot sxx : Island.this.spots) {
						if (sxx.live != null && sxx.dull.get() >= 0.5F == (pass == 1)) {
							if (!begun) {
								begun = true;
								ctx.buffers.endBatch();
								RenderSystem.setShaderColor(was[0] * r * tone, was[1] * g * tone, was[2] * b * tone, was[3]);
							}

							this.hold(ctx, sxx, sxx.liveBlock, time, alpha);

							try {
								items.renderStatic(sxx.live, ItemDisplayContext.FIXED, ctx.light, OverlayTexture.NO_OVERLAY, ctx.pose, ctx.buffers, ctx.level, 0);
							} catch (RuntimeException var17) {
							}

							ctx.pose.popPose();
						}
					}

					if (begun) {
						ctx.buffers.endBatch();
					}
				}

				RenderSystem.setShaderColor(was[0], was[1], was[2], was[3]);
			}
		}

		private void hold(StageRenderContext ctx, Island.Spot s, boolean block, float time, float alpha) {
			boolean still = s.node.locked() || s.atRest();
			boolean waiting = s.status == Rules.Status.CLAIMABLE;
			float bob = still ? 0.0F : Mth.sin(time * (waiting ? 3.4F : 2.1F) + s.phase) * 0.07F * s.scale;
			float turn = still ? 22.0F : (float)Math.toDegrees((double)s.phase) + time * (waiting ? 70.0F : 40.0F);
			float in = Mth.clamp((s.rise - 0.25F) / 0.75F, 0.0F, 1.0F);
			float size = (block ? 1.6F : 1.15F) * s.scale * (1.0F + 0.14F * s.node.hover.get()) * Ease.OUT_BACK.apply(in) * Math.min(1.0F, alpha * 1.4F);
			ctx.pose.pushPose();
			ctx.pose.translate(s.x, s.heart() + s.lift + bob, s.z);
			ctx.pose.mulPose(Axis.YP.rotationDegrees(turn));
			ctx.pose.scale(size, size, size);
		}

		private boolean holds(Island.Spot s) {
			return s.inView && s.rise > 0.25F && s.pr >= 5.8F && !s.node.secret();
		}

		@Nullable
		private BlockMesh pedestal(StageRenderContext ctx, int design) {
			return Island.this.props.get(Island.this.meshKeys[design], ctx.soft, p -> {
				switch (design) {
					case 1:
						p.put(0, 0, 0, Island.this.theme.goal);
						p.put(0, 1, 0, Island.this.theme.cap);
						break;
					case 2:
						p.put(0, 0, 0, Island.this.theme.challenge);
						p.put(0, 1, 0, Island.this.theme.post);
						p.put(0, 2, 0, Island.this.theme.cap);
						break;
					case 3:
						for (int y = 0; y < 2; y++) {
							p.put(-1, y, 0, Island.this.theme.gatePost);
							p.put(1, y, 0, Island.this.theme.gatePost);
						}

						p.put(-1, 2, 0, Island.this.theme.gateLintel);
						p.put(0, 2, 0, Island.this.theme.gateKey);
						p.put(1, 2, 0, Island.this.theme.gateLintel);
						break;
					default:
						p.put(0, 0, 0, Island.this.theme.post);
						p.put(0, 1, 0, Island.this.theme.cap);
				}
			});
		}

		private void locate(StageRenderContext ctx) {
			Island.this.toScreen.set(ctx.camera.projection()).mul(ctx.pose.last().pose());
			float size = Island.this.scale();

			for (Island.Spot s : Island.this.spots) {
				if (!s.node.visible) {
					s.inView = s.drawn = false;
					s.rise = 0.0F;
				} else {
					float in = s.node.appear();
					s.rise = in;
					float pop = in >= 1.0F ? 1.0F : Ease.OUT_BACK.apply(in);
					s.lift = ((pop - 1.0F) * 1.7F - (1.0F - s.node.lit.get()) * 0.32F + 0.07F * s.node.hover.get()) * s.scale;
					Island.this.point.set(s.x, s.heart() + s.lift, s.z, 1.0F);
					Island.this.toScreen.transform(Island.this.point);
					float reach = 3.6F * s.scale * size;
					float depth = Math.max(Island.this.point.w, 0.3F);
					float spread = reach / (depth * Island.this.lens.tan);
					s.drawn = Island.this.point.w > -reach && Math.abs(Island.this.point.x / depth) < 1.0F + spread && Math.abs(Island.this.point.y / depth) < 1.0F + spread;
					if (Island.this.point.w <= 0.05F) {
						s.inView = false;
						s.sr = s.pr = 1000.0F;
					} else {
						float nx = Island.this.point.x / Island.this.point.w;
						float ny = Island.this.point.y / Island.this.point.w;
						s.inView = nx > -1.2F && nx < 1.2F && ny > -1.4F && ny < 1.25F;
						s.sx = (float)Island.this.lens.x + (nx * 0.5F + 0.5F) * (float)Island.this.lens.w;
						s.sy = (float)Island.this.lens.y + (0.5F - ny * 0.5F) * (float)Island.this.lens.h;
						s.sr = 0.5F * s.scale * size * (float)Island.this.lens.h * 0.5F / (Island.this.point.w * Island.this.lens.tan);
						s.pr = s.sr * Island.this.lens.px;
					}
				}
			}
		}
	}

	static final class Spot {
		final NodeView node;
		final float x;
		final float z;
		final boolean gate;
		final int design;
		final float scale;
		final float top;
		final float phase;
		final QuestIcons.Drawable drawable;
		@Nullable
		final Icon glyph;
		@Nullable
		final ResourceLocation picture;
		final int beyond;
		final Anim dull = new Anim(1.0F, 420, Ease.OUT_CUBIC);
		final Anim light = new Anim(0.0F, 520, Ease.OUT_CUBIC);
		final Anim flag = new Anim(0.0F, 520, Ease.OUT_BACK);
		Rules.Status status = Rules.Status.LOCKED;
		boolean inView;
		boolean drawn;
		float sx;
		float sy;
		float sr;
		float pr;
		float rise;
		float lift;
		boolean blank;
		@Nullable
		private ItemStack live;
		private boolean liveBlock;

		Spot(NodeView node, IslandBuilder.Site site, float crowd) {
			this.node = node;
			this.x = (float)site.x;
			this.z = (float)site.z;
			this.gate = site.gate;
			Quest q = node.quest;
			this.design = this.gate ? 3 : (q.frame == Quest.Frame.CHALLENGE ? 2 : (q.frame == Quest.Frame.GOAL ? 1 : 0));
			float room = Mth.clamp(site.room / 3.4F, 0.7F, 1.0F) * crowd;
			this.scale = this.gate ? Mth.clamp(0.62F + 0.2F * site.size, 0.7F, 1.0F) * room : Mth.clamp(Mth.sqrt(site.size), 0.75F, 1.5F) * room;
			this.top = (this.gate ? 3.0F : (this.design == 2 ? 2.5F : 1.5F)) * this.scale;
			this.phase = IslandBuilder.hash(site.x, site.z, 977) * (float) (Math.PI * 2);
			this.drawable = node.icon();
			String spec = Island.iconSpec(node);
			Icon icon = null;
			ResourceLocation texture = null;
			if (spec.startsWith("icon:")) {
				try {
					icon = Icon.valueOf(spec.substring(5).trim().toUpperCase(Locale.ROOT));
				} catch (IllegalArgumentException var10) {
					icon = Icon.QUESTION;
				}
			} else if (spec.startsWith("texture:")) {
				texture = ResourceLocation.tryParse(spec.substring(8).trim());
				if (texture == null) {
					icon = Icon.QUESTION;
				}
			}

			this.glyph = icon;
			this.picture = texture;
			QuestBook book = QuestsState.book();
			this.beyond = this.gate && book != null ? QuestPainter.accentOf(book.chapter(q.subtree)) : 0;
		}

		ItemStack stack() {
			return this.glyph == null && this.picture == null ? this.drawable.stack() : ItemStack.EMPTY;
		}

		boolean atRest() {
			return this.status == Rules.Status.COMPLETE && !this.gate;
		}

		float heart() {
			return this.top + (this.atRest() ? 0.46F : 0.6F) * this.scale;
		}

		boolean far() {
			return this.pr < 5.2F;
		}

		float ghost() {
			return 1.0F - 0.6F * (1.0F - this.node.lit.get());
		}
	}
}
