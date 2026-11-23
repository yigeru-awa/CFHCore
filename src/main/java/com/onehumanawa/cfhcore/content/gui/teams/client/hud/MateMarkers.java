package com.onehumanawa.cfhcore.content.gui.teams.client.hud;

import com.onehumanawa.cfhcore.content.gui.teams.TeamsConfig;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsState;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

final class MateMarkers {
	private static final float LIFT = 0.95F;
	private static final int LABEL_H = 12;
	private static final int NAME_MAX = 84;
	private static final int CROWD = 4;
	private static final int ALLY = -8664924;
	private static final WorldView.Spot SPOT = new WorldView.Spot();
	private static final float[] AT = new float[2];

	static void render(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		List<Mates.Mate> all = Mates.all();
		if (!all.isEmpty() && mc.player != null) {
			TeamsConfig cfg = TeamsConfig.get();
			boolean show = cfg.worldMarkers && WorldView.ready();
			Vec3 me = mc.player.getPosition(WorldView.partialTick());
			int w = g.guiWidth();
			int h = g.guiHeight();
			float min = (float)Math.max(0, cfg.markerMinDistance);
			int inView = 0;
			if (show) {
				for (Mates.Mate m : all) {
					if (m.online
						&& m.here()
						&& !(m.distanceTo(me) <= (double)min)
						&& WorldView.project(m.x, m.y + (double)m.height + 0.95F, m.z, w, h, SPOT)
						&& SPOT.within(w, h, 0)) {
						inView++;
					}
				}
			}

			TeamData team = TeamsState.team();
			int teamColor = HudDraw.readable(team == null ? HudDraw.theme().accent() : team.color);

			for (Mates.Mate mx : all) {
				boolean on = show && mx.online && mx.here();
				mx.marker.set(on ? 1.0F : 0.0F);
				float shown = mx.marker.get();
				if (shown <= 0.01F) {
					mx.labelPlaced = false;
				} else {
					double d = mx.distanceTo(me);
					float alpha = shown * Mth.clamp((float)(d - (double)min) / 3.0F, 0.0F, 1.0F) * (1.0F - 0.28F * Mth.clamp((float)(d - 96.0) / 400.0F, 0.0F, 1.0F));
					if (mx.state.sneaking() && !mx.dead()) {
						alpha *= 0.8F;
					}

					if (mx.ally()) {
						alpha *= 0.9F;
					}

					if (alpha <= 0.02F) {
						mx.labelPlaced = false;
					} else {
						WorldView.project(mx.x, mx.y + (double)mx.height + 0.95F, mx.z, w, h, SPOT);
						Component distance = mx.distance.of(d).text;
						int distW = mx.distance.width;
						mx.fitLabelName(84);
						int compactW = 13 + distW + 4;
						boolean edge = WorldView.place(SPOT, w, h, compactW / 2, 6, AT);
						boolean wantName = !edge && !mx.name.isEmpty() && (inView <= 4 || SPOT.focus(w, h, (float)h * 0.2F) > 0.0F);
						mx.detail.set(wantName ? 1.0F : 0.0F);
						float detail = mx.detail.get();
						int targetW = wantName ? compactW + mx.labelNameW + 4 : compactW;
						int tipX = Math.round(AT[0]);
						int tipY = Math.round(AT[1]);
						int wantY = edge ? tipY - 6 : tipY - 3 - 12;
						int px = Mth.clamp(Math.round(AT[0] - (float)targetW / 2.0F), 2, Math.max(2, w - targetW - 2));
						int[] frames = PartyFrames.bounds();
						if (edge && frames != null && wantY + 12 > frames[1] - 4 && wantY < frames[3] + 4 && px < frames[2] + 12 && px + targetW > frames[0] - 12) {
							px = frames[0] < w / 2 ? frames[2] + 14 : frames[0] - 14 - targetW;
						}

						int clearY = LabelStack.place(px, wantY, targetW, 12, true);
						if (!mx.labelPlaced) {
							mx.labelWidth.snap((float)targetW);
							mx.labelLift.snap((float)(clearY - tipY));
							mx.labelPlaced = true;
						} else {
							mx.labelWidth.set((float)targetW);
							mx.labelLift.set((float)(clearY - tipY));
						}

						int lw = Math.round(mx.labelWidth.get());
						int lx = px + (targetW - lw) / 2;
						int ly = tipY + Math.round(mx.labelLift.get());
						int color = mx.ally() ? -8664924 : teamColor;
						float a = alpha;
						int textColor = mx.dead() ? HudDraw.dimColor() : HudDraw.textColor();
						HudDraw.batch(g, () -> {
							if (!edge) {
								HudDraw.stem(g, tipX, tipY, lx, ly + 12, lw, color, a);
							}

							HudDraw.pill(g, lx, ly, lw, 12, a);
							HudDraw.text(g, distance, lx + lw - 4 - distW, ly + 2, HudDraw.mutedColor(), a);
						});
						if (detail > 0.02F) {
							int nameX = lx + 13;
							int nameEnd = lx + lw - 4 - distW - 3;
							if (nameEnd > nameX) {
								g.enableScissor(nameX, ly, nameEnd, ly + 12);
								HudDraw.text(g, mx.labelName, nameX, ly + 2, textColor, a * detail);
								g.disableScissor();
							}
						}

						HudDraw.face(g, mx.skin(), lx + 2, ly + 2, 8, mx.dead() ? 9072238 : 16777215, a);
						if (edge) {
							float cx = (float)lx + (float)lw / 2.0F;
							float cy = (float)ly + 6.0F;
							HudDraw.dart(
								g, cx + SPOT.dirX * ((float)lw / 2.0F + 7.0F), cy + SPOT.dirY * 13.0F, (float)Math.atan2((double)SPOT.dirX, (double)(-SPOT.dirY)), 4.0F, color, a
							);
						}
					}
				}
			}
		}
	}

	private MateMarkers() {
	}
}
