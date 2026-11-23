package com.onehumanawa.cfhcore.content.gui.server.client.ui;

import com.onehumanawa.cfhcore.content.gui.config.CFHGuiConfigApi;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.client.DevHarness;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.screen.ScreenIds;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Layout;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerActions;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerClient;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerState;
import com.onehumanawa.cfhcore.content.gui.server.client.settings.ServerSettingsTab;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.admin.AdminScreen;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.admin.AdminShots;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.dialog.Forms;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.hub.ServerHubScreen;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Travel;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.server.model.Home;
import com.onehumanawa.cfhcore.content.gui.server.model.Warp;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public final class ServerUi {
	@Nullable
	private static Runnable pendingDialog;
	private static boolean variantHeld;

	public static void init() {
		ScreenIds.register(ServerHubScreen.class, "cfhgui_server:hub", "Server menu");
		ScreenIds.register(AdminScreen.class, "cfhgui_server:admin", "Server administration");
		MenuSlots.provide("cfhgui_server:hub", Layout.CUSTOM, ServerHubScreen::new);
		MenuSlots.provide("cfhgui_server:admin", Layout.CUSTOM, AdminScreen::new);
		ServerState.SELF_CHANGED.register(Travel::heard);
		if (CFHGuiPlatform.get().isModLoaded("cfhgui_config")) {
			ServerUi.ConfigSide.init();
		}

		if (CFHGuiPlatform.get().isDevelopmentEnvironment() || System.getProperty("cfh.autoScreens") != null || System.getProperty("cfh.autoWorldScreens") != null
			)
		 {
			developmentScreens();
			AdminShots.init();
		}
	}

	@Nullable
	public static Runnable takePendingDialog() {
		Runnable d = pendingDialog;
		pendingDialog = null;
		return d;
	}

	public static void freshPreview() {
		if (variantHeld) {
			variantHeld = false;
		} else {
			if (DevHarness.active() && ServerState.preview()) {
				ServerState.resetPreview("full");
			}
		}
	}

	public static void holdVariant(String variant) {
		ServerState.resetPreview(variant);
		variantHeld = true;
	}

	public static void pendingDialog(Runnable open) {
		pendingDialog = open;
	}

	public static boolean hasSettings() {
		return CFHGuiPlatform.get().isModLoaded("cfhgui_config");
	}

	@Nullable
	public static Screen settingsScreen(@Nullable Screen parent) {
		return hasSettings() ? ServerUi.ConfigSide.screen(parent) : null;
	}

	private static void developmentScreens() {
		for (String page : ServerClient.PAGES) {
			state("cfhgui_server:state/noserver/" + page, "full", ServerState.Pretend.NO_SERVER, page);
			state("cfhgui_server:state/empty/" + page, "empty", ServerState.Pretend.NOTHING, page);
			state("cfhgui_server:state/player/" + page, "player", ServerState.Pretend.NOTHING, page);
		}

		state("cfhgui_server:state/noserver", "full", ServerState.Pretend.NO_SERVER, "homes");
		state("cfhgui_server:state/newer", "full", ServerState.Pretend.NEWER, "homes");
		CoreActions.SCREEN_FACTORIES.put("cfhgui_server:state/warmup", parent -> {
			holdVariant("full");
			ServerActions.homeGo("mine", null);
			return hub(parent, "homes");
		});
		dialog("home", "homes", () -> new Forms.HomeForm(null).show());
		dialog("home_edit", "homes", () -> {
			List<Home> h = ServerState.self().homes;
			if (!h.isEmpty()) {
				new Forms.HomeForm(h.get(Math.min(1, h.size() - 1))).show();
			}
		});
		dialog("home_taken", "homes", () -> takenHome(false));
		dialog("home_move", "homes", () -> takenHome(true));
		dialog("warp", "warps", () -> new Forms.WarpForm(null).show());
		dialog("warp_edit", "warps", () -> {
			List<Warp> w = ServerState.warps().warps;
			if (!w.isEmpty()) {
				new Forms.WarpForm(w.get(0)).show();
			}
		});
		dialog("mail", "mail", () -> new Forms.MailForm(null).show());
		dialog(
			"nick",
			"info",
			() -> new Forms.Prompt(Txt.t("info.nick.title"), Icon.EDIT, Txt.t("info.nick.caption"), "Night_Owl", 16, Txt.t("form.save"), v -> true, (v, then) -> {
				}).allowEmpty().hint(Txt.t("info.nick.hint")).show()
		);
		dialog("delete", "homes", () -> Forms.confirm(Txt.t("home.delete.title", "mine"), Txt.t("home.delete.body"), Txt.t("form.delete"), () -> {
			}));

		for (String scene : new String[]{"request", "warmup", "status", "muted", "notices"}) {
			CoreActions.SCREEN_FACTORIES.put("cfhgui_server:hud/" + scene, parent -> {
				if (!scene.equals("warmup") || !Travel.pending()) {
					ServerState.previewScene(scene);
				}

				return scene.equals("muted") ? new ChatScreen("") : null;
			});
		}

		for (String skin : new String[]{"DARK", "VANILLA"}) {
			CoreActions.SCREEN_FACTORIES.put("cfhgui_server:look/" + skin.toLowerCase(Locale.ROOT), parent -> {
				CFHGui.config().skin = skin;
				Theme.reload();
				Reskin.invalidate();
				holdVariant("full");
				return hub(parent, "homes");
			});
		}

		for (Layout layout : new Layout[]{Layout.VANILLA, Layout.CUSTOM}) {
			CoreActions.SCREEN_FACTORIES.put("cfhgui_server:look/layout/" + layout.key(), parent -> {
				CFHGui.config().setLayout(layout);
				Theme.reload();
				Reskin.invalidate();
				return null;
			});
		}

		for (int scale = 0; scale <= 4; scale++) {
			int s = scale;
			CoreActions.SCREEN_FACTORIES.put("cfhgui_server:look/scale/" + s, parent -> {
				Minecraft mc = Minecraft.getInstance();
				mc.options.guiScale().set(s);
				mc.resizeDisplay();
				return hub(parent, "homes");
			});
		}
	}

	private static void takenHome(boolean submit) {
		Forms.HomeForm form = new Forms.HomeForm(null);
		form.show();
		form.keyPressed(259, 0, 0);
		if (submit) {
			form.keyPressed(257, 0, 0);
		}
	}

	private static void state(String id, String variant, ServerState.Pretend pretend, String page) {
		CoreActions.SCREEN_FACTORIES.put(id, parent -> {
			holdVariant(variant);
			Screen screen = hub(parent, page);
			if (screen instanceof ServerScreen menu) {
				menu.pretending(pretend);
			}

			return screen;
		});
	}

	private static void dialog(String name, String page, Runnable open) {
		CoreActions.SCREEN_FACTORIES.put("cfhgui_server:dialog/" + name, parent -> {
			holdVariant("full");
			pendingDialog = open;
			return hub(parent, page);
		});
	}

	private static Screen hub(@Nullable Screen parent, String page) {
		Function<Screen, Screen> factory = (Function<Screen, Screen>)CoreActions.SCREEN_FACTORIES.get("cfhgui_server:hub/" + page);
		return factory == null ? MenuSlots.create("cfhgui_server:hub", parent) : factory.apply(parent);
	}

	private ServerUi() {
	}

	private static final class ConfigSide {
		static void init() {
			ServerSettingsTab.register();
			CoreActions.SCREEN_FACTORIES.put("cfhgui_server:config", ServerUi.ConfigSide::screen);
		}

		static Screen screen(@Nullable Screen parent) {
			return CFHGuiConfigApi.hub(parent, "gameplay/server");
		}
	}

	public static final class SampleVariant {
		public static final String FULL = "full";
		public static final String EMPTY = "empty";
		public static final String PLAYER = "player";

		private SampleVariant() {
		}
	}
}
