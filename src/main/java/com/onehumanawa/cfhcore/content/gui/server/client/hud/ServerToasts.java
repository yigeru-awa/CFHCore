package com.onehumanawa.cfhcore.content.gui.server.client.hud;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.server.ServerClientConfig;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerClient;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerState;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Ui;
import com.onehumanawa.cfhcore.content.gui.server.model.TeleportRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.Nullable;

final class ServerToasts {
	private static final Map<UUID, ServerToasts.Asked> REQUESTS = new HashMap<>();

	static void clear() {
		REQUESTS.clear();
	}

	static boolean asking(UUID from) {
		ServerToasts.Asked asked = REQUESTS.get(from);
		return asked != null && System.currentTimeMillis() < asked.untilMs();
	}

	static void notice(ServerState.NoticeView notice) {
		ServerClientConfig config = ServerClientConfig.get();
		if (config.notices && notice.key() != null && !notice.key().isBlank()) {
			String key = notice.key();
			String base = "cfhgui_server.notice." + key;
			if (!key.equals("tpa_received") && !key.equals("tpahere_received")) {
				boolean letter = key.equals("mail_received");
				if (!letter || config.mailToast) {
					Object[] args = notice.args() == null ? new Object[0] : notice.args().toArray();
					MutableComponent title;
					Component body;
					if (I18n.exists(base)) {
						title = Component.translatable(base, args);
						body = I18n.exists(base + ".body") ? Component.translatable(base + ".body", args) : null;
					} else {
						title = Txt.t("toast.unknown");
						body = args.length == 0 ? null : Component.literal(String.join(", ", notice.args()));
					}

					if ((notice.color() & 16777215) != 0) {
						int tint = Ui.ink(notice.color() & 16777215) & 16777215;
						title.withStyle(s -> s.withColor(tint));
					}

					String page = page(key);
					CFHGuiToasts.show(title, body, icon(key), page == null ? null : () -> {
						if (ServerClient.hubAvailable()) {
							ServerClient.open(page);
						}
					});
					if (letter) {
						sound();
					}
				}
			}
		}
	}

	private static Icon icon(String key) {
		return switch (key) {
			case "tpa_accepted" -> Icon.CHECK;
			case "tpa_denied", "tpa_expired" -> Icon.CLOSE;
			case "cancelled_moved", "cancelled_hurt" -> Icon.WORLD;
			case "mail_received" -> Icon.SEND;
			case "role_granted", "role_revoked" -> Icon.CROWN;
			case "kit_received" -> Icon.PACK;
			case "muted" -> Icon.MIC_OFF;
			case "unmuted" -> Icon.MIC;
			case "warned" -> Icon.WARNING;
			case "frozen" -> Icon.LOCK;
			case "unfrozen" -> Icon.UNLOCK;
			case "death_place" -> Icon.PIN;
			default -> key.contains("mail")
			? Icon.SEND
			: (key.contains("role") ? Icon.CROWN : (key.contains("kit") ? Icon.PACK : (key.contains("tp") ? Icon.WORLD : Icon.SERVER)));
		};
	}

	@Nullable
	private static String page(String key) {
		return switch (key) {
			case "tpa_accepted", "tpa_denied", "tpa_expired" -> "players";
			case "mail_received" -> "mail";
			case "kit_received" -> "kits";
			case "role_granted", "role_revoked", "warned", "muted" -> "info";
			case "death_place" -> "homes";
			default -> null;
		};
	}

	static void requests() {
		ServerClientConfig config = ServerClientConfig.get();
		Map<UUID, TeleportRequest> incoming = new HashMap<>();

		for (TeleportRequest r : ServerState.requests()) {
			if (r.incoming) {
				incoming.put(r.player, r);
			}
		}

		REQUESTS.entrySet().removeIf(e -> {
			if (incoming.containsKey(e.getKey())) {
				return false;
			} else {
				CFHGuiToasts.dismiss(e.getValue().title());
				return true;
			}
		});
		if (config.notices && config.requestToast && ServerState.available() && Minecraft.getInstance().player != null) {
			for (TeleportRequest rx : incoming.values()) {
				if (!REQUESTS.containsKey(rx.player)) {
					Component title = Txt.t("toast.request.title", rx.name);
					MutableComponent body = Txt.t(rx.here ? "toast.request.here" : "toast.request.to", rx.name);
					boolean keys = !ServerClient.ACCEPT_KEY.isUnbound() && !ServerClient.DENY_KEY.isUnbound();
					body.append("\n")
						.append(
							keys
								? Txt.t("toast.request.keys", ServerClient.ACCEPT_KEY.getTranslatedKeyMessage(), ServerClient.DENY_KEY.getTranslatedKeyMessage())
								: Txt.t("toast.request.open")
						);
					long now = System.currentTimeMillis();
					long left = rx.expiresAt - now;
					int life = (int)Math.max(4000L, Math.min(120000L, left <= 0L ? 60000L : left));
					REQUESTS.put(rx.player, new ServerToasts.Asked(title, now + (long)life));
					CFHGuiToasts.show(title, body, Icon.FRIENDS, () -> {
						if (ServerClient.hubAvailable()) {
							ServerClient.open("players");
						}
					}, life);
					sound();
				}
			}
		}
	}

	private static void sound() {
		if (ServerClientConfig.get().noticeSound) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.getSoundManager() != null) {
				mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.25F, 0.35F));
			}
		}
	}

	private ServerToasts() {
	}

	private static record Asked(Component title, long untilMs) {
	}
}
