package com.onehumanawa.cfhcore.content.gui.veinminer.client;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexBuffer.Usage;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.onehumanawa.cfhcore.content.gui.core.client.render.CFHGuiRenderEvents;
import com.onehumanawa.cfhcore.content.gui.core.client.render.CFHGuiRenderEvents.WorldRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.veinminer.CFHGuiVeinminer;
import com.onehumanawa.cfhcore.content.gui.veinminer.config.VeinClientConfig;
import com.onehumanawa.cfhcore.content.gui.veinminer.vein.VeinFinder;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

final class OutlineRenderer {
	private static final float INFLATE = 0.003F;
	private static final float LIFT = 0.004F;
	private static final float TARGET_LIFT = 0.008F;
	@Nullable
	private static ByteBufferBuilder bytes;
	@Nullable
	private static VertexBuffer lines;
	@Nullable
	private static VertexBuffer fills;
	private static boolean hasLines;
	private static boolean hasFills;
	private static int builtVersion = Integer.MIN_VALUE;
	private static int builtKey;
	@Nullable
	private static BlockPos builtOrigin;
	private static final Matrix4f VIEW = new Matrix4f();
	private static final LongOpenHashSet SET = new LongOpenHashSet();
	private static boolean failedOnce;

	static void init() {
		CFHGuiRenderEvents.AFTER_TRANSLUCENT.register(OutlineRenderer::drawInLevel);
		CFHGuiRenderEvents.AFTER_LEVEL.register(OutlineRenderer::drawAfterLevel);
		CFHGuiEvents.CLIENT_LEFT_SERVER.register(OutlineRenderer::release);
	}

	private static void drawInLevel(WorldRenderContext ctx) {
		if (!IrisCompat.shaderPackInUse()) {
			draw(ctx);
		}
	}

	private static void drawAfterLevel(WorldRenderContext ctx) {
		if (IrisCompat.shaderPackInUse() && visible()) {
			Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
			Matrix4fStack view = RenderSystem.getModelViewStack();
			view.pushMatrix();
			view.set(ctx.modelView());
			RenderSystem.applyModelViewMatrix();

			try {
				draw(ctx);
			} finally {
				view.popMatrix();
				RenderSystem.applyModelViewMatrix();
			}
		}
	}

	private static boolean visible() {
		Minecraft mc = Minecraft.getInstance();
		return VeinClientConfig.get().preview
			&& VeinPreview.hasTarget()
			&& VeinPreview.count() > 0
			&& VeinPreview.origin() != null
			&& mc.level != null
			&& !mc.options.hideGui
			&& !IrisCompat.shadowPass();
	}

	static boolean drawsOutlineOf(BlockPos pos) {
		BlockPos origin = VeinPreview.origin();
		Minecraft mc = Minecraft.getInstance();
		return origin != null && origin.equals(pos) && VeinClientConfig.get().preview && VeinPreview.hasTarget() && VeinPreview.count() > 0 && mc.level != null;
	}

	private static void draw(WorldRenderContext ctx) {
		if (visible()) {
			VeinClientConfig cfg = VeinClientConfig.get();
			BlockPos origin = VeinPreview.origin();

			try {
				Palette p = Theme.current().palette();
				boolean custom = cfg.colorMode() == VeinClientConfig.ColorMode.CUSTOM;
				int ink = custom ? cfg.customArgb() : p.accentInk();
				int fill = custom ? cfg.customArgb() : p.accent();
				VeinClientConfig.Style style = cfg.style();
				int key = ((ink * 31 + fill) * 31 + style.ordinal()) * 31 + cfg.maxPreview;
				if (builtVersion != VeinPreview.positionsVersion() || builtKey != key || builtOrigin == null || !builtOrigin.equals(origin)) {
					build(VeinPreview.result(), origin, style, ink | 0xFF000000, fill | 0xFF000000, cfg.maxPreview);
					builtVersion = VeinPreview.positionsVersion();
					builtKey = key;
					builtOrigin = origin;
				}

				if (hasLines || hasFills) {
					Vec3 cam = ctx.camera().getPosition();
					VIEW.set(RenderSystem.getModelViewMatrix())
						.translate((float)((double)origin.getX() - cam.x), (float)((double)origin.getY() - cam.y), (float)((double)origin.getZ() - cam.z));
					Matrix4f projection = RenderSystem.getProjectionMatrix();
					float breathe = breathe();
					state();
					RenderSystem.enablePolygonOffset();
					RenderSystem.polygonOffset(-1.0F, -10.0F);
					boolean xray = cfg.throughWalls;
					if (hasFills && fills != null) {
						float a = style == VeinClientConfig.Style.FILL ? 0.34F : 0.2F;
						if (xray) {
							drawFills(fills, projection, a * 0.35F * breathe, false);
						}

						drawFills(fills, projection, a * breathe, true);
					}

					if (hasLines && lines != null) {
						if (xray) {
							drawLines(lines, projection, px(2.0F), 0.42F * breathe, false);
						}

						drawLines(lines, projection, px(7.0F), 0.16F * breathe, true);
						drawLines(lines, projection, px(3.0F), breathe, true);
					}

					return;
				}
			} catch (RuntimeException var17) {
				if (!failedOnce) {
					failedOnce = true;
					CFHGuiVeinminer.LOGGER.error("[CFHGui Veinminer] preview rendering failed (logged once)", var17);
				}

				return;
			} finally {
				restore();
			}
		}
	}

	private static void build(VeinFinder.Result vein, BlockPos origin, VeinClientConfig.Style style, int ink, int fill, int maxShown) {
		if (bytes == null) {
			bytes = new ByteBufferBuilder(65536);
		}

		int n = Math.min(vein.size(), Math.max(1, maxShown));
		SET.clear();

		for (int i = 0; i < n; i++) {
			SET.add(vein.get(i));
		}

		int ox = origin.getX();
		int oy = origin.getY();
		int oz = origin.getZ();
		BufferBuilder lineBuf = new BufferBuilder(bytes, Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
		if (style != VeinClientConfig.Style.FILL) {
			for (int i = 0; i < n; i++) {
				long b = vein.get(i);
				edges(lineBuf, BlockPos.getX(b), BlockPos.getY(b), BlockPos.getZ(b), ox, oy, oz, ink);
			}
		}

		targetEdges(lineBuf, Colors.mix(ink, -1, 0.5F) | 0xFF000000);
		hasLines = upload(lineBuf, lines());
		if (style != VeinClientConfig.Style.OUTLINE) {
			BufferBuilder buf = new BufferBuilder(bytes, Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

			for (int i = 0; i < n; i++) {
				long b = vein.get(i);
				faces(buf, BlockPos.getX(b), BlockPos.getY(b), BlockPos.getZ(b), ox, oy, oz, fill);
			}

			hasFills = upload(buf, fills());
		} else {
			hasFills = false;
		}
	}

	private static void edges(BufferBuilder buf, int x, int y, int z, int ox, int oy, int oz, int argb) {
		for (int axis = 0; axis < 3; axis++) {
			for (int du = 0; du <= 1; du++) {
				for (int dv = 0; dv <= 1; dv++) {
					boolean f00 = filled(x, y, z, axis, du - 1, dv - 1);
					boolean f10 = filled(x, y, z, axis, du, dv - 1);
					boolean f01 = filled(x, y, z, axis, du - 1, dv);
					boolean f11 = filled(x, y, z, axis, du, dv);
					int count = (f00 ? 1 : 0) + (f10 ? 1 : 0) + (f01 ? 1 : 0) + (f11 ? 1 : 0);
					boolean bends = count == 1 || count == 3 || count == 2 && f00 == f11;
					if (bends) {
						int owner = f00 ? 0 : (f10 ? 1 : (f01 ? 2 : 3));
						int self = 1 - du + 2 * (1 - dv);
						if (owner == self) {
							float lu = 0.004F * (float)Integer.signum((f00 ? 1 : 0) + (f01 ? 1 : 0) - (f10 ? 1 : 0) - (f11 ? 1 : 0));
							float lv = 0.004F * (float)Integer.signum((f00 ? 1 : 0) + (f10 ? 1 : 0) - (f01 ? 1 : 0) - (f11 ? 1 : 0));
							float rx = (float)(x - ox);
							float ry = (float)(y - oy);
							float rz = (float)(z - oz);
							switch (axis) {
								case 0:
									line(buf, rx - 0.004F, ry + (float)du + lu, rz + (float)dv + lv, rx + 1.0F + 0.004F, ry + (float)du + lu, rz + (float)dv + lv, argb, 1.0F, 0.0F, 0.0F);
									break;
								case 1:
									line(buf, rx + (float)du + lu, ry - 0.004F, rz + (float)dv + lv, rx + (float)du + lu, ry + 1.0F + 0.004F, rz + (float)dv + lv, argb, 0.0F, 1.0F, 0.0F);
									break;
								default:
									line(buf, rx + (float)du + lu, ry + (float)dv + lv, rz - 0.004F, rx + (float)du + lu, ry + (float)dv + lv, rz + 1.0F + 0.004F, argb, 0.0F, 0.0F, 1.0F);
							}
						}
					}
				}
			}
		}
	}

	private static void targetEdges(BufferBuilder buf, int argb) {
		float a = -0.008F;
		float b = 1.008F;

		for (int i = 0; i < 4; i++) {
			float p = (i & 1) == 0 ? -0.008F : 1.008F;
			float q = (i & 2) == 0 ? -0.008F : 1.008F;
			line(buf, -0.008F, p, q, 1.008F, p, q, argb, 1.0F, 0.0F, 0.0F);
			line(buf, p, -0.008F, q, p, 1.008F, q, argb, 0.0F, 1.0F, 0.0F);
			line(buf, p, q, -0.008F, p, q, 1.008F, argb, 0.0F, 0.0F, 1.0F);
		}
	}

	private static boolean filled(int x, int y, int z, int axis, int ou, int ov) {
		return switch (axis) {
			case 0 -> SET.contains(BlockPos.asLong(x, y + ou, z + ov));
			case 1 -> SET.contains(BlockPos.asLong(x + ou, y, z + ov));
			default -> SET.contains(BlockPos.asLong(x + ou, y + ov, z));
		};
	}

	private static void line(BufferBuilder buf, float x0, float y0, float z0, float x1, float y1, float z1, int argb, float nx, float ny, float nz) {
		buf.addVertex(x0, y0, z0).setColor(argb).setNormal(nx, ny, nz);
		buf.addVertex(x1, y1, z1).setColor(argb).setNormal(nx, ny, nz);
	}

	private static void faces(BufferBuilder buf, int x, int y, int z, int ox, int oy, int oz, int argb) {
		float x0 = (float)(x - ox) - 0.003F;
		float y0 = (float)(y - oy) - 0.003F;
		float z0 = (float)(z - oz) - 0.003F;
		float x1 = (float)(x - ox + 1) + 0.003F;
		float y1 = (float)(y - oy + 1) + 0.003F;
		float z1 = (float)(z - oz + 1) + 0.003F;
		if (!SET.contains(BlockPos.asLong(x, y - 1, z))) {
			quad(buf, argb, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
		}

		if (!SET.contains(BlockPos.asLong(x, y + 1, z))) {
			quad(buf, argb, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
		}

		if (!SET.contains(BlockPos.asLong(x, y, z - 1))) {
			quad(buf, argb, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
		}

		if (!SET.contains(BlockPos.asLong(x, y, z + 1))) {
			quad(buf, argb, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
		}

		if (!SET.contains(BlockPos.asLong(x - 1, y, z))) {
			quad(buf, argb, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
		}

		if (!SET.contains(BlockPos.asLong(x + 1, y, z))) {
			quad(buf, argb, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
		}
	}

	private static void quad(
		BufferBuilder buf, int argb, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz
	) {
		buf.addVertex(ax, ay, az).setColor(argb);
		buf.addVertex(bx, by, bz).setColor(argb);
		buf.addVertex(cx, cy, cz).setColor(argb);
		buf.addVertex(dx, dy, dz).setColor(argb);
	}

	private static boolean upload(BufferBuilder buf, VertexBuffer vb) {
		MeshData mesh = buf.build();
		if (mesh == null) {
			return false;
		} else {
			vb.bind();
			vb.upload(mesh);
			VertexBuffer.unbind();
			return true;
		}
	}

	private static VertexBuffer lines() {
		if (lines == null || lines.isInvalid()) {
			lines = new VertexBuffer(Usage.STATIC);
		}

		return lines;
	}

	private static VertexBuffer fills() {
		if (fills == null || fills.isInvalid()) {
			fills = new VertexBuffer(Usage.STATIC);
		}

		return fills;
	}

	private static void drawLines(VertexBuffer vb, Matrix4f projection, float width, float alpha, boolean depthTest) {
		ShaderInstance shader = GameRenderer.getRendertypeLinesShader();
		if (shader != null && !(alpha <= 0.004F)) {
			RenderSystem.depthFunc(depthTest ? 515 : 519);
			RenderSystem.lineWidth(width);
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
			vb.bind();
			vb.drawWithShader(VIEW, projection, shader);
			VertexBuffer.unbind();
		}
	}

	private static void drawFills(VertexBuffer vb, Matrix4f projection, float alpha, boolean depthTest) {
		ShaderInstance shader = GameRenderer.getPositionColorShader();
		if (shader != null && !(alpha <= 0.004F)) {
			RenderSystem.depthFunc(depthTest ? 515 : 519);
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
			vb.bind();
			vb.drawWithShader(VIEW, projection, shader);
			VertexBuffer.unbind();
		}
	}

	private static float breathe() {
		float motion = Theme.current().motion();
		if (motion <= 0.0F) {
			return 1.0F;
		} else {
			double t = (double)(Clock.nowMs() % 3600000L) / 1000.0 / (double)motion;
			return 0.86F + 0.14F * (float)Math.sin(t * Math.PI * 2.0 / 2.4);
		}
	}

	private static float px(float width) {
		return width * Math.max(1.0F, (float)Minecraft.getInstance().getWindow().getHeight() / 1080.0F);
	}

	private static void state() {
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		RenderSystem.enableDepthTest();
	}

	private static void restore() {
		RenderSystem.polygonOffset(0.0F, 0.0F);
		RenderSystem.disablePolygonOffset();
		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(515);
		RenderSystem.enableCull();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
		RenderSystem.lineWidth(1.0F);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private static void release() {
		if (!RenderSystem.isOnRenderThread()) {
			Minecraft.getInstance().execute(OutlineRenderer::release);
		} else {
			if (lines != null) {
				lines.close();
			}

			if (fills != null) {
				fills.close();
			}

			lines = null;
			fills = null;
			hasLines = false;
			hasFills = false;
			builtVersion = Integer.MIN_VALUE;
			builtOrigin = null;
		}
	}

	private OutlineRenderer() {
	}
}
