package com.onehumanawa.cfhcore.content.gui.server.client.ui.hub;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiContextMenu;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel.Style;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup.Item;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerActions;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerState;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.ServerPage;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.ServerScreen;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.dialog.Forms;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.PaintedCard;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Travel;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Ui;
import com.onehumanawa.cfhcore.content.gui.server.model.Home;
import com.onehumanawa.cfhcore.content.gui.server.model.SelfState;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

final class HomesPage extends ServerPage {
	private CFHGuiScrollPanel panel;
	private CFHGuiLabel count;
	private CFHGuiButton set;
	private boolean hadPending;
	private boolean waitingTp;
	private boolean waitingRtp;

	HomesPage(ServerScreen screen) {
		super(screen, "homes", Txt.page("homes"), Icon.HOME);
	}

	@Override
	protected void buildPage(SidebarScreen s, Rect area) {
		ServerPage.Bar bar = new ServerPage.Bar(s, area);
		this.set = new CFHGuiButton(0, 0, 110, Txt.t("home.set_here"), () -> new Forms.HomeForm(null).show()).variant(Variant.PRIMARY).icon(Icon.PLUS);
		this.set.setWidth(this.set.preferredWidth() + 14);
		if (ServerState.feature("homes")) {
			bar.right(this.set);
		}

		this.count = bar.left(new CFHGuiLabel(0, 0, Math.max(40, bar.room() - 8), Component.empty()).style(Style.MUTED));
		Rect r = under(area);
		this.panel = (CFHGuiScrollPanel)s.addPageWidget(new CFHGuiScrollPanel(r.x(), r.y(), r.w(), r.h()));
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

	public void tick() {
		if (this.panel != null) {
			if (this.hadPending != Travel.pending() || this.waitingTp && Travel.cooldownLeft("tp") <= 0L || this.waitingRtp && Travel.cooldownLeft("rtp") <= 0L) {
				this.stateChanged();
			}
		}
	}

	private void fill() {
		SelfState self = ServerState.self();
		int w = this.panel.innerWidth();
		int max = ServerState.number(Nodes.HOMES_MAX);
		boolean homes = ServerState.feature("homes");
		boolean maySet = ServerState.may(Nodes.HOME_SET);
		boolean full = self.homes.size() >= max;
		this.count.text(homes ? Txt.t("home.count", self.homes.size(), max) : Component.empty());
		this.set.active = maySet && !full;
		this.set.tip(!maySet ? Txt.t("denied") : (full ? Txt.t("home.full", max) : Txt.t("home.set_here.tip")));
		int y = 0;
		this.hadPending = Travel.pending();
		if (this.hadPending) {
			this.panel.add(this.pending(w), 0, y);
			y += 38;
		}

		List<PaintedCard> ways = new ArrayList<>();
		this.waitingTp = Travel.cooldownLeft("tp") > 0L;
		this.waitingRtp = Travel.cooldownLeft("rtp") > 0L;
		if (ServerState.feature("back") && ServerState.may(Nodes.BACK_USE) && self.back != null) {
			ways.add(
				way(
					Icon.UNDO,
					Txt.t("travel.back"),
					waiting(
						"tp",
						() -> Component.literal("").append(self.backIsDeath ? Txt.t("travel.back.death") : Txt.t("travel.back.last")).append(", ").append(Travel.far(self.back))
					),
					15226170,
					!this.waitingTp,
					ServerActions::back
				)
			);
		}

		if (ServerState.feature("spawn") && ServerState.may(Nodes.SPAWN_USE) && ServerState.warps().spawn != null) {
			ways.add(
				way(Icon.WORLD, Txt.t("travel.spawn"), waiting("tp", () -> Travel.where(ServerState.warps().spawn)), 7323486, !this.waitingTp, ServerActions::spawnGo)
			);
		}

		if (ServerState.feature("random_teleport") && ServerState.may(Nodes.RTP_USE)) {
			ways.add(
				way(Icon.SPARKLE, Txt.t("travel.random"), waiting("rtp", () -> Txt.t("travel.random.sub")), 10185686, !this.waitingRtp, () -> ServerActions.rtp(null))
			);
		}

		if (!ways.isEmpty()) {
			y = grid(this.panel, y, w, ways.size(), 150, 34, (i, cw) -> {
				ways.get(i).setWidth(cw);
				return (AbstractWidget)ways.get(i);
			});
			y += 2;
		}

		if (!homes) {
			this.panel.setContentHeight(y);
		} else {
			y = section(this.panel, y + 2, w, Txt.t("home.section", self.homes.size()));
			if (self.homes.isEmpty()) {
				this.panel
					.add(
						new PaintedCard(
								w,
								86,
								(g, f) -> Ui.empty(
										g, new Rect(f.x(), f.y(), f.w(), f.h()), Icon.HOME, Txt.t("home.none.title"), Txt.t(maySet ? "home.none" : "home.none.denied"), f.alpha()
									)
							)
							.bare(),
						0,
						y
					);
				y += 90;
			} else {
				y = grid(this.panel, y, w, self.homes.size(), 190, 56, (i, cw) -> this.home(self.homes.get(i), cw));
			}

			this.panel.setContentHeight(y + 2);
		}
	}

	private PaintedCard pending(int w) {
		PaintedCard card = new PaintedCard(
				w,
				32,
				(g, f) -> {
					Palette p = Theme.current().palette();
					float a = f.alpha();
					Ui.wash(g, f.x() + 1, f.y() + 1, f.w() - 2, f.h() - 2, p.accent() & 16777215, a);
					Ui.glyphTile(g, Travel.pendingIcon(), f.x() + 6, f.y() + 6, 20, p.accentInk(), a);
					int room = f.w() - 34 - 76;
					Ui.textFit(
						g,
						Txt.t("travel.pending", Travel.pendingTarget(), String.format(Locale.ROOT, "%.1f", (float)Travel.pendingLeftMs() / 1000.0F)),
						f.x() + 32,
						f.y() + 6,
						room,
						Ui.strong(),
						a
					);
					Ui.textFit(g, Txt.t("travel.pending.cancels"), f.x() + 32, f.y() + 17, room, p.textMuted(), a);
					Ui.bar(g, f.x() + 2, f.y() + f.h() - 3, f.w() - 4, 2, Travel.pendingProgress(), p.accent(), a);
				}
			)
			.flat()
			.narrate(Txt.t("travel.pending.cancels"));
		card.add(new CFHGuiButton(0, 0, 64, 18, Component.translatable("gui.cancel"), ServerActions::tpCancel).variant(Variant.SECONDARY), w - 70, 7);
		return card;
	}

	private static Supplier<Component> waiting(String what, Supplier<Component> sub) {
		return () -> {
			long left = Travel.cooldownLeft(what);
			return (Component)(left > 0L ? Txt.t("travel.wait", Txt.span(left)) : sub.get());
		};
	}

	private static PaintedCard way(Icon icon, Component title, Supplier<Component> sub, int rgb, boolean ready, Runnable go) {
		PaintedCard card = new PaintedCard(150, 34, (g, f) -> {
			float a = f.alpha() * (ready ? 1.0F : 0.55F);
			Ui.glyphTile(g, icon, f.x() + 6, f.y() + 7, 20, rgb, a);
			Ui.textFit(g, Fonts.heading(title), f.x() + 32, f.y() + 7, f.w() - 38, Ui.strong(), a);
			Ui.textFit(g, sub.get(), f.x() + 32, f.y() + 18, f.w() - 38, Ui.soft(), a);
			f.tip(0, 0, f.w(), f.h(), List.of(title, sub.get().copy().withStyle(ChatFormatting.GRAY)));
		}).narrate(title);
		if (ready) {
			card.onClick(go);
		} else {
			card.flat();
		}

		return card;
	}

	private PaintedCard home(Home home, int w) {
		boolean mayGo = ServerState.may(Nodes.HOME_USE);
		boolean maySet = ServerState.may(Nodes.HOME_SET);
		PaintedCard card = new PaintedCard(w, 56, (g, f) -> {
			Theme t = Theme.current();
			float a = f.alpha();
			Ui.wash(g, f.x() + 1, f.y() + 1, Math.min(f.w() - 2, 120), f.h() - 2, home.color, a * 0.8F);
			Ui.markTile(g, home.icon, Icon.HOME, f.x() + 8, f.y() + 8, 26, home.color, a);
			Component dim = Txt.dimension(home.place.dimension);
			int chipLeft = Ui.chipRight(g, dim, f.x() + f.w() - 8, f.y() + 7, dimensionColor(home.place.dimension, t.palette()), a);
			Ui.textFit(g, Fonts.heading(home.name), f.x() + 42, f.y() + 9, chipLeft - f.x() - 48, Ui.ink(home.color), a);
			Ui.textFit(g, Travel.where(home.place), f.x() + 42, f.y() + 21, f.w() - 50, Ui.soft(), a);
		}).flat().narrate(Component.literal(home.name));
		CFHGuiButton go = new CFHGuiButton(0, 0, 76, 16, Txt.t("travel.go"), () -> ServerActions.homeGo(home.name, null));
		go.icon(Icon.ARROW_RIGHT).iconSize(8);
		go.enabled(mayGo && !this.waitingTp);
		if (!mayGo) {
			go.tip(Txt.t("denied"));
		} else if (this.waitingTp) {
			go.tip(Txt.t("travel.wait.tip"));
		}

		card.add(go, w - 84, 34);
		if (maySet) {
			CFHGuiIconButton more = new CFHGuiIconButton(0, 0, 16, Icon.DOTS, Txt.t("more"), null);
			more.onPress(() -> this.menu(home, more.getX() - 96, more.getY() + 17));
			card.add(more, w - 104, 34);
			card.onRightClick(() -> this.menu(home, Ui.mouseX(), Ui.mouseY()));
		}

		return card;
	}

	static int dimensionColor(String dimension, Palette p) {
		return switch (dimension) {
			case "minecraft:overworld" -> p.success();
			case "minecraft:the_nether" -> p.danger();
			case "minecraft:the_end" -> -6591530;
			default -> p.textMuted();
		};
	}

	private void menu(Home home, int x, int y) {
		List<Item> items = new ArrayList<>();
		items.add(Item.of(Txt.t("home.edit"), Icon.EDIT, () -> new Forms.HomeForm(home).show()));
		items.add(
			Item.of(
				Txt.t("home.move"),
				Icon.PIN,
				() -> Forms.confirm(
						Txt.t("home.move.title", home.name), Txt.t("home.move.body", home.name), Txt.t("home.move"), () -> ServerActions.homeSet(home.name, null, -1, null)
					)
			)
		);
		items.add(Item.sep());
		items.add(
			Item.danger(
				Txt.t("form.delete"),
				Icon.TRASH,
				() -> Forms.confirmDelete(Txt.t("home.delete.title", home.name), Txt.t("home.delete.body"), () -> ServerActions.homeDelete(home.name, null, null))
			)
		);
		CFHGuiContextMenu.open((double)x, (double)y, items);
	}
}
