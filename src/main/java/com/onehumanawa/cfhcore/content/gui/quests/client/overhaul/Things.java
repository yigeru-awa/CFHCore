package com.onehumanawa.cfhcore.content.gui.quests.client.overhaul;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer.Usage;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageBlend;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageLevel;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageResources;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageSoft;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

final class Things implements AutoCloseable {
	private static final Things.Part[] NONE = new Things.Part[0];
	private static int held;
	private final StageLevel level;
	private final Map<ItemStack, Things.Thing> byStack = new IdentityHashMap<>();
	private final Map<Things.Key, Things.Thing> byKind = new HashMap<>();
	private int generation = -1;

	static int held() {
		return held;
	}

	Things(StageLevel level) {
		this.level = level;
	}

	Things.Thing of(ItemStack stack) {
		int gen = StageResources.generation();
		if (gen != this.generation) {
			this.close();
			this.generation = gen;
		}

		Things.Thing t = this.byStack.get(stack);
		if (t != null) {
			return t;
		} else {
			Things.Key key = new Things.Key(stack);
			t = this.byKind.get(key);
			if (t == null) {
				t = this.mesh(stack);
				this.byKind.put(key, t);
			}

			this.byStack.put(stack, t);
			return t;
		}
	}

	private Things.Thing mesh(ItemStack stack) {
		Things.Thing t = new Things.Thing();
		ItemRenderer items = Minecraft.getInstance().getItemRenderer();
		Things.Capture capture = new Things.Capture();

		try {
			BakedModel model = items.getModel(stack, this.level, null, 0);
			t.block = model.isGui3d();
			items.render(stack, ItemDisplayContext.FIXED, false, new PoseStack(), capture, 15728880, OverlayTexture.NO_OVERLAY, model);
			t.parts = capture.upload();
			if (t.parts.length > 0) {
				t.shimmer = capture.shimmer();
			}

			t.live = t.parts.length == 0 || capture.foreign || stack.is(Items.PLAYER_HEAD);
		} catch (RuntimeException var9) {
			CFHGuiQuests.LOGGER.debug("[CFHGui Quests] {} is drawn by the game itself on the quest island: {}", stack.getItem(), var9.toString());
			t.live = true;
		} finally {
			capture.close();
		}

		return t;
	}

	static void draw(Things.Thing thing, StageRenderContext ctx, float alpha) {
		for (Things.Part p : thing.parts) {
			StageSoft.draw(p.mesh(), ctx.pose.last().pose(), ctx.camera.projection(), p.texture(), Math.min(alpha, 0.999F), false);
		}
	}

	static void shimmer(Things.Thing thing, StageRenderContext ctx, float strength) {
		VertexBuffer faces = thing.shimmer;
		ShaderInstance cutout = GameRenderer.getRendertypeCutoutShader();
		if (faces != null && cutout != null && !(strength <= 0.02F)) {
			float[] was = (float[])RenderSystem.getShaderColor().clone();
			Matrix4f pose = ctx.pose.last().pose();
			Matrix4f projection = ctx.camera.projection();
			RenderSystem.enablePolygonOffset();
			RenderSystem.polygonOffset(-1.0F, -10.0F);
			RenderSystem.colorMask(false, false, false, false);
			RenderSystem.disableBlend();
			RenderSystem.enableDepthTest();
			RenderSystem.depthFunc(515);
			RenderSystem.depthMask(true);
			RenderSystem.enableCull();
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			cutout.safeGetUniform("ChunkOffset").set(0.0F, 0.0F, 0.0F);

			for (Things.Part p : thing.parts) {
				RenderSystem.setShaderTexture(0, p.texture());
				p.mesh().bind();
				p.mesh().drawWithShader(pose, projection, cutout);
			}

			RenderSystem.colorMask(true, true, true, true);
			RenderType glint = RenderType.glint();
			glint.setupRenderState();
			RenderSystem.setShaderColor(strength, strength, strength, 1.0F);
			faces.bind();
			faces.drawWithShader(pose, projection, RenderSystem.getShader());
			VertexBuffer.unbind();
			glint.clearRenderState();
			RenderSystem.polygonOffset(0.0F, 0.0F);
			RenderSystem.disablePolygonOffset();
			RenderSystem.setShaderColor(was[0], was[1], was[2], was[3]);
			RenderSystem.enableDepthTest();
			RenderSystem.depthFunc(515);
			RenderSystem.depthMask(true);
			RenderSystem.enableCull();
			RenderSystem.enableBlend();
			StageBlend.over();
		}
	}

	@Override
	public void close() {
		for (Things.Thing t : this.byKind.values()) {
			for (Things.Part p : t.parts) {
				p.mesh().close();
				held--;
			}

			t.parts = NONE;
			if (t.shimmer != null) {
				t.shimmer.close();
				t.shimmer = null;
				held--;
			}
		}

		this.byKind.clear();
		this.byStack.clear();
	}

	private static final class Capture implements MultiBufferSource, AutoCloseable {
		private static final Map<RenderType, Optional<ResourceLocation>> TEXTURES = new IdentityHashMap<>();
		private final Map<ResourceLocation, Things.Sink> sinks = new LinkedHashMap<>();
		@Nullable
		private Things.Shimmer shimmering;
		boolean foreign;
		private static final VertexConsumer NOWHERE = new VertexConsumer() {
			public VertexConsumer addVertex(float x, float y, float z) {
				return this;
			}

			public VertexConsumer setColor(int r, int g, int b, int a) {
				return this;
			}

			public VertexConsumer setUv(float u, float v) {
				return this;
			}

			public VertexConsumer setUv1(int u, int v) {
				return this;
			}

			public VertexConsumer setUv2(int u, int v) {
				return this;
			}

			public VertexConsumer setNormal(float x, float y, float z) {
				return this;
			}
		};

		public VertexConsumer getBuffer(RenderType type) {
			ResourceLocation texture = TEXTURES.computeIfAbsent(type, Things.Capture::textureOf).orElse(null);
			if (texture != null) {
				return this.sinks.computeIfAbsent(texture, t -> new Things.Sink());
			} else if (shimmer(type)) {
				if (this.shimmering == null) {
					this.shimmering = new Things.Shimmer();
				}

				return this.shimmering;
			} else {
				this.foreign = true;
				return NOWHERE;
			}
		}

		private static boolean shimmer(RenderType type) {
			return type == RenderType.glint()
				|| type == RenderType.glintTranslucent()
				|| type == RenderType.entityGlint()
				|| type == RenderType.entityGlintDirect()
				|| type == RenderType.armorEntityGlint()
				|| type.toString().contains("glint");
		}

		private static Optional<ResourceLocation> textureOf(RenderType type) {
			if (type.mode() != Mode.QUADS || shimmer(type)) {
				return Optional.empty();
			} else if (type != Sheets.cutoutBlockSheet()
				&& type != Sheets.solidBlockSheet()
				&& type != Sheets.translucentCullBlockSheet()
				&& type != Sheets.translucentItemSheet()) {
				VertexFormat format = type.format();
				if (format != DefaultVertexFormat.NEW_ENTITY && format != DefaultVertexFormat.BLOCK) {
					return Optional.empty();
				} else {
					String described = type.toString();
					String mark = "[Optional[";
					int at = described.indexOf("[Optional[");
					int end = at < 0 ? -1 : described.indexOf(93, at + "[Optional[".length());
					return end < 0 ? Optional.empty() : Optional.ofNullable(ResourceLocation.tryParse(described.substring(at + "[Optional[".length(), end)));
				}
			} else {
				return Optional.of(TextureAtlas.LOCATION_BLOCKS);
			}
		}

		Things.Part[] upload() {
			List<Things.Part> parts = new ArrayList<>();

			for (Entry<ResourceLocation, Things.Sink> e : this.sinks.entrySet()) {
				MeshData data = e.getValue().build();
				if (data != null) {
					VertexBuffer vbo = new VertexBuffer(Usage.STATIC);
					vbo.bind();
					vbo.upload(data);
					VertexBuffer.unbind();
					Things.held++;
					parts.add(new Things.Part(e.getKey(), vbo));
				}
			}

			return parts.toArray(Things.NONE);
		}

		@Nullable
		VertexBuffer shimmer() {
			MeshData data = this.shimmering == null ? null : this.shimmering.build();
			if (data == null) {
				return null;
			} else {
				VertexBuffer vbo = new VertexBuffer(Usage.STATIC);
				vbo.bind();
				vbo.upload(data);
				VertexBuffer.unbind();
				Things.held++;
				return vbo;
			}
		}

		@Override
		public void close() {
			for (Things.Sink s : this.sinks.values()) {
				s.memory.close();
			}

			if (this.shimmering != null) {
				this.shimmering.memory.close();
			}
		}
	}

	private static record Key(ItemStack stack) {
		@Override
		public boolean equals(Object o) {
			if (o instanceof Things.Key k && ItemStack.isSameItemSameComponents(this.stack, k.stack)) {
				return true;
			}

			return false;
		}

		@Override
		public int hashCode() {
			return ItemStack.hashItemAndComponents(this.stack);
		}
	}

	static record Part(ResourceLocation texture, VertexBuffer mesh) {
	}

	private static final class Shimmer implements VertexConsumer {
		final ByteBufferBuilder memory = new ByteBufferBuilder(4096);
		private final BufferBuilder out = new BufferBuilder(this.memory, Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		private boolean placed = true;
		private int vertices;

		public VertexConsumer addVertex(float x, float y, float z) {
			if (!this.placed) {
				this.out.setUv(0.0F, 0.0F);
			}

			this.out.addVertex(x, y, z);
			this.placed = false;
			this.vertices++;
			return this;
		}

		public VertexConsumer setUv(float u, float v) {
			if (!this.placed) {
				this.out.setUv(u, v);
				this.placed = true;
			}

			return this;
		}

		public VertexConsumer setColor(int r, int g, int b, int a) {
			return this;
		}

		public VertexConsumer setUv1(int u, int v) {
			return this;
		}

		public VertexConsumer setUv2(int u, int v) {
			return this;
		}

		public VertexConsumer setNormal(float x, float y, float z) {
			return this;
		}

		@Nullable
		MeshData build() {
			if (!this.placed) {
				this.out.setUv(0.0F, 0.0F);
			}

			this.placed = true;
			return this.vertices >= 4 && this.vertices % 4 == 0 ? this.out.build() : null;
		}
	}

	private static final class Sink implements VertexConsumer {
		private static final int COLOR = 1;
		private static final int UV = 2;
		private static final int NORMAL = 4;
		private static final int ALL = 7;
		final ByteBufferBuilder memory = new ByteBufferBuilder(8192);
		private final BufferBuilder out = new BufferBuilder(this.memory, Mode.QUADS, DefaultVertexFormat.BLOCK);
		private int has = 7;
		private int vertices;

		private void finish() {
			if (this.has != 7) {
				if ((this.has & 1) == 0) {
					this.out.setColor(255, 255, 255, 255);
				}

				if ((this.has & 2) == 0) {
					this.out.setUv(0.0F, 0.0F);
				}

				if ((this.has & 4) == 0) {
					this.out.setNormal(0.0F, 1.0F, 0.0F);
				}

				this.has = 7;
			}
		}

		public VertexConsumer addVertex(float x, float y, float z) {
			this.finish();
			this.out.addVertex(x, y, z);
			this.out.setUv2(0, 0);
			this.has = 0;
			this.vertices++;
			return this;
		}

		public VertexConsumer setColor(int r, int g, int b, int a) {
			if ((this.has & 1) == 0) {
				this.out.setColor(r, g, b, a);
				this.has |= 1;
			}

			return this;
		}

		public VertexConsumer setUv(float u, float v) {
			if ((this.has & 2) == 0) {
				this.out.setUv(u, v);
				this.has |= 2;
			}

			return this;
		}

		public VertexConsumer setUv1(int u, int v) {
			return this;
		}

		public VertexConsumer setUv2(int u, int v) {
			return this;
		}

		public VertexConsumer setNormal(float x, float y, float z) {
			if ((this.has & 4) == 0) {
				this.out.setNormal(x, y, z);
				this.has |= 4;
			}

			return this;
		}

		MeshData build() {
			this.finish();
			return this.vertices >= 4 && this.vertices % 4 == 0 ? this.out.build() : null;
		}
	}

	static final class Thing {
		Things.Part[] parts = Things.NONE;
		@Nullable
		VertexBuffer shimmer;
		boolean block;
		boolean live;
	}
}
