package com.onehumanawa.cfhcore.content.gui.quests.client.view;

public record LinkView(NodeView from, NodeView to) {
	public boolean satisfied() {
		return this.from.done();
	}

	public boolean leading() {
		return this.from.done() && this.to.status.open();
	}

	public boolean complete() {
		return this.from.done() && this.to.done();
	}

	public float hover() {
		return Math.max(this.from.hover.get(), this.to.hover.get());
	}

	public float lit() {
		return Math.min(this.from.lit.get(), this.to.lit.get());
	}

	public int seed() {
		return this.from.index * 31 + this.to.index * 17;
	}
}
