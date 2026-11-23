package com.onehumanawa.cfhcore.content.gui.earlywindow.scene;

enum Tile {
	WHITE(0, 0),
	CASING(1, 0),
	CASING_TOP(2, 0),
	BRASS(3, 0),
	BRASS_TOP(4, 0),
	COPPER(5, 0),
	COPPER_TOP(6, 0),
	GLASS(7, 0),
	BELT(8, 0),
	BELT_SIDE(9, 0),
	SHAFT(10, 0),
	COG(11, 0),
	COG_FACE(12, 0),
	PLINTH_TOP(13, 0),
	PLINTH_SIDE(14, 0),
	IRON(15, 0),
	IRON_DARK(0, 1),
	RAW(1, 1),
	BAR(2, 1),
	BAR_TOP(3, 1),
	GAUGE(4, 1),
	FLAP(5, 1),
	STONE(6, 1),
	STONE_SIDE(7, 1),
	PIPE(8, 1),
	CRATE(9, 1),
	TUNNEL(10, 1),
	BED(11, 1),
	BEAM(12, 1),
	LAMP(13, 1),
	GLASS_BACK(14, 1),
	ORE(15, 1),
	STONE_B(0, 2),
	STONE_C(1, 2),
	PLATES(2, 2),
	PLATE_TOP(3, 2);

	static final int ACROSS = 16;
	static final int SIZE = 16;
	private static final float INSET = 0.03125F;
	final int column;
	final int row;

	private Tile(final int column, final int row) {
		this.column = column;
		this.row = row;
	}

	float u(float t) {
		return ((float)(this.column * 16) + 0.03125F + t * 15.9375F) / 1024.0F;
	}

	float v(float t) {
		return ((float)(this.row * 16) + 0.03125F + t * 15.9375F) / 1024.0F;
	}
}
