package com.onehumanawa.cfhcore.content.gui.emi.api;

public enum EmiFillAction {
	FILL(0),
	CURSOR(1),
	QUICK_MOVE(2);

	public final int id;

	private EmiFillAction(int id) {
		this.id = id;
	}
}
