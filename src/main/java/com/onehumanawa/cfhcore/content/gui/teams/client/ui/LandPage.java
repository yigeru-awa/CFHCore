package com.onehumanawa.cfhcore.content.gui.teams.client.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiDropdown;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSectionHeader;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSegmented;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToggle;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel.Style;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.LandRuleList;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.PaintedCard;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamFlows;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsDraw;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.map.ClaimMap;
import com.onehumanawa.cfhcore.content.gui.teams.model.Access;
import com.onehumanawa.cfhcore.content.gui.teams.model.LandRules;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class LandPage extends TeamsPage {
	private static final int HEADER_H = 20;
	private static final int SIDE_W = 232;
	private static final int ROW_H = 24;
	private static boolean sheetOpen;
	@Nullable
	private ClaimMap map;
	@Nullable
	private CFHGuiScrollPanel rules;
	@Nullable
	private CFHGuiSectionHeader header;
	private final Anim sheet = new Anim(0.0F, 240, Ease.OUT_CUBIC);
	private boolean wide;
	private boolean builtWithLand;
	private boolean builtPersonal;
	private Rect area = new Rect(0, 0, 0, 0);

	LandPage(TeamsHubScreen screen) {
		super(screen, "land", Icon.MAP);
	}

	@Override
	protected void buildPage(SidebarScreen s, Rect area) {
		this.area = area;
		this.rules = null;
		this.header = null;
		boolean land = TeamsView.land() != null;
		this.builtWithLand = land;
		this.builtPersonal = TeamsView.personal() != null;
		this.wide = land && area.w() >= 480;
		int mapW = this.wide ? area.w() - 232 - 8 : area.w();
		this.map = (ClaimMap)s.addPageWidget(new ClaimMap(area.x(), area.y(), mapW, area.h()));
		if (land) {
			Component title = TeamsText.t(this.builtPersonal ? "land.rules.own" : "land.rules");
			if (this.wide) {
				this.header = (CFHGuiSectionHeader)s.addPageWidget(new CFHGuiSectionHeader(area.right() - 232, area.y(), 232, title, 0, false, null));
				this.rules = (CFHGuiScrollPanel)s.addPageWidget(new CFHGuiScrollPanel(area.right() - 232, area.y() + 20, 232, area.h() - 20));
			} else {
				this.sheet.snap(sheetOpen ? 1.0F : 0.0F);
				this.header = (CFHGuiSectionHeader)s.addPageWidget(
					new CFHGuiSectionHeader(area.x(), area.bottom() - 20, area.w(), title, LandRuleList.forLand().size(), !sheetOpen, () -> {
						sheetOpen = !sheetOpen;
						this.sheet.set(sheetOpen ? 1.0F : 0.0F);
					})
				);
				this.header.tip(TeamsText.t("land.rules.desc"));
				this.rules = (CFHGuiScrollPanel)s.addPageWidget(new CFHGuiScrollPanel(area.x(), area.bottom(), area.w(), 1));
			}

			fillRules(this.rules);
			this.place();
		}
	}

	@Override
	void stateChanged() {
		if (TeamsView.land() != null == this.builtWithLand && TeamsView.personal() != null == this.builtPersonal) {
			if (this.rules != null) {
				refillRules(this.rules);
			}
		} else {
			super.stateChanged();
		}
	}

	private void place() {
		if (this.map != null && !this.wide && this.rules != null && this.header != null) {
			int full = Math.min(this.rules.contentHeight(), Math.round((float)this.area.h() * 0.62F));
			int open = Math.round((float)full * this.sheet.get());
			this.header.setY(this.area.bottom() - 20 - open);
			this.rules.setY(this.area.bottom() - open);
			this.rules.setHeight(Math.max(1, open));
			this.rules.visible = open > 2;
			this.map.setHeight(Math.max(40, this.area.h() - 20 - open - 2));
		}
	}

	public void render(SidebarScreen s, GuiGraphics g, Rect area, int mouseX, int mouseY, float partialTick) {
		this.place();
	}

	static void fillRules(CFHGuiScrollPanel rules) {
		TeamData t = TeamsView.land();
		if (t != null) {
			int w = rules.innerWidth();
			int y = 2;

			for (LandRuleList.Rule rule : LandRuleList.forLand()) {
				rules.add(row(rule, t.land, w, rules), 0, y);
				y += 26;
			}

			if (t.solo) {
				y = ownLand(rules, w, y);
			}

			rules.setContentHeight(y);
		}
	}

	private static int ownLand(CFHGuiScrollPanel rules, int w, int top) {
		int y = section(rules, top + 4, w, TeamsText.t("solo.section"));
		CFHGuiLabel words = new CFHGuiLabel(0, 0, w - 4, TeamsText.t("solo.land_note")).style(Style.MUTED).wrap(true);
		rules.add(words, 2, y);
		y += words.getHeight() + 6;
		TeamFlows flows = Minecraft.getInstance().screen instanceof TeamsHubScreen hub ? hub.flows() : null;
		int bw = (w - 6) / 2;
		CFHGuiButton found = new CFHGuiButton(0, 0, bw, TeamsText.t("solo.create"), () -> {
			if (Minecraft.getInstance().screen instanceof TeamsHubScreen hubx) {
				hubx.go("overview");
			}
		}).variant(Variant.PRIMARY).icon(Icon.PLUS);
		found.tip(TeamsText.t("solo.create.desc"));
		rules.add(found, 0, y);
		CFHGuiButton giveUp = new CFHGuiButton(0, 0, w - bw - 6, TeamsText.t("action.give_up"), () -> {
			if (flows != null) {
				flows.giveUpLand();
			}
		}).variant(Variant.DANGER).icon(Icon.TRASH);
		giveUp.tip(TeamsText.t("solo.give_up.desc"));
		rules.add(giveUp, bw + 6, y);
		return y + 24;
	}

	static void refillRules(CFHGuiScrollPanel rules) {
		refill(rules, () -> fillRules(rules));
	}

	private static PaintedCard row(LandRuleList.Rule rule, LandRules land, int w, CFHGuiScrollPanel rules) {
		Component why = rule.whyLocked();
		AbstractWidget control = control(rule, land, w, rules);
		control.active = why == null;
		int controlW = control.getWidth();
		PaintedCard card = new PaintedCard(w, 24, (g, f) -> {
			Theme th = Theme.current();
			Palette p = th.palette();
			int room = f.w() - controlW - 16;
			int tx = f.x() + 6;
			if (why != null) {
				Icons.draw(g, Icon.LOCK, tx, f.y() + 8, 8, Colors.scaleAlpha(p.textDim(), f.alpha()));
				tx += 11;
				room -= 11;
			}

			TeamsDraw.textFit(g, rule.label(), tx, f.y() + 8, room, why != null ? p.textMuted() : (th.isVanilla() ? -1 : p.text()), f.alpha());
			List<Component> tip = new ArrayList<>();
			tip.add(rule.description());
			if (why != null) {
				tip.add(why.copy().withStyle(ChatFormatting.GRAY));
			}

			f.tip(0, 0, f.w() - controlW - 8, f.h(), tip);
		}).flat().narrate(rule.label());
		card.add(control, w - controlW - 4, (24 - control.getHeight()) / 2);
		return card;
	}

	private static AbstractWidget control(LandRuleList.Rule rule, LandRules land, int w, CFHGuiScrollPanel rules) {
		Runnable revert = () -> refillRules(rules);

		return (AbstractWidget)(switch (rule.kind()) {
			case ACCESS -> {
				boolean own = TeamsView.personal() != null;
				Access now = own && rule.access(land) == Access.ALLIES ? Access.MEMBERS : rule.access(land);
				Function<Access, Component> label = own ? LandRuleList::ownAccessShort : TeamsText::accessShort;
				Function<Access, Component> tip = own ? LandRuleList::ownAccess : TeamsText::access;
				yield new CFHGuiSegmented<>(
						0,
						0,
						Math.min(132, Math.max(120, w / 2 - 6)),
						own ? LandRuleList.OWN_ACCESS : List.of(Access.values()),
						now,
						label,
						v -> TeamFlows.setLandRule(rule.field(), v.name(), revert)
					)
					.optionTip(tip);
			}
			case SWITCH -> new CFHGuiToggle(0, 0, 22, Component.empty(), rule.on(land), v -> TeamFlows.setLandRule(rule.field(), String.valueOf(v), revert));
			case RANK -> new CFHGuiDropdown<>(
			0,
			0,
			Math.min(104, Math.max(84, w / 2 - 20)),
			LandRuleList.CLAIM_RANKS,
			land.claimRank,
			TeamsText::rank,
			v -> TeamFlows.setLandRule(rule.field(), v.name(), revert)
		);
		});
	}
}
