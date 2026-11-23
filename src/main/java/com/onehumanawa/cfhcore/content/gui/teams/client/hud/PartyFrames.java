package com.onehumanawa.cfhcore.content.gui.teams.client.hud;

import com.onehumanawa.cfhcore.content.gui.core.client.HudKeepOut;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Anchor;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.teams.TeamsConfig;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsState;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

final class PartyFrames {
	static final int H = 20;
	static final int GAP = 2;
	private static final int MIN_W = 92;
	private static final int MAX_W = 148;
	private static final int DISTANCE_ROOM = 23;
	private static final int ARROW_ROOM = 10;
	private static final int FOLD_H = 12;
	private static final int FOLD_FACES = 5;
	private static final int ARMOR = -4931380;
	private static final Component DEAD = Component.translatable("cfhgui_teams.hud.dead");
	private static final Anim FOLD = new Anim(0.0F, 200, Ease.OUT_CUBIC);
	private static final Anim WIDTH = new Anim(92.0F, 180, Ease.OUT_CUBIC);
	private static boolean widthKnown;
	private static int foldCount;
	@Nullable
	private static int[] bounds;

	@Nullable
	static int[] bounds() {
		return bounds;
	}

	static void render(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		TeamsConfig cfg = TeamsConfig.get();
		List<Mates.Mate> party = Mates.party();
		bounds = null;
		if (!party.isEmpty() && mc.player != null) {
			boolean show = cfg.partyFrames && TeamsState.hasTeam() && !mc.getDebugOverlay().showDebugScreen();
			Vec3 me = mc.player.getPosition(WorldView.partialTick());
			int max = Math.max(1, cfg.partyMax);
			int online = 0;
			int folded = 0;
			int need = 92;

			for (Mates.Mate m : party) {
				boolean in = show && m.online && online < max;
				if (m.online) {
					if (++online > max) {
						folded++;
					}
				}

				m.row.set(in ? 1.0F : 0.0F);
				if (in) {
					need = Math.max(need, 22 + m.frameNameWidth() + where(m, cfg, me) + 4);
				}
			}

			if (folded > 0) {
				foldCount = folded;
			}

			FOLD.set(show && folded > 0 ? 1.0F : 0.0F);
			float total = 0.0F;

			for (Mates.Mate m : party) {
				total += 22.0F * m.row.get();
			}

			float fold = FOLD.get();
			total += 14.0F * fold;
			if (total <= 0.5F) {
				widthKnown = false;
			} else {
				total -= 2.0F;
				need = Math.min(need, 148);
				if (!widthKnown) {
					WIDTH.snap((float)need);
					widthKnown = true;
				} else {
					WIDTH.set((float)need);
				}

				int rowW = Math.round(WIDTH.get());
				float scale = (float)Mth.clamp(cfg.partyScale, 0.5, 3.0);
				Anchor anchor = Anchor.parse(cfg.partyAnchor, Anchor.TOP_LEFT);
				boolean right = anchor.fx > 0.75F;
				int sw = g.guiWidth();
				int sh = g.guiHeight();
				float bw = (float)rowW * scale;
				float bh = total * scale;
				int ox = Math.round(
					anchor.fx > 0.75F ? (float)sw - bw - (float)cfg.partyX : (anchor.fx > 0.25F ? ((float)sw - bw) / 2.0F + (float)cfg.partyX : (float)cfg.partyX)
				);
				float wantY = anchor.fy > 0.75F
					? (float)sh - bh - (float)cfg.partyY
					: (anchor.fy > 0.25F ? ((float)sh - bh) / 2.0F + (float)cfg.partyY : (float)cfg.partyY);
				int oy = Math.round(HudKeepOut.clearY((float)ox, wantY, bw, bh, (float)sh, 4.0F));
				bounds = new int[]{ox, oy, Mth.ceil((float)ox + bw), Mth.ceil((float)oy + bh)};
				float yaw = mc.gameRenderer.getMainCamera().getYRot();
				TeamData team = TeamsState.team();
				int mark = HudDraw.readable(team == null ? HudDraw.theme().accent() : team.color);
				g.pose().pushPose();
				g.pose().translate((float)ox, (float)oy, 0.0F);
				g.pose().scale(scale, scale, 1.0F);
				float y = 0.0F;

				for (Mates.Mate m : party) {
					float r = m.row.get();
					if (!(r <= 0.01F)) {
						row(g, m, Math.round(y), rowW, need, r, right, cfg, me, yaw, mark, ox, oy, scale);
						y += 22.0F * r;
					}
				}

				if (fold > 0.01F) {
					fold(g, party, Math.round(y), rowW, fold, right, max);
				}

				g.pose().popPose();
			}
		} else {
			widthKnown = false;
		}
	}

	private static int where(Mates.Mate m, TeamsConfig cfg, Vec3 me) {
		if (m.dead()) {
			m.where = DEAD;
			m.whereW = CFHGuiDraw.width(DEAD);
			m.whereArrow = false;
			return 6 + m.whereW;
		} else if (!m.here()) {
			if (!m.state.dimension().equals(m.elsewhereOf)) {
				m.elsewhereOf = m.state.dimension();
				m.elsewhere = HudDraw.dimensionName(m.elsewhereOf);
				m.elsewhereW = CFHGuiDraw.width(m.elsewhere);
			}

			m.where = m.elsewhere;
			m.whereW = m.elsewhereW;
			m.whereArrow = false;
			return 6 + m.whereW;
		} else if (cfg.partyShowDistance) {
			m.where = m.distance.of(m.distanceTo(me)).text;
			m.whereW = m.distance.width;
			m.whereArrow = true;
			return 6 + Math.max(23, m.whereW) + 10;
		} else {
			m.where = null;
			m.whereW = 0;
			m.whereArrow = false;
			return 0;
		}
	}

	private static void row(
		GuiGraphics g, Mates.Mate m, int y, int w, int fullW, float in, boolean right, TeamsConfig cfg, Vec3 me, float yaw, int mark, int ox, int oy, float scale
	) {
		Theme t = HudDraw.theme();
		Palette p = t.palette();
		float a = Mth.clamp(in, 0.0F, 1.0F);
		int x = Math.round((1.0F - in) * (float)(right ? 10 : -10));
		boolean dead = m.dead();
		boolean sneaking = m.state.sneaking() && !dead;
		boolean bars = cfg.partyShowHealth;
		int textY = bars ? y + 3 : y + 6;
		Component where = m.where;
		boolean arrow = where != null && m.whereArrow;
		int whereColor = dead ? p.danger() : (arrow ? HudDraw.mutedColor() : HudDraw.dimColor());
		int end = x + w - 4 - (arrow ? 10 : 0);
		int whereX = end - m.whereW;
		int nameX = x + 22;
		int room = (where == null ? end : whereX - 6) - nameX;
		FormattedCharSequence name = m.frameName(room + (fullW - w));
		int nameColor = dead ? HudDraw.dimColor() : (sneaking ? HudDraw.mutedColor() : HudDraw.textColor());
		float hurt = m.hurt.get();
		float hp = m.health.get();
		float trail = m.trail;
		float armor = m.armor.get();
		HudDraw.batch(g, () -> {
			HudDraw.plate(g, x, y, w, 20, a);
			if (hurt > 0.01F) {
				CFHGuiDraw.rect(g, x + 1, y + 1, w - 2, 18, Colors.scaleAlpha(Colors.withAlpha(p.danger(), 88), hurt * a));
			}

			if (where != null) {
				HudDraw.text(g, where, whereX, textY, whereColor, a);
			}

			if (bars) {
				int bx = x + 22;
				int bw = w - 26;
				int by = y + 13;
				int color = HudDraw.healthColor(hp);
				if (!dead && hp < 0.28F && t.motion() > 0.0F) {
					color = Colors.brighten(color, 0.3F * (0.5F + 0.5F * Mth.sin((float)(Clock.nowMs() % 900L) / 900.0F * (float) (Math.PI * 2))));
				}

				HudDraw.bar(g, bx, by, bw, 3, hp, color, a);
				if (trail > hp + 0.004F) {
					int from = Math.round((float)bw * hp);
					int to = Math.round((float)bw * trail);
					CFHGuiDraw.rect(g, bx + from, by, to - from, 3, Colors.scaleAlpha(t.isVanilla() ? -1 : -856346, a * 0.85F));
				}

				if (armor > 0.01F && !dead) {
					HudDraw.bar(g, bx, by + 4, bw, 1, armor, -4931380, a * 0.9F);
				}
			}
		});
		if (room > 0) {
			g.enableScissor(
				ox + Mth.floor((float)nameX * scale),
				oy + Mth.floor((float)y * scale),
				ox + Mth.ceil((float)(nameX + room) * scale),
				oy + Mth.ceil((float)(y + 20) * scale)
			);
			HudDraw.text(g, name, nameX, textY, nameColor, a);
			g.disableScissor();
		}

		int tint = dead ? 9072238 : (sneaking ? 12500670 : 16777215);
		HudDraw.face(g, m.skin(), x + 2, y + 2, 16, tint, a);
		if (arrow) {
			float bearing = (float)Math.toDegrees(Math.atan2(-(m.x - me.x), m.z - me.z));
			HudDraw.dart(g, (float)end + 5.5F, (float)textY + 3.5F, Mth.wrapDegrees(bearing - yaw) * (float) (Math.PI / 180.0), 4.0F, mark, a);
		}
	}

	private static void fold(GuiGraphics g, List<Mates.Mate> party, int y, int rowW, float in, boolean right, int max) {
		float a = Mth.clamp(in, 0.0F, 1.0F);
		Component count = Component.translatable("cfhgui_teams.hud.more", new Object[]{Integer.toString(foldCount)});
		int faces = Math.min(5, foldCount);
		int w = 5 + CFHGuiDraw.width(count) + 4 + faces * 9 + 2;
		int x = (right ? rowW - w : 0) + Math.round((1.0F - in) * (float)(right ? 10 : -10));
		HudDraw.batch(g, () -> {
			HudDraw.pill(g, x, y, w, 12, a);
			HudDraw.text(g, count, x + 5, y + 2, HudDraw.mutedColor(), a);
		});
		int fx = x + 5 + CFHGuiDraw.width(count) + 4;
		int seen = 0;
		int drawn = 0;

		for (Mates.Mate m : party) {
			if (m.online) {
				if (++seen > max) {
					if (drawn++ >= faces) {
						break;
					}

					HudDraw.face(g, m.skin(), fx, y + 2, 8, m.dead() ? 9072238 : 16777215, a);
					fx += 9;
				}
			}
		}
	}

	static void clear() {
		bounds = null;
		widthKnown = false;
		FOLD.snap(0.0F);
	}

	private PartyFrames() {
	}
}
