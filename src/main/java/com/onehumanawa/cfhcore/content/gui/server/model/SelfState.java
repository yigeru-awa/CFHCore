package com.onehumanawa.cfhcore.content.gui.server.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public final class SelfState {
	public String nick = "";
	public List<Home> homes = new ArrayList<>();
	@Nullable
	public Place back;
	public boolean backIsDeath;
	@Nullable
	public Place death;
	public long deathAt;
	public boolean afk;
	public boolean vanished;
	public boolean god;
	public boolean fly;
	public boolean frozen;
	public long mutedUntil;
	public boolean acceptsRequests = true;
	public boolean statsHidden;
	public int unreadMail;
	public Map<String, Long> cooldowns = new LinkedHashMap<>();
	@Nullable
	public String pending;
	public long pendingAt;
	public List<Kit.View> kits = new ArrayList<>();

	public long cooldown(String what) {
		Long until = this.cooldowns.get(what);
		return until == null ? 0L : until;
	}

	public SelfState sanitize() {
		if (this.nick == null) {
			this.nick = "";
		}

		if (this.homes == null) {
			this.homes = new ArrayList<>();
		}

		this.homes.removeIf(h -> h == null);

		for (Home h : this.homes) {
			h.sanitize();
		}

		if (this.back != null) {
			this.back.sanitize();
		}

		if (this.death != null) {
			this.death.sanitize();
		}

		if (this.cooldowns == null) {
			this.cooldowns = new LinkedHashMap<>();
		}

		this.cooldowns.values().removeIf(v -> v == null);
		if (this.kits == null) {
			this.kits = new ArrayList<>();
		}

		this.kits.removeIf(kx -> kx == null);

		for (Kit.View k : this.kits) {
			k.sanitize();
		}

		return this;
	}
}
