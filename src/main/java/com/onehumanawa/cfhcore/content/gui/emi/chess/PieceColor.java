package com.onehumanawa.cfhcore.content.gui.emi.chess;

enum PieceColor {
	WHITE,
	BLACK;

	public PieceColor opposite() {
		return this == WHITE ? BLACK : WHITE;
	}
}
