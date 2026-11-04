package com.onehumanawa.cfhcore.foundation.ctm;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Ported from LDLib2's {@code Submap}. Holds the sixteen 4x4 tiles and the four 8x8
 * quarters of a 16-unit texture space — the CTM sheet is addressed through this grid:
 * indices 0..15 are the small edge/corner tiles, 16..19 are the quarter tiles.
 */
public class Submap implements ISubmap {

	/** The twenty addressable tiles of a CTM sheet, in 0..16 space. */
	public static final Submap[] uvs = new Submap[] {
			new Submap(4.0F, 4.0F, 0.0F, 0.0F),
			new Submap(4.0F, 4.0F, 4.0F, 0.0F),
			new Submap(4.0F, 4.0F, 8.0F, 0.0F),
			new Submap(4.0F, 4.0F, 12.0F, 0.0F),
			new Submap(4.0F, 4.0F, 0.0F, 4.0F),
			new Submap(4.0F, 4.0F, 4.0F, 4.0F),
			new Submap(4.0F, 4.0F, 8.0F, 4.0F),
			new Submap(4.0F, 4.0F, 12.0F, 4.0F),
			new Submap(4.0F, 4.0F, 0.0F, 8.0F),
			new Submap(4.0F, 4.0F, 4.0F, 8.0F),
			new Submap(4.0F, 4.0F, 8.0F, 8.0F),
			new Submap(4.0F, 4.0F, 12.0F, 8.0F),
			new Submap(4.0F, 4.0F, 0.0F, 12.0F),
			new Submap(4.0F, 4.0F, 4.0F, 12.0F),
			new Submap(4.0F, 4.0F, 8.0F, 12.0F),
			new Submap(4.0F, 4.0F, 12.0F, 12.0F),
			new Submap(8.0F, 8.0F, 0.0F, 0.0F),
			new Submap(8.0F, 8.0F, 8.0F, 0.0F),
			new Submap(8.0F, 8.0F, 0.0F, 8.0F),
			new Submap(8.0F, 8.0F, 8.0F, 8.0F) };

	public static final Submap FULL_TEXTURE = new Submap(16.0F, 16.0F, 0.0F, 0.0F);

	public final float width, height;
	public final float xOffset, yOffset;

	private final SubmapNormalized normalized = new SubmapNormalized(this);

	public Submap(float width, float height, float xOffset, float yOffset) {
		this.width = width;
		this.height = height;
		this.xOffset = xOffset;
		this.yOffset = yOffset;
	}

	@Override
	public float getXOffset() {
		return xOffset;
	}

	@Override
	public float getYOffset() {
		return yOffset;
	}

	@Override
	public float getWidth() {
		return width;
	}

	@Override
	public float getHeight() {
		return height;
	}

	@Override
	public float getInterpolatedU(TextureAtlasSprite sprite, float u) {
		return sprite.getU(xOffset + u / width);
	}

	@Override
	public float getInterpolatedV(TextureAtlasSprite sprite, float v) {
		return sprite.getV(yOffset + v / width);
	}

	@Override
	public float[] toArray() {
		return new float[] { xOffset, yOffset, xOffset + width, yOffset + height };
	}

	@Override
	public SubmapNormalized normalize() {
		return normalized;
	}

	@Override
	public Submap relativize() {
		return this;
	}
}
