package com.onehumanawa.cfhcore.content.gui.server.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Hello {
	public static final String HOMES = "homes";
	public static final String WARPS = "warps";
	public static final String SPAWN = "spawn";
	public static final String BACK = "back";
	public static final String REQUESTS = "requests";
	public static final String RANDOM_TELEPORT = "random_teleport";
	public static final String AFK = "afk";
	public static final String MAIL = "mail";
	public static final String NICKNAMES = "nicknames";
	public static final String KITS = "kits";
	public static final String STATS = "stats";
	public static final String RANKINGS = "rankings";
	public static final String STAFF_CHAT = "staff_chat";
	public static final String THIRST = "thirst";
	public static final int PROTOCOL = 1;
	public int protocol = 1;
	public List<String> features = new ArrayList<>();
	public List<String> may = new ArrayList<>();
	public Map<String, Integer> numbers = new LinkedHashMap<>();
	public List<Role.Tag> roles = new ArrayList<>();
	public List<String> rules = new ArrayList<>();
	public List<ServerRules.Link> links = new ArrayList<>();
	public List<String> featuredRankings = new ArrayList<>();
	public boolean maintenance;

	public boolean feature(String name) {
		return this.features.contains(name);
	}

	public boolean may(String node) {
		return this.may.contains(node);
	}

	public int number(String node, int fallback) {
		Integer n = this.numbers.get(node);
		return n == null ? fallback : n;
	}

	public Hello sanitize() {
		if (this.features == null) {
			this.features = new ArrayList<>();
		}

		this.features.removeIf(f -> f == null);
		if (this.may == null) {
			this.may = new ArrayList<>();
		}

		this.may.removeIf(n -> n == null);
		if (this.numbers == null) {
			this.numbers = new LinkedHashMap<>();
		}

		this.numbers.values().removeIf(v -> v == null);
		if (this.roles == null) {
			this.roles = new ArrayList<>();
		}

		this.roles.removeIf(r -> r == null);

		for (Role.Tag t : this.roles) {
			t.sanitize();
		}

		if (this.rules == null) {
			this.rules = new ArrayList<>();
		}

		this.rules.removeIf(r -> r == null);
		if (this.links == null) {
			this.links = new ArrayList<>();
		}

		this.links.removeIf(lx -> lx == null || lx.name == null || lx.url == null || !web(lx.url));

		for (ServerRules.Link l : this.links) {
			l.url = l.url.trim();
		}

		if (this.featuredRankings == null) {
			this.featuredRankings = new ArrayList<>();
		}

		this.featuredRankings.removeIf(k -> k == null);
		return this;
	}

	public static boolean web(String url) {
		try {
			URI uri = new URI(url.trim());
			String scheme = uri.getScheme();
			return scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")) && uri.getRawAuthority() != null;
		} catch (URISyntaxException var3) {
			return false;
		}
	}
}
