package com.onehumanawa.cfhcore.content.gui.teams.client.hud;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.teams.TeamsConfig;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsClient;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsState;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

final class Notices {
	static void show(TeamsState.NoticeView notice) {
		if (TeamsConfig.get().notices && notice.key() != null && !notice.key().isBlank()) {
			String key = notice.key();
			String base = "cfhgui_teams.notice." + key;
			Object[] args = notice.args() == null ? new Object[0] : notice.args().toArray();

			for (int i = 0; i < args.length; i++) {
				Object tint = args[i];
				if (tint instanceof String) {
					String s = (String)tint;
					if (!s.isEmpty() && s.length() <= 16 && I18n.exists("cfhgui_teams.rank." + s)) {
						args[i] = Component.translatable("cfhgui_teams.rank." + s);
					}
				}
			}

			MutableComponent title;
			Component body;
			if (I18n.exists(base)) {
				title = Component.translatable(base, args);
				body = I18n.exists(base + ".body") ? Component.translatable(base + ".body", args) : null;
			} else {
				title = Component.translatable("cfhgui_teams.hud.notice.unknown");
				body = args.length == 0 ? null : Component.literal(String.join(", ", notice.args()));
			}

			if ((notice.color() & 16777215) != 0) {
				int tint = HudDraw.readable(notice.color()) & 16777215;
				title.withStyle(s -> s.withColor(tint));
			}

			CFHGuiToasts.show(title, body, icon(key), () -> TeamsClient.open(page(key)));
		}
	}

	private static Icon icon(String key) {
		return switch (key) {
			case "invited", "declined" -> Icon.INVITE;
			case "request" -> Icon.BELL;
			case "joined", "accepted" -> Icon.FRIENDS;
			case "left" -> Icon.EXIT;
			case "kicked", "denied" -> Icon.BLOCKED;
			case "promoted" -> Icon.ARROW_UP;
			case "demoted" -> Icon.ARROW_DOWN;
			case "transferred" -> Icon.CROWN;
			case "ally", "ally_mutual", "unally" -> Icon.SHIELD;
			case "level" -> Icon.STAR_FILLED;
			case "disbanded" -> Icon.WARNING;
			default -> key.contains("claim") || key.contains("land")
			? Icon.MAP
			: (key.contains("waypoint") ? Icon.PIN : (key.contains("home") ? Icon.HOME : (!key.contains("quest") && !key.contains("chapter") ? Icon.TEAM : Icon.QUEST)));
		};
	}

	private static String page(String key) {
		return switch (key) {
			case "invited", "denied", "disbanded", "kicked", "level" -> "overview";
			case "joined", "left", "promoted", "demoted", "transferred", "request", "accepted", "declined" -> "members";
			case "ally", "ally_mutual", "unally" -> "allies";
			default -> key.contains("ally")
			? "allies"
			: (!key.contains("claim") && !key.contains("land") ? (key.contains("waypoint") ? "waypoints" : "overview") : "land");
		};
	}

	private Notices() {
	}
}
