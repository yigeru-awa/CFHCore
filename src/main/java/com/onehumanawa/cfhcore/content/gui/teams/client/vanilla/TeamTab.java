package com.onehumanawa.cfhcore.content.gui.teams.client.vanilla;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamActions;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsDraw;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import com.onehumanawa.cfhcore.content.gui.teams.model.InviteView;
import com.onehumanawa.cfhcore.content.gui.teams.model.Permission;
import com.onehumanawa.cfhcore.content.gui.teams.model.ServerInfo;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

final class TeamTab extends TeamsTab {
	private static final ResourceLocation XP_BACKGROUND = ResourceLocation.withDefaultNamespace("hud/experience_bar_background");
	private static final ResourceLocation XP_PROGRESS = ResourceLocation.withDefaultNamespace("hud/experience_bar_progress");
	private static final int CONTENT_MAX = 640;
	@Nullable
	private RowList list;
	@Nullable
	private Button accept;
	@Nullable
	private Button decline;
	private int panelW;
	private int left;

	TeamTab(VanillaTeamsScreen screen) {
		super(screen, "overview");
	}

	@Override
	protected boolean needsTeam() {
		return false;
	}

	@Override
	protected void build() {
		TeamData t = TeamsView.team();
		if (t == null) {
			this.buildNoTeam();
		} else {
			this.list = this.add(new RowList(this.mc, 16));
			this.fill(t);
			ServerInfo info = TeamsView.info();
			this.footButton(TeamsText.t("overview.chat"), () -> this.flows().teamChat());
			enable(this.footButton(TeamsText.t("overview.stash"), () -> this.flows().stash()), info.stash, TeamsText.t("overview.stash.off"));
			boolean mayHome = TeamsView.may(Permission.HOME);
			enable(
				this.footButton(TeamsText.t("overview.home.go"), () -> this.flows().goHome()),
				t.home != null && info.homeTeleport,
				TeamsText.t(t.home == null ? "overview.home.none" : "overview.home.off")
			);
			if (mayHome) {
				this.footButton(TeamsText.t("action.set_home"), () -> this.flows().setHome());
			}

			enable(
				this.footButton(TeamsText.t("overview.invite"), () -> this.flows().dialogs().invite()),
				TeamsView.may(Permission.INVITE),
				TeamsText.t("members.invite.rank", TeamsText.rank(Permission.INVITE.min()))
			);
		}
	}

	private void fill(TeamData t) {
		if (this.list != null) {
			RowList l = this.list;
			List<RowList.Row> rows = new ArrayList<>();
			if (TeamsView.may(Permission.INVITE) && !t.requests.isEmpty()) {
				rows.add(l.caption("requests", TeamsText.t("overview.requests", t.requests.size())));

				for (TeamData.Request r : t.requests) {
					rows.add(l.plain("request:" + r.player, Component.literal(r.playerName), (g, x, y, w, h, hovered, selected, mouseX, mouseY) -> {
						TeamsDraw.head(g, r.player, r.playerName, x + 3, y + (h - 10) / 2, 10, null, 1.0F);
						line(g, Component.literal(r.playerName).append(" ").append(TeamsText.t("overview.asked", TeamsText.ago(r.at))), x + 18, y + (h - 8) / 2, w - 22, true);
					}));
				}
			}

			rows.add(l.caption("activity", TeamsText.t("overview.activity")));

			for (int i = t.log.size() - 1; i >= 0; i--) {
				TeamData.LogEntry e = t.log.get(i);
				Component words = TeamsText.log(e);
				Component when = TeamsText.agoShort(e.at);
				rows.add(l.plain("log:" + i, words, (g, x, y, w, h, hovered, selected, mouseX, mouseY) -> {
					int whenW = CFHGuiDraw.width(when);
					line(g, words, x + 4, y + (h - 8) / 2, w - whenW - 14, true);
					TeamsDraw.textRight(g, when, x + w - 4, y + (h - 8) / 2, TeamsDraw.caption(), 1.0F);
				}));
			}

			l.rows(rows);
		}
	}

	private void buildNoTeam() {
		this.list = ((RowList)this.add(new RowList(this.mc, 26))).onSelect(this::update).emptyText(TeamsText.t("vanilla.no_invitations"));
		RowList l = this.list;
		List<RowList.Row> rows = new ArrayList<>();

		for (InviteView invite : TeamsView.invites()) {
			rows.add(
				l.row(
					"invite:" + invite.team,
					invite,
					Component.literal(invite.teamName),
					(g, x, y, w, h, hovered, selected, mouseX, mouseY) -> {
						TeamsDraw.bannerSmall(g, invite.banner, x + 5, y + (h - 20) / 2, 10, 1.0F);
						Component name = invite.tag.isEmpty() ? Component.literal(invite.teamName) : Component.literal(invite.teamName + " " + TeamsText.tag(invite.tag));
						Rows.twoLines(
							g,
							x,
							y,
							w,
							h,
							name,
							TeamsDraw.ink(invite.color),
							TeamsText.t("overview.invited_by", invite.byName, invite.members, TeamsText.until(invite.expiresAt)),
							null,
							0
						);
					}
				)
			);
		}

		l.rows(rows);
		ServerInfo info = TeamsView.info();
		enable(
			this.footButton(TeamsText.t("vanilla.found"), () -> this.flows().dialogs().foundTeam()),
			info.playersCreateTeams || info.admin,
			TeamsText.t("create.operators_only")
		);
		this.accept = this.footButton(TeamsText.t("action.accept"), () -> {
			InviteView v = l.chosen(InviteView.class);
			if (v != null) {
				TeamActions.acceptInvite(v.team, null);
			}
		});
		this.decline = this.footButton(TeamsText.t("action.decline"), () -> {
			InviteView v = l.chosen(InviteView.class);
			if (v != null) {
				TeamActions.declineInvite(v.team);
			}
		});
		this.footButton(TeamsText.t("state.browse"), () -> this.screen.go("browse"));
		if (TeamsView.personal() != null) {
			this.footButton(TeamsText.t("action.give_up"), () -> this.flows().giveUpLand()).setTooltip(Tooltip.create(TeamsText.t("solo.give_up.desc")));
		}

		this.update();
	}

	private void update() {
		boolean chosen = this.list != null && this.list.chosen(InviteView.class) != null;
		if (this.accept != null) {
			this.accept.active = chosen;
		}

		if (this.decline != null) {
			this.decline.active = chosen;
		}
	}

	@Override
	void refresh() {
		TeamData t = TeamsView.team();
		if (t != null && this.list != null && this.accept == null) {
			this.fill(t);
		} else {
			super.refresh();
		}
	}

	@Override
	protected void layout(ScreenRectangle area) {
		if (this.list != null) {
			if (!TeamsView.hasTeam()) {
				int w = Math.min(320, area.width() - 40);
				int top = TeamsView.personal() != null ? 40 : 26;
				this.list.place(area.left() + (area.width() - w) / 2, area.top() + top, w, area.height() - top);
			} else {
				int content = Math.min(area.width(), 640);
				this.left = area.left() + (area.width() - content) / 2;
				this.panelW = Math.max(150, Math.min(200, content * 2 / 5));
				int x = this.left + this.panelW + 12;
				this.list.place(x, area.top(), this.left + content - x, area.height());
			}
		}
	}

	@Override
	void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		if (this.built()) {
			Theme th = Theme.current();
			Palette p = th.palette();
			TeamData t = TeamsView.team();
			if (t == null) {
				int y = this.area.top() + 10;
				if (TeamsView.personal() != null) {
					Component own = TeamsText.t("solo.vanilla", TeamsView.held(), TeamsView.info().maxClaims);
					int w = Math.min(this.area.width() - 40, CFHGuiDraw.width(own));
					TeamsDraw.textFit(g, own, this.area.left() + (this.area.width() - w) / 2, y, w, p.textMuted(), 1.0F);
					y += 14;
				}

				TeamsDraw.textCentered(g, TeamsText.t("vanilla.invitations"), this.area.left() + this.area.width() / 2, y, p.text(), 1.0F);
				if (this.list != null) {
					Ground.rule(g, this.list.getX(), this.list.getY() - 2, this.list.getWidth(), true);
				}
			} else {
				ServerInfo info = TeamsView.info();
				int x = this.left + 12;
				int w = this.panelW - 8;
				Ground.divider(g, this.left + this.panelW + 7, this.area.top() + 8, this.area.height() - 16);
				int yx = this.area.top() + 10;
				TeamsDraw.banner(g, t.banner, x, yx, 20, 1.0F, 1.2F, 1.0F);
				int tx = x + 30;
				int tw = w - 30;
				Component name = t.tag.isEmpty() ? Component.literal(t.name) : Component.literal(t.name + " " + TeamsText.tag(t.tag));
				TeamsDraw.textFit(g, name, tx, yx, tw, TeamsDraw.ink(t.color), 1.0F);
				List<FormattedCharSequence> motto = CFHGuiDraw.font().split(t.motto.isEmpty() ? TeamsText.t("overview.no_motto") : Component.literal(t.motto), tw);

				for (int i = 0; i < Math.min(2, motto.size()); i++) {
					TeamsDraw.text(g, motto.get(i), tx, yx + 12 + i * 10, p.textMuted(), 1.0F);
				}

				line(g, TeamsText.privacy(t.privacy), tx, yx + 34, tw, false);
				yx += 50;
				line(g, TeamsText.t("overview.level", t.level), x, yx, w, true);
				Component xp = TeamsText.t("overview.xp", TeamsText.number(t.xp), TeamsText.number(TeamData.xpForLevel(t.level)));
				TeamsDraw.textRight(g, xp, x + w, yx, p.textMuted(), 1.0F);
				yx += 11;
				if (th.isVanilla()) {
					RenderSystem.enableBlend();
					g.blitSprite(XP_BACKGROUND, x, yx, w, 5);
					int fill = Math.round((float)w * t.levelProgress());
					if (fill > 0) {
						g.enableScissor(x, yx, x + fill, yx + 5);
						g.blitSprite(XP_PROGRESS, x, yx, w, 5);
						g.disableScissor();
					}

					RenderSystem.disableBlend();
				} else {
					TeamsDraw.bar(g, x, yx, w, 4, t.levelProgress(), TeamsDraw.ink(t.color), 1.0F);
				}

				yx += 12;
				figure(g, Icon.FRIENDS, TeamsText.t("vanilla.figure.members", TeamsView.onlineCount(t), t.members.size()), x, yx, w);
				figure(g, Icon.MAP, TeamsText.t("vanilla.figure.land", TeamsView.held(), info.maxClaims), x, yx + 12, w);
				figure(g, Icon.QUEST, TeamsText.t("vanilla.figure.quests", TeamsText.number(t.counter("quests"))), x, yx + 24, w);
				figure(g, Icon.SHIELD, TeamsText.t("vanilla.figure.allies", t.allies.size()), x, yx + 36, w);
				TeamData.Member owner = t.owner();
				if (owner != null && yx + 60 < this.area.bottom()) {
					Component founded = TeamsText.t("overview.founded.owner", TeamsText.ago(t.createdAt), owner.name);
					if (!TeamsDraw.cut(founded, w)) {
						line(g, founded, x, yx + 52, w, false);
					} else {
						line(g, TeamsText.t("overview.founded", TeamsText.ago(t.createdAt)), x, yx + 52, w, false);
						if (yx + 71 < this.area.bottom()) {
							line(g, TeamsText.t("overview.owner", owner.name), x, yx + 62, w, false);
						}
					}
				}
			}
		}
	}

	private static void figure(GuiGraphics g, Icon icon, Component text, int x, int y, int w) {
		Icons.draw(g, icon, x, y, 8, Theme.current().palette().textMuted());
		line(g, text, x + 12, y, w - 12, true);
	}
}
