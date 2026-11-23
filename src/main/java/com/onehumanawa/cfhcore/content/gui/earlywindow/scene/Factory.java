package com.onehumanawa.cfhcore.content.gui.earlywindow.scene;

abstract class Factory {
	static final float BASE = 0.375F;
	static final float BELT_SPEED = 1.9F;
	float beltY;
	float lumpX0;
	float lumpX1;
	Path way;
	float pressAt;
	float pressX;
	float[] bounds;
	final float[] target = new float[3];
	float pitch = 15.0F;
	final float[] stage = new float[4];
	final float[] memoryAt = new float[3];
	final float[] taskFrom = new float[3];
	final float[] taskTo = new float[3];
	float[][] over = new float[0][];
	final float[] totalAt = new float[3];

	abstract void lamps(Motion var1, SceneGl.Light var2, int var3);

	abstract void build(Motion var1, Frame var2, int var3);

	abstract float leaving(Motion var1);

	void free() {
	}

	final void planned(float x0, float z0, float x1, float z1, float[]... tall) {
		this.bounds = new float[24 * (tall.length + 1)];
		System.arraycopy(Camera.box(x0 - 0.05F, 0.0F, z0 - 0.05F, x1 + 0.05F, 0.375F, z1 + 0.05F), 0, this.bounds, 0, 24);

		for (int i = 0; i < tall.length; i++) {
			float[] b = tall[i];
			System.arraycopy(Camera.box(b[0], b[1], b[2], b[3], b[4], b[5]), 0, this.bounds, 24 * (i + 1), 24);
		}

		this.stage[0] = (x0 + x1) / 2.0F;
		this.stage[1] = (z0 + z1) / 2.0F;
		this.stage[2] = (x1 - x0) / 2.0F;
		this.stage[3] = (z1 - z0) / 2.0F;
		this.target[0] = this.stage[0];
		this.target[1] = 1.7F;
		this.target[2] = 0.0F;
	}

	final void place(float along, float[] out) {
		if (along >= 0.0F) {
			this.way.at(along, out);
		} else {
			out[0] = Math.max(this.lumpX0, this.lumpX1 + along);
			out[1] = this.beltY;
			out[2] = 0.0F;
		}
	}

	static void set(float[] point, float x, float y, float z) {
		point[0] = x;
		point[1] = y;
		point[2] = z;
	}

	static void lamp(SceneGl.Light light, int i, float x, float y, float z, float reach, float r, float g, float b) {
		light.lamp[i * 4] = x;
		light.lamp[i * 4 + 1] = y;
		light.lamp[i * 4 + 2] = z;
		light.lamp[i * 4 + 3] = reach;
		light.lampColour[i * 3] = r;
		light.lampColour[i * 3 + 1] = g;
		light.lampColour[i * 3 + 2] = b;
	}

	static void air(Motion m, Frame frame, float x0, float x1, float dim) {
		for (Motion.Spark s : m.sparks) {
			float t = s.life / s.span;
			if (s.smoke) {
				frame.halo(s.x, s.y, s.z, s.size * (1.0F + 1.6F * t), s.colour, (1.0F - t) * 0.1F);
			} else {
				frame.halo(s.x, s.y, s.z, s.size * (1.0F - 0.5F * t) * 2.2F, s.colour, 0.9F * (1.0F - t * t));
			}
		}

		for (int i = 0; i < 46; i++) {
			float a = Atlas.hash(i, 1, 77);
			float b = Atlas.hash(i, 2, 77);
			float c = Atlas.hash(i, 3, 77);
			float d = Atlas.hash(i, 4, 77);
			float x = x0 + (x1 - x0) * a + (float)Math.sin((double)(m.time * (0.11F + 0.1F * b) + (float)i)) * 0.5F;
			float y = 0.6F + 4.2F * ((b + m.time * (0.012F + 0.02F * c)) % 1.0F);
			float z = -1.6F + 3.4F * c + (float)Math.cos((double)(m.time * (0.13F + 0.08F * d) + (float)i * 2.1F)) * 0.4F;
			float twinkle = 0.5F + 0.5F * (float)Math.sin((double)(m.time * (0.7F + d) + (float)i * 1.7F));
			frame.halo(x, y, z, 0.035F + 0.03F * d, -3626, (0.1F + 0.25F * twinkle) * dim);
		}
	}
}
