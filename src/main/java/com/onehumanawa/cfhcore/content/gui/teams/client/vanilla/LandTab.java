package com.onehumanawa.cfhcore.content.gui.teams.client.vanilla;

import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.LandRuleList;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamFlows;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.map.ClaimMap;
import com.onehumanawa.cfhcore.content.gui.teams.model.Access;
import com.onehumanawa.cfhcore.content.gui.teams.model.LandRules;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class LandTab extends TeamsTab {
	private static final int RULES_W = 176;
	@Nullable
	private ClaimMap map;
	@Nullable
	private LandTab.RuleList rules;
	private boolean builtPersonal;

	LandTab(VanillaTeamsScreen screen) {
		super(screen, "land");
	}

	@Override
	protected boolean needsTeam() {
		return false;
	}

	@Override
	protected void build() {
		this.map = this.add(new ClaimMap(0, 0, 100, 100));
		TeamData t = TeamsView.land();
		this.builtPersonal = t != null && t.solo;
		if (t != null) {
			this.rules = this.add(new LandTab.RuleList(this.mc));
			this.rules.fill(t.land);
			if (t.solo) {
				Button found = this.footButton(TeamsText.t("solo.create"), () -> this.screen.go("overview"));
				found.setTooltip(Tooltip.create(TeamsText.t("solo.create.desc")));
				Button giveUp = this.footButton(TeamsText.t("action.give_up"), () -> this.flows().giveUpLand());
				giveUp.setTooltip(Tooltip.create(TeamsText.t("solo.give_up.desc")));
			}
		}
	}

	@Override
	void refresh() {
		TeamData t = TeamsView.land();
		if (this.rules != null && t != null && t.solo == this.builtPersonal) {
			this.rules.fill(t.land);
		} else {
			if (this.rules == null && t == null) {
				return;
			}

			super.refresh();
		}
	}

	@Override
	protected void layout(ScreenRectangle area) {
		if (this.map != null) {
			int rulesW = this.rules == null ? 0 : Math.min(176, area.width() * 2 / 5);
			this.map.setX(area.left() + 8);
			this.map.setY(area.top() + 6);
			this.map.setWidth(area.width() - 16 - (rulesW == 0 ? 0 : rulesW + 6));
			this.map.setHeight(area.height() - 12);
			if (this.rules != null) {
				this.rules.place(area.right() - 8 - rulesW, area.top() + 6, rulesW, area.height() - 12);
			}
		}
	}

	private static final class RuleList extends ContainerObjectSelectionList<LandTab.RuleList.Entry> {
		private int rowWidth = 160;

		RuleList(Minecraft mc) {
			super(mc, 100, 100, 0, 24);
		}

		void place(int x, int y, int width, int height) {
			this.setX(x);
			this.setY(y);
			this.setWidth(width);
			this.setHeight(height);
			this.rowWidth = Math.max(60, width - 14);

			for (LandTab.RuleList.Entry e : this.children()) {
				e.button.setWidth(this.rowWidth);
			}

			this.clampScrollAmount();
		}

		void fill(LandRules land) {
			double at = this.getScrollAmount();
			this.clearEntries();

			for (LandRuleList.Rule rule : LandRuleList.forLand()) {
				this.addEntry(new LandTab.RuleList.Entry(this.button(rule, land)));
			}

			this.setScrollAmount(at);
			this.clampScrollAmount();
		}

		private AbstractWidget button(LandRuleList.Rule rule, LandRules land) {
			Runnable revert = () -> {
				TeamData t = TeamsView.land();
				if (t != null) {
					this.fill(t.land);
				}
			};

			AbstractWidget b = switch (rule.kind()) {
				case ACCESS -> {
					boolean own = TeamsView.personal() != null;
					Access now = own && rule.access(land) == Access.ALLIES ? Access.MEMBERS : rule.access(land);
					yield CycleButton.builder(own ? LandRuleList::ownAccess : TeamsText::access)
						.withValues(own ? LandRuleList.OWN_ACCESS : List.of(Access.values()))
						.withInitialValue(now)
						.create(0, 0, this.rowWidth, 20, rule.label(), (button, value) -> TeamFlows.setLandRule(rule.field(), value.name(), revert));
				}
				case SWITCH -> CycleButton.onOffBuilder(rule.on(land))
				.create(0, 0, this.rowWidth, 20, rule.label(), (button, value) -> TeamFlows.setLandRule(rule.field(), String.valueOf(value), revert));
				case RANK -> CycleButton.builder(TeamsText::rank)
				.withValues(LandRuleList.CLAIM_RANKS)
				.withInitialValue(land.claimRank)
				.create(0, 0, this.rowWidth, 20, rule.label(), (button, value) -> TeamFlows.setLandRule(rule.field(), value.name(), revert));
			};
			Component why = rule.whyLocked();
			b.active = why == null;
			b.setTooltip(Tooltip.create((Component)(why == null ? rule.description() : Component.empty().append(rule.description()).append("\n").append(why))));
			return b;
		}

		public int getRowWidth() {
			return this.rowWidth;
		}

		protected int getScrollbarPosition() {
			return this.getX() + this.getWidth() - 6;
		}

		protected void renderListBackground(GuiGraphics g) {
		}

		protected void renderListSeparators(GuiGraphics g) {
		}

		static final class Entry extends net.minecraft.client.gui.components.ContainerObjectSelectionList.Entry<LandTab.RuleList.Entry> {
			final AbstractWidget button;

			Entry(AbstractWidget button) {
				this.button = button;
			}

			public void render(GuiGraphics g, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
				this.button.setX(left);
				this.button.setY(top);
				this.button.render(g, mouseX, mouseY, partialTick);
			}

			public List<? extends GuiEventListener> children() {
				return List.of(this.button);
			}

			public List<? extends NarratableEntry> narratables() {
				return List.of(this.button);
			}
		}
	}
}
