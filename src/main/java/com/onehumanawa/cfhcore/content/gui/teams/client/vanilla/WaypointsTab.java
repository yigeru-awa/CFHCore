package com.onehumanawa.cfhcore.content.gui.teams.client.vanilla;

import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class WaypointsTab extends TeamsTab {
	@Nullable
	private RowList list;
	@Nullable
	private Button change;
	@Nullable
	private Button remove;
	@Nullable
	private Button eye;

	WaypointsTab(VanillaTeamsScreen screen) {
		super(screen, "waypoints");
	}

	@Override
	protected void build() {
		this.list = ((RowList)this.add(new RowList(this.mc, 26))).onSelect(this::update).emptyText(TeamsText.t("waypoints.empty"));
		this.list.onActivate(row -> {
			if (row.value instanceof TeamData.Waypoint w && this.flows().canEdit(w)) {
				this.flows().dialogs().waypoint(w);
			}
		});
		TeamData t = TeamsView.team();
		boolean room = t != null && t.waypoints.size() < TeamsView.info().maxWaypoints;
		enable(this.footButton(TeamsText.t("vanilla.waypoint.add"), () -> this.flows().dialogs().waypoint(null)), room, TeamsText.shared("result.waypoint_limit"));
		this.change = this.footButton(TeamsText.t("vanilla.waypoint.change"), () -> {
			TeamData.Waypoint w = this.chosen();
			if (w != null) {
				this.flows().dialogs().waypoint(w);
			}
		});
		this.remove = this.footButton(TeamsText.t("action.remove"), () -> {
			TeamData.Waypoint w = this.chosen();
			if (w != null) {
				this.flows().removeWaypoint(w);
			}
		});
		this.eye = this.footButton(TeamsText.t("vanilla.waypoint.show"), () -> {
			TeamData.Waypoint w = this.chosen();
			if (w != null) {
				this.flows().setShown(w, !this.flows().shows(w));
				this.update();
			}
		});
		this.fill();
	}

	@Nullable
	private TeamData.Waypoint chosen() {
		return this.list == null ? null : this.list.chosen(TeamData.Waypoint.class);
	}

	private void fill() {
		TeamData t = TeamsView.team();
		if (t != null && this.list != null) {
			RowList l = this.list;
			List<TeamData.Waypoint> sorted = new ArrayList<>(t.waypoints);
			sorted.sort(
				Comparator.<TeamData.Waypoint, Boolean>comparing(px -> TeamsView.distanceTo(px) < 0.0)
					.thenComparingDouble(px -> Math.max(0.0, TeamsView.distanceTo(px)))
					.thenComparing(px -> px.name, String.CASE_INSENSITIVE_ORDER)
			);
			List<RowList.Row> rows = new ArrayList<>();

			for (TeamData.Waypoint p : sorted) {
				rows.add(l.row("waypoint:" + p.id, p, Component.literal(p.name), (g, x, y, w, h, hovered, selected, mouseX, mouseY) -> {
					double far = TeamsView.distanceTo(p);
					Component right = far < 0.0 ? TeamsText.dimension(p.dimension) : TeamsText.distance(far);
					Component place = TeamsText.t("waypoints.place", TeamsText.coords(p.x, p.y, p.z), TeamsText.dimension(p.dimension));
					Rows.waypoint(g, p, place, right, this.flows().shows(p), x, y, w, h);
				}));
			}

			l.rows(rows);
		}
	}

	private void update() {
		if (this.change != null && this.remove != null && this.eye != null) {
			TeamData.Waypoint w = this.chosen();
			boolean edit = w != null && this.flows().canEdit(w);
			this.change.active = edit;
			this.remove.active = edit;
			this.eye.active = w != null;
			this.eye.setMessage(TeamsText.t(w != null && !this.flows().shows(w) ? "vanilla.waypoint.show" : "vanilla.waypoint.hide"));
		}
	}

	@Override
	void refresh() {
		if (this.list != null && TeamsView.hasTeam()) {
			this.fill();
		} else {
			super.refresh();
		}
	}

	@Override
	protected void layout(ScreenRectangle area) {
		if (this.list != null) {
			this.list.place(area.left(), area.top(), area.width(), area.height());
		}
	}
}
