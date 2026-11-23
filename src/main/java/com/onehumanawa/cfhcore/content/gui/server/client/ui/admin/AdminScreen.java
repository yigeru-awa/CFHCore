package com.onehumanawa.cfhcore.content.gui.server.client.ui.admin;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Node;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarPage;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerActions;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerClient;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerState;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.ServerScreen;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.hub.WarpsPage;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Ui;
import com.onehumanawa.cfhcore.content.gui.server.model.AdminData;
import com.onehumanawa.cfhcore.content.gui.server.net.ServerNet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class AdminScreen extends ServerScreen {
	private boolean watching;
	@Nullable
	private static UUID pendingPlayer;

	public AdminScreen(@Nullable Screen parent) {
		super(Txt.t("admin.title"), parent, "cfhgui_server:admin", ServerClient.takePendingAdminPage());
	}

	@Override
	protected List<String> pageIds() {
		if (ServerState.available() && !ServerState.newerServer()) {
			List<String> out = new ArrayList<>();
			if (ServerState.may(Nodes.ADMIN_DASHBOARD)) {
				out.add("dashboard");
			}

			if (ServerState.may(Nodes.ADMIN_PLAYERS)) {
				out.add("players");
			}

			if (ServerState.may(Nodes.ADMIN_ROLES)) {
				out.add("roles");
			}

			if (ServerState.may(Nodes.WARP_SET) || ServerState.may(Nodes.SPAWN_SET)) {
				out.add("places");
			}

			if (ServerState.may(Nodes.ADMIN_KITS)) {
				out.add("kits");
			}

			if (ServerState.may(Nodes.MOD_HISTORY) || ServerState.may(Nodes.ADMIN_AUDIT)) {
				out.add("moderation");
			}

			if (ServerState.may(Nodes.ADMIN_RULES)) {
				out.add("rules");
			}

			if (ServerState.may(Nodes.ADMIN_WORLD)) {
				out.add("world");
			}

			for (AdminPages.Extra extra : AdminPages.all()) {
				if (extra.shown().getAsBoolean()) {
					out.add(extra.id());
				}
			}

			if (out.isEmpty()) {
				out.add("dashboard");
			}

			return out;
		} else {
			return ServerClient.ADMIN_PAGES;
		}
	}

	@Nullable
	@Override
	protected SidebarPage page(String id) {
		return (SidebarPage)(switch (id) {
			case "dashboard" -> new DashboardPage(this);
			case "players" -> new AdminPlayersPage(this);
			case "roles" -> new RolesPage(this);
			case "places" -> new WarpsPage(this, "places", Txt.adminPage("places"), true);
			case "kits" -> new AdminKitsPage(this);
			case "moderation" -> new ModerationPage(this);
			case "rules" -> new RulesPage(this);
			case "world" -> new WorldPage(this);
			default -> {
				AdminPages.Extra extra = AdminPages.get(id);
				yield extra == null ? null : extra.make().apply(this);
			}
		});
	}

	@Override
	protected Node menuNode() {
		return Nodes.ADMIN_MENU;
	}

	void watch() {
		if (!this.watching) {
			this.watching = true;
			ServerActions.adminWatch(true);
		}
	}

	private void unwatch() {
		if (this.watching) {
			this.watching = false;
			ServerActions.adminWatch(false);
		}
	}

	@Override
	protected void build() {
		super.build();
		if (ServerState.available() && !ServerState.newerServer() && ServerState.may(Nodes.ADMIN_MENU) && ServerState.may(Nodes.ADMIN_DASHBOARD)) {
			this.watch();
		} else {
			this.unwatch();
		}
	}

	@Override
	public void removed() {
		super.removed();
		this.unwatch();
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.currentPage() instanceof RolesPage roles && roles.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (this.currentPage() instanceof RolesPage roles && roles.mouseScrolled(mouseX, mouseY, scrollY)) {
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.currentPage() instanceof RolesPage roles && roles.keyPressed(keyCode, modifiers)) {
			return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	public void openPlayer(UUID player) {
		pendingPlayer = player;
		if (this.currentPage() != null && this.currentPage().id().equals("players")) {
			this.refreshPage();
		} else {
			this.go("players");
		}
	}

	@Nullable
	static UUID takePendingPlayer() {
		UUID p = pendingPlayer;
		pendingPlayer = null;
		return p;
	}

	static void pendingPlayer(@Nullable UUID player) {
		pendingPlayer = player;
	}

	static Consumer<ServerNet.Result> after(Runnable then) {
		return r -> {
			if (r.ok()) {
				then.run();
			} else {
				CFHGuiToasts.show(Component.translatable("cfhgui_server.result.failed"), ServerActions.describe(r), Icon.WARNING);
			}
		};
	}

	static Consumer<ServerNet.Result> settled(Consumer<Boolean> then) {
		return r -> {
			if (!r.ok()) {
				CFHGuiToasts.show(Component.translatable("cfhgui_server.result.failed"), ServerActions.describe(r), Icon.WARNING);
			}

			then.accept(r.ok());
		};
	}

	@Override
	protected int headerStatus(GuiGraphics g, int right, int y, int mouseX, int mouseY) {
		Palette p = Theme.current().palette();
		AdminData.Dashboard d = ServerState.dashboard();
		if (d != null) {
			right = Ui.chipRight(g, Txt.t("admin.header.tps", String.format(Locale.ROOT, "%.1f", d.tps)), right, y - 2, tpsColor(d.tps, p), 1.0F) - 6;
		}

		if (ServerState.hello().maintenance || d != null && d.maintenance) {
			right = Ui.chipRight(g, Txt.t("admin.header.maintenance"), right, y - 2, p.warning(), 1.0F) - 6;
		}

		return right;
	}

	static int tpsColor(double tps, Palette p) {
		return tps >= 19.0 ? p.success() : (tps >= 15.0 ? p.warning() : p.danger());
	}

	static int tickColor(double ms, Palette p) {
		return ms <= 40.0 ? p.success() : (ms <= 50.0 ? p.warning() : p.danger());
	}
}
