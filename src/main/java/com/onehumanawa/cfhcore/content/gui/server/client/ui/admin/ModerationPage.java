package com.onehumanawa.cfhcore.content.gui.server.client.ui.admin;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiDropdown;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSearchField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSegmented;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSpinner;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerActions;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerState;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.ServerPage;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.dialog.Forms;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.PaintedCard;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Ui;
import com.onehumanawa.cfhcore.content.gui.server.model.AdminData;
import com.onehumanawa.cfhcore.content.gui.server.model.Punishment;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class ModerationPage extends ServerPage {
	static final String ACTIVE = "active";
	static final String HISTORY = "history";
	static final String AUDIT = "audit";
	private static final List<String> KINDS = List.of("all", "mute", "ban", "warn", "kick", "freeze");
	@Nullable
	static String pendingView;
	private final AdminScreen admin;
	private CFHGuiScrollPanel panel;
	private String view = "active";
	private String kind = "all";
	private String query = "";
	private int page;
	private int total;
	private int asked;
	private int debounce = -1;
	private boolean loading;
	private List<AdminData.PunishmentRow> punishments = List.of();
	private List<AdminData.AuditEntry> audit = List.of();
	private boolean loaded;
	private CFHGuiIconButton prev;
	private CFHGuiIconButton next;
	private Rect rangeAt = new Rect(0, 0, 0, 0);

	ModerationPage(AdminScreen screen) {
		super(screen, "moderation", Txt.adminPage("moderation"), Icon.SHIELD);
		this.admin = screen;
	}

	private static List<String> views() {
		List<String> out = new ArrayList<>();
		if (ServerState.may(Nodes.MOD_HISTORY)) {
			out.add("active");
			out.add("history");
		}

		if (ServerState.may(Nodes.ADMIN_AUDIT)) {
			out.add("audit");
		}

		return out;
	}

	@Override
	protected void buildPage(SidebarScreen s, Rect area) {
		this.panel = null;
		List<String> views = views();
		if (pendingView != null) {
			if (views.contains(pendingView)) {
				this.view = pendingView;
				this.loaded = false;
			}

			pendingView = null;
		}

		if (!views.contains(this.view)) {
			this.view = views.isEmpty() ? "active" : views.get(0);
			this.loaded = false;
		}

		ServerPage.Bar bar = new ServerPage.Bar(s, area);
		this.next = bar.right(new CFHGuiIconButton(0, 0, 18, Icon.CHEVRON_RIGHT, Txt.t("pager.next"), () -> this.turn(1)));
		this.prev = bar.right(new CFHGuiIconButton(0, 0, 18, Icon.CHEVRON_LEFT, Txt.t("pager.prev"), () -> this.turn(-1)));
		if (views.size() > 1) {
			int segW = 12;

			for (String v : views) {
				segW += CFHGuiDraw.width(Txt.t("mod.view." + v)) + 16;
			}

			bar.left(new CFHGuiSegmented<>(0, 0, Math.min(segW, bar.room() - 120), views, this.view, vx -> Txt.t("mod.view." + vx), vx -> {
				if (!vx.equals(this.view)) {
					this.view = vx;
					this.page = 0;
					this.loaded = false;
					this.screen.refreshPage();
				}
			}));
		}

		if (this.view.equals("audit")) {
			CFHGuiSearchField search = bar.fill(new CFHGuiSearchField(0, 0, 140, q -> {
				String typed = q.trim();
				if (!typed.equals(this.query)) {
					this.query = typed;
					this.debounce = 6;
				}
			}), 180);
			search.setValue(this.query);
			search.placeholder(Txt.t("mod.audit.search"));
		} else {
			bar.left(new CFHGuiDropdown<>(0, 0, 92, KINDS, this.kind, k -> Txt.t("mod.kind." + k), k -> {
				if (!k.equals(this.kind)) {
					this.kind = k;
					this.page = 0;
					this.ask();
				}
			}));
		}

		this.rangeAt = new Rect(bar.leftX(), area.y(), Math.max(0, this.prev.getX() - 6 - bar.leftX()), 20);
		Rect r = under(area);
		this.panel = (CFHGuiScrollPanel)s.addPageWidget(new CFHGuiScrollPanel(r.x(), r.y(), r.w(), r.h()));
		if (!this.loaded) {
			this.loading = true;
			this.punishments = List.of();
			this.audit = List.of();
		}

		this.fill();
		if (!this.loaded) {
			this.ask();
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

	private void turn(int by) {
		int to = Math.max(0, Math.min(Math.max(0, (this.total - 1) / 50), this.page + by));
		if (to != this.page) {
			this.page = to;
			this.ask();
		}
	}

	private void ask() {
		this.loading = true;
		int request = ++this.asked;
		String wanted = this.view;
		if (wanted.equals("audit")) {
			ServerActions.adminAudit(this.query, this.page, answer -> {
				if (request == this.asked) {
					AdminData.AuditPage p = answer.map(AdminData.AuditPage::sanitize).orElseGet(AdminData.AuditPage::new);
					this.audit = p.rows;
					this.done(p.total, p.page);
				}
			});
		} else {
			ServerActions.adminPunishments(wanted.equals("active") ? "active" : "all", Punishment.Kind.parse(this.kind), this.page, answer -> {
				if (request == this.asked) {
					AdminData.PunishmentPage p = answer.map(AdminData.PunishmentPage::sanitize).orElseGet(AdminData.PunishmentPage::new);
					this.punishments = p.rows;
					this.done(p.total, p.page);
				}
			});
		}
	}

	private void done(int total, int page) {
		this.total = total;
		this.page = page;
		this.loading = false;
		this.loaded = true;
		if (this.panel != null) {
			this.fill();
			this.panel.snapScroll(0.0);
		}
	}

	private void fill() {
		this.panel.clear();
		int w = this.panel.innerWidth();
		boolean log = this.view.equals("audit");
		if (log ? !this.audit.isEmpty() : !this.punishments.isEmpty()) {
			int y = 0;
			if (log) {
				for (AdminData.AuditEntry e : this.audit) {
					this.panel.add(this.entry(e, w), 0, y);
					y += 24;
				}
			} else {
				boolean mayLift = this.view.equals("active");

				for (AdminData.PunishmentRow r : this.punishments) {
					PaintedCard card = punishment(r.punishment, r.player, r.playerName, w, mayLift && mayLift(r.punishment.kind) ? () -> this.lift(r) : null);
					card.onClick(() -> this.admin.openPlayer(r.player));
					this.panel.add(card, 0, y);
					y += 30;
				}
			}

			this.panel.setContentHeight(y);
		} else {
			int h = Math.max(96, this.panel.getHeight() - 4);
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
										log ? Icon.HISTORY : Icon.SHIELD,
										Txt.t("mod.none." + this.view + ".title"),
										Txt.t("mod.none." + this.view),
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

	private static boolean mayLift(Punishment.Kind kind) {
		return switch (kind) {
			case MUTE -> ServerState.may(Nodes.MOD_MUTE);
			case BAN -> ServerState.may(Nodes.MOD_BAN);
			case FREEZE -> ServerState.may(Nodes.MOD_FREEZE);
			default -> false;
		};
	}

	private void lift(AdminData.PunishmentRow r) {
		String key = r.punishment.kind.key();
		Forms.confirm(
			Txt.t("punish." + key + ".lift.title", r.playerName),
			Txt.t("punish.lift.body"),
			Txt.t("punish." + key + ".lift"),
			() -> ServerActions.pardon(r.player, r.punishment.kind, AdminScreen.after(this::ask))
		);
	}

	static int kindColor(Punishment.Kind kind, Palette p) {
		return switch (kind) {
			case MUTE -> p.warning();
			case BAN -> p.danger();
			case FREEZE -> -10770208;
			case KICK -> -3115319;
			default -> p.accentInk();
		};
	}

	static PaintedCard punishment(Punishment pn, @Nullable UUID player, @Nullable String playerName, int w, @Nullable Runnable lift) {
		PaintedCard card = new PaintedCard(w, 26, (g, f) -> {
			Palette p = Theme.current().palette();
			float a = f.alpha();
			long now = System.currentTimeMillis();
			boolean active = pn.active(now);
			int cw = Ui.chip(g, Txt.t("punish." + pn.kind.key() + ".word"), f.x() + 6, f.y() + 7, kindColor(pn.kind, p), a * (!active && pn.kind.lasts() ? 0.6F : 1.0F));
			int tx = f.x() + Math.max(58, cw + 12);
			if (player != null && playerName != null) {
				Ui.head(g, player, playerName, tx, f.y() + 5, 16, null, a);
				Ui.textFit(g, Component.literal(playerName), tx + 20, f.y() + 4, 96, Ui.strong(), a);
				Ui.textFit(g, Txt.t("aplayers.role.by", pn.byName, Txt.ago(pn.at)), tx + 20, f.y() + 15, 110, Ui.caption(), a);
				tx += 124;
			}

			Component state;
			int stateColor;
			if (pn.liftedAt != 0L) {
				state = Txt.t("punish.state.lifted", pn.liftedByName.isEmpty() ? "?" : pn.liftedByName);
				stateColor = Ui.caption();
			} else if (!pn.kind.lasts()) {
				state = Txt.ago(pn.at);
				stateColor = Ui.caption();
			} else if (active) {
				state = pn.expiresAt == 0L ? Txt.t("punish.state.for_good") : Txt.t("punish.state.left", Txt.left(pn.expiresAt));
				stateColor = kindColor(pn.kind, p);
			} else {
				state = Txt.t("punish.state.over");
				stateColor = Ui.caption();
			}

			int right = f.x() + f.w() - 8 - (lift != null ? 50 : 0);
			Ui.textRight(g, state, right, f.y() + (player != null ? 9 : 4), stateColor, a);
			int room = right - CFHGuiDraw.width(state) - 8 - tx;
			Component reason = pn.reason.isEmpty() ? Txt.t("punish.no_reason") : Component.literal(pn.reason);
			if (player != null) {
				Ui.textFit(g, reason, tx, f.y() + 9, room, pn.reason.isEmpty() ? Ui.caption() : Ui.soft(), a);
			} else {
				Ui.textFit(g, reason, tx, f.y() + 4, room, pn.reason.isEmpty() ? Ui.caption() : Ui.strong(), a);
				Ui.textFit(g, Txt.t("aplayers.role.by", pn.byName, Txt.ago(pn.at)), tx, f.y() + 15, right - tx, Ui.caption(), a);
			}

			List<Component> tip = new ArrayList<>();
			tip.add(reason);
			tip.add(Component.literal(Txt.date(pn.at)).withStyle(ChatFormatting.GRAY));
			if (pn.kind.lasts() && pn.expiresAt != 0L) {
				tip.add(Txt.t("punish.tip.until", Txt.date(pn.expiresAt)).withStyle(ChatFormatting.GRAY));
			}

			f.tip(tx - f.x(), 0, Math.max(10, right - tx), f.h(), tip);
		}).flat();
		if (lift != null) {
			CFHGuiButton b = new CFHGuiButton(0, 0, 44, 16, Txt.t("punish.lift"), lift).variant(Variant.GHOST);
			card.add(b, w - 50, 5);
		}

		return card;
	}

	private PaintedCard entry(AdminData.AuditEntry e, int w) {
		String key = "cfhgui_server.audit." + e.action;
		Component line = I18n.exists(key)
			? Component.translatable(key, new Object[]{e.actorName, e.targetName, e.detail})
			: Component.literal(e.actorName + ": " + e.action + (e.targetName.isEmpty() ? "" : " " + e.targetName) + (e.detail.isEmpty() ? "" : " (" + e.detail + ")"));
		PaintedCard card = new PaintedCard(w, 22, (g, f) -> {
			Palette p = Theme.current().palette();
			float a = f.alpha();
			Icons.draw(g, auditIcon(e.action), f.x() + 7, f.y() + 7, 8, Colors.scaleAlpha(auditColor(e.action, p), a));
			Component when = Txt.ago(e.at);
			Ui.textRight(g, when, f.x() + f.w() - 8, f.y() + 7, Ui.caption(), a);
			Ui.textFit(g, line, f.x() + 22, f.y() + 7, f.w() - 38 - CFHGuiDraw.width(when), Ui.strong(), a);
			f.tip(0, 0, f.w(), f.h(), List.of(line, Component.literal(Txt.date(e.at)).withStyle(ChatFormatting.GRAY)));
		}).flat();
		if (e.target != null && ServerState.may(Nodes.ADMIN_PLAYERS)) {
			card.onClick(() -> this.admin.openPlayer(e.target));
		}

		return card;
	}

	private static Icon auditIcon(String action) {
		if (action.startsWith("punish")) {
			return Icon.BLOCKED;
		} else if (action.startsWith("pardon")) {
			return Icon.UNLOCK;
		} else if (action.startsWith("role")) {
			return Icon.CROWN;
		} else if (action.startsWith("kit")) {
			return Icon.PACK;
		} else if (action.startsWith("rule") || action.equals("maintenance")) {
			return Icon.SLIDERS;
		} else if (action.startsWith("world")) {
			return Icon.WORLD;
		} else if (action.startsWith("warp") || action.startsWith("spawn") || action.startsWith("home")) {
			return Icon.PIN;
		} else if (action.startsWith("note")) {
			return Icon.EDIT;
		} else {
			return action.equals("broadcast") ? Icon.SEND : Icon.USER;
		}
	}

	private static int auditColor(String action, Palette p) {
		if (action.startsWith("punish")) {
			return p.danger();
		} else if (action.startsWith("pardon")) {
			return p.success();
		} else {
			return !action.startsWith("role") && !action.startsWith("rule") && !action.equals("maintenance") ? p.textMuted() : p.warning();
		}
	}

	public void render(SidebarScreen s, GuiGraphics g, Rect area, int mouseX, int mouseY, float partialTick) {
		if (this.panel != null && this.prev != null && this.rangeAt.w() >= 60) {
			this.prev.active = this.page > 0;
			this.next.active = (this.page + 1) * 50 < this.total;
			Component range = this.total == 0
				? Txt.t("pager.none")
				: Txt.t("pager.range", this.page * 50 + 1, Math.min(this.total, (this.page + 1) * 50), Txt.number((long)this.total));
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
}
