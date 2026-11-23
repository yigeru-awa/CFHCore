package com.onehumanawa.cfhcore.content.gui.teams.client.overhaul;

import com.onehumanawa.cfhcore.content.gui.core.client.DevHarness;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.stage.Stage;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageFinish;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageLighting;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.BlockNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.FxNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.MotesNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PivotNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PlayerNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.StageNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.TerrainNode;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsState;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.map.ClaimMap;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeammateState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel.ClientLevelData;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

final class LandTable {
	private static final float FOV = 38.0F;
	private static final float TABLE_TOP = 1.5F;
	private static final float WIDTH = 7.2F;
	private static final float CELL = 0.072F;
	private static final int ROOM = 18;
	private static final float PITCH_MIN = 24.0F;
	private static final float PITCH_MAX = 72.0F;
	private static final float RISE_AT = 380.0F;
	private static final float RISE_MS = 950.0F;
	private static final float ARRIVE_MS = 2000.0F;
	private static final float FIGURE = 2.0F;
	private static final int AIR = -16119801;
	private static boolean seen;
	final Stage stage;
	final LandModel model = new LandModel();
	private final LandSlabs slabs;
	private final PivotNode land = new PivotNode();
	private final PivotNode pieces = new PivotNode();
	private final PivotNode driver = new PivotNode();
	private final List<BlockNode> banners = new ArrayList<>();
	private String bannerLook = "";
	@Nullable
	private PlayerNode self;
	private final FxNode beam;
	private final FxNode ring;
	private final Map<UUID, PlayerNode> mates = new HashMap<>();
	private final Map<UUID, float[]> glide = new HashMap<>();
	@Nullable
	private BlockNode home;
	private String homeLook = "";
	private final List<LandTable.Mark> marks = new ArrayList<>();
	private float yaw;
	private float yawTo;
	private float pitch = 46.0F;
	private float pitchTo = 46.0F;
	private float zoom;
	private float zoomTo;
	private float focusX;
	private float focusZ;
	private float focusXTo;
	private float focusZTo;
	private float aspect = 1.6F;
	private float clock;
	private final float arriveMs;
	private final float riseAt;
	private final float riseMs;
	private final boolean full;
	private long lastTouchMs;
	private long ticks;
	private boolean pointing;
	private int pointCellX;
	private int pointCellZ;
	private float pointX;
	private float pointZ;
	private final Vector3f from = new Vector3f();
	private final Vector3f along = new Vector3f();
	private final Vector3f ndc = new Vector3f();
	private final Vector3f world = new Vector3f();

	LandTable(Screen owner) {
		Stage s = new Stage().bind(owner);
		this.stage = s;
		this.full = !seen || DevHarness.replayIntros();
		this.arriveMs = this.full ? 2000.0F : 800.0F;
		this.riseAt = this.full ? 380.0F : 60.0F;
		this.riseMs = this.full ? 950.0F : 420.0F;
		seen = true;
		s.soft(true);
		s.resolutionScale(Minecraft.getInstance().getWindow().getWidth() <= 2048 ? 2.0F : 1.5F);
		s.focusOutline(false);
		s.background(-16119801);
		s.finish(
			StageFinish.cinematic()
				.bloom(0.62F)
				.vignette(0.4F)
				.warmth(0.03F)
				.contrast(1.08F)
				.saturation(1.12F)
				.grain(1.1F)
				.shadows(0.004F, 0.002F, 0.012F)
				.highlights(0.03F, 0.014F, -0.012F)
		);
		StageLighting light = s.lighting();
		light.sun(0.25F, 1.0F, 0.35F)
			.ambient(0.5F)
			.key(1.8F, 9.5F, 2.6F)
			.keyColor(0.9F, 0.73F, 0.52F)
			.ambientColor(0.15F, 0.15F, 0.2F)
			.fill(-0.5F, 0.4F, -0.7F, 0.09F, 0.12F, 0.19F)
			.rim(1.0F, 0.8F, 0.56F, 0.16F)
			.wrap(0.5F);
		hall(s);
		table(s);

		for (int x : new int[]{-5, 5}) {
			for (int z : new int[]{-5, 5}) {
				light.lampFlicker(light.lamp((float)x, 2.5F, (float)z, 0.82F, 0.53F, 0.26F, 6.0F), 0.06F);
			}
		}

		light.lampFlicker(light.lamp(0.0F, 1.0F, -15.6F, 1.7F, 0.78F, 0.32F, 13.0F), 0.3F);
		((PivotNode)s.add(this.land)).at(0.0F, 1.51F, 0.0F).scale(0.072F, 0.0014399999F, 0.072F);
		((TerrainNode)s.add(this.model.node())).attachTo(this.land);
		this.slabs = (LandSlabs)s.add(new LandSlabs(this.model));
		this.slabs.attachTo(this.land);
		((PivotNode)s.add(this.pieces)).at(0.0F, 1.51F, 0.0F).scale(0.072F);
		this.ring = (FxNode)s.add(FxNode.glow(2.8F, 1962930898).fade(1.5F));
		this.ring.attachTo(this.pieces);
		this.beam = (FxNode)s.add(FxNode.cone(0.2F, 1.25F, 15.0F, 1090512568));
		this.beam.attachTo(this.pieces);
		((MotesNode)s.add(new MotesNode(46, 10.0F, 4.5F, 10.0F, 0.022F, 1493164724, 5L).sway(1.5F))).at(0.0F, 4.1F, 0.0F);
		s.add(new LandTable.GameTime());
		s.add(this.driver);
		this.driver.onFrame(this::frame);
		this.model.read();
		this.camera(0.0F, false);
	}

	private static void hall(Stage s) {
		int r = 18;
		Piece p = new Piece(s, "hall", -18, -1, -18, 18, 5, 18);

		for (int x = -18; x <= 18; x++) {
			for (int z = -18; z <= 18; z++) {
				p.put(x, -1, z, Math.floorMod(z, 4) == 0 ? "dark_oak_planks" : "spruce_planks");
			}
		}

		for (int i = -18; i <= 18; i++) {
			boolean post = Math.floorMod(i, 6) == 0;

			for (int y = 0; y <= 4; y++) {
				String block = post ? "dark_oak_log[axis=y]" : (y == 0 ? "stone_bricks" : (y == 4 ? "dark_oak_planks" : "spruce_planks"));
				p.put(i, y, -18, block).put(i, y, 18, block).put(-18, y, i, block).put(18, y, i, block);
			}

			if (post && Math.abs(i) < 18) {
				p.put(i, 2, -17, "wall_torch[facing=south]").put(i, 2, 17, "wall_torch[facing=north]");
				p.put(-17, 2, i, "wall_torch[facing=east]").put(17, 2, i, "wall_torch[facing=west]");
			}
		}

		p.fill(-2, 0, -17, 2, 4, -17, "bricks");
		p.fill(-1, 0, -17, 1, 1, -17, "air");
		p.fill(-1, 0, -18, 1, 1, -18, "nether_bricks");
		p.put(0, 0, -17, "campfire[lit=true,signal_fire=false,facing=south]");

		for (int z = -5; z <= 5; z++) {
			if (z != 0) {
				p.fill(17, 0, z, 17, 2, z, "bookshelf");
			}
		}

		p.put(16, 0, 0, "lectern[facing=west,has_book=false]");

		for (int zx = -7; zx <= -3; zx++) {
			p.put(-17, 0, zx, zx % 2 == 0 ? "barrel[facing=up]" : "barrel[facing=east]");
		}

		p.put(-17, 1, -6, "barrel[facing=up]").put(-17, 1, -5, "hay_block[axis=z]");
		p.put(-17, 0, 2, "chest[facing=east]").put(-17, 0, 4, "chest[facing=east]");
		p.put(0, 0, 18, "spruce_door[facing=north,half=lower,hinge=left,open=false]").put(0, 1, 18, "spruce_door[facing=north,half=upper,hinge=left,open=false]");
		p.put(3, 0, 17, "crafting_table").put(4, 0, 17, "cartography_table").put(5, 0, 17, "fletching_table");
		p.put(-3, 0, 17, "smithing_table").put(-4, 0, 17, "barrel[facing=up]");
		p.done(s);
	}

	private static void table(Stage s) {
		Piece p = new Piece(s, "table", -7, 0, -7, 7, 2, 7);

		for (int x = -7; x <= 7; x++) {
			for (int z = -7; z <= 7; z++) {
				p.put(x, 0, z, Math.max(Math.abs(x), Math.abs(z)) == 7 ? "black_carpet" : "brown_carpet");
			}
		}

		p.fill(-4, 1, -4, 4, 1, 4, "dark_oak_slab[type=bottom]");

		for (int x : new int[]{-4, 4}) {
			for (int z : new int[]{-4, 4}) {
				p.put(x, 0, z, "dark_oak_log[axis=y]");
			}
		}

		p.put(0, 0, 0, "dark_oak_log[axis=y]");

		for (int x : new int[]{-5, 5}) {
			for (int z : new int[]{-5, 5}) {
				p.put(x, 0, z, "dark_oak_fence").put(x, 1, z, "dark_oak_fence").put(x, 2, z, "lantern[hanging=false]");
			}
		}

		p.done(s);
	}

	void dress(@Nullable TeamData team) {
		String look = team == null ? "" : (team.banner == null ? "-" : team.banner.signature());
		if (!look.equals(this.bannerLook)) {
			this.bannerLook = look;

			for (BlockNode b : this.banners) {
				this.stage.remove(b);
			}

			this.banners.clear();
			if (team != null) {
				for (int x : new int[]{-1, 1}) {
					BlockNode north = (BlockNode)this.stage.add(BannerNodes.hanging(this.stage.level(), team.banner));
					north.at((float)x * 4.0F, 3.0F, -16.86F).scale(1.6F);
					BlockNode south = (BlockNode)this.stage.add(BannerNodes.hanging(this.stage.level(), team.banner));
					south.at((float)x * 4.0F, 3.0F, 16.86F).rotate(180.0F, 0.0F, 0.0F).scale(1.6F);
					north.tint(-4944280);
					south.tint(-10594738);
					this.banners.add(north);
					this.banners.add(south);

					for (int z : new int[]{-1, 1}) {
						BlockNode corner = (BlockNode)this.stage.add(BannerNodes.standing(this.stage.level(), team.banner));
						corner.at((float)x * 7.0F, 0.06F, (float)z * 7.0F).scale(1.5F);
						corner.tint(-3102590);
						this.banners.add(corner);
					}
				}
			}
		}

		this.slabs.stale();
	}

	LandSlabs slabs() {
		return this.slabs;
	}

	void shape(Rect view) {
		this.aspect = Math.max(0.5F, (float)view.w() / (float)Math.max(1, view.h()));
	}

	boolean pointing() {
		return this.pointing;
	}

	int pointedChunkX() {
		return this.model.originX() + this.pointCellX / 4;
	}

	int pointedChunkZ() {
		return this.model.originZ() + this.pointCellZ / 4;
	}

	boolean risen() {
		return this.clock >= this.riseAt + this.riseMs * 0.7F;
	}

	List<LandTable.Mark> marks() {
		return this.marks;
	}

	boolean screen(float x, float y, float z, Rect view, int[] out) {
		this.world.set(x, y, z);
		this.pieces.model.transformPosition(this.world);
		if (!this.stage.camera().project(this.world.x, this.world.y, this.world.z, this.ndc)) {
			return false;
		} else {
			out[0] = view.x() + Math.round((this.ndc.x * 0.5F + 0.5F) * (float)view.w());
			out[1] = view.y() + Math.round((0.5F - this.ndc.y * 0.5F) * (float)view.h());
			return true;
		}
	}

	void touch() {
		this.lastTouchMs = System.currentTimeMillis();
	}

	void turn(float degrees, float tip) {
		this.yawTo += degrees;
		this.pitchTo = Mth.clamp(this.pitchTo + tip, 24.0F, 72.0F);
		this.touch();
	}

	void zoom(int steps) {
		float to = Mth.clamp(this.zoomTo + (float)steps * 0.2F, 0.0F, 1.0F);
		if (to > this.zoomTo && this.pointing) {
			this.focusXTo = this.pointX;
			this.focusZTo = this.pointZ;
		}

		this.zoomTo = to;
		this.touch();
	}

	boolean canZoom(int direction) {
		return direction > 0 ? this.zoomTo < 0.999F : this.zoomTo > 0.001F;
	}

	void centre() {
		Vec3 me = TeamsView.position();
		this.focusXTo = me == null ? 0.0F : Mth.clamp(this.model.localX(me.x) * 0.072F, -3.6F, 3.6F);
		this.focusZTo = me == null ? 0.0F : Mth.clamp(this.model.localZ(me.z) * 0.072F, -3.6F, 3.6F);
		if (this.zoomTo < 0.5F) {
			this.zoomTo = 0.5F;
		}

		this.touch();
	}

	void lookAt(int chunkX, int chunkZ) {
		this.focusXTo = Mth.clamp(this.model.localX((double)(chunkX * 16 + 8)) * 0.072F, -3.6F, 3.6F);
		this.focusZTo = Mth.clamp(this.model.localZ((double)(chunkZ * 16 + 8)) * 0.072F, -3.6F, 3.6F);
		this.touch();
	}

	int[] step(int right, int up) {
		float r = (float)Math.toRadians((double)this.yawTo);
		float sin = Mth.sin(r);
		float cos = Mth.cos(r);
		int rx = Math.abs(cos) >= Math.abs(sin) ? (cos >= 0.0F ? 1 : -1) : 0;
		int rz = rx != 0 ? 0 : (sin >= 0.0F ? -1 : 1);
		int ux = Math.abs(sin) > Math.abs(cos) ? (sin >= 0.0F ? -1 : 1) : 0;
		int uz = ux != 0 ? 0 : (cos >= 0.0F ? -1 : 1);
		return new int[]{right * rx + up * ux, right * rz + up * uz};
	}

	void close() {
		this.stage.close();
	}

	private void frame(StageRenderContext ctx) {
		float motion = Theme.current().motion();
		boolean moving = motion > 0.0F;
		this.clock = moving ? this.clock + ctx.deltaMs / motion : 1.0E7F;
		this.model.read();
		float up = Ease.OUT_CUBIC.apply(Mth.clamp((this.clock - this.riseAt) / this.riseMs, 0.0F, 1.0F));
		this.land.scale(0.072F, 0.072F * Math.max(0.02F, up), 0.072F);
		this.slabs.shown(Mth.clamp((this.clock - this.riseAt - this.riseMs * 0.8F) / 520.0F, 0.0F, 1.0F));
		float stand = Ease.OUT_BACK.apply(Mth.clamp((this.clock - this.riseAt - this.riseMs) / 420.0F, 0.0F, 1.0F));
		this.pieces.visible(stand > 0.001F);
		this.pieces.scale(0.072F, 0.072F * Math.max(0.001F, stand), 0.072F);
		this.figures(ctx, moving);
		this.point(ctx);
		if (moving && !ctx.hasMouse && System.currentTimeMillis() - this.lastTouchMs > 2600L) {
			this.yawTo = this.yawTo + 1.5F * ctx.deltaMs / 1000.0F;
		}

		this.camera(ctx.deltaMs, moving);
		ClaimMap.used();
	}

	private void camera(float deltaMs, boolean moving) {
		float k = moving ? 1.0F - (float)Math.exp((double)(-deltaMs / 110.0F)) : 1.0F;
		this.yaw = this.yaw + (this.yawTo - this.yaw) * k;
		this.pitch = this.pitch + (this.pitchTo - this.pitch) * k;
		this.zoom = this.zoom + (this.zoomTo - this.zoom) * k;
		this.focusX = this.focusX + (this.focusXTo - this.focusX) * k;
		this.focusZ = this.focusZ + (this.focusZTo - this.focusZ) * k;
		float tan = (float)Math.tan(Math.toRadians(19.0));
		float far = Math.min(Math.max(5.328F / (tan * this.aspect), 4.4639997F / tan), 15.5F);
		float near = 3.4F;
		float z = this.zoom * this.zoom * (3.0F - 2.0F * this.zoom);
		float dist = Mth.lerp(z, far, 3.4F);
		float yawNow = this.yaw;
		float pitchNow = this.pitch;
		float away = 1.0F - Ease.OUT_CUBIC.apply(Mth.clamp(this.clock / this.arriveMs, 0.0F, 1.0F));
		if (away > 0.0F) {
			yawNow -= (this.full ? 48.0F : 14.0F) * away;
			pitchNow = Math.min(82.0F, pitchNow + (this.full ? 18.0F : 6.0F) * away);
			dist *= 1.0F + (this.full ? 0.4F : 0.12F) * away;
		}

		float yr = (float)Math.toRadians((double)yawNow);
		float pr = (float)Math.toRadians((double)pitchNow);
		float tx = this.focusX * z;
		float tz = this.focusZ * z;
		float ty = 1.95F;
		this.stage
			.camera()
			.at(tx + dist * Mth.sin(yr) * Mth.cos(pr), 1.95F + dist * Mth.sin(pr), tz + dist * Mth.cos(yr) * Mth.cos(pr))
			.lookAt(tx, 1.95F, tz)
			.fov(38.0F)
			.clip(0.1F, 140.0F);
		this.stage.fog(true, dist + 6.0F, dist + 27.0F, -16119801);
	}

	private void point(StageRenderContext ctx) {
		this.pointing = false;
		if (ctx.hasMouse && this.risen()) {
			this.from.set(ctx.rayOrigin);
			this.land.modelInverse.transformPosition(this.from);
			this.along.set(ctx.rayDir);
			this.land.modelInverse.transformDirection(this.along);
			float length = this.along.length();
			if (!(length < 1.0E-6F)) {
				this.along.div(length);
				float half = 50.0F;
				float roof = 56.0F;
				float t0 = 0.0F;
				float t1 = Float.MAX_VALUE;

				for (int axis = 0; axis < 3; axis++) {
					float at = axis == 0 ? this.from.x : (axis == 1 ? this.from.y : this.from.z);
					float by = axis == 0 ? this.along.x : (axis == 1 ? this.along.y : this.along.z);
					float lo = axis == 1 ? 0.0F : -50.0F;
					float hi = axis == 1 ? 56.0F : 50.0F;
					if (Math.abs(by) < 1.0E-6F) {
						if (at < lo || at > hi) {
							return;
						}
					} else {
						float a = (lo - at) / by;
						float b = (hi - at) / by;
						if (a > b) {
							float swap = a;
							a = b;
							b = swap;
						}

						t0 = Math.max(t0, a);
						t1 = Math.min(t1, b);
						if (t0 > t1) {
							return;
						}
					}
				}

				for (float t = t0; t <= t1; t += 0.2F) {
					float x = this.from.x + this.along.x * t;
					float y = this.from.y + this.along.y * t;
					float z = this.from.z + this.along.z * t;
					int cx = Mth.floor(x + 50.0F);
					int cz = Mth.floor(z + 50.0F);
					int reach = this.model.top(cx, cz);
					if (reach > 0 && !(y > (float)reach + 0.42F)) {
						this.pointing = true;
						this.pointCellX = cx;
						this.pointCellZ = cz;
						this.pointX = x * 0.072F;
						this.pointZ = z * 0.072F;
						return;
					}
				}
			}
		}
	}

	private boolean onLand(float x, float z) {
		float half = 49.5F;
		return Math.abs(x) <= 49.5F && Math.abs(z) <= 49.5F;
	}

	private void figures(StageRenderContext ctx, boolean moving) {
		this.marks.clear();
		Minecraft mc = Minecraft.getInstance();
		String dim = this.model.dimension();
		TeamData team = TeamsView.team();
		boolean hasHome = false;
		if (team != null && team.home != null && dim.equals(team.home.dimension)) {
			float x = this.model.localX(team.home.x);
			float z = this.model.localZ(team.home.z);
			if (this.onLand(x, z)) {
				hasHome = true;
				String look = team.banner == null ? "-" : team.banner.signature();
				if (this.home == null || !look.equals(this.homeLook)) {
					if (this.home != null) {
						this.stage.remove(this.home);
					}

					this.home = (BlockNode)this.stage.add(BannerNodes.standing(this.stage.level(), team.banner));
					this.home.attachTo(this.pieces);
					this.home.scale(1.8F);
					this.home.tint(-1782352);
					this.homeLook = look;
				}

				float foot = (float)this.model.topAt(team.home.x, team.home.z);
				this.home.at(x, foot, z);
				this.marks.add(new LandTable.Mark(x, foot, foot + 3.4F, z, TeamsText.t("map.home")));
			}
		}

		if (!hasHome && this.home != null) {
			this.stage.remove(this.home);
			this.home = null;
		}

		if (team != null) {
			for (TeamData.Waypoint wp : team.waypoints) {
				if (dim.equals(wp.dimension)) {
					float x = this.model.localX((double)wp.x + 0.5);
					float z = this.model.localZ((double)wp.z + 0.5);
					if (this.onLand(x, z)) {
						float foot = (float)this.model.topAt((double)wp.x + 0.5, (double)wp.z + 0.5);
						this.marks.add(new LandTable.Mark(x, foot, foot + 3.7F, z, Component.literal(wp.name)));
					}
				}
			}
		}

		float k = moving ? 1.0F - (float)Math.exp((double)(-ctx.deltaMs / 160.0F)) : 1.0F;
		List<UUID> there = new ArrayList<>();
		if (TeamsView.available() && team != null) {
			for (TeammateState mate : TeamsState.teammates()) {
				if (dim.equals(mate.dimension())) {
					float[] at = this.glide.computeIfAbsent(mate.id(), id -> new float[]{mate.x(), mate.z()});
					at[0] += (mate.x() - at[0]) * k;
					at[1] += (mate.z() - at[1]) * k;
					float x = this.model.localX((double)at[0]);
					float z = this.model.localZ((double)at[1]);
					if (this.onLand(x, z)) {
						there.add(mate.id());
						String name = TeamsView.nameOf(mate.id());
						PlayerNode figure = this.mates.get(mate.id());
						if (figure == null) {
							figure = this.figure(CampScene.body(mate.id(), name));
							this.mates.put(mate.id(), figure);
						}

						float foot = (float)this.model.topAt((double)at[0], (double)at[1]);
						figure.at(x, foot, z).yaw(-mate.yaw());
						figure.muted(mate.dead() ? 0.8F : 0.0F);
						this.marks
							.add(
								new LandTable.Mark(
									x,
									foot,
									foot + 3.8F,
									z,
									name.isEmpty()
										? TeamsText.t(mate.ally() ? "map.ally" : "map.teammate")
										: (mate.ally() ? TeamsText.t("map.ally.named", name) : Component.literal(name))
								)
							);
					}
				}
			}
		}

		Iterator<Entry<UUID, PlayerNode>> it = this.mates.entrySet().iterator();

		while (it.hasNext()) {
			Entry<UUID, PlayerNode> e = it.next();
			if (!there.contains(e.getKey())) {
				this.stage.remove((StageNode)e.getValue());
				this.glide.remove(e.getKey());
				it.remove();
			}
		}

		Vec3 me = TeamsView.position();
		float x = me == null ? 0.0F : this.model.localX(me.x);
		float z = me == null ? 0.0F : this.model.localZ(me.z);
		boolean here = me != null && this.onLand(x, z);
		if (here && this.self == null) {
			this.self = this.figure(PlayerNode.local());
		}

		if (this.self != null) {
			this.self.visible(here);
		}

		this.beam.visible(here);
		this.ring.visible(here);
		if (here && this.self != null) {
			float foot = (float)this.model.topAt(me.x, me.z);
			this.self.at(x, foot, z).yaw(mc.player == null ? 0.0F : -mc.player.getYRot());
			this.beam.at(x, foot, z);
			this.ring.at(x, foot + 0.42F + 0.12F, z);
			this.marks.add(new LandTable.Mark(x, foot, foot + 3.8F, z, TeamsText.t("map.you")));
		}
	}

	private PlayerNode figure(PlayerNode node) {
		node.lookAtCursor(false).breathe(false);
		node.pickable(false);
		node.hoverFeel(0.0F, 1.0F);
		((PlayerNode)this.stage.add(node)).attachTo(this.pieces);
		node.scale(2.0F);
		return node;
	}

	private final class GameTime extends PivotNode {
		public void tick(StageRenderContext ctx) {
			LandTable.this.ticks++;
			if (LandTable.this.stage.level().getLevelData() instanceof ClientLevelData data) {
				data.setGameTime(LandTable.this.ticks);
			}
		}
	}

	static record Mark(float x, float foot, float top, float z, Component name) {
	}
}
