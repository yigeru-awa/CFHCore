package com.onehumanawa.cfhcore.content.gui.quests.client.view;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class Backdrops {
	public static Backdrops.Kind kind(@Nullable String backdrop) {
		if (backdrop != null && !backdrop.isBlank()) {
			String b = backdrop.trim().toLowerCase(Locale.ROOT);
			if (b.indexOf(58) < 0 && b.indexOf(47) < 0) {
				return switch (b) {
					case "grid" -> Backdrops.Kind.GRID;
					case "meadow" -> Backdrops.Kind.MEADOW;
					case "cave" -> Backdrops.Kind.CAVE;
					case "nether" -> Backdrops.Kind.NETHER;
					case "end" -> Backdrops.Kind.END;
					case "ocean" -> Backdrops.Kind.OCEAN;
					case "parchment" -> Backdrops.Kind.PARCHMENT;
					case "workshop" -> Backdrops.Kind.WORKSHOP;
					default -> Backdrops.Kind.STARS;
				};
			} else {
				return Backdrops.Kind.TEXTURE;
			}
		} else {
			return Backdrops.Kind.STARS;
		}
	}

	public static Backdrops.Kind kind(@Nullable Chapter chapter) {
		return kind(chapter == null ? null : chapter.backdrop);
	}

	public static ResourceLocation texture(@Nullable String backdrop) {
		Backdrops.Kind kind = kind(backdrop);
		if (kind == Backdrops.Kind.TEXTURE) {
			ResourceLocation id = ResourceLocation.tryParse(backdrop.trim().toLowerCase(Locale.ROOT));
			if (id != null) {
				String path = id.getPath();
				return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), path.endsWith(".png") ? path : "textures/" + path + ".png");
			}
		}
		return ResourceLocation.withDefaultNamespace(switch (kind) {
			case GRID -> "textures/block/smooth_stone.png";
			case MEADOW -> "textures/block/moss_block.png";
			case CAVE -> "textures/block/deepslate.png";
			case NETHER -> "textures/gui/advancements/backgrounds/nether.png";
			case END -> "textures/gui/advancements/backgrounds/end.png";
			case OCEAN -> "textures/block/dark_prismarine.png";
			case PARCHMENT -> "textures/block/stripped_birch_log.png";
			case WORKSHOP -> "textures/block/spruce_planks.png";
			default -> "textures/gui/advancements/backgrounds/stone.png";
		});
	}

	public static int ground(Backdrops.Kind kind, int accent) {
		Palette p = Theme.current().palette();
		int base = Colors.mix(p.bg(), -16777216, 0.28F);

		return switch (kind) {
			case GRID -> Colors.mix(base, accent, 0.05F);
			case MEADOW -> Colors.mix(base, -13739478, 0.2F);
			case CAVE -> Colors.mix(base, -13948365, 0.3F);
			case NETHER -> Colors.mix(base, -9823728, 0.24F);
			case END -> Colors.mix(base, -12966826, 0.3F);
			case OCEAN -> Colors.mix(base, -15712674, 0.32F);
			case PARCHMENT -> Colors.mix(base, -9809872, 0.22F);
			case WORKSHOP -> Colors.mix(base, -14010812, 0.34F);
			default -> Colors.mix(base, accent, 0.04F);
		};
	}

	public static int ground(QuestPainter.View view) {
		return ground(kind(view.chapter()), view.accent()) | 0xFF000000;
	}

	public static void draw(GuiGraphics g, QuestPainter.View view, float unit) {
		draw(g, view, unit, 0);
	}

	public static void draw(GuiGraphics g, QuestPainter.View view, float unit, int over) {
		Theme t = Theme.current();
		Rect a = view.area();
		Backdrops.Kind kind = kind(view.chapter());
		float time = view.seconds();
		boolean ground = over == 0;
		float ox = (float)a.x() + (float)a.w() / 2.0F - view.camX() * unit;
		float oy = (float)a.y() + (float)a.h() / 2.0F - view.camY() * unit;
		if (!t.isVanilla() && kind != Backdrops.Kind.TEXTURE) {
			if (ground) {
				g.fill(a.x(), a.y(), a.right(), a.bottom(), t.panel(ground(kind, view.accent())));
			}
		} else {
			if (ground) {
				tiles(g, a, texture(view.chapter() == null ? null : view.chapter().backdrop), ox, oy, Mth.clamp(view.zoom(), 0.5F, 2.0F), t.isVanilla() ? 0.42F : 0.3F);
			}

			if (kind == Backdrops.Kind.TEXTURE) {
				dots(g, a, ox, oy, unit, Colors.withAlpha(-1, 40));
				return;
			}
		}

		int accent = view.accent();
		switch (kind) {
			case GRID:
				grid(g, a, ox, oy, unit, time, accent, 22, 48);
				break;
			case MEADOW:
				meadow(g, a, ox, oy, unit, time);
				break;
			case CAVE:
				cave(g, a, ox, oy, unit, time);
				break;
			case NETHER:
				nether(g, a, ox, oy, unit, time);
				break;
			case END:
				end(g, a, ox, oy, unit, time);
				break;
			case OCEAN:
				ocean(g, a, ox, oy, unit, time);
				break;
			case PARCHMENT:
				parchment(g, a, ox, oy, unit, time);
				break;
			case WORKSHOP:
				workshop(g, a, ox, oy, unit, time, accent, !ground ? over : (t.isVanilla() ? 0 : t.panel(ground(Backdrops.Kind.WORKSHOP, accent))));
				break;
			default:
				stars(g, a, ox, oy, unit, time, accent);
		}
	}

	public static void veil(GuiGraphics g, QuestPainter.View view, float unit, float alpha) {
		if (!(alpha <= 0.004F)) {
			Theme t = Theme.current();
			Rect a = view.area();
			Backdrops.Kind kind = kind(view.chapter());
			if (!t.isVanilla() && kind != Backdrops.Kind.TEXTURE) {
				g.fill(a.x(), a.y(), a.right(), a.bottom(), Colors.scaleAlpha(ground(view), alpha));
			} else {
				float ox = (float)a.x() + (float)a.w() / 2.0F - view.camX() * unit;
				float oy = (float)a.y() + (float)a.h() / 2.0F - view.camY() * unit;
				tiles(
					g, a, texture(view.chapter() == null ? null : view.chapter().backdrop), ox, oy, Mth.clamp(view.zoom(), 0.5F, 2.0F), t.isVanilla() ? 0.42F : 0.3F, alpha
				);
			}
		}
	}

	private static void tiles(GuiGraphics g, Rect a, ResourceLocation texture, float ox, float oy, float scale, float light) {
		tiles(g, a, texture, ox, oy, scale, light, 1.0F);
	}

	private static void tiles(GuiGraphics g, Rect a, ResourceLocation texture, float ox, float oy, float scale, float light, float alpha) {
		float tile = 16.0F * scale;
		float u = (((float)a.x() - ox) % tile + tile) % tile;
		float v = (((float)a.y() - oy) % tile + tile) % tile;
		g.setColor(light, light, light, alpha);
		RenderSystem.enableBlend();
		g.pose().pushPose();
		g.pose().translate((float)a.x() - u, (float)a.y() - v, 0.0F);
		g.pose().scale(scale, scale, 1.0F);
		int w = (int)Math.ceil((double)(((float)a.w() + tile) / scale));
		int h = (int)Math.ceil((double)(((float)a.h() + tile) / scale));
		g.blit(texture, 0, 0, 0.0F, 0.0F, w, h, 16, 16);
		g.pose().popPose();
		g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableBlend();
	}

	private static void dots(GuiGraphics g, Rect a, float ox, float oy, float unit, int color) {
		if (!(unit < 12.0F)) {
			int firstX = (int)Math.floor((double)(((float)a.x() - ox) / unit));
			int lastX = (int)Math.ceil((double)(((float)a.right() - ox) / unit));
			int firstY = (int)Math.floor((double)(((float)a.y() - oy) / unit));
			int lastY = (int)Math.ceil((double)(((float)a.bottom() - oy) / unit));

			for (int iy = firstY; iy <= lastY; iy++) {
				int y = Math.round(oy + (float)iy * unit);

				for (int ix = firstX; ix <= lastX; ix++) {
					int x = Math.round(ox + (float)ix * unit);
					g.fill(x, y, x + 1, y + 1, color);
				}
			}
		}
	}

	private static float rnd(int i, int salt) {
		int h = i * -1640531535 + salt * -2048144789;
		h ^= h >>> 15;
		h *= 739982445;
		h ^= h >>> 12;
		h *= 695872825;
		h ^= h >>> 15;
		return (float)(h & 16777215) / 1.6777216E7F;
	}

	private static float wrap(float v, float span) {
		float m = v % span;
		return m < 0.0F ? m + span : m;
	}

	private static float pulse(float time, float speed, float phase) {
		return 0.5F + 0.5F * Mth.sin((time * speed + phase) * (float) (Math.PI * 2));
	}

	private static void px(GuiGraphics g, float x, float y, int size, int color) {
		int ix = Math.round(x);
		int iy = Math.round(y);
		g.fill(ix, iy, ix + size, iy + size, color);
	}

	private static void glint(GuiGraphics g, float x, float y, int arm, int color) {
		int ix = Math.round(x);
		int iy = Math.round(y);
		g.fill(ix - arm, iy, ix + arm + 1, iy + 1, color);
		g.fill(ix, iy - arm, ix + 1, iy + arm + 1, color);
	}

	private static void scatter(Rect a, int count, int salt, float ox, float oy, float parallax, float driftX, float driftY, float time, Backdrops.Each each) {
		float w = (float)a.w() + 24.0F;
		float h = (float)a.h() + 24.0F;

		for (int i = 0; i < count; i++) {
			float x = (float)a.x() - 12.0F + wrap(rnd(i, salt) * w + ox * parallax + driftX * time * (0.6F + 0.8F * rnd(i, salt + 2)), w);
			float y = (float)a.y() - 12.0F + wrap(rnd(i, salt + 1) * h + oy * parallax + driftY * time * (0.6F + 0.8F * rnd(i, salt + 3)), h);
			each.at(i, x, y);
		}
	}

	private static void stars(GuiGraphics g, Rect a, float ox, float oy, float unit, float time, int accent) {
		dots(g, a, ox, oy, unit, Colors.withAlpha(Theme.current().palette().borderStrong(), unit >= 18.0F ? 80 : 48));
		int far = Colors.mix(-3616544, accent, 0.25F);
		int near = Colors.mix(-1, accent, 0.15F);
		scatter(
			a,
			70,
			11,
			ox,
			oy,
			0.12F,
			0.0F,
			0.0F,
			time,
			(i, x, y) -> px(g, x, y, 1, Colors.withAlpha(far, Math.round(40.0F + 112.0F * pulse(time, 0.12F + 0.2F * rnd(i, 5), rnd(i, 6)))))
		);
		scatter(a, 14, 21, ox, oy, 0.3F, 1.6F, 0.5F, time, (i, x, y) -> {
			float tw = pulse(time, 0.1F + 0.1F * rnd(i, 7), rnd(i, 8));
			if (tw > 0.86F) {
				glint(g, x, y, 1, Colors.withAlpha(near, Math.round(208.0F * (tw - 0.86F) / 0.14F)));
			}

			px(g, x, y, 1, Colors.withAlpha(near, Math.round(96.0F + 128.0F * tw)));
		});
		float every = 11.0F;
		float cycle = (float)Math.floor((double)(time / 11.0F));
		float t = (time - cycle * 11.0F) / 0.9F;
		if (t < 1.0F && time > 0.0F) {
			int c = (int)cycle;
			float sx = (float)a.x() + (float)a.w() * (0.15F + 0.7F * rnd(c, 31));
			float sy = (float)a.y() + (float)a.h() * (0.05F + 0.4F * rnd(c, 32));
			float dx = 150.0F * (rnd(c, 33) < 0.5F ? 1.0F : -1.0F);
			float dy = 55.0F;
			float fade = Mth.sin(t * (float) Math.PI);

			for (int k = 0; k < 4; k++) {
				float t0 = Math.max(0.0F, t - 0.035F * (float)(k + 1));
				float t1 = Math.max(0.0F, t - 0.035F * (float)k);
				CanvasDraw.line(
					g, sx + dx * t0, sy + 55.0F * t0, sx + dx * t1, sy + 55.0F * t1, 1.0F, Colors.withAlpha(near, Math.round(192.0F * fade * (1.0F - (float)k * 0.25F)))
				);
			}
		}
	}

	private static void grid(GuiGraphics g, Rect a, float ox, float oy, float unit, float time, int accent, int thin, int strong) {
		int line = Colors.mix(-1, accent, 0.5F);
		boolean fine = unit >= 14.0F;
		float step = fine ? unit : unit * 4.0F;
		if (step >= 10.0F) {
			int firstX = (int)Math.floor((double)(((float)a.x() - ox) / step));
			int lastX = (int)Math.ceil((double)(((float)a.right() - ox) / step));
			int firstY = (int)Math.floor((double)(((float)a.y() - oy) / step));
			int lastY = (int)Math.ceil((double)(((float)a.bottom() - oy) / step));
			float band = (float)a.x() + wrap(time * 22.0F, (float)a.w() + 240.0F) - 120.0F;

			for (int ix = firstX; ix <= lastX; ix++) {
				int x = Math.round(ox + (float)ix * step);
				boolean major = !fine || Math.floorMod(ix, 4) == 0;
				float glow = Math.max(0.0F, 1.0F - Math.abs((float)x - band) / 90.0F);
				g.fill(x, a.y(), x + 1, a.bottom(), Colors.withAlpha(line, Math.round((float)(major ? strong : thin) * (1.0F + glow))));
			}

			for (int iy = firstY; iy <= lastY; iy++) {
				int y = Math.round(oy + (float)iy * step);
				boolean major = !fine || Math.floorMod(iy, 4) == 0;
				g.fill(a.x(), y, a.right(), y + 1, Colors.withAlpha(line, major ? strong : thin));
			}
		}
	}

	private static void meadow(GuiGraphics g, Rect a, float ox, float oy, float unit, float time) {
		g.fillGradient(a.x(), a.y() + a.h() / 2, a.right(), a.bottom(), 4225586, 943749682);
		dots(g, a, ox, oy, unit, 949010544);
		scatter(
			a,
			34,
			41,
			ox,
			oy,
			0.45F,
			5.0F,
			-1.4F,
			time,
			(i, x, y) -> px(g, x, y + 3.0F * Mth.sin(time * 0.7F + (float)i), 1, Colors.withAlpha(-1511248, Math.round(48.0F + 80.0F * rnd(i, 9))))
		);
		scatter(a, 16, 51, ox, oy, 0.7F, 0.0F, 0.0F, time, (i, x, y) -> {
			float px = x + 14.0F * Mth.sin(time * (0.21F + 0.2F * rnd(i, 1)) + (float)i * 1.7F);
			float py = y + 9.0F * Mth.sin(time * (0.33F + 0.2F * rnd(i, 2)) + (float)i * 0.6F);
			float lit = pulse(time, 0.22F + 0.2F * rnd(i, 3), rnd(i, 4));
			if (!(lit < 0.25F)) {
				float k = (lit - 0.25F) / 0.75F;
				px(g, px - 1.0F, py - 1.0F, 3, Colors.withAlpha(-2559904, Math.round(48.0F * k)));
				px(g, px, py, 1, Colors.withAlpha(-983152, Math.round(232.0F * k)));
			}
		});
	}

	private static void cave(GuiGraphics g, Rect a, float ox, float oy, float unit, float time) {
		g.fillGradient(a.x(), a.y(), a.right(), a.y() + a.h() / 2, 1342177280, 0);
		dots(g, a, ox, oy, unit, 815833264);
		scatter(
			a,
			80,
			61,
			ox,
			oy,
			0.85F,
			0.0F,
			0.0F,
			time,
			(i, x, y) -> px(g, x, y, rnd(i, 1) < 0.2F ? 2 : 1, Colors.withAlpha(rnd(i, 2) < 0.5F ? -7697770 : -11513764, Math.round(40.0F + 56.0F * rnd(i, 3))))
		);
		scatter(
			a,
			22,
			71,
			ox,
			oy,
			0.4F,
			0.8F,
			3.5F,
			time,
			(i, x, y) -> px(g, x + 2.0F * Mth.sin(time * 0.5F + (float)i), y, 1, Colors.withAlpha(-3620688, Math.round(36.0F + 48.0F * rnd(i, 4))))
		);
		int[] gems = new int[]{-9509152, -865972, -2076592, -10954616, -10979096};
		scatter(a, 9, 81, ox, oy, 0.85F, 0.0F, 0.0F, time, (i, x, y) -> {
			float period = 3.4F + 3.0F * rnd(i, 5);
			float t = wrap(time + period * rnd(i, 6), period) / 0.8F;
			int gem = gems[i % gems.length];
			px(g, x, y, 1, Colors.withAlpha(gem, 80));
			if (t < 1.0F) {
				glint(g, x, y, 1, Colors.withAlpha(gem, Math.round(224.0F * Mth.sin(t * (float) Math.PI))));
			}
		});
	}

	private static void nether(GuiGraphics g, Rect a, float ox, float oy, float unit, float time) {
		float breath = 0.8F + 0.2F * Mth.sin(time * 0.9F);
		g.fillGradient(a.x(), a.y() + a.h() * 2 / 5, a.right(), a.bottom(), 13122576, Colors.withAlpha(-3654640, Math.round(88.0F * breath)));
		g.fillGradient(a.x(), a.bottom() - a.h() / 6, a.right(), a.bottom(), 16748576, Colors.withAlpha(-28640, Math.round(56.0F * breath)));
		dots(g, a, ox, oy, unit, 953180224);
		float h = (float)a.h() + 24.0F;
		scatter(a, 40, 91, ox, oy, 0.5F, 0.0F, -13.0F, time, (i, x, y) -> {
			float height = 1.0F - (y - (float)a.y() + 12.0F) / h;
			int color = Colors.lerp(-16320, -7726064, Mth.clamp(height * 1.2F, 0.0F, 1.0F));
			int alpha = Math.round(224.0F * (1.0F - height * 0.75F) * (0.6F + 0.4F * pulse(time, 0.8F + rnd(i, 1), rnd(i, 2))));
			px(g, x + 5.0F * Mth.sin(time * (0.6F + rnd(i, 3)) + (float)i), y, rnd(i, 4) < 0.25F ? 2 : 1, Colors.withAlpha(color, alpha));
		});
	}

	private static void end(GuiGraphics g, Rect a, float ox, float oy, float unit, float time) {
		dots(g, a, ox, oy, unit, 817930480);
		scatter(
			a,
			60,
			101,
			ox,
			oy,
			0.1F,
			0.0F,
			0.0F,
			time,
			(i, x, y) -> px(
					g, x, y, 1, Colors.withAlpha(rnd(i, 1) < 0.3F ? -724800 : -3098376, Math.round(48.0F + 128.0F * pulse(time, 0.08F + 0.15F * rnd(i, 2), rnd(i, 3))))
				)
		);
		scatter(a, 24, 111, ox, oy, 0.35F, -4.0F, 2.6F, time, (i, x, y) -> {
			int size = rnd(i, 4) < 0.3F ? 2 : 1;
			px(g, x, y, size, Colors.withAlpha(rnd(i, 5) < 0.5F ? -5218064 : -9420608, Math.round(80.0F + 112.0F * pulse(time, 0.2F + 0.2F * rnd(i, 6), rnd(i, 7)))));
		});
		scatter(a, 4, 121, ox, oy, 0.2F, 0.0F, 0.0F, time, (i, x, y) -> {
			float period = 5.0F + 4.0F * rnd(i, 8);
			float t = wrap(time + period * rnd(i, 9), period) / 1.2F;
			if (t < 1.0F) {
				glint(g, x, y, 2, Colors.withAlpha(-10426208, Math.round(176.0F * Mth.sin(t * (float) Math.PI))));
			}
		});
	}

	private static void ocean(GuiGraphics g, Rect a, float ox, float oy, float unit, float time) {
		g.fillGradient(a.x(), a.y(), a.right(), a.y() + a.h() / 2, 809541840, 4235472);
		g.fillGradient(a.x(), a.y() + a.h() / 2, a.right(), a.bottom(), 2068, 1879050260);
		dots(g, a, ox, oy, unit, 811643104);

		for (int band = 0; band < 3; band++) {
			float by = (float)a.y() + (float)a.h() * (0.12F + 0.11F * (float)band);
			int alpha = Math.round(20.0F + 18.0F * pulse(time, 0.12F, (float)band * 0.37F));

			for (int x = a.x(); x < a.right(); x += 8) {
				float y = by + 5.0F * Mth.sin((float)x * 0.035F + time * (0.5F + 0.13F * (float)band) + (float)band * 2.0F);
				g.fill(x, Math.round(y), Math.min(a.right(), x + 8), Math.round(y) + 1, Colors.withAlpha(-6233857, alpha));
			}
		}

		scatter(a, 26, 131, ox, oy, 0.5F, 0.0F, -11.0F, time, (i, xx, yx) -> {
			float bx = xx + 4.0F * Mth.sin(time * (0.8F + rnd(i, 1)) + (float)i);
			int c = Colors.withAlpha(-5183233, Math.round(80.0F + 96.0F * rnd(i, 2)));
			if (rnd(i, 3) < 0.3F) {
				int ix = Math.round(bx);
				int iy = Math.round(yx);
				g.fill(ix, iy - 1, ix + 2, iy, c);
				g.fill(ix, iy + 2, ix + 2, iy + 3, c);
				g.fill(ix - 1, iy, ix, iy + 2, c);
				g.fill(ix + 2, iy, ix + 3, iy + 2, c);
			} else {
				px(g, bx, yx, 1, c);
			}
		});
	}

	private static void parchment(GuiGraphics g, Rect a, float ox, float oy, float unit, float time) {
		scatter(a, 110, 141, ox, oy, 1.0F, 0.0F, 0.0F, time, (i, xx, yx) -> {
			int c = rnd(i, 1) < 0.5F ? -1519456 : -14017524;
			int len = 1 + (int)(rnd(i, 2) * 3.0F);
			int ixx = Math.round(xx);
			int iyx = Math.round(yx);
			g.fill(ixx, iyx, ixx + len, iyx + 1, Colors.withAlpha(c, Math.round(20.0F + 28.0F * rnd(i, 3))));
		});
		if (unit >= 14.0F) {
			int ink = 1222690960;
			int firstX = (int)Math.floor((double)(((float)a.x() - ox) / unit));
			int lastX = (int)Math.ceil((double)(((float)a.right() - ox) / unit));
			int firstY = (int)Math.floor((double)(((float)a.y() - oy) / unit));
			int lastY = (int)Math.ceil((double)(((float)a.bottom() - oy) / unit));

			for (int iy = firstY; iy <= lastY; iy++) {
				for (int ix = firstX; ix <= lastX; ix++) {
					if (Math.floorMod(ix + iy, 2) == 0) {
						int x = Math.round(ox + (float)ix * unit);
						int y = Math.round(oy + (float)iy * unit);
						g.fill(x - 1, y, x + 2, y + 1, 1222690960);
						g.fill(x, y - 1, x + 1, y + 2, 1222690960);
					}
				}
			}
		}

		scatter(
			a,
			10,
			151,
			ox,
			oy,
			0.3F,
			1.2F,
			0.9F,
			time,
			(i, xx, yx) -> px(g, xx, yx, 1, Colors.withAlpha(-5968, Math.round(48.0F + 80.0F * pulse(time, 0.15F + 0.1F * rnd(i, 4), rnd(i, 5)))))
		);
	}

	private static void workshop(GuiGraphics g, Rect a, float ox, float oy, float unit, float time, int accent, int hole) {
		int tone = Colors.mix(-8351576, accent, 0.3F);
		float size = Mth.clamp((float)Math.min(a.w(), a.h()) / 290.0F, 0.4F, 1.0F);
		float gx = (float)a.x() + (float)a.w() * 0.16F + ox * 0.06F;
		float gy = (float)a.y() + (float)a.h() * 0.8F + oy * 0.06F;
		gear(g, gx, gy, 4.6F * size, time * 9.0F, Colors.withAlpha(tone, 22), hole);
		gear(g, gx + 96.0F * size, gy - 64.0F * size, 2.6F * size, -time * 15.9F + 11.0F, Colors.withAlpha(tone, 18), hole);
		gear(
			g,
			(float)a.x() + (float)a.w() * 0.88F + ox * 0.06F,
			(float)a.y() + (float)a.h() * 0.2F + oy * 0.06F,
			3.6F * size,
			time * 7.0F + 5.0F,
			Colors.withAlpha(tone, 18),
			hole
		);
		grid(g, a, ox, oy, unit, time, accent, 16, 36);
		scatter(a, 12, 161, ox, oy, 0.4F, 0.0F, 0.0F, time, (i, x, y) -> {
			float period = 2.6F + 3.0F * rnd(i, 1);
			float t = wrap(time + period * rnd(i, 2), period) / 0.7F;
			if (!(t >= 1.0F)) {
				px(g, x + 9.0F * t * (rnd(i, 3) - 0.5F) * 2.0F, y + 12.0F * t * t - 5.0F * t, 1, Colors.withAlpha(-10096, Math.round(208.0F * (1.0F - t))));
			}
		});
	}

	private static void gear(GuiGraphics g, float x, float y, float scale, float degrees, int color, int hole) {
		g.pose().pushPose();
		g.pose().translate(x, y, 0.0F);
		g.pose().mulPose(Axis.ZP.rotationDegrees(degrees));
		g.pose().scale(scale, scale, 1.0F);
		CanvasDraw.shape(g, Quest.Shape.GEAR, 14, color);
		if (hole >>> 24 != 0) {
			CanvasDraw.shape(g, Quest.Shape.CIRCLE, 8, hole);
			CanvasDraw.shape(g, Quest.Shape.CIRCLE, 3, color);
		}

		g.fill(-8, -1, 9, 1, color);
		g.fill(-1, -8, 1, 9, color);
		g.pose().popPose();
	}

	private Backdrops() {
	}

	private interface Each {
		void at(int var1, float var2, float var3);
	}

	public static enum Kind {
		STARS,
		GRID,
		MEADOW,
		CAVE,
		NETHER,
		END,
		OCEAN,
		PARCHMENT,
		WORKSHOP,
		TEXTURE;
	}
}
