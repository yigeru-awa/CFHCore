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
import java.util.Map.Entry;
import java.util.function.LongConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class TreeGraph {
	static final int PLATE = 18;
	static final int NODE_H = 29;
	static final int MIN_W = 22;
	static final int GAP = 8;
	static final int LEVEL_GAP = 18;
	private static final int CAPSULE_H = 18;
	private static final int RAW_LINE = 10;
	private static final int PAD = 12;
	private static final int MOVE_MS = 220;
	private static final int ZOOM_MS = 140;
	private static final int LIT_MS = 900;
	private static final int SHAKE_MS = 320;
	private static final int BURST_MS = 1300;
	private static final float OVERVIEW = 0.45F;
	private static final float MIN_ZOOM = 0.03F;
	private static final float MAX_ZOOM = 2.5F;
	private final TreeView.Host host;
	private int x;
	private int y;
	private int w;
	private int h;
	@Nullable
	private TreeSession session;
	private int rowsVersion = -1;
	private int laidLooks = -1;
	private List<TreeGraph.Node> nodes = List.of();
	private Map<String, TreeGraph.Node> byId = new HashMap<>();
	private final List<TreeGraph.Node> gone = new ArrayList<>();
	private final List<TreeGraph.Node> visible = new ArrayList<>();
	private long changedAt;
	private float minX;
	private float maxX;
	private int maxY;
	private final Map<Island, List<TreeGraph.Node>> grounds = new IdentityHashMap<>();
	private float camX;
	private float camY;
	private float zoom = 1.0F;
	private float camFromX;
	private float camFromY;
	private float camToX;
	private float camToY;
	private float zoomFrom = 1.0F;
	private float zoomTo = 1.0F;
	private long camAt;
	private int camMs;
	private boolean anchored;
	private float anchorSX;
	private float anchorSY;
	private float anchorWX;
	private float anchorWY;
	private boolean placed;
	private int lw = 1;
	@Nullable
	private TreeGraph.Node hovered;
	@Nullable
	private Island hoveredGround;
	private TreeGraph.Zone zone = TreeGraph.Zone.NONE;
	private boolean pressed;
	private boolean panning;
	private int pressButton = -1;
	private double pressX;
	private double pressY;
	@Nullable
	private TreeGraph.Node pressedNode;
	@Nullable
	private Island pressedGround;
	private TreeGraph.Zone pressedZone = TreeGraph.Zone.NONE;
	@Nullable
	private String lastClicked;
	private long lastClickAt;
	@Nullable
	private String anchorId;
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
	private static final int CONTROL = 13;

	public TreeGraph(TreeView.Host host) {
		this.host = host;
	}

	public void setBounds(int x, int y, int w, int h) {
		if (this.placed && (w != this.w || h != this.h) && this.w > 0 && this.h > 0) {
			float midX = this.camX + (float)this.w / 2.0F / this.zoom;
			float midY = this.camY + (float)this.h / 2.0F / this.zoom;
			this.hold();
			this.camX = this.camToX = midX - (float)w / 2.0F / this.zoom;
			this.camY = this.camToY = midY - (float)h / 2.0F / this.zoom;
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
		if (now == null) {
			this.session = null;
			this.nodes = List.of();
			this.byId = new HashMap<>();
			this.gone.clear();
			this.grounds.clear();
			this.rowsVersion = -1;
		} else {
			boolean other = now != this.session;
			int looks = now.looks();
			if (other || this.rowsVersion != now.rowsVersion() || this.laidLooks != looks) {
				List<TreeRow> rows = now.rows();
				if (!other && this.rowsVersion == now.rowsVersion()) {
					this.laidLooks = looks;
					boolean wider = false;

					for (TreeGraph.Node node : this.nodes) {
						int width = width(now, node.row);
						if (width != node.w) {
							wider = true;
							break;
						}
					}

					if (!wider) {
						return;
					}
				}

				boolean moving = Theme.current().motion() > 0.0F;
				Map<String, TreeGraph.Node> old = (Map<String, TreeGraph.Node>)(other ? new HashMap<>() : this.byId);
				TreeGraph.Node anchor = !other && this.anchorId != null ? old.get(this.anchorId) : null;
				float anchorScreenX = anchor == null ? 0.0F : (anchor.cx - this.camX) * this.zoom;
				float anchorScreenY = anchor == null ? 0.0F : (anchor.cy - this.camY) * this.zoom;
				int n = rows.size();
				List<TreeGraph.Node> made = new ArrayList<>(n);
				Map<String, TreeGraph.Node> next = new HashMap<>(n * 2);
				int[] parent = new int[n];
				int[] width = new int[n];
				int[] height = new int[n];
				int[] depth = new int[n];

				for (int i = 0; i < n; i++) {
					TreeRow row = rows.get(i);
					TreeGraph.Node was = old.get(row.id);
					TreeGraph.Node nodex = new TreeGraph.Node(row);
					if (was != null) {
						nodex.cx = was.cx;
						nodex.cy = was.cy;
						nodex.alpha = was.alpha;
						nodex.hover.snap(was.hover.get());
						nodex.label = was.label;
						nodex.labelLooks = was.labelLooks;
					}

					nodex.w = width(now, row);
					nodex.h = row.isIsland() ? 18 + (rawsOf(now, row) ? 10 : 0) : 29;
					nodex.foldedSteps = row.folded && row.node != null ? steps(row.node) : 0;
					parent[i] = row.parent;
					width[i] = nodex.w;
					height[i] = nodex.h;
					depth[i] = row.depth;
					made.add(nodex);
					next.put(row.id, nodex);
				}

				GraphLayout layout = GraphLayout.of(parent, width, height, depth, 8.0F, 18);
				this.minX = layout.minX;
				this.maxX = layout.maxX;
				this.maxY = layout.maxY;
				Map<String, String> capsuleOf = new HashMap<>();

				for (TreeRow row : rows) {
					if (row.isIsland()) {
						for (TreeNode hidden : row.hidden) {
							capsuleOf.put(hidden.path, row.id);
						}
					}
				}

				Map<String, String> wasCapsuleOf = new HashMap<>();

				for (TreeGraph.Node nodex : old.values()) {
					if (nodex.island()) {
						for (TreeNode hidden : nodex.row.hidden) {
							wasCapsuleOf.put(hidden.path, nodex.row.id);
						}
					}
				}

				for (int i = 0; i < n; i++) {
					TreeGraph.Node nodexx = made.get(i);
					nodexx.tx = layout.x[i];
					nodexx.ty = (float)layout.y[i];
					boolean known = old.containsKey(nodexx.row.id);
					if (known && moving) {
						nodexx.fx = nodexx.cx;
						nodexx.fy = nodexx.cy;
						nodexx.fromAlpha = nodexx.alpha;
					} else if (moving && !other) {
						TreeGraph.Node from = cameFrom(nodexx, made, old, wasCapsuleOf);
						nodexx.fx = from != null ? from.cx : nodexx.tx;
						nodexx.fy = from != null ? from.cy : nodexx.ty;
						nodexx.fromAlpha = 0.0F;
						nodexx.entering = true;
					} else {
						nodexx.fx = nodexx.tx;
						nodexx.fy = nodexx.ty;
						nodexx.fromAlpha = other && moving ? 0.0F : 1.0F;
						nodexx.entering = other && moving;
					}

					nodexx.cx = nodexx.fx;
					nodexx.cy = nodexx.fy;
					nodexx.alpha = nodexx.fromAlpha;
				}

				this.gone.clear();
				if (moving && !other) {
					for (TreeGraph.Node was : old.values()) {
						if (!next.containsKey(was.row.id) && !(was.alpha < 0.05F)) {
							TreeGraph.Node into = null;
							String capsule = was.row.node != null ? capsuleOf.get(was.row.node.path) : null;
							if (capsule != null) {
								into = next.get(capsule);
							}

							if (into == null) {
								into = nearestStaying(was, old, next);
							}

							was.leaving = true;
							was.fx = was.cx;
							was.fy = was.cy;
							was.fromAlpha = was.alpha;
							was.tx = into != null ? into.tx : was.cx;
							was.ty = into != null ? into.ty : was.cy;
							this.gone.add(was);
						}
					}
				}

				this.nodes = made;
				this.byId = next;
				this.session = now;
				this.rowsVersion = now.rowsVersion();
				this.laidLooks = looks;
				this.changedAt = Clock.nowMs();
				this.grounds.clear();

				for (TreeGraph.Node nodexx : made) {
					for (Island island : nodexx.row.within) {
						this.grounds.computeIfAbsent(island, k -> new ArrayList<>()).add(nodexx);
					}
				}

				this.preview = null;
				this.previewFor = null;
				if (other || !this.placed) {
					this.home(false);
					this.placed = true;
				} else if (anchor != null) {
					TreeGraph.Node now2 = next.get(anchor.row.id);
					if (now2 != null) {
						this.move(now2.tx - anchorScreenX / this.zoom, now2.ty - anchorScreenY / this.zoom, this.zoom, 220, this.changedAt);
					}
				}
			}
		}
	}

	@Nullable
	private static TreeGraph.Node cameFrom(TreeGraph.Node node, List<TreeGraph.Node> made, Map<String, TreeGraph.Node> old, Map<String, String> wasCapsuleOf) {
		if (node.row.node != null) {
			String capsule = wasCapsuleOf.get(node.row.node.path);
			if (capsule != null && old.get(capsule) != null) {
				return old.get(capsule);
			}
		}

		int p = node.row.parent;

		while (p >= 0) {
			TreeGraph.Node was = old.get(made.get(p).row.id);
			if (was != null) {
				return was;
			}

			p = made.get(p).row.parent;
		}

		return null;
	}

	@Nullable
	private static TreeGraph.Node nearestStaying(TreeGraph.Node leaving, Map<String, TreeGraph.Node> old, Map<String, TreeGraph.Node> next) {
		for (String path = leaving.row.path(); path != null; path = TreePaths.parent(path)) {
			TreeGraph.Node there = next.get(path);
			if (there != null) {
				return there;
			}
		}

		return null;
	}

	private static int width(TreeSession s, TreeRow row) {
		if (row.isIsland()) {
			return capsuleWidth(s, row);
		} else {
			TreeNode node = row.node;
			TreeFigures f = s.figures(node.path);
			if (f.wanted <= 0L) {
				return 22;
			} else {
				String most = TreeTips.amount(node.ingredient(), f.wanted, f.chance);
				return Math.max(22, CFHGuiDraw.width(most + "/" + most.replace("~", "")) + 4);
			}
		}
	}

	private static int capsuleWidth(TreeSession s, TreeRow row) {
		TreeFigures.Fold fold = s.fold(row);
		int head = 15 + CFHGuiDraw.width(String.valueOf(fold.steps)) + 4 + Math.min(3, fold.first.size()) * 9 + 4;
		int raws = 6;
		int shown = 0;

		for (TreeFigures.Raw raw : fold.raws) {
			if (shown++ >= 3) {
				break;
			}

			raws += 9 + CFHGuiDraw.width(rawText(raw)) + 5;
		}

		return Math.max(40, Math.max(head, raws));
	}

	private static boolean rawsOf(TreeSession s, TreeRow row) {
		return !s.fold(row).raws.isEmpty();
	}

	private static int steps(TreeNode node) {
		int count = 0;

		for (TreeNode child : node.children) {
			count += (child.step() ? 1 : 0) + steps(child);
		}

		return count;
	}

	private static String rawText(TreeFigures.Raw raw) {
		String wanted = TreeTips.amount(raw.ingredient, raw.wanted, 1.0F);
		return (raw.estimated ? "~" : "") + (raw.have() > 0L ? TreeTips.amount(raw.ingredient, raw.have(), 1.0F) + "/" + wanted : wanted);
	}

	private static float guiScale() {
		return (float)Minecraft.getInstance().getWindow().getGuiScale();
	}

	private static float sharp(float z) {
		float gs = guiScale();
		return z * gs < 1.0F ? Mth.clamp(z, 0.03F, 1.0F / gs) : Mth.clamp((float)Math.floor((double)(z * gs + 0.001F)) / gs, 1.0F / gs, 2.5F);
	}

	private void move(float toX, float toY, float toZoom, int ms, long at) {
		this.camFromX = this.camX;
		this.camFromY = this.camY;
		this.zoomFrom = this.zoom;
		this.camToX = toX;
		this.camToY = toY;
		this.zoomTo = toZoom;
		this.camAt = at;
		this.camMs = Theme.current().motion() <= 0.0F ? 0 : Theme.current().ms(ms);
		this.anchored = false;
		if (this.camMs <= 0) {
			this.camX = toX;
			this.camY = toY;
			this.zoom = toZoom;
		}
	}

	private void zoomAt(float sx, float sy, float toZoom) {
		float z = Mth.clamp(toZoom, 0.03F, 2.5F);
		this.anchorSX = sx - (float)this.x;
		this.anchorSY = sy - (float)this.y;
		this.anchorWX = this.camX + this.anchorSX / this.zoom;
		this.anchorWY = this.camY + this.anchorSY / this.zoom;
		this.zoomFrom = this.zoom;
		this.zoomTo = z;
		this.camAt = Clock.nowMs();
		this.camMs = Theme.current().motion() <= 0.0F ? 0 : Theme.current().ms(140);
		this.anchored = true;
		this.camToX = this.anchorWX - this.anchorSX / z;
		this.camToY = this.anchorWY - this.anchorSY / z;
		if (this.camMs <= 0) {
			this.zoom = z;
			this.camX = this.camToX;
			this.camY = this.camToY;
			this.anchored = false;
		}
	}

	private void camera() {
		if (this.camMs > 0) {
			float t = Mth.clamp((float)(Clock.nowMs() - this.camAt) / (float)this.camMs, 0.0F, 1.0F);
			float e = Ease.OUT_CUBIC.apply(t);
			this.zoom = Mth.lerp(e, this.zoomFrom, this.zoomTo);
			if (this.anchored) {
				this.camX = this.anchorWX - this.anchorSX / this.zoom;
				this.camY = this.anchorWY - this.anchorSY / this.zoom;
			} else {
				this.camX = Mth.lerp(e, this.camFromX, this.camToX);
				this.camY = Mth.lerp(e, this.camFromY, this.camToY);
			}

			if (t >= 1.0F) {
				this.camMs = 0;
				this.zoom = this.zoomTo;
				this.camX = this.camToX;
				this.camY = this.camToY;
				this.anchored = false;
			}
		}
	}

	private void hold() {
		this.camMs = 0;
		this.anchored = false;
		this.zoomTo = this.zoom;
		this.camToX = this.camX;
		this.camToY = this.camY;
	}

	private void home(boolean smooth) {
		float contentW = this.maxX - this.minX + 24.0F;
		float contentH = (float)(this.maxY + 24);
		float fit = Math.min((float)this.w / Math.max(1.0F, contentW), (float)this.h / Math.max(1.0F, contentH));
		float gs = guiScale();
		float z = fit >= 0.6F ? 1.0F : Math.max(sharp(fit), (float)Math.ceil((double)(0.5F * gs - 0.001F)) / gs);
		float cx = (this.minX + this.maxX) / 2.0F;
		TreeGraph.Node goal = this.nodes.isEmpty() ? null : this.nodes.get(0);
		boolean whole = contentW * z <= (float)this.w;
		float toX = !whole && goal != null ? goal.tx - (float)this.w / 2.0F / z : cx - (float)this.w / 2.0F / z;
		float toY = contentH * z <= (float)this.h ? -12.0F - ((float)this.h / z - contentH) / 2.0F : -12.0F;
		if (smooth) {
			this.move(toX, toY, z, 220, Clock.nowMs());
		} else {
			this.camX = this.camToX = toX;
			this.camY = this.camToY = toY;
			this.zoom = this.zoomTo = z;
			this.camMs = 0;
		}
	}

	public void fit() {
		float contentW = this.maxX - this.minX + 24.0F;
		float contentH = (float)(this.maxY + 24);
		float z = sharp(Math.min(1.0F, Math.min((float)this.w / Math.max(1.0F, contentW), (float)this.h / Math.max(1.0F, contentH))));
		float cx = (this.minX + this.maxX) / 2.0F;
		this.move(cx - (float)this.w / 2.0F / z, -12.0F - Math.max(0.0F, ((float)this.h / z - contentH) / 2.0F), z, 220, Clock.nowMs());
	}

	private void zoomStep(int dir, float sx, float sy) {
		float gs = guiScale();
		float k = this.zoomTo * gs;
		float next;
		if (!(k > 1.001F) && (!(k >= 0.999F) || dir <= 0)) {
			next = Math.min(1.0F, dir > 0 ? k * 1.25F : k / 1.25F);
		} else {
			next = Math.max(1.0F, (float)(Math.round(k) + dir));
		}

		this.zoomAt(sx, sy, next / gs);
	}

	private void place() {
		long now = Clock.nowMs();
		float duration = (float)Theme.current().ms(220);
		float t = duration <= 0.0F ? 1.0F : Mth.clamp((float)(now - this.changedAt) / duration, 0.0F, 1.0F);
		float e = Ease.OUT_CUBIC.apply(t);

		for (TreeGraph.Node node : this.nodes) {
			place(node, t, e);
		}

		for (TreeGraph.Node node : this.gone) {
			place(node, t, e);
		}

		this.gone.removeIf(nodex -> nodex.alpha <= 0.01F);
	}

	private static void place(TreeGraph.Node node, float t, float e) {
		node.cx = Mth.lerp(e, node.fx, node.tx);
		node.cy = Mth.lerp(e, node.fy, node.ty);
		if (node.leaving) {
			node.alpha = node.fromAlpha * (1.0F - smooth(t / 0.6F));
		} else if (node.entering) {
			node.alpha = smooth((t - 0.2F) / 0.8F);
		} else {
			node.alpha = Mth.lerp(e, node.fromAlpha, 1.0F);
		}

		if (t >= 1.0F && !node.leaving) {
			node.entering = false;
			node.fx = node.tx;
			node.fy = node.ty;
			node.fromAlpha = 1.0F;
		}
	}

	private static float smooth(float t) {
		float k = Mth.clamp(t, 0.0F, 1.0F);
		return k * k * (3.0F - 2.0F * k);
	}

	public void render(GuiGraphics g, @Nullable TreeSession tree, int mouseX, int mouseY, float delta) {
		LongConsumer timing = TreeView.probe;
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
			this.camera();
			this.place();
			this.visible.clear();
			float wx0 = this.camX - 24.0F;
			float wx1 = this.camX + (float)this.w / this.zoom + 24.0F;
			float wy0 = this.camY - 24.0F;
			float wy1 = this.camY + (float)this.h / this.zoom + 24.0F;

			for (TreeGraph.Node node : this.gone) {
				if (inSight(node, wx0, wx1, wy0, wy1)) {
					this.visible.add(node);
				}
			}

			for (TreeGraph.Node nodex : this.nodes) {
				if (inSight(nodex, wx0, wx1, wy0, wy1)) {
					this.visible.add(nodex);
				}
			}

			this.hover(s, mouseX, mouseY);
			this.preview(s);
			g.enableScissor(this.x, this.y, this.x + this.w, this.y + this.h);
			g.pose().pushPose();
			float gs = guiScale();
			this.lw = Math.max(1, (int)Math.ceil((double)(1.0F / (this.zoom * gs) - 0.001F)));
			g.pose()
				.translate((float)Math.round(((float)this.x - this.camX * this.zoom) * gs) / gs, (float)Math.round(((float)this.y - this.camY * this.zoom) * gs) / gs, 0.0F);
			g.pose().scale(this.zoom, this.zoom, 1.0F);
			boolean overview = this.zoom < 0.45F;
			g.drawManaged(() -> {
				this.grounds(g, s);
				this.connectors(g, s, wx0, wx1, wy0, wy1);

				for (TreeGraph.Node nodexx : this.visible) {
					this.flat(g, s, nodexx, overview);
				}
			});
			if (!overview) {
				for (TreeGraph.Node nodexx : this.visible) {
					this.items(g, s, nodexx, delta);
				}
			}

			g.pose().pushPose();
			g.pose().translate(0.0F, 0.0F, 260.0F);
			g.drawManaged(() -> {
				for (TreeGraph.Node nodexx : this.visible) {
					this.over(g, s, nodexx, overview);
				}

				this.burst(g, s);
			});
			g.pose().popPose();
			g.pose().popPose();
			this.controls(g, mouseX, mouseY);
			g.disableScissor();
		}
	}

	private static boolean inSight(TreeGraph.Node node, float wx0, float wx1, float wy0, float wy1) {
		float left = node.cx - (float)node.w / 2.0F;
		float right = left + (float)node.w;
		return right >= wx0 && left <= wx1 && node.cy + (float)node.h + 12.0F >= wy0 && node.cy <= wy1;
	}

	private float worldX(double sx) {
		return this.camX + (float)(sx - (double)this.x) / this.zoom;
	}

	private float worldY(double sy) {
		return this.camY + (float)(sy - (double)this.y) / this.zoom;
	}

	private void hover(TreeSession s, int mouseX, int mouseY) {
		TreeGraph.Node was = this.hovered;
		this.hovered = null;
		this.hoveredGround = null;
		this.zone = TreeGraph.Zone.NONE;
		if (this.contains((double)mouseX, (double)mouseY) && !Popups.any() && !this.panning) {
			TreeGraph.Zone control = this.controlAt((double)mouseX, (double)mouseY);
			if (control != TreeGraph.Zone.NONE) {
				this.zone = control;
			} else {
				float wx = this.worldX((double)mouseX);
				float wy = this.worldY((double)mouseY);

				for (int i = this.visible.size() - 1; i >= 0; i--) {
					TreeGraph.Node node = this.visible.get(i);
					if (!node.leaving) {
						TreeGraph.Zone z = this.zoneOf(s, node, wx, wy);
						if (z != TreeGraph.Zone.NONE) {
							this.hovered = node;
							this.zone = z;
							break;
						}
					}
				}

				if (this.hovered == null) {
					this.hoveredGround = this.groundAt(wx, wy);
					if (this.hoveredGround != null) {
						this.zone = TreeGraph.Zone.ISLAND_GROUND;
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

	private TreeGraph.Zone zoneOf(TreeSession s, TreeGraph.Node node, float wx, float wy) {
		float cx = node.cx;
		float top = node.cy;
		if (node.island()) {
			float left = cx - (float)node.w / 2.0F;
			if (craftShown(s, node) && wx >= left + (float)node.w - 6.0F && wx < left + (float)node.w + 6.0F && wy >= top - 5.0F && wy < top + 7.0F) {
				return TreeGraph.Zone.CRAFT;
			} else {
				return wx >= left && wx < left + (float)node.w && wy >= top && wy < top + (float)node.h ? TreeGraph.Zone.BODY : TreeGraph.Zone.NONE;
			}
		} else {
			float plateX = (float)Math.round(cx) - 9.0F;
			if (craftShown(s, node) && wx >= plateX + 12.0F && wx < plateX + 24.0F && wy >= top - 6.0F && wy < top + 6.0F) {
				return TreeGraph.Zone.CRAFT;
			} else {
				if (node.row.hasChildren && !s.foldMode) {
					float ky = top + 29.0F + 3.0F;
					if (Math.abs(wx - cx) <= (node.row.folded ? (float)this.knobWidth(node) / 2.0F + 1.0F : 5.0F) && wy >= ky - 4.0F && wy < ky + 5.0F) {
						return TreeGraph.Zone.KNOB;
					}
				}

				float left = cx - (float)node.w / 2.0F;
				return wx >= left && wx < left + (float)node.w && wy >= top - 1.0F && wy < top + 29.0F ? TreeGraph.Zone.BODY : TreeGraph.Zone.NONE;
			}
		}
	}

	@Nullable
	private Island groundAt(float wx, float wy) {
		Island found = null;
		int deepest = -1;

		for (Entry<Island, List<TreeGraph.Node>> entry : this.grounds.entrySet()) {
			for (TreeGraph.Node node : entry.getValue()) {
				int depth = node.row.within.indexOf(entry.getKey());
				int grow = 6 - Math.min(3, depth) * 2;
				float left = node.cx - (float)node.w / 2.0F - (float)grow;
				float top = node.cy - (float)grow;
				if (wx >= left && wx < left + (float)node.w + (float)(grow * 2) && wy >= top && wy < top + (float)node.h + (float)(grow * 2)) {
					if (depth > deepest) {
						deepest = depth;
						found = entry.getKey();
					}
					break;
				}
			}
		}

		return found;
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

	private static boolean craftShown(TreeSession s, TreeGraph.Node node) {
		if (node.leaving) {
			return false;
		} else if (node.island()) {
			return s.fold(node.row).next != null;
		} else {
			TreeFigures f = s.figures(node.row.node.path);
			return f.state == TreeFigures.State.READY && f.here && node.row.node.step();
		}
	}

	private void grounds(GuiGraphics g, TreeSession s) {
		if (!this.grounds.isEmpty()) {
			Theme t = Theme.current();
			Palette p = t.palette();
			boolean van = t.isVanilla();
			List<Entry<Island, List<TreeGraph.Node>>> order = new ArrayList<>(this.grounds.entrySet());
			order.sort((a, b) -> Integer.compare(depthOf((Entry<Island, List<TreeGraph.Node>>)a), depthOf((Entry<Island, List<TreeGraph.Node>>)b)));

			for (Entry<Island, List<TreeGraph.Node>> entry : order) {
				Island island = entry.getKey();
				int depth = Math.max(0, depthOf(entry));
				boolean hot = this.hoveredGround == island;
				float amount = Math.min(0.32F, 0.1F + (float)depth * 0.06F + (hot ? 0.07F : 0.0F));
				int base = van ? -14277082 : p.bg2();
				int color = Colors.mix(base, van ? -1 : p.accent(), amount);
				int grow = 6 - Math.min(3, depth) * 2;

				for (TreeGraph.Node node : entry.getValue()) {
					if (!(node.alpha < 0.3F)) {
						int left = Math.round(node.cx - (float)node.w / 2.0F) - grow;
						int top = Math.round(node.cy) - grow;
						CFHGuiDraw.pixelRound(g, left, top, node.w + grow * 2, node.h + grow * 2, color, van ? 0 : 3);
						int parent = node.row.parent;
						if (parent >= 0 && parent < this.nodes.size()) {
							TreeGraph.Node up = this.nodes.get(parent);
							if (up.row.within.contains(island)) {
								int px = Math.round(up.cx);
								int cxi = Math.round(node.cx);
								int bottom = Math.round(up.cy) + up.h;
								int ctop = Math.round(node.cy);
								int mid = bottom + (ctop - bottom) / 2;
								g.fill(px - grow + 2, bottom, px + grow - 1, mid + grow - 1, color);
								g.fill(Math.min(px, cxi) - grow + 2, mid - grow + 2, Math.max(px, cxi) + grow - 1, mid + grow - 1, color);
								g.fill(cxi - grow + 2, mid - grow + 2, cxi + grow - 1, ctop, color);
							}
						}
					}
				}
			}
		}
	}

	private static int depthOf(Entry<Island, List<TreeGraph.Node>> entry) {
		return entry.getValue().isEmpty() ? 0 : entry.getValue().get(0).row.within.indexOf(entry.getKey());
	}

	private static int dropColor(TreeSession s, TreeGraph.Node node) {
		if (node.island()) {
			return s.fold(node.row).ready ? (TreePaint.vanilla() ? -1 : TreePaint.accent()) : TreePaint.rail();
		} else {
			TreeFigures f = s.figures(node.row.node.path);

			return switch (f.state) {
				case DONE -> f.unasked() ? TreePaint.rail() : Colors.mix(TreePaint.rail(), TreePaint.success(), TreePaint.vanilla() ? 0.7F : 0.5F);
				case READY -> TreePaint.vanilla() ? -1 : TreePaint.accent();
				default -> TreePaint.rail();
			};
		}
	}

	private void connectors(GuiGraphics g, TreeSession s, float wx0, float wx1, float wy0, float wy1) {
		int rail = TreePaint.rail();

		for (TreeGraph.Node parent : this.nodes) {
			if (parent.row.hasChildren && !parent.row.folded && !parent.leaving) {
				int first = parent.row.index + 1;
				if (first < this.nodes.size() && this.nodes.get(first).row.parent == parent.row.index) {
					int lo = Integer.MAX_VALUE;
					int hi = Integer.MIN_VALUE;
					int childTop = Integer.MAX_VALUE;
					int count = 0;

					for (int c = first; c >= 0 && c < this.nodes.size(); c = this.nodes.get(c).row.nextSibling) {
						TreeGraph.Node child = this.nodes.get(c);
						lo = Math.min(lo, Math.round(child.cx));
						hi = Math.max(hi, Math.round(child.cx));
						childTop = Math.min(childTop, Math.round(child.cy));
						count++;
						if (child.row.nextSibling < 0) {
							break;
						}
					}

					if (count != 0) {
						int px = Math.round(parent.cx);
						int bottom = Math.round(parent.cy) + parent.h + (parent.row.isIsland() ? 0 : 1);
						if (!((float)Math.max(hi, px) < wx0) && !((float)Math.min(lo, px) > wx1) && !((float)childTop < wy0 - 40.0F) && !((float)bottom > wy1)) {
							int mid = bottom + Math.max(3, (childTop - bottom) / 2);
							float a = parent.alpha;
							int trunk = Colors.scaleAlpha(rail, a);
							int t = this.lw;
							if (count == 1 && Math.abs(lo - px) <= 1) {
								TreeGraph.Node only = this.nodes.get(first);
								g.fill(px, bottom, px + t, mid, trunk);
								g.fill(px, mid, px + t, Math.round(only.cy), Colors.scaleAlpha(dropColor(s, only), Math.min(a, only.alpha)));
							} else {
								g.fill(px, bottom, px + t, mid, trunk);
								if (t > 1) {
									g.fill(Math.min(px, lo), mid, Math.max(px, hi) + t, mid + t, trunk);

									for (int cx = first; cx >= 0 && cx < this.nodes.size(); cx = this.nodes.get(cx).row.nextSibling) {
										TreeGraph.Node child = this.nodes.get(cx);
										g.fill(Math.round(child.cx), mid, Math.round(child.cx) + t, Math.round(child.cy), Colors.scaleAlpha(dropColor(s, child), Math.min(a, child.alpha)));
										if (child.row.nextSibling < 0) {
											break;
										}
									}
								} else {
									int barL = Math.min(px, lo + 2);
									int barR = Math.max(px + 1, hi - 1);
									if (barR > barL) {
										g.fill(barL, mid, barR, mid + 1, trunk);
									}

									for (int cxx = first; cxx >= 0 && cxx < this.nodes.size(); cxx = this.nodes.get(cxx).row.nextSibling) {
										TreeGraph.Node child = this.nodes.get(cxx);
										int cxi = Math.round(child.cx);
										int ctop = Math.round(child.cy);
										int color = Colors.scaleAlpha(dropColor(s, child), Math.min(a, child.alpha));
										if (cxi == lo && cxi < px) {
											g.fill(cxi + 1, mid + 1, cxi + 2, mid + 2, color);
											g.fill(cxi, mid + 2, cxi + 1, ctop, color);
										} else if (cxi == hi && cxi > px) {
											g.fill(cxi - 1, mid + 1, cxi, mid + 2, color);
											g.fill(cxi, mid + 2, cxi + 1, ctop, color);
										} else {
											g.fill(cxi, mid + 1, cxi + 1, ctop, color);
										}

										if (child.row.nextSibling < 0) {
											break;
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private int shakeOf(TreeGraph.Node node) {
		if (this.refusedId != null && this.refusedId.equals(node.row.id)) {
			float t = (float)(Clock.nowMs() - this.refusedAt) / 320.0F;
			return !(t >= 1.0F) && !(Theme.current().motion() <= 0.0F) ? Math.round(Mth.sin(t * (float) (Math.PI * 2) * 3.0F) * 3.0F * (1.0F - t)) : 0;
		} else {
			return 0;
		}
	}

	@Nullable
	private static String label(TreeGraph.Node node, TreeFigures f, int looks) {
		if (node.labelLooks == looks) {
			return node.label;
		} else {
			EmiIngredient ingredient = node.row.node.ingredient();
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

			node.label = now;
			node.labelLooks = looks;
			return now;
		}
	}

	private int knobWidth(TreeGraph.Node node) {
		return CFHGuiDraw.width("+" + node.foldedSteps) + 6;
	}

	private void flat(GuiGraphics g, TreeSession s, TreeGraph.Node node, boolean overview) {
		float a = node.alpha;
		if (!(a <= 0.01F)) {
			Theme t = Theme.current();
			Palette p = t.palette();
			boolean van = t.isVanilla();
			if (node.island()) {
				this.capsule(g, s, node, overview);
			} else {
				TreeNode tn = node.row.node;
				TreeFigures f = s.figures(tn.path);
				int shake = this.shakeOf(node);
				int cx = Math.round(node.cx) + shake;
				int top = Math.round(node.cy);
				int plateX = cx - 9;
				int left = Math.round(node.cx - (float)node.w / 2.0F) + shake;
				float hov = node.hover.get();
				boolean selected = tn.path.equals(s.selected);
				boolean done = f.state == TreeFigures.State.DONE;
				if (selected) {
					CFHGuiDraw.pixelRound(g, left - 3, top - 3, node.w + 6, 34, Colors.scaleAlpha(van ? 1358954495 : Colors.withAlpha(p.surfaceActive(), 224), a), van ? 0 : 3);
					CFHGuiDraw.outline(g, left - 3, top - 3, node.w + 6, 34, Colors.scaleAlpha(TreePaint.accent(), a), van ? 0 : 3);
				} else if (hov > 0.01F) {
					CFHGuiDraw.pixelRound(
						g, left - 3, top - 3, node.w + 6, 34, Colors.scaleAlpha(van ? 822083583 : Colors.withAlpha(p.surfaceHover(), 192), a * hov), van ? 0 : 3
					);
				}

				if (this.preview != null) {
					int wash = this.previewRefused ? TreePaint.danger() : TreePaint.accent();
					if (this.preview.hides(tn.path)) {
						CFHGuiDraw.pixelRound(g, left - 3, top - 3, node.w + 6, 34, Colors.scaleAlpha(Colors.withAlpha(wash, 64), a), van ? 0 : 3);
					} else if (tn.path.equals(this.preview.a) || tn.path.equals(this.preview.b)) {
						CFHGuiDraw.outline(g, left - 3, top - 3, node.w + 6, 34, Colors.scaleAlpha(Colors.withAlpha(wash, 208), a), van ? 0 : 3);
					}
				}

				Long lit = s.lit.get(tn.path);
				float glow = lit != null && !(t.motion() <= 0.0F) ? 1.0F - Mth.clamp((float)(Clock.nowMs() - lit) / (float)t.ms(900), 0.0F, 1.0F) : 0.0F;
				if (glow > 0.01F) {
					CFHGuiDraw.pixelRound(g, left - 3, top - 3, node.w + 6, 34, Colors.scaleAlpha(Colors.withAlpha(TreePaint.accent(), 80), a * glow * glow), 3);
				}

				if (s.foldMode) {
					int hx = plateX - 6;
					int hy = top + 9;
					if (tn.path.equals(s.firstPoint)) {
						float beat = t.motion() <= 0.0F ? 1.0F : 0.6F + 0.4F * Mth.sin((float)Clock.nowMs() / 180.0F);
						CFHGuiDraw.outline(g, plateX - 2, top - 2, 22, 22, Colors.scaleAlpha(Colors.withAlpha(TreePaint.accent(), Math.round(224.0F * beat)), a), van ? 0 : 3);
						TreePaint.pixelsCentered(g, TreePaint.DIAMOND, hx, hy, Colors.scaleAlpha(TreePaint.accent(), a));
					} else {
						TreePaint.pixelsCentered(
							g, hov > 0.5F ? TreePaint.DIAMOND : TreePaint.DIAMOND_HOLLOW, hx, hy, Colors.scaleAlpha(Colors.lerp(TreePaint.dim(), TreePaint.accent(), hov), a)
						);
					}
				}
				int mark = switch (f.state) {
					case DONE, COVERED -> TreePaint.success();
					case READY -> TreePaint.accent();
					case MISSING, GIVEN -> TreePaint.warning();
					default -> TreePaint.dim();
				};
				if (overview && this.lw > 1) {
					g.fill(plateX, top, plateX + 18, top + 18, Colors.scaleAlpha(selected ? TreePaint.text() : mark, a));
				} else {
					int ring = f.state == TreeFigures.State.READY ? (van ? -1 : p.accent()) : (tn.given ? TreePaint.muted() : 0);
					TreePaint.plate(g, plateX, top, hov, done && !f.unasked(), ring, tn.given, a);
					if (overview) {
						g.fill(plateX + 5, top + 5, plateX + 18 - 5, top + 18 - 5, Colors.scaleAlpha(mark, a));
					} else {
						if (f.state == TreeFigures.State.READY && !van && t.motion() > 0.0F) {
							float breath = 0.5F + 0.5F * Mth.sin((float)Clock.nowMs() / 420.0F + (float)node.row.index);
							CFHGuiDraw.outline(g, plateX - 1, top - 1, 20, 20, Colors.scaleAlpha(Colors.withAlpha(p.accent(), Math.round(24.0F + 52.0F * breath)), a), 3);
						}

						String label = label(node, f, s.looks());
						if (label != null) {
							int color = switch (f.state) {
								case READY -> TreePaint.text();
								case COVERED -> TreePaint.dim();
								case MISSING, GIVEN -> TreePaint.warning();
								default -> f.have() > 0L ? TreePaint.text() : TreePaint.muted();
							};
							TreePaint.text(g, label, cx - CFHGuiDraw.width(label) / 2, top + 18 + 2, Colors.scaleAlpha(color, a));
						}

						if (node.row.hasChildren) {
							int ky = top + 29 + 3;
							boolean hot = this.hovered == node && this.zone == TreeGraph.Zone.KNOB;
							if (node.row.folded) {
								String text = "+" + node.foldedSteps;
								int kw = this.knobWidth(node);
								CFHGuiDraw.pixelRound(
									g, cx - kw / 2, ky - 4, kw, 10, Colors.scaleAlpha(van ? (hot ? -9803158 : -11908534) : (hot ? p.surfaceActive() : p.surface()), a), van ? 0 : 2
								);
								CFHGuiDraw.outline(g, cx - kw / 2, ky - 4, kw, 10, Colors.scaleAlpha(hot ? TreePaint.accent() : (van ? -16777216 : p.borderStrong()), a), van ? 0 : 2);
								TreePaint.text(g, text, cx - CFHGuiDraw.width(text) / 2 + 1, ky - 3, Colors.scaleAlpha(hot ? TreePaint.text() : TreePaint.muted(), a));
							} else if (hov > 0.3F && !s.foldMode) {
								CFHGuiDraw.pixelCircle(g, cx, ky, 4, Colors.scaleAlpha(van ? -11908534 : (hot ? p.surfaceActive() : p.surface()), a * hov));
								TreePaint.pixelsCentered(g, TreePaint.MINUS, cx + 1, ky + 1, Colors.scaleAlpha(hot ? TreePaint.accent() : TreePaint.muted(), a * hov));
							}
						}
					}
				}
			}
		}
	}

	private void capsule(GuiGraphics g, TreeSession s, TreeGraph.Node node, boolean overview) {
		Theme t = Theme.current();
		Palette p = t.palette();
		boolean van = t.isVanilla();
		float a = node.alpha;
		float hov = node.hover.get();
		TreeFigures.Fold fold = s.fold(node.row);
		int left = Math.round(node.cx - (float)node.w / 2.0F);
		int top = Math.round(node.cy);
		boolean complete = fold.complete();
		int edge = complete ? TreePaint.success() : (fold.ready ? TreePaint.accent() : (van ? -3750202 : Colors.lerp(p.textDim(), p.textMuted(), hov)));
		int fill = van ? Colors.lerp(-1071636448, -802148304, hov) : Colors.lerp(p.surface(), p.surfaceHover(), hov);
		if (overview && this.lw > 1) {
			g.fill(left, top, left + node.w, top + node.h, Colors.scaleAlpha(Colors.mix(fill, edge, 0.5F), a));
		} else {
			CFHGuiDraw.pixelRound(g, left, top, node.w, node.h, Colors.scaleAlpha(fill, a), van ? 0 : 3);
			float walk = fold.ready && t.motion() > 0.0F ? (float)Clock.nowMs() / 110.0F : 0.0F;
			TreePaint.dashes(g, left, top, node.w, node.h, van ? 0 : 3, 3, 2, walk, Colors.scaleAlpha(edge, a));
			TreePaint.pixelsCentered(g, TreePaint.DIAMOND, left + 8, top + 9, Colors.scaleAlpha(edge, a));
			if (!overview) {
				String steps = String.valueOf(fold.steps);
				TreePaint.text(g, steps, left + 14, top + 5, Colors.scaleAlpha(van ? -1 : Colors.lerp(p.textMuted(), p.text(), hov), a));
				if (fold.steps > 0 && fold.done > 0 && !complete) {
					TreePaint.bar(
						g,
						left + 4,
						top + node.h - 2,
						node.w - 8,
						1,
						fold.fraction(),
						Colors.scaleAlpha(van ? 1610612736 : Colors.withAlpha(p.borderStrong(), 144), a),
						Colors.scaleAlpha(TreePaint.accent(), a)
					);
				}

				if (node.h > 18) {
					int rx = left + 6;
					int ry = top + 18 - 1;
					int shown = 0;

					for (TreeFigures.Raw raw : fold.raws) {
						if (shown++ >= 3) {
							break;
						}

						String text = rawText(raw);
						TreePaint.text(g, text, rx + 9, ry, Colors.scaleAlpha(raw.missing > 0L ? TreePaint.warning() : TreePaint.success(), a));
						rx += 9 + CFHGuiDraw.width(text) + 5;
					}
				}
			}
		}
	}

	private void items(GuiGraphics g, TreeSession s, TreeGraph.Node node, float delta) {
		if (!(node.alpha <= 0.02F)) {
			try {
				if (node.row.node != null) {
					EmiIngredient shown = node.row.node.ingredient();
					int plateX = Math.round(node.cx) + this.shakeOf(node) - 9;
					int top = Math.round(node.cy);
					if (shown != null) {
						shown.render(g, plateX + 1, top + 1, delta, -3);
					}

					EmiRecipe recipe = node.row.node.recipe();
					TreeFigures f = s.figures(node.row.node.path);
					if (recipe != null
						&& node.row.node.step()
						&& recipe.getCategory() != VanillaEmiRecipeCategories.CRAFTING
						&& f.state != TreeFigures.State.DONE
						&& f.state != TreeFigures.State.COVERED
						&& TreeConfig.get().workstations) {
						RenderSystem.enableBlend();
						g.setColor(1.0F, 1.0F, 1.0F, 0.8F * node.alpha);
						g.pose().pushPose();
						g.pose().translate((float)(plateX - 5), (float)(top - 5), 0.0F);
						g.pose().scale(0.5F, 0.5F, 1.0F);
						recipe.getCategory().renderSimplified(g, 0, 0, delta);
						g.pose().popPose();
						g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
					}

					return;
				}

				TreeFigures.Fold fold = s.fold(node.row);
				int left = Math.round(node.cx - (float)node.w / 2.0F);
				int topx = Math.round(node.cy);
				int mx = left + 14 + CFHGuiDraw.width(String.valueOf(fold.steps)) + 4;
				int count = 0;

				for (TreeNode hidden : fold.first) {
					if (count++ >= 3 || hidden.ingredient() == null) {
						break;
					}

					small(g, hidden.ingredient(), mx, topx + 5, delta);
					mx += 9;
				}

				if (node.h > 18) {
					int rx = left + 6;
					int ry = topx + 18 - 1;
					int shownx = 0;

					for (TreeFigures.Raw raw : fold.raws) {
						if (shownx++ >= 3) {
							break;
						}

						small(g, raw.ingredient, rx, ry, delta);
						rx += 9 + CFHGuiDraw.width(rawText(raw)) + 5;
					}
				}
			} catch (RuntimeException var15) {
				if (!complained) {
					complained = true;
					CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] a stack of the graph could not be drawn (said once)", var15);
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

	private void over(GuiGraphics g, TreeSession s, TreeGraph.Node node, boolean overview) {
		int shake = this.shakeOf(node);
		int cx = Math.round(node.cx) + shake;
		int top = Math.round(node.cy);
		int plateX = cx - 9;
		if (node.alpha < 0.995F) {
			int veil = Colors.withAlpha(TreePaint.ground(), Math.round(255.0F * (1.0F - node.alpha)));
			if (node.row.node != null) {
				g.fill(plateX, top, plateX + 18, top + 18, veil);
			} else {
				g.fill(Math.round(node.cx - (float)node.w / 2.0F), top, Math.round(node.cx + (float)node.w / 2.0F), top + node.h, veil);
			}
		}

		if (!(node.alpha <= 0.3F) && !overview) {
			if (node.row.node != null) {
				TreeFigures f = s.figures(node.row.node.path);
				if (f.state == TreeFigures.State.DONE || f.state == TreeFigures.State.COVERED) {
					boolean asked = f.state == TreeFigures.State.COVERED || !f.unasked();
					g.fill(
						plateX + 1,
						top + 1,
						plateX + 18 - 1,
						top + 18 - 1,
						Colors.withAlpha(TreePaint.ground(), Math.round((float)(TreePaint.vanilla() ? 88 : 106) * (asked ? 1.0F : 1.3F) * node.alpha))
					);
					if (asked) {
						TreePaint.badge(
							g, plateX + 18 - 3, top + 18 - 3, f.state == TreeFigures.State.COVERED ? TreePaint.muted() : TreePaint.success(), TreePaint.TICK_SMALL, node.alpha
						);
					}
				}

				Long lit = s.lit.get(node.row.node.path);
				if (lit != null && Theme.current().motion() > 0.0F) {
					float t = (float)(Clock.nowMs() - lit) / (float)Theme.current().ms(900);
					if (t < 1.0F) {
						float e = 1.0F - (1.0F - t) * (1.0F - t);
						int grow = Math.round(e * 7.0F);
						CFHGuiDraw.outline(
							g,
							plateX - grow,
							top - grow,
							18 + grow * 2,
							18 + grow * 2,
							Colors.withAlpha(TreePaint.accent(), Math.round(208.0F * (1.0F - t) * (1.0F - t) * node.alpha)),
							2 + grow / 2
						);
					}
				}
			}

			if (craftShown(s, node)) {
				int bx = node.row.node != null ? plateX + 18 - 6 : Math.round(node.cx + (float)node.w / 2.0F) - 6;
				int by = top - 5;
				hammer(g, bx, by, this.hovered == node && this.zone == TreeGraph.Zone.CRAFT ? 1.0F : 0.0F, node.alpha);
			}
		}
	}

	private static void hammer(GuiGraphics g, int x, int y, float hover, float alpha) {
		Theme t = Theme.current();
		Palette p = t.palette();
		if (t.isVanilla()) {
			CFHGuiDraw.vanillaButton(g, x, y, 12, 12, hover, true, alpha);
			Icons.draw(g, Icon.HAMMER, x + 2, y + 2, 8, Colors.scaleAlpha(-1, alpha));
		} else {
			int fill = Colors.lerp(p.accent(), p.accentHover(), hover);
			CFHGuiDraw.pixelRound(g, x, y, 12, 12, Colors.scaleAlpha(fill, alpha), 3);
			CFHGuiDraw.outline(g, x, y, 12, 12, Colors.scaleAlpha(Colors.brighten(fill, 0.18F), alpha), 3);
			Icons.draw(g, Icon.HAMMER, x + 2, y + 2, 8, Colors.scaleAlpha(p.accentText(), alpha));
		}
	}

	private void burst(GuiGraphics g, TreeSession s) {
		if (s.celebrated != 0L && !(Theme.current().motion() <= 0.0F) && !this.nodes.isEmpty()) {
			float t = (float)(Clock.nowMs() - s.celebrated) / (float)Theme.current().ms(1300);
			if (!(t < 0.0F) && !(t >= 1.0F)) {
				TreeGraph.Node goal = this.nodes.get(0);
				int cx = Math.round(goal.cx);
				int cy = Math.round(goal.cy) + 9;
				int[] colors = TreePaint.vanilla() ? new int[]{-171, -1} : new int[]{TreePaint.success(), TreePaint.accent()};

				for (int k = 0; k < 2; k++) {
					float tk = Mth.clamp((t - (float)k * 0.12F) / 0.55F, 0.0F, 1.0F);
					if (!(tk <= 0.0F) && !(tk >= 1.0F)) {
						int grow = Math.round((1.0F - (1.0F - tk) * (1.0F - tk)) * 18.0F);
						CFHGuiDraw.outline(
							g, cx - 9 - grow, cy - 9 - grow, 18 + grow * 2, 18 + grow * 2, Colors.withAlpha(colors[k], Math.round(224.0F * (1.0F - tk))), 2 + grow / 2
						);
					}
				}
			}
		}
	}

	private int controlsLeft() {
		return this.x + this.w - 6 - 39 - 2 - this.zoomLabelWidth();
	}

	private int zoomLabelWidth() {
		return CFHGuiDraw.width("250%") + 4;
	}

	private TreeGraph.Zone controlAt(double mx, double my) {
		int cy = this.y + this.h - 6 - 13;
		if (!(my < (double)cy) && !(my >= (double)(cy + 13))) {
			int cx = this.controlsLeft();
			if (mx >= (double)cx && mx < (double)(cx + 13)) {
				return TreeGraph.Zone.ZOOM_OUT;
			} else {
				cx += 13 + this.zoomLabelWidth();
				if (mx >= (double)cx && mx < (double)(cx + 13)) {
					return TreeGraph.Zone.ZOOM_IN;
				} else {
					cx += 15;
					return mx >= (double)cx && mx < (double)(cx + 13) ? TreeGraph.Zone.FIT : TreeGraph.Zone.NONE;
				}
			}
		} else {
			return TreeGraph.Zone.NONE;
		}
	}

	private void controls(GuiGraphics g, int mouseX, int mouseY) {
		Theme t = Theme.current();
		Palette p = t.palette();
		boolean van = t.isVanilla();
		int cy = this.y + this.h - 6 - 13;
		int cx = this.controlsLeft();
		int labelW = this.zoomLabelWidth();
		int total = 39 + labelW + 2;
		if (van) {
			g.fill(cx - 2, cy - 2, cx + total + 2, cy + 13 + 2, -1073741824);
		} else {
			CFHGuiDraw.pixelRound(g, cx - 2, cy - 2, total + 4, 17, Colors.withAlpha(p.bg(), 224), 3);
			CFHGuiDraw.outline(g, cx - 2, cy - 2, total + 4, 17, p.border(), 3);
		}

		control(g, cx, cy, Icon.MINUS, this.zone == TreeGraph.Zone.ZOOM_OUT, this.zoomTo > 0.031F);
		cx += 13;
		String pct = Math.round(this.zoomTo * 100.0F) + "%";
		g.drawString(CFHGuiDraw.font(), pct, cx + (labelW - CFHGuiDraw.width(pct)) / 2, cy + 3, TreePaint.muted(), van);
		cx += labelW;
		control(g, cx, cy, Icon.PLUS, this.zone == TreeGraph.Zone.ZOOM_IN, this.zoomTo < 2.499F);
		cx += 15;
		control(g, cx, cy, Icon.RESIZE, this.zone == TreeGraph.Zone.FIT, true);
	}

	private static void control(GuiGraphics g, int x, int y, Icon icon, boolean hot, boolean active) {
		Theme t = Theme.current();
		Palette p = t.palette();
		if (hot && active) {
			CFHGuiDraw.pixelRound(g, x, y, 13, 13, t.isVanilla() ? 1090519039 : p.surfaceHover(), 2);
		}

		int color = !active ? TreePaint.dim() : (hot ? TreePaint.text() : TreePaint.muted());
		Icons.draw(g, icon, x + 2, y + 2, 8, color);
	}

	public EmiStackInteraction hoveredStack() {
		if (this.hovered != null && this.hovered.row.node != null && this.session != null && !this.session.foldMode && this.zone == TreeGraph.Zone.BODY) {
			EmiIngredient shown = this.hovered.row.node.ingredient();
			return shown == null ? EmiStackInteraction.EMPTY : new EmiStackInteraction(shown, this.hovered.row.node.recipe(), false);
		} else {
			return EmiStackInteraction.EMPTY;
		}
	}

	@Nullable
	public List<ClientTooltipComponent> tooltip() {
		TreeSession s = this.session;
		if (s != null && !this.panning) {
			if (this.refusedWhy != null && Clock.nowMs() - this.refusedAt < 2600L) {
				return List.of(
					line(
						Component.translatable("cfhgui_crafting.tree.refused." + this.refusedWhy.name().toLowerCase(Locale.ROOT))
							.withStyle(st -> st.withColor(TreePaint.danger() & 16777215))
					)
				);
			} else {
				switch (this.zone) {
					case ISLAND_GROUND:
						if (this.hoveredGround == null) {
							return null;
						}

						long hiddenSteps = TreeRows.hidden(s.outline(), this.hoveredGround).stream().filter(TreeNode::step).count();
						return List.of(line(TreeTips.counted("cfhgui_crafting.tree.island.fold", hiddenSteps, hiddenSteps)), dim("cfhgui_crafting.planner.graph.ground.keys"));
					case ZOOM_OUT:
						return List.of(line(Component.translatable("cfhgui_crafting.planner.graph.zoom_out")));
					case ZOOM_IN:
						return List.of(line(Component.translatable("cfhgui_crafting.planner.graph.zoom_in")));
					case FIT:
						return List.of(line(Component.translatable("cfhgui_crafting.planner.graph.fit")), dim("cfhgui_crafting.planner.graph.fit.tip"));
					default:
						if (this.hovered == null) {
							return null;
						} else if (this.zone == TreeGraph.Zone.CRAFT) {
							TreeNode node = this.hovered.island() ? s.fold(this.hovered.row).next : this.hovered.row.node;
							if (node == null) {
								return null;
							} else {
								long crafts = s.figures(node.path).crafts;
								List<ClientTooltipComponent> out = new ArrayList<>();
								out.add(line(Component.translatable("cfhgui_crafting.tree.craft", new Object[]{TreeActions.name(node.ingredient())})));
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
						} else if (this.zone == TreeGraph.Zone.KNOB) {
							return this.hovered.row.folded
								? List.of(line(TreeTips.counted("cfhgui_crafting.planner.graph.unfold", (long)this.hovered.foldedSteps, this.hovered.foldedSteps)))
								: List.of(line(Component.translatable("cfhgui_crafting.planner.graph.fold")));
						} else {
							return this.hovered.island() ? TreeTips.island(s, this.hovered.row) : TreeTips.node(s, this.hovered.row.node);
						}
				}
			}
		} else {
			return null;
		}
	}

	private static ClientTooltipComponent line(Component text) {
		return ClientTooltipComponent.create(text.getVisualOrderText());
	}

	private static ClientTooltipComponent dim(String key) {
		return line(Component.translatable(key).withStyle(st -> st.withColor(TreePaint.dim() & 16777215)));
	}

	public boolean mouseClicked(double mx, double my, int button) {
		TreeSession s = this.session;
		if (s == null || !this.contains(mx, my)) {
			return false;
		} else if (button != 0 && button != 1) {
			return false;
		} else {
			this.hover(s, (int)mx, (int)my);
			if (button != 1) {
				this.pressed = true;
				this.panning = false;
				this.pressButton = button;
				this.pressX = mx;
				this.pressY = my;
				this.pressedNode = this.hovered;
				this.pressedGround = this.hoveredGround;
				this.pressedZone = this.zone;
				return true;
			} else {
				if (this.hovered != null && this.hovered.island()) {
					TreeMenus.island(s, this.hovered.row.island, mx, my);
				} else if (this.hovered != null) {
					s.selected = this.hovered.row.node.path;
					TreeMenus.row(s, this.hovered.row.node, mx, my);
				} else if (this.hoveredGround != null) {
					TreeMenus.island(s, this.hoveredGround, mx, my);
				}

				return true;
			}
		}
	}

	public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
		if (this.pressed && button == this.pressButton) {
			if (!this.panning && (Math.abs(mx - this.pressX) > 3.0 || Math.abs(my - this.pressY) > 3.0)) {
				this.panning = true;
				this.hold();
				if (this.hovered != null) {
					this.hovered.hover.set(0.0F);
				}
			}

			if (this.panning) {
				this.camX = this.camX - (float)dx / this.zoom;
				this.camY = this.camY - (float)dy / this.zoom;
				this.camToX = this.camX;
				this.camToY = this.camY;
				this.clampCamera();
			}

			return true;
		} else {
			return false;
		}
	}

	private void clampCamera() {
		float keep = 40.0F / this.zoom;
		this.camX = Mth.clamp(this.camX, this.minX - (float)this.w / this.zoom + keep, this.maxX - keep);
		this.camY = Mth.clamp(this.camY, (float)(-this.h) / this.zoom + keep - 12.0F, (float)this.maxY - keep);
		this.camToX = this.camX;
		this.camToY = this.camY;
	}

	public boolean mouseReleased(double mx, double my, int button) {
		if (this.pressed && button == this.pressButton) {
			this.pressed = false;
			boolean wasPanning = this.panning;
			this.panning = false;
			TreeSession s = this.session;
			if (!wasPanning && s != null) {
				this.hover(s, (int)mx, (int)my);
				if (this.zone == this.pressedZone && this.hovered == this.pressedNode && this.hoveredGround == this.pressedGround) {
					switch (this.zone) {
						case ISLAND_GROUND:
							if (this.hoveredGround != null) {
								this.anchorId = this.hoveredGround.top();
								s.open(this.hoveredGround, false);
								CFHGuiSounds.tick();
							}

							return true;
						case ZOOM_OUT:
							this.zoomStep(-1, (float)this.x + (float)this.w / 2.0F, (float)this.y + (float)this.h / 2.0F);
							CFHGuiSounds.tick();
							return true;
						case ZOOM_IN:
							this.zoomStep(1, (float)this.x + (float)this.w / 2.0F, (float)this.y + (float)this.h / 2.0F);
							CFHGuiSounds.tick();
							return true;
						case FIT:
							this.fit();
							CFHGuiSounds.tick();
							return true;
						default:
							TreeGraph.Node node = this.hovered;
							if (node == null) {
								return true;
							} else if (node.island()) {
								if (this.zone == TreeGraph.Zone.CRAFT) {
									TreeNode next = s.fold(node.row).next;
									if (next != null) {
										TreeActions.craft(s, next, EmiInput.isShiftDown());
									}
								} else {
									this.anchorId = node.row.island.top();
									s.open(node.row.island, true);
									CFHGuiSounds.tick();
								}

								return true;
							} else {
								TreeNode tn = node.row.node;
								if (this.zone == TreeGraph.Zone.CRAFT) {
									TreeActions.craft(s, tn, EmiInput.isShiftDown());
									return true;
								} else if (s.foldMode) {
									this.pick(s, node);
									return true;
								} else if (this.zone == TreeGraph.Zone.KNOB) {
									this.anchorId = tn.path;
									s.fold(tn.path, !node.row.folded);
									CFHGuiSounds.tick();
									return true;
								} else {
									long now = Clock.nowMs();
									boolean again = tn.path.equals(this.lastClicked) && now - this.lastClickAt < 320L;
									this.lastClicked = tn.path;
									this.lastClickAt = now;
									if (again && TreeActions.craft(s, tn, EmiInput.isShiftDown())) {
										return true;
									} else {
										if (!tn.path.equals(s.selected)) {
											CFHGuiSounds.tick();
										}

										s.selected = tn.path;
										this.anchorId = tn.path;
										this.host.chosen(s, tn);
										return true;
									}
								}
							}
					}
				} else {
					return true;
				}
			} else {
				return true;
			}
		} else {
			return false;
		}
	}

	private void pick(TreeSession s, TreeGraph.Node node) {
		String path = node.row.node.path;
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
					this.anchorId = there.top();
					s.open(there, false);
				}

				s.firstPoint = null;
				CFHGuiSounds.tick();
			} else if (refusal != null) {
				this.refusedId = node.row.id;
				this.refusedWhy = refusal;
				this.refusedAt = Clock.nowMs();
			} else {
				this.anchorId = TreePaths.common(s.firstPoint, path);
				s.firstPoint = null;
				CFHGuiSounds.click();
			}

			this.preview = null;
			this.previewFor = null;
		}
	}

	public boolean mouseScrolled(double mx, double my, double amount) {
		if (this.session != null && this.contains(mx, my) && amount != 0.0) {
			this.zoomStep(amount > 0.0 ? 1 : -1, (float)mx, (float)my);
			return true;
		} else {
			return false;
		}
	}

	public boolean keyPressed(int key, int scan, int modifiers) {
		TreeSession s = this.session;
		if (s == null || this.nodes.isEmpty()) {
			return false;
		} else if (key == 70 && modifiers == 0) {
			TreeView.foldMode(s, !s.foldMode);
			return true;
		} else if (key == 256) {
			if (s.firstPoint != null) {
				s.firstPoint = null;
				return true;
			} else if (s.foldMode) {
				TreeView.foldMode(s, false);
				return true;
			} else {
				return false;
			}
		} else if (key == 268) {
			this.fit();
			return true;
		} else if (key == 61 || key == 334) {
			this.zoomStep(1, (float)this.x + (float)this.w / 2.0F, (float)this.y + (float)this.h / 2.0F);
			return true;
		} else if (key != 45 && key != 333) {
			TreeGraph.Node at = s.selected == null ? null : this.byId.get(s.selected);
			if (at != null) {
				switch (key) {
					case 257:
					case 335:
						if (!TreeActions.craft(s, at.row.node, (modifiers & 1) != 0)) {
							this.host.chosen(s, at.row.node);
						}

						return true;
					case 262:
					case 263:
						int i = key == 263 ? at.row.prevSibling : at.row.nextSibling;

						while (i >= 0 && this.nodes.get(i).row.node == null) {
							i = key == 263 ? this.nodes.get(i).row.prevSibling : this.nodes.get(i).row.nextSibling;
						}

						if (i >= 0) {
							this.select(s, this.nodes.get(i));
						}

						return true;
					case 264:
						int c = at.row.index + 1;
						if (c < this.nodes.size() && this.nodes.get(c).row.parent == at.row.index) {
							TreeGraph.Node child = this.nodes.get(c);
							if (child.island()) {
								this.anchorId = child.row.island.top();
								s.open(child.row.island, true);
								CFHGuiSounds.tick();
							} else {
								this.select(s, child);
							}
						} else if (at.row.folded) {
							this.anchorId = at.row.node.path;
							s.fold(at.row.node.path, false);
							CFHGuiSounds.tick();
						}

						return true;
					case 265:
						for (int p = at.row.parent; p >= 0; p = this.nodes.get(p).row.parent) {
							if (this.nodes.get(p).row.node != null) {
								this.select(s, this.nodes.get(p));
								break;
							}
						}

						return true;
					default:
						return false;
				}
			} else if (key >= 262 && key <= 265) {
				this.select(s, this.nodes.get(0));
				return true;
			} else {
				return false;
			}
		} else {
			this.zoomStep(-1, (float)this.x + (float)this.w / 2.0F, (float)this.y + (float)this.h / 2.0F);
			return true;
		}
	}

	private void select(TreeSession s, TreeGraph.Node node) {
		s.selected = node.row.node.path;
		this.anchorId = node.row.node.path;
		this.reveal(node);
		CFHGuiSounds.tick();
		this.host.chosen(s, node.row.node);
	}

	private void reveal(TreeGraph.Node node) {
		float margin = 24.0F / this.zoom;
		float viewW = (float)this.w / this.zoom;
		float viewH = (float)this.h / this.zoom;
		float toX = this.camToX;
		float toY = this.camToY;
		float left = node.tx - (float)node.w / 2.0F;
		float right = left + (float)node.w;
		if (left < toX + margin) {
			toX = left - margin;
		} else if (right > toX + viewW - margin) {
			toX = right - viewW + margin;
		}

		if (node.ty < toY + margin) {
			toY = node.ty - margin;
		} else if (node.ty + (float)node.h > toY + viewH - margin) {
			toY = node.ty + (float)node.h - viewH + margin;
		}

		if (toX != this.camToX || toY != this.camToY) {
			this.move(toX, toY, this.zoomTo, 220, Clock.nowMs());
		}
	}

	public void reveal(String path) {
		TreeGraph.Node node = this.byId.get(path);
		if (node != null) {
			this.reveal(node);
		}
	}

	@Nullable
	public int[] placeOf(String id) {
		TreeGraph.Node node = this.byId.get(id);
		return node == null
			? null
			: new int[]{Math.round((float)this.x + (node.cx - this.camX) * this.zoom), Math.round((float)this.y + (node.cy + 9.0F - this.camY) * this.zoom)};
	}

	void zoomTo(float z) {
		this.zoomAt((float)this.x + (float)this.w / 2.0F, (float)this.y + (float)this.h / 2.0F, sharp(z));
	}

	void panBy(float dx, float dy) {
		this.hold();
		this.camX += dx;
		this.camY += dy;
		this.clampCamera();
	}

	void sweep(float dx) {
		this.hold();
		this.camX = this.camX + dx / this.zoom;
		if (this.camX > this.maxX) {
			this.camX = this.minX - (float)this.w / this.zoom;
		}

		this.camToX = this.camX;
	}

	float zoom() {
		return this.zoomTo;
	}

	public int nodeCount() {
		return this.nodes.size();
	}

	private static final class Node {
		final TreeRow row;
		int w;
		int h;
		float tx;
		float ty;
		float fx;
		float fy;
		float cx;
		float cy;
		float fromAlpha = 1.0F;
		float alpha = 1.0F;
		boolean entering;
		boolean leaving;
		final Anim hover = new Anim(0.0F, 110, Ease.OUT_CUBIC);
		@Nullable
		String label;
		int labelLooks = -1;
		int foldedSteps;

		Node(TreeRow row) {
			this.row = row;
		}

		boolean island() {
			return this.row.isIsland();
		}
	}

	private static enum Zone {
		NONE,
		BODY,
		CRAFT,
		KNOB,
		ISLAND_GROUND,
		ZOOM_OUT,
		ZOOM_IN,
		FIT;
	}
}
