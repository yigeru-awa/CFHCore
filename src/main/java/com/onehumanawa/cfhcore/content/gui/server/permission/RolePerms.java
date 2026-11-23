package com.onehumanawa.cfhcore.content.gui.server.permission;

import com.onehumanawa.cfhcore.content.gui.core.perm.Perms;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Node;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Resolver;
import com.onehumanawa.cfhcore.content.gui.server.model.Role;
import com.onehumanawa.cfhcore.content.gui.server.model.RoleBook;
import com.onehumanawa.cfhcore.content.gui.server.model.ServerRules;
import com.onehumanawa.cfhcore.content.gui.server.platform.ServerPlatform;
import com.onehumanawa.cfhcore.content.gui.server.service.Actor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class RolePerms implements Resolver {
	public static final int TOP = Integer.MAX_VALUE;
	public static final int NONE = Integer.MIN_VALUE;
	private final RolePerms.Source source;
	private final boolean external;
	private final Map<UUID, RolePerms.Cached> cache = new ConcurrentHashMap<>();

	public RolePerms(RolePerms.Source source, boolean external) {
		this.source = source;
		this.external = external;
	}

	public void changed() {
		this.cache.clear();
	}

	public void changed(UUID player) {
		this.cache.remove(player);
	}

	@Nullable
	public Boolean has(ServerPlayer player, String node) {
		if (this.external) {
			Boolean other = ServerPlatform.get().externalPermission(player, node);
			if (other != null) {
				return other;
			}
		}

		return this.decide(player.getUUID(), level(player), player.server.isSingleplayerOwner(player.getGameProfile()), node);
	}

	@Nullable
	public Integer number(ServerPlayer player, String node) {
		return this.number(player.getUUID(), level(player), node);
	}

	@Nullable
	public Boolean hasOffline(UUID player, String node) {
		return this.hasOffline(player, 0, false, node);
	}

	@Nullable
	public Boolean hasOffline(UUID player, int level, boolean owner, String node) {
		Boolean b = this.decide(player, level, owner, node);
		if (b != null) {
			return b;
		} else {
			Node declared = Perms.node(node);
			return declared != null && !declared.number() ? declared.defaultLevel() <= 0 || level >= declared.defaultLevel() : null;
		}
	}

	private static int level(ServerPlayer player) {
		return player.server.getProfilePermissions(player.getGameProfile());
	}

	public boolean may(Actor actor, String node, int fallbackLevel) {
		if (actor.console()) {
			return true;
		} else {
			Boolean b = null;
			if (this.external && actor.player() != null) {
				b = ServerPlatform.get().externalPermission(actor.player(), node);
			}

			if (b == null) {
				b = this.decide(actor.id(), actor.level(), actor.owner(), node);
			}

			return b != null ? b : fallbackLevel <= 0 || actor.level() >= fallbackLevel;
		}
	}

	public boolean may(Actor actor, Node node) {
		return this.may(actor, node.id(), node.defaultLevel());
	}

	public int number(Actor actor, Node node, int fallback) {
		Integer n = this.number(actor.id(), actor.level(), node.id());
		return n == null ? fallback : n;
	}

	@Nullable
	public Boolean decide(UUID player, int level, boolean owner, String node) {
		RolePerms.Cached c = this.cached(player, level);
		Byte known = c.answers.get(node);
		if (known == null) {
			byte answer = -1;

			for (Role role : c.order) {
				Boolean says = role.says(node);
				if (says != null) {
					answer = (byte)(says ? 1 : 0);
					break;
				}
			}

			known = answer;
			c.answers.put(node, known);
		}

		if (known >= 0) {
			return known == 1;
		} else {
			return (owner || level >= 4) && this.source.rules().operatorsHaveEverything ? Boolean.TRUE : null;
		}
	}

	@Nullable
	public Integer number(UUID player, int level, String node) {
		for (Role role : this.cached(player, level).order) {
			Integer n = role.numbers.get(node);
			if (n != null) {
				return n;
			}
		}

		return null;
	}

	public List<Role> held(UUID player, int level) {
		return new ArrayList<>(this.cached(player, level).held);
	}

	public int rank(UUID player, int level, boolean owner) {
		if (!owner && level < 4) {
			List<Role> held = this.cached(player, level).held;
			return held.isEmpty() ? Integer.MIN_VALUE : held.get(0).priority;
		} else {
			return Integer.MAX_VALUE;
		}
	}

	public int rank(Actor actor) {
		return actor.console() ? Integer.MAX_VALUE : this.rank(actor.id(), actor.level(), actor.owner());
	}

	public boolean outranks(Actor actor, int otherRank) {
		return actor.console() || this.rank(actor) > otherRank;
	}

	private RolePerms.Cached cached(UUID player, int level) {
		long now = this.source.now();
		RolePerms.Cached c = this.cache.get(player);
		if (c != null && c.level == level && now < c.until) {
			return c;
		} else {
			RoleBook book = this.source.roles();
			List<String> grants = this.source.grants(player);
			List<Role> held = new ArrayList<>();

			for (Role role : book.roles) {
				if (role.everyone || role.operators && level >= 2 || grants.contains(role.id)) {
					held.add(role);
				}
			}

			held.sort(Comparator.<Role>comparingInt(r -> r.priority).reversed());
			List<Role> order = new ArrayList<>(held.size() + 2);
			Set<String> seen = new HashSet<>();

			for (Role rolex : held) {
				visit(book, rolex, order, seen);
			}

			c = new RolePerms.Cached(level, this.source.validUntil(player), List.copyOf(held), List.copyOf(order));
			this.cache.put(player, c);
			return c;
		}
	}

	private static void visit(RoleBook book, Role role, List<Role> order, Set<String> seen) {
		if (seen.add(role.id)) {
			order.add(role);

			for (String id : role.inherits) {
				Role inherited = book.get(id);
				if (inherited != null) {
					visit(book, inherited, order, seen);
				}
			}
		}
	}

	public static List<Role> withInherited(RoleBook book, Role role) {
		List<Role> order = new ArrayList<>();
		visit(book, role, order, new HashSet<>());
		return order;
	}

	private static final class Cached {
		final int level;
		final long until;
		final List<Role> held;
		final List<Role> order;
		final Map<String, Byte> answers = new ConcurrentHashMap<>();

		Cached(int level, long until, List<Role> held, List<Role> order) {
			this.level = level;
			this.until = until;
			this.held = held;
			this.order = order;
		}
	}

	public interface Source {
		RoleBook roles();

		ServerRules rules();

		List<String> grants(UUID var1);

		long validUntil(UUID var1);

		long now();
	}
}
