package com.onehumanawa.cfhcore.content.gui.earlywindow.scene;

final class OwnFactory extends Factory {
	private static final float BELT_T = 0.3125F;
	private static final float BELT_W = 0.4375F;
	private static final float BELT_Y = 1.1875F;
	private static final float TANK_X = -7.25F;
	private static final float TANK_H = 3.0F;
	private static final float A_X0 = -5.875F;
	private static final float A_X1 = -0.375F;
	private static final float B_X0 = -0.125F;
	private static final float B_FLAT = 2.75F;
	private static final float RISE = 2.5F;
	private static final float B_TOP0 = 5.25F;
	private static final float B_TOP1 = 6.4F;
	private static final float PRESS_X = 0.875F;
	private static final float HOUSING_Y = 2.4375F;
	private static final float SILO_X0 = 5.5F;
	private static final float SILO_X1 = 7.5F;
	private static final float SILO_Z = 1.0F;
	private static final float SILO_H = 3.0F;
	private static final float LUMP = 0.44F;
	private static final float BAR_L = 0.5F;
	private static final float BAR_H = 0.19F;
	private static final float BAR_D = 0.28F;
	private static final int STORE_X = 3;
	private static final int STORE_Z = 6;
	private static final int STORE_LAYERS = 14;
	private static final int STORE = 252;
	private static final float SQRT2 = 1.4142135F;
	private final Mesh fixed = new Mesh(8000);
	private final Mesh pile = new Mesh(8000);
	private boolean fixedBuilt;
	private int piled = -1;
	private final float[] at = new float[3];

	OwnFactory() {
		this.beltY = 1.1875F;
		this.lumpX0 = -5.375F;
		this.lumpX1 = -0.725F;
		this.way = new Path(this.lumpX1, 1.1875F, 2.75F, 1.1875F, 5.25F, 3.6875F, 6.35F, 3.6875F);
		this.pressX = 0.875F;
		this.pressAt = 0.875F - this.lumpX1;
		this.planned(
			-8.25F,
			-1.75F,
			8.25F,
			1.75F,
			new float[][]{
				{-7.75F, 0.375F, -0.5F, -6.75F, 3.575F, 0.5F},
				{0.375F, 0.375F, -1.2F, 1.375F, 3.4375F, 0.5F},
				{2.75F, 0.375F, -0.5F, 7.5F, 4.1375F, 0.5F},
				{5.5F, 0.375F, -1.0F, 7.5F, 3.375F, 1.0F}
			}
		);
		set(this.memoryAt, -7.25F, 3.5F, 0.0F);
		set(this.taskFrom, -5.775F, 1.1875F, 0.5F);
		set(this.taskTo, 0.375F, 2.4375F, -1.125F);
		this.over = new float[][]{{-2.7F, 2.325F, -0.78F}, {-5.375F, 2.4375F, -0.4F}};
		set(this.totalAt, 6.5F, 4.4375F, 0.0F);
	}

	@Override
	void free() {
		this.fixed.free();
		this.pile.free();
	}

	@Override
	float leaving(Motion m) {
		float drop = Math.max(0.15F, 3.6875F - pileTop(m));
		return (float)Math.sqrt((double)(2.0F * drop / 12.0F));
	}

	private static int stored(Motion m) {
		return Math.round(Math.max(0.0F, Math.min(1.0F, m.overall)) * 252.0F);
	}

	private static float pileTop(Motion m) {
		int layers = (stored(m) + 18 - 1) / 18;
		return 0.5F + (float)layers * 0.2F;
	}

	@Override
	void lamps(Motion m, SceneGl.Light light, int accent) {
		float flicker = 0.92F + 0.08F * (float)Math.sin((double)m.time * 7.3) * (float)Math.sin((double)m.time * 3.1 + 1.0);
		float lava = (0.35F + 0.65F * m.memory) * flicker;
		lamp(light, 0, -7.05F, 0.775F + 1.25F * m.memory, 0.9F, 4.6F, 1.0F * lava, 0.42F * lava, 0.1F * lava);
		float store = 0.1F + 0.34F * m.overall;
		lamp(
			light,
			1,
			6.5F,
			pileTop(m) + 0.5F,
			0.6F,
			3.6F,
			(float)(accent >>> 16 & 0xFF) / 255.0F * store,
			(float)(accent >>> 8 & 0xFF) / 255.0F * store,
			(float)(accent & 0xFF) / 255.0F * store
		);
		float flash = m.stroke >= 0.11F ? Math.max(0.0F, 1.0F - (m.stroke - 0.11F) / 0.18F) : 0.0F;
		lamp(light, 2, 0.875F, 1.4875F, 0.5F, 2.6F, 1.0F * flash, 0.8F * flash, 0.5F * flash);
	}

	@Override
	void build(Motion m, Frame f, int accent) {
		f.plinth(-8.25F, -1.75F, 8.25F, 1.75F, 0.375F);
		if (!this.fixedBuilt) {
			this.fixedBuilt = true;
			this.fixed.clear();
			tankFrame(this.fixed);
			tunnel(this.fixed);
			beds(this.fixed);
			pressFrame(this.fixed);
			storeFrame(this.fixed);
			things(this.fixed, accent);
		}

		f.solid.add(this.fixed);
		tank(m, f);
		this.firstBelt(m, f, accent);
		this.secondBelt(m, f, accent);
		press(m, f, accent);
		this.store(m, f, accent);
		wheels(m, f.solid);
		air(m, f, -8.0F, 8.0F, 1.0F);
	}

	private static void tankFrame(Mesh m) {
		float x0 = -7.75F;
		float x1 = -6.75F;
		float y0 = 0.375F;
		float y1 = 3.375F;
		m.reset();
		m.tiles(Tile.COPPER, Tile.COPPER_TOP, Tile.COPPER_TOP).gloss(0.5F).ground(0.8F);
		m.box(-7.75F, 0.375F, -0.5F, -6.75F, 0.625F, 0.5F);
		m.ground(1.0F);
		m.box(-7.75F, 3.125F, -0.5F, -6.75F, 3.375F, 0.5F);
		m.tile(Tile.COPPER).texel(1.0F, 0.0F);

		for (int i = 0; i < 4; i++) {
			float px = (i & 1) == 0 ? -7.75F : -6.8125F;
			float pz = (i & 2) == 0 ? -0.5F : 0.4375F;
			m.box(px, 0.625F, pz, px + 0.0625F, 3.125F, pz + 0.0625F);
		}

		m.texel(0.0F, 7.0F);

		for (int i = 1; i < 3; i++) {
			float by = 0.375F + (float)i - 0.03125F;
			m.box(-7.75F, by, 0.4375F, -6.75F, by + 0.0625F, 0.5F);
			m.box(-7.75F, by, -0.5F, -6.75F, by + 0.0625F, -0.4375F);
			m.box(-7.75F, by, -0.4375F, -7.6875F, by + 0.0625F, 0.4375F);
			m.box(-6.8125F, by, -0.4375F, -6.75F, by + 0.0625F, 0.4375F);
		}

		m.reset();
		m.tile(Tile.COPPER_TOP).gloss(0.5F);
		m.box(-7.4375F, 3.375F, -0.1875F, -7.0625F, 3.5F, 0.1875F);
		m.reset();
	}

	private static void tank(Motion m, Frame f) {
		float x0 = -7.75F;
		float x1 = -6.75F;
		float y0 = 0.375F;
		float y1 = 3.375F;
		float level = 0.04F + 2.46F * m.memory;
		f.lava(-7.71F, 0.625F, -0.46F, -6.79F, 0.625F + level, 0.46F);
		panes(f.glass, -7.73F, 0.625F, -0.48F, -6.77F, 3.125F, 0.48F);
		float heat = 0.25F + 0.75F * m.memory;
		f.halo(-7.25F, 0.625F + level * 0.5F, 0.55F, 1.3F + level * 0.55F, -34274, 0.34F * heat);
		f.halo(-7.25F, 0.625F + level, 0.55F, 0.9F, -19641, 0.22F * heat);
	}

	private static void panes(Mesh glass, float x0, float y0, float z0, float x1, float y1, float z1) {
		glass.reset();
		glass.tile(Tile.GLASS_BACK).faces(24).emissive(0.5F);
		glass.box(x0, y0, z0, x1, y1, z1);
		glass.tile(Tile.GLASS).faces(36).emissive(0.5F);
		glass.box(x0, y0, z0, x1, y1, z1);
		glass.reset();
	}

	private static void tunnel(Mesh m) {
		float x0 = -5.8125F;
		float x1 = -4.8125F;
		float y0 = 0.875F;
		float y1 = 2.0F;
		m.reset();
		m.tiles(Tile.TUNNEL, Tile.CASING_TOP, Tile.CASING_TOP).ground(0.85F);
		m.texel(0.0F, 14.0F);
		m.box(-5.8125F, 0.875F, 0.4375F, -4.8125F, 2.0F, 0.5F);
		m.box(-5.8125F, 0.875F, -0.5F, -4.8125F, 2.0F, -0.4375F);
		m.texel(0.0F, 0.0F);
		m.tiles(Tile.CASING, Tile.CASING_TOP, Tile.CASING_TOP);
		m.box(-5.8125F, 0.875F, -0.4375F, -5.75F, 2.0F, 0.4375F);
		m.box(-5.8125F, 1.875F, -0.4375F, -4.8125F, 2.0F, 0.4375F);
		m.tiles(Tile.CASING, Tile.IRON_DARK, Tile.CASING_TOP).ground(0.85F);
		m.box(-5.625F, 2.0F, -0.3125F, -5.0F, 2.3125F, 0.3125F);
		m.tiles(Tile.BEAM, Tile.IRON_DARK, Tile.BEAM).ground(1.0F);
		m.box(-5.75F, 2.3125F, -0.4375F, -4.875F, 2.4375F, 0.4375F);
		m.reset();
		m.tile(Tile.FLAP);
		float fy0 = 1.2175F;
		float fy1 = 1.875F;
		float v1 = Math.min(1.0F, 0.6575F);
		m.quad(-4.8125F, 1.875F, 0.4375F, -4.8125F, 1.2175F, 0.4375F, -4.8125F, 1.2175F, -0.4375F, -4.8125F, 1.875F, -0.4375F, 0.0F, 1.0F - v1, 0.875F, 1.0F);
		m.quad(-4.8125F, 1.875F, -0.4375F, -4.8125F, 1.2175F, -0.4375F, -4.8125F, 1.2175F, 0.4375F, -4.8125F, 1.875F, 0.4375F, 0.0F, 1.0F - v1, 0.875F, 1.0F);
		m.reset();
	}

	private static void beds(Mesh m) {
		m.reset();
		m.tiles(Tile.BED, Tile.IRON_DARK, Tile.IRON_DARK).ground(0.78F);
		m.box(-5.75F, 0.375F, -0.375F, -0.5F, 0.875F, 0.375F);
		m.box(0.0F, 0.375F, -0.375F, 2.5F, 0.875F, 0.375F);
		float slope = 3.535534F;
		m.reset();
		m.tile(Tile.BEAM).ground(0.8F);

		for (float px : new float[]{3.55F, 4.85F}) {
			float under = 1.1875F + (px - 2.75F) - 0.44194174F;
			m.box(px - 0.125F, 0.375F, -0.125F, px + 0.125F, under - 0.05F, 0.125F);
			m.box(px - 0.25F, 0.375F, -0.25F, px + 0.25F, 0.5F, 0.25F);
		}

		m.ground(1.0F);
		m.push();
		m.translate(2.75F, 1.1875F, 0.0F);
		m.rotateZ(45.0F);
		m.box(0.35F, -0.4525F, -0.3125F, 3.435534F, -0.3125F, -0.1875F);
		m.box(0.35F, -0.4525F, 0.1875F, 3.435534F, -0.3125F, 0.3125F);
		m.pop();
		m.reset();
	}

	private void firstBelt(Motion m, Frame f, int accent) {
		f.solid.push();
		f.solid.translate(-5.875F, 1.1875F, 0.0F);
		belt(f.solid, 5.5F, m.beltA, m.beltA);
		f.solid.pop();
	}

	private void secondBelt(Motion m, Frame f, int accent) {
		Mesh solid = f.solid;
		float flat = 2.875F;
		float slope = 3.535534F;
		float top = 1.1500001F;
		solid.push();
		solid.translate(-0.125F, 1.1875F, 0.0F);
		belt(solid, 2.875F, m.beltB, m.beltB);
		solid.pop();
		solid.push();
		solid.translate(2.75F, 1.1875F, 0.0F);
		solid.rotateZ(45.0F);
		belt(solid, 3.535534F, m.beltB - 2.875F, m.beltB);
		solid.pop();
		solid.push();
		solid.translate(5.25F, 3.6875F, 0.0F);
		belt(solid, 1.1500001F, m.beltB - 2.875F - 3.535534F, m.beltB);
		solid.pop();

		for (Motion.Item it : m.items) {
			this.item(m, solid, accent, it);
		}
	}

	private static void belt(Mesh solid, float length, float travel, float turned) {
		float r = 0.15625F;
		solid.reset();
		solid.tiles(Tile.BELT_SIDE, Tile.BELT, Tile.BELT_SIDE).faces(14);
		solid.box(0.15625F, -0.3125F, -0.4375F, length - 0.15625F, 0.0F, 0.4375F);
		solid.reset();
		solid.tile(Tile.BELT);
		float x = 0.15625F;
		float end = length - 0.15625F;

		while (x < end - 1.0E-4F) {
			float phase = x - travel;
			float into = phase - (float)Math.floor((double)phase);
			if (into > 0.9999F) {
				into = 0.0F;
			}

			float span = Math.min(end - x, 1.0F - into);
			solid.quad(x, 0.0F, -0.4375F, x, 0.0F, 0.4375F, x + span, 0.0F, 0.4375F, x + span, 0.0F, -0.4375F, into, 0.0F, into + span, 1.0F);
			x += span;
		}

		float angle = -turned / 0.15625F * (float) (180.0 / Math.PI);

		for (float cx : new float[]{0.15625F, length - 0.15625F}) {
			solid.reset();
			solid.tiles(Tile.IRON_DARK, Tile.SHAFT, Tile.SHAFT).colour(-7697776);
			solid.push();
			solid.translate(cx, -0.15625F, 0.0F);
			solid.rotateZ(angle);
			solid.cylinderZ(0.0F, 0.0F, -0.4375F, 0.4375F, 0.15625F, 10);
			solid.reset();
			solid.tiles(Tile.SHAFT, Tile.SHAFT, Tile.SHAFT);
			solid.cylinderZ(0.0F, 0.0F, -0.5375F, 0.5375F, 0.07F, 8);
			solid.pop();
		}

		solid.reset();
	}

	private static void lump(Mesh solid, int accent, float x, float y, float slope, float squash) {
		solid.push();
		solid.translate(x, y, 0.0F);
		solid.rotateZ(slope);
		float h = 0.44F * (1.0F - squash) + 0.19F * squash;
		float half = 0.22F * (1.0F + 0.25F * squash);
		solid.reset();
		solid.tile(Tile.RAW).ground(0.8F);
		solid.box(-half, 0.0F, -half, half, h, half);
		solid.reset();
		solid.tile(Tile.ORE).colour(Atlas.mix(accent, -1, 0.12F)).emissive(0.25F + 0.6F * squash).gloss(0.5F);
		float o = 0.004F;
		solid.box(-half - 0.004F, -0.004F, -half - 0.004F, half + 0.004F, h + 0.004F, half + 0.004F);
		solid.reset();
		solid.pop();
	}

	private static void bar(Mesh m, int colour, float x, float y, float z, float slope, boolean along, float shine) {
		m.push();
		m.translate(x, y, z);
		m.rotateZ(slope);
		if (!along) {
			m.rotateY(90.0F);
		}

		castBar(m, colour, shine);
		m.pop();
	}

	private static void castBar(Mesh m, int colour, float shine) {
		float hx = 0.25F;
		float hz = 0.14F;
		float in = 0.045F;
		float h = 0.19F;
		m.reset();
		m.colour(colour).gloss(0.55F).emissive(0.1F + 0.5F * shine);
		m.tile(Tile.BAR_TOP);
		m.quad(-0.205F, 0.19F, -0.095F, -0.205F, 0.19F, 0.095F, 0.205F, 0.19F, 0.095F, 0.205F, 0.19F, -0.095F, 0.0F, 0.0F, 1.0F, 1.0F);
		m.tile(Tile.BAR);
		m.quad(-0.205F, 0.19F, 0.095F, -0.25F, 0.0F, 0.14F, 0.25F, 0.0F, 0.14F, 0.205F, 0.19F, 0.095F, 0.0F, 0.0F, 1.0F, 0.4F);
		m.quad(0.205F, 0.19F, -0.095F, 0.25F, 0.0F, -0.14F, -0.25F, 0.0F, -0.14F, -0.205F, 0.19F, -0.095F, 0.0F, 0.0F, 1.0F, 0.4F);
		m.quad(0.205F, 0.19F, 0.095F, 0.25F, 0.0F, 0.14F, 0.25F, 0.0F, -0.14F, 0.205F, 0.19F, -0.095F, 0.0F, 0.0F, 0.6F, 0.4F);
		m.quad(-0.205F, 0.19F, -0.095F, -0.25F, 0.0F, -0.14F, -0.25F, 0.0F, 0.14F, -0.205F, 0.19F, 0.095F, 0.0F, 0.0F, 0.6F, 0.4F);
		m.reset();
	}

	private void item(Motion m, Mesh solid, int accent, Motion.Item it) {
		float x;
		float y;
		float slope;
		if (it.leaving >= 0.0F) {
			this.way.at(this.way.length(), this.at);
			x = this.at[0] + 1.045F * it.leaving;
			y = Math.max(pileTop(m), this.at[1] - 6.0F * it.leaving * it.leaving);
			slope = -Math.min(1.0F, it.leaving / this.leaving(m)) * 18.0F;
		} else {
			this.place(it.s, this.at);
			x = this.at[0];
			y = this.at[1];
			slope = this.at[2];
		}

		if (it.pressed) {
			bar(solid, accent, x, y, 0.0F, slope, false, it.s < this.pressAt + 1.2F ? 1.0F - (it.s - this.pressAt) / 1.2F : 0.0F);
		} else {
			float under = Math.abs(it.s - this.pressAt) < 0.05F ? m.pressDown : 0.0F;
			lump(solid, accent, x, y, slope, under * under);
		}
	}

	private static void pressFrame(Mesh m) {
		float y0 = 2.4375F;
		float y1 = 3.4375F;
		m.reset();
		m.tile(Tile.BEAM).ground(0.8F);

		for (float px : new float[]{0.375F, 1.125F}) {
			m.box(px, 0.375F, -1.125F, px + 0.25F, 3.4375F, -0.875F);
			m.box(px - 0.0625F, 0.375F, -1.1875F, px + 0.3125F, 0.5F, -0.8125F);
		}

		m.ground(1.0F);
		m.box(0.375F, 3.1875F, -0.875F, 1.375F, 3.4375F, -0.5F);
		m.reset();
		m.tiles(Tile.CASING, Tile.CASING_TOP, Tile.CASING_TOP);
		m.box(0.375F, 2.4375F, -0.5F, 1.375F, 3.4375F, 0.5F);
		m.reset();
		m.tile(Tile.GAUGE).emissive(0.25F);
		float gy = 2.9375F;
		float g = 0.36F;
		float gz = 0.505F;
		m.quad(0.515F, 3.2975001F, 0.505F, 0.515F, 2.5774999F, 0.505F, 1.235F, 2.5774999F, 0.505F, 1.235F, 3.2975001F, 0.505F, 0.0F, 0.0F, 1.0F, 1.0F);
		m.reset();
		m.colour(-12964322);
		m.box(0.835F, 2.8975F, 0.525F, 0.915F, 2.9775F, 0.54F);
		m.reset();
	}

	private static void press(Motion m, Frame f, int accent) {
		Mesh solid = f.solid;
		float y0 = 2.4375F;
		solid.reset();
		solid.tile(Tile.SHAFT);
		solid.push();
		solid.translate(0.875F, 2.9375F, 0.0F);
		solid.rotateY(90.0F);
		solid.rotateZ(m.beltB * 120.0F);
		solid.cylinderZ(0.0F, 0.0F, -0.75F, 0.75F, 0.09F, 8);
		solid.pop();
		solid.reset();
		solid.colour(Atlas.shade(accent, 0.8F)).emissive(0.5F);
		solid.push();
		solid.translate(0.875F, 2.9375F, 0.515F);
		solid.rotateZ(210.0F - 240.0F * Math.max(0.0F, Math.min(1.0F, m.needle)));
		solid.box(-0.05F, -0.022F, 0.0F, 0.27F, 0.022F, 0.012F);
		solid.pop();
		float top = 2.1875F;
		float bottom = 1.3795F;
		float head = 2.1875F + -0.80799997F * m.pressDown;
		solid.reset();
		solid.tile(Tile.IRON_DARK).gloss(0.4F);
		solid.box(0.75F, head + 0.1875F, -0.125F, 1.0F, 2.4375F, 0.125F);
		solid.tile(Tile.IRON).gloss(0.7F);
		solid.box(0.4375F, head + 0.0625F, -0.4375F, 1.3125F, head + 0.1875F, 0.4375F);
		solid.tile(Tile.IRON_DARK);
		solid.box(0.5625F, head, -0.3125F, 1.1875F, head + 0.0625F, 0.3125F);
		solid.reset();
		if (m.stroke >= 0.11F) {
			float flash = Math.max(0.0F, 1.0F - (m.stroke - 0.11F) / 0.2F);
			f.halo(0.875F, 1.3875F, 0.45F, 1.1F, -11638, 0.5F * flash);
		}
	}

	private static void storeFrame(Mesh m) {
		float y0 = 0.375F;
		float y1 = 3.375F;
		m.reset();
		m.tiles(Tile.BRASS, Tile.BRASS_TOP, Tile.BRASS_TOP).gloss(0.5F).ground(0.85F);
		m.box(5.5F, 0.375F, -1.0F, 7.5F, 0.5F, 1.0F);
		m.ground(1.0F);
		m.tile(Tile.BRASS).texel(1.0F, 0.0F);

		for (int i = 0; i < 4; i++) {
			float px = (i & 1) == 0 ? 5.5F : 7.375F;
			float pz = (i & 2) == 0 ? -1.0F : 0.875F;
			m.box(px, 0.5F, pz, px + 0.125F, 3.375F, pz + 0.125F);
		}

		m.texel(0.0F, 1.0F);

		for (int i = 1; i <= 3; i++) {
			float by = 0.375F + (float)i - (i == 3 ? 0.125F : 0.0625F);
			float bh = i == 3 ? 0.125F : 0.0625F;
			m.box(5.625F, by, 0.875F, 7.375F, by + bh, 1.0F);
			m.box(5.625F, by, -1.0F, 7.375F, by + bh, -0.875F);
			m.box(5.5F, by, -0.875F, 5.625F, by + bh, 0.875F);
			m.box(7.375F, by, -0.875F, 7.5F, by + bh, 0.875F);
		}

		m.reset();
	}

	private void store(Motion m, Frame f, int accent) {
		float y0 = 0.375F;
		float y1 = 3.375F;
		panes(f.glass, 5.54F, 0.5F, -0.96F, 7.46F, 3.25F, 0.96F);
		int count = stored(m);
		int layer = 18;
		float cx = 6.5F;
		if (count != this.piled) {
			this.piled = count;
			this.pile.clear();
			int top = (count - 1) / 18;

			for (int i = 0; i < count - 1; i++) {
				int l = i / 18;
				int in = i % 18;
				int a = in % 3;
				int b = in / 3;
				if (l >= top - 1 || a <= 0 || a >= 2 || b <= 0 || b >= 5) {
					place(this.pile, accent, i, 0.0F, 0.0F);
				}
			}
		}

		f.solid.add(this.pile);
		if (count > 0) {
			float settle = 1.0F - m.landed;
			place(f.solid, accent, count - 1, 0.06F * settle * settle, settle);
			f.halo(6.5F, pileTop(m) + 0.1F, 0.2F, 1.5F, Atlas.mix(accent, -1, 0.2F), 0.1F + 0.16F * m.overall + 0.2F * settle);
		}
	}

	private static void place(Mesh m, int accent, int i, float lift, float shine) {
		int layer = 18;
		int l = i / 18;
		int in = i % 18;
		int a = in % 3;
		int b = in / 3;
		boolean along = (l & 1) == 0;
		float cx = 6.5F;
		float pitchLong = 0.56666666F;
		float pitchShort = 0.28333333F;
		float u = ((float)a - 1.0F) * 0.56666666F;
		float v = ((float)b - 2.5F) * 0.28333333F;
		float y = 0.5F + (float)l * 0.2F + lift;
		bar(m, accent, along ? 6.5F + u : 6.5F + v, y, along ? v : u, 0.0F, along, shine);
	}

	private static void wheels(Motion m, Mesh solid) {
		float turn = m.beltA * 70.0F;
		wheel(solid, -2.7F, 1.375F, -0.78F, 0.95F, 16, turn);
		wheel(solid, -1.37F, 1.1949999F, -0.78F, 0.45F, 8, -turn * 2.0F + 22.5F);
	}

	private static void wheel(Mesh solid, float x, float y, float z, float radius, int teeth, float angle) {
		solid.push();
		solid.translate(x, y, z);
		solid.rotateZ(angle);
		solid.reset();
		solid.tiles(Tile.COG, Tile.COG_FACE, Tile.COG_FACE);
		solid.cylinderZ(0.0F, 0.0F, -0.11F, 0.11F, radius * 0.84F, teeth * 2);
		solid.tile(Tile.COG);
		float w = (float)(Math.PI * (double)radius / (double)teeth) * 0.52F;

		for (int i = 0; i < teeth; i++) {
			solid.push();
			solid.rotateZ((float)i * 360.0F / (float)teeth);
			solid.box(radius * 0.8F, -w, -0.09F, radius, w, 0.09F);
			solid.pop();
		}

		solid.tile(Tile.SHAFT);
		solid.cylinderZ(0.0F, 0.0F, -0.35F, 0.3F, 0.075F, 8);
		solid.reset();
		solid.pop();
	}

	private static void things(Mesh m, int accent) {
		m.reset();
		m.tile(Tile.CRATE).ground(0.78F);
		m.push();
		m.translate(-6.3F, 0.375F, -1.05F);
		m.rotateY(12.0F);
		m.box(-0.375F, 0.0F, -0.375F, 0.375F, 0.75F, 0.375F);
		m.pop();
		m.push();
		m.translate(-4.1F, 0.375F, 1.2F);
		m.rotateY(31.0F);
		m.box(-0.25F, 0.0F, -0.25F, 0.25F, 0.5F, 0.25F);
		m.pop();
		bar(m, accent, 3.4F, 0.375F, 1.2F, 0.0F, true, 0.0F);
		m.push();
		m.translate(3.95F, 0.375F, 1.05F);
		m.rotateY(40.0F);
		castBar(m, accent, 0.0F);
		m.pop();
		m.reset();
	}
}
