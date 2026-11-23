package com.onehumanawa.cfhcore.content.gui.server.client.ui.admin;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Node;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiDropdown;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSearchField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSpinner;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToggle;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiAvatar.Status;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerActions;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerClient;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerState;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.ServerPage;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.dialog.AdminForms;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.dialog.Forms;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.PaintedCard;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Ui;
import com.onehumanawa.cfhcore.content.gui.server.model.AdminData;
import com.onehumanawa.cfhcore.content.gui.server.model.Home;
import com.onehumanawa.cfhcore.content.gui.server.model.Kit;
import com.onehumanawa.cfhcore.content.gui.server.model.PlayerRecord;
import com.onehumanawa.cfhcore.content.gui.server.model.Punishment;
import com.onehumanawa.cfhcore.content.gui.server.model.Role;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class AdminPlayersPage extends ServerPage {
	private static final List<String> FILTERS = List.of("all", "online", "offline", "muted", "banned", "frozen");
	private final AdminScreen admin;
	private CFHGuiScrollPanel panel;
	private String query = "";
	private String filter = "all";
	private int page;
	@Nullable
	private AdminData.PlayerPage data;
	private boolean loading;
	private int asked;
	private int debounce = -1;
	@Nullable
	private UUID open;
	@Nullable
	private AdminData.PlayerDetail detail;
	@Nullable
	private AdminData.RoleSet roleSet;
	@Nullable
	private UUID paced;
	@Nullable
	private String pace;
	private CFHGuiIconButton prev;
	private CFHGuiIconButton next;
	private Rect rangeAt = new Rect(0, 0, 0, 0);

	AdminPlayersPage(AdminScreen screen) {
		super(screen, "players", Txt.adminPage("players"), Icon.FRIENDS);
		this.admin = screen;
	}

	@Override
	protected Node node() {
		return Nodes.ADMIN_PLAYERS;
	}

	@Override
	protected void buildPage(SidebarScreen s, Rect area) {
		this.panel = null;
		UUID pending = AdminScreen.takePendingPlayer();
		if (pending != null) {
			this.open = pending;
			this.detail = null;
		}

		if (this.open != null) {
			this.buildSheet(s, area);
		} else {
			this.buildList(s, area);
		}
	}

	@Override
	public void stateChanged() {
	}

	public void tick() {
		if (this.debounce >= 0 && --this.debounce < 0) {
			this.page = 0;
			this.ask();
		}
	}

	private void buildList(SidebarScreen s, Rect area) {
		ServerPage.Bar bar = new ServerPage.Bar(s, area);
		this.next = bar.right(new CFHGuiIconButton(0, 0, 18, Icon.CHEVRON_RIGHT, Txt.t("pager.next"), () -> this.turn(1)));
		this.prev = bar.right(new CFHGuiIconButton(0, 0, 18, Icon.CHEVRON_LEFT, Txt.t("pager.prev"), () -> this.turn(-1)));
		CFHGuiDropdown<String> filters = new CFHGuiDropdown<>(0, 0, 92, FILTERS, this.filter, f -> Txt.t("aplayers.filter." + f), f -> {
			if (!f.equals(this.filter)) {
				this.filter = f;
				this.page = 0;
				this.ask();
			}
		});
		int rangeW = bar.room() >= 330 ? 100 : 0;
		CFHGuiSearchField search = bar.fill(new CFHGuiSearchField(0, 0, 160, q -> {
			String typed = q.trim();
			if (!typed.equals(this.query)) {
				this.query = typed;
				this.debounce = 6;
			}
		}), Math.min(220, bar.room() - 96 - rangeW - 8));
		search.setValue(this.query);
		search.placeholder(Txt.t("aplayers.search"));
		bar.left(filters);
		this.rangeAt = rangeW == 0 ? new Rect(0, 0, 0, 0) : new Rect(bar.leftX(), area.y(), Math.max(0, this.prev.getX() - 6 - bar.leftX()), 20);
		Rect r = under(area);
		this.panel = (CFHGuiScrollPanel)s.addPageWidget(new CFHGuiScrollPanel(r.x(), r.y(), r.w(), r.h()));
		if (this.data == null) {
			this.loading = true;
		}

		this.fillList();
		if (this.data == null) {
			this.ask();
		}
	}

	private void turn(int by) {
		int last = this.data == null ? 0 : Math.max(0, (this.data.total - 1) / 50);
		int to = Math.max(0, Math.min(last, this.page + by));
		if (to != this.page) {
			this.page = to;
			this.ask();
		}
	}

	private void ask() {
		this.loading = true;
		int request = ++this.asked;
		ServerActions.adminPlayers(this.query, this.filter, this.page, answer -> {
			if (request == this.asked) {
				this.loading = false;
				this.data = answer.map(AdminData.PlayerPage::sanitize).orElseGet(AdminData.PlayerPage::new);
				this.page = this.data.page;
				if (this.panel != null && this.open == null) {
					this.fillList();
					this.panel.snapScroll(0.0);
				}
			}
		});
	}

	private void fillList() {
		this.panel.clear();
		AdminData.PlayerPage d = this.data;
		int w = this.panel.innerWidth();
		if (d != null && !d.rows.isEmpty()) {
			int y = 0;

			for (AdminData.PlayerRow row : d.rows) {
				this.panel.add(this.row(row, w), 0, y);
				y += 28;
			}

			this.panel.setContentHeight(y);
		} else {
			int h = Math.max(96, this.panel.getHeight() - 4);
			boolean searching = !this.query.isEmpty() || !this.filter.equals("all");
			this.panel
				.add(
					new PaintedCard(
							w,
							h,
							(g, f) -> {
								if (this.loading) {
									CFHGuiSpinner.draw(g, f.x() + f.w() / 2 - 6, f.y() + f.h() / 2 - 6, 12, Ui.soft());
								} else {
									Ui.empty(
										g,
										new Rect(f.x(), f.y(), f.w(), f.h()),
										Icon.SEARCH,
										Txt.t("aplayers.none.title"),
										Txt.t(searching ? "aplayers.none" : "aplayers.none.all"),
										f.alpha()
									);
								}
							}
						)
						.bare(),
					0,
					0
				);
			this.panel.setContentHeight(h);
		}
	}

	private PaintedCard row(AdminData.PlayerRow row, int w) {
		return new PaintedCard(
				w,
				26,
				(g, f) -> {
					Palette p = Theme.current().palette();
					float a = f.alpha();
					Status status = !row.online ? Status.OFFLINE : (row.afk ? Status.AWAY : Status.ONLINE);
					Ui.head(g, row.id, row.name, f.x() + 6, f.y() + 5, 16, status, a * (row.online ? 1.0F : 0.7F));
					int right = f.x() + f.w() - 8;
					if (row.banned) {
						right = Ui.chipRight(g, Txt.t("flag.banned"), right, f.y() + 7, p.danger(), a) - 4;
					}

					if (row.muted) {
						right = Ui.chipRight(g, Txt.t("flag.muted"), right, f.y() + 7, p.warning(), a) - 4;
					}

					if (row.frozen) {
						right = Ui.chipRight(g, Txt.t("flag.frozen"), right, f.y() + 7, -10770208, a) - 4;
					}

					if (row.vanished) {
						right = Ui.chipRight(g, Txt.t("flag.vanished"), right, f.y() + 7, p.textMuted(), a) - 4;
					}

					if (row.afk) {
						right = Ui.chipRight(g, Txt.t("flag.afk"), right, f.y() + 7, p.warning(), a) - 4;
					}

					if (row.role != null && !row.role.name.isEmpty()) {
						right -= Ui.roleChipWidth(row.role);
						Ui.roleChip(g, row.role, right, f.y() + 7, a);
						right -= 4;
					}

					int room = right - f.x() - 36;
					Component shown = row.nick.isEmpty() ? Component.literal(row.name) : Txt.nick(row.nick);
					int used = Ui.textFit(g, shown, f.x() + 30, f.y() + 4, room, row.online ? Ui.strong() : Ui.soft(), a);
					if (!row.nick.isEmpty()) {
						Ui.textFit(g, Component.literal(row.name), f.x() + 36 + used, f.y() + 4, room - used - 6, Ui.caption(), a);
					}

					Component where = row.online
						? (row.dimension.isEmpty() ? Txt.t("aplayers.online") : Txt.t("aplayers.in", Txt.dimension(row.dimension)))
						: Txt.t("aplayers.seen", Txt.ago(row.lastSeen));
					Ui.textFit(g, where, f.x() + 30, f.y() + 15, room, Ui.caption(), a);
				}
			)
			.onClick(() -> {
				this.open = row.id;
				this.detail = null;
				this.screen.refreshPage();
			})
			.narrate(Component.literal(row.name));
	}

	public void render(SidebarScreen s, GuiGraphics g, Rect area, int mouseX, int mouseY, float partialTick) {
		if (this.open == null && this.panel != null && this.data != null && this.rangeAt.w() >= 60) {
			int total = this.data.total;
			this.prev.active = this.page > 0;
			this.next.active = (this.page + 1) * 50 < total;
			Component range = total == 0
				? Txt.t("pager.none")
				: Txt.t("pager.range", this.page * 50 + 1, Math.min(total, (this.page + 1) * 50), Txt.number((long)total));
			Ui.text(
				g,
				CFHGuiDraw.truncate(range, this.rangeAt.w()),
				Math.max(this.rangeAt.x(), this.rangeAt.right() - CFHGuiDraw.width(range)),
				this.rangeAt.y() + 6,
				Ui.caption(),
				1.0F
			);
		}
	}

	private void buildSheet(SidebarScreen s, Rect area) {
		ServerPage.Bar bar = new ServerPage.Bar(s, area);
		CFHGuiButton back = new CFHGuiButton(0, 0, 80, Txt.t("aplayers.back"), () -> {
			this.open = null;
			this.detail = null;
			this.paced = null;
			this.screen.refreshPage();
		}).variant(Variant.GHOST).icon(Icon.CHEVRON_LEFT);
		back.setWidth(back.preferredWidth() + 12);
		bar.left(back);
		bar.right(new CFHGuiIconButton(0, 0, 18, Icon.REFRESH, Txt.t("refresh"), this::reload));
		Rect r = under(area);
		this.panel = (CFHGuiScrollPanel)s.addPageWidget(new CFHGuiScrollPanel(r.x(), r.y(), r.w(), r.h()));
		if (this.detail == null) {
			this.waiting();
			this.reload();
		} else {
			this.fillSheet(this.detail);
		}
	}

	private void waiting() {
		int h = Math.max(96, this.panel.getHeight() - 4);
		this.panel
			.add(new PaintedCard(this.panel.innerWidth(), h, (g, f) -> CFHGuiSpinner.draw(g, f.x() + f.w() / 2 - 6, f.y() + f.h() / 2 - 6, 12, Ui.soft())).bare(), 0, 0);
		this.panel.setContentHeight(h);
	}

	private void reload() {
		UUID who = this.open;
		if (who != null) {
			int request = ++this.asked;
			if (this.roleSet == null && ServerState.may(Nodes.ADMIN_ROLES)) {
				ServerActions.adminRoles(answer -> {
					this.roleSet = answer.map(AdminData.RoleSet::sanitize).orElse(null);
					this.redraw();
				});
			}

			ServerActions.adminPlayer(who, answer -> {
				if (request == this.asked && who.equals(this.open)) {
					this.detail = answer.map(AdminData.PlayerDetail::sanitize).orElse(null);
					this.data = null;
					if (this.detail == null) {
						this.open = null;
						this.screen.refreshPage();
					} else {
						this.redraw();
					}
				}
			});
		}
	}

	private void redraw() {
		if (this.panel != null && this.open != null && this.detail != null) {
			AdminData.PlayerDetail d = this.detail;
			refill(this.panel, () -> this.fillSheet(d));
		}
	}

	private static CFHGuiButton small(Component label, @Nullable Icon icon, Runnable run) {
		CFHGuiButton b = new CFHGuiButton(0, 0, 60, 18, label, run);
		if (icon != null) {
			b.icon(icon).iconSize(8);
		}

		b.setWidth(b.preferredWidth() + 12);
		return b;
	}

	private int flow(int y, int w, List<? extends AbstractWidget> widgets) {
		int x = 0;
		int row = y;

		for (AbstractWidget b : widgets) {
			if (x > 0 && x + b.getWidth() > w) {
				x = 0;
				row += 22;
			}

			this.panel.add(b, x, row + (18 - Math.min(18, b.getHeight())) / 2);
			x += b.getWidth() + 4;
		}

		return widgets.isEmpty() ? y : row + 24;
	}

	private void fillSheet(AdminData.PlayerDetail d) {
		int w = this.panel.innerWidth();
		UUID id = d.row.id;
		String name = d.row.name;
		boolean on = d.row.online;
		boolean act = d.mayActOn;
		boolean can = act || id.equals(ServerState.selfId());
		Component outranked = Txt.t("aplayers.outranked", name);
		Component away = Txt.t("aplayers.away", name);
		Component why = on ? outranked : away;
		int y = 0;
		PaintedCard profile = this.profile(d, w);
		this.panel.add(profile, 0, y);
		y += profile.getHeight() + 6;
		List<AbstractWidget> doing = new ArrayList<>();
		if (ServerState.may(Nodes.MOD_TELEPORT)) {
			doing.add(small(Txt.t("aplayers.goto"), Icon.ARROW_RIGHT, () -> ServerActions.playerDo(id, "goto", null, AdminScreen.after(() -> {
				}))).tip(Txt.t(on ? "aplayers.goto.tip" : "aplayers.goto.away")));
			doing.add(gate(small(Txt.t("aplayers.bring"), Icon.DOWNLOAD, () -> ServerActions.playerDo(id, "bring", null, AdminScreen.after(() -> {
				}))), on && act, why));
		}

		if (ServerState.may(Nodes.MOD_HEAL)) {
			doing.add(
				gate(small(Txt.t("aplayers.heal"), Icon.HEART_FILLED, () -> ServerActions.playerDo(id, "heal", null, AdminScreen.after(this::reload))), on && can, why)
			);
			doing.add(gate(small(Txt.t("aplayers.feed"), null, () -> ServerActions.playerDo(id, "feed", null, AdminScreen.after(this::reload))), on && can, why));
			if (ServerState.feature("thirst")) {
				doing.add(gate(small(Txt.t("aplayers.drink"), null, () -> ServerActions.playerDo(id, "drink", null, AdminScreen.after(this::reload))), on && can, why));
			}
		}

		if (ServerState.may(Nodes.MOD_INVSEE)) {
			doing.add(gate(small(Txt.t("aplayers.inventory"), Icon.GRID, () -> ServerActions.playerDo(id, "invsee", null, AdminScreen.after(() -> {
				}))), on && can, why));
			doing.add(gate(small(Txt.t("aplayers.enderchest"), Icon.LOCK, () -> ServerActions.playerDo(id, "endersee", null, AdminScreen.after(() -> {
				}))), on && can, why));
		}

		if (ServerState.feature("stats")) {
			doing.add(
				gate(small(Txt.t("players.stats"), Icon.TROPHY, () -> ServerClient.openStats(id)), Minecraft.getInstance().player != null, Txt.t("state.no_world.short"))
			);
		}

		if (ServerState.may(Nodes.ADMIN_KITS)) {
			doing.add(small(Txt.t("aplayers.kit"), Icon.PACK, () -> this.giveKit(id, name)));
		}

		y = section(this.panel, y, w, Txt.t("aplayers.section.do"));
		y = this.flow(y, w, doing);
		List<AbstractWidget> state = new ArrayList<>();
		if (ServerState.may(Nodes.MOD_GAMEMODE)) {
			List<String> modes = List.of("survival", "creative", "adventure", "spectator");
			CFHGuiDropdown<String> mode = new CFHGuiDropdown<>(
				0,
				0,
				150,
				modes,
				modes.contains(d.gameMode) ? d.gameMode : "survival",
				m -> Component.translatable("selectWorld.gameMode." + m),
				m -> ServerActions.playerDo(id, "gamemode", m, AdminScreen.settled(ok -> this.reload()))
			);
			mode.label(Txt.t("aplayers.gamemode"));
			state.add(gate(mode, on && can, why));
		}

		if (ServerState.may(Nodes.MOD_SPEED)) {
			CFHGuiDropdown<String> speed = new CFHGuiDropdown<>(
				0,
				0,
				96,
				List.of("1", "2", "3", "5", "10"),
				id.equals(this.paced) ? this.pace : null,
				v -> Component.literal(v + "×"),
				v -> ServerActions.playerDo(id, "speed", v, AdminScreen.settled(ok -> {
						this.paced = ok ? id : null;
						this.pace = v;
						if (!ok) {
							this.redraw();
						}
					}))
			);
			speed.label(Txt.t("aplayers.speed"));
			if (on && can) {
				speed.tip(Txt.t("aplayers.speed.tip"));
			}

			state.add(gate(speed, on && can, why));
		}

		if (ServerState.may(Nodes.MOD_FLY)) {
			state.add(
				gate(toggle("aplayers.fly", d.fly, v -> ServerActions.playerDo(id, "fly", v ? "on" : "off", AdminScreen.settled(ok -> this.reload()))), on && can, why)
			);
		}

		if (ServerState.may(Nodes.MOD_GOD)) {
			CFHGuiToggle god = toggle("aplayers.god", d.god, v -> ServerActions.playerDo(id, "god", v ? "on" : "off", AdminScreen.settled(ok -> this.reload())));
			god.tip(Txt.t("aplayers.god.tip"));
			state.add(gate(god, on && can, why));
		}

		if (ServerState.may(Nodes.MOD_VANISH)) {
			state.add(
				gate(
					toggle("aplayers.vanish", d.row.vanished, v -> ServerActions.playerDo(id, "vanish", v ? "on" : "off", AdminScreen.settled(ok -> this.reload()))),
					on && can,
					why
				)
			);
		}

		y = this.flow(y, w, state);
		List<CFHGuiButton> mod = new ArrayList<>();
		if (ServerState.may(Nodes.MOD_WARN)) {
			mod.add(this.punish(d, Punishment.Kind.WARN, false, true));
		}

		if (ServerState.may(Nodes.MOD_MUTE)) {
			mod.add(this.punish(d, Punishment.Kind.MUTE, d.row.muted, true));
		}

		if (ServerState.may(Nodes.MOD_KICK)) {
			mod.add(this.punish(d, Punishment.Kind.KICK, false, on));
		}

		if (ServerState.may(Nodes.MOD_FREEZE)) {
			mod.add(this.punish(d, Punishment.Kind.FREEZE, d.row.frozen, true));
		}

		if (ServerState.may(Nodes.MOD_BAN)) {
			mod.add(this.punish(d, Punishment.Kind.BAN, d.row.banned, true));
		}

		if (!mod.isEmpty()) {
			y = section(this.panel, y + 2, w, Txt.t("aplayers.section.mod"));
			y = this.flow(y, w, mod);
		}

		if (ServerState.may(Nodes.ADMIN_ROLES)) {
			y = section(this.panel, y + 2, w, Txt.t("aplayers.section.roles", d.roles.size()));

			for (Role.Grant grant : d.roles) {
				this.panel.add(this.grant(d, grant, w), 0, y);
				y += 24;
			}

			CFHGuiButton give = small(Txt.t("aplayers.role.give"), Icon.PLUS, () -> this.giveRole(d));
			this.panel.add((CFHGuiButton)gate(give, act, outranked), 0, y);
			y += 26;
		}

		if (ServerState.may(Nodes.MOD_HISTORY)) {
			y = section(this.panel, y + 2, w, Txt.t("aplayers.section.notes", d.notes.size()));

			for (int i = 0; i < d.notes.size(); i++) {
				PaintedCard card = this.note(d, i, w);
				this.panel.add(card, 0, y);
				y += card.getHeight() + 4;
			}

			this.panel
				.add(
					small(
						Txt.t("aplayers.note.add"),
						Icon.EDIT,
						() -> new Forms.Prompt(
									Txt.t("aplayers.note.title", name),
									Icon.EDIT,
									Txt.t("aplayers.note.caption"),
									"",
									256,
									Txt.t("form.save"),
									v -> true,
									(v, then) -> ServerActions.noteAdd(id, v, r -> {
											then.accept(r);
											if (r.ok()) {
												this.reload();
											}
										})
								)
								.show()
					),
					0,
					y
				);
			y += 26;
			y = section(this.panel, y + 2, w, Txt.t("aplayers.section.history", d.punishments.size()));
			if (d.punishments.isEmpty()) {
				this.panel
					.add(
						new PaintedCard(w, 22, (g, f) -> Ui.textFit(g, Txt.t("aplayers.history.none"), f.x() + 8, f.y() + 7, f.w() - 16, Ui.caption(), f.alpha())).flat(), 0, y
					);
				y += 26;
			}

			for (int i = d.punishments.size() - 1; i >= 0; i--) {
				this.panel.add(ModerationPage.punishment(d.punishments.get(i), null, null, w, null), 0, y);
				y += 30;
			}
		}

		if (ServerState.may(Nodes.HOME_OTHERS) && !d.homes.isEmpty()) {
			y = section(this.panel, y + 2, w, Txt.t("aplayers.section.homes", d.homes.size()));

			for (Home home : d.homes) {
				this.panel.add(this.home(d, home, w), 0, y);
				y += 26;
			}
		}

		this.panel.setContentHeight(y + 4);
	}

	private static <T extends AbstractWidget> T gate(T widget, boolean enabled, Component why) {
		widget.active = enabled;
		if (!enabled && widget instanceof CFHGuiWidget sw) {
			sw.tip(why);
		}

		return widget;
	}

	private static CFHGuiToggle toggle(String key, boolean value, Consumer<Boolean> change) {
		CFHGuiToggle t = new CFHGuiToggle(0, 0, 80, Txt.t(key), value, change).switchFirst();
		t.setWidth(CFHGuiDraw.width(Txt.t(key)) + 36);
		return t;
	}

	private PaintedCard profile(AdminData.PlayerDetail d, int w) {
		return new PaintedCard(
				w,
				d.row.online ? 90 : 66,
				(g, f) -> {
					Palette p = Theme.current().palette();
					float a = f.alpha();
					AdminData.PlayerRow r = d.row;
					Ui.head(g, r.id, r.name, f.x() + 10, f.y() + 10, 44, !r.online ? Status.OFFLINE : (r.afk ? Status.AWAY : Status.ONLINE), a);
					int tx = f.x() + 64;
					int used = Ui.textFit(g, Fonts.heading(r.nick.isEmpty() ? Component.literal(r.name) : Txt.nick(r.nick)), tx, f.y() + 9, f.w() - 180, Ui.strong(), a);
					if (!r.nick.isEmpty()) {
						used += 6 + Ui.textFit(g, Component.literal(r.name), tx + used + 6, f.y() + 9, 80, Ui.caption(), a);
					}

					int cx = tx + used + 8;
					if (r.role != null) {
						cx += Ui.roleChip(g, r.role, cx, f.y() + 7, a) + 4;
					}

					if (d.operator) {
						cx += Ui.chip(g, Txt.t("flag.operator"), cx, f.y() + 7, p.accentInk(), a) + 4;
					}

					if (r.banned) {
						cx += Ui.chip(g, Txt.t("flag.banned"), cx, f.y() + 7, p.danger(), a) + 4;
					}

					if (r.muted) {
						cx += Ui.chip(g, Txt.t("flag.muted"), cx, f.y() + 7, p.warning(), a) + 4;
					}

					if (r.frozen) {
						Ui.chip(g, Txt.t("flag.frozen"), cx, f.y() + 7, -10770208, a);
					}

					Component where = r.online
						? Component.literal("")
							.append(Txt.t(r.afk ? "aplayers.online.afk" : "aplayers.online"))
							.append(d.place == null ? Component.empty() : Component.literal("  ·  ").append(Txt.dimension(d.place.dimension)).append("  ·  " + Txt.coords(d.place)))
						: Component.literal("")
							.append(Txt.t("aplayers.seen", Txt.ago(r.lastSeen)))
							.append(d.place == null ? Component.empty() : Component.literal("  ·  ").append(Txt.dimension(d.place.dimension)).append("  ·  " + Txt.coords(d.place)));
					Ui.textFit(g, where, tx, f.y() + 22, f.w() - 72, r.online ? p.success() : Ui.soft(), a);
					List<Component[]> facts = new ArrayList<>();
					facts.add(new Component[]{Txt.t("aplayers.fact.first"), Component.literal(d.firstSeen <= 0L ? "-" : Txt.date(d.firstSeen))});
					facts.add(new Component[]{Txt.t("aplayers.fact.sessions"), Component.literal(Txt.number(d.sessions))});
					facts.add(new Component[]{Txt.t("aplayers.fact.played"), Component.literal(Txt.ticks(d.playTicks))});
					facts.add(new Component[]{Txt.t("aplayers.fact.afk"), Component.literal(Txt.ticks(d.afkTicks))});
					if (r.online) {
						facts.add(new Component[]{Txt.t("aplayers.fact.health"), Component.literal(String.format(Locale.ROOT, "%.0f / %.0f", d.health, d.maxHealth))});
						facts.add(new Component[]{Txt.t("aplayers.fact.food"), Component.literal(d.food + " / 20")});
						facts.add(new Component[]{Txt.t("aplayers.fact.level"), Component.literal(Integer.toString(d.level))});
						facts.add(new Component[]{Txt.t("aplayers.fact.ping"), Component.literal(d.ping + " ms")});
					}

					int fw = Math.max(52, (f.w() - 72) / 4);

					for (int i = 0; i < facts.size(); i++) {
						int fx = tx + i % 4 * fw;
						int fy = f.y() + 37 + i / 4 * 24;
						if (fx + 40 <= f.x() + f.w()) {
							Ui.textFit(g, facts.get(i)[0], fx, fy, fw - 4, Ui.caption(), a);
							Ui.textFit(g, facts.get(i)[1], fx, fy + 11, fw - 4, Ui.strong(), a);
						}
					}
				}
			)
			.flat()
			.narrate(Component.literal(d.row.name));
	}

	private CFHGuiButton punish(AdminData.PlayerDetail d, Punishment.Kind kind, boolean holds, boolean possible) {
		UUID id = d.row.id;
		String name = d.row.name;
		CFHGuiButton b;
		if (holds) {
			b = small(
				Txt.t("punish." + kind.key() + ".lift"),
				Icon.UNLOCK,
				() -> Forms.confirm(
						Txt.t("punish." + kind.key() + ".lift.title", name),
						Txt.t("punish.lift.body"),
						Txt.t("punish." + kind.key() + ".lift"),
						() -> ServerActions.pardon(id, kind, AdminScreen.after(this::reload))
					)
			);
		} else {
			b = small(Txt.t("punish." + kind.key()), null, () -> new AdminForms.PunishForm(id, name, kind, this::reload).show());
			b.variant(kind != Punishment.Kind.BAN && kind != Punishment.Kind.KICK ? Variant.SECONDARY : Variant.DANGER);
		}

		if (!d.mayActOn) {
			gate(b, false, Txt.t("aplayers.outranked", name));
		} else if (!possible) {
			gate(b, false, Txt.t("aplayers.away", name));
		}

		return b;
	}

	private PaintedCard grant(AdminData.PlayerDetail d, Role.Grant grant, int w) {
		Role found = null;
		if (this.roleSet != null) {
			for (Role r : this.roleSet.roles) {
				if (r.id.equals(grant.role)) {
					found = r;
				}
			}
		}

		Role role = found;
		PaintedCard card = new PaintedCard(
				w,
				22,
				(g, f) -> {
					float a = f.alpha();
					int cw = role == null
						? Ui.chip(g, Component.literal(grant.role), f.x() + 6, f.y() + 5, Ui.soft(), a)
						: Ui.roleChip(g, new Role.Tag(role), f.x() + 6, f.y() + 5, a);
					Component by = grant.byName.isEmpty() ? Component.empty() : Txt.t("aplayers.role.by", grant.byName, Txt.ago(grant.at));
					int used = Ui.textFit(g, by, f.x() + 12 + cw, f.y() + 7, f.w() - cw - 150, Ui.caption(), a);
					Component until = grant.expiresAt == 0L ? Txt.t("time.forever") : Txt.t("aplayers.role.until", Txt.left(grant.expiresAt));
					Ui.textFit(
						g, until, f.x() + 20 + cw + used, f.y() + 7, f.w() - cw - used - 52, grant.expiresAt == 0L ? Ui.caption() : Theme.current().palette().warning(), a
					);
				}
			)
			.flat();
		CFHGuiIconButton take = new CFHGuiIconButton(
			0,
			0,
			16,
			Icon.CLOSE,
			Txt.t("aplayers.role.take"),
			() -> Forms.confirm(
					Txt.t("aplayers.role.take.title", role == null ? grant.role : role.name, d.row.name),
					Txt.t("aplayers.role.take.body"),
					Txt.t("aplayers.role.take"),
					() -> ServerActions.roleRevoke(d.row.id, grant.role, AdminScreen.after(this::reload))
				)
		);
		card.add((CFHGuiIconButton)gate(take, d.mayActOn, Txt.t("aplayers.outranked", d.row.name)), w - 22, 3);
		return card;
	}

	private void giveRole(AdminData.PlayerDetail d) {
		Runnable open = () -> {
			List<Role> can = new ArrayList<>();
			if (this.roleSet != null) {
				for (Role r : this.roleSet.roles) {
					if (!r.everyone && r.priority < this.roleSet.editableBelow) {
						can.add(r);
					}
				}
			}

			new AdminForms.GrantForm(d.row.id, d.row.name, can, this::reload).show();
		};
		if (this.roleSet != null) {
			open.run();
		} else {
			ServerActions.adminRoles(answer -> {
				this.roleSet = answer.map(AdminData.RoleSet::sanitize).orElse(null);
				open.run();
			});
		}
	}

	private void giveKit(UUID id, String name) {
		ServerActions.adminKits(
			answer -> {
				List<Kit> kits = answer.orElse(List.of());
				new AdminForms.PickForm<>(
						Txt.t("aplayers.kit.title", name),
						Icon.PACK,
						Txt.t("aplayers.kit.caption"),
						kits,
						k -> Component.literal(k.name.isEmpty() ? k.id : k.name),
						Txt.t("aplayers.kit.ok"),
						(k, then) -> ServerActions.kitGive(id, k.id, then)
					)
					.show();
			}
		);
	}

	private PaintedCard note(AdminData.PlayerDetail d, int index, int w) {
		PlayerRecord.Note note = d.notes.get(index);
		Component text = Component.literal(note.text);
		int th = Ui.wrappedHeight(text, w - 40, 6);
		PaintedCard card = new PaintedCard(w, th + 24, (g, f) -> {
			Ui.wrapped(g, text, f.x() + 8, f.y() + 6, f.w() - 40, 6, Theme.current().palette().text(), f.alpha());
			Ui.textFit(g, Txt.t("aplayers.role.by", note.byName, Txt.ago(note.at)), f.x() + 8, f.y() + 9 + th, f.w() - 16, Ui.caption(), f.alpha());
		}).flat();
		card.add(
			new CFHGuiIconButton(
				0,
				0,
				16,
				Icon.CLOSE,
				Txt.t("aplayers.note.delete"),
				() -> Forms.confirmDelete(
						Txt.t("aplayers.note.delete"), Txt.t("aplayers.note.delete.body"), () -> ServerActions.noteDelete(d.row.id, index, AdminScreen.after(this::reload))
					)
			),
			w - 22,
			4
		);
		return card;
	}

	private PaintedCard home(AdminData.PlayerDetail d, Home home, int w) {
		PaintedCard card = new PaintedCard(
				w,
				24,
				(g, f) -> {
					float a = f.alpha();
					Ui.markTile(g, home.icon, Icon.HOME, f.x() + 4, f.y() + 3, 18, home.color, a);
					int used = Ui.textFit(g, Component.literal(home.name), f.x() + 28, f.y() + 8, 90, Ui.ink(home.color), a);
					Ui.textFit(
						g,
						Component.literal("").append(Txt.dimension(home.place.dimension)).append("  ·  " + Txt.coords(home.place)),
						f.x() + 36 + used,
						f.y() + 8,
						f.w() - used - 160,
						Ui.caption(),
						a
					);
				}
			)
			.flat();
		CFHGuiButton delete = small(
			Txt.t("form.delete"),
			null,
			() -> Forms.confirm(
					Txt.t("aplayers.home.delete.title", d.row.name, home.name),
					Txt.t("home.delete.body"),
					Txt.t("form.delete"),
					() -> ServerActions.homeDelete(home.name, d.row.id, AdminScreen.after(this::reload))
				)
		);
		delete.variant(Variant.GHOST);
		card.add(delete, w - 6 - delete.getWidth(), 3);
		CFHGuiButton go = small(Txt.t("travel.go"), Icon.ARROW_RIGHT, () -> ServerActions.homeGo(home.name, d.row.id));
		card.add(go, w - 10 - delete.getWidth() - go.getWidth(), 3);
		return card;
	}
}
