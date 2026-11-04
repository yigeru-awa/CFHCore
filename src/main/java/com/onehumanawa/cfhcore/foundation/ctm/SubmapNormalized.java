package com.onehumanawa.cfhcore.foundation.ctm;

/**
 * Ported from LDLib2's {@code SubmapNormalized}: the same region expressed in relative
 * 0..1 UV space instead of 0..16 texture space.
 */
public class SubmapNormalized implements ISubmap {

	private static final float FACTOR = 16f;

	private final Submap parent;

	SubmapNormalized(Submap submap) {
		this.parent = submap;
	}

	@Override
	public float getXOffset() {
		return parent.getXOffset() / FACTOR;
	}

	@Override
	public float getYOffset() {
		return parent.getYOffset() / FACTOR;
	}

	@Override
	public float getWidth() {
		return parent.getWidth() / FACTOR;
	}

	@Override
	public float getHeight() {
		return parent.getHeight() / FACTOR;
	}

	@Override
	public Submap relativize() {
		return parent;
	}

	@Override
	public SubmapNormalized normalize() {
		return this;
	}

	@Override
	public float[] toArray() {
		return parent.toArray();
	}
}
