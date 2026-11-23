package com.onehumanawa.cfhcore.content.gui.server.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public final class Role {
	public static final int ID_MAX = 24;
	public static final int NAME_MAX = 24;
	public static final int PREFIX_MAX = 16;
	public static final String DEFAULT = "default";
	public static final String MODERATOR = "moderator";
	public static final String ADMIN = "admin";
	public String id = "";
	public String name = "";
	public int color = 11184810;
	public String prefix = "";
	public int priority = 0;
	public List<String> inherits = new ArrayList<>();
	public List<String> allow = new ArrayList<>();
	public List<String> deny = new ArrayList<>();
	public Map<String, Integer> numbers = new LinkedHashMap<>();
	public boolean everyone = false;
	public boolean operators = false;

	public static boolean matches(String pattern, String node) {
		if (pattern == null || pattern.isEmpty()) {
			return false;
		} else if (pattern.equals("*") || pattern.equals(node)) {
			return true;
		} else {
			return !pattern.endsWith(".*") ? false : node.startsWith(pattern.substring(0, pattern.length() - 1));
		}
	}

	@Nullable
	public Boolean says(String node) {
		for (String p : this.deny) {
			if (matches(p, node)) {
				return Boolean.FALSE;
			}
		}

		for (String px : this.allow) {
			if (matches(px, node)) {
				return Boolean.TRUE;
			}
		}

		return null;
	}

	public static String idOf(String name) {
		String s = name == null ? "" : name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
		return s.length() > 24 ? s.substring(0, 24) : s;
	}

	public Role sanitize() {
		if (this.id == null) {
			this.id = "";
		}

		if (this.name == null || this.name.isBlank()) {
			this.name = this.id;
		}

		if (this.prefix == null) {
			this.prefix = "";
		}

		if (this.inherits == null) {
			this.inherits = new ArrayList<>();
		}

		if (this.allow == null) {
			this.allow = new ArrayList<>();
		}

		if (this.deny == null) {
			this.deny = new ArrayList<>();
		}

		if (this.numbers == null) {
			this.numbers = new LinkedHashMap<>();
		}

		this.inherits.removeIf(s -> s == null || s.isBlank() || s.equals(this.id));
		this.allow.removeIf(s -> s == null || s.isBlank());
		this.deny.removeIf(s -> s == null || s.isBlank());
		this.numbers.values().removeIf(v -> v == null);
		this.color &= 16777215;
		return this;
	}

	public static final class Grant {
		public String role = "";
		public long expiresAt;
		public UUID by = new UUID(0L, 0L);
		public String byName = "";
		public long at;

		public boolean expired(long now) {
			return this.expiresAt > 0L && now >= this.expiresAt;
		}

		public Role.Grant sanitize() {
			if (this.role == null) {
				this.role = "";
			}

			if (this.by == null) {
				this.by = new UUID(0L, 0L);
			}

			if (this.byName == null) {
				this.byName = "";
			}

			return this;
		}
	}

	public static final class Tag {
		public String id = "";
		public String name = "";
		public int color = 11184810;
		public String prefix = "";

		public Tag() {
		}

		public Tag(Role role) {
			this.id = role.id;
			this.name = role.name;
			this.color = role.color;
			this.prefix = role.prefix;
		}

		public Role.Tag sanitize() {
			if (this.id == null) {
				this.id = "";
			}

			if (this.name == null) {
				this.name = "";
			}

			if (this.prefix == null) {
				this.prefix = "";
			}

			this.color &= 16777215;
			return this;
		}
	}
}
