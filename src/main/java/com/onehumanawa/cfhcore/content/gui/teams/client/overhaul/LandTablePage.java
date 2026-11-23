package com.onehumanawa.cfhcore.content.gui.teams.client.overhaul;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarPage;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSectionHeader;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.TeamsHubScreen;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.LandRuleList;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.map.ClaimMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class LandTablePage extends SidebarPage implements TeamsHubScreen.StatePage {
	private static final int HEADER_H = 20;
	private static final int SIDE_W = 232;
	private static final int BUTTON = 16;
	private static boolean sheetOpen;
	private final OverhaulTeamsScreen screen;
	@Nullable
	private SidebarPage hosted;
	@Nullable
	private LandTableView view;
	@Nullable
	private CFHGuiScrollPanel rules;
	@Nullable
	private CFHGuiSectionHeader header;
	private final List<CFHGuiIconButton> buttons = new ArrayList<>();
	private final Anim sheet = new Anim(0.0F, 240, Ease.OUT_CUBIC);
	private boolean wide;
	private boolean builtWithLand;
	private boolean builtPersonal;
	private Rect area = new Rect(0, 0, 0, 0);

	LandTablePage(OverhaulTeamsScreen screen) {
		super("land", TeamsText.page("land"), Icon.MAP);
		this.screen = screen;
	}

	public void build(SidebarScreen s, Rect area) {
		this.screen.headerInset(0);
		this.area = area;
		this.hosted = null;
		this.view = null;
		this.rules = null;
		this.header = null;
		this.buttons.clear();
		if (!TeamsView.available()) {
			this.hosted = this.screen.custom("land");
			if (this.hosted != null) {
				this.hosted.build(s, area);
			}
		} else {
			boolean land = TeamsView.land() != null;
			this.builtWithLand = land;
			this.builtPersonal = TeamsView.personal() != null;
			this.wide = land && area.w() >= 480;
			int tableW = this.wide ? area.w() - 232 - 8 : area.w();
			LandTable table = this.screen.table();
			table.dress(TeamsView.team());
			LandTableView v = (LandTableView)s.addPageWidget(new LandTableView(new Rect(area.x(), area.y(), tableW, area.h()), table, OverhaulTeams.t("table.title")));
			this.view = v;
			this.buttons(s, v, table);
			if (!land) {
				this.place();
			} else {
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

				OverhaulTeamsScreen.rules(this.rules);
				this.place();
			}
		}
	}

	private void buttons(SidebarScreen s, LandTableView v, LandTable table) {
		int x = v.getX() + v.getWidth() - 16 - 4;
		int y = v.getY() + 4;
		y = this.button(s, v, x, y, Icon.PLUS, TeamsText.t("map.zoom_in"), () -> table.zoom(1));
		y = this.button(s, v, x, y, Icon.MINUS, TeamsText.t("map.zoom_out"), () -> table.zoom(-1));
		y = this.button(s, v, x, y, Icon.COMPASS, TeamsText.t("map.centre"), table::centre) + 3;
		y = this.button(s, v, x, y, Icon.UNDO, OverhaulTeams.t("table.turn_left"), () -> table.turn(-30.0F, 0.0F));
		y = this.button(s, v, x, y, Icon.REDO, OverhaulTeams.t("table.turn_right"), () -> table.turn(30.0F, 0.0F)) + 3;
		CFHGuiIconButton[] tool = new CFHGuiIconButton[1];
		y = this.button(s, v, x, y, Icon.SELECTION, TeamsText.t("map.tool.claim"), () -> {
			LandTableView.toggleTool();
			showTool(tool[0]);
		});
		tool[0] = this.buttons.get(this.buttons.size() - 1);
		showTool(tool[0]);
		this.button(s, v, x, y, Icon.QUESTION, OverhaulTeams.t("table.help"), () -> {
		});
		this.buttons
			.get(this.buttons.size() - 1)
			.tip(
				List.of(
					OverhaulTeams.t("table.help"),
					OverhaulTeams.t("table.help.drag").withStyle(ChatFormatting.GRAY),
					OverhaulTeams.t("table.help.right").withStyle(ChatFormatting.GRAY),
					OverhaulTeams.t("table.help.shift").withStyle(ChatFormatting.GRAY),
					OverhaulTeams.t("table.help.view").withStyle(ChatFormatting.GRAY),
					OverhaulTeams.t("table.help.keys").withStyle(ChatFormatting.GRAY)
				)
			);
	}

	private int button(SidebarScreen s, LandTableView v, int x, int y, Icon icon, Component label, Runnable press) {
		CFHGuiIconButton b = (CFHGuiIconButton)s.addPageWidget(new CFHGuiIconButton(x, y, 16, icon, label, press).variant(Variant.SECONDARY).iconSize(8).tip(label));
		this.buttons.add(b);
		v.cover(b);
		return y + 16 + 1;
	}

	private static void showTool(CFHGuiIconButton button) {
		boolean load = LandTableView.tool() == ClaimMap.Tool.KEEP_LOADED;
		button.setIcon(load ? Icon.BOLT : Icon.SELECTION).toggled(load);
		button.tip(
			List.of(
				TeamsText.t(load ? "map.tool.load" : "map.tool.claim"), TeamsText.t(load ? "map.tool.load.desc" : "map.tool.claim.desc").withStyle(ChatFormatting.GRAY)
			)
		);
	}

	private void place() {
		LandTableView v = this.view;
		if (v != null) {
			if (!this.wide && this.rules != null && this.header != null) {
				int full = Math.min(this.rules.contentHeight(), Math.round((float)this.area.h() * 0.62F));
				int open = Math.round((float)full * this.sheet.get());
				this.header.setY(this.area.bottom() - 20 - open);
				this.rules.setY(this.area.bottom() - open);
				this.rules.setHeight(Math.max(1, open));
				this.rules.visible = open > 2;
				v.setHeight(Math.max(40, this.area.h() - 20 - open - 2));
			}

			for (CFHGuiIconButton b : this.buttons) {
				b.visible = b.getY() + b.getHeight() <= v.getY() + v.getHeight() - 2;
			}
		}
	}

	@Override
	public void stateChanged() {
		if (this.hosted != null) {
			OverhaulTeamsScreen.followCustom(this.hosted);
		} else if (TeamsView.land() != null == this.builtWithLand && TeamsView.personal() != null == this.builtPersonal) {
			if (this.view != null) {
				this.screen.table().dress(TeamsView.team());
			}

			if (this.rules != null && TeamsView.land() != null) {
				OverhaulTeamsScreen.rules(this.rules);
			}
		} else {
			this.screen.refreshPage();
		}
	}

	public void render(SidebarScreen s, GuiGraphics g, Rect area, int mouseX, int mouseY, float partialTick) {
		if (this.hosted != null) {
			this.hosted.render(s, g, area, mouseX, mouseY, partialTick);
		}

		this.place();
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
