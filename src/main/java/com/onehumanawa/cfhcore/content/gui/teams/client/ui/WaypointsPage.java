package com.onehumanawa.cfhcore.content.gui.teams.client.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel.Style;
import com.onehumanawa.cfhcore.content.gui.teams.client.WaypointIcons;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.PaintedCard;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsDraw;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

final class WaypointsPage extends TeamsPage {
	private CFHGuiScrollPanel panel;
	private CFHGuiLabel count;
	private CFHGuiButton add;

	WaypointsPage(TeamsHubScreen screen) {
		super(screen, "waypoints", Icon.PIN);
	}

	@Override
	protected void buildPage(SidebarScreen s, Rect area) {
		this.panel = null;
		TeamData t = TeamsView.team();
		if (t == null) {
			this.noTeam(s, area, Icon.PIN, TeamsText.t("waypoints.no_team"));
		} else {
			this.add = new CFHGuiButton(0, 0, 96, TeamsText.t("waypoints.add"), () -> this.flows().dialogs().waypoint(null)).variant(Variant.PRIMARY).icon(Icon.PLUS);
			this.add.setWidth(Math.max(84, this.add.preferredWidth() + 12));
			this.add.setX(area.right() - this.add.getWidth());
			this.add.setY(area.y());
			this.count = (CFHGuiLabel)s.addPageWidget(
				new CFHGuiLabel(area.x() + 2, area.y() + 6, area.w() - this.add.getWidth() - 10, Component.empty()).style(Style.MUTED)
			);
			s.addPageWidget(this.add);
			this.panel = (CFHGuiScrollPanel)s.addPageWidget(new CFHGuiScrollPanel(area.x(), area.y() + 26, area.w(), area.h() - 26));
			this.fill();
		}
	}

	@Override
	void stateChanged() {
		if (this.panel != null && TeamsView.team() != null) {
			refill(this.panel, this::fill);
		} else {
			super.stateChanged();
		}
	}

	private void fill() {
		TeamData t = TeamsView.team();
		if (t != null) {
			int w = this.panel.innerWidth();
			int max = TeamsView.info().maxWaypoints;
			this.count.text(TeamsText.t("waypoints.count", t.waypoints.size(), max));
			boolean room = t.waypoints.size() < max;
			this.add.tip(room ? TeamsText.t("waypoints.add.desc") : TeamsText.shared("result.waypoint_limit")).enabled(room);
			if (t.waypoints.isEmpty()) {
				int h = Math.max(96, this.panel.getHeight() - 4);
				this.panel
					.add(
						new PaintedCard(
								w,
								h,
								(g, f) -> TeamsDraw.empty(
										g, new Rect(f.x(), f.y(), f.w(), f.h()), Icon.PIN, TeamsText.t("waypoints.empty"), TeamsText.t("waypoints.empty.desc"), f.alpha()
									)
							)
							.bare(),
						0,
						0
					);
				this.panel.setContentHeight(h);
			} else {
				List<TeamData.Waypoint> sorted = new ArrayList<>(t.waypoints);
				sorted.sort(
					Comparator.<TeamData.Waypoint, Boolean>comparing(px -> TeamsView.distanceTo(px) < 0.0)
						.thenComparingDouble(px -> Math.max(0.0, TeamsView.distanceTo(px)))
						.thenComparing(px -> px.name, String.CASE_INSENSITIVE_ORDER)
				);
				int y = 0;

				for (TeamData.Waypoint p : sorted) {
					this.panel.add(this.row(t, p, w), 0, y);
					y += 32;
				}

				this.panel.setContentHeight(y);
			}
		}
	}

	private PaintedCard row(TeamData t, TeamData.Waypoint p, int w) {
		boolean edit = this.flows().canEdit(p);
		int buttons = edit ? 58 : 22;
		TeamData.Member by = t.member(p.by);
		PaintedCard card = new PaintedCard(
				w,
				28,
				(g, f) -> {
					Theme th = Theme.current();
					Palette pal = th.palette();
					boolean shown = this.flows().shows(p);
					float a = f.alpha() * (shown ? 1.0F : 0.55F);
					TeamsDraw.glyphTile(g, WaypointIcons.get(p.icon), f.x() + 5, f.y() + 4, 20, p.color, a);
					int tx = f.x() + 31;
					int room = f.w() - 31 - buttons - 6;
					double far = TeamsView.distanceTo(p);
					Component away = far < 0.0 ? TeamsText.dimension(p.dimension) : TeamsText.distance(far);
					int awayW = CFHGuiDraw.width(away);
					TeamsDraw.textFit(g, Component.literal(p.name), tx, f.y() + 5, room - awayW - 6, th.isVanilla() ? -1 : pal.text(), a);
					TeamsDraw.text(g, away, tx + room - awayW, f.y() + 5, far < 0.0 ? pal.textDim() : pal.textMuted(), a);
					Component place = far < 0.0
						? Component.literal(TeamsText.coords(p.x, p.y, p.z))
						: TeamsText.t("waypoints.place", TeamsText.coords(p.x, p.y, p.z), TeamsText.dimension(p.dimension));
					TeamsDraw.textFit(g, place, tx, f.y() + 16, room, pal.textDim(), a);
					List<Component> tip = new ArrayList<>();
					tip.add(Component.literal(p.name).withStyle(st -> st.withColor(TeamsDraw.ink(p.color) & 16777215)));
					tip.add(TeamsText.t("waypoints.place", TeamsText.coords(p.x, p.y, p.z), TeamsText.dimension(p.dimension)).withStyle(ChatFormatting.GRAY));
					if (by != null) {
						tip.add(TeamsText.t("waypoints.by", by.name, TeamsText.ago(p.at)).withStyle(ChatFormatting.GRAY));
					}

					if (!shown) {
						tip.add(TeamsText.t("waypoints.hidden").withStyle(ChatFormatting.GRAY));
					}

					f.tip(0, 0, f.w() - buttons - 4, f.h(), tip);
				}
			)
			.flat()
			.narrate(Component.literal(p.name));
		CFHGuiIconButton eye = new CFHGuiIconButton(0, 0, 16, this.flows().shows(p) ? Icon.EYE : Icon.EYE_OFF, Component.empty(), null);
		eye.tip(TeamsText.t(this.flows().shows(p) ? "waypoints.hide" : "waypoints.show"));
		eye.onPress(() -> {
			boolean now = !this.flows().shows(p);
			this.flows().setShown(p, now);
			eye.setIcon(now ? Icon.EYE : Icon.EYE_OFF);
			eye.tip(TeamsText.t(now ? "waypoints.hide" : "waypoints.show"));
		});
		card.add(eye, w - (edit ? 58 : 22), 6);
		if (edit) {
			card.add(new CFHGuiIconButton(0, 0, 16, Icon.EDIT, TeamsText.t("waypoints.edit"), () -> this.flows().dialogs().waypoint(p)), w - 40, 6);
			card.add(new CFHGuiIconButton(0, 0, 16, Icon.TRASH, TeamsText.t("action.remove"), () -> this.flows().removeWaypoint(p)), w - 22, 6);
		}

		return card;
	}
}
