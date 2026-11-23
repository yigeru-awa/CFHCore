package com.onehumanawa.cfhcore.content.gui.teams.client.overhaul;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiContextMenu;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSeparator;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup.Item;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamActions;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.ActivityFeed;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.PaintedCard;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsDraw;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import com.onehumanawa.cfhcore.content.gui.teams.model.Permission;
import com.onehumanawa.cfhcore.content.gui.teams.model.ServerInfo;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

final class TeamCampPage extends CampPage {
	@Nullable
	private CFHGuiScrollPanel panel;
	private final Anim title = new Anim(0.0F, 620, Ease.OUT_CUBIC);

	TeamCampPage(OverhaulTeamsScreen screen) {
		super(screen, "overview", Icon.TEAM);
	}

	public int badge() {
		TeamData t = TeamsView.team();
		if (t == null) {
			return TeamsView.invites().size();
		} else {
			return TeamsView.may(Permission.INVITE) ? t.requests.size() : 0;
		}
	}

	@Override
	protected CampScene.View framing() {
		return CampScene.View.CAMP;
	}

	@Override
	protected void picked(CampScene.Person who) {
		if (who.kind() == CampScene.Kind.ALONE) {
			this.flows().dialogs().foundTeam();
		} else {
			this.screen.roster().choose(who.id());
			this.screen.go("members");
		}
	}

	@Override
	public void build(SidebarScreen s, Rect area) {
		this.panel = null;
		this.screen.headerInset(0);
		super.build(s, area);
		this.title.set(1.0F);
	}

	@Override
	protected void panel(SidebarScreen s, Rect area, TeamData team) {
		this.panel = (CFHGuiScrollPanel)s.addPageWidget(new CFHGuiScrollPanel(area.x(), area.y(), area.w(), area.h()));
		this.fill(team);
	}

	@Override
	public void stateChanged() {
		TeamData t = TeamsView.team();
		if (this.panel != null && t != null) {
			this.populate();
			double at = this.panel.scrollAmount();
			this.panel.clear();
			this.fill(t);
			this.panel.snapScroll(at);
		} else {
			super.stateChanged();
		}
	}

	private void fill(TeamData t) {
		CFHGuiScrollPanel panel = this.panel;
		if (panel != null) {
			int w = panel.innerWidth();
			ServerInfo info = TeamsView.info();
			int y = 0;
			panel.add(caption(t, w), 0, y + 1);
			y += 15;
			int gap = 4;
			int bw = (w - 12) / 4;
			panel.add(
				new CFHGuiButton(0, 0, bw, TeamsText.t("overview.chat"), () -> this.flows().teamChat()).icon(Icon.CHAT).tip(TeamsText.t("overview.chat.desc")), 0, y
			);
			CFHGuiButton stash = new CFHGuiButton(0, 0, bw, TeamsText.t("overview.stash"), () -> this.flows().stash()).icon(Icon.BACKUP);
			stash.tip(TeamsText.t(info.stash ? "overview.stash.desc" : "overview.stash.off")).enabled(info.stash);
			panel.add(stash, bw + 4, y);
			CFHGuiButton home = new CFHGuiButton(0, 0, bw, TeamsText.t("overview.home"), null).icon(Icon.HOME);
			home.onPress(() -> this.home(t, home));
			home.tip(TeamsText.t(t.home == null ? "overview.home.none" : "overview.home.desc"));
			panel.add(home, (bw + 4) * 2, y);
			boolean mayInvite = TeamsView.may(Permission.INVITE);
			CFHGuiButton invite = new CFHGuiButton(0, 0, w - (bw + 4) * 3, TeamsText.t("overview.invite"), () -> this.flows().dialogs().invite())
				.icon(Icon.INVITE)
				.variant(Variant.PRIMARY);
			invite.tip(TeamsText.t(mayInvite ? "overview.invite.desc" : "members.invite.rank", TeamsText.rank(Permission.INVITE.min()))).enabled(mayInvite);
			panel.add(invite, (bw + 4) * 3, y);
			y += 24;
			int across = (w - 12) / 4 >= 62 ? 4 : 2;
			int tw = (w - 4 * (across - 1)) / across;
			List<PaintedCard> figures = List.of(
				figure(
					tw,
					Icon.FRIENDS,
					TeamsView.onlineCount(t),
					" / " + t.members.size(),
					TeamsText.t("overview.online"),
					TeamsText.t("overview.online.desc"),
					() -> this.screen.go("members")
				),
				figure(
					tw, Icon.MAP, TeamsView.held(), " / " + info.maxClaims, TeamsText.t("overview.land"), TeamsText.t("overview.land.desc"), () -> this.screen.go("land")
				),
				figure(tw, Icon.QUEST, (int)Math.min(2147483647L, t.counter("quests")), "", TeamsText.t("overview.quests"), TeamsText.t("overview.quests.desc"), null),
				figure(tw, Icon.SHIELD, t.allies.size(), "", TeamsText.t("overview.allies"), TeamsText.t("overview.allies.desc"), () -> this.screen.go("allies"))
			);

			for (int i = 0; i < figures.size(); i++) {
				PaintedCard f = figures.get(i);
				if (i % across == across - 1) {
					f.setWidth(w - (tw + 4) * (across - 1));
				}

				panel.add(f, i % across * (tw + 4), y + i / across * 32);
			}

			y += 32 * (4 / across);
			if (mayInvite && !t.requests.isEmpty()) {
				panel.add(new CFHGuiSeparator(0, 0, w, TeamsText.t("overview.requests", t.requests.size())), 0, y);
				y += 14;

				for (TeamData.Request r : t.requests) {
					panel.add(request(r, w), 0, y);
					y += 28;
				}

				y += 2;
			}

			panel.add(new CFHGuiSeparator(0, 0, w, TeamsText.t("overview.activity")), 0, y);
			y += 14;
			ActivityFeed feed = new ActivityFeed(0, 0, w, t.log);
			panel.add(feed, 0, y);
			panel.setContentHeight(y + feed.getHeight() + 4);
		}
	}

	private static PaintedCard caption(TeamData t, int w) {
		TeamData.Member owner = t.owner();
		Component founded = owner == null
			? TeamsText.t("overview.founded", TeamsText.ago(t.createdAt))
			: TeamsText.t("overview.founded.owner", TeamsText.ago(t.createdAt), owner.name);
		return new PaintedCard(w, 12, (g, f) -> {
			Palette p = Theme.current().palette();
			Component privacy = TeamsText.privacy(t.privacy);
			int chip = TeamsDraw.chip(g, privacy, f.x(), f.y(), p.textMuted(), f.alpha());
			f.tip(0, 0, chip, 12, TeamsText.privacyHint(t.privacy));
			TeamsDraw.textFit(g, founded, f.x() + chip + 6, f.y() + 2, f.w() - chip - 6, TeamsDraw.caption(), f.alpha());
		}).bare().narrate(founded);
	}

	private static PaintedCard figure(int w, Icon icon, int value, String suffix, Component label, Component tip, @Nullable Runnable onClick) {
		PaintedCard card = new PaintedCard(
				w,
				28,
				(g, f) -> {
					Theme th = Theme.current();
					Palette p = th.palette();
					float a = f.alpha();
					Component number = Fonts.heading(Math.round((float)value * f.grow()) + suffix);
					g.drawString(
						CFHGuiDraw.font(), CFHGuiDraw.truncate(number, f.w() - 18), f.x() + 5, f.y() + 4, Colors.scaleAlpha(th.isVanilla() ? -1 : p.text(), a), th.isVanilla()
					);
					TeamsDraw.textFit(g, label, f.x() + 5, f.y() + 16, f.w() - 8, p.textDim(), a);
					Icons.draw(g, icon, f.x() + f.w() - 13, f.y() + 4, 8, Colors.scaleAlpha(Colors.lerp(p.textDim(), th.accentInk(), f.hover()), a));
					f.tip(0, 0, f.w(), f.h(), tip);
				}
			)
			.narrate(label);
		if (onClick != null) {
			card.onClick(onClick);
		} else {
			card.flat();
		}

		return card;
	}

	private void home(TeamData t, CFHGuiButton anchor) {
		ServerInfo info = TeamsView.info();
		if (!TeamsView.may(Permission.HOME)) {
			if (t.home == null) {
				TeamActions.goHome();
			} else {
				this.flows().goHome();
			}
		} else {
			List<Item> items = new ArrayList<>();
			if (t.home != null && info.homeTeleport) {
				items.add(Item.of(TeamsText.t("overview.home.go"), Icon.HOME, () -> this.flows().goHome()));
			} else {
				items.add(Item.disabled(TeamsText.t(t.home == null ? "overview.home.none" : "overview.home.off"), Icon.HOME));
			}

			items.add(Item.sep());
			items.add(Item.of(TeamsText.t("action.set_home"), Icon.PIN, () -> this.flows().setHome()));
			if (t.home != null) {
				items.add(Item.danger(TeamsText.t("action.clear_home"), Icon.TRASH, () -> this.flows().clearHome()));
			}

			CFHGuiContextMenu.open((double)anchor.getX(), (double)(anchor.getY() + anchor.getHeight() + 1), items);
		}
	}

	private static PaintedCard request(TeamData.Request r, int w) {
		PaintedCard card = new PaintedCard(w, 24, (g, f) -> {
			Palette p = Theme.current().palette();
			TeamsDraw.head(g, r.player, r.playerName, f.x() + 5, f.y() + 4, 16, null, f.alpha());
			int tx = f.x() + 27;
			int room = f.w() - 27 - 118;
			int used = TeamsDraw.textFit(g, Component.literal(r.playerName), tx, f.y() + 8, room, Theme.current().isVanilla() ? -1 : p.text(), f.alpha());
			TeamsDraw.textFit(g, TeamsText.t("overview.asked", TeamsText.ago(r.at)), tx + used + 6, f.y() + 8, room - used - 6, p.textDim(), f.alpha());
		}).flat();
		card.add(
			new CFHGuiButton(0, 0, 66, 16, TeamsText.t("action.accept"), () -> TeamActions.acceptRequest(r.player)).variant(Variant.PRIMARY).icon(Icon.CHECK).iconSize(8),
			w - 112,
			4
		);
		card.add(new CFHGuiButton(0, 0, 40, 16, TeamsText.t("action.deny"), () -> TeamActions.denyRequest(r.player)).variant(Variant.GHOST), w - 44, 4);
		return card;
	}

	@Override
	protected void overlay(GuiGraphics g, int mouseX, int mouseY) {
		super.overlay(g, mouseX, mouseY);
		TeamData t = TeamsView.team();
		if (t != null) {
			Theme th = Theme.current();
			Palette p = th.palette();
			boolean van = th.isVanilla();
			Font font = CFHGuiDraw.font();
			float a = this.title.get();
			if (!(a <= 0.01F)) {
				int slide = Math.round((1.0F - a) * 5.0F);
				int ink = TeamsDraw.ink(t.color);
				int pad = 8;
				int left = this.view.x() + 8;
				int right = this.view.right() - 8;
				boolean large = this.view.h() >= 150;
				float scale = large ? 2.0F : 1.0F;
				SceneFrame.footShade(g, this.view, large ? 70 : 46, a);
				int barY = this.view.bottom() - 8;
				TeamsDraw.bar(g, left, barY, right - left, 3, t.levelProgress() * a, ink, a);
				Component level = TeamsText.t("overview.level", t.level);
				long need = TeamData.xpForLevel(t.level);
				Component xp = TeamsText.t("overview.xp", TeamsText.number((long)Math.round((float)t.xp * a)), TeamsText.number(need));
				int rowY = barY - 12 + slide;
				g.drawString(font, level, left, rowY, Colors.scaleAlpha(van ? -1 : p.text(), a), true);
				int xpW = font.width(xp);
				g.drawString(font, xp, right - xpW, rowY, Colors.scaleAlpha(van ? -3092272 : p.textMuted(), a), true);
				CampBuild.Tier tier = CampBuild.Tier.of(t.level);
				Component stage = OverhaulTeams.t("camp." + tier.name().toLowerCase(Locale.ROOT));
				int levelW = font.width(level);
				int stageRoom = right - xpW - 10 - (left + levelW + 10);
				if (stageRoom > 40) {
					FormattedCharSequence seq = CFHGuiDraw.truncate(stage, stageRoom);
					g.drawString(font, seq, left + levelW + 10, rowY, Colors.scaleAlpha(van ? -5197648 : p.textDim(), a), true);
				}

				if (mouseX >= left && mouseX < right && mouseY >= rowY - 2 && mouseY < this.view.bottom() - 2) {
					List<Component> tip = new ArrayList<>();
					tip.add(TeamsText.t("overview.xp.desc", t.level + 1, TeamsText.number(Math.max(0L, need - t.xp))));
					CampBuild.Tier next = tier.next();
					tip.add(
						(next == null
								? OverhaulTeams.t("camp.grown")
								: OverhaulTeams.t("camp.next", next.level, OverhaulTeams.t("camp." + next.name().toLowerCase(Locale.ROOT) + ".gains")))
							.withStyle(ChatFormatting.GRAY)
					);
					CFHGuiTooltips.request(tip, null);
				}

				int mottoY = rowY - 13;
				Component motto = t.motto.isEmpty() ? null : Component.literal(t.motto);
				if (motto != null) {
					g.drawString(font, CFHGuiDraw.truncate(motto, right - left), left, mottoY, Colors.scaleAlpha(van ? -2039584 : p.textMuted(), a), true);
				}

				int nameY = (motto != null ? mottoY : rowY) - Math.round(9.0F * scale) - 4;
				Component name = Fonts.heading(t.name);
				int tagW = t.tag.isEmpty() ? 0 : TeamsDraw.chipWidth(Component.literal(t.tag)) + 6;
				int nameRoom = Math.round((float)(right - left - tagW) / scale);
				FormattedCharSequence cut = CFHGuiDraw.truncate(name, nameRoom);
				g.pose().pushPose();
				g.pose().translate((float)left, (float)nameY, 0.0F);
				g.pose().scale(scale, scale, 1.0F);
				g.drawString(font, cut, 0, 0, Colors.scaleAlpha(ink, a), true);
				g.pose().popPose();
				if (!t.tag.isEmpty()) {
					TeamsDraw.chip(g, Component.literal(t.tag), left + Math.round((float)font.width(cut) * scale) + 6, nameY + Math.round(7.0F * scale) - 9, ink, a);
				}
			}
		}
	}
}
