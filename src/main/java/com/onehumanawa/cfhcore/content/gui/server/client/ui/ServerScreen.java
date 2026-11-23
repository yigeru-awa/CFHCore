package com.onehumanawa.cfhcore.content.gui.server.client.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Node;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarPage;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popup;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerState;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Ui;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public abstract class ServerScreen extends SidebarScreen {
	@Nullable
	private String pendingPage;
	@Nullable
	private Runnable pendingDialog;
	private final Runnable changed = () -> this.dirty = true;
	private final Runnable helloChanged = () -> this.helloDirty = true;
	private final Runnable dashboardChanged = () -> this.dashboardDirty = true;
	private boolean listening;
	private boolean dirty;
	private boolean helloDirty;
	private boolean dashboardDirty;
	private List<String> shownPages = List.of();
	private ServerState.Pretend pretending = ServerState.Pretend.NOTHING;

	public ServerScreen pretending(ServerState.Pretend state) {
		this.pretending = state;
		return this;
	}

	protected ServerScreen(Component title, @Nullable Screen parent, String rememberKey, @Nullable String page) {
		super(title, parent, rememberKey);
		this.pendingPage = page;
		this.pendingDialog = ServerUi.takePendingDialog();
	}

	protected abstract List<String> pageIds();

	@Nullable
	protected abstract SidebarPage page(String var1);

	protected abstract Node menuNode();

	protected abstract int headerStatus(GuiGraphics var1, int var2, int var3, int var4, int var5);

	protected boolean showPageTitle() {
		return false;
	}

	public Component getTitle() {
		SidebarPage page = this.pages().isEmpty() ? null : this.currentPage();
		return (Component)(page == null ? super.getTitle() : Component.literal(super.getTitle().getString() + " – " + page.title().getString()));
	}

	protected void definePages(List<SidebarPage> pages) {
		List<String> ids = this.pageIds();

		for (String id : ids) {
			SidebarPage p = this.page(id);
			if (p != null) {
				pages.add(p);
			}
		}

		this.shownPages = List.copyOf(ids);
	}

	protected void build() {
		ServerState.pretend(this.pretending);
		super.build();
		if (!this.listening) {
			this.listening = true;
			ServerState.CHANGED.register(this.changed);
			ServerState.DASHBOARD_CHANGED.register(this.dashboardChanged);
			ServerState.HELLO_CHANGED.register(this.helloChanged);
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
			ServerState.CHANGED.unregister(this.changed);
			ServerState.DASHBOARD_CHANGED.unregister(this.dashboardChanged);
			ServerState.HELLO_CHANGED.unregister(this.helloChanged);
		}

		Popups.closeAll();
		ServerState.pretend(ServerState.Pretend.NOTHING);
	}

	public void tick() {
		super.tick();
		if (this.pendingDialog != null) {
			Runnable open = this.pendingDialog;
			this.pendingDialog = null;
			open.run();
		}

		if (this.helloDirty) {
			this.helloDirty = false;
			this.dirty = false;
			if (!this.pageIds().equals(this.shownPages)) {
				SidebarPage was = this.currentPage();
				Popups.closeAll();
				this.pages().clear();
				this.rebuildWidgets();
				if (was != null) {
					this.showPage(was.id());
				}
			} else {
				this.refreshPage();
			}
		} else {
			if (this.dashboardDirty) {
				this.dashboardDirty = false;
				if (this.currentPage() instanceof ServerPage page) {
					page.dashboardChanged();
				}
			}

			if (this.dirty) {
				this.dirty = false;
				if (this.currentPage() instanceof ServerPage page) {
					page.stateChanged();
				}
			}
		}
	}

	public boolean isPauseScreen() {
		return false;
	}

	public void go(String pageId) {
		this.showPage(pageId);
	}

	protected List<String> shownPages() {
		return new ArrayList<>(this.shownPages);
	}

	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		Popup top = Popups.top();
		boolean covered = top != null && top.isModal();
		super.render(g, covered ? -1000 : mouseX, covered ? -1000 : mouseY, partialTick);
	}

	protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		Palette p = Theme.current().palette();
		int y = 11;
		int edge = this.width - 12;
		int right = edge;
		if (ServerState.preview()) {
			right = Ui.chipRight(g, Txt.t("preview"), edge, 9, p.warning(), 1.0F) - 6;
			if (mouseX >= right && mouseX < edge && mouseY >= 9 && mouseY < 21) {
				CFHGuiTooltips.request(Txt.t("preview.desc"), null);
			}
		}

		if (ServerState.available() && !ServerState.newerServer()) {
			int left = this.headerTitleX() + this.font.width(Fonts.heading(this.getTitle())) + 16;
			if (right - left >= 60) {
				CFHGuiDraw.scissor(g, left, 0, right - left, 32);
				this.headerStatus(g, right, 11, mouseX, mouseY);
				CFHGuiDraw.unscissor(g);
			}
		}
	}
}
