package com.onehumanawa.cfhcore.content.gui.teams.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

final class HudDraw {
	static Theme theme() {
		return Theme.variant(Theme.globalStyle());
	}

	static boolean vanilla() {
		return theme().isVanilla();
	}

	static int textColor() {
		return vanilla() ? -1 : theme().palette().text();
	}

	static int mutedColor() {
		return vanilla() ? -3750202 : theme().palette().textMuted();
	}

	static int dimColor() {
		return vanilla() ? -6645094 : theme().palette().textDim();
	}

	static int readable(int rgb) {
		int c = 0xFF000000 | rgb;

		for (int i = 0; i < 6 && Colors.luminance(c) < 0.42F; i++) {
			c = Colors.mix(c, -1, 0.18F);
		}

		return c;
	}

	static int healthColor(float fraction) {
		Palette p = theme().palette();
		if (fraction >= 0.55F) {
			return p.success();
		} else if (fraction >= 0.4F) {
			return Colors.lerp(p.warning(), p.success(), (fraction - 0.4F) / 0.15F);
		} else if (fraction >= 0.28F) {
			return p.warning();
		} else {
			return fraction >= 0.18F ? Colors.lerp(p.danger(), p.warning(), (fraction - 0.18F) / 0.1F) : p.danger();
		}
	}

	static void batch(GuiGraphics g, Runnable draw) {
		g.drawManaged(draw);
	}

	static void plate(GuiGraphics g, int x, int y, int w, int h, float alpha) {
		plate(g, x, y, w, h, theme().radius(), alpha);
	}

	static void pill(GuiGraphics g, int x, int y, int w, int h, float alpha) {
		plate(g, x, y, w, h, Math.min(2, theme().radius()), alpha);
	}

	static void notice(GuiGraphics g, int x, int y, int w, int h, float alpha) {
		if (!vanilla()) {
			plate(g, x, y, w, h, alpha);
		} else if (!(alpha <= 0.01F) && w > 0 && h > 0) {
			CFHGuiDraw.rect(g, x, y, w, h, Colors.scaleAlpha(-603979776, alpha));
			CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(-16777216, alpha), 0);
			CFHGuiDraw.outline(g, x + 1, y + 1, w - 2, h - 2, Colors.scaleAlpha(1224736767, alpha), 0);
		}
	}

	static void plate(GuiGraphics g, int x, int y, int w, int h, int radius, float alpha) {
		if (!(alpha <= 0.01F) && w > 0 && h > 0) {
			Theme t = theme();
			if (t.isVanilla()) {
				CFHGuiDraw.rect(g, x, y, w, h, Colors.scaleAlpha(-1879048192, alpha));
			} else {
				Palette p = t.palette();
				CFHGuiDraw.shadow(g, x, y, w, h, Colors.scaleAlpha(p.shadow(), alpha), radius);
				CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(Colors.withAlpha(p.bg(), t.translucentPanels() ? 196 : 236), alpha), radius);
				CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(p.border(), alpha), radius);
			}
		}
	}

	static void text(GuiGraphics g, Component text, int x, int y, int color, float alpha) {
		text(g, text.getVisualOrderText(), x, y, color, alpha);
	}

	static void text(GuiGraphics g, String text, int x, int y, int color, float alpha) {
		int c = Colors.scaleAlpha(color, alpha);
		if (Colors.alpha(c) >= 8) {
			g.drawString(CFHGuiDraw.font(), text, x, y, c, vanilla());
		}
	}

	static void text(GuiGraphics g, FormattedCharSequence text, int x, int y, int color, float alpha) {
		int c = Colors.scaleAlpha(color, alpha);
		if (Colors.alpha(c) >= 8) {
			g.drawString(CFHGuiDraw.font(), text, x, y, c, vanilla());
		}
	}

	static Component distance(double blocks) {
		return blocks < 999.5
			? Component.translatable("cfhgui_teams.hud.distance.m", new Object[]{Integer.toString((int)Math.round(blocks))})
			: Component.translatable("cfhgui_teams.hud.distance.km", new Object[]{String.format(Locale.ROOT, "%.1f", blocks / 1000.0)});
	}

	static Component dimensionName(String id) {
		int colon = id.indexOf(58);
		String namespace = colon < 0 ? "minecraft" : id.substring(0, colon);
		String path = colon < 0 ? id : id.substring(colon + 1);
		String key = "cfhgui_teams.hud.dimension." + namespace + "." + path.replace('/', '.');
		if (I18n.exists(key)) {
			return Component.translatable(key);
		} else {
			StringBuilder sb = new StringBuilder();
			boolean up = true;

			for (char c : path.substring(path.lastIndexOf(47) + 1).toCharArray()) {
				if (c == '_') {
					sb.append(' ');
					up = true;
				} else {
					sb.append(up ? Character.toUpperCase(c) : c);
					up = false;
				}
			}

			return Component.literal(sb.toString());
		}
	}

	static void bar(GuiGraphics g, int x, int y, int w, int h, float fraction, int color, float alpha) {
		if (!(alpha <= 0.01F) && w > 0) {
			Theme t = theme();
			CFHGuiDraw.rect(g, x, y, w, h, Colors.scaleAlpha(t.isVanilla() ? -533186504 : t.palette().surfaceActive(), alpha));
			int fill = Math.round((float)w * Mth.clamp(fraction, 0.0F, 1.0F));
			if (fill > 0) {
				CFHGuiDraw.rect(g, x, y, fill, h, Colors.scaleAlpha(color, alpha));
			}
		}
	}

	static void face(GuiGraphics g, ResourceLocation skin, int x, int y, int size, int tint, float alpha) {
		if (!(alpha <= 0.01F)) {
			RenderSystem.enableBlend();
			g.setColor((float)Colors.red(tint) / 255.0F, (float)Colors.green(tint) / 255.0F, (float)Colors.blue(tint) / 255.0F, alpha);
			PlayerFaceRenderer.draw(g, skin, x, y, size, true, false);
			g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.disableBlend();
		}
	}

	static void badge(GuiGraphics g, Icon icon, int x, int y, int size, int color, float alpha) {
		if (!(alpha <= 0.01F)) {
			int c = Colors.withAlpha(color, 255);
			if (vanilla()) {
				CFHGuiDraw.rect(g, x, y, size, size, Colors.scaleAlpha(-16777216, alpha));
				CFHGuiDraw.rect(g, x + 1, y + 1, size - 2, size - 2, Colors.scaleAlpha(c, alpha));
			} else {
				CFHGuiDraw.pixelRound(g, x, y, size, size, Colors.scaleAlpha(c, alpha), 2);
			}

			int glyph = size >= 20 ? 16 : (size >= 15 ? 12 : 8);
			Icons.draw(g, icon, x + (size - glyph) / 2, y + (size - glyph) / 2, glyph, Colors.scaleAlpha(Colors.readableOn(c), alpha));
		}
	}

	static void caret(GuiGraphics g, int cx, int tipY, int color, float alpha) {
		int c = Colors.scaleAlpha(color, alpha);
		if (Colors.alpha(c) != 0) {
			g.fill(cx - 2, tipY - 3, cx + 3, tipY - 2, c);
			g.fill(cx - 1, tipY - 2, cx + 2, tipY - 1, c);
			g.fill(cx, tipY - 1, cx + 1, tipY, c);
		}
	}

	static void stem(GuiGraphics g, int tipX, int tipY, int labelX, int footY, int labelW, int color, float alpha) {
		g.pose().pushPose();
		g.pose().translate(0.0F, 0.0F, -10.0F);
		if (footY < tipY - 3) {
			CFHGuiDraw.rect(g, Mth.clamp(tipX, labelX + 3, labelX + labelW - 4), footY, 1, tipY - 3 - footY, Colors.scaleAlpha(color, alpha * 0.7F));
		}

		caret(g, tipX, tipY, color, alpha);
		g.pose().popPose();
	}

	static void dart(GuiGraphics g, float cx, float cy, float angle, float size, int color, float alpha) {
		int c = Colors.scaleAlpha(color, alpha);
		if (Colors.alpha(c) != 0) {
			int rim = Colors.scaleAlpha(vanilla() ? -536870912 : Colors.withAlpha(theme().palette().bg(), 224), alpha);
			VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
			Matrix4f m = g.pose().last().pose();
			dartShape(vc, m, cx, cy, angle, size + 1.6F, rim);
			dartShape(vc, m, cx, cy, angle, size, c);
			g.flush();
		}
	}

	private static void dartShape(VertexConsumer vc, Matrix4f m, float cx, float cy, float angle, float s, int color) {
		float sin = Mth.sin(angle);
		float cos = Mth.cos(angle);
		float tx = cx + sin * s;
		float ty = cy - cos * s;
		float bx = 0.6F * s;
		float by = 0.74F * s;
		float ny = 0.3F * s;
		float lx = cx - bx * cos - by * sin;
		float ly = cy - bx * sin + by * cos;
		float rx = cx + bx * cos - by * sin;
		float ry = cy + bx * sin + by * cos;
		float nx = cx - ny * sin;
		float nyy = cy + ny * cos;
		tri(vc, m, tx, ty, lx, ly, nx, nyy, color);
		tri(vc, m, tx, ty, nx, nyy, rx, ry, color);
	}

	static void tri(VertexConsumer vc, Matrix4f m, float ax, float ay, float bx, float by, float cx, float cy, int color) {
		if ((bx - ax) * (cy - ay) - (by - ay) * (cx - ax) > 0.0F) {
			float sx = bx;
			float sy = by;
			bx = cx;
			by = cy;
			cx = sx;
			cy = sy;
		}

		vc.addVertex(m, ax, ay, 0.0F).setColor(color);
		vc.addVertex(m, bx, by, 0.0F).setColor(color);
		vc.addVertex(m, cx, cy, 0.0F).setColor(color);
		vc.addVertex(m, cx, cy, 0.0F).setColor(color);
	}

	static void edgePoint(int w, int h, float dirX, float dirY, int left, int top, int right, int bottom, float[] out) {
		float cx = (float)w / 2.0F;
		float cy = (float)h / 2.0F;
		float tx = dirX > 1.0E-4F ? ((float)(w - right) - cx) / dirX : (dirX < -1.0E-4F ? ((float)left - cx) / dirX : Float.MAX_VALUE);
		float ty = dirY > 1.0E-4F ? ((float)(h - bottom) - cy) / dirY : (dirY < -1.0E-4F ? ((float)top - cy) / dirY : Float.MAX_VALUE);
		float t = Math.min(tx, ty);
		out[0] = cx + dirX * t;
		out[1] = cy + dirY * t;
	}

	private HudDraw() {
	}

	static final class Distance {
		private int said = Integer.MIN_VALUE;
		Component text = Component.empty();
		int width;

		HudDraw.Distance of(double blocks) {
			int say = blocks < 999.5 ? (int)Math.round(blocks) : 100000 + (int)Math.round(blocks / 100.0);
			if (say != this.said) {
				this.said = say;
				this.text = HudDraw.distance(blocks);
				this.width = CFHGuiDraw.width(this.text);
			}

			return this;
		}
	}
}
