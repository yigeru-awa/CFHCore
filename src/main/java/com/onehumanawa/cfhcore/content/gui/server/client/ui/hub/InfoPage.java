package com.onehumanawa.cfhcore.content.gui.server.client.ui.hub;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToggle;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerActions;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerState;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.ServerPage;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.ServerScreen;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.dialog.Forms;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.PaintedCard;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Ui;
import com.onehumanawa.cfhcore.content.gui.server.model.Hello;
import com.onehumanawa.cfhcore.content.gui.server.model.Role;
import com.onehumanawa.cfhcore.content.gui.server.model.SelfState;
import com.onehumanawa.cfhcore.content.gui.server.model.ServerRules;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

final class InfoPage extends ServerPage {
	private CFHGuiScrollPanel panel;

	InfoPage(ServerScreen screen) {
		super(screen, "info", Txt.page("info"), Icon.INFO);
	}

	@Override
	protected void buildPage(SidebarScreen s, Rect area) {
		this.panel = (CFHGuiScrollPanel)s.addPageWidget(new CFHGuiScrollPanel(area.x(), area.y(), area.w(), area.h()));
		this.fill();
	}

	@Override
	public void stateChanged() {
		if (this.panel == null) {
			super.stateChanged();
		} else {
			refill(this.panel, this::fill);
		}
	}

	private void fill() {
		Hello hello = ServerState.hello();
		SelfState self = ServerState.self();
		int w = this.panel.innerWidth();
		int y = 0;
		y = section(this.panel, y, w, Txt.t("info.rules"));
		if (hello.rules.isEmpty()) {
			this.panel
				.add(new PaintedCard(w, 24, (g, f) -> Ui.textFit(g, Txt.t("info.rules.none"), f.x() + 8, f.y() + 8, f.w() - 16, Ui.caption(), f.alpha())).flat(), 0, y);
			y += 30;
		} else {
			List<List<FormattedCharSequence>> lines = new ArrayList<>();
			int h = 10;

			for (String rule : hello.rules) {
				if (rule != null && !rule.isBlank()) {
					List<FormattedCharSequence> wrapped = CFHGuiDraw.font().split(Component.literal(rule), Math.max(60, w - 44));
					lines.add(wrapped);
					h += wrapped.size() * 10 + 5;
				}
			}

			int height = h + 3;
			this.panel.add(new PaintedCard(w, height, (g, f) -> {
				Palette p = Theme.current().palette();
				int ly = f.y() + 8;

				for (int i = 0; i < lines.size(); i++) {
					Ui.textRight(g, Component.literal(i + 1 + "."), f.x() + 26, ly, p.accentInk(), f.alpha());

					for (FormattedCharSequence line : lines.get(i)) {
						Ui.text(g, line, f.x() + 32, ly, p.text(), f.alpha());
						ly += 10;
					}

					ly += 5;
				}
			}).flat(), 0, y);
			y += height + 6;
		}

		if (!hello.links.isEmpty()) {
			y = section(this.panel, y, w, Txt.t("info.links"));
			int x = 0;

			for (ServerRules.Link link : hello.links) {
				CFHGuiButton b = new CFHGuiButton(0, 0, 100, Component.literal(link.name), () -> ConfirmLinkScreen.confirmLinkNow(this.screen, link.url, false))
					.icon(Icon.EXTERNAL);
				b.setWidth(Math.min(w, b.preferredWidth() + 16));
				b.tip(Component.literal(link.url));
				if (x > 0 && x + b.getWidth() > w) {
					x = 0;
					y += 24;
				}

				this.panel.add(b, x, y);
				x += b.getWidth() + 6;
			}

			y += 28;
		}

		y = section(this.panel, y, w, Txt.t("info.you"));
		boolean nick = ServerState.feature("nicknames") && ServerState.may(Nodes.NICK_USE);
		PaintedCard you = new PaintedCard(w, 44, (g, f) -> {
			Palette p = Theme.current().palette();
			float a = f.alpha();
			Minecraft mc = Minecraft.getInstance();
			Ui.head(g, ServerState.selfId(), mc.getUser().getName(), f.x() + 8, f.y() + 8, 28, null, a);
			Component shown = self.nick.isEmpty() ? Component.literal(mc.getUser().getName()) : Txt.nick(self.nick);
			int used = Ui.textFit(g, Fonts.heading(shown), f.x() + 44, f.y() + 9, f.w() - 160, Ui.strong(), a);
			if (!self.nick.isEmpty()) {
				Ui.textFit(g, Txt.t("info.nick.of", mc.getUser().getName()), f.x() + 50 + used, f.y() + 9, f.w() - 210 - used, Ui.caption(), a);
			}

			int cx = f.x() + 44;
			if (hello.roles.isEmpty()) {
				Ui.text(g, Txt.t("info.roles.none"), cx, f.y() + 25, Ui.caption(), a);
			}

			for (Role.Tag role : hello.roles) {
				if (cx + Ui.roleChipWidth(role) > f.x() + f.w() - 8) {
					break;
				}

				cx += Ui.roleChip(g, role, cx, f.y() + 23, a) + 4;
			}

			if (self.mutedUntil != 0L) {
				Ui.chip(g, self.mutedUntil < 0L ? Txt.t("flag.muted") : Txt.t("flag.muted.until", Txt.left(self.mutedUntil)), cx, f.y() + 23, p.danger(), a);
			}
		}).flat();
		if (nick) {
			boolean colours = ServerState.may(Nodes.NICK_COLOR);
			String name = "[A-Za-z0-9_]{3,16}";
			CFHGuiButton change = new CFHGuiButton(
					0,
					0,
					100,
					18,
					Txt.t("info.nick.change"),
					() -> {
						Forms.Prompt prompt = new Forms.Prompt(
								Txt.t("info.nick.title"),
								Icon.EDIT,
								Txt.t("info.nick.caption"),
								self.nick,
								colours ? 64 : 16,
								Txt.t("form.save"),
								v -> (colours ? Txt.plainNick(v) : v).matches("[A-Za-z0-9_]{3,16}"),
								ServerActions::nick
							)
							.allowEmpty()
							.hint(Txt.t("info.nick.hint"));
						if (colours) {
							prompt.hint(Txt.t("info.nick.hint.colours"));
						}

						prompt.show();
					}
				)
				.icon(Icon.EDIT)
				.iconSize(8);
			change.setWidth(change.preferredWidth() + 12);
			you.add(change, w - 8 - change.getWidth(), 13);
		}

		this.panel.add(you, 0, y);
		y += 50;
		List<CFHGuiToggle> toggles = new ArrayList<>();
		if (ServerState.feature("afk") && ServerState.may(Nodes.AFK_USE)) {
			toggles.add((CFHGuiToggle)new CFHGuiToggle(0, 0, 100, Txt.t("info.afk"), self.afk, ServerActions::afk).tip(Txt.t("info.afk.tip")));
		}

		if (ServerState.feature("stats") && ServerState.may(Nodes.STATS_HIDE)) {
			toggles.add((CFHGuiToggle)new CFHGuiToggle(0, 0, 100, Txt.t("info.stats_hide"), self.statsHidden, ServerActions::statsHide).tip(Txt.t("info.stats_hide.tip")));
		}

		if (ServerState.feature("requests")) {
			toggles.add(
				(CFHGuiToggle)new CFHGuiToggle(0, 0, 100, Txt.t("players.accept"), self.acceptsRequests, ServerActions::tpaToggle).tip(Txt.t("players.accept.tip"))
			);
		}

		if (!toggles.isEmpty()) {
			y = grid(this.panel, y, w, toggles.size(), 200, 20, (i, cw) -> {
				toggles.get(i).setWidth(cw);
				return (AbstractWidget)toggles.get(i);
			});
			y += 4;
		}

		List<CFHGuiButton> tools = new ArrayList<>();
		boolean inGame = Minecraft.getInstance().player != null;
		if (ServerState.may(Nodes.TRASH_USE)) {
			tools.add(tool(Icon.TRASH, "info.tool.trash", "trash", inGame));
		}

		if (ServerState.may(Nodes.ENDERCHEST_USE)) {
			tools.add(tool(Icon.LOCK, "info.tool.enderchest", "enderchest", inGame));
		}

		if (ServerState.may(Nodes.WORKBENCH_USE)) {
			tools.add(tool(Icon.GRID, "info.tool.workbench", "workbench", inGame));
		}

		if (!tools.isEmpty()) {
			y = section(this.panel, y, w, Txt.t("info.tools"));
			y = grid(this.panel, y, w, tools.size(), 130, 20, (i, cw) -> {
				tools.get(i).setWidth(cw);
				return (AbstractWidget)tools.get(i);
			});
		}

		this.panel.setContentHeight(y + 2);
	}

	private static CFHGuiButton tool(Icon icon, String key, String what, boolean inGame) {
		CFHGuiButton b = new CFHGuiButton(0, 0, 100, Txt.t(key), () -> ServerActions.open(what)).icon(icon);
		b.enabled(inGame);
		b.tip(Txt.t(inGame ? key + ".tip" : "state.no_world.short"));
		return b;
	}
}
