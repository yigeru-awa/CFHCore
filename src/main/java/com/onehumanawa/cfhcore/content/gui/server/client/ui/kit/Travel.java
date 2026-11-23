package com.onehumanawa.cfhcore.content.gui.server.client.ui.kit;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerState;
import com.onehumanawa.cfhcore.content.gui.server.model.Place;
import com.onehumanawa.cfhcore.content.gui.server.model.SelfState;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class Travel {
	private static String seenPending;
	private static long seenAt;
	private static long endsAt;
	public static final String TP = "tp";
	public static final String RTP = "rtp";
	private static final Map<String, Travel.Wait> WAITS = new HashMap<>();

	@Nullable
	public static String dimension() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level == null ? null : mc.level.dimension().location().toString();
	}

	public static double distance(Place place) {
		LocalPlayer p = Minecraft.getInstance().player;
		if (p != null) {
			if (!place.dimension.equals(dimension())) {
				return -1.0;
			} else {
				double dx = place.x - p.getX();
				double dy = place.y - p.getY();
				double dz = place.z - p.getZ();
				return Math.sqrt(dx * dx + dy * dy + dz * dz);
			}
		} else {
			return ServerState.preview() && place.dimension.equals("minecraft:overworld") ? Math.sqrt(place.x * place.x + place.z * place.z) : -1.0;
		}
	}

	public static Component far(Place place) {
		double d = distance(place);
		return (Component)(d < 0.0 ? Txt.dimension(place.dimension) : Component.literal(Txt.distance(d)));
	}

	public static Component where(Place place) {
		double d = distance(place);
		return Component.literal(Txt.coords(place) + "  ·  ").append((Component)(d < 0.0 ? Txt.dimension(place.dimension) : Component.literal(Txt.distance(d))));
	}

	public static boolean pending() {
		SelfState s = ServerState.self();
		return s.pending != null && !s.pending.isEmpty();
	}

	public static Component pendingTarget() {
		String p = ServerState.self().pending;
		if (p == null) {
			return Component.empty();
		} else {
			int colon = p.indexOf(58);
			String kind = colon < 0 ? p : p.substring(0, colon);
			String name = colon < 0 ? "" : p.substring(colon + 1);

			return switch (kind) {
				case "home" -> Txt.t("travel.to.home", name);
				case "warp" -> Txt.t("travel.to.warp", name);
				case "player" -> Txt.t("travel.to.player", name);
				case "spawn" -> Txt.t("travel.to.spawn");
				case "back" -> Txt.t("travel.to.back");
				case "rtp" -> Txt.t("travel.to.random");
				default -> Txt.t("travel.to.place");
			};
		}
	}

	public static Icon pendingIcon() {
		String p = ServerState.self().pending;
		if (p == null) {
			return Icon.WORLD;
		} else if (p.startsWith("home")) {
			return Icon.HOME;
		} else if (p.startsWith("warp")) {
			return Icon.PIN;
		} else if (p.startsWith("player")) {
			return Icon.USER;
		} else {
			return p.startsWith("back") ? Icon.UNDO : Icon.WORLD;
		}
	}

	public static float pendingProgress() {
		track();
		long total = Math.max(1L, endsAt - seenAt);
		return Math.max(0.0F, Math.min(1.0F, (float)(System.currentTimeMillis() - seenAt) / (float)total));
	}

	public static long pendingLeftMs() {
		track();
		return Math.max(0L, endsAt - System.currentTimeMillis());
	}

	private static void track() {
		SelfState s = ServerState.self();
		String key = s.pending + "@" + s.pendingAt;
		if (!key.equals(seenPending)) {
			seenPending = key;
			long now = System.currentTimeMillis();
			long warmup = (long)Math.max(1, ServerState.number(Nodes.TP_WARMUP)) * 1000L;
			long left = s.pendingAt - now;
			seenAt = now;
			endsAt = now + (left >= 200L && left <= warmup + 2000L ? left : warmup);
		}
	}

	public static void heard() {
		SelfState s = ServerState.self();
		long now = System.currentTimeMillis();

		for (String what : new String[]{"tp", "rtp"}) {
			long until = s.cooldown(what);
			if (until <= 0L) {
				WAITS.remove(what);
			} else {
				Travel.Wait known = WAITS.get(what);
				if (known == null || known.until() != until) {
					long whole = (long)Math.max(0, ServerState.number("tp".equals(what) ? Nodes.TP_COOLDOWN : Nodes.RTP_COOLDOWN)) * 1000L;
					long left = until - now;
					WAITS.put(what, new Travel.Wait(until, now + (left > whole + 2000L ? whole : Math.max(0L, left))));
				}
			}
		}
	}

	public static long cooldownLeft(String what) {
		long until = ServerState.self().cooldown(what);
		if (until <= 0L) {
			return 0L;
		} else {
			Travel.Wait wait = WAITS.get(what);
			if (wait == null || wait.until() != until) {
				heard();
				wait = WAITS.get(what);
			}

			return wait == null ? 0L : Math.max(0L, (wait.endsAt() - System.currentTimeMillis() + 999L) / 1000L);
		}
	}

	private Travel() {
	}

	private static record Wait(long until, long endsAt) {
	}
}
