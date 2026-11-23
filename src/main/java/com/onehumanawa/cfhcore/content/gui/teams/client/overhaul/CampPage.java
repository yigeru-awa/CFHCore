package com.onehumanawa.cfhcore.content.gui.teams.client.overhaul;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarPage;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.TeamsHubScreen;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamFlows;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

abstract class CampPage extends SidebarPage implements TeamsHubScreen.StatePage {
	protected final OverhaulTeamsScreen screen;
	protected Rect view = new Rect(0, 0, 0, 0);
	protected Rect below = new Rect(0, 0, 0, 0);
	@Nullable
	protected SidebarPage hosted;
	@Nullable
	protected SidebarScreen host;

	CampPage(OverhaulTeamsScreen screen, String id, Icon icon) {
		super(id, TeamsText.page(id), icon);
		this.screen = screen;
	}

	protected TeamFlows flows() {
		return this.screen.flows();
	}

	protected CampScene scene() {
		return this.screen.camp();
	}

	protected abstract CampScene.View framing();

	protected abstract void panel(SidebarScreen var1, Rect var2, TeamData var3);

	protected void overlay(GuiGraphics g, int mouseX, int mouseY) {
		this.scene().drawTags(g, this.view);
	}

	protected void wheel(int direction) {
	}

	protected void picked(CampScene.Person who) {
	}

	protected float sceneShare() {
		return 0.58F;
	}

	public void build(SidebarScreen s, Rect area) {
		this.host = s;
		TeamData team = TeamsView.team();
		float share = team != null ? this.sceneShare() : 0.42F;
		int viewH = Mth.clamp(Math.round((float)area.h() * share), 56, Math.max(56, Math.round((float)area.w() / 2.1F)));
		this.view = new Rect(area.x(), area.y(), area.w(), viewH);
		this.below = new Rect(area.x(), this.view.bottom() + 6, area.w(), Math.max(20, area.bottom() - this.view.bottom() - 6));
		CampScene scene = this.scene();
		s.addPageWidget(new SceneFrame.View(this.view, scene.stage, this.title(), this::wheel));
		s.addPageWidget(new SceneFrame.Overlay(this.view, this::overlay));
		this.populate();
		this.hosted = null;
		if (team == null) {
			this.hosted = this.screen.custom(this.id());
			if (this.hosted != null) {
				this.hosted.build(s, this.below);
			}
		} else {
			this.panel(s, this.below, team);
		}
	}

	protected void populate() {
		CampScene scene = this.scene();
		TeamData team = TeamsView.team();
		boolean camp = team != null;
		scene.show(camp, CampBuild.Tier.of(camp ? team.level : 1), camp ? team.color : 16777215, camp ? team.banner : null);
		CampRoster roster = this.screen.roster();
		roster.refresh();
		List<CampScene.Person> yard = roster.yard(CampScene.fitting(this.view));
		scene.shape(this.view);
		scene.people(yard);
		scene.view(this.framing());
		scene.select(this.framing() == CampScene.View.YARD ? roster.chosen() : null);
		scene.onPick(this::picked);
	}

	protected void refresh() {
		if (this.host != null && this.host.currentPage() == this) {
			this.host.refreshPage();
		}
	}

	@Override
	public void stateChanged() {
		if (this.hosted != null) {
			this.populate();
			OverhaulTeamsScreen.followCustom(this.hosted);
		} else {
			this.refresh();
		}
	}

	public void render(SidebarScreen s, GuiGraphics g, Rect area, int mouseX, int mouseY, float partialTick) {
		SceneFrame.plate(g, this.view);
		if (this.hosted != null) {
			this.hosted.render(s, g, this.below, mouseX, mouseY, partialTick);
		}
	}

	public void tick() {
		if (this.hosted != null) {
			this.hosted.tick();
		}
	}

	public void onHide() {
		if (this.hosted != null) {
			this.hosted.onHide();
		}
	}
}
