package com.onehumanawa.cfhcore.content.gui.teams.client.hud;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.teams.TeamsConfig;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsState;
import com.onehumanawa.cfhcore.content.gui.teams.model.PingData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

final class Pings {
	private static final int FADE_IN_MS = 160;
	private static final int FADE_OUT_MS = 700;
	private static final int PULSE_MS = 1300;
	private static final int BADGE_R = 8;
	private static final int TEXT_H = 12;
	private static final Map<TeamsState.ActivePing, Pings.Words> WORDS = new HashMap<>();
	private static final WorldView.Spot SPOT = new WorldView.Spot();
	private static final float[] AT = new float[2];
	private static final double[] POS = new double[4];

	static int color(int kind) {
		return switch (kind) {
			case 1 -> -1750963;
			case 2 -> -2055080;
			case 3 -> -10702474;
			case 4 -> -3115319;
			default -> -10770208;
		};
	}

	static Icon icon(int kind) {
		return switch (kind) {
			case 1 -> Icon.WARNING;
			case 2 -> Icon.STAR_FILLED;
			case 3 -> Icon.UPG_SPEED;
			case 4 -> Icon.PLUS;
			default -> Icon.PIN;
		};
	}

	static Component words(int kind) {
		return Component.translatable("cfhgui_teams.ping." + PingData.kindKey(kind));
	}

	private static List<TeamsState.ActivePing> pings() {
		List<TeamsState.ActivePing> demo = HudDemo.pings();
		return demo != null ? demo : TeamsState.pings();
	}

	static void arrived(PingData ping) {
		if (TeamsConfig.get().pingSound && ping.dimension().equals(TeamsState.currentDimension())) {
			SoundEvent sound;
			float pitch;
			switch (ping.kind()) {
				case 1:
					sound = (SoundEvent)SoundEvents.NOTE_BLOCK_BIT.value();
					pitch = 0.75F;
					break;
				case 2:
					sound = (SoundEvent)SoundEvents.NOTE_BLOCK_CHIME.value();
					pitch = 1.5F;
					break;
				case 3:
					sound = (SoundEvent)SoundEvents.NOTE_BLOCK_HARP.value();
					pitch = 1.25F;
					break;
				case 4:
					sound = (SoundEvent)SoundEvents.NOTE_BLOCK_BELL.value();
					pitch = 0.9F;
					break;
				default:
					sound = (SoundEvent)SoundEvents.NOTE_BLOCK_PLING.value();
					pitch = 1.6F;
			}

			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, 0.55F));
		}
	}

	private static void position(PingData p, double[] out) {
		Minecraft mc = Minecraft.getInstance();
		if (p.entity() >= 0 && mc.level != null) {
			Entity e = mc.level.getEntity(p.entity());
			if (e != null && !e.isRemoved()) {
				Vec3 at = e.getPosition(WorldView.partialTick());
				out[0] = at.x;
				out[1] = at.y;
				out[2] = at.z;
				out[3] = (double)e.getBbHeight() + 0.55;
				return;
			}
		}

		out[0] = p.x();
		out[1] = p.y();
		out[2] = p.z();
		out[3] = 0.6;
	}

	private static float life(long ms) {
		return ms >= 0L && ms < 9000L ? Math.min(Math.min(1.0F, (float)ms / 160.0F), Math.min(1.0F, (float)(9000L - ms) / 700.0F)) : 0.0F;
	}

	static void light(BufferBuilder b) {
		List<TeamsState.ActivePing> pings = pings();
		if (!pings.isEmpty()) {
			String dimension = TeamsState.currentDimension();
			long now = System.currentTimeMillis();
			float motion = HudDraw.theme().motion();

			for (TeamsState.ActivePing ap : pings) {
				PingData p = ap.ping();
				if (dimension.equals(p.dimension())) {
					long ms = now - ap.bornMs();
					float life = life(ms);
					if (!(life <= 0.0F)) {
						position(p, POS);
						float x = (float)(POS[0] - WorldView.camX());
						float y = (float)(POS[1] - WorldView.camY()) + 0.03F;
						float z = (float)(POS[2] - WorldView.camZ());
						int color = color(p.kind());
						float far = (float)Math.sqrt((double)(x * x + y * y + z * z));
						float reach = Math.max(1.9F, far * 0.035F);
						if (motion <= 0.0F) {
							WorldDraw.ring(b, x, y, z, reach * 0.6F, reach * 0.2F, color, 0.7F * life);
						} else {
							float period = 1300.0F * motion;

							for (int i = 0; i < 2; i++) {
								float t = ((float)ms + (float)i * period / 2.0F) % period / period;
								WorldDraw.ring(b, x, y, z, 0.2F + reach * Ease.OUT_CUBIC.apply(t), reach * 0.12F + reach * 0.16F * t, color, 0.85F * (1.0F - t) * life);
							}
						}

						WorldDraw.ring(b, x, y, z, 0.3F, 0.3F, color, 0.8F * life);
						WorldDraw.beam(b, x, y, z, Math.max(5.0F, far * 0.08F), Math.max(0.05F, far * 0.0012F), Math.max(0.3F, far * 0.005F), color, 0.75F * life, Float.NaN);
					}
				}
			}
		}
	}

	static void marks(GuiGraphics g) {
		List<TeamsState.ActivePing> pings = pings();
		if (pings.isEmpty()) {
			if (!WORDS.isEmpty()) {
				WORDS.clear();
			}
		} else {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null && WorldView.ready()) {
				String dimension = TeamsState.currentDimension();
				Vec3 me = mc.player.getPosition(WorldView.partialTick());
				int w = g.guiWidth();
				int h = g.guiHeight();
				long now = System.currentTimeMillis();
				float motion = HudDraw.theme().motion();
				boolean vanilla = HudDraw.vanilla();

				for (TeamsState.ActivePing ap : pings) {
					PingData p = ap.ping();
					if (dimension.equals(p.dimension())) {
						long ms = now - ap.bornMs();
						float life = life(ms);
						if (!(life <= 0.02F)) {
							position(p, POS);
							double dx = POS[0] - me.x;
							double dy = POS[1] - me.y;
							double dz = POS[2] - me.z;
							double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
							WorldView.project(POS[0], POS[1] + POS[3], POS[2], w, h, SPOT);
							boolean edge = WorldView.place(SPOT, w, h, 10, 10, AT);
							int cx = Math.round(AT[0]);
							int cy = Math.round(AT[1]);
							int color = color(p.kind());
							float a = life;
							if (motion <= 0.0F) {
								ring(g, cx, cy, 12, Colors.scaleAlpha(color, 0.5F * life), vanilla);
							} else {
								float period = 1300.0F * motion;

								for (int i = 0; i < 2; i++) {
									float t = ((float)ms + (float)i * period / 2.0F) % period / period;
									ring(g, cx, cy, 9 + Math.round(13.0F * Ease.OUT_CUBIC.apply(t)), Colors.scaleAlpha(color, 0.8F * (1.0F - t) * a), vanilla);
								}
							}

							float pop = motion <= 0.0F ? 1.0F : 0.6F + 0.4F * Ease.OUT_BACK.apply(Mth.clamp((float)ms / (220.0F * motion), 0.0F, 1.0F));
							g.pose().pushPose();
							g.pose().translate((float)cx + 0.5F, (float)cy + 0.5F, 0.0F);
							g.pose().scale(pop, pop, 1.0F);
							g.pose().translate(-0.5F, -0.5F, 0.0F);
							HudDraw.batch(g, () -> {
								if (vanilla) {
									CFHGuiDraw.rect(g, -9, -9, 19, 19, Colors.scaleAlpha(-16777216, a));
									CFHGuiDraw.rect(g, -8, -8, 17, 17, Colors.scaleAlpha(color, a));
								} else {
									CFHGuiDraw.pixelCircle(g, 0, 0, 9, Colors.scaleAlpha(Colors.withAlpha(HudDraw.theme().palette().bg(), 240), a));
									CFHGuiDraw.pixelCircle(g, 0, 0, 8, Colors.scaleAlpha(color, a));
								}
							});
							g.pose().translate(0.5F, 0.5F, 0.0F);
							Icons.draw(g, icon(p.kind()), -6, -6, 12, Colors.scaleAlpha(Colors.readableOn(color), a));
							g.pose().popPose();
							Pings.Words words = words(ap);
							words.distance().of(d);
							if (edge) {
								HudDraw.dart(
									g,
									(float)cx + 0.5F + SPOT.dirX * 17.0F,
									(float)cy + 0.5F + SPOT.dirY * 17.0F,
									(float)Math.atan2((double)SPOT.dirX, (double)(-SPOT.dirY)),
									4.0F,
									color,
									a
								);
								int tw = words.distance().width + 8;
								int tx = Mth.clamp(cx - tw / 2, 2, Math.max(2, w - tw - 2));
								boolean above = SPOT.dirY > 0.5F;
								int ty = LabelStack.place(tx, above ? cy - 8 - 4 - 12 : cy + 8 + 4, tw, 12, above);
								HudDraw.batch(g, () -> {
									HudDraw.pill(g, tx, ty, tw, 12, a);
									HudDraw.text(g, words.distance().text, tx + 4, ty + 2, HudDraw.mutedColor(), a);
								});
							} else {
								int tw = 4 + words.nameW() + 4 + words.kindW() + 5 + words.distance().width + 4;
								int tx = Mth.clamp(cx - tw / 2, 2, Math.max(2, w - tw - 2));
								int ty = LabelStack.place(tx, cy + 8 + 5, tw, 12, false);
								HudDraw.batch(g, () -> {
									HudDraw.pill(g, tx, ty, tw, 12, a);
									HudDraw.text(g, words.name(), tx + 4, ty + 2, HudDraw.textColor(), a);
									HudDraw.text(g, words.kind(), tx + 4 + words.nameW() + 4, ty + 2, HudDraw.readable(color), a);
									HudDraw.text(g, words.distance().text, tx + tw - 4 - words.distance().width, ty + 2, HudDraw.mutedColor(), a);
								});
							}
						}
					}
				}

				if (WORDS.size() > pings.size()) {
					WORDS.keySet().removeIf(k -> !pings.contains(k));
				}
			}
		}
	}

	private static void ring(GuiGraphics g, int cx, int cy, int r, int color, boolean square) {
		if (Colors.alpha(color) >= 4) {
			if (square) {
				CFHGuiDraw.outline(g, cx - r, cy - r, 2 * r + 1, 2 * r + 1, color, 0);
			} else {
				HudDraw.batch(g, () -> CFHGuiDraw.pixelRing(g, cx, cy, r, color));
			}
		}
	}

	private static Pings.Words words(TeamsState.ActivePing ap) {
		Pings.Words words = WORDS.get(ap);
		if (words == null) {
			Component name = Component.literal(name(ap.ping().by()));
			Component kind = words(ap.ping().kind());
			words = new Pings.Words(name, CFHGuiDraw.width(name), kind, CFHGuiDraw.width(kind), new HudDraw.Distance());
			WORDS.put(ap, words);
		}

		return words;
	}

	private static String name(UUID by) {
		Minecraft mc = Minecraft.getInstance();
		if (by.equals(TeamsState.self())) {
			return mc.getUser().getName();
		} else {
			TeamData team = TeamsState.team();
			TeamData.Member member = team == null ? null : team.member(by);
			if (member != null && !member.name.isEmpty()) {
				return member.name;
			} else {
				ClientPacketListener connection = mc.getConnection();
				PlayerInfo info = connection == null ? null : connection.getPlayerInfo(by);
				return info != null && info.getProfile().getName() != null ? info.getProfile().getName() : "";
			}
		}
	}

	static void clear() {
		WORDS.clear();
	}

	private Pings() {
	}

	private static record Words(Component name, int nameW, Component kind, int kindW, HudDraw.Distance distance) {
	}
}
