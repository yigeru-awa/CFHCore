package com.onehumanawa.cfhcore.content.gui.server.model;

import java.util.UUID;

public final class TeleportRequest {
	public UUID player = new UUID(0L, 0L);
	public String name = "";
	public boolean incoming;
	public boolean here;
	public long expiresAt;

	public TeleportRequest sanitize() {
		if (this.player == null) {
			this.player = new UUID(0L, 0L);
		}

		if (this.name == null) {
			this.name = "";
		}

		return this;
	}
}
