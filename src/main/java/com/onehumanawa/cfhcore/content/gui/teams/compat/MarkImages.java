package com.onehumanawa.cfhcore.content.gui.teams.compat;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.teams.CFHGuiTeams;
import java.io.InputStream;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.Nullable;

public final class MarkImages {
	private static final int RIM = -15724526;
	public static final int HEAD_SIZE = 24;
	public static final int WAYPOINT_SIZE = 28;
	public static final int PING_SIZE = 32;
	private final String owner;
	private final Map<MapMark, MarkImages.Head> heads = new IdentityHashMap<>();
	private final Map<Long, ResourceLocation> stills = new HashMap<>();
	private int made;
	@Nullable
	private NativeImage glyphs;
	private boolean glyphsTried;

	public MarkImages(String owner) {
		this.owner = owner;
	}

	public ResourceLocation head(MapMark mark) {
		MarkImages.Head head = this.heads.get(mark);
		if (head == null) {
			DynamicTexture texture = new DynamicTexture(new NativeImage(24, 24, true));
			head = new MarkImages.Head(CFHGuiTeams.id("map/" + this.owner + "/head/" + this.made++), texture);
			Minecraft.getInstance().getTextureManager().register(head.location, texture);
			this.heads.put(mark, head);
			draw(head, mark);
		}

		return head.location;
	}

	public void tick() {
		for (Entry<MapMark, MarkImages.Head> e : this.heads.entrySet()) {
			MapMark mark = e.getKey();
			MarkImages.Head head = e.getValue();
			boolean changed = head.color != mark.color || head.dead != mark.dead || !mark.skin().equals(head.skin);
			if (changed || --head.check <= 0) {
				draw(head, mark);
			}
		}
	}

	private static void draw(MarkImages.Head head, MapMark mark) {
		NativeImage out = head.texture.getPixels();
		if (out != null) {
			ResourceLocation skin = mark.skin();
			boolean changedSkin = !skin.equals(head.skin);
			head.skin = skin;
			head.color = mark.color;
			head.dead = mark.dead;
			head.interval = changedSkin ? 20 : Math.min(200, Math.max(20, head.interval * 2));
			head.check = head.interval;
			int drawn = compose(out, skin, mark.color, mark.dead);
			if (drawn != head.drawn) {
				head.drawn = drawn;
				head.texture.upload();
			}
		}
	}

	private static int compose(NativeImage out, ResourceLocation skin, int rgb, boolean dead) {
		int rim = abgr(-15724526);
		int ring = abgr(0xFF000000 | rgb);

		for (int y = 0; y < 24; y++) {
			for (int x = 0; x < 24; x++) {
				boolean edge = x < 2 || y < 2 || x >= 22 || y >= 22;
				out.setPixelRGBA(x, y, edge ? rim : ring);
			}
		}

		try {
			NativeImage source = download(skin);

			try {
				if (source != null) {
					face(out, source, dead);
				}
			} catch (Throwable var10) {
				if (source != null) {
					try {
						source.close();
					} catch (Throwable var9) {
						var10.addSuppressed(var9);
					}
				}

				throw var10;
			}

			if (source != null) {
				source.close();
			}
		} catch (Throwable var11) {
		}

		int h = 1;

		for (int y = 0; y < 24; y++) {
			for (int x = 0; x < 24; x++) {
				h = h * 31 + out.getPixelRGBA(x, y);
			}
		}

		return h;
	}

	private static void face(NativeImage out, NativeImage skin, boolean dead) {
		int scale = Math.max(1, skin.getWidth() / 64);
		boolean hat = skin.getHeight() >= 64 * scale;

		for (int v = 0; v < 8; v++) {
			for (int u = 0; u < 8; u++) {
				int c = skin.getPixelRGBA((8 + u) * scale, (8 + v) * scale);
				if (hat) {
					int over = skin.getPixelRGBA((40 + u) * scale, (8 + v) * scale);
					if (over >>> 24 != 0) {
						c = blend(c, over);
					}
				}

				if (c >>> 24 == 0) {
					c = abgr(-15724526);
				}

				if (dead) {
					c = blend(c, abgr(-1341124590));
				}

				c |= -16777216;

				for (int dy = 0; dy < 2; dy++) {
					for (int dx = 0; dx < 2; dx++) {
						out.setPixelRGBA(4 + u * 2 + dx, 4 + v * 2 + dy, c);
					}
				}
			}
		}
	}

	@Nullable
	private static NativeImage download(ResourceLocation skin) {
		TextureManager textures = Minecraft.getInstance().getTextureManager();
		AbstractTexture texture = textures.getTexture(skin);
		texture.bind();
		int w = GlStateManager._getTexLevelParameter(3553, 0, 4096);
		int h = GlStateManager._getTexLevelParameter(3553, 0, 4097);
		if (w >= 64 && h >= 32 && w <= 1024 && h <= 1024) {
			NativeImage image = new NativeImage(w, h, false);
			image.downloadTexture(0, false);
			return image;
		} else {
			return null;
		}
	}

	public ResourceLocation waypoint(MapMark mark) {
		return this.still(0, mark, 28);
	}

	public ResourceLocation ping(MapMark mark) {
		return this.still(1, mark, 32);
	}

	private ResourceLocation still(int shape, MapMark mark, int size) {
		Icon icon = mark.icon == null ? Icon.PIN : mark.icon;
		long id = (long)shape << 48 | (long)icon.ordinal() << 24 | (long)(mark.color & 16777215);
		ResourceLocation location = this.stills.get(id);
		if (location == null) {
			NativeImage image = new NativeImage(size, size, true);
			if (shape == 0) {
				plate(image, mark.color);
			} else {
				disc(image, mark.color);
			}

			this.glyph(image, icon, mark.color);
			location = CFHGuiTeams.id("map/" + this.owner + "/still/" + this.made++);
			Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
			this.stills.put(id, location);
		}

		return location;
	}

	private static void plate(NativeImage out, int rgb) {
		int rim = abgr(-15724526);
		int fill = abgr(0xFF000000 | rgb);

		for (int y = 0; y < 28; y++) {
			for (int x = 0; x < 28; x++) {
				int ux = x / 2;
				int uy = y / 2;
				out.setPixelRGBA(x, y, stepped(ux - 1, uy - 1, 12, 2) ? fill : (stepped(ux, uy, 14, 3) ? rim : 0));
			}
		}
	}

	private static boolean stepped(int x, int y, int size, int r) {
		if (x >= 0 && y >= 0 && x < size && y < size) {
			int inset = y < r ? r - y : (y >= size - r ? r - (size - 1 - y) : 0);
			return x >= inset && x < size - inset;
		} else {
			return false;
		}
	}

	private static void disc(NativeImage out, int rgb) {
		int rim = abgr(-15724526);
		int fill = abgr(0xFF000000 | rgb);
		int halo = abgr(-1879048192 | rgb);
		float c = 15.5F;

		for (int y = 0; y < 32; y++) {
			for (int x = 0; x < 32; x++) {
				double d = Math.hypot((double)((float)x - 15.5F), (double)((float)y - 15.5F));
				out.setPixelRGBA(x, y, d <= 9.5 ? fill : (d <= 11.5 ? rim : (d >= 13.0 && d < 15.0 ? halo : 0)));
			}
		}
	}

	private void glyph(NativeImage out, Icon icon, int rgb) {
		NativeImage atlas = this.glyphs();
		if (atlas != null) {
			int tint = Colors.readableOn(0xFF000000 | rgb);
			int left = (out.getWidth() - 16) / 2;
			int top = (out.getHeight() - 16) / 2;

			for (int v = 0; v < 16; v++) {
				for (int u = 0; u < 16; u++) {
					int a = atlas.getPixelRGBA(icon.u() + u, icon.v() + v) >>> 24;
					if (a != 0) {
						out.setPixelRGBA(left + u, top + v, blend(out.getPixelRGBA(left + u, top + v), abgr(Colors.withAlpha(tint, a))));
					}
				}
			}
		}
	}

	@Nullable
	private NativeImage glyphs() {
		if (this.glyphs == null && !this.glyphsTried) {
			this.glyphsTried = true;

			try {
				Resource resource = Minecraft.getInstance().getResourceManager().getResourceOrThrow(Icons.ATLAS);

				try (InputStream in = resource.open()) {
					this.glyphs = NativeImage.read(in);
				}
			} catch (Exception var7) {
				CFHGuiTeams.LOGGER.warn("[CFHGui Teams] the glyphs could not be read for the map's waypoints and pings: {}", var7.toString());
			}

			return this.glyphs;
		} else {
			return this.glyphs;
		}
	}

	public void release(MapMark mark) {
		MarkImages.Head head = this.heads.remove(mark);
		if (head != null) {
			Minecraft.getInstance().getTextureManager().release(head.location);
		}
	}

	public void clear() {
		TextureManager textures = Minecraft.getInstance().getTextureManager();

		for (MarkImages.Head head : this.heads.values()) {
			textures.release(head.location);
		}

		this.heads.clear();

		for (ResourceLocation location : this.stills.values()) {
			textures.release(location);
		}

		this.stills.clear();
		if (this.glyphs != null) {
			this.glyphs.close();
			this.glyphs = null;
		}

		this.glyphsTried = false;
	}

	private static int abgr(int argb) {
		return argb & -16711936 | argb >> 16 & 0xFF | (argb & 0xFF) << 16;
	}

	private static int blend(int under, int over) {
		int a = over >>> 24;
		if (a >= 255) {
			return over;
		} else if (a == 0) {
			return under;
		} else {
			int r = ((over & 0xFF) * a + (under & 0xFF) * (255 - a)) / 255;
			int g = ((over >> 8 & 0xFF) * a + (under >> 8 & 0xFF) * (255 - a)) / 255;
			int b = ((over >> 16 & 0xFF) * a + (under >> 16 & 0xFF) * (255 - a)) / 255;
			int ua = under >>> 24;
			int oa = a + ua * (255 - a) / 255;
			return oa << 24 | b << 16 | g << 8 | r;
		}
	}

	private static final class Head {
		final ResourceLocation location;
		final DynamicTexture texture;
		ResourceLocation skin;
		int color;
		int drawn;
		boolean dead;
		int check;
		int interval;

		Head(ResourceLocation location, DynamicTexture texture) {
			this.location = location;
			this.texture = texture;
		}
	}
}
