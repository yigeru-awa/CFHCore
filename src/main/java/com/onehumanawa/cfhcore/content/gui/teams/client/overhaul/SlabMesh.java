package com.onehumanawa.cfhcore.content.gui.teams.client.overhaul;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexBuffer.Usage;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageSoft;
import com.onehumanawa.cfhcore.content.gui.core.stage.mesh.QuadMesh;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

final class SlabMesh implements AutoCloseable {
	@Nullable
	private VertexBuffer buffer;

	private SlabMesh(@Nullable VertexBuffer buffer) {
		this.buffer = buffer;
	}

	static SlabMesh.Builder builder() {
		return new SlabMesh.Builder();
	}

	void draw(StageRenderContext ctx, float alpha, float glow) {
		VertexBuffer vbo = this.buffer;
		if (vbo != null && !(alpha <= 0.004F)) {
			Matrix4f modelView = ctx.pose.last().pose();
			RenderSystem.enablePolygonOffset();
			RenderSystem.polygonOffset(-1.0F, -2.0F);

			try {
				if (!ctx.soft) {
					RenderType type = RenderType.translucent();
					float[] was = (float[])RenderSystem.getShaderColor().clone();
					type.setupRenderState();
					RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
					vbo.bind();
					vbo.drawWithShader(modelView, ctx.camera.projection(), RenderSystem.getShader());
					VertexBuffer.unbind();
					type.clearRenderState();
					RenderSystem.setShaderColor(was[0], was[1], was[2], was[3]);
					return;
				}

				StageSoft.draw(vbo, modelView, ctx.camera.projection(), TextureAtlas.LOCATION_BLOCKS, alpha, true, glow);
			} finally {
				RenderSystem.polygonOffset(0.0F, 0.0F);
				RenderSystem.disablePolygonOffset();
			}
		}
	}

	@Override
	public void close() {
		if (this.buffer != null) {
			this.buffer.close();
			this.buffer = null;
		}
	}

	static final class Builder {
		private final ByteBufferBuilder memory = new ByteBufferBuilder(32768);
		private final BufferBuilder bb = new BufferBuilder(this.memory, Mode.QUADS, DefaultVertexFormat.BLOCK);
		private final float u;
		private final float v;

		private Builder() {
			TextureAtlasSprite white = QuadMesh.sprite("snow");
			this.u = white.getU(0.5F);
			this.v = white.getV(0.5F);
		}

		private void vertex(float x, float y, float z, int argb, float nx, float ny, float nz) {
			this.bb.addVertex(x, y, z).setColor(argb).setUv(this.u, this.v).setLight(15728880).setNormal(nx, ny, nz);
		}

		SlabMesh.Builder top(float x0, float z0, float x1, float z1, float y, int argb) {
			this.vertex(x0, y, z0, argb, 0.0F, 1.0F, 0.0F);
			this.vertex(x0, y, z1, argb, 0.0F, 1.0F, 0.0F);
			this.vertex(x1, y, z1, argb, 0.0F, 1.0F, 0.0F);
			this.vertex(x1, y, z0, argb, 0.0F, 1.0F, 0.0F);
			return this;
		}

		SlabMesh.Builder side(Direction side, float x0, float z0, float x1, float z1, float y0, float y1, int argb) {
			if (y1 <= y0) {
				return this;
			} else {
				switch (side) {
					case NORTH:
						this.vertex(x0, y0, z0, argb, 0.0F, 0.0F, -1.0F);
						this.vertex(x0, y1, z0, argb, 0.0F, 0.0F, -1.0F);
						this.vertex(x1, y1, z0, argb, 0.0F, 0.0F, -1.0F);
						this.vertex(x1, y0, z0, argb, 0.0F, 0.0F, -1.0F);
						break;
					case SOUTH:
						this.vertex(x1, y0, z1, argb, 0.0F, 0.0F, 1.0F);
						this.vertex(x1, y1, z1, argb, 0.0F, 0.0F, 1.0F);
						this.vertex(x0, y1, z1, argb, 0.0F, 0.0F, 1.0F);
						this.vertex(x0, y0, z1, argb, 0.0F, 0.0F, 1.0F);
						break;
					case WEST:
						this.vertex(x0, y0, z1, argb, -1.0F, 0.0F, 0.0F);
						this.vertex(x0, y1, z1, argb, -1.0F, 0.0F, 0.0F);
						this.vertex(x0, y1, z0, argb, -1.0F, 0.0F, 0.0F);
						this.vertex(x0, y0, z0, argb, -1.0F, 0.0F, 0.0F);
						break;
					case EAST:
						this.vertex(x1, y0, z0, argb, 1.0F, 0.0F, 0.0F);
						this.vertex(x1, y1, z0, argb, 1.0F, 0.0F, 0.0F);
						this.vertex(x1, y1, z1, argb, 1.0F, 0.0F, 0.0F);
						this.vertex(x1, y0, z1, argb, 1.0F, 0.0F, 0.0F);
				}

				return this;
			}
		}

		SlabMesh.Builder box(float x0, float y0, float z0, float x1, float y1, float z1, int top, int sides) {
			this.top(x0, z0, x1, z1, y1, top);

			for (Direction d : Plane.HORIZONTAL) {
				this.side(d, x0, z0, x1, z1, y0, y1, sides);
			}

			return this;
		}

		SlabMesh.Builder frame(float x0, float z0, float x1, float z1, float y, float width, int argb) {
			this.top(x0, z0, x1, z0 + width, y, argb);
			this.top(x0, z1 - width, x1, z1, y, argb);
			this.top(x0, z0 + width, x0 + width, z1 - width, y, argb);
			this.top(x1 - width, z0 + width, x1, z1 - width, y, argb);
			return this;
		}

		SlabMesh build() {
			MeshData data = this.bb.build();
			VertexBuffer vbo = null;
			if (data != null) {
				vbo = new VertexBuffer(Usage.STATIC);
				vbo.bind();
				vbo.upload(data);
				VertexBuffer.unbind();
			}

			this.memory.close();
			return new SlabMesh(vbo);
		}
	}
}
