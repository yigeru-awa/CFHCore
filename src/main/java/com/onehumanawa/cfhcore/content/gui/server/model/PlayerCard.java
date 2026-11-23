package com.onehumanawa.cfhcore.content.gui.server.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PlayerCard {
	public UUID id = new UUID(0L, 0L);
	public String name = "";
	public String nick = "";
	public List<Role.Tag> roles = new ArrayList<>();
	public boolean online;
	public boolean afk;
	public long firstSeen;
	public long lastSeen;
	public long sessions;
	public long playTicks;
	public long afkTicks;
	public boolean statsHidden;

	public PlayerCard sanitize() {
		if (this.id == null) {
			this.id = new UUID(0L, 0L);
		}

		if (this.name == null) {
			this.name = "";
		}

		if (this.nick == null) {
			this.nick = "";
		}

		if (this.roles == null) {
			this.roles = new ArrayList<>();
		}

		this.roles.removeIf(r -> r == null);

		for (Role.Tag t : this.roles) {
			t.sanitize();
		}

		return this;
	}
}
