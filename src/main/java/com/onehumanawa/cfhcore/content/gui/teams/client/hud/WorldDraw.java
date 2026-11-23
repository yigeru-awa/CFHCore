package com.onehumanawa.cfhcore.content.gui.teams.client.hud;

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
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

final class WorldDraw {
	private static final ByteBufferBuilder BYTES = new ByteBufferBuilder(32768);
	@Nullable
	private static VertexBuffer buffer;

	static BufferBuilder begin() {
		return new BufferBuilder(BYTES, Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
	}

	static void beam(BufferBuilder b, float x, float y, float z, float height, float core, float glow, int rgb, float alpha, float wave) {
		if (!(alpha <= 0.004F) && !(height <= 0.0F)) {
			float hd = (float)Math.sqrt((double)(x * x + z * z));
			float sx = hd < 0.001F ? 1.0F : -z / hd;
			float sz = hd < 0.001F ? 0.0F : x / hd;
			int segments = 8;
			float a0 = strength(0.0F, wave) * alpha;

			for (int i = 0; i < 8; i++) {
				float t0 = (float)i / 8.0F;
				float t1 = (float)(i + 1) / 8.0F;
				float a1 = strength(t1, wave) * alpha;
				float y0 = y + height * t0 * t0;
				float y1 = y + height * t1 * t1;
				int g0 = Colors.withAlpha(rgb, Math.round(91.8F * a0));
				int g1 = Colors.withAlpha(rgb, Math.round(91.8F * a1));
				int c0 = Colors.withAlpha(rgb, Math.round(229.5F * a0));
				int c1 = Colors.withAlpha(rgb, Math.round(229.5F * a1));
				int clear = rgb & 16777215;
				quad(b, x - sx * glow, y0, z - sz * glow, clear, x, y0, z, g0, x, y1, z, g1, x - sx * glow, y1, z - sz * glow, clear);
				quad(b, x, y0, z, g0, x + sx * glow, y0, z + sz * glow, clear, x + sx * glow, y1, z + sz * glow, clear, x, y1, z, g1);
				quad(
					b, x - sx * core, y0, z - sz * core, c0, x + sx * core, y0, z + sz * core, c0, x + sx * core, y1, z + sz * core, c1, x - sx * core, y1, z - sz * core, c1
				);
				a0 = a1;
			}
		}
	}

	private static float strength(float t, float wave) {
		float left = 1.0F - t * t;
		float fall = left * (float)Math.sqrt((double)left);
		return Float.isNaN(wave) ? fall : fall * (0.82F + 0.18F * Mth.sin(wave - t * 9.0F));
	}

	static void ring(BufferBuilder b, float x, float y, float z, float radius, float width, int rgb, float alpha) {
		if (!(alpha <= 0.004F) && !(radius <= 0.0F)) {
			int segments = 28;
			float inner = Math.max(0.0F, radius - width / 2.0F);
			float outer = radius + width / 2.0F;
			int mid = Colors.withAlpha(rgb, Math.round(255.0F * Mth.clamp(alpha, 0.0F, 1.0F)));
			int clear = rgb & 16777215;
			float c0 = 1.0F;
			float s0 = 0.0F;

			for (int i = 1; i <= 28; i++) {
				float ang = (float)i * (float) (Math.PI * 2) / 28.0F;
				float c1 = Mth.cos(ang);
				float s1 = Mth.sin(ang);
				quad(
					b,
					x + c0 * inner,
					y,
					z + s0 * inner,
					clear,
					x + c0 * radius,
					y,
					z + s0 * radius,
					mid,
					x + c1 * radius,
					y,
					z + s1 * radius,
					mid,
					x + c1 * inner,
					y,
					z + s1 * inner,
					clear
				);
				quad(
					b,
					x + c0 * radius,
					y,
					z + s0 * radius,
					mid,
					x + c0 * outer,
					y,
					z + s0 * outer,
					clear,
					x + c1 * outer,
					y,
					z + s1 * outer,
					clear,
					x + c1 * radius,
					y,
					z + s1 * radius,
					mid
				);
				c0 = c1;
				s0 = s1;
			}
		}
	}

	static void quad(
		BufferBuilder b,
		float x0,
		float y0,
		float z0,
		int c0,
		float x1,
		float y1,
		float z1,
		int c1,
		float x2,
		float y2,
		float z2,
		int c2,
		float x3,
		float y3,
		float z3,
		int c3
	) {
		b.addVertex(x0, y0, z0).setColor(c0);
		b.addVertex(x1, y1, z1).setColor(c1);
		b.addVertex(x2, y2, z2).setColor(c2);
		b.addVertex(x3, y3, z3).setColor(c3);
	}

	static void drawLight(BufferBuilder b) {
		MeshData mesh = b.build();
		if (mesh != null) {
			if (buffer == null || buffer.isInvalid()) {
				buffer = new VertexBuffer(Usage.DYNAMIC);
			}

			buffer.bind();
			buffer.upload(mesh);
			ShaderInstance shader = GameRenderer.getPositionColorShader();
			if (shader != null) {
				translucent();
				Matrix4f modelView = RenderSystem.getModelViewMatrix();
				Matrix4f projection = RenderSystem.getProjectionMatrix();
				RenderSystem.depthFunc(519);
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.22F);
				buffer.drawWithShader(modelView, projection, shader);
				RenderSystem.depthFunc(515);
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
				buffer.drawWithShader(modelView, projection, shader);
			}

			VertexBuffer.unbind();
		}
	}

	static void translucent() {
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(515);
	}

	static void restore() {
		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(515);
		RenderSystem.enableCull();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	static void release() {
		if (buffer != null) {
			buffer.close();
		}

		buffer = null;
	}

	private WorldDraw() {
	}
}
