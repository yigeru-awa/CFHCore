package com.onehumanawa.cfhcore.content.gui.earlywindow;

import com.onehumanawa.cfhcore.content.gui.earlywindow.scene.Gfx;
import com.onehumanawa.cfhcore.content.gui.earlywindow.scene.PackWindow;
import com.onehumanawa.cfhcore.content.gui.earlywindow.scene.PixelFont;
import com.onehumanawa.cfhcore.content.gui.earlywindow.scene.StartupClock;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Locale;
import net.neoforged.fml.loading.progress.ProgressMeter;
import org.lwjgl.system.MemoryUtil;

final class CFHGuiScene {
	private static final int BG = -15329771;
	private static final int BG2 = -15000806;
	private static final int BORDER = -13421777;
	private static final int TEXT = -1250588;
	private static final int MUTED = -6185065;
	private static final int DIM = -9540506;
	private static final int DANGER = -1750963;
	private static final int SHADOW = 1711276032;
	private static final int PAD = 12;
	private static final int MAX_W = 300;
	private static final int LINE = 10;
	private static final long FADE_IN_NS = 350000000L;
	private final int accent;
	private final int radius;
	private final PackWindow pack;
	private final int bg;
	private final int panel;
	private final int track;
	private final int border;
	private final int ink;
	private final int muted;
	private final int dim;
	private final CFHGuiScene.Face face;
	private final String mcVersion;
	private final String loaderVersion;
	private final boolean timer = CFHGuiLook.startupTimer();
	private final long start = System.nanoTime();
	private final Gfx gfx = new Gfx();
	private ByteBuffer ttf;
	private PixelFont title;
	private PixelFont text;
	private float bakedScale = -1.0F;
	private float shown = -1.0F;
	private long lastFrame = System.nanoTime();

	CFHGuiScene(int accent, int radius, String font, String mcVersion, String loaderVersion, PackWindow pack) {
		this.pack = pack;
		this.accent = pack != null ? pack.accent(accent) : accent;
		this.radius = radius;
		this.bg = pack != null ? pack.background : -15329771;
		this.panel = pack != null ? pack.panel : -266988011;
		this.track = pack != null ? pack.track : -15000806;
		this.border = pack != null ? pack.border : -13421777;
		this.ink = pack != null ? pack.text : -1250588;
		this.muted = pack != null ? pack.muted : -6185065;
		this.dim = pack != null ? pack.dim : -9540506;
		this.face = CFHGuiScene.Face.of(font);
		this.mcVersion = mcVersion;
		this.loaderVersion = loaderVersion;
	}

	void draw(int pixelW, int pixelH, FmlFeed feed) {
		int scale = guiScale(pixelW, pixelH);
		float w = (float)pixelW / (float)scale;
		float h = (float)pixelH / (float)scale;
		this.gfx.begin(pixelW, pixelH, (float)scale);

		try {
			this.fonts((float)scale);
			if (this.pack != null) {
				this.pack.drawBackground(this.gfx, w, h);
			} else {
				this.gfx.rect(0.0F, 0.0F, w, h, this.bg);
				this.gfx.vignette(w, h, 1493172224);
			}

			this.gfx.alpha(ease(Math.min(1.0F, (float)(System.nanoTime() - this.start) / 3.5E8F)));
			this.layout(w, h, feed);
		} finally {
			this.gfx.end();
		}
	}

	void dispose() {
		if (this.pack != null) {
			this.pack.dispose();
		}

		if (this.title != null) {
			this.title.delete();
			this.text.delete();
			this.title = null;
		}

		this.gfx.delete();
		if (this.ttf != null) {
			MemoryUtil.memFree(this.ttf);
		}

		this.ttf = null;
	}

	private static int guiScale(int w, int h) {
		return Math.max(1, Math.min(w / 320, h / 240));
	}

	private void fonts(float scale) {
		if (scale != this.bakedScale) {
			if (this.ttf == null) {
				this.ttf = load("/cfhgui_earlywindow/" + this.face.file());
			}

			if (this.title != null) {
				this.title.delete();
				this.text.delete();
			}

			this.title = PixelFont.bake(this.ttf, this.face.title(), scale, 95, this.face.crisp());
			this.text = PixelFont.bake(this.ttf, this.face.text(), scale, 224, this.face.crisp());
			this.bakedScale = scale;
		}
	}

	private void layout(float w, float h, FmlFeed feed) {
		List<ProgressMeter> subs = feed.steps();
		String log = feed.phaseDetail();
		int cardW = (int)Math.min(w - 24.0F, 300.0F);
		int inner = cardW - 24;
		int fixedH = 51;
		int logH = 16;
		int subH = 17;
		int cardH = 51 + (log != null ? 16 : 0) + subs.size() * 17;
		float capH = this.title.capHeight();
		float[] logo = this.pack != null && this.pack.hasLogo() ? this.pack.logoSize(w - 24.0F) : null;
		String word = this.pack != null ? this.title.fit(this.pack.title, w - 24.0F) : "Minecraft";
		String version = this.pack != null
			? this.text.fit(PackWindow.fill(this.pack.subtitle, this.mcVersion, this.loaderName()), w - 24.0F)
			: "Java Edition " + this.mcVersion;
		String tip = this.pack != null ? this.pack.tip(System.nanoTime() - this.start) : null;
		float headH = (logo != null ? logo[1] : capH + 6.0F + 2.0F) + (float)(version.isEmpty() ? 0 : 13);
		float tipH = tip != null ? 22.0F : 0.0F;
		int gap = 20;
		float y = (float)Math.round(Math.max(8.0F, (h - headH - 20.0F - 101.0F - tipH) * 0.45F));
		int clear = this.accent & 16777215 | 805306368;
		if (logo != null) {
			this.pack.drawLogo(this.gfx, (float)Math.round((w - logo[0]) / 2.0F), y, logo[0], logo[1]);
			y = (float)Math.round(y + logo[1]);
		} else {
			this.title.draw(this.gfx, word, (float)Math.round((w - this.title.width(word)) / 2.0F), y, this.ink);
			y = (float)Math.round(y + capH + 6.0F);
			float cap = (float)(Math.round(Math.min(96.0F, this.title.width(word) * 0.6F) / 2.0F) * 2);
			this.gfx.hgradient((float)Math.round(w / 2.0F) - cap / 2.0F, y, cap / 2.0F, 2.0F, clear, this.accent);
			this.gfx.hgradient((float)Math.round(w / 2.0F), y, cap / 2.0F, 2.0F, this.accent, clear);
			y += 2.0F;
		}

		if (!version.isEmpty()) {
			y += 6.0F;
			this.text.draw(this.gfx, version, (float)Math.round((w - this.text.width(version)) / 2.0F), y, this.muted);
			y += 7.0F;
		}

		y = (float)Math.round(y + 20.0F);
		float x = (float)Math.round((w - (float)cardW) / 2.0F);
		this.gfx.shadow(x, y, (float)cardW, (float)cardH, 1711276032, this.radius);
		this.gfx.pixelRound(x, y, (float)cardW, (float)cardH, this.panel, this.radius);
		this.gfx.rect(x + (float)this.radius + 1.0F, y + 1.0F, (float)(cardW - 2 * this.radius - 2), 1.0F, 352321535);
		this.gfx.outline(x, y, (float)cardW, (float)cardH, this.border, this.radius);
		this.spinner(x + 12.0F - 1.0F, y + 12.0F - 1.0F, 12.0F);
		String label = this.text.fit(feed.phase(), (float)(inner - 16));
		this.text.draw(this.gfx, label, x + 12.0F + 16.0F, y + 12.0F + 1.0F, this.ink);
		float capW = Math.max(16.0F, Math.min(this.text.width(label), 48.0F));
		this.gfx.hgradient(x + 12.0F + 16.0F, y + 12.0F + 11.0F, capW, 2.0F, this.accent, clear);
		float cy = y + 12.0F + 12.0F + 6.0F;
		if (log != null) {
			this.text.draw(this.gfx, this.text.fit(log, (float)inner), x + 12.0F, cy, this.muted);
			cy += 16.0F;
		}

		this.progressRow(x + 12.0F, cy, (float)inner, feed);
		cy += 9.0F;

		for (ProgressMeter m : subs) {
			cy += 6.0F;
			String count = m.steps() > 0 ? Math.min(m.current(), m.steps()) + " / " + m.steps() : "";
			float countW = this.text.width(count);
			this.text.draw(this.gfx, this.text.fit(FmlFeed.label(m), (float)inner - countW - 8.0F), x + 12.0F, cy, this.dim);
			if (!count.isEmpty()) {
				this.text.draw(this.gfx, count, x + 12.0F + (float)inner - countW, cy, this.dim);
			}

			cy += 10.0F;
			this.bar(x + 12.0F, cy, (float)inner, 1.0F, m, this.track);
			cy++;
		}

		if (tip != null) {
			String line = this.text.fit(tip, w - 24.0F);
			this.text.draw(this.gfx, line, (float)Math.round((w - this.text.width(line)) / 2.0F), (float)Math.round(y + (float)cardH + 12.0F), this.muted);
		}

		this.footer(w, h);
	}

	private void progressRow(float x, float y, float w, FmlFeed feed) {
		long now = System.nanoTime();
		float target = Math.max(0.0F, Math.min(1.0F, feed.overall()));
		if (!(this.shown < 0.0F) && !(target < this.shown)) {
			this.shown = this.shown + (target - this.shown) * Math.min(1.0F, (float)(now - this.lastFrame) / 1.0E9F * 8.0F);
		} else {
			this.shown = target;
		}

		this.lastFrame = now;
		String pct = feed.percent() + "%";
		float pctW = this.text.width(pct);
		float barW = Math.max(20.0F, w - pctW - 8.0F);
		this.gfx.rect(x, y + 3.0F, barW, 3.0F, this.border);
		this.gfx.rect(x, y + 3.0F, (float)Math.round(barW * this.shown), 3.0F, this.accent);
		this.text.draw(this.gfx, pct, x + w - pctW, y, this.ink);
	}

	private void footer(float w, float h) {
		float y = (float)Math.round(h - 8.0F - 7.0F);
		String loader = this.pack != null ? this.text.fit(PackWindow.fill(this.pack.footer, this.mcVersion, this.loaderName()), w / 2.0F) : this.loaderName();
		this.text.draw(this.gfx, loader, 10.0F, y, this.dim);
		if (this.timer) {
			this.text
				.draw(
					this.gfx, StartupClock.clock(StartupClock.elapsedMs()), loader.isEmpty() ? 10.0F : (float)Math.round(10.0F + this.text.width(loader) + 12.0F), y, this.dim
				);
		}

		Runtime rt = Runtime.getRuntime();
		long max = rt.maxMemory();
		long used = rt.totalMemory() - rt.freeMemory();
		String mem = String.format(Locale.ROOT, "Memory %.1f / %.1f GB", (double)used / 1.0737418E9F, (double)max / 1.0737418E9F);
		float barW = 40.0F;
		float right = (float)Math.round(w - 10.0F);
		this.text.draw(this.gfx, mem, right - 40.0F - 6.0F - this.text.width(mem), y, this.dim);
		float frac = max > 0L ? Math.min(1.0F, (float)used / (float)max) : 0.0F;
		this.gfx.rect(right - 40.0F, y + 3.0F, 40.0F, 2.0F, this.border);
		this.gfx.rect(right - 40.0F, y + 3.0F, (float)Math.round(40.0F * frac), 2.0F, frac > 0.85F ? -1750963 : this.accent);
	}

	private void spinner(float x, float y, float size) {
		int dots = 8;
		int lit = (int)(System.nanoTime() / 90000000L % 8L);
		float r = (size - 2.0F) / 2.0F;
		float cx = x + size / 2.0F;
		float cy = y + size / 2.0F;
		float dot = (float)Math.max(1, (int)size / 8);

		for (int i = 0; i < 8; i++) {
			double ang = (double)i * Math.PI * 2.0 / 8.0;
			float px = (float)Math.round(cx + (float)Math.cos(ang) * r - dot / 2.0F);
			float py = (float)Math.round(cy + (float)Math.sin(ang) * r - dot / 2.0F);
			int d = (i - lit + 8) % 8;
			float a = d == 0 ? 1.0F : (d == 1 ? 0.6F : (d == 2 ? 0.35F : 0.15F));
			this.gfx.rect(px, py, dot, dot, withAlpha(this.accent, a));
		}
	}

	private void bar(float x, float y, float w, float h, ProgressMeter m, int track) {
		this.gfx.rect(x, y, w, h, track);
		if (m != null && m.steps() > 0) {
			this.gfx.rect(x, y, (float)Math.round(w * Math.max(0.0F, Math.min(1.0F, m.progress()))), h, this.accent);
		} else {
			float seg = Math.max(24.0F, w / 3.0F);
			float phase = (float)(System.nanoTime() / 1000000L % 1400L) / 1400.0F;
			float sx = (float)Math.round(x - seg + (w + seg) * phase);
			float from = Math.max(x, sx);
			float to = Math.min(x + w, sx + seg);
			if (to > from) {
				this.gfx.rect(from, y, to - from, h, this.accent);
			}
		}
	}

	private String loaderName() {
		return "NeoForge " + this.loaderVersion;
	}

	private static float ease(float t) {
		float u = 1.0F - t;
		return 1.0F - u * u * u;
	}

	private static int withAlpha(int argb, float a) {
		return Math.round((float)(argb >>> 24 & 0xFF) * a) << 24 | argb & 16777215;
	}

	private static ByteBuffer load(String resource) {
		byte[] bytes = bytes(resource);
		return MemoryUtil.memAlloc(bytes.length).put(bytes).flip();
	}

	static byte[] bytes(String resource) {
		try {
			byte[] var2;
			try (InputStream in = CFHGuiScene.class.getResourceAsStream(resource)) {
				if (in == null) {
					throw new IllegalStateException("missing " + resource);
				}

				var2 = in.readAllBytes();
			}

			return var2;
		} catch (IOException var6) {
			throw new IllegalStateException("could not read " + resource, var6);
		}
	}

	static int scaleFor(int w, int h) {
		return guiScale(w, h);
	}

	private static record Face(String file, float title, float text, boolean crisp) {
		static CFHGuiScene.Face of(String key) {
			return switch (key) {
				case "monocraft" -> new CFHGuiScene.Face("monocraft.ttf", 27.0F, 9.0F, true);
				case "pixelify" -> new CFHGuiScene.Face("pixelify.ttf", 36.0F, 10.0F, false);
				default -> new CFHGuiScene.Face("pixeloidsans.ttf", 27.0F, 9.0F, true);
			};
		}
	}
}
