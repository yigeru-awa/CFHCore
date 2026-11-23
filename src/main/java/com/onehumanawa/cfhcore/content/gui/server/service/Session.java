package com.onehumanawa.cfhcore.content.gui.server.service;

import com.onehumanawa.cfhcore.content.gui.server.model.Place;
import com.onehumanawa.cfhcore.content.gui.server.model.PlayerRecord;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

final class Session {
	final UUID id;
	final String name;
	@Nullable
	ServerPlayer player;
	final PlayerRecord record;
	final long joinedAt;
	long seenBefore;
	final int fakeLevel;
	int levelSeen;
	boolean afk;
	boolean afkManual;
	long afkSince;
	long lastActive;
	long actionSeen;
	float yaw;
	float pitch;
	int afkGrace;
	@Nullable
	Teleports.Pending pending;
	@Nullable
	Place fakePlace;
	@Nullable
	Place frozenAt;
	boolean wasMuted;
	boolean wasFrozen;
	boolean watching;
	long toldAt;
	@Nullable
	Vitals steady;
	boolean replaced;
	private final Map<String, Long> last = new HashMap<>();

	Session(UUID id, String name, @Nullable ServerPlayer player, PlayerRecord record, long now, int fakeLevel) {
		this.id = id;
		this.name = name;
		this.player = player;
		this.record = record;
		this.joinedAt = now;
		this.lastActive = now;
		this.fakeLevel = fakeLevel;
		if (player != null) {
			this.levelSeen = player.server.getProfilePermissions(player.getGameProfile());
			this.actionSeen = player.getLastActionTime();
			this.yaw = player.getYRot();
			this.pitch = player.getXRot();
		}
	}

	Actor actor() {
		return this.player != null ? Actor.of(this.player) : Actor.fake(this.id, this.name, this.fakeLevel);
	}

	@Nullable
	Place place() {
		ServerPlayer p = this.player;
		if (p == null) {
			return this.fakePlace == null ? null : this.fakePlace.copy();
		} else {
			return new Place(p.level().dimension().location().toString(), p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot());
		}
	}

	boolean allow(String what, long gapMs, long now) {
		Long before = this.last.get(what);
		if (before != null && now - before < gapMs && now >= before) {
			return false;
		} else {
			this.last.put(what, now);
			return true;
		}
	}
}
