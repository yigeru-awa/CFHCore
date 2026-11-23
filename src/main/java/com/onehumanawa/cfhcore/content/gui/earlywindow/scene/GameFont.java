package com.onehumanawa.cfhcore.content.gui.earlywindow.scene;

import org.lwjgl.opengl.GL32C;

final class GameFont {
	private static final int FIRST = 32;
	private static final int LAST = 126;
	private static final int SPACE = 4;
	private final Picture picture;
	private final int cell;
	private final int[] advance = new int[127];
	private int texture;

	private GameFont(Picture picture) {
		this.picture = picture;
		this.cell = picture.w() / 16;

		for (int c = 32; c <= 126; c++) {
			int x0 = c % 16 * this.cell;
			int y0 = c / 16 * this.cell;
			int width = 0;

			for (int i = this.cell - 1; i >= 0 && width == 0; i--) {
				for (int j = 0; j < this.cell; j++) {
					if (picture.argb()[(y0 + j) * picture.w() + x0 + i] >>> 24 != 0) {
						width = i + 1;
						break;
					}
				}
			}

			this.advance[c] = c == 32 ? 4 : Math.round((float)width * 8.0F / (float)this.cell) + 1;
		}
	}

	static GameFont read(Assets assets) {
		Picture p = assets == null ? null : Picture.read(assets.read("minecraft", "textures/font/ascii.png"));
		return p != null && p.w() == p.h() && p.w() % 16 == 0 ? new GameFont(p) : null;
	}

	void delete() {
		if (this.texture != 0) {
			GL32C.glDeleteTextures(this.texture);
		}

		this.texture = 0;
	}

	private static int glyph(char c) {
		return c >= 32 && c <= 126 ? c : 63;
	}

	float width(String s) {
		float w = 0.0F;

		for (int i = 0; i < s.length(); i++) {
			w += (float)this.advance[glyph(s.charAt(i))];
		}

		return w;
	}

	String fit(String s, float maxWidth) {
		if (this.width(s) <= maxWidth) {
			return s;
		} else {
			float dots = this.width("...");
			int end = s.length();

			while (end > 0 && this.width(s.substring(0, end)) + dots > maxWidth) {
				end--;
			}

			return s.substring(0, end).stripTrailing() + "...";
		}
	}

	void draw(Gfx g, String s, float x, float y, int argb) {
		if (this.texture == 0) {
			this.texture = this.picture.texture(true);
		}

		float side = (float)this.picture.w();
		float at = x;

		for (int i = 0; i < s.length(); i++) {
			int c = glyph(s.charAt(i));
			if (c != 32) {
				float u = (float)(c % 16 * this.cell) / side;
				float v = (float)(c / 16 * this.cell) / side;
				float span = (float)this.cell / side;
				g.image(this.texture, at, y, 8.0F, 8.0F, u, v, u + span, v + span, argb);
			}

			at += (float)this.advance[c];
		}
	}
}
