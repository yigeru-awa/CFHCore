package com.onehumanawa.cfhcore.content.gui.teams.client.vanilla;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsClient;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsState;
import com.onehumanawa.cfhcore.content.gui.teams.client.hud.TeamsHud;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.TeamsUi;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamFlows;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.map.ClaimMap;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class VanillaTeamsScreen extends Screen {
	static final int FOOTER_H = 34;
	private static final int TALL_FOOTER_H = 58;
	private static final int SEAM = 2;
	private static int lastTab;
	private int footH = 34;
	@Nullable
	private final Screen parent;
	private final TeamsView.Pretend pretend;
	private final TeamFlows flows;
	@Nullable
	private Consumer<TeamFlows> pendingDialog;
	private int startTab;
	private final List<TeamsTab> tabs = new ArrayList<>();
	@Nullable
	private TabManager tabManager;
	@Nullable
	private TabNavigationBar bar;
	@Nullable
	private Button done;
	private final Runnable changed = () -> this.dirty = true;
	private final Consumer<String> claimsChanged = dimension -> this.dirty = true;
	private boolean listening;
	private boolean dirty;
	private boolean hadTeam;
	private boolean wasAvailable;

	public VanillaTeamsScreen(@Nullable Screen parent) {
		super(TeamsText.shared("title"));
		this.parent = parent;
		this.pretend = TeamsView.takePending();
		this.pendingDialog = TeamsUi.takePendingDialog();
		String page = TeamsClient.takePendingPage();
		this.startTab = page == null ? lastTab : Math.max(0, TeamsClient.PAGES.indexOf(page));
		this.flows = new TeamFlows(new VanillaTeamDialogs(this));
		TeamsView.apply(this.pretend);
	}

	TeamFlows flows() {
		return this.flows;
	}

	protected void init() {
		TeamsView.apply(this.pretend);
		this.hadTeam = TeamsView.hasTeam();
		this.wasAvailable = TeamsView.available();
		this.tabs.clear();
		this.tabs.add(new TeamTab(this));
		this.tabs.add(new MembersTab(this));
		this.tabs.add(new LandTab(this));
		this.tabs.add(new AlliesTab(this));
		this.tabs.add(new WaypointsTab(this));
		this.tabs.add(new BrowseTab(this));
		this.tabs.add(new SettingsTab(this));

		for (TeamsTab tab : this.tabs) {
			tab.rebuild();
		}

		this.footH = 34;

		for (TeamsTab tab : this.tabs) {
			if (footWidth(tab.foot) + 72 + 4 > this.width - 12) {
				this.footH = 58;
			}
		}

		this.tabManager = new TabManager(x$0 -> {
			AbstractWidget var10000 = (AbstractWidget)this.addRenderableWidget(x$0);
		}, x$0 -> this.removeWidget(x$0));
		this.bar = TabNavigationBar.builder(this.tabManager, this.width).addTabs(this.tabs.toArray(new Tab[0])).build();
		this.addRenderableWidget(this.bar);
		this.done = (Button)this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose()).size(72, 20).build());
		this.bar.selectTab(Mth.clamp(this.startTab, 0, this.tabs.size() - 1), false);
		if (!this.listening) {
			this.listening = true;
			TeamsState.CHANGED.register(this.changed);
			TeamsState.CLAIMS_CHANGED.register(this.claimsChanged);
		}

		this.arrange();
	}

	protected void repositionElements() {
		if (!this.listening) {
			this.rebuildWidgets();
		} else {
			this.arrange();
		}
	}

	private void arrange() {
		if (this.bar != null && this.tabManager != null) {
			this.bar.setWidth(this.width);
			this.bar.arrangeElements();
			this.widenTabs(this.bar);
			int top = this.bar.getRectangle().bottom();
			this.tabManager.setTabArea(new ScreenRectangle(0, top, this.width, this.groundBottom() - top));
		}
	}

	private int groundBottom() {
		return this.height - this.footH - 2;
	}

	private static int footWidth(List<AbstractWidget> row) {
		int total = -4;

		for (AbstractWidget w : row) {
			total += w.getWidth() + 4;
		}

		return Math.max(0, total);
	}

	private void widenTabs(TabNavigationBar bar) {
		List<AbstractWidget> buttons = new ArrayList<>();

		for (GuiEventListener child : bar.children()) {
			if (child instanceof AbstractWidget w) {
				buttons.add(w);
			}
		}

		if (!buttons.isEmpty()) {
			int longest = 0;

			for (AbstractWidget b : buttons) {
				longest = Math.max(longest, Math.max(this.font.width(b.getMessage()), CFHGuiDraw.width(b.getMessage())));
			}

			int given = buttons.get(0).getWidth();
			int each = Mth.roundToward(Math.min(longest + 18, (this.width - 28) / buttons.size()), 2);
			if (each > given) {
				int x = Mth.roundToward((this.width - each * buttons.size()) / 2, 2);

				for (AbstractWidget b : buttons) {
					b.setWidth(each);
					b.setX(x);
					x += each;
				}
			}
		}
	}

	void placeFoot(List<AbstractWidget> foot) {
		if (this.done != null) {
			List<AbstractWidget> all = new ArrayList<>(foot);
			all.add(this.done);
			int gap = 4;
			int room = this.width - 12;
			int rows = this.footH > 34 ? 2 : 1;
			List<List<AbstractWidget>> lines = new ArrayList<>();
			List<AbstractWidget> line = new ArrayList<>();
			int used = -4;

			for (AbstractWidget w : all) {
				if (!line.isEmpty() && used + 4 + w.getWidth() > room && lines.size() < rows - 1) {
					lines.add(line);
					line = new ArrayList<>();
					used = -4;
				}

				line.add(w);
				used += 4 + w.getWidth();
			}

			lines.add(line);
			int y = this.height - this.footH + (this.footH - (lines.size() * 20 + (lines.size() - 1) * 4)) / 2 + 1;

			for (List<AbstractWidget> row : lines) {
				if (footWidth(row) > room) {
					int each = Math.max(40, (room - 4 * (row.size() - 1)) / row.size());

					for (AbstractWidget w : row) {
						w.setWidth(Math.min(w.getWidth(), each));
					}
				}

				int x = (this.width - footWidth(row)) / 2;

				for (AbstractWidget w : row) {
					w.setX(x);
					w.setY(y);
					x += w.getWidth() + 4;
				}

				y += 24;
			}
		}
	}

	@Nullable
	private TeamsTab current() {
		return (this.tabManager == null ? null : this.tabManager.getCurrentTab()) instanceof TeamsTab t ? t : null;
	}

	void remake() {
		TeamsTab tab = this.current();
		if (tab != null) {
			this.startTab = this.tabs.indexOf(tab);
		}

		this.rebuildWidgets();
	}

	void go(String page) {
		int i = TeamsClient.PAGES.indexOf(page);
		if (this.bar != null && i >= 0 && i < this.tabs.size()) {
			this.bar.selectTab(i, true);
		}
	}

	public void tick() {
		super.tick();
		TeamsTab tab = this.current();
		if (tab != null) {
			lastTab = Math.max(0, this.tabs.indexOf(tab));
		}

		if (this.pendingDialog != null) {
			Consumer<TeamFlows> open = this.pendingDialog;
			this.pendingDialog = null;
			open.accept(this.flows);
		} else {
			if (this.dirty) {
				this.dirty = false;
				if (TeamsView.hasTeam() != this.hadTeam || TeamsView.available() != this.wasAvailable) {
					this.remake();
				} else if (tab != null) {
					tab.refresh();
				}
			}

			TeamsTab now = this.current();
			if (now != null) {
				now.tick();
			}
		}
	}

	public void removed() {
		super.removed();
		if (this.listening) {
			this.listening = false;
			TeamsState.CHANGED.unregister(this.changed);
			TeamsState.CLAIMS_CHANGED.unregister(this.claimsChanged);
		}

		TeamsTab tab = this.current();
		if (tab != null) {
			this.startTab = this.tabs.indexOf(tab);
		}

		TeamsView.apply(TeamsView.Pretend.NOTHING);
		if (System.currentTimeMillis() - ClaimMap.lastUsedMs() < 1500L) {
			TeamsHud.showClaimBorders();
		}
	}

	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}

	public boolean isPauseScreen() {
		return false;
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.bar != null && this.bar.keyPressed(keyCode)) {
			return true;
		} else {
			return super.keyPressed(keyCode, scanCode, modifiers)
				? true
				: (keyCode == 257 || keyCode == 335) && this.current() instanceof SettingsTab settings && settings.enter();
		}
	}

	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		int line = this.height - this.footH;
		if (Theme.current().isVanilla()) {
			RenderSystem.enableBlend();
			g.blit(this.minecraft.level == null ? Screen.FOOTER_SEPARATOR : Screen.INWORLD_FOOTER_SEPARATOR, 0, line - 2, 0.0F, 0.0F, this.width, 2, 32, 2);
			RenderSystem.disableBlend();
		} else {
			g.fill(0, line - 1, this.width, line, Theme.current().palette().border());
		}

		TeamsTab tab = this.current();
		if (tab != null) {
			tab.render(g, mouseX, mouseY, partialTick);
		}
	}

	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(g, mouseX, mouseY, partialTick);
		int top = this.bar == null ? 24 : this.bar.getRectangle().bottom();
		int bottom = this.height - this.footH - (Theme.current().isVanilla() ? 2 : 1);
		Ground.band(g, 0, top, this.width, bottom - top);
	}

	protected void renderMenuBackground(GuiGraphics g) {
		int top = this.bar == null ? 24 : this.bar.getRectangle().bottom();
		if (Theme.current().isVanilla()) {
			g.blit(CreateWorldScreen.TAB_HEADER_BACKGROUND, 0, 0, 0.0F, 0.0F, this.width, top, 16, 16);
		}

		this.renderMenuBackground(g, 0, top, this.width, this.height);
	}
}
