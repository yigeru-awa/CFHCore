package com.onehumanawa.cfhcore.content.gui.teams.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.onehumanawa.cfhcore.content.gui.core.client.render.CFHGuiRenderEvents;
import com.onehumanawa.cfhcore.content.gui.core.client.render.CFHGuiRenderEvents.WorldRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiKeys;
import com.onehumanawa.cfhcore.content.gui.teams.CFHGuiTeams;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class TeamsHud {
	private static boolean initialised;
	private static boolean hudFailed;
	private static boolean worldFailed;

	public static synchronized void init() {
		if (!initialised) {
			initialised = true;
			CFHGuiKeys.register(HudKeys.BORDERS_KEY);
			CFHGuiEvents.CLIENT_TICK_END.register(TeamsHud::tick);
			CFHGuiEvents.HUD_RENDER.register(TeamsHud::hud);
			CFHGuiRenderEvents.AFTER_TRANSLUCENT.register(TeamsHud::world);
			CFHGuiEvents.CLIENT_JOINED_SERVER.register(TeamsHud::joined);
			CFHGuiEvents.CLIENT_LEFT_SERVER.register(TeamsHud::left);
			TeamsState.NOTICE.register(Notices::show);
			TeamsState.PING.register(Pings::arrived);
			TeamsState.CLAIMS_CHANGED.register(ClaimBorders::claimsChanged);
			HudDemo.init();
		}
	}

	public static void showClaimBorders() {
		ClaimBorders.show();
	}

	private static void tick() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && mc.level != null) {
			Mates.tick();
			PingWheel.tick();
			HudKeys.tick();
			LandNotice.tick();
			ClaimBorders.tick();
		}
	}

	private static void world(WorldRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null && mc.player != null && !ShaderMods.shadowPass()) {
			WorldView.capture(context);
			if (!worldFailed && !mc.options.hideGui && TeamsState.available()) {
				try {
					BufferBuilder light = WorldDraw.begin();
					WaypointMarks.beams(light);
					Pings.light(light);
					WorldDraw.drawLight(light);
					ClaimBorders.render();
				} catch (RuntimeException var6) {
					worldFailed = true;
					CFHGuiTeams.LOGGER.error("[CFHGui Teams] drawing in the world failed; beams and borders are off for this session", var6);
				} finally {
					WorldDraw.restore();
				}
			}
		}
	}

	private static void hud(GuiGraphics g, float partialTick) {
		Minecraft mc = Minecraft.getInstance();

		try {
			if (!hudFailed && mc.player != null && mc.level != null && !mc.options.hideGui && TeamsState.available()) {
				RenderSystem.enableDepthTest();
				Mates.frame(WorldView.ready() ? WorldView.partialTick() : partialTick);
				LabelStack.begin();
				LandNotice.reserve(g);
				WaypointMarks.labels(g);
				MateMarkers.render(g);
				Pings.marks(g);
				PartyFrames.render(g);
				LandNotice.render(g);
				PingWheel.render(g);
				return;
			}
		} catch (RuntimeException var7) {
			hudFailed = true;
			CFHGuiTeams.LOGGER.error("[CFHGui Teams] drawing the HUD failed; it is off for this session", var7);
			return;
		} finally {
			WorldView.endFrame();
		}
	}

	private static void joined() {
		LandNotice.joined();
		ClaimBorders.joined();
	}

	private static void left() {
		if (!RenderSystem.isOnRenderThread()) {
			Minecraft.getInstance().execute(TeamsHud::left);
		} else {
			Mates.clear();
			PartyFrames.clear();
			WaypointMarks.clear();
			Pings.clear();
			PingWheel.reset();
			LandNotice.clear();
			ClaimBorders.release();
			WorldDraw.release();
		}
	}

	private TeamsHud() {
	}
}
