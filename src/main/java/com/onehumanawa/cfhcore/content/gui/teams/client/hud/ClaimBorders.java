package com.onehumanawa.cfhcore.content.gui.teams.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexBuffer.Usage;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.teams.TeamsConfig;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsState;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamSummary;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

final class ClaimBorders {
	private static final int RADIUS = 5;
	private static final int SHOW_MS = 7000;
	private static final int QUIET_AFTER_JOIN_MS = 3000;
	private static final float INSET = 0.035F;
	private static final float LINE = 0.14F;
	private static final float STRIP = 0.4F;
	private static final float CURTAIN = 2.75F;
	private static final float BAND = 5.0F;
	@Nullable
	private static VertexBuffer mesh;
	private static boolean meshFilled;
	private static boolean dirty = true;
	private static double originX;
	private static double originY;
	private static double originZ;
	private static int builtX = Integer.MIN_VALUE;
	private static int builtZ;
	private static int builtBand = Integer.MIN_VALUE;
	private static String builtDimension = "";
	private static long signature;
	private static boolean signatureKnown;
	private static boolean changed;
	private static int atX = Integer.MIN_VALUE;
	private static int atZ;
	private static String atDimension = "";
	private static long showUntil;
	private static long quietUntil;
	private static final Anim SHOWN = new Anim(0.0F, 300, Ease.OUT_CUBIC);

	static void claimsChanged(String dimension) {
		if (dimension.equals(TeamsState.currentDimension())) {
			changed = true;
		}
	}

	static void joined() {
		signatureKnown = false;
		quietUntil = Clock.nowMs() + 3000L;
		showUntil = 0L;
		SHOWN.snap(0.0F);
		dirty = true;
	}

	static void show() {
		showUntil = Clock.nowMs() + 7000L;
	}

	static void tick() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && mc.level != null) {
			long now = Clock.nowMs();
			String dimension = TeamsState.currentDimension();
			int cx = mc.player.chunkPosition().x;
			int cz = mc.player.chunkPosition().z;
			boolean moved = cx != atX || cz != atZ || !dimension.equals(atDimension);
			if (moved || changed || !signatureKnown) {
				long sig = signature(dimension, cx, cz);
				if (changed && !moved && signatureKnown && sig != signature && now >= quietUntil) {
					show();
				}

				if (sig != signature || moved) {
					dirty = true;
				}

				signature = sig;
				signatureKnown = true;
				changed = false;
				atX = cx;
				atZ = cz;
				atDimension = dimension;
			}

			boolean want = TeamsConfig.get().claimBorders && TeamsState.available() && (HudKeys.BORDERS_KEY.isDown() || now < showUntil);
			SHOWN.set(want ? 1.0F : 0.0F);
		}
	}

	private static long signature(String dimension, int cx, int cz) {
		long sig = 1125899906842597L;

		for (int dz = -5; dz <= 5; dz++) {
			for (int dx = -5; dx <= 5; dx++) {
				TeamsState.Claim c = claimAt(dimension, cx + dx, cz + dz);
				sig = sig * 31L + (c == null ? 0L : (long)c.team().hashCode() * 2L + 1L);
			}
		}

		return sig;
	}

	static void render() {
		float shown = SHOWN.get();
		if (!(shown <= 0.01F)) {
			Minecraft mc = Minecraft.getInstance();
			ClientLevel level = mc.level;
			if (level != null && mc.player != null) {
				int px = Mth.floor(mc.player.getX());
				int pz = Mth.floor(mc.player.getZ());
				boolean under = mc.player.getY() < (double)(ground(level, px, pz, new MutableBlockPos()) - 6);
				int band = under ? Mth.floor(mc.player.getY() / 3.0) : -2147483647;
				if (dirty || band != builtBand || atX != builtX || atZ != builtZ || !atDimension.equals(builtDimension)) {
					build(level, under, band);
				}

				if (meshFilled && mesh != null) {
					ShaderInstance shader = GameRenderer.getPositionColorShader();
					if (shader != null) {
						float motion = HudDraw.theme().motion();
						float breathe = motion <= 0.0F ? 1.0F : 0.86F + 0.14F * Mth.sin((float)(Clock.nowMs() % 3200L) / 3200.0F * (float) (Math.PI * 2));
						Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix())
							.translate((float)(originX - WorldView.camX()), (float)(originY - WorldView.camY()), (float)(originZ - WorldView.camZ()));
						WorldDraw.translucent();
						RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, shown * breathe);
						mesh.bind();
						mesh.drawWithShader(modelView, RenderSystem.getProjectionMatrix(), shader);
						VertexBuffer.unbind();
						RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
					}
				}
			}
		}
	}

	private static void build(ClientLevel level, boolean under, int band) {
		dirty = false;
		builtX = atX;
		builtZ = atZ;
		builtBand = band;
		builtDimension = atDimension;
		Minecraft mc = Minecraft.getInstance();
		originX = (double)atX * 16.0;
		originZ = (double)atZ * 16.0;
		originY = mc.player == null ? 64.0 : Math.floor(mc.player.getY());
		float bandY = under && mc.player != null ? (float)(mc.player.getY() + 0.9 - originY) : Float.NaN;
		MutableBlockPos pos = new MutableBlockPos();
		ByteBufferBuilder bytes = new ByteBufferBuilder(65536);

		try {
			BufferBuilder b = new BufferBuilder(bytes, Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

			for (int dz = -5; dz <= 5; dz++) {
				for (int dx = -5; dx <= 5; dx++) {
					int cx = atX + dx;
					int cz = atZ + dz;
					TeamsState.Claim claim = claimAt(atDimension, cx, cz);
					if (claim != null && level.hasChunk(cx, cz)) {
						UUID team = claim.team();
						int color = color(team);
						float strength = TeamsState.isOwn(team) ? 1.0F : 0.8F;
						if (!same(team, cx - 1, cz)) {
							side(b, level, pos, cx, cz, 0, color, strength, bandY);
						}

						if (!same(team, cx + 1, cz)) {
							side(b, level, pos, cx, cz, 1, color, strength, bandY);
						}

						if (!same(team, cx, cz - 1)) {
							side(b, level, pos, cx, cz, 2, color, strength, bandY);
						}

						if (!same(team, cx, cz + 1)) {
							side(b, level, pos, cx, cz, 3, color, strength, bandY);
						}
					}
				}
			}

			MeshData data = b.build();
			meshFilled = data != null;
			if (data != null) {
				if (mesh == null || mesh.isInvalid()) {
					mesh = new VertexBuffer(Usage.STATIC);
				}

				mesh.bind();
				mesh.upload(data);
				VertexBuffer.unbind();
			}
		} catch (Throwable var17) {
			try {
				bytes.close();
			} catch (Throwable var16) {
				var17.addSuppressed(var16);
			}

			throw var17;
		}

		bytes.close();
	}

	@Nullable
	private static TeamsState.Claim claimAt(String dimension, int cx, int cz) {
		if (HudDemo.hasClaims()) {
			TeamsState.Claim laid = HudDemo.claimAt(dimension, ChunkPos.asLong(cx, cz));
			if (laid != null) {
				return laid;
			}
		}

		return TeamsState.claimAt(dimension, cx, cz);
	}

	private static boolean same(UUID team, int cx, int cz) {
		TeamsState.Claim c = claimAt(atDimension, cx, cz);
		return c != null && team.equals(c.team());
	}

	private static int color(UUID team) {
		TeamData mine = TeamsState.land();
		if (mine != null && mine.id.equals(team)) {
			return HudDraw.readable(mine.color);
		} else {
			TeamSummary s = TeamsState.summary(team);
			return s == null ? -5197648 : HudDraw.readable(s.color);
		}
	}

	private static void side(BufferBuilder b, ClientLevel level, MutableBlockPos pos, int cx, int cz, int side, int color, float strength, float bandY) {
		int x0 = cx << 4;
		int z0 = cz << 4;
		int line = Colors.withAlpha(color, Math.round(235.0F * strength));
		int foot = Colors.withAlpha(color, Math.round(105.0F * strength));
		int clear = color & 16777215;

		for (int i = 0; i < 16; i++) {
			int bx = side == 0 ? x0 : (side == 1 ? x0 + 15 : x0 + i);
			int bz = side == 2 ? z0 : (side == 3 ? z0 + 15 : z0 + i);
			float ax;
			float az;
			float ex;
			float ez;
			if (side < 2) {
				ax = ex = (float)((double)(side == 0 ? (float)x0 + 0.035F : (float)(x0 + 16) - 0.035F) - originX);
				az = (float)((double)(z0 + i) - originZ);
				ez = az + 1.0F;
			} else {
				az = ez = (float)((double)(side == 2 ? (float)z0 + 0.035F : (float)(z0 + 16) - 0.035F) - originZ);
				ax = (float)((double)(x0 + i) - originX);
				ex = ax + 1.0F;
			}

			float y = (float)((double)ground(level, bx, bz, pos) - originY) + 0.02F;
			float in = side != 0 && side != 2 ? -0.4F : 0.4F;
			if (side < 2) {
				WorldDraw.quad(b, ax, y, az, line, ex, y, ez, line, ex + in, y, ez, line, ax + in, y, az, line);
			} else {
				WorldDraw.quad(b, ax, y, az, line, ex, y, ez, line, ex, y, ez + in, line, ax, y, az + in, line);
			}

			WorldDraw.quad(b, ax, y, az, line, ex, y, ez, line, ex, y + 0.14F, ez, line, ax, y + 0.14F, az, line);
			WorldDraw.quad(b, ax, y + 0.14F, az, foot, ex, y + 0.14F, ez, foot, ex, y + 2.75F, ez, clear, ax, y + 2.75F, az, clear);
			if (!Float.isNaN(bandY)) {
				int mid = Colors.withAlpha(color, Math.round(120.0F * strength));
				WorldDraw.quad(b, ax, bandY - 5.0F, az, clear, ex, bandY - 5.0F, ez, clear, ex, bandY, ez, mid, ax, bandY, az, mid);
				WorldDraw.quad(b, ax, bandY, az, mid, ex, bandY, ez, mid, ex, bandY + 5.0F, ez, clear, ax, bandY + 5.0F, az, clear);
			}
		}
	}

	static int ground(ClientLevel level, int x, int z, MutableBlockPos pos) {
		int y = level.getHeight(Types.MOTION_BLOCKING, x, z);
		int min = level.getMinBuildHeight();

		for (int i = 0; i < 64 && y > min; i++) {
			BlockState below = level.getBlockState(pos.set(x, y - 1, z));
			boolean tree = below.is(BlockTags.LEAVES) || below.is(BlockTags.LOGS);
			if (!tree && (!below.getFluidState().isEmpty() || !below.getCollisionShape(level, pos).isEmpty())) {
				break;
			}

			y--;
		}

		return y;
	}

	static void release() {
		if (mesh != null) {
			mesh.close();
		}

		mesh = null;
		meshFilled = false;
		dirty = true;
		signatureKnown = false;
		atX = Integer.MIN_VALUE;
		builtX = Integer.MIN_VALUE;
		SHOWN.snap(0.0F);
	}

	private ClaimBorders() {
	}
}
