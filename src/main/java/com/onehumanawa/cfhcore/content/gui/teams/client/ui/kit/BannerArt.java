package com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.teams.client.Banners;
import com.onehumanawa.cfhcore.content.gui.teams.model.BannerData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPattern;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public final class BannerArt {
	private static final float U0 = 0.015625F;
	private static final float U1 = 0.328125F;
	private static final float V0 = 0.015625F;
	private static final float V1 = 0.640625F;
	private static final int STRIPS = 40;
	private static final ResourceLocation BASE = ResourceLocation.withDefaultNamespace("entity/banner_base");
	private static final List<String> LOOM_ORDER = List.of(
		"square_bottom_left",
		"square_bottom_right",
		"square_top_left",
		"square_top_right",
		"stripe_bottom",
		"stripe_top",
		"stripe_left",
		"stripe_right",
		"stripe_center",
		"stripe_middle",
		"stripe_downright",
		"stripe_downleft",
		"small_stripes",
		"cross",
		"straight_cross",
		"triangle_bottom",
		"triangle_top",
		"triangles_bottom",
		"triangles_top",
		"diagonal_left",
		"diagonal_up_right",
		"diagonal_up_left",
		"diagonal_right",
		"circle",
		"rhombus",
		"half_vertical",
		"half_horizontal",
		"half_vertical_right",
		"half_horizontal_bottom",
		"border",
		"curly_border",
		"gradient",
		"gradient_up",
		"bricks",
		"globe",
		"creeper",
		"skull",
		"flower",
		"mojang",
		"piglin",
		"flow",
		"guster"
	);
	private static final Map<ResourceLocation, ResourceLocation> ASSETS = new HashMap<>();
	@Nullable
	private static RegistryAccess assetsFor;
	@Nullable
	private static List<ResourceLocation> resourcePatterns;

	public static void flag(GuiGraphics g, @Nullable BannerData banner, float x, float y, float w, float alpha) {
		flag(g, banner, x, y, w, w * 2.0F, alpha, 0.0F);
	}

	public static void flag(GuiGraphics g, @Nullable BannerData banner, float x, float y, float w, float h, float alpha, float sway) {
		if (!(alpha <= 0.004F) && !(w <= 0.0F) && !(h <= 0.0F)) {
			BannerData b = banner == null ? new BannerData() : banner;
			TextureAtlas atlas = atlas();
			List<TextureAtlasSprite> sprites = new ArrayList<>(b.layers.size() + 1);
			List<Integer> tints = new ArrayList<>(b.layers.size() + 1);
			sprites.add(atlas.getSprite(BASE));
			tints.add(rgb(Banners.dye(b.base)));

			for (BannerData.Layer l : b.layers) {
				TextureAtlasSprite s = sprite(atlas, ResourceLocation.tryParse(l.pattern));
				if (s != null) {
					sprites.add(s);
					tints.add(rgb(Banners.dye(l.color)));
				}
			}

			draw(g, atlas, sprites, tints, x, y, w, h, alpha, Theme.current().motion() <= 0.0F ? 0.0F : sway);
		}
	}

	public static void pattern(GuiGraphics g, ResourceLocation pattern, DyeColor color, DyeColor base, float x, float y, float w, float h, float alpha) {
		if (!(alpha <= 0.004F)) {
			TextureAtlas atlas = atlas();
			List<TextureAtlasSprite> sprites = new ArrayList<>(2);
			List<Integer> tints = new ArrayList<>(2);
			sprites.add(atlas.getSprite(BASE));
			tints.add(rgb(base));
			TextureAtlasSprite s = sprite(atlas, pattern);
			if (s != null) {
				sprites.add(s);
				tints.add(rgb(color));
			}

			draw(g, atlas, sprites, tints, x, y, w, h, alpha, 0.0F);
		}
	}

	private static void draw(
		GuiGraphics g, TextureAtlas atlas, List<TextureAtlasSprite> sprites, List<Integer> tints, float x, float y, float w, float h, float alpha, float sway
	) {
		g.flush();
		RenderSystem.setShaderTexture(0, atlas.location());
		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		Matrix4f pose = g.pose().last().pose();
		BufferBuilder buf = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		int strips = sway > 0.01F ? 40 : 1;
		double time = (double)Clock.nowMs() / 1000.0;
		int a = Math.round(255.0F * Math.min(1.0F, alpha));

		for (int layer = 0; layer < sprites.size(); layer++) {
			TextureAtlasSprite s = sprites.get(layer);
			int tint = tints.get(layer);
			float su0 = s.getU0() + (s.getU1() - s.getU0()) * 0.015625F;
			float su1 = s.getU0() + (s.getU1() - s.getU0()) * 0.328125F;
			float sv0 = s.getV0() + (s.getV1() - s.getV0()) * 0.015625F;
			float sv1 = s.getV0() + (s.getV1() - s.getV0()) * 0.640625F;

			for (int i = 0; i < strips; i++) {
				float f0 = (float)i / (float)strips;
				float f1 = (float)(i + 1) / (float)strips;
				float dx0 = offset(f0, time, sway);
				float dx1 = offset(f1, time, sway);
				float lean = strips == 1 ? 0.0F : (dx1 - dx0) / Math.max(0.01F, h / (float)strips);
				float shade = Math.max(0.8F, Math.min(1.1F, 1.0F - 0.7F * lean));
				int r = Math.min(255, Math.round((float)Colors.red(tint) * shade));
				int gr = Math.min(255, Math.round((float)Colors.green(tint) * shade));
				int bl = Math.min(255, Math.round((float)Colors.blue(tint) * shade));
				float y0 = y + h * f0;
				float y1 = y + h * f1;
				float v0 = sv0 + (sv1 - sv0) * f0;
				float v1 = sv0 + (sv1 - sv0) * f1;
				buf.addVertex(pose, x + dx0, y0, 0.0F).setUv(su0, v0).setColor(r, gr, bl, a);
				buf.addVertex(pose, x + dx1, y1, 0.0F).setUv(su0, v1).setColor(r, gr, bl, a);
				buf.addVertex(pose, x + w + dx1, y1, 0.0F).setUv(su1, v1).setColor(r, gr, bl, a);
				buf.addVertex(pose, x + w + dx0, y0, 0.0F).setUv(su1, v0).setColor(r, gr, bl, a);
			}
		}

		BufferUploader.drawWithShader(buf.buildOrThrow());
		RenderSystem.disableBlend();
	}

	private static float offset(float f, double time, float sway) {
		return sway <= 0.01F ? 0.0F : (float)(Math.sin(time * 1.7 - (double)f * 3.4) * 0.65 + Math.sin(time * 2.9 - (double)f * 6.1) * 0.35) * sway * f * f;
	}

	private static TextureAtlas atlas() {
		return Minecraft.getInstance().getModelManager().getAtlas(Sheets.BANNER_SHEET);
	}

	@Nullable
	private static TextureAtlasSprite sprite(TextureAtlas atlas, @Nullable ResourceLocation pattern) {
		if (pattern == null) {
			return null;
		} else {
			TextureAtlasSprite s = atlas.getSprite(asset(pattern).withPrefix("entity/banner/"));
			return s.contents().name().equals(MissingTextureAtlasSprite.getLocation()) ? null : s;
		}
	}

	private static ResourceLocation asset(ResourceLocation pattern) {
		RegistryAccess registries = Banners.registries();
		if (registries != assetsFor) {
			ASSETS.clear();
			assetsFor = registries;
		}

		return registries == null ? pattern : ASSETS.computeIfAbsent(pattern, id -> {
			Optional<RegistryLookup<BannerPattern>> lookup = registries.lookup(Registries.BANNER_PATTERN);
			if (lookup.isEmpty()) {
				return (ResourceLocation)id;
			} else {
				Optional<Reference<BannerPattern>> holder = lookup.get().get(ResourceKey.create(Registries.BANNER_PATTERN, id));
				return holder.<ResourceLocation>map(h -> ((BannerPattern)h.value()).assetId()).orElse(id);
			}
		});
	}

	public static List<ResourceLocation> patterns() {
		List<ResourceLocation> all = Banners.patterns();
		if (all.isEmpty()) {
			all = fromResources();
		}

		List<ResourceLocation> out = new ArrayList<>(all);
		out.removeIf(id -> id.getPath().equals("base"));
		out.sort(Comparator.comparingInt(BannerArt::loomIndex).thenComparing(ResourceLocation::toString));
		return out;
	}

	private static int loomIndex(ResourceLocation id) {
		if (!id.getNamespace().equals("minecraft")) {
			return LOOM_ORDER.size() + 1;
		} else {
			int i = LOOM_ORDER.indexOf(id.getPath());
			return i < 0 ? LOOM_ORDER.size() : i;
		}
	}

	private static List<ResourceLocation> fromResources() {
		if (resourcePatterns == null) {
			List<ResourceLocation> found = new ArrayList<>();
			String dir = "textures/entity/banner";

			for (ResourceLocation file : Minecraft.getInstance()
				.getResourceManager()
				.listResources("textures/entity/banner", f -> f.getPath().endsWith(".png"))
				.keySet()) {
				String path = file.getPath();
				String name = path.substring("textures/entity/banner".length() + 1, path.length() - 4);
				if (!name.contains("/")) {
					found.add(ResourceLocation.fromNamespaceAndPath(file.getNamespace(), name));
				}
			}

			resourcePatterns = List.copyOf(found);
		}

		return resourcePatterns;
	}

	public static Component patternName(ResourceLocation pattern, DyeColor color) {
		String key = "block." + pattern.getNamespace() + ".banner." + pattern.getPath() + "." + color.getName();
		return I18n.exists(key) ? Component.translatable(key) : Component.literal(TeamsText.readable(pattern.getPath()));
	}

	public static Component dyeName(DyeColor color) {
		return Component.translatable("color.minecraft." + color.getName());
	}

	public static int rgb(DyeColor color) {
		return 0xFF000000 | color.getTextureDiffuseColor();
	}

	private BannerArt() {
	}
}
