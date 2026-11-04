package com.onehumanawa.cfhcore.foundation.ctm;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * A rectangular region of a texture, in either relative 0..1 UV space or in the 0..16
 * pixel-ish space used by vanilla model coordinates.
 * <p>
 * Ported from LDLib2's {@code ISubmap}.
 */
public interface ISubmap {

	float getXOffset();

	float getYOffset();

	float getWidth();

	float getHeight();

	default float getInterpolatedU(TextureAtlasSprite sprite, float u) {
		return sprite.getU(getXOffset() + u / getWidth());
	}

	default float getInterpolatedV(TextureAtlasSprite sprite, float v) {
		return sprite.getV(getYOffset() + v / getWidth());
	}

	float[] toArray();

	ISubmap normalize();

	ISubmap relativize();
}
