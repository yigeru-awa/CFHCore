package com.onehumanawa.cfhcore.content.gui.quests.client.overhaul;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.stage.Stage;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageBlend;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageFinish;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageLighting;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.FxNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.MotesNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.SkyNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.StageNode;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

final class Air {
	private static final int TURN_MS = 520;
	private final Stage stage;
	private final SkyNode sky = new SkyNode(260, 31L);
	private final FxNode glow;
	private final FxNode cloudsFar;
	private final FxNode cloudsNear;
	private final FxNode starsBelow;
	private final MotesNode motes;
	private final Air.Blueprint grid = new Air.Blueprint();
	private final StageFinish finish = StageFinish.cinematic();
	@Nullable
	private IslandTheme was;
	@Nullable
	private IslandTheme now;
	private long turnedAtMs;

	Air(Stage stage) {
		this.stage = stage;
		stage.add(this.sky);
		this.glow = (FxNode)stage.add(FxNode.glow(1.0F, -1).fade(1.7F));
		this.cloudsFar = (FxNode)stage.add(FxNode.clouds(64, 380.0F, 280.0F, 0.0F, 7.5F, 77L, 0.45F));
		this.cloudsNear = (FxNode)stage.add(FxNode.clouds(30, 240.0F, 170.0F, 0.0F, 4.4F, 78L, 0.8F));
		this.starsBelow = (FxNode)stage.add(FxNode.sparkles(170, 300.0F, 50.0F, 240.0F, 0.55F, -2235137, 79L));
		stage.add(this.grid);
		this.motes = (MotesNode)stage.add(new MotesNode(110, 64.0F, 13.0F, 44.0F, 0.06F, -1, 80L));
		stage.finish(this.finish);
	}

	void theme(IslandTheme theme, boolean atOnce) {
		if (this.now != theme) {
			this.was = !atOnce && this.now != null && !(Theme.current().motion() <= 0.0F) ? this.now : theme;
			this.now = theme;
			this.turnedAtMs = Clock.nowMs();
			this.stage.lighting().sun(-0.5F, 1.0F, 0.55F).ambient(0.5F);
		}
	}

	int below() {
		return this.was != null && this.now != null ? Colors.lerp(this.was.below, this.now.below, this.turned()) : -15724528;
	}

	private float turned() {
		if (this.was == this.now) {
			return 1.0F;
		} else {
			float motion = Theme.current().motion();
			if (motion <= 0.0F) {
				return 1.0F;
			} else {
				float t = Mth.clamp((float)(Clock.nowMs() - this.turnedAtMs) / (520.0F * motion), 0.0F, 1.0F);
				if (t >= 1.0F) {
					this.was = this.now;
				}

				return t * t * (3.0F - 2.0F * t);
			}
		}
	}

	private static float mix(float t, float a, float b) {
		return a + (b - a) * t;
	}

	void frame(float radius, float far) {
		IslandTheme a = this.was;
		IslandTheme b = this.now;
		if (a != null && b != null) {
			float t = this.turned();
			int below = Colors.lerp(a.below, b.below, t);
			this.stage.background(below);
			this.stage.fog(true, far + radius + 12.0F, far + radius * 2.4F + 150.0F, below);
			this.sky
				.colors(below, Colors.lerp(a.horizon, b.horizon, t), Colors.lerp(a.low, b.low, t), Colors.lerp(a.high, b.high, t), Colors.lerp(a.zenith, b.zenith, t));
			this.sky
				.sun(
					mix(t, a.sunYaw, b.sunYaw),
					mix(t, a.sunEl, b.sunEl),
					Colors.lerp(a.sunColor, b.sunColor, t),
					mix(t, a.sunGlow, b.sunGlow),
					mix(t, a.sunDisc, b.sunDisc),
					mix(t, a.sunReach, b.sunReach)
				);
			this.sky.stars(mix(t, a.stars, b.stars));
			float deep = 9.0F + radius * 0.22F;
			int light = Colors.lerp(a.glow, b.glow, t);
			this.glow.color(light | 0xFF000000).opacity((float)Colors.alpha(light) / 255.0F);
			this.glow.at(0.0F, -deep, 0.0F).scale(radius * 2.6F + 30.0F);
			int cloud = Colors.lerp(a.clouds, b.clouds, t);
			float thick = mix(t, a.cloudOpacity, b.cloudOpacity);
			this.cloudsNear.color(cloud).opacity(thick * 0.85F);
			this.cloudsNear.at(0.0F, -deep - 7.0F, 0.0F);
			this.cloudsFar.color(cloud).opacity(thick * 0.7F);
			this.cloudsFar.at(0.0F, -deep - 24.0F, 0.0F);
			this.starsBelow.opacity(mix(t, a.starsBelow ? 1.0F : 0.0F, b.starsBelow ? 1.0F : 0.0F));
			this.starsBelow.at(0.0F, -deep - 44.0F, 0.0F);
			this.grid.strength = mix(t, a.gridBelow ? 1.0F : 0.0F, b.gridBelow ? 1.0F : 0.0F);
			this.grid.at(0.0F, -deep - 6.0F, 0.0F);
			this.motes.color(Colors.lerp(a.motes, b.motes, t)).rise(mix(t, a.moteRise, b.moteRise)).grow(mix(t, a.moteSize, b.moteSize) / 0.06F);
			float wide = Math.max(1.0F, radius / 26.0F);
			this.motes.at(0.0F, -1.0F, 0.0F).scale(wide, 1.0F, wide);
			StageLighting l = this.stage.lighting();
			float reach = radius + 22.0F;
			l.key(-0.75F * reach, 1.15F * reach, 0.8F * reach)
				.keyColor(mix(t, a.key[0], b.key[0]), mix(t, a.key[1], b.key[1]), mix(t, a.key[2], b.key[2]))
				.ambientColor(mix(t, a.ambient[0], b.ambient[0]), mix(t, a.ambient[1], b.ambient[1]), mix(t, a.ambient[2], b.ambient[2]))
				.fill(
					mix(t, a.fillDir[0], b.fillDir[0]),
					mix(t, a.fillDir[1], b.fillDir[1]),
					mix(t, a.fillDir[2], b.fillDir[2]),
					mix(t, a.fill[0], b.fill[0]),
					mix(t, a.fill[1], b.fill[1]),
					mix(t, a.fill[2], b.fill[2])
				)
				.rim(mix(t, a.rim[0], b.rim[0]), mix(t, a.rim[1], b.rim[1]), mix(t, a.rim[2], b.rim[2]), mix(t, a.rim[3], b.rim[3]))
				.wrap(mix(t, a.wrap, b.wrap))
				.brightness(mix(t, a.brightness, b.brightness));
			this.finish
				.bloom(mix(t, a.bloom, b.bloom))
				.vignette(mix(t, a.vignette, b.vignette))
				.warmth(mix(t, a.warmth, b.warmth))
				.saturation(mix(t, a.saturation, b.saturation))
				.contrast(mix(t, a.contrast, b.contrast));
		}
	}

	private static final class Blueprint extends StageNode {
		private static final int REACH = 132;
		private static final int STEP = 4;
		private static final int PIECES = 6;
		float strength;

		Blueprint() {
			this.pickable(false);
			this.hoverFeel(0.0F, 1.0F);
			this.named("sheet");
		}

		protected void draw(StageRenderContext ctx) {
		}

		public boolean hasTranslucentPass() {
			return true;
		}

		protected void drawTranslucent(StageRenderContext ctx) {
			float a = this.strength * this.alpha();
			if (!(a <= 0.01F)) {
				Matrix4f m = ctx.pose.last().pose();
				RenderSystem.setShader(GameRenderer::getPositionColorShader);
				RenderSystem.enableBlend();
				StageBlend.over();
				RenderSystem.enableDepthTest();
				RenderSystem.depthMask(false);
				RenderSystem.disableCull();
				BufferBuilder bb = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

				for (int i = -132; i <= 132; i += 4) {
					boolean major = Math.floorMod(i, 16) == 0;
					float w = major ? 0.2F : 0.11F;
					float base = (major ? 0.3F : 0.13F) * a;

					for (int k = 0; k < 12; k++) {
						float from = -132.0F + (float)(k * 132) / 6.0F;
						float to = from + 22.0F;
						float a0 = base * fade((float)i, from);
						float a1 = base * fade((float)i, to);
						if (!(a0 <= 0.004F) || !(a1 <= 0.004F)) {
							int c0 = Math.round(255.0F * a0);
							int c1 = Math.round(255.0F * a1);
							bb.addVertex(m, (float)i - w, 0.0F, from).setColor(150, 196, 255, c0);
							bb.addVertex(m, (float)i + w, 0.0F, from).setColor(150, 196, 255, c0);
							bb.addVertex(m, (float)i + w, 0.0F, to).setColor(150, 196, 255, c1);
							bb.addVertex(m, (float)i - w, 0.0F, to).setColor(150, 196, 255, c1);
							bb.addVertex(m, from, 0.0F, (float)i - w).setColor(150, 196, 255, c0);
							bb.addVertex(m, from, 0.0F, (float)i + w).setColor(150, 196, 255, c0);
							bb.addVertex(m, to, 0.0F, (float)i + w).setColor(150, 196, 255, c1);
							bb.addVertex(m, to, 0.0F, (float)i - w).setColor(150, 196, 255, c1);
						}
					}
				}

				MeshData mesh = bb.build();
				if (mesh != null) {
					BufferUploader.drawWithShader(mesh);
				}

				RenderSystem.enableCull();
				RenderSystem.depthMask(true);
			}
		}

		private static float fade(float across, float along) {
			float far = Mth.sqrt(across * across + along * along) / 132.0F;
			return Mth.clamp(1.0F - far, 0.0F, 1.0F);
		}
	}
}
