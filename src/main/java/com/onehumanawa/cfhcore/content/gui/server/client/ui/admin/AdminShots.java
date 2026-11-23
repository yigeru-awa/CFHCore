package com.onehumanawa.cfhcore.content.gui.server.client.ui.admin;

import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerActions;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerState;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.ServerScreen;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.ServerUi;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.dialog.AdminForms;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.dialog.Forms;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.server.model.Punishment;
import com.onehumanawa.cfhcore.content.gui.server.model.Role;
import com.onehumanawa.cfhcore.content.gui.server.model.RoleBook;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public final class AdminShots {
	private static UUID sample(String name) {
		return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
	}

	public static void init() {
		put("cfhgui_server:admin/player", "players", () -> AdminScreen.pendingPlayer(sample("Juno")), null);

		for (String tab : new String[]{"general", "permissions", "limits"}) {
			put("cfhgui_server:admin/role/" + tab, "roles", () -> {
				RolesPage.pendingRole = "moderator";
				RolesPage.pendingTab = tab;
			}, null);
		}

		for (String view : new String[]{"active", "history", "audit"}) {
			put("cfhgui_server:admin/moderation/" + view, "moderation", () -> ModerationPage.pendingView = view, null);
		}

		UUID juno = sample("Juno");
		put(
			"cfhgui_server:admin/dialog/mute",
			"players",
			() -> AdminScreen.pendingPlayer(juno),
			() -> new AdminForms.PunishForm(juno, "Juno", Punishment.Kind.MUTE, () -> {
				}).show()
		);
		put(
			"cfhgui_server:admin/dialog/ban",
			"players",
			() -> AdminScreen.pendingPlayer(juno),
			() -> new AdminForms.PunishForm(juno, "Juno", Punishment.Kind.BAN, () -> {
				}).show()
		);
		put(
			"cfhgui_server:admin/dialog/warn",
			"players",
			() -> AdminScreen.pendingPlayer(juno),
			() -> new AdminForms.PunishForm(juno, "Juno", Punishment.Kind.WARN, () -> {
				}).show()
		);
		put("cfhgui_server:admin/dialog/grant", "players", () -> AdminScreen.pendingPlayer(juno), () -> {
			List<Role> roles = RoleBook.defaults().roles;
			roles.removeIf(r -> r.everyone);
			new AdminForms.GrantForm(juno, "Juno", roles, () -> {
			}).show();
		});
		put("cfhgui_server:admin/dialog/kit", "kits", () -> {
		}, () -> new AdminForms.KitForm(null, List.of(), () -> {
			}).show());
		put("cfhgui_server:admin/dialog/kit_taken", "kits", () -> {
		}, () -> ServerActions.adminKits(kits -> {
				AdminForms.KitForm form = new AdminForms.KitForm(null, kits.orElse(List.of()), () -> {
				});
				form.show();

				for (char c : "Starter".toCharArray()) {
					form.charTyped(c, 0);
				}
			}));
		put("cfhgui_server:admin/dialog/lines", "rules", () -> {
		}, () -> new AdminForms.LinesForm(Txt.t("rule.rules"), Txt.t("rules.lines.hint"), ServerState.hello().rules, 200, v -> true, (lines, then) -> {
			}).show());
		put(
			"cfhgui_server:admin/dialog/broadcast",
			"dashboard",
			() -> {
			},
			() -> new Forms.Prompt(
						Txt.t("dash.broadcast.title"), Icon.SEND, Txt.t("dash.broadcast.caption"), "", 200, Txt.t("dash.broadcast.ok"), v -> true, (v, then) -> {
						}
					)
					.show()
		);
		put(
			"cfhgui_server:admin/dialog/lift",
			"moderation",
			() -> ModerationPage.pendingView = "active",
			() -> Forms.confirm(Txt.t("punish.mute.lift.title", "Juno"), Txt.t("punish.lift.body"), Txt.t("punish.mute.lift"), () -> {
				})
		);
		CoreActions.SCREEN_FACTORIES.put("cfhgui_server:admin/state/denied", parent -> {
			ServerUi.holdVariant("player");
			return admin(parent, "dashboard");
		});
		CoreActions.SCREEN_FACTORIES.put("cfhgui_server:admin/state/noserver", parent -> {
			ServerUi.holdVariant("full");
			Screen screen = admin(parent, "dashboard");
			if (screen instanceof ServerScreen menu) {
				menu.pretending(ServerState.Pretend.NO_SERVER);
			}

			return screen;
		});
	}

	private static void put(String id, String page, Runnable before, @Nullable Runnable dialog) {
		CoreActions.SCREEN_FACTORIES.put(id, parent -> {
			ServerUi.holdVariant("full");
			before.run();
			if (dialog != null) {
				ServerUi.pendingDialog(dialog);
			}

			return admin(parent, page);
		});
	}

	private static Screen admin(@Nullable Screen parent, String page) {
		Function<Screen, Screen> factory = (Function<Screen, Screen>)CoreActions.SCREEN_FACTORIES.get("cfhgui_server:admin/" + page);
		return factory == null ? MenuSlots.create("cfhgui_server:admin", parent) : factory.apply(parent);
	}

	private AdminShots() {
	}
}
