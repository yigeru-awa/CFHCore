package com.onehumanawa.cfhcore.content.gui.server.client.ui.hub;

import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Node;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarPage;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerClient;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerState;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.ServerScreen;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Ui;
import com.onehumanawa.cfhcore.content.gui.server.model.Role;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class ServerHubScreen extends ServerScreen {
	public ServerHubScreen(@Nullable Screen parent) {
		super(Component.translatable("cfhgui_server.title"), parent, "cfhgui_server:hub", ServerClient.takePendingPage());
	}

	@Override
	protected List<String> pageIds() {
		if (ServerState.available() && !ServerState.newerServer()) {
			List<String> out = new ArrayList<>();
			boolean travel = ServerState.feature("homes") || ServerState.feature("back") || ServerState.feature("spawn") || ServerState.feature("random_teleport");
			if (travel) {
				out.add("homes");
			}

			if (ServerState.feature("warps")) {
				out.add("warps");
			}

			out.add("players");
			if (ServerState.feature("rankings")) {
				out.add("rankings");
			}

			if (ServerState.feature("mail")) {
				out.add("mail");
			}

			if (ServerState.feature("kits")) {
				out.add("kits");
			}

			out.add("info");
			return out;
		} else {
			return ServerClient.PAGES;
		}
	}

	@Nullable
	@Override
	protected SidebarPage page(String id) {
		return (SidebarPage)(switch (id) {
			case "homes" -> new HomesPage(this);
			case "warps" -> new WarpsPage(this, "warps", Txt.page("warps"), false);
			case "players" -> new PlayersPage(this);
			case "rankings" -> new RankingsPage(this);
			case "mail" -> new MailPage(this);
			case "kits" -> new KitsPage(this);
			case "info" -> new InfoPage(this);
			default -> null;
		});
	}

	@Override
	protected Node menuNode() {
		return Nodes.HUB_USE;
	}

	@Override
	protected int headerStatus(GuiGraphics g, int right, int y, int mouseX, int mouseY) {
		Component online = Txt.t("header.online", ServerState.online().size());
		right -= this.font.width(online);
		Ui.text(g, online, right, y, Theme.current().palette().textMuted(), 1.0F);
		g.fill(right - 7, y + 2, right - 3, y + 6, Theme.current().palette().success());
		right -= 14;
		List<Role.Tag> roles = ServerState.hello().roles;
		if (!roles.isEmpty()) {
			right -= Ui.roleChipWidth(roles.get(0));
			Ui.roleChip(g, roles.get(0), right, y - 2, 1.0F);
		}

		return right;
	}
}
