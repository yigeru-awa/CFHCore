package com.onehumanawa.cfhcore.content.gui.earlywindow.scene;

final class Frame {
	final Mesh ground = new Mesh(64);
	final Mesh stage = new Mesh(2048);
	final Mesh polish = new Mesh(2048);
	final Mesh solid = new Mesh(24000);
	final Mesh glass = new Mesh(4096);
	final Mesh glow = new Mesh(2048);
	Camera camera;

	void clear() {
		this.ground.clear();
		this.stage.clear();
		this.polish.clear();
		this.solid.clear();
		this.glass.clear();
		this.glow.clear();
	}

	void free() {
		this.ground.free();
		this.stage.free();
		this.polish.free();
		this.solid.free();
		this.glass.free();
		this.glow.free();
	}

	void halo(float x, float y, float z, float radius, int colour, float strength) {
		this.glow.reset();
		this.glow.kind(2).colour(Math.round(Math.max(0.0F, Math.min(1.0F, strength)) * 255.0F) << 24 | colour & 16777215);
		float rx = this.camera.right[0] * radius;
		float ry = this.camera.right[1] * radius;
		float rz = this.camera.right[2] * radius;
		float ux = this.camera.up[0] * radius;
		float uy = this.camera.up[1] * radius;
		float uz = this.camera.up[2] * radius;
		this.glow
			.rawQuad(
				x - rx + ux,
				y - ry + uy,
				z - rz + uz,
				x - rx - ux,
				y - ry - uy,
				z - rz - uz,
				x + rx - ux,
				y + ry - uy,
				z + rz - uz,
				x + rx + ux,
				y + ry + uy,
				z + rz + uz,
				0.0F,
				0.0F,
				1.0F,
				0.0F,
				0.0F,
				0.0F,
				1.0F,
				1.0F,
				1.0F,
				1.0F,
				0.0F
			);
	}

	void lava(float x0, float y0, float z0, float x1, float y1, float z1) {
		Mesh m = this.solid;
		m.reset();
		m.kind(1);
		float s = 16.0F;
		m.rawQuad(
			x0,
			y1,
			z1,
			x0,
			y0,
			z1,
			x1,
			y0,
			z1,
			x1,
			y1,
			z1,
			0.0F,
			0.0F,
			1.0F,
			x0 * 16.0F,
			-y1 * 16.0F,
			x0 * 16.0F,
			-y0 * 16.0F,
			x1 * 16.0F,
			-y0 * 16.0F,
			x1 * 16.0F,
			-y1 * 16.0F
		);
		m.rawQuad(
			x1,
			y1,
			z0,
			x1,
			y0,
			z0,
			x0,
			y0,
			z0,
			x0,
			y1,
			z0,
			0.0F,
			0.0F,
			-1.0F,
			-x1 * 16.0F,
			-y1 * 16.0F,
			-x1 * 16.0F,
			-y0 * 16.0F,
			-x0 * 16.0F,
			-y0 * 16.0F,
			-x0 * 16.0F,
			-y1 * 16.0F
		);
		m.rawQuad(
			x1,
			y1,
			z1,
			x1,
			y0,
			z1,
			x1,
			y0,
			z0,
			x1,
			y1,
			z0,
			1.0F,
			0.0F,
			0.0F,
			-z1 * 16.0F,
			-y1 * 16.0F,
			-z1 * 16.0F,
			-y0 * 16.0F,
			-z0 * 16.0F,
			-y0 * 16.0F,
			-z0 * 16.0F,
			-y1 * 16.0F
		);
		m.rawQuad(
			x0,
			y1,
			z0,
			x0,
			y0,
			z0,
			x0,
			y0,
			z1,
			x0,
			y1,
			z1,
			-1.0F,
			0.0F,
			0.0F,
			z0 * 16.0F,
			-y1 * 16.0F,
			z0 * 16.0F,
			-y0 * 16.0F,
			z1 * 16.0F,
			-y0 * 16.0F,
			z1 * 16.0F,
			-y1 * 16.0F
		);
		m.rawQuad(
			x0,
			y1,
			z0,
			x0,
			y1,
			z1,
			x1,
			y1,
			z1,
			x1,
			y1,
			z0,
			0.0F,
			1.0F,
			0.0F,
			x0 * 16.0F,
			z0 * 16.0F + 40.0F,
			x0 * 16.0F,
			z1 * 16.0F + 40.0F,
			x1 * 16.0F,
			z1 * 16.0F + 40.0F,
			x1 * 16.0F,
			z0 * 16.0F + 40.0F
		);
		m.reset();
	}

	void plinth(float x0, float z0, float x1, float z1, float top) {
		this.stage.reset();
		this.stage.tiles(Tile.PLINTH_SIDE, Tile.PLINTH_TOP, Tile.PLINTH_TOP).faces(60);
		this.stage.box(x0, 0.0F, z0, x1, top, z1);
		this.polish.reset();
		this.polish.tile(Tile.PLINTH_TOP).faces(1).gloss(0.3F).colour(-654311425);
		this.polish.box(x0, 0.0F, z0, x1, top, z1);
	}
}
