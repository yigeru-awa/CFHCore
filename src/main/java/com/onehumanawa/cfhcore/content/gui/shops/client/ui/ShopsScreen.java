package com.onehumanawa.cfhcore.content.gui.shops.client.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarPage;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popup;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import com.onehumanawa.cfhcore.content.gui.shops.client.ShopsState;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.Ui;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class ShopsScreen extends SidebarScreen {
	public static final String BROWSE = "browse";
	public static final String MINE = "mine";
	public static final String WALLET = "wallet";
	public static final String ADMIN = "admin";
	public static final List<String> PAGES = List.of("browse", "mine", "wallet", "admin");
	@Nullable
	private String pendingPage;
	@Nullable
	private String pendingShop;
	private List<String> shownPages = List.of();
	private final Runnable changed = () -> this.pagesDirty = true;
	private boolean listening;
	private boolean pagesDirty;

	public ShopsScreen(@Nullable Screen parent, @Nullable String page, @Nullable String shop) {
		super(Txt.t("title"), parent, "cfhgui_shops:shops");
		this.pendingPage = shop != null ? "browse" : page;
		this.pendingShop = shop;
	}

	@Nullable
	public Screen parentScreen() {
		return this.parent;
	}

	private List<String> pageIds() {
		List<String> out = new ArrayList<>();
		out.add("browse");
		if (!ShopsState.available() || ShopsState.hello().playerShops && ShopsState.may("create")) {
			out.add("mine");
		}

		out.add("wallet");
		if (ShopsState.may("admin")) {
			out.add("admin");
		}

		return out;
	}

	protected void definePages(List<SidebarPage> pages) {
		List<String> ids = this.pageIds();

		for (String id : ids) {
			pages.add((SidebarPage)(switch (id) {
				case "mine" -> new MyShopPage(this);
				case "wallet" -> new WalletPage(this);
				case "admin" -> new AdminShopsPage("admin", this);
				default -> new BrowsePage(this);
			}));
		}

		this.shownPages = List.copyOf(ids);
	}

	protected void build() {
		if (this.pendingShop != null) {
			BrowsePage.openOn(this.pendingShop);
		}

		super.build();
		if (!this.listening) {
			this.listening = true;
			ShopsState.CHANGED.register(this.changed);
		}

		if (this.pendingPage != null) {
			String page = this.pendingPage;
			this.pendingPage = null;
			this.pendingShop = null;
			this.showPage(page);
		}
	}

	public void tick() {
		super.tick();
		if (this.pagesDirty) {
			this.pagesDirty = false;
			if (!this.pageIds().equals(this.shownPages)) {
				SidebarPage was = this.currentPage();
				if (was != null) {
					was.onHide();
				}

				Popups.closeAll();
				this.pages().clear();
				this.rebuildWidgets();
				if (was != null) {
					this.showPage(was.id());
				}
			}
		}
	}

	public void removed() {
		super.removed();
		if (this.listening) {
			this.listening = false;
			ShopsState.CHANGED.unregister(this.changed);
		}

		Popups.closeAll();
	}

	protected boolean showPageTitle() {
		return false;
	}

	public Component getTitle() {
		SidebarPage page = this.pages().isEmpty() ? null : this.currentPage();
		return (Component)(page == null ? super.getTitle() : Component.literal(super.getTitle().getString() + " – " + page.title().getString()));
	}

	public boolean isPauseScreen() {
		return false;
	}

	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		Popup top = Popups.top();
		boolean covered = top != null && top.isModal();
		super.render(g, covered ? -1000 : mouseX, covered ? -1000 : mouseY, partialTick);
	}

	protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		if (ShopsState.available() && !ShopsState.newerServer()) {
			int y = 11;
			int left = this.headerTitleX() + this.font.width(Fonts.heading(this.getTitle())) + 16;
			int right = this.width - 12;
			Component balance = Txt.t("header.balance", ShopsState.money(ShopsState.balance()));
			if (right - left >= Ui.chipWidth(balance) + 4) {
				CFHGuiDraw.scissor(g, left, 0, right - left, 32);
				Ui.chipRight(g, balance, right, 9, Theme.current().palette().success(), 1.0F);
				CFHGuiDraw.unscissor(g);
			}
		}
	}
}
