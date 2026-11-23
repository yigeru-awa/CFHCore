package com.onehumanawa.cfhcore.content.gui.teams.client.overhaul;

import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.screen.MenuBackdrop;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarPage;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.TeamsHubScreen;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public final class OverhaulTeamsScreen extends TeamsHubScreen {
	@Nullable
	private CampScene camp;
	@Nullable
	private LandTable table;
	private final CampRoster roster = new CampRoster();
	private int headerInset;

	public OverhaulTeamsScreen(@Nullable Screen parent) {
		super(parent);
	}

	protected boolean overhaulNav() {
		return true;
	}

	@Override
	protected void definePages(List<SidebarPage> pages) {
		pages.add(new TeamCampPage(this));
		pages.add(new MembersCampPage(this));
		pages.add(new LandTablePage(this));

		for (String id : List.of("allies", "waypoints", "browse", "settings")) {
			pages.add(this.customPage(id));
		}
	}

	CampScene camp() {
		if (this.camp == null || this.camp.stage.isClosed()) {
			this.camp = new CampScene(this);
		}

		return this.camp;
	}

	CampRoster roster() {
		return this.roster;
	}

	LandTable table() {
		if (this.table == null || this.table.stage.isClosed()) {
			this.table = new LandTable(this);
		}

		return this.table;
	}

	static void rules(CFHGuiScrollPanel panel) {
		landRules(panel);
	}

	@Nullable
	SidebarPage custom(String id) {
		return this.customPage(id);
	}

	static void followCustom(@Nullable SidebarPage page) {
		follow(page);
	}

	void headerInset(int px) {
		this.headerInset = Math.max(0, px);
	}

	@Override
	protected int headerRight() {
		return super.headerRight() - this.headerInset;
	}

	public void showPage(int index) {
		if (index != this.currentIndex()) {
			this.headerInset = 0;
		}

		super.showPage(index);
	}

	@Override
	public void removed() {
		super.removed();
		if (this.camp != null) {
			this.camp.close();
			this.camp = null;
		}

		if (this.table != null) {
			this.table.close();
			this.table = null;
		}
	}

	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(g, mouseX, mouseY, partialTick);
		if (!Theme.current().isVanilla() && !MenuBackdrop.worldBehind()) {
			CFHGuiDraw.vgradient(g, 0, 32, this.width, this.height - 32, -16053233, -15067114);
			CFHGuiDraw.vignette(g, 0, 32, this.width, this.height - 32, 0.4F);
		}
	}
}
