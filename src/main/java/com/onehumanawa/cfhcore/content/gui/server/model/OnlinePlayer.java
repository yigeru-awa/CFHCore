package com.onehumanawa.cfhcore.content.gui.server.model;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public final class OnlinePlayer {
	public UUID id = new UUID(0L, 0L);
	public String name = "";
	public String nick = "";
	@Nullable
	public Role.Tag role;
	public boolean afk;
	public boolean vanished;
	public boolean acceptsRequests = true;
	public long joinedAt;

	public String shownName() {
		return this.nick != null && !this.nick.isEmpty() ? this.nick : this.name;
	}

	public OnlinePlayer sanitize() {
		if (this.id == null) {
			this.id = new UUID(0L, 0L);
		}

		if (this.name == null) {
			this.name = "";
		}

		if (this.nick == null) {
			this.nick = "";
		}

		if (this.role != null) {
			this.role.sanitize();
		}

		return this;
	}
}
