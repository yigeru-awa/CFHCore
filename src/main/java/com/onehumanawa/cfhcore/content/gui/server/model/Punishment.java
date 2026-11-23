package com.onehumanawa.cfhcore.content.gui.server.model;

import java.util.Locale;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public final class Punishment {
	public static final int REASON_MAX = 160;
	public String id = "";
	public Punishment.Kind kind = Punishment.Kind.WARN;
	public String reason = "";
	public UUID by = new UUID(0L, 0L);
	public String byName = "";
	public long at;
	public long expiresAt;
	public long liftedAt;
	public String liftedByName = "";

	public boolean active(long now) {
		return this.kind != null && this.kind.lasts() && this.liftedAt == 0L && (this.expiresAt == 0L || now < this.expiresAt);
	}

	public Punishment sanitize() {
		if (this.id == null) {
			this.id = "";
		}

		if (this.kind == null) {
			this.kind = Punishment.Kind.WARN;
		}

		if (this.reason == null) {
			this.reason = "";
		}

		if (this.by == null) {
			this.by = new UUID(0L, 0L);
		}

		if (this.byName == null) {
			this.byName = "";
		}

		if (this.liftedByName == null) {
			this.liftedByName = "";
		}

		return this;
	}

	public static enum Kind {
		MUTE,
		BAN,
		WARN,
		KICK,
		FREEZE;

		public boolean lasts() {
			return this == MUTE || this == BAN || this == FREEZE;
		}

		public String key() {
			return this.name().toLowerCase(Locale.ROOT);
		}

		@Nullable
		public static Punishment.Kind parse(@Nullable String s) {
			if (s == null) {
				return null;
			} else {
				try {
					return valueOf(s.trim().toUpperCase(Locale.ROOT));
				} catch (IllegalArgumentException var2) {
					return null;
				}
			}
		}
	}
}
