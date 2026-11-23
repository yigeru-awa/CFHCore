package com.onehumanawa.cfhcore.content.gui.teams.client.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarPage;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popup;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsClient;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsState;
import com.onehumanawa.cfhcore.content.gui.teams.client.hud.TeamsHud;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.dialog.CFHGuiTeamDialogs;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.BannerArt;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamFlows;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsDraw;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.map.ClaimMap;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class TeamsHubScreen extends SidebarScreen {
	private final TeamsView.Pretend pretend;
	private final TeamFlows flows = new TeamFlows(new CFHGuiTeamDialogs());
	@Nullable
	private String pendingPage;
	@Nullable
	private Consumer<TeamFlows> pendingDialog;
	private final Runnable changed = () -> this.dirty = true;
	private final Consumer<String> claimsChanged = dimension -> this.dirty = true;
	private boolean listening;
	private boolean dirty;
	private boolean hadTeam;
	private boolean wasAvailable;

	public TeamsHubScreen(@Nullable Screen parent) {
		super(TeamsText.shared("title"), parent, "cfhgui_teams:hub");
		this.pretend = TeamsView.takePending();
		this.pendingPage = TeamsClient.takePendingPage();
		this.pendingDialog = TeamsUi.takePendingDialog();
		TeamsView.apply(this.pretend);
	}

	public TeamFlows flows() {
		return this.flows;
	}

	protected boolean showPageTitle() {
		return false;
	}

	public Component getTitle() {
		SidebarPage page = this.pages().isEmpty() ? null : this.currentPage();
		return (Component)(page == null ? super.getTitle() : Component.literal(super.getTitle().getString() + " – " + page.title().getString()));
	}

	protected void definePages(List<SidebarPage> pages) {
		pages.add(new OverviewPage(this));
		pages.add(new MembersPage(this));
		pages.add(new LandPage(this));
		pages.add(new AlliesPage(this));
		pages.add(new WaypointsPage(this));
		pages.add(new BrowsePage(this));
		pages.add(new SettingsPage(this));
	}

	@Nullable
	protected final SidebarPage customPage(String id) {
		return (SidebarPage)(switch (id) {
			case "overview" -> new OverviewPage(this);
			case "members" -> new MembersPage(this);
			case "land" -> new LandPage(this);
			case "allies" -> new AlliesPage(this);
			case "waypoints" -> new WaypointsPage(this);
			case "browse" -> new BrowsePage(this);
			case "settings" -> new SettingsPage(this);
			default -> null;
		});
	}

	protected static void follow(@Nullable SidebarPage page) {
		if (page instanceof TeamsPage custom) {
			custom.stateChanged();
		}
	}

	protected static void landRules(CFHGuiScrollPanel rules) {
		LandPage.refillRules(rules);
	}

	protected void build() {
		TeamsView.apply(this.pretend);
		this.hadTeam = TeamsView.hasTeam();
		this.wasAvailable = TeamsView.available();
		super.build();
		if (!this.listening) {
			this.listening = true;
			TeamsState.CHANGED.register(this.changed);
			TeamsState.CLAIMS_CHANGED.register(this.claimsChanged);
		}

		if (this.pendingPage != null) {
			String page = this.pendingPage;
			this.pendingPage = null;
			this.showPage(page);
		}
	}

	public void removed() {
		super.removed();
		if (this.listening) {
			this.listening = false;
			TeamsState.CHANGED.unregister(this.changed);
			TeamsState.CLAIMS_CHANGED.unregister(this.claimsChanged);
		}

		Popups.closeAll();
		TeamsView.apply(TeamsView.Pretend.NOTHING);
		if (System.currentTimeMillis() - ClaimMap.lastUsedMs() < 1500L) {
			TeamsHud.showClaimBorders();
		}
	}

	public void tick() {
		super.tick();
		if (this.pendingDialog != null) {
			Consumer<TeamFlows> open = this.pendingDialog;
			this.pendingDialog = null;
			open.accept(this.flows);
		}

		if (this.dirty) {
			this.dirty = false;
			boolean team = TeamsView.hasTeam();
			boolean available = TeamsView.available();
			if (team == this.hadTeam && available == this.wasAvailable) {
				if (this.currentPage() instanceof TeamsPage page) {
					page.stateChanged();
				} else if (this.currentPage() instanceof TeamsHubScreen.StatePage page) {
					page.stateChanged();
				}
			} else {
				this.hadTeam = team;
				this.wasAvailable = available;
				Popups.closeAll();
				this.refreshPage();
			}
		}
	}

	public boolean isPauseScreen() {
		return false;
	}

	public void go(String pageId) {
		this.showPage(pageId);
	}

	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		Popup top = Popups.top();
		boolean covered = top != null && top.isModal();
		super.render(g, covered ? -1000 : mouseX, covered ? -1000 : mouseY, partialTick);
	}

	protected int headerRight() {
		return this.width - 12;
	}

	protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		Theme t = Theme.current();
		Palette p = t.palette();
		TeamData team = TeamsView.team();
		int y = 11;
		int edge = this.headerRight();
		int right = edge;
		if (TeamsView.preview()) {
			right = TeamsDraw.chipRight(g, TeamsText.t("preview"), edge, 9, p.warning(), 1.0F) - 6;
			if (mouseX >= right && mouseX < edge && mouseY >= 9 && mouseY < 21) {
				CFHGuiTooltips.request(TeamsText.t("preview.desc"), null);
			}
		}

		int left = this.headerTitleX() + this.font.width(Fonts.heading(this.getTitle())) + 16;
		if (team == null) {
			if (TeamsView.available()) {
				Component none = TeamsView.personal() != null ? TeamsText.t("solo.header", TeamsText.chunks(TeamsView.held())) : TeamsText.shared("state.no_team");
				if (right - CFHGuiDraw.width(none) > left) {
					TeamsDraw.textRight(g, none, right, 11, p.textDim(), 1.0F);
				}
			}
		} else {
			Component online = TeamsText.t("header.online", TeamsView.onlineCount(team), team.members.size());
			int onlineW = CFHGuiDraw.width(online);
			Component name = Fonts.heading(team.name);
			int nameW = Math.min(CFHGuiDraw.width(name), Math.max(0, right - left - onlineW - 30));
			if (nameW >= 24) {
				int x = right - onlineW;
				TeamsDraw.text(g, online, x, 11, p.textMuted(), 1.0F);
				g.fill(x - 7, 13, x - 3, 17, p.success());
				x -= 14 + nameW;
				g.drawString(this.font, CFHGuiDraw.truncate(name, nameW), x, 11, TeamsDraw.ink(team.color), t.isVanilla());
				BannerArt.flag(g, team.banner, (float)(x - 12), 8.0F, 8.0F, 1.0F);
				CFHGuiDraw.outline(g, x - 13, 7, 10, 18, t.isVanilla() ? -16777216 : p.borderStrong(), 0);
			}
		}
	}

	public interface StatePage {
		void stateChanged();
	}
}
