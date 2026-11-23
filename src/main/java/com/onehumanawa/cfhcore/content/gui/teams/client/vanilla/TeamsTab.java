package com.onehumanawa.cfhcore.content.gui.teams.client.vanilla;

import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamFlows;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsDraw;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

abstract class TeamsTab implements Tab {
	protected final VanillaTeamsScreen screen;
	protected final Minecraft mc = Minecraft.getInstance();
	private final String id;
	protected final List<AbstractWidget> widgets = new ArrayList<>();
	protected final List<AbstractWidget> foot = new ArrayList<>();
	protected ScreenRectangle area = ScreenRectangle.empty();
	private boolean built;

	TeamsTab(VanillaTeamsScreen screen, String id) {
		this.screen = screen;
		this.id = id;
	}

	String id() {
		return this.id;
	}

	protected TeamFlows flows() {
		return this.screen.flows();
	}

	public Component getTabTitle() {
		return TeamsText.page(this.id);
	}

	public void visitChildren(Consumer<AbstractWidget> consumer) {
		this.widgets.forEach(consumer);
		this.foot.forEach(consumer);
	}

	public void doLayout(ScreenRectangle rectangle) {
		this.area = rectangle;
		if (this.built) {
			this.layout(rectangle);
		}

		this.screen.placeFoot(this.foot);
	}

	final void rebuild() {
		this.widgets.clear();
		this.foot.clear();
		this.built = this.message() == null;
		if (this.built) {
			this.build();
		}
	}

	protected boolean built() {
		return this.built;
	}

	protected abstract void build();

	protected abstract void layout(ScreenRectangle var1);

	void refresh() {
		this.screen.remake();
	}

	void tick() {
	}

	void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		Component message = this.message();
		if (message != null) {
			TeamsDraw.textCentered(
				g, message, this.area.left() + this.area.width() / 2, this.area.top() + this.area.height() / 2 - 4, Theme.current().palette().textMuted(), 1.0F
			);
		}
	}

	@Nullable
	protected Component message() {
		if (!TeamsView.available()) {
			return TeamsText.shared(this.mc.level == null ? "state.no_world" : "state.no_server");
		} else if (this.needsTeam() && !TeamsView.hasTeam()) {
			return TeamsView.personal() != null ? TeamsText.t("solo.share") : TeamsText.shared("state.no_team");
		} else {
			return null;
		}
	}

	protected boolean needsTeam() {
		return true;
	}

	protected <T extends AbstractWidget> T add(T widget) {
		this.widgets.add(widget);
		return widget;
	}

	protected Button footButton(Component label, Runnable action) {
		Button b = Button.builder(label, button -> action.run()).size(Math.max(60, this.mc.font.width(label) + 14), 20).build();
		this.foot.add(b);
		return b;
	}

	protected static void enable(Button button, boolean on, @Nullable Component whyNot) {
		button.active = on;
		button.setTooltip(!on && whyNot != null ? Tooltip.create(whyNot) : null);
	}

	protected static void line(GuiGraphics g, Component text, int x, int y, int width, boolean strong) {
		Theme t = Theme.current();
		TeamsDraw.textFit(g, text, x, y, width, strong ? t.palette().text() : t.palette().textMuted(), 1.0F);
	}
}
