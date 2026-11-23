package com.onehumanawa.cfhcore.content.gui.teams.client;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.core.net.CFHGuiNetwork;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.teams.model.BannerData;
import com.onehumanawa.cfhcore.content.gui.teams.model.PingData;
import com.onehumanawa.cfhcore.content.gui.teams.model.Privacy;
import com.onehumanawa.cfhcore.content.gui.teams.net.TeamsNet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class TeamActions {
	private static int nextRequest = 1;
	private static final Map<Integer, Consumer<TeamsNet.Result>> WAITING = new HashMap<>();

	public static void create(String name, String tag, int color, @Nullable Consumer<TeamsNet.Result> then) {
		CompoundTag a = new CompoundTag();
		a.putString("name", name);
		a.putString("tag", tag);
		a.putInt("color", color & 16777215);
		send("create", a, then);
	}

	public static void disband(@Nullable Consumer<TeamsNet.Result> then) {
		send("disband", new CompoundTag(), then);
	}

	public static void leave(@Nullable Consumer<TeamsNet.Result> then) {
		send("leave", new CompoundTag(), then);
	}

	public static void rename(String name, @Nullable Consumer<TeamsNet.Result> then) {
		send("rename", string("name", name), then);
	}

	public static void setTag(String tag, @Nullable Consumer<TeamsNet.Result> then) {
		send("set_tag", string("tag", tag), then);
	}

	public static void setColor(int rgb) {
		CompoundTag a = new CompoundTag();
		a.putInt("color", rgb & 16777215);
		send("set_color", a, null);
	}

	public static void setMotto(String motto) {
		send("set_motto", string("motto", motto), null);
	}

	public static void setBanner(BannerData banner) {
		send("set_banner", string("banner", JsonWire.GSON.toJson(banner)), null);
	}

	public static void setPrivacy(Privacy privacy) {
		send("set_privacy", string("privacy", privacy.name()), null);
	}

	public static void setFriendlyFire(boolean on) {
		CompoundTag a = new CompoundTag();
		a.putBoolean("on", on);
		send("set_friendly_fire", a, null);
	}

	public static void setLandRule(String rule, String value) {
		CompoundTag a = new CompoundTag();
		a.putString("rule", rule);
		a.putString("value", value);
		send("set_land_rule", a, null);
	}

	public static void invite(String playerName, @Nullable Consumer<TeamsNet.Result> then) {
		send("invite", string("player", playerName), then);
	}

	public static void invite(UUID player, @Nullable Consumer<TeamsNet.Result> then) {
		send("invite", id("id", player), then);
	}

	public static void cancelInvite(UUID player) {
		send("cancel_invite", id("id", player), null);
	}

	public static void acceptInvite(UUID team, @Nullable Consumer<TeamsNet.Result> then) {
		send("accept_invite", id("team", team), then);
	}

	public static void declineInvite(UUID team) {
		send("decline_invite", id("team", team), null);
	}

	public static void join(UUID team, @Nullable Consumer<TeamsNet.Result> then) {
		send("join", id("team", team), then);
	}

	public static void cancelRequest(UUID team) {
		send("cancel_request", id("team", team), null);
	}

	public static void acceptRequest(UUID player) {
		send("accept_request", id("id", player), null);
	}

	public static void denyRequest(UUID player) {
		send("deny_request", id("id", player), null);
	}

	public static void kick(UUID player) {
		send("kick", id("id", player), null);
	}

	public static void promote(UUID player) {
		send("promote", id("id", player), null);
	}

	public static void demote(UUID player) {
		send("demote", id("id", player), null);
	}

	public static void transfer(UUID player, @Nullable Consumer<TeamsNet.Result> then) {
		send("transfer", id("id", player), then);
	}

	public static void ally(UUID team) {
		send("ally", id("team", team), null);
	}

	public static void unally(UUID team) {
		send("unally", id("team", team), null);
	}

	public static void setHome() {
		send("set_home", new CompoundTag(), null);
	}

	public static void clearHome() {
		send("clear_home", new CompoundTag(), null);
	}

	public static void goHome() {
		send("go_home", new CompoundTag(), null);
	}

	public static void addWaypoint(String name, int x, int y, int z, @Nullable String dimension, int color, String icon, @Nullable Consumer<TeamsNet.Result> then) {
		CompoundTag a = new CompoundTag();
		a.putString("name", name);
		a.putInt("x", x);
		a.putInt("y", y);
		a.putInt("z", z);
		if (dimension != null) {
			a.putString("dimension", dimension);
		}

		a.putInt("color", color & 16777215);
		a.putString("icon", icon);
		send("add_waypoint", a, then);
	}

	public static void editWaypoint(String waypointId, CompoundTag changes, @Nullable Consumer<TeamsNet.Result> then) {
		CompoundTag a = changes.copy();
		a.putString("id", waypointId);
		send("edit_waypoint", a, then);
	}

	public static void removeWaypoint(String waypointId) {
		send("remove_waypoint", string("id", waypointId), null);
	}

	public static void claim(@Nullable String dimension, Collection<Long> chunks, @Nullable Consumer<TeamsNet.Result> then) {
		send("claim", chunks(dimension, chunks), then);
	}

	public static void unclaim(@Nullable String dimension, Collection<Long> chunks, @Nullable Consumer<TeamsNet.Result> then) {
		send("unclaim", chunks(dimension, chunks), then);
	}

	public static void forceLoad(@Nullable String dimension, Collection<Long> chunks, boolean on, @Nullable Consumer<TeamsNet.Result> then) {
		CompoundTag a = chunks(dimension, chunks);
		a.putBoolean("on", on);
		send("force_load", a, then);
	}

	public static void claimServerLand(@Nullable String dimension, Collection<Long> chunks, @Nullable Consumer<TeamsNet.Result> then) {
		CompoundTag a = chunks(dimension, chunks);
		a.putBoolean("server", true);
		send("claim", a, then);
	}

	public static void unclaimServerLand(@Nullable String dimension, Collection<Long> chunks, @Nullable Consumer<TeamsNet.Result> then) {
		CompoundTag a = chunks(dimension, chunks);
		a.putBoolean("server", true);
		send("unclaim", a, then);
	}

	public static void chat(String message) {
		send("chat", string("message", message), null);
	}

	public static void ping(double x, double y, double z, int kind, int entity) {
		CompoundTag a = new CompoundTag();
		a.putDouble("x", x);
		a.putDouble("y", y);
		a.putDouble("z", z);
		a.putInt("kind", kind);
		a.putInt("entity", entity);
		send("ping", a, r -> {
		});
		if (TeamsState.preview()) {
			TeamsState.addPing(new PingData(TeamsState.self(), TeamsState.currentDimension(), x, y, z, kind, entity));
		}
	}

	public static void openStash() {
		send("open_stash", new CompoundTag(), null);
	}

	public static void sync() {
		send("sync", new CompoundTag(), r -> {
		});
	}

	public static void send(String op, CompoundTag args, @Nullable Consumer<TeamsNet.Result> then) {
		int request = nextRequest++;
		CFHGuiNetwork net = CFHGuiNetwork.get();
		if (TeamsState.connected() && net.serverHasChannel(TeamsNet.Action.TYPE)) {
			if (then != null) {
				WAITING.put(request, then);
			}

			net.sendToServer(new TeamsNet.Action(request, op, args));
		} else {
			deliver(TeamsNet.Result.fail(request, "disabled"), then);
		}
	}

	public static void onResult(TeamsNet.Result result) {
		deliver(result, WAITING.remove(result.request()));
	}

	private static void deliver(TeamsNet.Result result, @Nullable Consumer<TeamsNet.Result> then) {
		if (then != null) {
			then.accept(result);
		} else {
			if (!result.ok()) {
				CFHGuiToasts.show(Component.translatable("cfhgui_teams.result.failed"), describe(result), Icon.WARNING);
			}
		}
	}

	public static Component describe(TeamsNet.Result result) {
		return Component.translatable("cfhgui_teams.result." + result.code(), result.args().toArray());
	}

	static void reset() {
		WAITING.clear();
	}

	private static CompoundTag string(String key, String value) {
		CompoundTag a = new CompoundTag();
		a.putString(key, value == null ? "" : value);
		return a;
	}

	private static CompoundTag id(String key, UUID value) {
		CompoundTag a = new CompoundTag();
		a.putUUID(key, value);
		return a;
	}

	private static CompoundTag chunks(@Nullable String dimension, Collection<Long> chunks) {
		CompoundTag a = new CompoundTag();
		long[] packed = new long[chunks.size()];
		int i = 0;

		for (Long c : chunks) {
			packed[i++] = c;
		}

		a.putLongArray("chunks", packed);
		if (dimension != null) {
			a.putString("dimension", dimension);
		}

		return a;
	}

	private TeamActions() {
	}
}
