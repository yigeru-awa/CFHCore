package com.onehumanawa.cfhcore.content.gui.server.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public final class PlayerRecord {
	public static final int NICK_MIN = 3;
	public static final int NICK_MAX = 16;
	public static final int MAIL_TEXT_MAX = 256;
	public static final int NOTE_MAX = 256;
	public UUID id = new UUID(0L, 0L);
	public String name = "";
	public String nick = "";
	public long firstSeen;
	public long lastSeen;
	public long sessions;
	public long afkTicks;
	public boolean statsHidden = false;
	public List<Home> homes = new ArrayList<>();
	@Nullable
	public Place back;
	public boolean backIsDeath;
	@Nullable
	public Place death;
	public long deathAt;
	@Nullable
	public Place lastPlace;
	public boolean acceptsRequests = true;
	public Map<String, Long> cooldowns = new LinkedHashMap<>();
	public List<String> kitsTaken = new ArrayList<>();
	public List<Role.Grant> roles = new ArrayList<>();
	public List<PlayerRecord.Mail> mail = new ArrayList<>();
	public List<Punishment> punishments = new ArrayList<>();
	public List<PlayerRecord.Note> notes = new ArrayList<>();
	public boolean vanished;
	public boolean god;
	public boolean fly;

	public String shownName() {
		return this.nick != null && !this.nick.isEmpty() ? this.nick : this.name;
	}

	@Nullable
	public Home home(String homeName) {
		String key = homeName.toLowerCase(Locale.ROOT);

		for (Home h : this.homes) {
			if (h.name.toLowerCase(Locale.ROOT).equals(key)) {
				return h;
			}
		}

		return null;
	}

	@Nullable
	public Punishment active(Punishment.Kind kind, long now) {
		for (int i = this.punishments.size() - 1; i >= 0; i--) {
			Punishment p = this.punishments.get(i);
			if (p.kind == kind && p.active(now)) {
				return p;
			}
		}

		return null;
	}

	public int unreadMail() {
		int n = 0;

		for (PlayerRecord.Mail m : this.mail) {
			if (!m.read) {
				n++;
			}
		}

		return n;
	}

	public PlayerRecord sanitize() {
		if (this.id == null) {
			this.id = new UUID(0L, 0L);
		}

		if (this.name == null) {
			this.name = "";
		}

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

		if (this.lastPlace != null) {
			this.lastPlace.sanitize();
		}

		if (this.cooldowns == null) {
			this.cooldowns = new LinkedHashMap<>();
		}

		this.cooldowns.values().removeIf(v -> v == null);
		if (this.kitsTaken == null) {
			this.kitsTaken = new ArrayList<>();
		}

		if (this.roles == null) {
			this.roles = new ArrayList<>();
		}

		this.roles.removeIf(g -> g == null);

		for (Role.Grant g : this.roles) {
			g.sanitize();
		}

		if (this.mail == null) {
			this.mail = new ArrayList<>();
		}

		this.mail.removeIf(m -> m == null);

		for (PlayerRecord.Mail m : this.mail) {
			m.sanitize();
		}

		if (this.punishments == null) {
			this.punishments = new ArrayList<>();
		}

		this.punishments.removeIf(p -> p == null);

		for (Punishment p : this.punishments) {
			p.sanitize();
		}

		if (this.notes == null) {
			this.notes = new ArrayList<>();
		}

		this.notes.removeIf(nx -> nx == null);

		for (PlayerRecord.Note n : this.notes) {
			n.sanitize();
		}

		return this;
	}

	public static final class Mail {
		public String id = "";
		public UUID from = new UUID(0L, 0L);
		public String fromName = "";
		public String text = "";
		public long at;
		public boolean read;

		public PlayerRecord.Mail sanitize() {
			if (this.id == null) {
				this.id = "";
			}

			if (this.from == null) {
				this.from = new UUID(0L, 0L);
			}

			if (this.fromName == null) {
				this.fromName = "";
			}

			if (this.text == null) {
				this.text = "";
			}

			return this;
		}
	}

	public static final class Note {
		public UUID by = new UUID(0L, 0L);
		public String byName = "";
		public String text = "";
		public long at;

		public PlayerRecord.Note sanitize() {
			if (this.by == null) {
				this.by = new UUID(0L, 0L);
			}

			if (this.byName == null) {
				this.byName = "";
			}

			if (this.text == null) {
				this.text = "";
			}

			return this;
		}
	}
}
