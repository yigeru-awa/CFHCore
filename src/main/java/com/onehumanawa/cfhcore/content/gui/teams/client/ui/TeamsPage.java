package com.onehumanawa.cfhcore.content.gui.teams.client.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarPage;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSeparator;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.PaintedCard;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamFlows;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsDraw;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

abstract class TeamsPage extends SidebarPage {
	protected final TeamsHubScreen screen;

	TeamsPage(TeamsHubScreen screen, String id, Icon icon) {
		super(id, TeamsText.page(id), icon);
		this.screen = screen;
	}

	protected TeamFlows flows() {
		return this.screen.flows();
	}

	public final void build(SidebarScreen s, Rect area) {
		if (!TeamsView.available() && this.needsServer()) {
			this.noServer(s, area);
		} else {
			this.buildPage(s, area);
		}
	}

	protected boolean needsServer() {
		return true;
	}

	protected abstract void buildPage(SidebarScreen var1, Rect var2);

	void stateChanged() {
		this.screen.refreshPage();
	}

	private void noServer(SidebarScreen s, Rect area) {
		boolean inWorld = Minecraft.getInstance().level != null;
		Component title = TeamsText.t(inWorld ? "state.no_server.title" : "state.no_world.title");
		Component body = TeamsText.shared(inWorld ? "state.no_server" : "state.no_world");
		s.addPageWidget(this.empty(area, Icon.TEAM, title, body));
	}

	protected PaintedCard empty(Rect area, Icon icon, Component title, @Nullable Component body) {
		PaintedCard card = new PaintedCard(area.w(), area.h(), (g, f) -> TeamsDraw.empty(g, new Rect(f.x(), f.y(), f.w(), f.h()), icon, title, body, f.alpha()))
			.bare();
		card.setX(area.x());
		card.setY(area.y());
		card.narrate(title);
		return card;
	}

	protected void noTeam(SidebarScreen s, Rect area, Icon icon, Component body) {
		boolean own = TeamsView.personal() != null;
		Component title = own ? TeamsText.t("solo.share") : TeamsText.shared("state.no_team");
		Component words = (Component)(own ? TeamsText.t("solo.share.desc") : body);
		PaintedCard card = this.empty(area, icon, title, words);
		int by = TeamsDraw.emptyButtonsY(area.h(), words, area.w());
		CFHGuiButton found = new CFHGuiButton(0, 0, 96, TeamsText.t(own ? "solo.create" : "state.found"), () -> this.screen.go("overview"))
			.variant(Variant.PRIMARY)
			.icon(Icon.PLUS);
		CFHGuiButton browse = own
			? new CFHGuiButton(0, 0, 96, TeamsText.t("solo.land"), () -> this.screen.go("land")).icon(Icon.MAP)
			: new CFHGuiButton(0, 0, 96, TeamsText.t("state.browse"), () -> this.screen.go("browse")).icon(Icon.SEARCH);
		found.setWidth(Math.max(96, found.preferredWidth() + 10));
		browse.setWidth(Math.max(96, browse.preferredWidth() + 10));
		int total = found.getWidth() + 6 + browse.getWidth();
		card.add(found, (area.w() - total) / 2, by);
		card.add(browse, (area.w() - total) / 2 + found.getWidth() + 6, by);
		s.addPageWidget(card);
	}

	protected static int section(CFHGuiScrollPanel panel, int y, int w, Component caption) {
		panel.add(new CFHGuiSeparator(0, 0, w, caption), 0, y);
		return y + 14;
	}

	protected static void refill(CFHGuiScrollPanel panel, Runnable fill) {
		double at = panel.scrollAmount();
		panel.clear();
		fill.run();
		panel.snapScroll(at);
	}
}
