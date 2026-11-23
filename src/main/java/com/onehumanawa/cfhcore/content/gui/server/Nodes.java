package com.onehumanawa.cfhcore.content.gui.server;

import com.onehumanawa.cfhcore.content.gui.core.perm.Perms;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Node;

public final class Nodes {
	private static final String P = "cfhgui_server.";
	public static final Node HOME_USE = flag("home.use", 0);
	public static final Node HOME_SET = flag("home.set", 0);
	public static final Node HOME_OTHERS = flag("home.others", 2);
	public static final Node HOMES_MAX = number("homes.max", 3);
	public static final Node BACK_USE = flag("back.use", 0);
	public static final Node BACK_DEATH = flag("back.death", 0);
	public static final Node SPAWN_USE = flag("spawn.use", 0);
	public static final Node SPAWN_SET = flag("spawn.set", 3);
	public static final Node WARP_USE = flag("warp.use", 0);
	public static final Node WARP_SET = flag("warp.set", 2);
	public static final Node TPA_USE = flag("tpa.use", 0);
	public static final Node TPA_HERE = flag("tpa.here", 0);
	public static final Node RTP_USE = flag("rtp.use", 0);
	public static final Node RTP_COOLDOWN = number("rtp.cooldown", 300);
	public static final Node TP_WARMUP = number("tp.warmup", 3);
	public static final Node TP_COOLDOWN = number("tp.cooldown", 5);
	public static final Node TP_POSITION = flag("tp.position", 2);
	public static final Node TP_ANYWHERE = flag("tp.anywhere", 2);
	public static final Node AFK_USE = flag("afk.use", 0);
	public static final Node AFK_EXEMPT = flag("afk.exempt", 2);
	public static final Node SEEN_USE = flag("seen.use", 0);
	public static final Node PLAYTIME_USE = flag("playtime.use", 0);
	public static final Node STATS_OTHERS = flag("stats.others", 0);
	public static final Node STATS_HIDE = flag("stats.hide", 0);
	public static final Node STATS_HIDDEN = flag("stats.hidden", 2);
	public static final Node RANKINGS_USE = flag("rankings.use", 0);
	public static final Node MAIL_USE = flag("mail.use", 0);
	public static final Node NICK_USE = flag("nick.use", 0);
	public static final Node NICK_COLOR = flag("nick.color", 2);
	public static final Node KIT_USE = flag("kit.use", 0);
	public static final Node TRASH_USE = flag("trash.use", 0);
	public static final Node ENDERCHEST_USE = flag("enderchest.use", 2);
	public static final Node WORKBENCH_USE = flag("workbench.use", 2);
	public static final Node HUB_USE = flag("hub.use", 0);
	public static final Node MOD_MUTE = flag("mod.mute", 2);
	public static final Node MOD_KICK = flag("mod.kick", 2);
	public static final Node MOD_WARN = flag("mod.warn", 2);
	public static final Node MOD_FREEZE = flag("mod.freeze", 2);
	public static final Node MOD_BAN = flag("mod.ban", 2);
	public static final Node MOD_BAN_PERMANENT = flag("mod.ban.permanent", 3);
	public static final Node MOD_HISTORY = flag("mod.history", 2);
	public static final Node MOD_VANISH = flag("mod.vanish", 2);
	public static final Node MOD_SEE_VANISHED = flag("mod.vanish.see", 2);
	public static final Node MOD_INVSEE = flag("mod.invsee", 2);
	public static final Node MOD_INVSEE_EDIT = flag("mod.invsee.edit", 3);
	public static final Node MOD_HEAL = flag("mod.heal", 2);
	public static final Node MOD_FLY = flag("mod.fly", 2);
	public static final Node MOD_GOD = flag("mod.god", 2);
	public static final Node MOD_SPEED = flag("mod.speed", 2);
	public static final Node MOD_GAMEMODE = flag("mod.gamemode", 2);
	public static final Node MOD_TELEPORT = flag("mod.teleport", 2);
	public static final Node MOD_BROADCAST = flag("mod.broadcast", 2);
	public static final Node MOD_STAFF_CHAT = flag("mod.staffchat", 2);
	public static final Node MOD_EXEMPT = flag("mod.exempt", 3);
	public static final Node ADMIN_MENU = flag("admin.menu", 2);
	public static final Node ADMIN_DASHBOARD = flag("admin.dashboard", 2);
	public static final Node ADMIN_PLAYERS = flag("admin.players", 2);
	public static final Node ADMIN_ROLES = flag("admin.roles", 3);
	public static final Node ADMIN_KITS = flag("admin.kits", 3);
	public static final Node ADMIN_AUDIT = flag("admin.audit", 3);
	public static final Node ADMIN_RULES = flag("admin.rules", 4);
	public static final Node ADMIN_WORLD = flag("admin.world", 3);
	public static final Node ADMIN_MAINTENANCE = flag("admin.maintenance", 4);
	public static final Node MAINTENANCE_BYPASS = flag("maintenance.bypass", 2);
	public static final Node ADMIN_SELFTEST = flag("admin.selftest", 4);

	private static Node flag(String path, int level) {
		return Perms.declare("cfhgui_server." + path, level);
	}

	private static Node number(String path, int fallback) {
		return Perms.declareNumber("cfhgui_server." + path, fallback);
	}

	public static void init() {
	}

	private Nodes() {
	}
}
