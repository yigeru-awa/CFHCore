package com.onehumanawa.cfhcore.content.gui.server.client.hud;

import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.server.ServerClientConfig;
import com.onehumanawa.cfhcore.content.gui.server.CFHGuiServer;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerActions;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerClient;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerState;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Travel;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Ui;
import com.onehumanawa.cfhcore.content.gui.server.model.SelfState;
import com.onehumanawa.cfhcore.content.gui.server.model.TeleportRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class ServerHud {
	private static boolean initialised;
	private static final Anim WARMUP = new Anim(0.0F, 180, Ease.OUT_CUBIC);
	private static Component lastTarget = Component.empty();
	private static Icon lastIcon = Icon.WORLD;
	private static long answeredAt;

	public static synchronized void init() {
		if (!initialised) {
			initialised = true;
			CFHGuiEvents.HUD_RENDER.register(ServerHud::hud);
			CFHGuiEvents.SCREEN_RENDER_POST.register(ServerHud::overScreen);
			InventoryRow.register();
			CFHGuiEvents.KEY_PRESSED.register(ServerHud::key);
			CFHGuiEvents.CLIENT_TICK_END.register(ServerHud::tick);
			ServerState.NOTICE.register(ServerToasts::notice);
			ServerState.REQUESTS_CHANGED.register(ServerToasts::requests);
			CFHGuiEvents.CLIENT_LEFT_SERVER.register(ServerToasts::clear);
		}
	}

	private static void hud(GuiGraphics g, float partialTick) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && !mc.options.hideGui && ServerState.available()) {
			try {
				ServerClientConfig config = ServerClientConfig.get();
				if (config.hudWarmup) {
					warmup(g, g.guiWidth(), g.guiHeight());
				}

				if (config.hudStatus && mc.screen == null) {
					status(g, mc, g.guiWidth(), g.guiHeight());
				}
			} catch (RuntimeException var4) {
				CFHGuiServer.LOGGER.debug("[CFHGui Server] could not draw the HUD: {}", var4.toString());
			}
		}
	}

	private static void warmup(GuiGraphics g, int sw, int sh) {
		boolean pending = Travel.pending();
		WARMUP.set(pending);
		float a = WARMUP.get();
		if (!(a <= 0.01F)) {
			if (pending) {
				lastTarget = Travel.pendingTarget();
				lastIcon = Travel.pendingIcon();
			}

			Theme t = Theme.current();
			Palette p = t.palette();
			Component title = Txt.t("hud.warmup", lastTarget);
			Component hint = Txt.t("hud.warmup.cancels");
			int w = Math.min(sw - 16, Math.max(150, Math.max(CFHGuiDraw.width(title) + 62, CFHGuiDraw.width(hint) + 34)));
			int h = 30;
			int x = (sw - w) / 2;
			int y = sh / 2 + 28 + Math.round((1.0F - a) * 6.0F);
			g.pose().pushPose();
			g.pose().translate(0.0F, 0.0F, 250.0F);
			if (t.isVanilla()) {
				g.fill(x, y, x + w, y + 30, Colors.scaleAlpha(-1072689136, a));
				CFHGuiDraw.outline(g, x, y, w, 30, Colors.scaleAlpha(-7631989, a), 0);
			} else {
				CFHGuiDraw.floatingPanel(g, x, y, w, 30, a);
			}

			Icons.draw(g, lastIcon, x + 8, y + 7, 12, Colors.scaleAlpha(t.isVanilla() ? -1 : p.accentInk(), a));
			String left = String.format(Locale.ROOT, "%.1f s", (float)Travel.pendingLeftMs() / 1000.0F);
			if (pending) {
				Ui.textRight(g, Component.literal(left), x + w - 8, y + 6, Ui.strong(), a);
			}

			Ui.textFit(g, title, x + 26, y + 6, w - 34 - (pending ? CFHGuiDraw.width(left) + 6 : 0), Ui.strong(), a);
			Ui.textFit(g, hint, x + 26, y + 16, w - 34, p.textMuted(), a);
			Ui.bar(g, x + 3, y + 30 - 4, w - 6, 2, pending ? Travel.pendingProgress() : 1.0F, t.isVanilla() ? -1 : p.accent(), a);
			g.pose().popPose();
		}
	}

	private static void status(GuiGraphics g, Minecraft mc, int sw, int sh) {
		SelfState self = ServerState.self();
		if (self.afk || self.vanished || self.frozen || self.god) {
			Palette p = Theme.current().palette();
			List<Component> marks = new ArrayList<>();
			List<Integer> colors = new ArrayList<>();
			if (self.frozen) {
				marks.add(Txt.t("flag.frozen"));
				colors.add(-10770208);
			}

			if (self.afk) {
				marks.add(Txt.t("flag.afk"));
				colors.add(p.warning());
			}

			if (self.vanished) {
				marks.add(Txt.t("flag.vanished"));
				colors.add(p.textMuted());
			}

			if (self.god) {
				marks.add(Txt.t("flag.god"));
				colors.add(p.success());
			}

			int x = sw / 2 + 91 + 8 + (mc.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR ? 22 : 0);
			int y = sh - 17;

			for (int i = 0; i < marks.size(); i++) {
				g.fill(x, y - 1, x + CFHGuiDraw.width(marks.get(i)) + 8, y + 13, -1879048192);
				x += Ui.chip(g, marks.get(i), x, y, colors.get(i), 1.0F) + 4;
			}
		}
	}

	private static void overScreen(Screen screen, GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		if (screen instanceof ChatScreen && ServerState.available() && ServerClientConfig.get().mutedNote) {
			long until = ServerState.self().mutedUntil;
			if (until != 0L && (until <= 0L || until > System.currentTimeMillis())) {
				Theme t = Theme.current();
				Palette p = t.palette();
				Component line = until < 0L ? Txt.t("hud.muted.open") : Txt.t("hud.muted", Txt.left(until), Txt.clock(until));
				int w = CFHGuiDraw.width(line) + 26;
				int x = 2;
				int y = screen.height - 30;
				g.pose().pushPose();
				g.pose().translate(0.0F, 0.0F, 300.0F);
				g.fill(2, y, 2 + w, y + 13, t.isVanilla() ? -1073741824 : Colors.withAlpha(p.bg(), 224));
				CFHGuiDraw.rect(g, 2, y, 2, 13, p.danger());
				Icons.draw(g, Icon.MIC_OFF, 8, y + 3, 8, p.danger());
				g.drawString(CFHGuiDraw.font(), line, 20, y + 3, Ui.strong(), t.isVanilla());
				g.pose().popPose();
			}
		}
	}

	@Nullable
	static TeleportRequest newestAsked() {
		TeleportRequest newest = null;

		for (TeleportRequest r : ServerState.requests()) {
			if (r.incoming && ServerToasts.asking(r.player) && (newest == null || r.expiresAt > newest.expiresAt)) {
				newest = r;
			}
		}

		return newest;
	}

	private static boolean key(int key, int scancode, int modifiers) {
		if (!ServerState.available()) {
			return false;
		} else {
			boolean accept = ServerClient.ACCEPT_KEY.matches(key, scancode);
			boolean deny = ServerClient.DENY_KEY.matches(key, scancode);
			if (!accept && !deny) {
				return false;
			} else if (!answer(accept)) {
				return false;
			} else {
				answeredAt = System.currentTimeMillis();
				return true;
			}
		}
	}

	private static void tick() {
		boolean accept = false;
		boolean deny = false;

		while (ServerClient.ACCEPT_KEY.consumeClick()) {
			accept = true;
		}

		while (ServerClient.DENY_KEY.consumeClick()) {
			deny = true;
		}

		if ((accept || deny) && System.currentTimeMillis() - answeredAt >= 400L) {
			if (Minecraft.getInstance().screen == null && ServerState.available()) {
				answer(accept);
			}
		}
	}

	private static boolean answer(boolean accept) {
		TeleportRequest r = newestAsked();
		if (r == null) {
			return false;
		} else {
			if (accept) {
				ServerActions.tpaAccept(r.player);
			} else {
				ServerActions.tpaDeny(r.player);
			}

			return true;
		}
	}

	private ServerHud() {
	}
}
