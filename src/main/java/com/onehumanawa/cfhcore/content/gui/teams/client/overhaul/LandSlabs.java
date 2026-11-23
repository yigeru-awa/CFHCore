package com.onehumanawa.cfhcore.content.gui.teams.client.overhaul;

import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageResources;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.StageNode;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsState;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsDraw;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.map.ClaimMap;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamSummary;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

final class LandSlabs extends StageNode {
	static final float LIFT = 0.42F;
	private static final float SKIN = 0.16F;
	private static final float OVER = 0.12F;
	private static final float HAIR = 0.06F;
	private static final int CHUNKS = 25;
	private static final int PER = 4;
	private static final int CELLS = 100;
	private final LandModel model;
	@Nullable
	private SlabMesh claims;
	@Nullable
	private SlabMesh pins;
	@Nullable
	private SlabMesh marks;
	private boolean stale = true;
	private int laid = -1;
	private int resources = -1;
	private float shown = 1.0F;
	private final int[] owners = new int[625];
	private final boolean[] forced = new boolean[625];
	private final List<LandSlabs.Look> looks = new ArrayList<>();
	private final Map<UUID, Integer> lookIndex = new HashMap<>();
	private boolean marksStale = true;
	private boolean hovering;
	private boolean selecting;
	private boolean cursing;
	private int hoverX;
	private int hoverZ;
	private int fromX;
	private int fromZ;
	private int toX;
	private int toZ;
	private int cursorX;
	private int cursorZ;
	private int selectColor;
	@Nullable
	private ClaimMap.Gesture selectKind;

	LandSlabs(LandModel model) {
		this.model = model;
		this.pickable(false);
		this.hoverFeel(0.0F, 1.0F);
		this.named("claims");
		this.bounds(-50.0F, 0.0F, -50.0F, 50.0F, 50.0F, 50.0F);
	}

	void stale() {
		this.stale = true;
		this.marksStale = true;
	}

	void shown(float amount) {
		this.shown = Mth.clamp(amount, 0.0F, 1.0F);
	}

	void hover(boolean on, int chunkX, int chunkZ) {
		if (on != this.hovering || on && (chunkX != this.hoverX || chunkZ != this.hoverZ)) {
			this.hovering = on;
			this.hoverX = chunkX;
			this.hoverZ = chunkZ;
			this.marksStale = true;
		}
	}

	void select(@Nullable ClaimMap.Gesture kind, int ax, int az, int bx, int bz, int color) {
		if (kind != this.selectKind || kind != null && (ax != this.fromX || az != this.fromZ || bx != this.toX || bz != this.toZ || color != this.selectColor)) {
			this.selecting = kind != null;
			this.selectKind = kind;
			this.fromX = ax;
			this.fromZ = az;
			this.toX = bx;
			this.toZ = bz;
			this.selectColor = color;
			this.marksStale = true;
		}
	}

	void cursor(boolean on, int chunkX, int chunkZ) {
		if (on != this.cursing || on && (chunkX != this.cursorX || chunkZ != this.cursorZ)) {
			this.cursing = on;
			this.cursorX = chunkX;
			this.cursorZ = chunkZ;
			this.marksStale = true;
		}
	}

	private static float edge(int chunk) {
		return (float)(chunk * 4) - 50.0F;
	}

	private int look(UUID team) {
		Integer known = this.lookIndex.get(team);
		if (known != null) {
			return known;
		} else {
			TeamSummary summary = TeamsView.summary(team);
			TeamsView.Relation relation = TeamsView.relation(team);
			int rgb = summary == null ? 9079434 : summary.color & 16777215;
			int ink = TeamsDraw.ink(rgb);
			boolean own = relation == TeamsView.Relation.OWN;
			int edge = own
				? Colors.withAlpha(Colors.brighten(ink, 0.3F), 248)
				: (relation.ally() ? Colors.withAlpha(Colors.brighten(ink, 0.12F), 224) : Colors.withAlpha(ink, 180));
			float opacity = Math.min(0.9F, ClaimMap.claimOpacity(relation) * 1.25F + 0.12F);
			this.looks.add(new LandSlabs.Look(Colors.withAlpha(rgb, Math.round(255.0F * opacity)), edge, own ? Colors.withAlpha(Colors.brighten(ink, 0.15F), 140) : 0));
			this.lookIndex.put(team, this.looks.size() - 1);
			return this.looks.size() - 1;
		}
	}

	private static int regionOfCell(int[] region, int cellX, int cellZ) {
		return cellX >= 0 && cellZ >= 0 && cellX < 100 && cellZ < 100 ? region[cellZ / 4 * 25 + cellX / 4] : -1;
	}

	private void skin(SlabMesh.Builder b, int[] region, List<LandSlabs.Look> by, int[] fills, float lift, float rim) {
		float half = 50.0F;

		for (int cz = 0; cz < 100; cz++) {
			for (int cx = 0; cx < 100; cx++) {
				int o = regionOfCell(region, cx, cz);
				if (o >= 0) {
					LandSlabs.Look look = by.get(o);
					int fill = fills[cz / 4 * 25 + cx / 4];
					float x0 = (float)cx - 50.0F;
					float z0 = (float)cz - 50.0F;
					float x1 = x0 + 1.0F;
					float z1 = z0 + 1.0F;
					int ground = this.model.top(cx, cz);
					float y = (float)ground + lift;
					float px0 = x0;
					float px1 = x1;
					float pz0 = z0;
					float pz1 = z1;

					for (Direction d : Plane.HORIZONTAL) {
						int nx = cx + d.getStepX();
						int nz = cz + d.getStepZ();
						if (regionOfCell(region, nx, nz) == o) {
							int nTop = this.model.top(nx, nz);
							float by1 = nTop < ground ? 0.06F : (nTop > ground ? -0.06F : 0.0F);
							switch (d) {
								case EAST:
									px1 += by1;
									break;
								case WEST:
									px0 -= by1;
									break;
								case SOUTH:
									pz1 += by1;
									break;
								default:
									pz0 -= by1;
							}
						}
					}

					if (fill != 0) {
						b.top(px0, pz0, px1, pz1, y, fill);
					}

					for (Direction dx : Plane.HORIZONTAL) {
						int nx = cx + dx.getStepX();
						int nz = cz + dx.getStepZ();
						if (regionOfCell(region, nx, nz) == o) {
							float ny = (float)this.model.top(nx, nz) + lift;
							if (fill != 0 && ny < y) {
								b.side(dx, px0, pz0, px1, pz1, ny, y, fill);
							}
						} else {
							b.side(dx, x0, z0, x1, z1, (float)ground + 0.03F, y, look.edge());
							float lx0 = dx == Direction.EAST ? x1 - rim : x0;
							float lx1 = dx == Direction.WEST ? x0 + rim : x1;
							float lz0 = dx == Direction.SOUTH ? z1 - rim : z0;
							float lz1 = dx == Direction.NORTH ? z0 + rim : z1;
							b.top(lx0, lz0, lx1, lz1, y + 0.015F, look.edge());
							if (look.inner() != 0) {
								float ix0 = dx == Direction.EAST ? x1 - 3.0F * rim : (dx == Direction.WEST ? x0 + rim : x0);
								float ix1 = dx == Direction.WEST ? x0 + 3.0F * rim : (dx == Direction.EAST ? x1 - rim : x1);
								float iz0 = dx == Direction.SOUTH ? z1 - 3.0F * rim : (dx == Direction.NORTH ? z0 + rim : z0);
								float iz1 = dx == Direction.NORTH ? z0 + 3.0F * rim : (dx == Direction.SOUTH ? z1 - rim : z1);
								b.top(ix0, iz0, ix1, iz1, y + 0.012F, look.inner());
							}
						}
					}
				}
			}
		}
	}

	private void layClaims() {
		if (this.claims != null) {
			this.claims.close();
		}

		this.looks.clear();
		this.lookIndex.clear();
		SlabMesh.Builder b = SlabMesh.builder();
		Long2ObjectMap<TeamsState.Claim> all = TeamsView.available() ? TeamsState.claims(this.model.dimension()) : null;
		boolean any = false;

		for (int iz = 0; iz < 25; iz++) {
			for (int ix = 0; ix < 25; ix++) {
				TeamsState.Claim claim = all == null ? null : (TeamsState.Claim)all.get(ChunkPos.asLong(this.model.originX() + ix, this.model.originZ() + iz));
				int i = iz * 25 + ix;
				if (claim != null && claim.team() != null) {
					this.owners[i] = this.look(claim.team());
					this.forced[i] = claim.forced();
					any = true;
				} else {
					this.owners[i] = -1;
				}
			}
		}

		if (any) {
			int[] fills = new int[625];

			for (int i = 0; i < fills.length; i++) {
				fills[i] = this.owners[i] < 0 ? 0 : this.looks.get(this.owners[i]).fill();
			}

			this.skin(b, this.owners, this.looks, fills, 0.16F, 0.2F);

			for (int i = 0; i < 625; i++) {
				if (this.owners[i] >= 0 && this.forced[i]) {
					int ixx = i % 25;
					int iz = i / 25;
					float mx = edge(ixx) + 2.0F;
					float mz = edge(iz) + 2.0F;
					float y = (float)Math.max(
							Math.max(this.model.top(ixx * 4 + 1, iz * 4 + 1), this.model.top(ixx * 4 + 2, iz * 4 + 1)),
							Math.max(this.model.top(ixx * 4 + 1, iz * 4 + 2), this.model.top(ixx * 4 + 2, iz * 4 + 2))
						)
						+ 0.42F;
					b.box(mx - 0.7F, y, mz - 0.7F, mx + 0.7F, y + 0.55F, mz + 0.7F, -251658241, -1056968504);
				}
			}
		}

		this.claims = b.build();
	}

	private void layPins() {
		if (this.pins != null) {
			this.pins.close();
		}

		SlabMesh.Builder b = SlabMesh.builder();
		TeamData team = TeamsView.team();
		if (team != null) {
			float half = 49.4F;

			for (TeamData.Waypoint wp : team.waypoints) {
				if (this.model.dimension().equals(wp.dimension)) {
					float x = this.model.localX((double)wp.x + 0.5);
					float z = this.model.localZ((double)wp.z + 0.5);
					if (!(Math.abs(x) > 49.4F) && !(Math.abs(z) > 49.4F)) {
						float foot = (float)this.model.topAt((double)wp.x + 0.5, (double)wp.z + 0.5);
						float neck = foot + 2.7F;
						int ink = TeamsDraw.ink(wp.color);
						b.box(x - 0.13F, foot, z - 0.13F, x + 0.13F, neck, z + 0.13F, -15000801, -14013904);
						b.box(x - 0.5F, neck, z - 0.5F, x + 0.5F, neck + 1.0F, z + 0.5F, Colors.withAlpha(Colors.brighten(ink, 0.25F), 255), Colors.withAlpha(ink, 255));
					}
				}
			}
		}

		this.pins = b.build();
	}

	private boolean onModel(int ix, int iz) {
		return ix >= 0 && iz >= 0 && ix < 25 && iz < 25;
	}

	private void layMarks() {
		if (this.marks != null) {
			this.marks.close();
		}

		SlabMesh.Builder b = SlabMesh.builder();
		int ox = this.model.originX();
		int oz = this.model.originZ();
		if (this.selecting && this.selectKind != null) {
			String dim = this.model.dimension();
			int x0 = Math.min(this.fromX, this.toX);
			int x1 = Math.max(this.fromX, this.toX);
			int z0 = Math.min(this.fromZ, this.toZ);
			int z1 = Math.max(this.fromZ, this.toZ);
			int fill = Colors.withAlpha(this.selectColor, 168);
			int line = Colors.withAlpha(Colors.brighten(this.selectColor, 0.35F), 255);
			int[] region = new int[625];
			int[] fills = new int[625];
			Arrays.fill(region, -1);

			for (int z = Math.max(z0, oz); z <= Math.min(z1, oz + 25 - 1); z++) {
				for (int x = Math.max(x0, ox); x <= Math.min(x1, ox + 25 - 1); x++) {
					int i = (z - oz) * 25 + x - ox;
					region[i] = 0;
					fills[i] = ClaimMap.eligible(this.selectKind, TeamsState.claimAt(dim, x, z)) ? fill : 0;
				}
			}

			this.skin(b, region, List.of(new LandSlabs.Look(0, line, 0)), fills, 0.28F, 0.3F);
		} else if (this.hovering && this.onModel(this.hoverX - ox, this.hoverZ - oz)) {
			int ix = this.hoverX - ox;
			int iz = this.hoverZ - oz;
			float y = (float)this.model.chunkTop(ix, iz) + 0.42F + 0.4F;
			b.top(edge(ix), edge(iz), edge(ix) + 4.0F, edge(iz) + 4.0F, y, 822083583);
			b.frame(edge(ix), edge(iz), edge(ix) + 4.0F, edge(iz) + 4.0F, y + 0.02F, 0.28F, -385875969);
		}

		if (this.cursing && this.onModel(this.cursorX - ox, this.cursorZ - oz)) {
			int ix = this.cursorX - ox;
			int iz = this.cursorZ - oz;
			float y = (float)this.model.chunkTop(ix, iz) + 0.42F + 0.5F;
			int accent = Colors.withAlpha(Theme.current().accent(), 255);
			b.frame(edge(ix) - 0.15F, edge(iz) - 0.15F, edge(ix) + 4.0F + 0.15F, edge(iz) + 4.0F + 0.15F, y, 0.4F, accent);
		}

		this.marks = b.build();
	}

	protected void draw(StageRenderContext ctx) {
	}

	public boolean hasTranslucentPass() {
		return true;
	}

	protected void drawTranslucent(StageRenderContext ctx) {
		int generation = StageResources.generation();
		if (generation != this.resources) {
			this.resources = generation;
			this.stale = true;
		}

		if (this.stale || this.laid != this.model.version()) {
			this.stale = false;
			this.laid = this.model.version();
			this.layClaims();
			this.layPins();
			this.marksStale = true;
		}

		if (this.marksStale) {
			this.marksStale = false;
			this.layMarks();
		}

		float a = this.alpha();
		if (this.claims != null) {
			this.claims.draw(ctx, a * this.shown, 0.6F);
		}

		if (this.pins != null) {
			this.pins.draw(ctx, a * this.shown, 0.5F);
		}

		if (this.marks != null) {
			float pulse = this.selecting && Theme.current().motion() > 0.0F ? 0.72F + 0.28F * Mth.sin(ctx.seconds() * 5.6F) : 1.0F;
			this.marks.draw(ctx, a * pulse, 0.3F);
		}
	}

	public void dispose() {
		if (this.claims != null) {
			this.claims.close();
			this.claims = null;
		}

		if (this.pins != null) {
			this.pins.close();
			this.pins = null;
		}

		if (this.marks != null) {
			this.marks.close();
			this.marks = null;
		}
	}

	private static record Look(int fill, int edge, int inner) {
	}
}
