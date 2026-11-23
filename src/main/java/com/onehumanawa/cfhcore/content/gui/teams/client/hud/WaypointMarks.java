package com.onehumanawa.cfhcore.content.gui.teams.client.hud;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.teams.TeamsConfig;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsState;
import com.onehumanawa.cfhcore.content.gui.teams.client.WaypointIcons;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

final class WaypointMarks {
	private static final int LABEL_H = 12;
	private static final int NAME_MAX = 110;
	private static final float BEAM_GONE = 5.0F;
	private static final float BEAM_WHOLE = 14.0F;
	private static final float BEAM_HEIGHT = 200.0F;
	private static final Map<String, WaypointMarks.Label> LABELS = new HashMap<>();
	private static final WorldView.Spot SPOT = new WorldView.Spot();

	private static List<TeamData.Waypoint> waypoints() {
		List<TeamData.Waypoint> demo = HudDemo.waypoints();
		if (demo != null) {
			return demo;
		} else {
			TeamData team = TeamsState.team();
			return team == null ? List.of() : team.waypoints;
		}
	}

	static void beams(BufferBuilder b) {
		TeamsConfig cfg = TeamsConfig.get();
		if (cfg.worldWaypoints) {
			List<TeamData.Waypoint> list = waypoints();
			if (!list.isEmpty()) {
				Minecraft mc = Minecraft.getInstance();
				String dimension = TeamsState.currentDimension();
				float reach = Math.max(64.0F, (float)mc.options.getEffectiveRenderDistance() * 16.0F * 0.85F);
				float motion = HudDraw.theme().motion();
				float time = motion <= 0.0F ? Float.NaN : (float)(Clock.nowMs() % 600000L) / 1000.0F / motion;

				for (TeamData.Waypoint wp : list) {
					if (dimension.equals(wp.dimension) && cfg.showsWaypoint(wp.id)) {
						float x = (float)((double)wp.x + 0.5 - WorldView.camX());
						float y = (float)((double)wp.y - WorldView.camY());
						float z = (float)((double)wp.z + 0.5 - WorldView.camZ());
						float flat = (float)Math.sqrt((double)(x * x + z * z));
						float near = Mth.clamp((flat - 5.0F) / 9.0F, 0.0F, 1.0F);
						if (!(near <= 0.0F)) {
							float shrink = 1.0F;
							if (flat > reach) {
								shrink = reach / flat;
								x *= shrink;
								y *= shrink;
								z *= shrink;
							}

							float at = flat * shrink;
							float core = Math.max(0.13F, at * 0.0022F);
							float glow = Math.max(0.7F, at * 0.011F);
							float height = Math.max(200.0F * shrink, at * 0.6F);
							float alpha = near * (0.95F - 0.25F * Mth.clamp((flat - 96.0F) / 400.0F, 0.0F, 1.0F));
							float wave = Float.isNaN(time) ? Float.NaN : time * 2.4F + (float)(wp.id.hashCode() & 0xFF) * 0.11F;
							WorldDraw.beam(b, x, y, z, height, core, glow, wp.color, alpha, wave);
						}
					}
				}
			}
		}
	}

	static void labels(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		List<TeamData.Waypoint> list = waypoints();
		TeamsConfig cfg = TeamsConfig.get();
		boolean on = cfg.worldWaypoints && WorldView.ready() && mc.player != null;
		if (!list.isEmpty() && mc.player != null) {
			String dimension = TeamsState.currentDimension();
			Vec3 me = mc.player.getPosition(WorldView.partialTick());
			int w = g.guiWidth();
			int h = g.guiHeight();
			long now = Clock.nowMs();

			for (TeamData.Waypoint wp : list) {
				boolean here = on && dimension.equals(wp.dimension) && cfg.showsWaypoint(wp.id);
				WaypointMarks.Label l = LABELS.get(wp.id);
				if (l == null) {
					if (!here) {
						continue;
					}

					l = new WaypointMarks.Label();
					LABELS.put(wp.id, l);
				}

				l.seen = now;
				double dx = (double)wp.x + 0.5 - me.x;
				double dy = (double)wp.y - me.y;
				double dz = (double)wp.z + 0.5 - me.z;
				double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
				boolean front = here && WorldView.project((double)wp.x + 0.5, (double)wp.y + 1.8, (double)wp.z + 0.5, w, h, SPOT) && SPOT.within(w, h, -8);
				l.shown.set(front ? 1.0F : 0.0F);
				float shown = l.shown.get();
				if (shown <= 0.01F) {
					l.placed = false;
				} else if (here && SPOT.front) {
					float focus = SPOT.focus(w, h, (float)h * 0.16F);
					l.open.set(focus > 0.0F ? 1.0F : 0.0F);
					float open = l.open.get();
					float alpha = shown
						* Mth.clamp((float)(d - 3.0) / 5.0F, 0.0F, 1.0F)
						* (0.74F + 0.26F * open)
						* (1.0F - 0.2F * Mth.clamp((float)(d - 128.0) / 512.0F, 0.0F, 1.0F));
					if (!(alpha <= 0.02F)) {
						if (!l.nameOf.equals(wp.name)) {
							l.nameOf = wp.name;
							Component name = Component.literal(wp.name);
							l.name = CFHGuiDraw.truncate(name, 110);
							l.nameW = Math.min(110, CFHGuiDraw.width(name));
						}

						Component distance = l.distance.of(d).text;
						int distW = l.distance.width;
						int compactW = 15 + distW + 4;
						int targetW = focus > 0.0F && l.nameW > 0 ? compactW + l.nameW + 4 : compactW;
						int tipX = Math.round(SPOT.x);
						int tipY = Math.round(SPOT.y);
						int wantY = tipY - 3 - 12;
						int px = Mth.clamp(Math.round(SPOT.x - (float)targetW / 2.0F), 2, Math.max(2, w - targetW - 2));
						int clearY = LabelStack.place(px, wantY, targetW, 12, true);
						if (!l.placed) {
							l.width.snap((float)targetW);
							l.lift.snap((float)(clearY - tipY));
							l.placed = true;
						} else {
							l.width.set((float)targetW);
							l.lift.set((float)(clearY - tipY));
						}

						int lw = Math.round(l.width.get());
						int lx = px + (targetW - lw) / 2;
						int ly = tipY + Math.round(l.lift.get());
						int color = 0xFF000000 | wp.color;
						int mark = HudDraw.readable(wp.color);
						HudDraw.batch(g, () -> {
							HudDraw.stem(g, tipX, tipY, lx, ly + 12, lw, mark, alpha);
							HudDraw.pill(g, lx, ly, lw, 12, alpha);
							HudDraw.text(g, distance, lx + lw - 4 - distW, ly + 2, HudDraw.mutedColor(), alpha);
						});
						if (open > 0.02F) {
							int nameX = lx + 12 + 3;
							int nameEnd = lx + lw - 4 - distW - 3;
							if (nameEnd > nameX) {
								g.enableScissor(nameX, ly, nameEnd, ly + 12);
								HudDraw.text(g, l.name, nameX, ly + 2, HudDraw.textColor(), alpha * open);
								g.disableScissor();
							}
						}

						HudDraw.badge(g, WaypointIcons.get(wp.icon), lx, ly, 12, color, alpha);
					}
				}
			}

			if (LABELS.size() > list.size()) {
				LABELS.values().removeIf(lxx -> lxx.seen != now);
			}
		} else {
			LABELS.clear();
		}
	}

	static void clear() {
		LABELS.clear();
	}

	private WaypointMarks() {
	}

	private static final class Label {
		final Anim shown = new Anim(0.0F, 220, Ease.OUT_CUBIC);
		final Anim open = new Anim(0.0F, 170, Ease.OUT_CUBIC);
		final Anim width = new Anim(0.0F, 170, Ease.OUT_CUBIC);
		final Anim lift = new Anim(0.0F, 160, Ease.OUT_CUBIC);
		boolean placed;
		long seen;
		String nameOf = "";
		FormattedCharSequence name = FormattedCharSequence.EMPTY;
		int nameW;
		final HudDraw.Distance distance = new HudDraw.Distance();
	}
}
